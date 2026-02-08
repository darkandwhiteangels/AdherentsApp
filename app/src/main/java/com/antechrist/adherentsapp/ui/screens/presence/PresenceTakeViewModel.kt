package com.antechrist.adherentsapp.ui.screens.presence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.data.firestore.PresenceDataSource
import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.model.PresenceRecord
import com.antechrist.adherentsapp.domain.usecase.GetAdherentsByGroup
import com.antechrist.adherentsapp.domain.usecase.StreamPresenceForGroupDate
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class SiteFilter { TOUS, PUSSAY, SACLAS }

sealed class PresenceEvent {
    object Saved : PresenceEvent()
    data class Error(val message: String) : PresenceEvent()
}

data class PresenceTakeUiState(
    val date: LocalDate = LocalDate.now(),
    val groupDisplay: String = "Enfants < 14 ans",
    val siteFilter: SiteFilter = SiteFilter.TOUS,
    val items: List<PresenceItem> = emptyList(), // ⚠️ déjà filtrés par site
    val loading: Boolean = true,
    val isSaving: Boolean = false,
)

@HiltViewModel
class PresenceTakeViewModel @Inject constructor(
    private val getAdherentsByGroup: GetAdherentsByGroup,
    private val streamPresence: StreamPresenceForGroupDate,
    private val presenceDS: PresenceDataSource,
    private val auth: FirebaseAuth
) : ViewModel() {

    val groups = listOf("Baby", "Enfants < 14 ans", "14+ / Adultes")

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val selectedGroup = MutableStateFlow(groups[1])

    // 🆕 filtre site (TOUS / PUSSAY / SACLAS)
    private val selectedSiteFilter = MutableStateFlow(SiteFilter.TOUS)

    /** Édits locaux (status uniquement). */
    private val edits = MutableStateFlow<Map<String, PresenceRecord>>(emptyMap())

    /** Flag pour empêcher les sauvegardes parallèles. */
    private val isSaving = MutableStateFlow(false)

    private val _events = MutableSharedFlow<PresenceEvent>()
    val events: SharedFlow<PresenceEvent> = _events.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<PresenceTakeUiState> =
        combine(selectedDate, selectedGroup, selectedSiteFilter) { date, group, site ->
            val dateKey = presenceDS.dateKey(date)
            val groupKey = presenceDS.groupKey(group)
            Triple(
                Triple(date, group, site),
                getAdherentsByGroup(group),
                streamPresence(dateKey, groupKey)
            )
        }.flatMapLatest { (triple, adherentFlow, flowPresence) ->
            val (date, group, site) = triple
            combine(
                adherentFlow,
                flowPresence,
                edits,
                isSaving
            ) { adherents: List<Adherent>, stored, localEdits, saving ->

                // 🔤 Tri par prénom (puis nom en cas d’égalité)
                val sorted = adherents.sortedWith(
                    compareBy<Adherent>(
                        { it.prenom.trim().lowercase() },
                        { it.nom.trim().lowercase() }
                    )
                )

                // Construction des items bruts
                val rawItems = sorted.map { a ->
                    val rec = localEdits[a.id] ?: stored[a.id]
                    PresenceItem(
                        id = a.id,
                        displayName = listOf(a.prenom, a.nom)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank { a.email ?: a.id },
                        status = (rec?.status ?: "absent").toStatusEnum(),
                        note = "",
                        beltCode = a.beltCode,
                        attribution = a.attribution // "pussay", "saclas", "encadrant", etc.
                    )
                }

                // 🆕 filtre site appliqué sur attribution
                val filteredItems = when (site) {
                    SiteFilter.TOUS -> rawItems
                    SiteFilter.PUSSAY -> rawItems.filter {
                        it.attribution?.lowercase() == "pussay"
                    }
                    SiteFilter.SACLAS -> rawItems.filter {
                        it.attribution?.lowercase() == "saclas"
                    }
                }

                PresenceTakeUiState(
                    date = date,
                    groupDisplay = group,
                    siteFilter = site,
                    items = filteredItems,
                    loading = false,
                    isSaving = saving
                )
            }.onStart {
                emit(
                    PresenceTakeUiState(
                        date = date,
                        groupDisplay = group,
                        siteFilter = site,
                        loading = true,
                        isSaving = isSaving.value
                    )
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PresenceTakeUiState())

    fun onGroupChanged(display: String) {
        selectedGroup.value = display
        edits.value = emptyMap()
    }

    fun onDateChanged(date: LocalDate) {
        selectedDate.value = date
        edits.value = emptyMap()
    }

    // 🆕 changement de filtre site
    fun onSiteFilterChanged(filter: SiteFilter) {
        selectedSiteFilter.value = filter
        // pas besoin de reset edits -> on garde les statuts saisis même si masqués
    }

    /** Met uniquement à jour le statut local (présent/absent). */
    fun updateStatus(adherentId: String, status: PresenceStatus) {
        val current = edits.value.toMutableMap()
        current[adherentId] = PresenceRecord(status = status.asString())
        edits.value = current
    }

    /** Sauvegarde: uniquement les visibles selon le filtre courant; bloque les doubles clics. */
    fun save() {
        if (isSaving.value) return

        viewModelScope.launch {
            isSaving.value = true
            try {
                val date = selectedDate.value
                val group = selectedGroup.value
                val site = selectedSiteFilter.value
                val dateKey = presenceDS.dateKey(date)
                val groupKey = presenceDS.groupKey(group)
                val adminUid = auth.currentUser?.uid

                // Récupère la liste filtrée actuellement affichée dans l'UI
                val visibleItems = ui.value.items

                // Si aucun edit manuel : on prend l'état affiché à l'écran pour ces visibles uniquement
                val toApply: Map<String, PresenceRecord> = edits.value.ifEmpty {
                    visibleItems.associate { item ->
                        item.id to PresenceRecord(status = item.status.asString())
                    }
                }.filterKeys { id -> visibleItems.any { it.id == id } }

                for ((adherentId, rec) in toApply) {
                    presenceDS.updateStatusWithTrial(
                        dateKey = dateKey,
                        groupKey = groupKey,
                        adherentId = adherentId,
                        newStatus = rec.status,
                        overrideUpdatedBy = adminUid,
                        groupDisplay = group
                    )
                }

                // on ne vide PAS les edits globaux => tu peux changer de filtre et re-sauvegarder l'autre site
                _events.emit(PresenceEvent.Saved)
            } catch (e: Exception) {
                _events.emit(PresenceEvent.Error(e.message ?: "Erreur de sauvegarde"))
            } finally {
                isSaving.value = false
            }
        }
    }
}

/* ===== Utils ===== */

private fun PresenceStatus.asString(): String = when (this) {
    PresenceStatus.Present -> "present"
    PresenceStatus.Absent  -> "absent"
}

private fun String.toStatusEnum(): PresenceStatus = when (this.lowercase()) {
    "present" -> PresenceStatus.Present
    "absent"  -> PresenceStatus.Absent
    else      -> PresenceStatus.Absent
}
