package com.stockai.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Hierarquia do StockAi:
 * Usuario → Ambiente → Prateleira → Gondola → Item
 *
 * Cada nível aponta para o de cima (chave estrangeira) com CASCADE:
 * se você apagar um ambiente, as prateleiras, gôndolas e itens dele somem junto.
 */

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["email"], unique = true)] // não permite e-mail repetido
)
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val email: String,
    val senhaHash: String // nunca guardamos a senha pura, só o hash (Fase 3)
)

@Entity(
    tableName = "ambientes",
    foreignKeys = [ForeignKey(
        entity = Usuario::class,
        parentColumns = ["id"],
        childColumns = ["usuarioId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("usuarioId")]
)
data class Ambiente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val nome: String,          // ex.: "Almoxarifado"
    val largura: Float,        // em metros (eixo X da planta)
    val comprimento: Float,    // em metros (eixo Y da planta)
    val criadoEm: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "prateleiras",
    foreignKeys = [ForeignKey(
        entity = Ambiente::class,
        parentColumns = ["id"],
        childColumns = ["ambienteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("ambienteId")]
)
data class Prateleira(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ambienteId: Long,
    val nome: String,          // ex.: "Prateleira B" ou só "B"
    val posX: Float,           // canto superior esquerdo na planta, em metros
    val posY: Float,
    val largura: Float,        // em metros
    val profundidade: Float,   // em metros
    val altura: Float,         // em metros (usado no 3D)
    val rotacao: Int = 0       // 0 ou 90 graus (prateleira deitada ou em pé na planta)
)

@Entity(
    tableName = "gondolas",
    foreignKeys = [ForeignKey(
        entity = Prateleira::class,
        parentColumns = ["id"],
        childColumns = ["prateleiraId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("prateleiraId")]
)
data class Gondola(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prateleiraId: Long,
    val codigo: String,        // ex.: "B3"
    val nome: String,          // ex.: "Parafusos"
    val nivel: Int             // 1 = nível de baixo (usado no 3D para a altura)
)

@Entity(
    tableName = "itens",
    foreignKeys = [ForeignKey(
        entity = Gondola::class,
        parentColumns = ["id"],
        childColumns = ["gondolaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("gondolaId"), Index("nome")] // índice no nome deixa a busca mais rápida
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gondolaId: Long,
    val nome: String,          // ex.: "Parafuso M8 × 30 mm"
    val quantidade: Int,
    val unidade: String = "un.",
    val descricao: String? = null,
    val atualizadoEm: Long = System.currentTimeMillis()
)
