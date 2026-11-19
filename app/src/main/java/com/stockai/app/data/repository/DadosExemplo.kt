package com.stockai.app.data.repository

import androidx.room.withTransaction
import com.stockai.app.data.local.Ambiente
import com.stockai.app.data.local.Gondola
import com.stockai.app.data.local.Item
import com.stockai.app.data.local.Prateleira
import com.stockai.app.data.local.StockAiDatabase

/**
 * Cria um almoxarifado completo de exemplo, igual ao do protótipo do banner.
 * Útil para demonstrar o app sem cadastrar tudo na mão.
 */
class DadosExemplo(private val db: StockAiDatabase) {

    /** Modelos simples só para deixar a lista de dados fácil de ler */
    private data class G(val codigo: String, val nome: String, val nivel: Int, val itens: List<Pair<String, Int>>)
    private data class P(
        val nome: String, val x: Float, val y: Float,
        val largura: Float, val rotacao: Int, val gondolas: List<G>
    )

    private val prateleiras = listOf(
        P("Prateleira A", 1f, 1f, 3f, 0, listOf(
            G("A1", "Fitas e adesivos", 1, listOf("Fita isolante" to 24, "Fita crepe 18 mm" to 18)),
            G("A2", "Abraçadeiras", 2, listOf("Abraçadeira de nylon 20 cm" to 100)),
            G("A3", "Material elétrico", 3, listOf("Interruptor simples" to 15, "Tomada 10 A" to 22))
        )),
        P("Prateleira B", 1f, 3f, 3f, 0, listOf(
            G("B1", "Buchas", 1, listOf("Bucha 6 mm" to 200, "Bucha 8 mm" to 150)),
            G("B2", "Porcas e arruelas", 2, listOf("Porca sextavada M8" to 300, "Arruela lisa 8 mm" to 250)),
            G("B3", "Parafusos", 3, listOf(
                "Parafuso M8 × 30 mm" to 120,
                "Parafuso sextavado 1/4\"" to 45,
                "Parafuso M6 × 20 mm" to 80
            )),
            G("B4", "Ferramentas manuais", 4, listOf("Chave de fenda 1/4\"" to 6, "Alicate universal" to 3, "Martelo" to 2))
        )),
        P("Prateleira C", 1f, 6f, 4f, 0, listOf(
            G("C1", "EPIs", 1, listOf("Luva de vaqueta (par)" to 12, "Óculos de proteção" to 8)),
            G("C2", "Limpeza", 2, listOf("Pano multiuso" to 40))
        )),
        P("Prateleira D", 7f, 1f, 4f, 90, listOf(
            G("D1", "Tintas", 1, listOf("Lata de tinta 18 L" to 4)),
            G("D2", "Pincéis e rolos", 2, listOf("Rolo de lã 23 cm" to 6, "Pincel 2\"" to 10))
        )),
        P("Prateleira E", 9.5f, 1f, 4f, 90, listOf(
            G("E1", "Embalagens", 1, listOf("Caixa de papelão M" to 50, "Plástico bolha (m)" to 30))
        ))
    )

    /** Cria tudo de uma vez, numa transação: ou entra tudo, ou nada */
    suspend fun criar(usuarioId: Long): Long = db.withTransaction {
        val ambienteId = db.ambienteDao().inserir(
            Ambiente(usuarioId = usuarioId, nome = "Almoxarifado (exemplo)", largura = 12f, comprimento = 8f)
        )
        prateleiras.forEach { p ->
            val prateleiraId = db.prateleiraDao().inserir(
                Prateleira(
                    ambienteId = ambienteId, nome = p.nome,
                    posX = p.x, posY = p.y,
                    largura = p.largura, profundidade = 0.6f, altura = 2f,
                    rotacao = p.rotacao
                )
            )
            p.gondolas.forEach { g ->
                val gondolaId = db.gondolaDao().inserir(
                    Gondola(prateleiraId = prateleiraId, codigo = g.codigo, nome = g.nome, nivel = g.nivel)
                )
                g.itens.forEach { (nome, qtd) ->
                    db.itemDao().inserir(Item(gondolaId = gondolaId, nome = nome, quantidade = qtd))
                }
            }
        }
        ambienteId
    }
}
