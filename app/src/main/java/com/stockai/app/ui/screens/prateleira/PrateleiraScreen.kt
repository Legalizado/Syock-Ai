package com.stockai.app.ui.screens.prateleira

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockai.app.data.local.GondolaResumo
import com.stockai.app.ui.AppViewModelProvider
import com.stockai.app.ui.components.BarraTopo
import com.stockai.app.ui.components.formatarMetros
import com.stockai.app.ui.components.plural

@Composable
fun PrateleiraScreen(
    onVoltar: () -> Unit,
    onAbrirGondola: (Long) -> Unit,
    viewModel: PrateleiraViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val prateleira by viewModel.prateleira.collectAsState()
    val gondolas by viewModel.gondolas.collectAsState()
    val form by viewModel.form.collectAsState()
    val paraExcluir by viewModel.paraExcluir.collectAsState()

    Scaffold(
        topBar = {
            val p = prateleira
            BarraTopo(
                titulo = p?.nome ?: "",
                subtitulo = p?.let {
                    "${formatarMetros(it.largura)} × ${formatarMetros(it.profundidade)} × ${formatarMetros(it.altura)} m"
                },
                onVoltar = onVoltar
            )
        }
    ) { innerPadding ->
        if (prateleira == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ---------- Vista frontal ----------
                item {
                    Text(
                        "Vista frontal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            Modifier
                                .padding(16.dp)
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (gondolas.isEmpty()) {
                                Text(
                                    "Adicione gôndolas para ver os níveis da prateleira",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                VistaFrontalCanvas(
                                    gondolas = gondolas,
                                    onTocarGondola = onAbrirGondola,
                                    destacadaId = viewModel.gondolaDestacadaId,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // ---------- Lista de gôndolas ----------
                item {
                    Text(
                        text = "Gôndolas (${gondolas.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(gondolas, key = { it.gondola.id }) { resumo ->
                    CardGondola(
                        resumo = resumo,
                        destacada = resumo.gondola.id == viewModel.gondolaDestacadaId,
                        onClick = { onAbrirGondola(resumo.gondola.id) },
                        onEditar = { viewModel.editar(resumo.gondola) },
                        onExcluir = { viewModel.pedirExclusao(resumo.gondola) }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = viewModel::novaGondola,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Adicionar gôndola")
                    }
                }
            }
        }
    }

    form?.let { f ->
        AlertDialog(
            onDismissRequest = viewModel::fecharForm,
            title = { Text(if (f.original == null) "Nova gôndola" else "Editar gôndola") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = f.codigo,
                            onValueChange = { viewModel.atualizarForm(f.copy(codigo = it)) },
                            label = { Text("Código") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = f.nivel,
                            onValueChange = { viewModel.atualizarForm(f.copy(nivel = it)) },
                            label = { Text("Nível") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = f.nome,
                        onValueChange = { viewModel.atualizarForm(f.copy(nome = it)) },
                        label = { Text("Nome (ex.: Parafusos)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "O nível 1 é o de baixo. Gôndolas no mesmo nível ficam lado a lado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    f.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { Button(onClick = viewModel::salvar) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = viewModel::fecharForm) { Text("Cancelar") } }
        )
    }

    paraExcluir?.let { g ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarExclusao,
            title = { Text("Excluir a gôndola ${g.codigo}?") },
            text = { Text("Os itens guardados nela também serão excluídos.") },
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
private fun CardGondola(
    resumo: GondolaResumo,
    destacada: Boolean,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit
) {
    val g = resumo.gondola
    var menuAberto by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (destacada) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (destacada) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selo com o código (ex.: B3)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    g.codigo,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.nome, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Nível ${g.nivel} · ${plural(resumo.totalItens, "item", "itens")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuAberto = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Opções")
                }
                DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuAberto = false; onEditar() }
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { menuAberto = false; onExcluir() }
                    )
                }
            }
        }
    }
}
