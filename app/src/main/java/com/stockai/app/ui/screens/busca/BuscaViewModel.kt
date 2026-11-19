package com.stockai.app.ui.screens.busca

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockai.app.data.local.ResultadoBusca
import com.stockai.app.data.repository.AuthRepository
import com.stockai.app.data.repository.BuscasRecentesRepository
import com.stockai.app.data.repository.ItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BuscaViewModel(
    private val itemRepo: ItemRepository,
    private val recentesRepo: BuscasRecentesRepository,
    auth: AuthRepository
) : ViewModel() {

    private val usuarioId: Long = auth.usuarioLogadoId ?: -1L

    private val _termo = MutableStateFlow("")
    val termo: StateFlow<String> = _termo.asStateFlow()

    /*
     * A busca acontece enquanto o usuário digita:
     * - debounce(250): espera 250 ms sem digitar antes de consultar (evita
     *   uma consulta a cada letra)
     * - flatMapLatest: se o termo mudar, cancela a busca antiga e faz a nova
     * - null = campo vazio (mostra as buscas recentes em vez de resultados)
     */
    val resultados: StateFlow<List<ResultadoBusca>?> =
        _termo
            .debounce(250)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { t ->
                if (t.isEmpty()) flowOf(null) else itemRepo.buscar(usuarioId, t)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _recentes = MutableStateFlow(recentesRepo.listar(usuarioId))
    val recentes: StateFlow<List<String>> = _recentes.asStateFlow()

    fun onTermoChange(valor: String) {
        _termo.value = valor
    }

    /** Toque numa busca recente: preenche o campo */
    fun usarRecente(termo: String) {
        _termo.value = termo
    }

    /** Guarda o termo atual nas recentes (ao pesquisar ou abrir um resultado) */
    fun registrarBusca() {
        _recentes.value = recentesRepo.adicionar(usuarioId, _termo.value)
    }

    fun limparRecentes() {
        recentesRepo.limpar(usuarioId)
        _recentes.value = emptyList()
    }
}
