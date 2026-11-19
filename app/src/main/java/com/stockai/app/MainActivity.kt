package com.stockai.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stockai.app.ui.navigation.StockAiNavHost
import com.stockai.app.ui.theme.StockAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ícones da barra de status (hora, bateria) em branco,
        // porque o topo das telas do StockAi é verde-petróleo
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            StockAiTheme {
                StockAiNavHost()
            }
        }
    }
}
