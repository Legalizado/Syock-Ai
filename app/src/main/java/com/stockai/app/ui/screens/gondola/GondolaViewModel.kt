package com.stockai.app.ui.screens.gondola

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockai.app.data.local.Gondola
import com.stockai.app.data.local.Item
import com.stockai.app.data.repository.GondolaRepository
import com.stockai.app.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormItem(
    val original: Item? = null,
    val nome: String = "",
    val quantidade: String = "",
    val unidade: String = "un.",
    val descricao: String = "",
    val erro: String? = null
)

class GondolaViewModel(
    savedStateHandle: SavedStateHandle,
    gondolaRepo: GondolaRepository,
    private val itemRepo: ItemRepository
) : ViewModel() {

    private val gondolaId: Long = checkNotNull(savedStateHandle["gondolaId"])

    val gondola: StateFlow<Gondola?> =
        gondolaRepo.observarPorId(gondolaId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null = carregando */
    val itens: StateFlow<List<Item>?> =
        itemRepo.observarPorGondola(gondolaId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _form = MutableStateFlow<FormItem?>(null)
    val form: StateFlow<FormItem?> = _form.asStateFlow()

    private val _paraExcluir = MutableStateFlow<Item?>(null)
    val paraExcluir: StateFlow<Item?> = _paraExcluir.asStateFlow()

    fun novoItem() {
        _form.value = FormItem()
    }

    fun editar(item: Item) {
        _form.value = FormItem(
            original = item,
            nome = item.nome,
            quantidade = item.quantidade.toString(),
            unidade = item.unidade,
            descricao = item.descricao.orEmpty()
        )
    }

    fun atualizarForm(novo: FormItem) {
        _form.value = novo.copy(erro = null)
    }

    fun fecharForm() {
        _form.value = null
    }

    fun salvar() {
        val f = _form.value ?: return
        val quantidade = f.quantidade.trim().toIntOrNull()

        if (f.nome.isBlank()) return mostrarErro("Informe o nome do item")
        if (quantidade == null || quantidade < 0) return mostrarErro("Quantidade inválida")
        if (f.unidade.isBlank()) return mostrarErro("Informe a unidade (ex.: un., cx., kg)")

        val descricao = f.descricao.trim().ifBlank { null }

        viewModelScope.launch {
            val original = f.original
            if (original == null) {
                itemRepo.criar(
                    Item(
                        gondolaId = gondolaId,
                        nome = f.nome.trim(),
                        quantidade = quantidade,
                        unidade = f.unidade.trim(),
                        descricao = descricao
                    )
                )
            } else {
                itemRepo.atualizar(
                    original.copy(
                        nome = f.nome.trim(),
                        quantidade = quantidade,
                        unidade = f.unidade.trim(),
                        descricao = descricao
                    )
                )
            }
            _form.value = null
        }
    }

    private fun mostrarErro(mensagem: String) {
        _form.update { it?.copy(erro = mensagem) }
    }

    /** Botões + e − do card */
    fun ajustarQuantidade(item: Item, delta: Int) {
        viewModelScope.launch { itemRepo.ajustarQuantidade(item, delta) }
    }

    fun pedirExclusao(item: Item) {
        _paraExcluir.value = item
    }

    fun cancelarExclusao() {
        _paraExcluir.value = null
    }

    fun confirmarExclusao() {
        val item = _paraExcluir.value ?: return
        viewModelScope.launch {
            itemRepo.apagar(item)
            _paraExcluir.value = null
        }
    }
}
