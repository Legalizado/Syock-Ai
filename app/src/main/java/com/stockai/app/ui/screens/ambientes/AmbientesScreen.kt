package com.stockai.app.ui.screens.ambientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockai.app.data.local.AmbienteResumo
import com.stockai.app.ui.AppViewModelProvider
import com.stockai.app.ui.components.formatarMetros
import com.stockai.app.ui.components.plural
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientesScreen(
    onAbrirAmbiente: (Long) -> Unit,
    onBuscar: () -> Unit,
    onSair: () -> Unit,
    viewModel: AmbientesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val ambientes by viewModel.ambientes.collectAsState()
    val form by viewModel.form.collectAsState()
    val paraExcluir by viewModel.paraExcluir.collectAsState()
    val criandoExemplo by viewModel.criandoExemplo.collectAsState()
    var menuTopo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StockAi", fontWeight = FontWeight.Bold) },
                actions = {
                    // Menu do topo: dados de exemplo e sair
                    Box {
                        IconButton(onClick = { menuTopo = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(expanded = menuTopo, onDismissRequest = { menuTopo = false }) {
                            DropdownMenuItem(
                                text = { Text("Criar ambiente de exemplo") },
                                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                                enabled = !criandoExemplo,
                                onClick = {
                                    menuTopo = false
                                    viewModel.criarExemplo()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sair") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                                onClick = {
                                    menuTopo = false
                                    onSair()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::novo,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo ambiente", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barra de busca (por enquanto só abre a tela de busca)
            Surface(
                onClick = onBuscar,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Buscar item em todos os ambientes",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Meus ambientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            val lista = ambientes
            when {
                lista == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                lista.isEmpty() -> EstadoVazio(
                    criandoExemplo = criandoExemplo,
                    onCriarExemplo = viewModel::criarExemplo
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(lista, key = { it.ambiente.id }) { resumo ->
                        CardAmbiente(
                            resumo = resumo,
                            onClick = { onAbrirAmbiente(resumo.ambiente.id) },
                            onEditar = { viewModel.editar(resumo.ambiente) },
                            onExcluir = { viewModel.pedirExclusao(resumo.ambiente) }
                        )
                    }
                }
            }
        }
    }

    // ---------- Diálogos ----------

    form?.let { f ->
        DialogoAmbiente(
            form = f,
            onNomeChange = viewModel::onNomeChange,
            onLarguraChange = viewModel::onLarguraChange,
            onComprimentoChange = viewModel::onComprimentoChange,
            onSalvar = viewModel::salvar,
            onCancelar = viewModel::fecharForm
        )
    }

    paraExcluir?.let { ambiente ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarExclusao,
            title = { Text("Excluir \"${ambiente.nome}\"?") },
            text = {
                Text(
                    "Todas as prateleiras, gôndolas e itens deste ambiente também serão " +
                            "excluídos. Essa ação não pode ser desfeita."
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmarExclusao,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarExclusao) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CardAmbiente(
    resumo: AmbienteResumo,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit
) {
    val ambiente = resumo.ambiente
    var menuAberto by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniPlanta(
                largura = ambiente.largura,
                comprimento = ambiente.comprimento,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ambiente.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatarMetros(ambiente.largura)} × ${formatarMetros(ambiente.comprimento)} m · " +
                            plural(resumo.totalPrateleiras, "prateleira", "prateleiras"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = plural(resumo.totalItens, "item", "itens"),
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
                        onClick = {
                            menuAberto = false
                            onEditar()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuAberto = false
                            onExcluir()
                        }
                    )
                }
            }
        }
    }
}

/** Desenha o contorno do ambiente na proporção real (12 × 8 m fica retangular) */
@Composable
private fun MiniPlanta(largura: Float, comprimento: Float, modifier: Modifier = Modifier) {
    val corFundo = MaterialTheme.colorScheme.primaryContainer
    val corLinha = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(corFundo)
            .padding(10.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val escala = min(size.width / largura, size.height / comprimento)
            val w = largura * escala
            val h = comprimento * escala
            drawRect(
                color = corLinha,
                topLeft = Offset((size.width - w) / 2, (size.height - h) / 2),
                size = Size(w, h),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun EstadoVazio(criandoExemplo: Boolean, onCriarExemplo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Warehouse,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Nenhum ambiente ainda",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Toque em \"Novo ambiente\" para cadastrar seu primeiro estoque.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onCriarExemplo, enabled = !criandoExemplo) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Criar ambiente de exemplo")
        }
    }
}

@Composable
private fun DialogoAmbiente(
    form: FormAmbiente,
    onNomeChange: (String) -> Unit,
    onLarguraChange: (String) -> Unit,
    onComprimentoChange: (String) -> Unit,
    onSalvar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (form.original == null) "Novo ambiente" else "Editar ambiente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.nome,
                    onValueChange = onNomeChange,
                    label = { Text("Nome (ex.: Almoxarifado)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = form.largura,
                        onValueChange = onLarguraChange,
                        label = { Text("Largura") },
                        suffix = { Text("m") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = form.comprimento,
                        onValueChange = onComprimentoChange,
                        label = { Text("Comprimento") },
                        suffix = { Text("m") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Use as medidas reais do espaço, em metros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                form.erro?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { Button(onClick = onSalvar) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
