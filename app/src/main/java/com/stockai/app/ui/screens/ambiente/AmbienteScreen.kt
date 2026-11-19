package com.stockai.app.ui.screens.ambiente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockai.app.data.local.Prateleira
import com.stockai.app.ui.AppViewModelProvider
import com.stockai.app.ui.components.formatarMetros

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbienteScreen(
    onVoltar: () -> Unit,
    onAbrirPrateleira: (prateleiraId: Long, gondolaId: Long?) -> Unit,
    viewModel: AmbienteViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val ambiente by viewModel.ambiente.collectAsState()
    val prateleiras by viewModel.prateleiras.collectAsState()
    val selecionadaId by viewModel.selecionadaId.collectAsState()
    val form by viewModel.form.collectAsState()
    val paraExcluir by viewModel.paraExcluir.collectAsState()
    val destaque by viewModel.destaque.collectAsState()
    val gondolas by viewModel.gondolas.collectAsState()
    val aviso by viewModel.aviso.collectAsState()

    // Mostra avisos (ex.: prateleira em cima de outra) numa barrinha embaixo
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(aviso) {
        aviso?.let {
            snackbar.showSnackbar(it)
            viewModel.limparAviso()
        }
    }

    var aba by rememberSaveable { mutableIntStateOf(0) } // 0 = Planta, 1 = 3D
    val selecionada = prateleiras.find { it.id == selecionadaId }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(ambiente?.nome ?: "") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::novaPrateleira, enabled = ambiente != null) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Prateleira", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        val amb = ambiente
        if (amb == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Abas Planta | 3D
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    listOf("Planta", "3D").forEachIndexed { indice, rotulo ->
                        SegmentedButton(
                            selected = aba == indice,
                            onClick = { aba = indice },
                            shape = SegmentedButtonDefaults.itemShape(index = indice, count = 2)
                        ) { Text(rotulo) }
                    }
                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (aba == 0) {
                        PlantaCanvas(
                            ambiente = amb,
                            prateleiras = prateleiras,
                            selecionadaId = selecionadaId,
                            onSelecionar = viewModel::selecionar,
                            onMover = viewModel::mover,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (prateleiras.isEmpty()) {
                            Text(
                                text = "Toque em \"+ Prateleira\" para adicionar a primeira",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            )
                        }
                    } else {
                        Visualizacao3D(
                            ambiente = amb,
                            prateleiras = prateleiras,
                            gondolas = gondolas,
                            selecionadaId = selecionadaId,
                            gondolaDestacadaId = destaque?.gondola?.id,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "Arraste para girar · pinça para zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                        )
                    }
                }

                val achado = destaque
                run {
                    if (achado != null) {
                        CardDestaque(
                            destaque = achado,
                            nomePrateleira = prateleiras.find { it.id == achado.gondola.prateleiraId }?.nome ?: "",
                            onVerNaPrateleira = {
                                onAbrirPrateleira(achado.gondola.prateleiraId, achado.gondola.id)
                            },
                            onFechar = viewModel::fecharDestaque
                        )
                    } else if (selecionada != null) {
                        PainelPrateleira(
                            prateleira = selecionada,
                            onGirar = { viewModel.girar(selecionada) },
                            onEditar = { viewModel.editar(selecionada) },
                            onExcluir = { viewModel.pedirExclusao(selecionada) },
                            onAbrir = { onAbrirPrateleira(selecionada.id, null) }
                        )
                    } else if (aba == 0 && prateleiras.isNotEmpty()) {
                        Text(
                            text = "Toque numa prateleira para selecionar · arraste para mover",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    // ---------- Diálogos ----------

    form?.let { f ->
        DialogoPrateleira(
            form = f,
            onChange = viewModel::atualizarForm,
            onSalvar = viewModel::salvar,
            onCancelar = viewModel::fecharForm
        )
    }

    paraExcluir?.let { p ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarExclusao,
            title = { Text("Excluir \"${p.nome}\"?") },
            text = { Text("As gôndolas e os itens desta prateleira também serão excluídos.") },
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

/** Cartão âmbar mostrando onde está o item encontrado na busca */
@Composable
private fun CardDestaque(
    destaque: Destaque,
    nomePrateleira: String,
    onVerNaPrateleira: () -> Unit,
    onFechar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = destaque.item.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "$nomePrateleira · Gôndola ${destaque.gondola.codigo} · " +
                                "${destaque.gondola.nivel}º nível · " +
                                "${destaque.item.quantidade} ${destaque.item.unidade}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                IconButton(onClick = onFechar) {
                    Icon(Icons.Outlined.Close, contentDescription = "Fechar")
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onVerNaPrateleira,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Ver gôndola ${destaque.gondola.codigo} na prateleira", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Painel inferior com os dados da prateleira selecionada */
@Composable
private fun PainelPrateleira(
    prateleira: Prateleira,
    onGirar: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
    onAbrir: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = prateleira.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatarMetros(prateleira.largura)} × " +
                                "${formatarMetros(prateleira.profundidade)} × " +
                                "${formatarMetros(prateleira.altura)} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onGirar) {
                    Icon(Icons.Outlined.Rotate90DegreesCw, contentDescription = "Girar 90°")
                }
                IconButton(onClick = onEditar) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onExcluir) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onAbrir,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Ver gôndolas e itens", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DialogoPrateleira(
    form: FormPrateleira,
    onChange: (FormPrateleira) -> Unit,
    onSalvar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (form.original == null) "Nova prateleira" else "Editar prateleira") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.nome,
                    onValueChange = { onChange(form.copy(nome = it)) },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Medidas (m)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoMetros(form.largura, "Larg.", { onChange(form.copy(largura = it)) }, Modifier.weight(1f))
                    CampoMetros(form.profundidade, "Prof.", { onChange(form.copy(profundidade = it)) }, Modifier.weight(1f))
                    CampoMetros(form.altura, "Alt.", { onChange(form.copy(altura = it)) }, Modifier.weight(1f))
                }

                Text("Posição na planta (m)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoMetros(form.posX, "X", { onChange(form.copy(posX = it)) }, Modifier.weight(1f))
                    CampoMetros(form.posY, "Y", { onChange(form.copy(posY = it)) }, Modifier.weight(1f))
                }
                Text(
                    text = "X conta a partir da parede esquerda e Y a partir da parede de cima. " +
                            "Você também pode arrastar a prateleira na planta.",
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

@Composable
private fun CampoMetros(
    valor: String,
    rotulo: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(rotulo) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
