package com.stockai.app.ui.screens.prateleira

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockai.app.data.local.Gondola
import com.stockai.app.data.local.GondolaResumo
import com.stockai.app.data.local.Prateleira
import com.stockai.app.data.repository.GondolaRepository
import com.stockai.app.data.repository.PrateleiraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormGondola(
    val original: Gondola? = null,
    val codigo: String = "",
    val nome: String = "",
    val nivel: String = "",
    val erro: String? = null
)

class PrateleiraViewModel(
    savedStateHandle: SavedStateHandle,
    prateleiraRepo: PrateleiraRepository,
    private val gondolaRepo: GondolaRepository
) : ViewModel() {

    private val prateleiraId: Long = checkNotNull(savedStateHandle["prateleiraId"])

    /** Gôndola para destacar em âmbar quando a tela é aberta a partir da busca */
    val gondolaDestacadaId: Long? = savedStateHandle.get<Long>("gondolaId")?.takeIf { it > 0 }

    val prateleira: StateFlow<Prateleira?> =
        prateleiraRepo.observarPorId(prateleiraId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val gondolas: StateFlow<List<GondolaResumo>> =
        gondolaRepo.observarResumos(prateleiraId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow<FormGondola?>(null)
    val form: StateFlow<FormGondola?> = _form.asStateFlow()

    private val _paraExcluir = MutableStateFlow<Gondola?>(null)
    val paraExcluir: StateFlow<Gondola?> = _paraExcluir.asStateFlow()

    /** Letra da prateleira: "Prateleira B" → 'B' (usada nos códigos B1, B2...) */
    private fun letraDaPrateleira(): Char =
        prateleira.value?.nome?.removePrefix("Prateleira ")?.trim()
            ?.firstOrNull()?.uppercaseChar() ?: 'G'

    fun novaGondola() {
        // Próximo nível = um acima do mais alto que já existe
        val proximoNivel = (gondolas.value.maxOfOrNull { it.gondola.nivel } ?: 0) + 1
        _form.value = FormGondola(
            codigo = "${letraDaPrateleira()}$proximoNivel",
            nivel = proximoNivel.toString()
        )
    }

    fun editar(g: Gondola) {
        _form.value = FormGondola(
            original = g,
            codigo = g.codigo,
            nome = g.nome,
            nivel = g.nivel.toString()
        )
    }

    fun atualizarForm(novo: FormGondola) {
        _form.value = novo.copy(erro = null)
    }

    fun fecharForm() {
        _form.value = null
    }

    fun salvar() {
        val f = _form.value ?: return
        val codigo = f.codigo.trim().uppercase()
        val nivel = f.nivel.trim().toIntOrNull()

        if (codigo.isBlank()) return mostrarErro("Informe o código (ex.: B1)")
        if (f.nome.isBlank()) return mostrarErro("Informe o nome (ex.: Parafusos)")
        if (nivel == null || nivel !in 1..20) return mostrarErro("O nível deve ser de 1 a 20")

        val repetido = gondolas.value.any {
            it.gondola.codigo.equals(codigo, ignoreCase = true) && it.gondola.id != f.original?.id
        }
        if (repetido) return mostrarErro("Já existe a gôndola $codigo nesta prateleira")

        viewModelScope.launch {
            val original = f.original
            if (original == null) {
                gondolaRepo.criar(
                    Gondola(prateleiraId = prateleiraId, codigo = codigo, nome = f.nome.trim(), nivel = nivel)
                )
            } else {
                gondolaRepo.atualizar(original.copy(codigo = codigo, nome = f.nome.trim(), nivel = nivel))
            }
            _form.value = null
        }
    }

    private fun mostrarErro(mensagem: String) {
        _form.update { it?.copy(erro = mensagem) }
    }

    fun pedirExclusao(g: Gondola) {
        _paraExcluir.value = g
    }

    fun cancelarExclusao() {
        _paraExcluir.value = null
    }

    fun confirmarExclusao() {
        val g = _paraExcluir.value ?: return
        viewModelScope.launch {
            gondolaRepo.apagar(g)
            _paraExcluir.value = null
        }
    }
}
