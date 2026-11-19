package com.stockai.app.data.repository

import com.stockai.app.data.local.Prateleira
import com.stockai.app.data.local.PrateleiraDao
import kotlinx.coroutines.flow.Flow

/** Intermediário entre as telas e o banco para tudo que é prateleira */
class PrateleiraRepository(private val dao: PrateleiraDao) {

    fun observarPorAmbiente(ambienteId: Long): Flow<List<Prateleira>> =
        dao.observarPorAmbiente(ambienteId)

    fun observarPorId(id: Long): Flow<Prateleira?> = dao.observarPorId(id)

    suspend fun criar(prateleira: Prateleira): Long = dao.inserir(prateleira)

    suspend fun atualizar(prateleira: Prateleira) = dao.atualizar(prateleira)

    suspend fun apagar(prateleira: Prateleira) = dao.apagar(prateleira)
}
