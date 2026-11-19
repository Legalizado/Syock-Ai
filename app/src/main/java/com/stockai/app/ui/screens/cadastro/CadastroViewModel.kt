package com.stockai.app.ui.screens.cadastro

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockai.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CadastroUiState(
    val nome: String = "",
    val email: String = "",
    val senha: String = "",
    val confirmacao: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val sucesso: Boolean = false
)

class CadastroViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CadastroUiState())
    val uiState: StateFlow<CadastroUiState> = _uiState.asStateFlow()

    fun onNomeChange(valor: String) = _uiState.update { it.copy(nome = valor, erro = null) }
    fun onEmailChange(valor: String) = _uiState.update { it.copy(email = valor, erro = null) }
    fun onSenhaChange(valor: String) = _uiState.update { it.copy(senha = valor, erro = null) }
    fun onConfirmacaoChange(valor: String) = _uiState.update { it.copy(confirmacao = valor, erro = null) }

    fun criarConta() {
        val estado = _uiState.value

        val erroValidacao = when {
            estado.nome.isBlank() -> "Informe seu nome"
            !Patterns.EMAIL_ADDRESS.matcher(estado.email.trim()).matches() -> "E-mail inválido"
            estado.senha.length < 6 -> "A senha precisa ter pelo menos 6 caracteres"
            estado.senha != estado.confirmacao -> "As senhas não conferem"
            else -> null
        }
        if (erroValidacao != null) {
            _uiState.update { it.copy(erro = erroValidacao) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(carregando = true, erro = null) }
            auth.cadastrar(estado.nome, estado.email, estado.senha)
                .onSuccess { _uiState.update { it.copy(carregando = false, sucesso = true) } }
                .onFailure { e -> _uiState.update { it.copy(carregando = false, erro = e.message) } }
        }
    }
}
