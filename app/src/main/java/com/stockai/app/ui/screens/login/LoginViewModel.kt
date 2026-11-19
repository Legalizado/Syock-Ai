package com.stockai.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockai.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tudo que a tela de login precisa mostrar */
data class LoginUiState(
    val email: String = "",
    val senha: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val sucesso: Boolean = false
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) = _uiState.update { it.copy(email = valor, erro = null) }

    fun onSenhaChange(valor: String) = _uiState.update { it.copy(senha = valor, erro = null) }

    fun entrar() {
        val estado = _uiState.value
        if (estado.email.isBlank() || estado.senha.isBlank()) {
            _uiState.update { it.copy(erro = "Preencha e-mail e senha") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(carregando = true, erro = null) }
            auth.entrar(estado.email, estado.senha)
                .onSuccess { _uiState.update { it.copy(carregando = false, sucesso = true) } }
                .onFailure { e -> _uiState.update { it.copy(carregando = false, erro = e.message) } }
        }
    }
}
