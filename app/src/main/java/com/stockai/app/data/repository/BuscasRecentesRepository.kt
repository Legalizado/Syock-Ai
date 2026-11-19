package com.stockai.app.data.repository

import android.content.Context

/** Guarda as últimas buscas de cada usuário (as "Buscas recentes" do protótipo) */
class BuscasRecentesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("stockai_buscas", Context.MODE_PRIVATE)

    private fun chave(usuarioId: Long) = "recentes_$usuarioId"

    fun listar(usuarioId: Long): List<String> =
        prefs.getString(chave(usuarioId), null)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    /** Coloca o termo no começo da lista, sem repetir, guardando no máximo 6 */
    fun adicionar(usuarioId: Long, termo: String): List<String> {
        val limpo = termo.trim()
        if (limpo.isEmpty()) return listar(usuarioId)
        val nova = (listOf(limpo) + listar(usuarioId).filterNot { it.equals(limpo, ignoreCase = true) })
            .take(MAXIMO)
        prefs.edit().putString(chave(usuarioId), nova.joinToString("\n")).apply()
        return nova
    }

    fun limpar(usuarioId: Long) {
        prefs.edit().remove(chave(usuarioId)).apply()
    }

    private companion object {
        const val MAXIMO = 6
    }
}
