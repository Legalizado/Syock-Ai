package com.stockai.app.ui.screens.ambiente

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.stockai.app.data.local.Ambiente
import com.stockai.app.data.local.GondolaResumo
import com.stockai.app.data.local.Prateleira
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import kotlin.math.max

/*
 * Visualização 3D do ambiente com SceneView (motor Filament).
 *
 * Sistema de coordenadas (em METROS, igual ao banco):
 *   x → largura do ambiente (esquerda/direita)
 *   y → altura (para cima)
 *   z → comprimento do ambiente (fundo/frente)
 * O centro do piso fica na origem (0, 0, 0).
 *
 * Tudo é montado com cubos (CubeNode): piso, muretas, colunas,
 * tábuas e caixas. Não precisa de nenhum modelo 3D externo.
 */

/** Cores dos objetos 3D, criadas uma vez só */
private class Materiais3D(loader: MaterialLoader) {
    val piso = loader.createColorInstance(Color(0xFFE6ECEE), roughness = 0.9f)
    val parede = loader.createColorInstance(Color(0xFF0F4C5C), roughness = 0.7f)
    val estrutura = loader.createColorInstance(Color(0xFF0F4C5C), metallic = 0.4f, roughness = 0.5f)
    val tabua = loader.createColorInstance(Color(0xFF7FA7B1), roughness = 0.7f)
    val caixa = loader.createColorInstance(Color(0xFFC9A26B), roughness = 0.8f)
    val destaque = loader.createColorInstance(Color(0xFFF5A800), roughness = 0.5f)
    val marcador = loader.createColorInstance(Color(0xFFD32F2F), roughness = 0.4f)
}

/**
 * O Filament precisa de OpenGL ES 3.0. Se o aparelho (ou emulador) não tiver,
 * o motor fecha o app na hora — e isso não dá para capturar com try/catch,
 * porque o erro acontece no código nativo. Então conferimos ANTES de abrir.
 */
private fun suportaOpenGles3(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.deviceConfigurationInfo.reqGlEsVersion >= 0x30000
}

@Composable
fun Visualizacao3D(
    ambiente: Ambiente,
    prateleiras: List<Prateleira>,
    gondolas: List<GondolaResumo>,
    selecionadaId: Long?,
    gondolaDestacadaId: Long?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val suporta = remember { suportaOpenGles3(context) }

    if (suporta) {
        Cena3D(ambiente, prateleiras, gondolas, selecionadaId, gondolaDestacadaId, modifier)
    } else {
        Column(
            modifier = modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.ViewInAr,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Text(
                "Visualização 3D indisponível neste aparelho",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "O 3D precisa de OpenGL ES 3.0. A planta 2D continua funcionando normalmente.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Cena3D(
    ambiente: Ambiente,
    prateleiras: List<Prateleira>,
    gondolas: List<GondolaResumo>,
    selecionadaId: Long?,
    gondolaDestacadaId: Long?,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val materiais = remember(materialLoader) { Materiais3D(materialLoader) }

    // Câmera: começa no alto, na diagonal, olhando para o centro do ambiente
    val maiorLado = max(ambiente.largura, ambiente.comprimento)
    val casaX = maiorLado * 0.55f
    val casaY = maiorLado * 0.75f
    val casaZ = maiorLado * 0.95f

    val cameraNode = remember(engine) { SceneView.createCameraNode(engine) }
    DisposableEffect(cameraNode) { onDispose { cameraNode.destroy() } }
    LaunchedEffect(cameraNode, casaX, casaY, casaZ) {
        cameraNode.position = Position(x = casaX, y = casaY, z = casaZ)
        cameraNode.lookAt(Position(x = 0f, y = 0f, z = 0f))
    }

    // Gesto de órbita: arrastar gira em volta do centro, pinça aproxima
    val manipulador = remember(casaX, casaY, casaZ) {
        CameraGestureDetector.DefaultCameraManipulator(
            orbitHomePosition = Position(x = casaX, y = casaY, z = casaZ),
            targetPosition = Position(x = 0f, y = 0f, z = 0f)
        )
    }

    // Um nó "raiz" fixo; os cubos entram e saem como filhos dele
    val raiz = remember(engine) { Node(engine) }
    DisposableEffect(raiz) { onDispose { raiz.destroy() } }
    val nosDaCena = remember(raiz) { listOf(raiz) }

    // Sempre que os dados mudam, a cena é remontada (e os cubos antigos destruídos)
    DisposableEffect(raiz, ambiente, prateleiras, gondolas, selecionadaId, gondolaDestacadaId) {
        val cubos = montarCena(
            engine = engine,
            m = materiais,
            ambiente = ambiente,
            prateleiras = prateleiras,
            gondolas = gondolas,
            selecionadaId = selecionadaId,
            gondolaDestacadaId = gondolaDestacadaId
        )
        cubos.forEach { raiz.addChildNode(it) }
        onDispose { cubos.forEach { it.destroy() } }
    }

    Scene(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        isOpaque = false, // fundo transparente: aparece a cor do tema por trás
        cameraNode = cameraNode,
        cameraManipulator = manipulador,
        childNodes = nosDaCena
    )
}

private fun montarCena(
    engine: Engine,
    m: Materiais3D,
    ambiente: Ambiente,
    prateleiras: List<Prateleira>,
    gondolas: List<GondolaResumo>,
    selecionadaId: Long?,
    gondolaDestacadaId: Long?
): List<Node> {
    val nos = mutableListOf<Node>()
    val larguraAmb = ambiente.largura
    val compAmb = ambiente.comprimento

    fun cubo(tamanho: Size, centro: Position, material: MaterialInstance) {
        nos += CubeNode(engine, size = tamanho, center = centro, materialInstance = material)
    }

    // ---------- Piso e muretas (12 cm de altura, só para marcar as paredes) ----------
    cubo(Size(larguraAmb, 0.04f, compAmb), Position(0f, -0.02f, 0f), m.piso)
    val hMureta = 0.12f
    val esp = 0.05f
    cubo(Size(larguraAmb + 2 * esp, hMureta, esp), Position(0f, hMureta / 2, -compAmb / 2 - esp / 2), m.parede)
    cubo(Size(larguraAmb + 2 * esp, hMureta, esp), Position(0f, hMureta / 2, compAmb / 2 + esp / 2), m.parede)
    cubo(Size(esp, hMureta, compAmb), Position(-larguraAmb / 2 - esp / 2, hMureta / 2, 0f), m.parede)
    cubo(Size(esp, hMureta, compAmb), Position(larguraAmb / 2 + esp / 2, hMureta / 2, 0f), m.parede)

    // ---------- Prateleiras ----------
    val gondolasPorPrateleira = gondolas.groupBy { it.gondola.prateleiraId }

    prateleiras.forEach { p ->
        val girada = p.rotacao == 90
        val comprimento = p.largura      // eixo maior da prateleira
        val profundidade = p.profundidade
        val altura = p.altura

        // Centro da prateleira no mundo (a planta usa o canto; aqui usamos o centro)
        val cx = -larguraAmb / 2 + p.posX + p.pegadaX / 2
        val cz = -compAmb / 2 + p.posY + p.pegadaY / 2

        // Converte coordenadas "da prateleira" (ao longo / através) para o mundo,
        // levando em conta a rotação de 90°
        fun noMundo(aoLongo: Float, atraves: Float, y: Float) =
            if (!girada) Position(cx + aoLongo, y, cz + atraves) else Position(cx + atraves, y, cz + aoLongo)

        fun tamanho(aoLongo: Float, alturaCubo: Float, atraves: Float) =
            if (!girada) Size(aoLongo, alturaCubo, atraves) else Size(atraves, alturaCubo, aoLongo)

        val matEstrutura = if (p.id == selecionadaId) m.destaque else m.estrutura
        val coluna = 0.05f
        val tabua = 0.03f
        val folgaChao = 0.06f

        // 4 colunas nos cantos
        for (lado in listOf(-1f, 1f)) {
            for (frente in listOf(-1f, 1f)) {
                cubo(
                    tamanho(coluna, altura, coluna),
                    noMundo(lado * (comprimento / 2 - coluna / 2), frente * (profundidade / 2 - coluna / 2), altura / 2),
                    matEstrutura
                )
            }
        }

        val daPrateleira = gondolasPorPrateleira[p.id].orEmpty()
        val niveis = (daPrateleira.maxOfOrNull { it.gondola.nivel } ?: 3).coerceAtLeast(1)
        val alturaNivel = (altura - folgaChao) / niveis

        // Uma tábua na base de cada nível + uma no topo
        for (n in 0 until niveis) {
            val y = folgaChao + n * alturaNivel + tabua / 2
            cubo(tamanho(comprimento, tabua, profundidade), noMundo(0f, 0f, y), m.tabua)
        }
        cubo(tamanho(comprimento, tabua, profundidade), noMundo(0f, 0f, altura - tabua / 2), m.tabua)

        // Caixas: uma por gôndola que tem itens, no nível dela
        daPrateleira.groupBy { it.gondola.nivel }.forEach { (nivel, doNivel) ->
            val ordenadas = doNivel.sortedBy { it.gondola.codigo }
            val larguraUtil = comprimento - 2 * coluna
            val larguraCada = larguraUtil / ordenadas.size
            val alturaCaixa = alturaNivel * 0.55f
            val baseNivel = folgaChao + (nivel - 1) * alturaNivel + tabua

            ordenadas.forEachIndexed { i, g ->
                val destacada = g.gondola.id == gondolaDestacadaId
                if (g.totalItens == 0 && !destacada) return@forEachIndexed

                val aoLongo = -comprimento / 2 + coluna + larguraCada * (i + 0.5f)
                cubo(
                    tamanho(larguraCada * 0.8f, alturaCaixa, profundidade * 0.75f),
                    noMundo(aoLongo, 0f, baseNivel + alturaCaixa / 2),
                    if (destacada) m.destaque else m.caixa
                )

                // Marcador vermelho flutuando acima da prateleira, como um "pino" de mapa
                if (destacada) {
                    cubo(tamanho(0.02f, 0.25f, 0.02f), noMundo(aoLongo, 0f, altura + 0.125f), m.marcador)
                    cubo(tamanho(0.14f, 0.14f, 0.14f), noMundo(aoLongo, 0f, altura + 0.32f), m.marcador)
                }
            }
        }
    }

    return nos
}
