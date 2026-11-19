package com.stockai.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoresClaras = lightColorScheme(
    primary = Petroleo,
    onPrimary = Color.White,
    primaryContainer = PetroleoClaro,
    onPrimaryContainer = PetroleoEscuro,
    secondary = Ambar,
    onSecondary = PetroleoEscuro,
    secondaryContainer = AmbarClaro,
    onSecondaryContainer = PetroleoEscuro,
    background = Fundo,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    onSurfaceVariant = TextoSecundario,
    error = Erro
)

private val CoresEscuras = darkColorScheme(
    primary = PetroleoNoite,
    onPrimary = PetroleoEscuro,
    primaryContainer = Petroleo,
    onPrimaryContainer = TextoNoite,
    secondary = Ambar,
    onSecondary = PetroleoEscuro,
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = AmbarClaro,
    background = FundoNoite,
    onBackground = TextoNoite,
    surface = SuperficieNoite,
    onSurface = TextoNoite,
    onSurfaceVariant = Color(0xFFA9BABF),
    error = Color(0xFFF2B8B5)
)

/**
 * Tema do StockAi.
 * Cores dinâmicas (Material You) desativadas de propósito,
 * para manter a identidade visual do app em qualquer aparelho.
 */
@Composable
fun StockAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) CoresEscuras else CoresClaras,
        typography = Typography, // vem do Type.kt gerado pelo Android Studio
        content = content
    )
}
