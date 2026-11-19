package com.stockai.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stockai.app.StockAiApp
import com.stockai.app.ui.screens.ambiente.AmbienteViewModel
import com.stockai.app.ui.screens.ambientes.AmbientesViewModel
import com.stockai.app.ui.screens.busca.BuscaViewModel
import com.stockai.app.ui.screens.cadastro.CadastroViewModel
import com.stockai.app.ui.screens.gondola.GondolaViewModel
import com.stockai.app.ui.screens.login.LoginViewModel
import com.stockai.app.ui.screens.prateleira.PrateleiraViewModel

/**
 * Ensina o Android a criar cada ViewModel, entregando o repositório certo.
 * A cada tela nova, acrescentamos um "initializer" aqui.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { LoginViewModel(stockAiApp().authRepository) }
        initializer { CadastroViewModel(stockAiApp().authRepository) }
        initializer {
            AmbientesViewModel(
                repo = stockAiApp().ambienteRepository,
                auth = stockAiApp().authRepository,
                exemplo = stockAiApp().dadosExemplo
            )
        }
        initializer {
            AmbienteViewModel(
                // SavedStateHandle traz o {ambienteId} (e os dados da busca) que vieram na rota
                savedStateHandle = createSavedStateHandle(),
                ambienteRepo = stockAiApp().ambienteRepository,
                prateleiraRepo = stockAiApp().prateleiraRepository,
                gondolaRepo = stockAiApp().gondolaRepository,
                itemRepo = stockAiApp().itemRepository
            )
        }
        initializer {
            PrateleiraViewModel(
                savedStateHandle = createSavedStateHandle(),
                prateleiraRepo = stockAiApp().prateleiraRepository,
                gondolaRepo = stockAiApp().gondolaRepository
            )
        }
        initializer {
            GondolaViewModel(
                savedStateHandle = createSavedStateHandle(),
                gondolaRepo = stockAiApp().gondolaRepository,
                itemRepo = stockAiApp().itemRepository
            )
        }
        initializer {
            BuscaViewModel(
                itemRepo = stockAiApp().itemRepository,
                recentesRepo = stockAiApp().buscasRecentesRepository,
                auth = stockAiApp().authRepository
            )
        }
    }
}

fun CreationExtras.stockAiApp(): StockAiApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StockAiApp
