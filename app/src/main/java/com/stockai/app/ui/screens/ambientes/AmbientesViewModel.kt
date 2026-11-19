package com.stockai.app.ui.screens.ambientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockai.app.data.local.Ambiente
import com.stockai.app.data.local.AmbienteResumo
import com.stockai.app.data.repository.AmbienteRepository
import com.stockai.app.data.repository.AuthRepository
import com.stockai.app.data.repository.DadosExemplo
import com.stockai.app.ui.components.formatarMetros
import com.stockai.app.ui.components.lerDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Dados do diálogo de criar/editar. original == null → criando um novo */
data class FormAmbiente(
    val original: Ambiente? = null,
    val nome: String = "",
    val largura: String = "",
    val comprimento: String = "",
    val erro: String? = null
)

class AmbientesViewModel(
    private val repo: AmbienteRepository,
    auth: AuthRepository,
    private val exemplo: DadosExemplo
) : ViewModel() {

    private val usuarioId: Long = auth.usuarioLogadoId ?: -1L

    /** null = ainda carregando; lista vazia = usuário sem ambientes */
    val ambientes: StateFlow<List<AmbienteResumo>?> =
        repo.observarResumos(usuarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _form = MutableStateFlow<FormAmbiente?>(null) // null = diálogo fechado
    val form: StateFlow<FormAmbiente?> = _form.asStateFlow()

    private val _paraExcluir = MutableStateFlow<Ambiente?>(null)
    val paraExcluir: StateFlow<Ambiente?> = _paraExcluir.asStateFlow()

    // ---------- Criar / editar ----------

    fun novo() {
        _form.value = FormAmbiente()
    }

    fun editar(ambiente: Ambiente) {
        _form.value = FormAmbiente(
            original = ambiente,
            nome = ambiente.nome,
            largura = formatarMetros(ambiente.largura),
            comprimento = formatarMetros(ambiente.comprimento)
        )
    }

    fun fecharForm() {
        _form.value = null
    }

    fun onNomeChange(valor: String) = _form.update { it?.copy(nome = valor, erro = null) }
    fun onLarguraChange(valor: String) = _form.update { it?.copy(largura = valor, erro = null) }
    fun onComprimentoChange(valor: String) = _form.update { it?.copy(comprimento = valor, erro = null) }

    fun salvar() {
        val f = _form.value ?: return
        val largura = lerDecimal(f.largura)
        val comprimento = lerDecimal(f.comprimento)

        if (f.nome.isBlank()) return mostrarErro("Informe o nome do ambiente")
        if (largura == null || largura <= 0f) return mostrarErro("Largura inválida")
        if (comprimento == null || comprimento <= 0f) return mostrarErro("Comprimento inválido")
        if (largura > 500f || comprimento > 500f) return mostrarErro("Máximo de 500 m por lado")

        viewModelScope.launch {
            val original = f.original
            if (original == null) {
                repo.criar(usuarioId, f.nome.trim(), largura, comprimento)
            } else {
                repo.atualizar(
                    original.copy(nome = f.nome.trim(), largura = largura, comprimento = comprimento)
                )
            }
            _form.value = null
        }
    }

    private fun mostrarErro(mensagem: String) {
        _form.update { it?.copy(erro = mensagem) }
    }

    // ---------- Dados de demonstração (Fase 9) ----------

    private val _criandoExemplo = MutableStateFlow(false)
    val criandoExemplo: StateFlow<Boolean> = _criandoExemplo.asStateFlow()

    fun criarExemplo() {
        if (_criandoExemplo.value) return // evita criar dois por toque duplo
        viewModelScope.launch {
            _criandoExemplo.value = true
            exemplo.criar(usuarioId)
            _criandoExemplo.value = false
        }
    }

    // ---------- Excluir (com confirmação) ----------

    fun pedirExclusao(ambiente: Ambiente) {
        _paraExcluir.value = ambiente
    }

    fun cancelarExclusao() {
        _paraExcluir.value = null
    }

    fun confirmarExclusao() {
        val ambiente = _paraExcluir.value ?: return
        viewModelScope.launch {
            repo.apagar(ambiente) // CASCADE apaga prateleiras, gôndolas e itens junto
            _paraExcluir.value = null
        }
    }
}
