package com.stockai.app.data.repository

import com.stockai.app.data.local.Item
import com.stockai.app.data.local.ItemDao
import com.stockai.app.data.local.ResultadoBusca
import kotlinx.coroutines.flow.Flow

/** Intermediário entre as telas e o banco para tudo que é item */
class ItemRepository(private val dao: ItemDao) {

    fun observarPorGondola(gondolaId: Long): Flow<List<Item>> = dao.observarPorGondola(gondolaId)

    fun observarPorId(id: Long): Flow<Item?> = dao.observarPorId(id)

    /** NOVO na Fase 7: busca em todos os ambientes do usuário */
    fun buscar(usuarioId: Long, termo: String): Flow<List<ResultadoBusca>> =
        dao.buscar(usuarioId, termo)

    suspend fun criar(item: Item): Long = dao.inserir(item)

    /** Sempre que um item muda, a data de "atualizado em" é renovada */
    suspend fun atualizar(item: Item) =
        dao.atualizar(item.copy(atualizadoEm = System.currentTimeMillis()))

    /** Botões + e −: soma (ou subtrai) da quantidade, sem deixar ficar negativa */
    suspend fun ajustarQuantidade(item: Item, delta: Int) =
        atualizar(item.copy(quantidade = (item.quantidade + delta).coerceAtLeast(0)))

    suspend fun apagar(item: Item) = dao.apagar(item)
}
