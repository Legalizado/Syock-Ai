package com.stockai.app.ui.screens.busca

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockai.app.data.local.ResultadoBusca
import com.stockai.app.ui.AppViewModelProvider
import com.stockai.app.ui.components.BarraTopo
import com.stockai.app.ui.components.plural

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuscaScreen(
    onVoltar: () -> Unit,
    onAbrirResultado: (ResultadoBusca) -> Unit,
    viewModel: BuscaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val termo by viewModel.termo.collectAsState()
    val resultados by viewModel.resultados.collectAsState()
    val recentes by viewModel.recentes.collectAsState()

    val foco = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current

    // Abre a tela já com o cursor no campo e o teclado aberto
    LaunchedEffect(Unit) { foco.requestFocus() }

    Scaffold(
        topBar = { BarraTopo(titulo = "Buscar item", onVoltar = onVoltar) }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = termo,
                onValueChange = viewModel::onTermoChange,
                placeholder = { Text("Ex.: parafuso, fita, chave 10…") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (termo.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onTermoChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Limpar")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.registrarBusca()
                    teclado?.hide()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(foco)
            )

            val lista = resultados
            when {
                // Campo vazio: mostra as buscas recentes
                lista == null -> Column(Modifier.padding(horizontal = 16.dp)) {
                    if (recentes.isEmpty()) {
                        Text(
                            "Digite o nome de um item para descobrir em qual ambiente, " +
                                    "prateleira e gôndola ele está.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Buscas recentes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = viewModel::limparRecentes) { Text("Limpar") }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentes.forEach { r ->
                                SuggestionChip(
                                    onClick = { viewModel.usarRecente(r) },
                                    label = { Text(r) },
                                    icon = { Icon(Icons.Outlined.History, contentDescription = null, Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }

                lista.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Nenhum item encontrado para \"${termo.trim()}\"",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Text(
                        text = plural(lista.size, "resultado", "resultados"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(lista, key = { _, r -> r.itemId }) { indice, r ->
                            CardResultado(
                                resultado = r,
                                termo = termo.trim(),
                                principal = indice == 0, // o 1º resultado ganha destaque âmbar
                                onClick = {
                                    viewModel.registrarBusca()
                                    onAbrirResultado(r)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardResultado(
    resultado: ResultadoBusca,
    termo: String,
    principal: Boolean,
    onClick: () -> Unit
) {
    val cores = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (principal) cores.secondaryContainer else cores.surface
        ),
        border = if (principal) BorderStroke(2.dp, cores.secondary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = destacarTermo(resultado.itemNome, termo, cores.secondary.copy(alpha = 0.35f)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = resultado.caminho,
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${resultado.quantidade} ${resultado.unidade}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Pinta o fundo do trecho que bate com o que foi digitado ("Parafuso" em "Parafuso M8") */
private fun destacarTermo(texto: String, termo: String, cor: Color): AnnotatedString {
    val inicio = if (termo.isEmpty()) -1 else texto.indexOf(termo, ignoreCase = true)
    if (inicio < 0) return AnnotatedString(texto)
    return buildAnnotatedString {
        append(texto.substring(0, inicio))
        withStyle(SpanStyle(background = cor)) {
            append(texto.substring(inicio, inicio + termo.length))
        }
        append(texto.substring(inicio + termo.length))
    }
}
