package com.antechrist.adherentsapp.domain.repository

import com.antechrist.adherentsapp.domain.model.Role

data class AuthStatus(
    val isAuthenticated: Boolean,
    val isAdmin: Boolean,
    val role: Role?,
    val email: String?
)

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthStatus
    suspend fun signOut()
    suspend fun refreshStatus(): AuthStatus
    fun currentEmailOrNull(): String?
}
