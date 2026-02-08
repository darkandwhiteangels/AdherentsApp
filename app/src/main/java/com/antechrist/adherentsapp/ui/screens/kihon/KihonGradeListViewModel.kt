package com.antechrist.adherentsapp.ui.screens.kihon

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.usecase.GetKihonSequencesByGrade
import com.antechrist.adherentsapp.domain.usecase.SaveKihonSequence
import com.antechrist.adherentsapp.domain.usecase.DeleteKihonSequence
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class KihonGradeListViewModel @Inject constructor(
    private val getByGrade: GetKihonSequencesByGrade,
    private val saveSequence: SaveKihonSequence,
    private val deleteSequenceUc: DeleteKihonSequence,      // ⬅️ nouveau use case
    private val auth: FirebaseAuth
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val items: List<KihonSequence> = emptyList(),
        val query: String = "",
        val statusFilter: KihonSequence.Status? = null,
        val error: String? = null
    )

    private val _gradeKey = MutableStateFlow<String?>(null)
    private val _ui = MutableStateFlow(UiState(loading = true))
    val ui: StateFlow<UiState> = _ui

    private val TAG = "KihonGradeListVM"

    init {
        viewModelScope.launch {
            _gradeKey
                .flatMapLatest { gk ->
                    if (gk.isNullOrBlank()) MutableStateFlow(emptyList())
                    else getByGrade(gk)
                }
                .combine(_ui.map { it.query to it.statusFilter }) { list, (q, filt) ->
                    list
                        .filter { seq ->
                            (filt == null || seq.status == filt) &&
                                    (q.isBlank() || seq.name.contains(q, ignoreCase = true))
                        }
                        .sortedWith(
                            compareBy<KihonSequence> { it.status.ordinal }
                                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                        )
                }
                .collect { items ->
                    _ui.update { it.copy(loading = false, items = items, error = null) }
                }
        }
    }

    fun setGradeKey(gradeKey: String) {
        if (_gradeKey.value != gradeKey) {
            _ui.update { it.copy(loading = true) }
            _gradeKey.value = gradeKey
        }
    }

    fun setQuery(q: String) { _ui.update { it.copy(query = q) } }
    fun clearQuery() = setQuery("")
    fun setStatusFilter(f: KihonSequence.Status?) { _ui.update { it.copy(statusFilter = f) } }

    /**
     * Crée une séquence brouillon pour le grade courant et renvoie l'ID.
     * Échoue si le grade n'est pas défini ou si Firestore refuse l'écriture.
     */
    suspend fun createDraftSequence(): Result<String> {
        val gradeKey = _gradeKey.value ?: return Result.failure(IllegalStateException("Grade invalide"))
        val uid = auth.currentUser?.uid ?: "unknown"
        val now = System.currentTimeMillis()
        val newId = java.util.UUID.randomUUID().toString()

        val draft = KihonSequence(
            id = newId,
            name = "Séquence ",
            gradeKey = gradeKey,
            version = "KIHON_V1",
            status = KihonSequence.Status.DRAFT,
            objective = null,
            tags = emptyList(),
            steps = emptyList(),
            isExamRequired = false,
            authorId = uid,
            createdAt = now,
            updatedAt = now,
            publishedAt = null,
            publishedBy = null
        )

        val res = saveSequence(draft)
        return res
            .onSuccess { Log.d(TAG, "createDraftSequence: saved id=$newId") }
            .onFailure { e -> Log.e(TAG, "createDraftSequence: FAILED ${e.message}", e) }
            .map { newId }
    }

    /**
     * Supprime une séquence par ID.
     * Le flux _getByGrade_ mettra automatiquement à jour la liste après suppression.
     */
    suspend fun deleteSequence(id: String): Result<Unit> {
        return deleteSequenceUc(id)
            .onSuccess { Log.d(TAG, "deleteSequence: OK id=$id") }
            .onFailure { e -> Log.e(TAG, "deleteSequence: FAILED ${e.message}", e) }
    }
}
