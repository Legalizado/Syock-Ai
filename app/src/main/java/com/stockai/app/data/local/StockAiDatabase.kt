package com.stockai.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Usuario::class, Ambiente::class, Prateleira::class, Gondola::class, Item::class],
    version = 1,
    exportSchema = false
)
abstract class StockAiDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun ambienteDao(): AmbienteDao
    abstract fun prateleiraDao(): PrateleiraDao
    abstract fun gondolaDao(): GondolaDao
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var instancia: StockAiDatabase? = null

        /** Garante que o app inteiro use um único banco (padrão Singleton) */
        fun getInstance(context: Context): StockAiDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockAiDatabase::class.java,
                    "stockai.db"
                )
                    // Durante o desenvolvimento: se mudarmos as tabelas e subirmos a
                    // "version", o banco é recriado do zero em vez de dar erro.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}