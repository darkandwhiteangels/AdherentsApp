package com.antechrist.adherentsapp.ui.screens.cotisations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.core.pdf.CotisationReportRowPdf
import com.antechrist.adherentsapp.core.pdf.CotisationReportTotalsPdf
import com.antechrist.adherentsapp.core.pdf.CotisationsReportPdf
import com.antechrist.adherentsapp.data.remote.ClubConfigRemote
import com.antechrist.adherentsapp.domain.model.finance.HouseholdStatus
import com.antechrist.adherentsapp.ui.utils.FileShare
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

data class CotisationsReportHouseholdUi(
    val guardianId: String,
    val guardianDisplay: String,
    val membersCount: Int,
    val status: HouseholdStatus?,
    val dueCents: Long,
    val paidCents: Long,
    val remainingCents: Long,
)

data class CotisationsReportUi(
    val seasonKey: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val fromSnapshot: Boolean = false,
    val snapshotAt: Long? = null,
    val clubName: String = "",
    val households: List<CotisationsReportHouseholdUi> = emptyList()
)

@HiltViewModel
class CotisationsReportViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _ui = MutableStateFlow(CotisationsReportUi())
    val ui: StateFlow<CotisationsReportUi> = _ui

    fun load(seasonKey: String) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, seasonKey = seasonKey) }
            try {
                val club = ClubConfigRemote.get()
                _ui.update { it.copy(clubName = club?.clubName ?: "") }

                // tente snapshot d'abord
                val meta = db.collection("cotisations_reports")
                    .document(seasonKey)
                    .collection("meta")
                    .document("summary")
                    .get().await()

                if (meta.exists()) {
                    val snapAt = (meta.get("snapshotAt") as? Number)?.toLong()
                    val hhSnap = db.collection("cotisations_reports")
                        .document(seasonKey)
                        .collection("households")
                        .get().await()

                    val households = hhSnap.documents.map { d ->
                        val m = d.data ?: emptyMap<String, Any?>()
                        CotisationsReportHouseholdUi(
                            guardianId = d.id,
                            guardianDisplay = (m["guardianDisplay"] as? String) ?: d.id,
                            membersCount = (m["membersCount"] as? Number)?.toInt() ?: 0,
                            status = (m["status"] as? String)?.let { runCatching { HouseholdStatus.valueOf(it) }.getOrNull() },
                            dueCents = (m["dueCents"] as? Number)?.toLong() ?: 0L,
                            paidCents = (m["paidCents"] as? Number)?.toLong() ?: 0L,
                            remainingCents = (m["remainingCents"] as? Number)?.toLong() ?: 0L
                        )
                    }.sortedBy { it.guardianDisplay.lowercase() }

                    _ui.update {
                        it.copy(
                            loading = false,
                            fromSnapshot = true,
                            snapshotAt = snapAt,
                            households = households
                        )
                    }
                    return@launch
                }

                // sinon mode live (utile avant snapshot)
                val households = computeLive(seasonKey)
                _ui.update {
                    it.copy(
                        loading = false,
                        fromSnapshot = false,
                        snapshotAt = null,
                        households = households
                    )
                }

            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur chargement") }
            }
        }
    }

    fun generateSnapshot() {
        val seasonKey = _ui.value.seasonKey
        if (seasonKey.isBlank()) return

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val households = computeLive(seasonKey)
                val now = System.currentTimeMillis()

                // summary minimal
                val householdCount = households.size
                val membersCount = households.sumOf { it.membersCount }
                val totalDue = households.sumOf { it.dueCents }
                val totalPaid = households.sumOf { it.paidCents }
                val totalRemaining = households.sumOf { it.remainingCents }

                db.collection("cotisations_reports")
                    .document(seasonKey)
                    .collection("meta")
                    .document("summary")
                    .set(
                        mapOf(
                            "seasonKey" to seasonKey,
                            "snapshotAt" to now,
                            "householdCount" to householdCount,
                            "membersCount" to membersCount,
                            "totalDueCents" to totalDue,
                            "totalPaidCents" to totalPaid,
                            "totalRemainingCents" to totalRemaining
                        ),
                        SetOptions.merge()
                    ).await()

                val baseRef = db.collection("cotisations_reports")
                    .document(seasonKey)
                    .collection("households")

                // purge old pour éviter les “ghost rows”
                val existing = baseRef.get().await()
                existing.documents.forEach { it.reference.delete().await() }

                households.forEach { h ->
                    baseRef.document(h.guardianId).set(
                        mapOf(
                            "guardianDisplay" to h.guardianDisplay,
                            "membersCount" to h.membersCount,
                            "status" to (h.status?.name),
                            "dueCents" to h.dueCents,
                            "paidCents" to h.paidCents,
                            "remainingCents" to h.remainingCents
                        ),
                        SetOptions.merge()
                    ).await()
                }

                // reload snapshot
                load(seasonKey)

            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur snapshot") }
            }
        }
    }

    fun exportCsvAndShare(context: Context) {
        val s = _ui.value
        viewModelScope.launch {
            try {
                val file = buildCsvFile(context, s.seasonKey, s.households)
                FileShare.shareCsv(context, file, "Partager CSV cotisations")
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur export CSV") }
            }
        }
    }

    /**
     * PDF basé sur snapshot.
     * Si pas de snapshot => on force génération snapshot, puis on relance export.
     */
    fun exportPdfAndShare(context: Context) {
        val s = _ui.value
        if (!s.fromSnapshot || s.snapshotAt == null) {
            // snapshot d’abord
            generateSnapshot()
            return
        }

        viewModelScope.launch {
            try {
                val rows = s.households.map {
                    CotisationReportRowPdf(
                        guardianName = it.guardianDisplay,
                        membersCount = it.membersCount,
                        statusLabel = it.status?.name ?: "-",
                        dueCents = it.dueCents,
                        paidCents = it.paidCents,
                        remainingCents = it.remainingCents
                    )
                }

                val totals = CotisationReportTotalsPdf(
                    householdCount = s.households.size,
                    membersCount = s.households.sumOf { it.membersCount },
                    totalDueCents = s.households.sumOf { it.dueCents },
                    totalPaidCents = s.households.sumOf { it.paidCents },
                    totalRemainingCents = s.households.sumOf { it.remainingCents }
                )

                val file = CotisationsReportPdf.generate(
                    context = context,
                    clubName = s.clubName,
                    seasonKey = s.seasonKey,
                    snapshotAtMillis = s.snapshotAt,
                    rows = rows,
                    totals = totals
                )
                FileShare.sharePdf(context, file, "Partager PDF cotisations")
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur export PDF") }
            }
        }
    }

    // ---------------- internals ----------------

    private suspend fun buildCsvFile(context: Context, seasonKey: String, list: List<CotisationsReportHouseholdUi>): File {
        val header = "guardianId,guardian,membersCount,status,dueCents,paidCents,remainingCents"
        val lines = list.joinToString("\n") { h ->
            listOf(
                h.guardianId,
                h.guardianDisplay.replace(",", " "),
                h.membersCount.toString(),
                (h.status?.name ?: ""),
                h.dueCents.toString(),
                h.paidCents.toString(),
                h.remainingCents.toString()
            ).joinToString(",")
        }

        val content = buildString {
            appendLine(header)
            if (lines.isNotBlank()) append(lines)
        }

        val out = File(context.cacheDir, "cotisations_report_${seasonKey}_${System.currentTimeMillis()}.csv")
        out.writeText(content, Charsets.UTF_8)
        return out
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun computeLive(seasonKey: String): List<CotisationsReportHouseholdUi> {
        val hhSnap = db.collection("cotisations_families")
            .document(seasonKey)
            .collection("households")
            .get().await()

        val hhDocs = hhSnap.documents
        if (hhDocs.isEmpty()) return emptyList()

        val guardianIds = hhDocs.mapNotNull { it.getString("guardianId") }.distinct()

        val guardiansMap = mutableMapOf<String, Map<String, Any?>>()
        for (chunk in guardianIds.chunked(10)) {
            val gSnap = db.collection("guardians")
                .whereIn(FieldPath.documentId(), chunk)
                .get().await()
            gSnap.documents.forEach { gDoc ->
                guardiansMap[gDoc.id] = gDoc.data ?: emptyMap()
            }
        }

        fun paySum(listAny: Any?): Long {
            val raw = (listAny as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: return 0L
            return raw.sumOf { (it["amountCents"] as? Number)?.toLong() ?: 0L }
        }

        return hhDocs.map { d ->
            val m = d.data ?: emptyMap<String, Any?>()
            val gid = (m["guardianId"] as? String) ?: d.id
            val g = guardiansMap[gid] ?: emptyMap()

            val nom = (g["nom"] as? String)?.trim().orEmpty()
            val prenom = (g["prenom"] as? String)?.trim().orEmpty()
            val display = listOf(prenom, nom).joinToString(" ").trim().ifBlank { gid }

            val memberIds = (m["memberIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            val due = (m["amountDueCents"] as? Number)?.toLong() ?: 0L
            val planned = paySum(m["paymentsPlan"])
            val received = paySum(m["paymentsReceived"])
            val paid = planned + received
            val remaining = (due - paid).coerceAtLeast(0L)

            val status = (m["status"] as? String)?.let { runCatching { HouseholdStatus.valueOf(it) }.getOrNull() }

            CotisationsReportHouseholdUi(
                guardianId = gid,
                guardianDisplay = display,
                membersCount = memberIds.size,
                status = status,
                dueCents = due,
                paidCents = paid,
                remainingCents = remaining
            )
        }.sortedBy { it.guardianDisplay.lowercase() }
    }
}
