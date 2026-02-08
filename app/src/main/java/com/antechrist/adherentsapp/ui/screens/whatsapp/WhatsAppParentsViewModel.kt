package com.antechrist.adherentsapp.ui.screens.whatsapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ParentContact(
    val id: String,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String
)

data class WhatsAppParentsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val parents: List<ParentContact> = emptyList(),
    val copiedMessage: String? = null,
    val selectedIds: Set<String> = emptySet()
)


@HiltViewModel
class WhatsAppParentsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatsAppParentsUiState(isLoading = true))
    val uiState: StateFlow<WhatsAppParentsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, copiedMessage = null)
            try {
                val snap = firestore.collection("guardians").get().await()

                val parents = snap.documents.mapNotNull { doc ->
                    val nom = (doc.getString("nom") ?: "").trim()
                    val prenom = (doc.getString("prenom") ?: "").trim()
                    val telephone = (doc.getString("telephone") ?: "").trim()
                    val email = (doc.getString("email") ?: "").trim()

                    if (telephone.isBlank()) return@mapNotNull null

                    ParentContact(
                        id = doc.id,
                        nom = nom,
                        prenom = prenom,
                        telephone = telephone,
                        email = email
                    )
                }.sortedWith(
                    compareBy<ParentContact> { it.nom.lowercase() }
                        .thenBy { it.prenom.lowercase() }
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    parents = parents,
                    selectedIds = emptySet()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Erreur lors du chargement"
                )
            }
        }
    }

    fun clearCopiedMessage() {
        _uiState.value = _uiState.value.copy(copiedMessage = null)
    }

    fun buildTexteLisible(): String {
        val parents = _uiState.value.parents
        return parents.joinToString(separator = "\n") { p ->
            val identite = listOf(p.nom, p.prenom).filter { it.isNotBlank() }.joinToString(" ")
            if (identite.isBlank()) p.telephone else "$identite - ${p.telephone}"
        }
    }

    fun onCopiedListeLisible() {
        _uiState.value = _uiState.value.copy(
            copiedMessage = "Liste copiée"
        )
    }

    fun buildNumerosPourSms(): String {
        // La plupart des applis SMS acceptent les destinataires séparés par ";".
        // (certaines acceptent aussi ",", mais ";" est souvent le plus compatible)
        return _uiState.value.parents
            .map { it.telephone.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(separator = ";")
    }

    fun toggleSelect(id: String) {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _uiState.value = _uiState.value.copy(selectedIds = current)
    }

    fun setSelectAll(selectAll: Boolean) {
        val allIds = _uiState.value.parents.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedIds = if (selectAll) allIds else emptySet()
        )
    }

    fun selectedCount(): Int = _uiState.value.selectedIds.size

    fun buildNumerosPourSmsSelection(): String {
        val selected = _uiState.value.selectedIds
        return _uiState.value.parents
            .asSequence()
            .filter { selected.contains(it.id) }
            .map { it.telephone.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(separator = ";")
    }

    fun buildSmsBatches(batchSize: Int = 20): List<String> {
        val selected = _uiState.value.selectedIds
        val nums = _uiState.value.parents
            .asSequence()
            .filter { selected.contains(it.id) }
            .map { it.telephone.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        if (nums.isEmpty()) return emptyList()

        return nums.chunked(batchSize).map { chunk ->
            // séparateur compatible SMS
            chunk.joinToString(separator = ";")
        }
    }

    fun buildEmailsPourBccSelection(): String {
        val selected = _uiState.value.selectedIds
        return _uiState.value.parents
            .asSequence()
            .filter { selected.contains(it.id) }
            .map { it.email.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(separator = ",")
    }

}
