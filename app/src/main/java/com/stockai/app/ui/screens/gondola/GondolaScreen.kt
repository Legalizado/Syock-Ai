package com.stockai.app.ui.screens.gondola

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockai.app.data.local.Item
import com.stockai.app.ui.AppViewModelProvider
import com.stockai.app.ui.components.BarraTopo
import com.stockai.app.ui.components.plural
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoData = SimpleDateFormat("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR"))

@Composable
fun GondolaScreen(
    onVoltar: () -> Unit,
    viewModel: GondolaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val gondola by viewModel.gondola.collectAsState()
    val itens by viewModel.itens.collectAsState()
    val form by viewModel.form.collectAsState()
    val paraExcluir by viewModel.paraExcluir.collectAsState()

    Scaffold(
        topBar = {
            val g = gondola
            BarraTopo(
                titulo = if (g != null) "Gôndola ${g.codigo} · ${g.nome}" else "",
                subtitulo = itens?.let { plural(it.size, "item", "itens") },
                onVoltar = onVoltar
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::novoItem,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Item", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        }
    ) { innerPadding ->
        val lista = itens
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                lista == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                lista.isEmpty() -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Gôndola vazia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Toque em \"+ Item\" para cadastrar o que está guardado aqui.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(lista, key = { it.id }) { item ->
                        CardItem(
                            item = item,
                            onClick = { viewModel.editar(item) },
                            onMenos = { viewModel.ajustarQuantidade(item, -1) },
                            onMais = { viewModel.ajustarQuantidade(item, +1) }
                        )
                    }
                }
            }
        }
    }

    form?.let { f ->
        AlertDialog(
            onDismissRequest = viewModel::fecharForm,
            title = { Text(if (f.original == null) "Novo item" else "Editar item") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = f.nome,
                        onValueChange = { viewModel.atualizarForm(f.copy(nome = it)) },
                        label = { Text("Nome (ex.: Parafuso M8 × 30 mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = f.quantidade,
                            onValueChange = { viewModel.atualizarForm(f.copy(quantidade = it)) },
                            label = { Text("Quantidade") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = f.unidade,
                            onValueChange = { viewModel.atualizarForm(f.copy(unidade = it)) },
                            label = { Text("Unidade") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = f.descricao,
                        onValueChange = { viewModel.atualizarForm(f.copy(descricao = it)) },
                        label = { Text("Observação (opcional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    f.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    // No modo edição, o botão de excluir fica dentro do formulário
                    f.original?.let { original ->
                        TextButton(
                            onClick = {
                                viewModel.fecharForm()
                                viewModel.pedirExclusao(original)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Text("Excluir item")
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = viewModel::salvar) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = viewModel::fecharForm) { Text("Cancelar") } }
        )
    }

    paraExcluir?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarExclusao,
            title = { Text("Excluir \"${item.nome}\"?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmarExclusao,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarExclusao) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CardItem(
    item: Item,
    onClick: () -> Unit,
    onMenos: () -> Unit,
    onMais: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.nome,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.descricao?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "Atualizado ${formatoData.format(Date(item.atualizadoEm))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Controle rápido de quantidade
            FilledTonalIconButton(onClick = onMenos, enabled = item.quantidade > 0) {
                Icon(Icons.Outlined.Remove, contentDescription = "Diminuir")
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(min = 48.dp)
            ) {
                Text(
                    "${item.quantidade}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.quantidade == 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    item.unidade,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(onClick = onMais) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar")
            }
        }
    }
}
