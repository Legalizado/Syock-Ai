package com.stockai.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stockai.app.StockAiApp
import com.stockai.app.ui.screens.ambiente.AmbienteScreen
import com.stockai.app.ui.screens.ambientes.AmbientesScreen
import com.stockai.app.ui.screens.busca.BuscaScreen
import com.stockai.app.ui.screens.cadastro.CadastroScreen
import com.stockai.app.ui.screens.gondola.GondolaScreen
import com.stockai.app.ui.screens.login.LoginScreen
import com.stockai.app.ui.screens.prateleira.PrateleiraScreen

/**
 * Nome de cada tela (rota).
 * O que vem depois de "?" são parâmetros OPCIONAIS: usados quando a tela
 * é aberta pela busca, para saber o que destacar. -1 = não destacar nada.
 */
object Rotas {
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val AMBIENTES = "ambientes"
    const val BUSCA = "busca"
    const val AMBIENTE = "ambiente/{ambienteId}?prateleiraId={prateleiraId}&gondolaId={gondolaId}&itemId={itemId}"
    const val PRATELEIRA = "prateleira/{prateleiraId}?gondolaId={gondolaId}"
    const val GONDOLA = "gondola/{gondolaId}"

    fun ambiente(id: Long, prateleiraId: Long = -1, gondolaId: Long = -1, itemId: Long = -1) =
        "ambiente/$id?prateleiraId=$prateleiraId&gondolaId=$gondolaId&itemId=$itemId"

    fun prateleira(id: Long, gondolaId: Long = -1) = "prateleira/$id?gondolaId=$gondolaId"

    fun gondola(id: Long) = "gondola/$id"
}

/** Parâmetro opcional do tipo Long, com -1 quando não informado */
private fun argOpcional(nome: String) = navArgument(nome) {
    type = NavType.LongType
    defaultValue = -1L
}

@Composable
fun StockAiNavHost(navController: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as StockAiApp

    // Se já tem alguém logado, abre direto nos ambientes
    val telaInicial = remember {
        if (app.authRepository.usuarioLogadoId != null) Rotas.AMBIENTES else Rotas.LOGIN
    }

    NavHost(navController = navController, startDestination = telaInicial) {

        composable(Rotas.LOGIN) {
            LoginScreen(
                onLoginOk = {
                    navController.navigate(Rotas.AMBIENTES) {
                        popUpTo(Rotas.LOGIN) { inclusive = true }
                    }
                },
                onCriarConta = { navController.navigate(Rotas.CADASTRO) }
            )
        }

        composable(Rotas.CADASTRO) {
            CadastroScreen(
                onVoltar = { navController.popBackStack() },
                onCadastroOk = {
                    navController.navigate(Rotas.AMBIENTES) {
                        popUpTo(Rotas.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Rotas.AMBIENTES) {
            AmbientesScreen(
                onAbrirAmbiente = { id -> navController.navigate(Rotas.ambiente(id)) },
                onBuscar = { navController.navigate(Rotas.BUSCA) },
                onSair = {
                    app.authRepository.sair()
                    navController.navigate(Rotas.LOGIN) {
                        popUpTo(Rotas.AMBIENTES) { inclusive = true }
                    }
                }
            )
        }

        composable(Rotas.BUSCA) {
            BuscaScreen(
                onVoltar = { navController.popBackStack() },
                onAbrirResultado = { r ->
                    navController.navigate(
                        Rotas.ambiente(
                            id = r.ambienteId,
                            prateleiraId = r.prateleiraId,
                            gondolaId = r.gondolaId,
                            itemId = r.itemId
                        )
                    )
                }
            )
        }

        composable(
            route = Rotas.AMBIENTE,
            arguments = listOf(
                navArgument("ambienteId") { type = NavType.LongType },
                argOpcional("prateleiraId"),
                argOpcional("gondolaId"),
                argOpcional("itemId")
            )
        ) {
            AmbienteScreen(
                onVoltar = { navController.popBackStack() },
                onAbrirPrateleira = { prateleiraId, gondolaId ->
                    navController.navigate(Rotas.prateleira(prateleiraId, gondolaId ?: -1))
                }
            )
        }

        composable(
            route = Rotas.PRATELEIRA,
            arguments = listOf(
                navArgument("prateleiraId") { type = NavType.LongType },
                argOpcional("gondolaId")
            )
        ) {
            PrateleiraScreen(
                onVoltar = { navController.popBackStack() },
                onAbrirGondola = { id -> navController.navigate(Rotas.gondola(id)) }
            )
        }

        composable(
            route = Rotas.GONDOLA,
            arguments = listOf(navArgument("gondolaId") { type = NavType.LongType })
        ) {
            GondolaScreen(onVoltar = { navController.popBackStack() })
        }
    }
}
