package com.stockai.app.data.repository

import com.stockai.app.data.local.Gondola
import com.stockai.app.data.local.GondolaDao
import com.stockai.app.data.local.GondolaResumo
import kotlinx.coroutines.flow.Flow

/** Intermediário entre as telas e o banco para tudo que é gôndola */
class GondolaRepository(private val dao: GondolaDao) {

    fun observarResumos(prateleiraId: Long): Flow<List<GondolaResumo>> =
        dao.observarResumos(prateleiraId)

    fun observarPorId(id: Long): Flow<Gondola?> = dao.observarPorId(id)

    /** NOVO na Fase 8: gôndolas de todas as prateleiras de um ambiente */
    fun observarResumosPorAmbiente(ambienteId: Long): Flow<List<GondolaResumo>> =
        dao.observarResumosPorAmbiente(ambienteId)

    suspend fun criar(gondola: Gondola): Long = dao.inserir(gondola)

    suspend fun atualizar(gondola: Gondola) = dao.atualizar(gondola)

    suspend fun apagar(gondola: Gondola) = dao.apagar(gondola)
}