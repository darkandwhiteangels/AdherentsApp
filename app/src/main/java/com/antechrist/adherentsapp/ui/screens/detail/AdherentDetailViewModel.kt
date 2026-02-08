package com.antechrist.adherentsapp.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import com.antechrist.adherentsapp.domain.usecase.GetAdherentStreamByIdUseCase
import com.antechrist.adherentsapp.domain.usecase.GetGuardianByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// MODIFIÉ : Ajout du champ pour gérer la confirmation de suppression.
data class DetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val adherent: Adherent? = null,
    val primaryGuardian: Guardian? = null,
    val askDeleteConfirmation: Boolean = false // <-- LIGNE AJOUTÉE
)

@HiltViewModel
class AdherentDetailViewModel @Inject constructor(
    private val getById: GetAdherentStreamByIdUseCase,
    private val repo: AdherentsRepository,
    private val uploadAdherentPhoto: com.antechrist.adherentsapp.domain.usecase.UploadAdherentPhoto,
    private val getGuardianById: GetGuardianByIdUseCase,
    // refactor guardian
    private val linkAuthUidToGuardian: com.antechrist.adherentsapp.domain.usecase.LinkAuthUidToGuardianUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            getById(id)
                .onStart { _state.update { it.copy(loading = true, error = null) } }
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collect { a ->
                    val guardian: Guardian? = try {
                        val pid = a?.primaryGuardianId
                        if (pid.isNullOrBlank()) {
                            null
                        } else {
                            getGuardianById(pid)
                        }
                    } catch (_: Exception) {
                        null
                    }

                    _state.update {
                        it.copy(
                            loading = false,
                            adherent = a,
                            primaryGuardian = guardian,
                            error = null
                        )
                    }
                }
        }
    }

    // MODIFIÉ : Ajout de ces trois fonctions pour gérer la boîte de dialogue.
    fun onTryDelete() {
        _state.update { it.copy(askDeleteConfirmation = true) }
    }

    fun onCancelDelete() {
        _state.update { it.copy(askDeleteConfirmation = false) }
    }

    /** Supprime et renvoie true si OK, false si refus/erreur. */
    suspend fun deleteSafe(id: String): Boolean {
        // Remet l'état de la confirmation à false au début de la tentative
        _state.update { it.copy(askDeleteConfirmation = false) }
        return try {
            repo.delete(id)
            true
        } catch (e: Exception) {
            val msg = e.message ?: "Suppression refusée"
            val nice = if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                "Suppression refusée (droits insuffisants)."
            } else msg
            _state.update { it.copy(error = nice) }
            false
        }
    }

    private fun seasonKey(): String {
        val zone = java.time.ZoneId.of("Europe/Paris")
        val d = java.time.LocalDate.now(zone)
        val y = d.year
        return if (d.monthValue >= 9) "$y-${y+1}" else "${y-1}-$y"
    }

    fun onPhotoPicked(adherentId: String, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                uploadAdherentPhoto(adherentId, uri)
            } catch (e: Exception) {
                // _state.update { it.copy(error = "Upload photo: ${e.message}") }
            }
        }
    }

    // Refactor Guardian
    /**
     * Lie le guardian affiché (primaryGuardian) au compte Firebase Auth courant.
     * Appelé après activation / invitation du parent.
     */
    fun linkPrimaryGuardianToCurrentUser(authUid: String) {
        val guardian = state.value.primaryGuardian ?: return
        if (authUid.isBlank()) return

        viewModelScope.launch {
            try {
                linkAuthUidToGuardian(guardian.id, authUid)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Impossible de lier le compte au responsable")
                }
            }
        }
    }

}
