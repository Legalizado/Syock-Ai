package com.stockai.app.data.repository

import com.stockai.app.data.local.Ambiente
import com.stockai.app.data.local.AmbienteDao
import com.stockai.app.data.local.AmbienteResumo
import kotlinx.coroutines.flow.Flow

/** Intermediário entre as telas e o banco para tudo que é ambiente */
class AmbienteRepository(private val dao: AmbienteDao) {

    fun observarResumos(usuarioId: Long): Flow<List<AmbienteResumo>> =
        dao.observarResumos(usuarioId)

    fun observarPorId(id: Long): Flow<Ambiente?> = dao.observarPorId(id)

    suspend fun criar(usuarioId: Long, nome: String, largura: Float, comprimento: Float): Long =
        dao.inserir(
            Ambiente(
                usuarioId = usuarioId,
                nome = nome,
                largura = largura,
                comprimento = comprimento
            )
        )

    suspend fun atualizar(ambiente: Ambiente) = dao.atualizar(ambiente)

    suspend fun apagar(ambiente: Ambiente) = dao.apagar(ambiente)
}
