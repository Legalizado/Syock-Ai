package com.stockai.app.data.local

import androidx.room.Embedded

/*
 * Classes que NÃO são tabelas: são o "formato" do resultado
 * de consultas que juntam informações de várias tabelas.
 */

/** Card da tela "Meus ambientes": "12 × 8 m · 5 prateleiras · 438 itens" */
data class AmbienteResumo(
    @Embedded val ambiente: Ambiente,
    val totalPrateleiras: Int,
    val totalItens: Int
)

/** Linha da lista de gôndolas de uma prateleira: "B1 Buchas 36" */
data class GondolaResumo(
    @Embedded val gondola: Gondola,
    val totalItens: Int
)

/** Resultado da busca: o item e o "caminho" até ele */
data class ResultadoBusca(
    val itemId: Long,
    val itemNome: String,
    val quantidade: Int,
    val unidade: String,
    val gondolaId: Long,
    val gondolaCodigo: String,
    val gondolaNome: String,
    val prateleiraId: Long,
    val prateleiraNome: String,
    val ambienteId: Long,
    val ambienteNome: String
) {
    /** "Almoxarifado › Prateleira B › Gôndola B3" */
    val caminho: String
        get() = "$ambienteNome › $prateleiraNome › Gôndola $gondolaCodigo"
}
