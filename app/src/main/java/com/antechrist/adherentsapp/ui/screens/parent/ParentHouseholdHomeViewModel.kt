package com.antechrist.adherentsapp.ui.screens.parent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.domain.repository.AuthRepository
import com.antechrist.adherentsapp.domain.usecase.GetGuardianForCurrentUserUseCase
import com.antechrist.adherentsapp.domain.usecase.GetMyHouseholdAdherentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import javax.inject.Inject

sealed class ParentHomeUiState {
    object Loading : ParentHomeUiState()
    data class Success(
        val guardian: Guardian,
        val adherents: List<Adherent>
    ) : ParentHomeUiState()
    data class Error(val message: String) : ParentHomeUiState()
    object NoGuardianLinked : ParentHomeUiState()
}

@HiltViewModel
class ParentHouseholdHomeViewModel @Inject constructor(
    private val getGuardianForCurrentUserUseCase: GetGuardianForCurrentUserUseCase,
    private val getMyHouseholdAdherentsUseCase: GetMyHouseholdAdherentsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ParentHomeUiState>(ParentHomeUiState.Loading)
    val uiState: StateFlow<ParentHomeUiState> = _uiState.asStateFlow()

    init {
        loadHouseholdData()
    }

//    private fun loadHouseholdData() {
//        viewModelScope.launch {
//            try {
//                // ✅ Force refresh ID token pour récupérer les custom claims à jour
//                try {
//                    Firebase.auth.currentUser?.getIdToken(true)?.await()
//                } catch (_: Exception) {
//                    // pas bloquant
//                }
//
//                // 1. Récupérer le guardian du parent connecté
//                val guardian = getGuardianForCurrentUserUseCase()
//
//                if (guardian == null) {
//                    Log.w("ParentHomeVM", "No guardian found for current user")
//                    _uiState.value = ParentHomeUiState.NoGuardianLinked
//                    return@launch
//                }
//
//                Log.d("ParentHomeVM", "Guardian loaded: ${guardian.nom} ${guardian.prenom}")
//
//                // 2. Écouter les adhérents du foyer
//                getMyHouseholdAdherentsUseCase().collect { adherents ->
//                    Log.d("ParentHomeVM", "Adherents loaded: ${adherents.size} members")
//                    _uiState.value = ParentHomeUiState.Success(
//                        guardian = guardian,
//                        adherents = adherents.sortedBy { it.prenom } // Trier par prénom
//                    )
//                }
//
//            } catch (e: Exception) {
//                Log.e("ParentHomeVM", "Error loading household data", e)
//                _uiState.value = ParentHomeUiState.Error(
//                    message = e.message ?: "Erreur lors du chargement des données"
//                )
//            }
//        }
//    }
    private fun loadHouseholdData() {
        viewModelScope.launch {
            val TAG = "ParentHomeTrace"
            try {
                Log.d(TAG, "START loadHouseholdData()")

                Log.d(TAG, "STEP 1 -> getGuardianForCurrentUserUseCase()")
                val guardian = getGuardianForCurrentUserUseCase()
                Log.d(TAG, "STEP 1 OK -> guardian=${guardian?.id} ${guardian?.nom} ${guardian?.prenom}")

                if (guardian == null) {
                    Log.w(TAG, "No guardian linked -> NoGuardianLinked")
                    _uiState.value = ParentHomeUiState.NoGuardianLinked
                    return@launch
                }

                Log.d(TAG, "STEP 2 -> collect getMyHouseholdAdherentsUseCase() START")
                getMyHouseholdAdherentsUseCase().collect { adherents ->
                    Log.d(TAG, "STEP 2 OK -> adherents.size=${adherents.size}")
                    _uiState.value = ParentHomeUiState.Success(
                        guardian = guardian,
                        adherents = adherents.sortedBy { it.prenom }
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "FAILED loadHouseholdData(): ${e.message}", e)
                _uiState.value = ParentHomeUiState.Error(e.message ?: "Erreur chargement")
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                onComplete()
            } catch (e: Exception) {
                Log.e("ParentHomeVM", "Error during logout", e)
                onComplete() // Logout quand même
            }
        }
    }

    fun retry() {
        _uiState.value = ParentHomeUiState.Loading
        loadHouseholdData()
    }
}