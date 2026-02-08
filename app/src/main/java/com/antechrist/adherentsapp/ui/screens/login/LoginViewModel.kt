package com.antechrist.adherentsapp.ui.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.Role
import com.antechrist.adherentsapp.domain.model.isStaff
import com.antechrist.adherentsapp.domain.model.roleFromString
import com.antechrist.adherentsapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface LoginNav {
    data object None : LoginNav
    data object GoList : LoginNav
    data object GoAccessDenied : LoginNav
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    private val _nav = MutableStateFlow<LoginNav>(LoginNav.None)
    val nav: StateFlow<LoginNav> = _nav.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun onEmailChange(v: String) = _ui.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String) = _ui.update { it.copy(password = v, error = null) }

    fun signIn() {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password

        if (email.isEmpty() || pass.isEmpty()) {
            _ui.update { it.copy(error = "Email et mot de passe requis.") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                // 1) Connexion (ton repo fait déjà le refresh des claims côté serveur si besoin)
                authRepo.signIn(email, pass)

                // 2) Récupère un ID token FRAIS pour lire le claim "role"
                val token = auth.currentUser?.getIdToken(true)?.await()
                // 🔍 LOGS DE DÉBOGAGE - DÉBUT
                Log.d("LoginViewModel", "==========================================")
                Log.d("LoginViewModel", "=== DÉBUT LOGS LOGIN ===")
                Log.d("LoginViewModel", "User UID: ${auth.currentUser?.uid}")
                Log.d("LoginViewModel", "User Email: ${auth.currentUser?.email}")
                Log.d("LoginViewModel", "Email verified: ${auth.currentUser?.isEmailVerified}")
                Log.d("LoginViewModel", "ALL Token claims: ${token?.claims}")
                Log.d("LoginViewModel", "Role claim: ${token?.claims?.get("role")}")
                Log.d("LoginViewModel", "Parent claim: ${token?.claims?.get("parent")}")
                Log.d("LoginViewModel", "IsGuardian claim: ${token?.claims?.get("isGuardian")}")
                Log.d("LoginViewModel", "Email from token: ${token?.claims?.get("email")}")
                Log.d("LoginViewModel", "Email lowercase: ${token?.claims?.get("email")?.toString()?.lowercase()}")
                Log.d("LoginViewModel", "=== FIN LOGS LOGIN ===")
                Log.d("LoginViewModel", "==========================================")
                // 🔍 LOGS DE DÉBOGAGE - FIN
                val rawRole = token?.claims?.get("role")?.toString()
                val role: Role = roleFromString(rawRole)

                // 3) Gating : staff OU (trésorier / secrétaire) = accès
                val authorized =
                    role.isStaff() || role == Role.TRESORIER || role == Role.SECRETAIRE

                Log.d("LoginViewModel", "Role parsed: $role")
                Log.d("LoginViewModel", "Authorized for admin: $authorized")

                _ui.update { it.copy(loading = false) }
                _nav.value = if (authorized) LoginNav.GoList else LoginNav.GoAccessDenied
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error during sign in", e)
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur de connexion") }
            }
        }
    }

    fun consumeNav() { _nav.value = LoginNav.None }
}
