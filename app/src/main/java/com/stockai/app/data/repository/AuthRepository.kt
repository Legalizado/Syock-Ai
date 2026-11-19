package com.stockai.app.data.repository

import android.content.Context
import android.util.Base64
import com.stockai.app.data.local.Usuario
import com.stockai.app.data.local.UsuarioDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cuida de tudo que envolve conta: cadastro, login, logout e sessão.
 * As telas nunca falam direto com o DAO; falam com o repositório.
 */
class AuthRepository(
    private val usuarioDao: UsuarioDao,
    context: Context
) {
    // Guarda quem está logado, para não pedir login toda vez que abrir o app
    private val prefs = context.getSharedPreferences("stockai_sessao", Context.MODE_PRIVATE)

    val usuarioLogadoId: Long?
        get() = prefs.getLong(CHAVE_USUARIO, -1L).takeIf { it != -1L }

    suspend fun cadastrar(nome: String, email: String, senha: String): Result<Usuario> {
        val emailNormalizado = email.trim().lowercase()
        if (usuarioDao.buscarPorEmail(emailNormalizado) != null) {
            return Result.failure(IllegalArgumentException("Já existe uma conta com esse e-mail"))
        }
        val usuario = Usuario(
            nome = nome.trim(),
            email = emailNormalizado,
            senhaHash = gerarHash(senha)
        )
        val id = usuarioDao.inserir(usuario)
        salvarSessao(id)
        return Result.success(usuario.copy(id = id))
    }

    suspend fun entrar(email: String, senha: String): Result<Usuario> {
        val usuario = usuarioDao.buscarPorEmail(email.trim().lowercase())
        if (usuario == null || !conferirSenha(senha, usuario.senhaHash)) {
            // Mesma mensagem nos dois casos: não revela se o e-mail existe
            return Result.failure(IllegalArgumentException("E-mail ou senha incorretos"))
        }
        salvarSessao(usuario.id)
        return Result.success(usuario)
    }

    fun sair() {
        prefs.edit().remove(CHAVE_USUARIO).apply()
    }

    private fun salvarSessao(id: Long) {
        prefs.edit().putLong(CHAVE_USUARIO, id).apply()
    }

    // ---------- Segurança da senha ----------
    // PBKDF2: algoritmo próprio para senhas. Cada usuário recebe um "salt"
    // aleatório, então duas senhas iguais geram hashes diferentes.

    private suspend fun gerarHash(senha: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(senha, salt)
        return "${salt.paraBase64()}:${hash.paraBase64()}"
    }

    private suspend fun conferirSenha(senha: String, armazenado: String): Boolean {
        val partes = armazenado.split(":")
        if (partes.size != 2) return false
        val salt = Base64.decode(partes[0], Base64.NO_WRAP)
        val esperado = Base64.decode(partes[1], Base64.NO_WRAP)
        return MessageDigest.isEqual(pbkdf2(senha, salt), esperado)
    }

    // Cálculo pesado de propósito: roda fora da thread da tela para não travar
    private suspend fun pbkdf2(senha: String, salt: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            val spec = PBEKeySpec(senha.toCharArray(), salt, ITERACOES, 256)
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        }

    private fun ByteArray.paraBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private companion object {
        const val CHAVE_USUARIO = "usuario_id"
        const val ITERACOES = 100_000
    }
}
