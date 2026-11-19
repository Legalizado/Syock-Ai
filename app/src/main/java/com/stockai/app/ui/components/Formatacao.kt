package com.stockai.app.ui.components

import java.util.Locale

private val ptBR: Locale = Locale.forLanguageTag("pt-BR")

/** 12.0 → "12" | 2.5 → "2,5" | 3.75 → "3,75" */
fun formatarMetros(valor: Float): String =
    if (valor % 1f == 0f) {
        valor.toInt().toString()
    } else {
        String.format(ptBR, "%.2f", valor).trimEnd('0').trimEnd(',')
    }

/** plural(1, "item", "itens") → "1 item" | plural(5, ...) → "5 itens" */
fun plural(quantidade: Int, singular: String, plural: String): String =
    "$quantidade ${if (quantidade == 1) singular else plural}"

/** Aceita "2,5" ou "2.5" digitado pelo usuário */
fun lerDecimal(texto: String): Float? =
    texto.trim().replace(',', '.').toFloatOrNull()
