package com.stockai.app

import android.app.Application
import com.stockai.app.data.local.StockAiDatabase
import com.stockai.app.data.repository.AmbienteRepository
import com.stockai.app.data.repository.AuthRepository
import com.stockai.app.data.repository.BuscasRecentesRepository
import com.stockai.app.data.repository.DadosExemplo
import com.stockai.app.data.repository.GondolaRepository
import com.stockai.app.data.repository.ItemRepository
import com.stockai.app.data.repository.PrateleiraRepository

/**
 * Classe que representa o app inteiro. É criada antes de qualquer tela
 * e serve como "container" das dependências.
 */
class StockAiApp : Application() {
    val database: StockAiDatabase by lazy { StockAiDatabase.getInstance(this) }
    val authRepository: AuthRepository by lazy { AuthRepository(database.usuarioDao(), this) }
    val ambienteRepository: AmbienteRepository by lazy { AmbienteRepository(database.ambienteDao()) }
    val prateleiraRepository: PrateleiraRepository by lazy { PrateleiraRepository(database.prateleiraDao()) }
    val gondolaRepository: GondolaRepository by lazy { GondolaRepository(database.gondolaDao()) }
    val itemRepository: ItemRepository by lazy { ItemRepository(database.itemDao()) }
    val buscasRecentesRepository: BuscasRecentesRepository by lazy { BuscasRecentesRepository(this) }
    val dadosExemplo: DadosExemplo by lazy { DadosExemplo(database) }
}
