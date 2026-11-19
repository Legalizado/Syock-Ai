package com.stockai.app.ui.screens.ambiente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockai.app.data.local.Ambiente
import com.stockai.app.data.local.Gondola
import com.stockai.app.data.local.GondolaResumo
import com.stockai.app.data.local.Item
import com.stockai.app.data.local.Prateleira
import com.stockai.app.data.repository.AmbienteRepository
import com.stockai.app.data.repository.GondolaRepository
import com.stockai.app.data.repository.ItemRepository
import com.stockai.app.data.repository.PrateleiraRepository
import com.stockai.app.ui.components.formatarMetros
import com.stockai.app.ui.components.lerDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/*
 * "Pegada" = o espaço que a prateleira ocupa no chão, vista de cima.
 * Com rotação 0, a largura fica na horizontal da planta.
 * Com rotação 90, ela fica "em pé": largura e profundidade trocam de lugar.
 */
val Prateleira.pegadaX: Float get() = if (rotacao == 90) profundidade else largura
val Prateleira.pegadaY: Float get() = if (rotacao == 90) largura else profundidade

/** Dados do diálogo de criar/editar prateleira (tudo em texto, como digitado) */
/** Item encontrado pela busca, para mostrar em destaque na planta */
data class Destaque(val item: Item, val gondola: Gondola)

data class FormPrateleira(
    val original: Prateleira? = null,
    val nome: String = "",
    val largura: String = "",
    val profundidade: String = "",
    val altura: String = "",
    val posX: String = "",
    val posY: String = "",
    val erro: String? = null
)

class AmbienteViewModel(
    savedStateHandle: SavedStateHandle,
    ambienteRepo: AmbienteRepository,
    private val prateleiraRepo: PrateleiraRepository,
    gondolaRepo: GondolaRepository,
    itemRepo: ItemRepository
) : ViewModel() {

    private val ambienteId: Long = checkNotNull(savedStateHandle["ambienteId"])

    // Parâmetros opcionais que vêm da busca (-1 = não veio da busca)
    private val prateleiraBuscadaId: Long = savedStateHandle.get<Long>("prateleiraId") ?: -1L
    private val gondolaBuscadaId: Long = savedStateHandle.get<Long>("gondolaId") ?: -1L
    private val itemBuscadoId: Long = savedStateHandle.get<Long>("itemId") ?: -1L

    val ambiente: StateFlow<Ambiente?> =
        ambienteRepo.observarPorId(ambienteId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val prateleiras: StateFlow<List<Prateleira>> =
        prateleiraRepo.observarPorAmbiente(ambienteId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Se veio da busca, a prateleira do item já abre selecionada (em âmbar)
    /** Gôndolas de todas as prateleiras deste ambiente (usadas na vista 3D) */
    val gondolas: StateFlow<List<GondolaResumo>> =
        gondolaRepo.observarResumosPorAmbiente(ambienteId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selecionadaId = MutableStateFlow(prateleiraBuscadaId.takeIf { it > 0 })
    val selecionadaId: StateFlow<Long?> = _selecionadaId.asStateFlow()

    private val _form = MutableStateFlow<FormPrateleira?>(null)
    val form: StateFlow<FormPrateleira?> = _form.asStateFlow()

    private val _paraExcluir = MutableStateFlow<Prateleira?>(null)
    val paraExcluir: StateFlow<Prateleira?> = _paraExcluir.asStateFlow()

    // ---------- Destaque do item buscado ----------

    private val _mostrarDestaque = MutableStateFlow(itemBuscadoId > 0)

    val destaque: StateFlow<Destaque?> =
        if (itemBuscadoId > 0) {
            combine(
                itemRepo.observarPorId(itemBuscadoId),
                gondolaRepo.observarPorId(gondolaBuscadaId),
                _mostrarDestaque
            ) { item, gondola, mostrar ->
                if (item != null && gondola != null && mostrar) Destaque(item, gondola) else null
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        } else {
            MutableStateFlow<Destaque?>(null).asStateFlow()
        }

    fun fecharDestaque() {
        _mostrarDestaque.value = false
    }

    // ---------- Avisos rápidos (Snackbar) ----------

    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    fun limparAviso() {
        _aviso.value = null
    }

    // ---------- Sobreposição (Fase 9) ----------

    /** true se o retângulo (x, y, w, h) invade a pegada de [outra]. Só encostar não conta. */
    private fun invade(x: Float, y: Float, w: Float, h: Float, outra: Prateleira): Boolean {
        val tol = 0.01f
        return x < outra.posX + outra.pegadaX - tol && x + w > outra.posX + tol &&
                y < outra.posY + outra.pegadaY - tol && y + h > outra.posY + tol
    }

    /** Devolve a primeira prateleira (diferente de [ignorarId]) que ficaria embaixo do retângulo */
    private fun conflito(ignorarId: Long?, x: Float, y: Float, w: Float, h: Float): Prateleira? =
        prateleiras.value.firstOrNull { it.id != ignorarId && invade(x, y, w, h, it) }

    /** Procura, de 0,5 em 0,5 m, o primeiro lugar livre onde cabe uma pegada w × h */
    private fun posicaoLivre(w: Float, h: Float): Pair<Float, Float> {
        val amb = ambiente.value ?: return 0f to 0f
        var y = 0f
        while (y + h <= amb.comprimento + 0.001f) {
            var x = 0f
            while (x + w <= amb.largura + 0.001f) {
                if (conflito(null, x, y, w, h) == null) return x to y
                x += 0.5f
            }
            y += 0.5f
        }
        return 0f to 0f // ambiente cheio: o usuário ajusta na mão
    }

    fun selecionar(id: Long?) {
        _selecionadaId.value = id
        // Tocar em outra prateleira esconde o destaque da busca
        if (id != prateleiraBuscadaId) _mostrarDestaque.value = false
    }

    // ---------- Mover e girar na planta ----------

    /** Chamado quando o usuário solta a prateleira depois de arrastar */
    fun mover(prateleira: Prateleira, x: Float, y: Float) {
        val amb = ambiente.value ?: return
        val movida = prateleira.copy(
            posX = ajustar(x, amb.largura - prateleira.pegadaX),
            posY = ajustar(y, amb.comprimento - prateleira.pegadaY)
        )
        // Se cair em cima de outra, não salva: a prateleira volta para onde estava
        conflito(movida.id, movida.posX, movida.posY, movida.pegadaX, movida.pegadaY)?.let {
            _aviso.value = "${prateleira.nome} ficaria em cima da ${it.nome}"
            return
        }
        viewModelScope.launch { prateleiraRepo.atualizar(movida) }
    }

    fun girar(prateleira: Prateleira) {
        val amb = ambiente.value ?: return
        val girada = prateleira.copy(rotacao = if (prateleira.rotacao == 90) 0 else 90)
        val ajustada = girada.copy(
            posX = ajustar(girada.posX, amb.largura - girada.pegadaX),
            posY = ajustar(girada.posY, amb.comprimento - girada.pegadaY)
        )
        conflito(ajustada.id, ajustada.posX, ajustada.posY, ajustada.pegadaX, ajustada.pegadaY)?.let {
            _aviso.value = "Girando, ${prateleira.nome} ficaria em cima da ${it.nome}"
            return
        }
        viewModelScope.launch { prateleiraRepo.atualizar(ajustada) }
    }

    /** Arredonda para 10 cm e mantém a prateleira dentro das paredes */
    private fun ajustar(valor: Float, maximo: Float): Float =
        ((valor * 10).roundToInt() / 10f).coerceIn(0f, maximo.coerceAtLeast(0f))

    // ---------- Criar / editar ----------

    fun novaPrateleira() {
        // Já sugere um lugar livre para a prateleira padrão de 2 × 0,6 m
        val (x, y) = posicaoLivre(2f, 0.6f)
        _form.value = FormPrateleira(
            nome = "Prateleira ${proximaLetra()}",
            largura = "2",
            profundidade = "0,6",
            altura = "2",
            posX = formatarMetros(x),
            posY = formatarMetros(y)
        )
    }

    /** Sugere a primeira letra livre: A, B, C... */
    private fun proximaLetra(): String {
        val usadas = prateleiras.value.map { it.nome.removePrefix("Prateleira ").trim() }.toSet()
        return ('A'..'Z').map { it.toString() }.firstOrNull { it !in usadas }
            ?: (prateleiras.value.size + 1).toString()
    }

    fun editar(p: Prateleira) {
        _form.value = FormPrateleira(
            original = p,
            nome = p.nome,
            largura = formatarMetros(p.largura),
            profundidade = formatarMetros(p.profundidade),
            altura = formatarMetros(p.altura),
            posX = formatarMetros(p.posX),
            posY = formatarMetros(p.posY)
        )
    }

    fun atualizarForm(novo: FormPrateleira) {
        _form.value = novo.copy(erro = null)
    }

    fun fecharForm() {
        _form.value = null
    }

    fun salvar() {
        val f = _form.value ?: return
        val amb = ambiente.value ?: return

        val largura = lerDecimal(f.largura)
        val profundidade = lerDecimal(f.profundidade)
        val altura = lerDecimal(f.altura)
        val posX = lerDecimal(f.posX)
        val posY = lerDecimal(f.posY)

        if (f.nome.isBlank()) return mostrarErro("Informe o nome da prateleira")
        if (largura == null || largura <= 0f ||
            profundidade == null || profundidade <= 0f ||
            altura == null || altura <= 0f
        ) return mostrarErro("Largura, profundidade e altura precisam ser maiores que zero")
        if (altura > 10f) return mostrarErro("Altura máxima de 10 m")
        if (posX == null || posX < 0f || posY == null || posY < 0f) return mostrarErro("Posição inválida")

        val rotacao = f.original?.rotacao ?: 0
        val pegadaX = if (rotacao == 90) profundidade else largura
        val pegadaY = if (rotacao == 90) largura else profundidade
        if (posX + pegadaX > amb.largura + 0.001f || posY + pegadaY > amb.comprimento + 0.001f) {
            return mostrarErro(
                "A prateleira passa das paredes do ambiente " +
                        "(${formatarMetros(amb.largura)} × ${formatarMetros(amb.comprimento)} m)"
            )
        }

        conflito(f.original?.id, posX, posY, pegadaX, pegadaY)?.let {
            return mostrarErro("Nessa posição ela fica em cima da ${it.nome}")
        }

        viewModelScope.launch {
            val original = f.original
            if (original == null) {
                val id = prateleiraRepo.criar(
                    Prateleira(
                        ambienteId = ambienteId,
                        nome = f.nome.trim(),
                        posX = posX,
                        posY = posY,
                        largura = largura,
                        profundidade = profundidade,
                        altura = altura
                    )
                )
                _selecionadaId.value = id // já deixa a nova selecionada
            } else {
                prateleiraRepo.atualizar(
                    original.copy(
                        nome = f.nome.trim(),
                        posX = posX,
                        posY = posY,
                        largura = largura,
                        profundidade = profundidade,
                        altura = altura
                    )
                )
            }
            _form.value = null
        }
    }

    private fun mostrarErro(mensagem: String) {
        _form.update { it?.copy(erro = mensagem) }
    }

    // ---------- Excluir ----------

    fun pedirExclusao(p: Prateleira) {
        _paraExcluir.value = p
    }

    fun cancelarExclusao() {
        _paraExcluir.value = null
    }

    fun confirmarExclusao() {
        val p = _paraExcluir.value ?: return
        viewModelScope.launch {
            prateleiraRepo.apagar(p)
            if (_selecionadaId.value == p.id) _selecionadaId.value = null
            _paraExcluir.value = null
        }
    }
}
