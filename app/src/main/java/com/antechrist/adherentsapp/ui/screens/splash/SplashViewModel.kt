//package com.antechrist.adherentsapp.ui.screens.splash
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.antechrist.adherentsapp.domain.repository.AuthRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//sealed interface SplashNav { data object None: SplashNav; data object ToLogin: SplashNav; data object ToList: SplashNav; data object ToDenied: SplashNav }
//
//@HiltViewModel
//class SplashViewModel @Inject constructor(
//    private val auth: AuthRepository
//) : ViewModel() {
//    private val _nav = MutableStateFlow<SplashNav>(SplashNav.None)
//    val nav: StateFlow<SplashNav> = _nav
//
//    fun check() {
//        viewModelScope.launch {
//            try {
//                val st = auth.refreshStatus() // lit claims
//                _nav.value = when {
//                    !st.isAuthenticated -> SplashNav.ToLogin
//                    st.isAdmin -> SplashNav.ToList
//                    else -> SplashNav.ToDenied
//                }
//            } catch (_: Exception) {
//                _nav.value = SplashNav.ToLogin
//            }
//        }
//    }
//}
package com.antechrist.adherentsapp.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.Role
import com.antechrist.adherentsapp.domain.model.isStaff
import com.antechrist.adherentsapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Navigation destinations pour le splash screen
// ═══════════════════════════════════════════════════════════════════════════
sealed interface SplashNav {
    data object None : SplashNav
    data object ToLogin : SplashNav
    data object ToList : SplashNav
    data object ToDenied : SplashNav
    data object ToParentHome : SplashNav  // ✅ AJOUT pour routing parent
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: AuthRepository  // ✅ Utilise AuthRepository (PAS FirebaseAuth)
) : ViewModel() {

    private val _nav = MutableStateFlow<SplashNav>(SplashNav.None)
    val nav: StateFlow<SplashNav> = _nav

    // ═══════════════════════════════════════════════════════════════════════
    // Vérifie l'authentification et redirige selon le rôle
    // ═══════════════════════════════════════════════════════════════════════
    fun check() {
        viewModelScope.launch {
            try {
                // Rafraîchit le statut Auth (lit les custom claims)
                val status = auth.refreshStatus()

                // Routing basé sur le rôle
                _nav.value = when {
                    // Pas authentifié → Login
                    !status.isAuthenticated -> SplashNav.ToLogin

                    // Pas de rôle → Login (sécurité)
                    status.role == null -> SplashNav.ToLogin

                    // PARENT → Espace Parent
                    status.role == Role.PARENT -> SplashNav.ToParentHome

                    // STAFF (admin, super_admin, professeur) → Liste adhérents
                    status.role.isStaff() -> SplashNav.ToList

                    // BUREAU (trésorier, secrétaire) → Liste adhérents
                    status.role == Role.TRESORIER || status.role == Role.SECRETAIRE -> SplashNav.ToList

                    // Autres rôles → Accès refusé
                    else -> SplashNav.ToDenied
                }
            } catch (_: Exception) {
                // En cas d'erreur → Login
                _nav.value = SplashNav.ToLogin
            }
        }
    }
}