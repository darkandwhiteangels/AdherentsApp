package com.antechrist.adherentsapp.ui.screens.presence

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.data.firestore.PresenceDataSource
import com.antechrist.adherentsapp.domain.model.PresenceRow
import com.antechrist.adherentsapp.domain.model.PresenceStatus
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import com.antechrist.adherentsapp.domain.usecase.ExportPresenceToPdf
import com.antechrist.adherentsapp.ui.utils.FileShare
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "PresenceReportsVM"

data class PresenceReportRow(
    val dateKey: String,
    val groupDisplay: String,
    val present: Int,
    val absent: Int,
    val retard: Int,
    val total: Int
)

data class PresenceReportsUi(
    val start: LocalDate = LocalDate.now(),
    val end: LocalDate = LocalDate.now(),
    val groupDisplay: String = "Enfants < 14 ans",
    val rows: List<PresenceReportRow> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

sealed class ReportsEvent {
    data class CsvReady(val filename: String, val content: String) : ReportsEvent()
    data class Error(val message: String) : ReportsEvent()
    data class OpenTake(val dateKey: String, val groupDisplay: String) : ReportsEvent()
}

@HiltViewModel
class PresenceReportsViewModel @Inject constructor(
    private val ds: PresenceDataSource,
    private val adherentsRepo: AdherentsRepository,
    // ✅ Ajouts pour l'export PDF
    private val exportPresenceToPdf: ExportPresenceToPdf,
    @IoDispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val main: CoroutineDispatcher,
) : ViewModel() {

    // NOTE: fournis ces qualifiers dans ton DispatcherModule (déjà en place selon ton mémo projet)
    // @Qualifier annotation class IoDispatcher ; @Qualifier annotation class MainDispatcher

    val groups = listOf("Baby", "Enfants < 14 ans", "14+ / Adultes")
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val today = LocalDate.now()

    private val _ui = MutableStateFlow(
        PresenceReportsUi(
            start = today,
            end = today
        )
    )
    val ui: StateFlow<PresenceReportsUi> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<ReportsEvent>()
    val events = _events

    init {
        // Charger automatiquement la présence du jour
        load()
    }

    fun onStartChanged(d: LocalDate) {
        _ui.update { it.copy(start = d) }
    }

    fun onEndChanged(d: LocalDate) {
        _ui.update { it.copy(end = d) }
    }

    fun onGroupChanged(display: String) {
        _ui.update { it.copy(groupDisplay = display) }
    }

    /** Génère toutes les dates inclusives entre start..end (ordre ascendant). */
    private fun generateDateKeysInclusive(start: LocalDate, end: LocalDate): List<String> {
        var s = start
        var e = end
        if (e.isBefore(s)) {
            val tmp = s; s = e; e = tmp
            Log.w(TAG, "generateDateKeysInclusive: swapped dates because end < start")
        }
        val out = ArrayList<String>()
        var cur = s
        while (!cur.isAfter(e)) {
            out += cur.format(fmt)
            cur = cur.plusDays(1)
        }
        return out
    }

    fun load() {
        viewModelScope.launch {
            try {
                val start = _ui.value.start
                val end = _ui.value.end
                val groupDisplay = _ui.value.groupDisplay
                val groupKey = ds.groupKey(groupDisplay)

                _ui.update { it.copy(loading = true, error = null) }

                Log.d(TAG, "load(): start=$start end=$end group='$groupDisplay' key='$groupKey'")

                val dateKeys = generateDateKeysInclusive(start, end)
                val rows = mutableListOf<PresenceReportRow>()

                for (dk in dateKeys) {
                    val map = ds.getDayGroupPresence(dk, groupKey)

                    var p = 0; var a = 0; var r = 0
                    map.values.forEach { rec ->
                        when (rec.status.lowercase()) {
                            "present" -> p++
                            "absent"  -> a++
                            "retard"  -> r++
                            else -> { /* ignore unknown */ }
                        }
                    }
                    val total = p + a + r
                    if (total > 0) {
                        rows += PresenceReportRow(
                            dateKey = dk,
                            groupDisplay = groupDisplay,
                            present = p,
                            absent = a,
                            retard = r,
                            total = total
                        )
                    }
                    Log.d(TAG, "Day $dk -> total=$total (docs=${map.size})")
                }

                _ui.update { it.copy(rows = rows.sortedBy { r -> r.dateKey }, loading = false) }

            } catch (e: Exception) {
                Log.e(TAG, "load() error: ${e.message}", e)
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur de chargement") }
                _events.tryEmit(ReportsEvent.Error(_ui.value.error ?: "Erreur inconnue"))
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val header = "date,groupe,presents,absents,retards,total"
            val lines = _ui.value.rows.joinToString("\n") { r ->
                "${r.dateKey},${r.groupDisplay},${r.present},${r.absent},${r.retard},${r.total}"
            }
            val content = buildString {
                appendLine(header)
                if (lines.isNotBlank()) append(lines)
            }
            _events.emit(
                ReportsEvent.CsvReady(
                    filename = "presences_${_ui.value.start}_to_${_ui.value.end}_${ds.groupKey(_ui.value.groupDisplay)}.csv",
                    content = content
                )
            )
        }
    }

    fun openTake(dateKey: String) {
        viewModelScope.launch {
            _events.emit(ReportsEvent.OpenTake(dateKey, _ui.value.groupDisplay))
        }
    }

    // ============ ✅ NOUVEAU : Export PDF détaillé pour une carte (dateKey + groupDisplay) ============
    fun exportPdfFor(context: Context, dateKey: String, groupDisplay: String) {
        viewModelScope.launch(io) {
            try {
                val groupKey = ds.groupKey(groupDisplay)
                val map = ds.getDayGroupPresence(dateKey, groupKey) // Map<adherentId, PresenceRecord>

                // === LOG ids récupérés ===
                Log.d(TAG, "exportPdfFor: dateKey=$dateKey groupKey=$groupKey map.size=${map.size}")
                val ids: Set<String> = map.keys
                Log.d(TAG, "exportPdfFor: adherentIds=$ids")

                // 1) Récupère les noms/prénoms pour tous les IDs d’un coup
                //    (nécessite AdherentsRepository.getMemberNamesByIds(ids))
                val namesById: Map<String, Pair<String, String>> =
                    adherentsRepo.getMemberNamesByIds(ids)

                // === LOG mappage noms ===
                Log.d(TAG, "exportPdfFor: namesById.size=${namesById.size}")
                Log.d(TAG, "exportPdfFor: namesById=$namesById")

                // 2) Map -> PresenceRow (avec fallback sur l'UID si pas de nom/prénom)
                val rows: List<PresenceRow> = map.entries.map { (adherentId, rec) ->
                    val (rawNom, rawPrenom) = namesById[adherentId] ?: ("" to "")
                    val nom = rawNom.trim().uppercase()
                    val prenom = rawPrenom.trim().replaceFirstChar { c ->
                        if (c.isLowerCase()) c.titlecase() else c.toString()
                    }

                    Log.d(TAG, "Row -> id=$adherentId, nom='$nom', prenom='$prenom', status=${rec.status}")

                    PresenceRow(
                        nom = if (nom.isNotBlank()) nom else adherentId.uppercase(),
                        prenom = prenom,
                        statut = when (rec.status.lowercase()) {
                            "present" -> PresenceStatus.PRESENT
                            "absent"  -> PresenceStatus.ABSENT
                            "retard"  -> PresenceStatus.RETARD
                            else      -> PresenceStatus.ABSENT
                        },
                        note = rec.note
                    )
                }.sortedWith(compareBy({ it.nom }, { it.prenom }))

                val date = LocalDate.parse(dateKey, fmt)
                val file = exportPresenceToPdf.invoke(
                    context = context,
                    date = date,
                    groupLabel = groupDisplay,
                    rows = rows
                )

                withContext(main) {
                    FileShare.sharePdf(context, file, "Cahier de présence")
                }
            } catch (e: Exception) {
                Log.e(TAG, "exportPdfFor() failed: ${e.message}", e)
                withContext(main) {
                    _events.emit(ReportsEvent.Error("Échec export PDF"))
                }
            }
        }
    }


    /**
     * Essaie de déduire nom/prénom :
     * - si tu stockes déjà "nom" / "prenom" dans le document présence, expose-les dans PresenceRecord et utilise-les ici
     * - sinon, fais un join avec ton AdherentsRepository (non montré ici)
     * - fallback : vide (on mettra l'adherentId en NOM dans le PDF)
     */
    private fun resolveMemberName(
        adherentId: String,
        rec: com.antechrist.adherentsapp.domain.model.PresenceRecord // adapte le package si besoin
    ): Pair<String, String> {
        // TODO si PresenceRecord contient nom/prenom -> les renvoyer
        // ex: return rec.nom?.uppercase().orEmpty() to (rec.prenom?.replaceFirstChar { it.titlecase() }.orEmpty())

        // Fallback : pas de nom/prénom connus ici
        return "" to ""
    }
}

// --- Qualifiers Dispatchers (à placer si pas déjà dans un fichier commun) ---
@javax.inject.Qualifier
annotation class IoDispatcher

@javax.inject.Qualifier
annotation class MainDispatcher
