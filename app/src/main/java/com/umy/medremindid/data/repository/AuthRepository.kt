package com.umy.medremindid.data.repository

import com.umy.medremindid.data.local.dao.UserDao
import com.umy.medremindid.data.local.entity.UserEntity
import com.umy.medremindid.data.session.SessionManager
import com.umy.medremindid.security.PasswordHasher
import java.time.LocalDate

sealed class AuthResult {
    data class Success(val userId: Long) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        dateOfBirth: LocalDate? = null,
        gender: String? = null
    ): AuthResult {
        val trimmedEmail = email.trim().lowercase()

        if (fullName.isBlank()) return AuthResult.Error("Nama tidak boleh kosong.")
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) return AuthResult.Error("Email tidak valid.")
        if (password.length < 6) return AuthResult.Error("Password minimal 6 karakter.")

        val existing = userDao.findByEmail(trimmedEmail)
        if (existing != null) return AuthResult.Error("Email sudah terdaftar.")

        val hash = PasswordHasher.hash(password.toCharArray())

        val id = userDao.insert(
            UserEntity(
                fullName = fullName.trim(),
                email = trimmedEmail,
                passwordHash = hash,
                dateOfBirth = dateOfBirth,
                gender = gender
            )
        )

        sessionManager.setLoggedIn(id)
        return AuthResult.Success(id)
    }

    suspend fun login(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim().lowercase()
        val user = userDao.findByEmail(trimmedEmail)
            ?: return AuthResult.Error("Email atau password salah.")

        val ok = PasswordHasher.verify(password.toCharArray(), user.passwordHash)
        if (!ok) return AuthResult.Error("Email atau password salah.")

        sessionManager.setLoggedIn(user.userId)
        return AuthResult.Success(user.userId)
    }

    suspend fun logout() {
        sessionManager.logout()
    }
}
