package com.antechrist.adherentsapp.ui.role

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.Role
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "RoleLog"

/**
 * ViewModel centralisant le rôle applicatif de l'utilisateur courant (basé sur le custom claim "role").
 *
 * - Lit le claim "role" depuis l'ID token Firebase (forceRefresh configurable).
 * - Émet un StateFlow<Role?> : null si non connecté, sinon un Role mappé.
 * - Écoute les changements d'auth (login/logout) via AuthStateListener et rafraîchit automatiquement.
 *
 * Rôles supportés côté claims (sans accents) :
 *   "member", "bureau", "admin", "super_admin" | "superadmin" | "super-admin",
 *   "tresorier", "secretaire"
 */
@HiltViewModel
class RoleViewModel @Inject constructor(
    // On peut injecter FirebaseAuth via Hilt si tu as un module, sinon on prend l'instance statique
    // private val auth: FirebaseAuth
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _role = MutableStateFlow<Role?>(null)
    val role: StateFlow<Role?> = _role.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Pour debug/inspection : dernière valeur brute du claim "role"
    private val _rawClaimRole = MutableStateFlow<String?>(null)
    val rawClaimRole: StateFlow<String?> = _rawClaimRole.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { fb ->
        val user = fb.currentUser
        Log.d(TAG, "AuthStateListener: user=${user?.uid ?: "null"}")
        if (user == null) {
            _role.value = null
            _rawClaimRole.value = null
            _error.value = null
            _loading.value = false
        } else {
            // On ne force pas systématiquement ici (évite spam réseau) ; on laisse refresh(false)
            refresh(force = false)
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        // Premier refresh — on force pour récupérer les claims à jour au démarrage.
        refresh(force = true)
    }

    /**
     * Rafraîchit le rôle depuis les custom claims Firebase.
     * @param force true => force un refresh du token (getIdToken(true))
     */
    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _role.value = null
                _rawClaimRole.value = null
                _error.value = null
                _loading.value = false
                Log.d(TAG, "refresh(): no user, role=null")
                return@launch
            }

            try {
                _loading.value = true
                _error.value = null

                val token = user.getIdToken(force).await()
                val claim = token.claims["role"]?.toString()
                _rawClaimRole.value = claim
                val mapped = mapClaimToRole(claim)

                _role.value = mapped

                Log.d(TAG, "claims.role=$claim -> mapped=${mapped?.name}")
            } catch (e: Exception) {
                Log.w(TAG, "refresh(): failed to load role: ${e.message}", e)
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Mapping robuste du string claim => Role enum (insensible à la casse / variantes).
     * Retourne MEMBER par défaut si inconnu.
     */
    private fun mapClaimToRole(claim: String?): Role? {
        if (claim.isNullOrBlank()) return Role.MEMBER
        return when (claim.trim().lowercase()) {
            "member", "membre", "adherent", "adhérent" -> Role.MEMBER
            "parent" -> Role.PARENT
            "bureau" -> Role.BUREAU
            "membre_bureau" -> Role.MEMBRE_BUREAU
            "admin" -> Role.ADMIN
            "super_admin", "superadmin", "super-admin" -> Role.SUPER_ADMIN
            "tresorier", "trésorier" -> Role.TRESORIER
            "secretaire", "secrétaire" -> Role.SECRETAIRE
            "professeur" -> Role.PROFESSEUR
            "juge_grade", "juge", "judge", "juge-grade" -> Role.JUGE_GRADE
            else -> Role.MEMBER
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}
