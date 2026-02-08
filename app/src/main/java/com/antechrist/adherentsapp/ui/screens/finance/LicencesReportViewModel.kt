package com.antechrist.adherentsapp.ui.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.core.utils.SeasonUtils
import com.antechrist.adherentsapp.domain.model.finance.HouseholdStatus
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.antechrist.adherentsapp.core.pdf.LicencesReportPdf
import com.antechrist.adherentsapp.core.pdf.LicenceReportRowPdf
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

data class LicenceRow(
    val guardianId: String,
    val guardianName: String,
    val status: HouseholdStatus,
    val licenceCount: Int,
    val licencesTotalCents: Long
)

enum class LicencesFilter { ALL, SOLDE, A_REGLER, EN_RETARD, ANNULE }

data class LicencesReportUi(
    val loading: Boolean = true,
    val error: String? = null,
    val seasonKey: String = SeasonUtils.currentSeasonKey(),
    val filter: LicencesFilter = LicencesFilter.ALL,

    val rows: List<LicenceRow> = emptyList(),

    val totalLicences: Int = 0,
    val totalLicencesAmountCents: Long = 0L
)

@HiltViewModel
class LicencesReportViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _ui = MutableStateFlow(LicencesReportUi())
    val ui: StateFlow<LicencesReportUi> = _ui

    fun load(seasonKey: String = SeasonUtils.currentSeasonKey()) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, seasonKey = seasonKey) }
            try {
                // 1) récupérer tous les foyers de la saison
                val householdsSnap = db.collection("cotisations_families")
                    .document(seasonKey)
                    .collection("households")
                    .get()
                    .await()

                val households = householdsSnap.documents.map { doc ->
                    val m = doc.data ?: emptyMap<String, Any?>()

                    val guardianId = doc.id
                    val status = runCatching {
                        HouseholdStatus.valueOf((m["status"] as? String) ?: "A_REGLER")
                    }.getOrDefault(HouseholdStatus.A_REGLER)

                    val licenceCount = (m["licenceCount"] as? Number)?.toInt() ?: 0
                    val licencesTotalCents =
                        (m["licencesTotalCents"] as? Number)?.toLong()
                            ?: 0L

                    Triple(guardianId, status, Pair(licenceCount, licencesTotalCents))
                }

                // 2) noms responsables (guardians)
                val guardianIds = households.map { it.first }.distinct()
                val guardiansMap = mutableMapOf<String, String>()
                // fetch en batch simple (si tu veux optimiser plus tard, on fera un cache)
                for (gid in guardianIds) {
                    val g = db.collection("guardians").document(gid).get().await()
                    val nom = (g.getString("nom") ?: "").trim()
                    val prenom = (g.getString("prenom") ?: "").trim()
                    val name = listOf(prenom, nom).joinToString(" ").trim().ifBlank { gid }
                    guardiansMap[gid] = name
                }

                val rows = households.map { (gid, status, lic) ->
                    LicenceRow(
                        guardianId = gid,
                        guardianName = guardiansMap[gid] ?: gid,
                        status = status,
                        licenceCount = lic.first,
                        licencesTotalCents = lic.second
                    )
                }.sortedBy { it.guardianName.lowercase() }

                recompute(rows)
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur chargement") }
            }
        }
    }

    fun setFilter(filter: LicencesFilter) {
        _ui.update { it.copy(filter = filter) }
        recompute(_ui.value.rows) // recalcul totals avec filtre
    }

    private fun recompute(allRows: List<LicenceRow>) {
        val f = _ui.value.filter
        val filtered = when (f) {
            LicencesFilter.ALL -> allRows
            LicencesFilter.SOLDE -> allRows.filter { it.status == HouseholdStatus.SOLDE }
            LicencesFilter.A_REGLER -> allRows.filter { it.status == HouseholdStatus.A_REGLER }
            LicencesFilter.EN_RETARD -> allRows.filter { it.status == HouseholdStatus.EN_RETARD }
            LicencesFilter.ANNULE -> allRows.filter { it.status == HouseholdStatus.ANNULE }
        }

        val totalLic = filtered.sumOf { it.licenceCount }
        val totalCents = filtered.sumOf { it.licencesTotalCents }

        _ui.update {
            it.copy(
                loading = false,
                rows = allRows, // on garde tout en mémoire, et on filtre côté UI
                totalLicences = totalLic,
                totalLicencesAmountCents = totalCents
            )
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            try {
                val s = _ui.value

                val filtered = when (s.filter) {
                    LicencesFilter.ALL -> s.rows
                    LicencesFilter.SOLDE -> s.rows.filter { it.status == HouseholdStatus.SOLDE }
                    LicencesFilter.A_REGLER -> s.rows.filter { it.status == HouseholdStatus.A_REGLER }
                    LicencesFilter.EN_RETARD -> s.rows.filter { it.status == HouseholdStatus.EN_RETARD }
                    LicencesFilter.ANNULE -> s.rows.filter { it.status == HouseholdStatus.ANNULE }
                }

                val totalLic = filtered.sumOf { it.licenceCount }
                val totalCents = filtered.sumOf { it.licencesTotalCents }

                val filterLabel = when (s.filter) {
                    LicencesFilter.ALL -> "Tous"
                    LicencesFilter.SOLDE -> "Soldé"
                    LicencesFilter.A_REGLER -> "À régler"
                    LicencesFilter.EN_RETARD -> "En retard"
                    LicencesFilter.ANNULE -> "Annulé"
                }

                val pdfRows = filtered.map {
                    LicenceReportRowPdf(
                        guardianName = it.guardianName,
                        statusLabel = it.status.label(),
                        licenceCount = it.licenceCount,
                        licencesTotalCents = it.licencesTotalCents
                    )
                }

                val file = LicencesReportPdf.generate(
                    context = context,
                    seasonKey = s.seasonKey,
                    filterLabel = filterLabel,
                    totalLicences = totalLic,
                    totalAmountCents = totalCents,
                    rows = pdfRows
                )

                val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_SUBJECT, "Rapport licences FFK ${s.seasonKey}")
                    putExtra(Intent.EXTRA_TEXT, "Rapport licences FFK – Saison ${s.seasonKey} (filtre : $filterLabel)")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(intent, "Partager le rapport PDF"))
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur export PDF") }
            }
        }
    }
}
