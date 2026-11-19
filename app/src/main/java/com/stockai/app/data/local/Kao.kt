package com.stockai.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/*
 * DAO = Data Access Object: onde ficam as operações no banco.
 * - Funções "suspend" fazem uma operação e terminam (inserir, apagar...).
 * - Funções que devolvem Flow ficam "observando": quando o banco muda,
 *   a tela é atualizada sozinha.
 * O Room confere o SQL na hora de compilar: se tiver erro, o build avisa.
 */

@Dao
interface UsuarioDao {
    @Insert // dá erro se o e-mail já existir (índice único)
    suspend fun inserir(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int
}

@Dao
interface AmbienteDao {
    @Query(
        """
        SELECT a.*,
            (SELECT COUNT(*) FROM prateleiras p WHERE p.ambienteId = a.id) AS totalPrateleiras,
            (SELECT COUNT(*) FROM itens i
                JOIN gondolas g ON i.gondolaId = g.id
                JOIN prateleiras p ON g.prateleiraId = p.id
                WHERE p.ambienteId = a.id) AS totalItens
        FROM ambientes a
        WHERE a.usuarioId = :usuarioId
        ORDER BY a.nome
        """
    )
    fun observarResumos(usuarioId: Long): Flow<List<AmbienteResumo>>

    @Query("SELECT * FROM ambientes WHERE id = :id")
    fun observarPorId(id: Long): Flow<Ambiente?>

    @Insert
    suspend fun inserir(ambiente: Ambiente): Long

    @Update
    suspend fun atualizar(ambiente: Ambiente)

    @Delete
    suspend fun apagar(ambiente: Ambiente)
}

@Dao
interface PrateleiraDao {
    @Query("SELECT * FROM prateleiras WHERE ambienteId = :ambienteId ORDER BY nome")
    fun observarPorAmbiente(ambienteId: Long): Flow<List<Prateleira>>

    @Query("SELECT * FROM prateleiras WHERE id = :id")
    fun observarPorId(id: Long): Flow<Prateleira?>

    @Insert
    suspend fun inserir(prateleira: Prateleira): Long

    @Update
    suspend fun atualizar(prateleira: Prateleira)

    @Delete
    suspend fun apagar(prateleira: Prateleira)
}

@Dao
interface GondolaDao {
    @Query(
        """
        SELECT g.*,
            (SELECT COUNT(*) FROM itens i WHERE i.gondolaId = g.id) AS totalItens
        FROM gondolas g
        WHERE g.prateleiraId = :prateleiraId
        ORDER BY g.nivel DESC, g.codigo
        """
    )
    fun observarResumos(prateleiraId: Long): Flow<List<GondolaResumo>>

    // NOVO na Fase 6
    @Query("SELECT * FROM gondolas WHERE id = :id")
    fun observarPorId(id: Long): Flow<Gondola?>

    // NOVO na Fase 8: todas as gôndolas de um ambiente (para montar o 3D)
    @Query(
        """
        SELECT g.*,
            (SELECT COUNT(*) FROM itens i WHERE i.gondolaId = g.id) AS totalItens
        FROM gondolas g
        JOIN prateleiras p ON g.prateleiraId = p.id
        WHERE p.ambienteId = :ambienteId
        """
    )
    fun observarResumosPorAmbiente(ambienteId: Long): Flow<List<GondolaResumo>>

    @Insert
    suspend fun inserir(gondola: Gondola): Long

    @Update
    suspend fun atualizar(gondola: Gondola)

    @Delete
    suspend fun apagar(gondola: Gondola)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM itens WHERE gondolaId = :gondolaId ORDER BY nome")
    fun observarPorGondola(gondolaId: Long): Flow<List<Item>>

    @Query("SELECT * FROM itens WHERE id = :id")
    fun observarPorId(id: Long): Flow<Item?>

    /** Busca em todos os ambientes do usuário, devolvendo o caminho completo */
    @Query(
        """
        SELECT i.id AS itemId, i.nome AS itemNome, i.quantidade, i.unidade,
               g.id AS gondolaId, g.codigo AS gondolaCodigo, g.nome AS gondolaNome,
               p.id AS prateleiraId, p.nome AS prateleiraNome,
               a.id AS ambienteId, a.nome AS ambienteNome
        FROM itens i
        JOIN gondolas g ON i.gondolaId = g.id
        JOIN prateleiras p ON g.prateleiraId = p.id
        JOIN ambientes a ON p.ambienteId = a.id
        WHERE a.usuarioId = :usuarioId
          AND i.nome LIKE '%' || :termo || '%'
        ORDER BY i.nome
        LIMIT 50
        """
    )
    fun buscar(usuarioId: Long, termo: String): Flow<List<ResultadoBusca>>

    @Insert
    suspend fun inserir(item: Item): Long

    @Update
    suspend fun atualizar(item: Item)

    @Delete
    suspend fun apagar(item: Item)
}
