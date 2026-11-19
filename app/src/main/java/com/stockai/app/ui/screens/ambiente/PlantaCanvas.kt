package com.stockai.app.ui.screens.ambiente

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockai.app.data.local.Ambiente
import com.stockai.app.data.local.Prateleira
import com.stockai.app.ui.components.formatarMetros
import kotlin.math.min

/**
 * Converte metros (mundo real) ↔ pixels (tela).
 * escala = quantos pixels vale 1 metro; origem = onde fica o canto (0, 0) do ambiente.
 */
private data class Geometria(val escala: Float, val origem: Offset) {
    fun paraTela(xMetros: Float, yMetros: Float) =
        Offset(origem.x + xMetros * escala, origem.y + yMetros * escala)

    fun paraMetros(ponto: Offset) =
        Offset((ponto.x - origem.x) / escala, (ponto.y - origem.y) / escala)
}

@Composable
fun PlantaCanvas(
    ambiente: Ambiente,
    prateleiras: List<Prateleira>,
    selecionadaId: Long?,
    onSelecionar: (Long?) -> Unit,
    onMover: (Prateleira, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val margemPx = with(LocalDensity.current) { 36.dp.toPx() }

    // Calcula a escala para o ambiente inteiro caber na área disponível
    var tamanho by remember { mutableStateOf(IntSize.Zero) }
    val geometria = remember(tamanho, ambiente.largura, ambiente.comprimento) {
        if (tamanho == IntSize.Zero) {
            null
        } else {
            val escala = min(
                (tamanho.width - 2 * margemPx) / ambiente.largura,
                (tamanho.height - 2 * margemPx) / ambiente.comprimento
            )
            val w = ambiente.largura * escala
            val h = ambiente.comprimento * escala
            Geometria(escala, Offset((tamanho.width - w) / 2, (tamanho.height - h) / 2))
        }
    }

    // Arraste em andamento: qual prateleira e a posição provisória dela (em metros)
    var arrasteId by remember { mutableStateOf<Long?>(null) }
    var arrastePos by remember { mutableStateOf(Offset.Zero) }

    // Os detectores de gesto são criados uma vez só; estes "Atual" garantem
    // que eles sempre enxerguem os dados mais recentes
    val geoAtual by rememberUpdatedState(geometria)
    val prateleirasAtuais by rememberUpdatedState(prateleiras)
    val onSelecionarAtual by rememberUpdatedState(onSelecionar)
    val onMoverAtual by rememberUpdatedState(onMover)

    /** Descobre qual prateleira está embaixo do dedo */
    fun acharPrateleira(toque: Offset): Prateleira? {
        val geo = geoAtual ?: return null
        val m = geo.paraMetros(toque)
        return prateleirasAtuais.lastOrNull { p ->
            m.x in p.posX..(p.posX + p.pegadaX) && m.y in p.posY..(p.posY + p.pegadaY)
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { tamanho = it }
            // Toque simples: seleciona (ou desmarca, se tocar no vazio)
            .pointerInput(Unit) {
                detectTapGestures { toque -> onSelecionarAtual(acharPrateleira(toque)?.id) }
            }
            // Arrastar: move a prateleira e só salva quando soltar o dedo
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { toque ->
                        val p = acharPrateleira(toque)
                        if (p != null) {
                            arrasteId = p.id
                            arrastePos = Offset(p.posX, p.posY)
                            onSelecionarAtual(p.id)
                        }
                    },
                    onDrag = { change, delta ->
                        val geo = geoAtual
                        if (arrasteId != null && geo != null) {
                            change.consume()
                            arrastePos += delta / geo.escala // pixels → metros
                        }
                    },
                    onDragEnd = {
                        val p = prateleirasAtuais.find { it.id == arrasteId }
                        if (p != null) onMoverAtual(p, arrastePos.x, arrastePos.y)
                        arrasteId = null
                    },
                    onDragCancel = { arrasteId = null }
                )
            }
    ) {
        val geo = geometria ?: return@Canvas
        val origem = geo.origem
        val w = ambiente.largura * geo.escala
        val h = ambiente.comprimento * geo.escala

        // Piso
        drawRect(color = cores.surface, topLeft = origem, size = Size(w, h))

        // Grade de 1 em 1 metro
        var x = 1f
        while (x < ambiente.largura) {
            val px = origem.x + x * geo.escala
            drawLine(cores.outlineVariant, Offset(px, origem.y), Offset(px, origem.y + h), strokeWidth = 1f)
            x += 1f
        }
        var y = 1f
        while (y < ambiente.comprimento) {
            val py = origem.y + y * geo.escala
            drawLine(cores.outlineVariant, Offset(origem.x, py), Offset(origem.x + w, py), strokeWidth = 1f)
            y += 1f
        }

        // Paredes
        drawRect(
            color = cores.primary,
            topLeft = origem,
            size = Size(w, h),
            style = Stroke(width = 3.dp.toPx())
        )

        // Prateleiras (a selecionada é desenhada por último, por cima das outras)
        val ordenadas = prateleiras.sortedBy { it.id == selecionadaId || it.id == arrasteId }
        ordenadas.forEach { p ->
            val pos = if (p.id == arrasteId) arrastePos else Offset(p.posX, p.posY)
            val topo = geo.paraTela(pos.x, pos.y)
            val tam = Size(p.pegadaX * geo.escala, p.pegadaY * geo.escala)
            val destacada = p.id == selecionadaId

            drawRoundRect(
                color = if (destacada) cores.secondary else cores.primary,
                topLeft = topo,
                size = tam,
                cornerRadius = CornerRadius(3.dp.toPx())
            )

            // Rótulo curto: "Prateleira B" → "B"
            val rotulo = p.nome.removePrefix("Prateleira ").take(3)
            val texto = textMeasurer.measure(
                rotulo,
                TextStyle(
                    color = if (destacada) cores.onSecondary else cores.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                texto,
                topLeft = Offset(
                    topo.x + (tam.width - texto.size.width) / 2,
                    topo.y + (tam.height - texto.size.height) / 2
                )
            )
        }

        // Cotas: largura embaixo, comprimento na lateral direita
        val estiloCota = TextStyle(color = cores.onSurfaceVariant, fontSize = 11.sp)
        val cotaLargura = textMeasurer.measure("${formatarMetros(ambiente.largura)} m", estiloCota)
        drawText(
            cotaLargura,
            topLeft = Offset(origem.x + (w - cotaLargura.size.width) / 2, origem.y + h + 6.dp.toPx())
        )
        val cotaComprimento = textMeasurer.measure("${formatarMetros(ambiente.comprimento)} m", estiloCota)
        val pivo = Offset(origem.x + w + 8.dp.toPx() + cotaComprimento.size.height / 2f, origem.y + h / 2)
        rotate(degrees = 90f, pivot = pivo) {
            drawText(
                cotaComprimento,
                topLeft = Offset(
                    pivo.x - cotaComprimento.size.width / 2f,
                    pivo.y - cotaComprimento.size.height / 2f
                )
            )
        }
    }
}
