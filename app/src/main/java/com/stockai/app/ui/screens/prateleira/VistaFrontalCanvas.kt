package com.stockai.app.ui.screens.prateleira

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockai.app.data.local.GondolaResumo

/*
 * Vista frontal: a prateleira de frente, com os níveis empilhados.
 * Nível 1 fica embaixo. Gôndolas no mesmo nível ficam lado a lado.
 */

/** Calcula o retângulo (em pixels) de cada gôndola. Usado para desenhar E para o toque. */
private fun Density.calcularLayout(
    largura: Float,
    altura: Float,
    gondolas: List<GondolaResumo>
): List<Pair<GondolaResumo, Rect>> {
    if (gondolas.isEmpty()) return emptyList()
    val coluna = 8.dp.toPx()       // largura das colunas laterais
    val tabua = 4.dp.toPx()        // espessura de cada tábua
    val folga = 4.dp.toPx()
    val niveis = gondolas.maxOf { it.gondola.nivel }.coerceAtLeast(1)
    val alturaNivel = altura / niveis
    val larguraUtil = largura - 2 * coluna

    return gondolas
        .groupBy { it.gondola.nivel }
        .flatMap { (nivel, doNivel) ->
            val ordenadas = doNivel.sortedBy { it.gondola.codigo }
            val larguraCada = larguraUtil / ordenadas.size
            val topo = altura - nivel * alturaNivel
            ordenadas.mapIndexed { i, g ->
                g to Rect(
                    left = coluna + i * larguraCada + folga,
                    top = topo + folga,
                    right = coluna + (i + 1) * larguraCada - folga,
                    bottom = topo + alturaNivel - tabua - folga / 2
                )
            }
        }
}

@Composable
fun VistaFrontalCanvas(
    gondolas: List<GondolaResumo>,
    onTocarGondola: (Long) -> Unit,
    modifier: Modifier = Modifier,
    destacadaId: Long? = null
) {
    val cores = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val gondolasAtuais by rememberUpdatedState(gondolas)
    val onTocarAtual by rememberUpdatedState(onTocarGondola)

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { toque ->
                calcularLayout(size.width.toFloat(), size.height.toFloat(), gondolasAtuais)
                    .firstOrNull { (_, ret) -> ret.contains(toque) }
                    ?.let { (g, _) -> onTocarAtual(g.gondola.id) }
            }
        }
    ) {
        val coluna = 8.dp.toPx()
        val tabua = 4.dp.toPx()
        val niveis = (gondolas.maxOfOrNull { it.gondola.nivel } ?: 1).coerceAtLeast(1)
        val alturaNivel = size.height / niveis

        // Colunas laterais da estrutura
        drawRect(cores.primary, topLeft = Offset.Zero, size = Size(coluna, size.height))
        drawRect(cores.primary, topLeft = Offset(size.width - coluna, 0f), size = Size(coluna, size.height))

        // Uma tábua na base de cada nível
        for (n in 1..niveis) {
            val y = size.height - (n - 1) * alturaNivel - tabua
            drawRect(cores.primary, topLeft = Offset(0f, y), size = Size(size.width, tabua))
        }

        // Gôndolas com código e quantidade de itens
        calcularLayout(size.width, size.height, gondolas).forEach { (g, ret) ->
            val destacada = g.gondola.id == destacadaId
            drawRoundRect(
                color = if (destacada) cores.secondary else cores.primaryContainer,
                topLeft = ret.topLeft,
                size = ret.size,
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            val corTexto = if (destacada) cores.onSecondary else cores.onPrimaryContainer
            val codigo = textMeasurer.measure(
                g.gondola.codigo,
                TextStyle(color = corTexto, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            val total = textMeasurer.measure(
                "${g.totalItens}",
                TextStyle(color = corTexto, fontSize = 10.sp)
            )
            val alturaTextos = codigo.size.height + total.size.height
            if (ret.height >= alturaTextos) {
                val y0 = ret.center.y - alturaTextos / 2f
                drawText(codigo, topLeft = Offset(ret.center.x - codigo.size.width / 2f, y0))
                drawText(total, topLeft = Offset(ret.center.x - total.size.width / 2f, y0 + codigo.size.height))
            } else {
                drawText(codigo, topLeft = Offset(ret.center.x - codigo.size.width / 2f, ret.center.y - codigo.size.height / 2f))
            }
        }
    }
}
