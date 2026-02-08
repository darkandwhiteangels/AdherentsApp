package com.antechrist.adherentsapp.ui.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UnpaidGuardianUi(
    val guardianId: String,
    val guardianDisplay: String,
    val remainingCents: Long
)

data class SeasonClosureUi(
    val seasonKey: String = "",
    val loading: Boolean = false,
    val closing: Boolean = false,
    val status: String = "OPEN",
    val canRollbackDev: Boolean = false,
    val rollbackInfo: String? = null,
    val error: String? = null,

    val totalHouseholds: Int = 0,
    val unpaid: List<UnpaidGuardianUi> = emptyList(),

    val snapshotAt: Long? = null,
    val message: String? = null
)

@HiltViewModel
class SeasonClosureViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _ui = MutableStateFlow(SeasonClosureUi())
    val ui: StateFlow<SeasonClosureUi> = _ui

    fun load(seasonKey: String) {
        viewModelScope.launch {
            _ui.update { it.copy(seasonKey = seasonKey, loading = true, error = null, message = null) }

            try {
                // 1) Lire la saison (indispensable)
                val seasonDoc = db.collection("finance_seasons")
                    .document(seasonKey)
                    .get()
                    .await()

                val status = seasonDoc.getString("status") ?: "OPEN"
                val closedAt = seasonDoc.getLong("closedAt") // millis (Long)

                // 2) Lire flags (optionnel : si permission denied => rollback OFF)
                var devRollback = false
                var windowHours = 24L
                var flagsDenied = false

                try {
                    val flagsDoc = db.collection("app_flags")
                        .document("finance")
                        .get()
                        .await()

                    devRollback = flagsDoc.getBoolean("devAllowSeasonRollback") == true
                    windowHours = (flagsDoc.getLong("rollbackWindowHours") ?: 24L).coerceAtLeast(1L)
                } catch (_: Exception) {
                    flagsDenied = true
                    devRollback = false
                    windowHours = 24L
                }

                // 3) Calcul fenêtre rollback (si CLOSED + closedAt présent)
                val now = System.currentTimeMillis()
                val within = if (closedAt != null) {
                    val windowMs = windowHours * 3600L * 1000L
                    now <= (closedAt + windowMs)
                } else false

                val canRollback = devRollback && status == "CLOSED" && within

                val info = when {
                    status != "CLOSED" -> null
                    flagsDenied -> "Rollback DEV indisponible (permissions app_flags)"
                    !devRollback -> "Rollback DEV désactivé"
                    closedAt == null -> "closedAt manquant : rollback impossible"
                    !within -> "Fenêtre rollback dépassée (${windowHours}h)"
                    else -> "Rollback DEV autorisé (${windowHours}h)"
                }

                _ui.update {
                    it.copy(
                        loading = false,
                        status = status,
                        canRollbackDev = canRollback,
                        rollbackInfo = info,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur chargement") }
            }
        }
    }


    fun runCloseSeasonStep1() {
        val seasonKey = _ui.value.seasonKey
        if (seasonKey.isBlank()) return

        viewModelScope.launch {
            _ui.update { it.copy(closing = true, error = null, message = null, unpaid = emptyList()) }

            var wroteClosing = false
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val now = System.currentTimeMillis()

            try {
                // 0) Charger households
                val hhSnap = db.collection("cotisations_families")
                    .document(seasonKey)
                    .collection("households")
                    .get()
                    .await()

                fun sumPayments(listAny: Any?): Long {
                    val raw = (listAny as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: return 0L
                    return raw.sumOf { (it["amountCents"] as? Number)?.toLong() ?: 0L }
                }

                val unpaidRaw = mutableListOf<Pair<String, Long>>() // guardianId -> remaining
                var totalHouseholds = 0

                for (d in hhSnap.documents) {
                    totalHouseholds++
                    val m = d.data ?: emptyMap<String, Any?>()

                    val due = (m["amountDueCents"] as? Number)?.toLong() ?: 0L
                    val paid = sumPayments(m["paymentsPlan"]) + sumPayments(m["paymentsReceived"])
                    val remaining = (due - paid).coerceAtLeast(0L)

                    if (remaining > 0L) {
                        val gid = (m["guardianId"] as? String) ?: d.id
                        unpaidRaw.add(gid to remaining)
                    }
                }

                // 1) Bloquant : impayés
                if (unpaidRaw.isNotEmpty()) {
                    val ids = unpaidRaw.map { it.first }.distinct()
                    val displayMap = mutableMapOf<String, String>()

                    for (chunk in ids.chunked(10)) {
                        val gSnap = db.collection("guardians")
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
                            .await()

                        for (g in gSnap.documents) {
                            val gm = g.data ?: emptyMap<String, Any?>()
                            val prenom = (gm["prenom"] as? String)?.trim().orEmpty()
                            val nom = (gm["nom"] as? String)?.trim().orEmpty()
                            displayMap[g.id] = listOf(prenom, nom).joinToString(" ").trim().ifBlank { g.id }
                        }
                    }

                    val unpaid = unpaidRaw.map { (gid, rem) ->
                        UnpaidGuardianUi(
                            guardianId = gid,
                            guardianDisplay = displayMap[gid] ?: gid,
                            remainingCents = rem
                        )
                    }.sortedByDescending { it.remainingCents }
                        .take(50)

                    _ui.update {
                        it.copy(
                            closing = false,
                            totalHouseholds = totalHouseholds,
                            unpaid = unpaid,
                            message = "Clôture impossible : ${unpaid.size} foyer(s) non soldé(s)."
                        )
                    }
                    return@launch
                }

                // 2) Passage en CLOSING + lock config
                db.collection("finance_seasons")
                    .document(seasonKey)
                    .set(
                        mapOf(
                            "seasonKey" to seasonKey,
                            "status" to "CLOSING",
                            "locked" to true,
                            "lockedAt" to now,
                            "lockedByUid" to uid
                        ),
                        SetOptions.merge()
                    )
                    .await()

                wroteClosing = true

                _ui.update {
                    it.copy(
                        status = "CLOSING",
                        totalHouseholds = totalHouseholds,
                        message = "Fermeture en cours… (snapshot)"
                    )
                }

                // 3) Snapshot cotisations_reports (peut throw permission-denied)
                writeCotisationsSnapshot(seasonKey, now)

                // 4) Passage en CLOSED
                db.collection("finance_seasons")
                    .document(seasonKey)
                    .set(
                        mapOf(
                            "seasonKey" to seasonKey,
                            "status" to "CLOSED",
                            "closedAt" to now,
                            "closedByUid" to uid
                        ),
                        SetOptions.merge()
                    )
                    .await()

                _ui.update {
                    it.copy(
                        closing = false,
                        status = "CLOSED",
                        totalHouseholds = totalHouseholds,
                        snapshotAt = now,
                        message = "Saison clôturée ✔ Snapshot généré ✔"
                    )
                }

            } catch (e: Exception) {

                // ✅ Best-effort: si on a déjà mis CLOSING et qu'une étape échoue,
                // on tente de revenir à OPEN pour éviter la saison "bloquée".
                if (wroteClosing) {
                    runCatching {
                        db.collection("finance_seasons")
                            .document(seasonKey)
                            .set(
                                mapOf(
                                    "seasonKey" to seasonKey,
                                    "status" to "OPEN",
                                    "locked" to false,
                                    // (optionnel) trace DEV
                                    "lastCloseError" to (e.message ?: "error")
                                ),
                                SetOptions.merge()
                            )
                            .await()
                    }
                    _ui.update { it.copy(status = "OPEN") }
                }

                _ui.update {
                    it.copy(
                        closing = false,
                        error = e.message ?: "Erreur clôture",
                        message = null
                    )
                }
            }
        }
    }

    private suspend fun writeCotisationsSnapshot(seasonKey: String, snapshotAt: Long) {
        val hhSnap = db.collection("cotisations_families")
            .document(seasonKey)
            .collection("households")
            .get().await()

        fun sumPayments(listAny: Any?): Long {
            val raw = (listAny as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: return 0L
            return raw.sumOf { (it["amountCents"] as? Number)?.toLong() ?: 0L }
        }

        val baseRef = db.collection("cotisations_reports")
            .document(seasonKey)
            .collection("households")

        // purge old
        val existing = baseRef.get().await()
        existing.documents.forEach { it.reference.delete().await() }

        var householdCount = 0
        var membersCount = 0
        var totalDue = 0L
        var totalPaid = 0L
        var totalRemaining = 0L

        for (d in hhSnap.documents) {
            householdCount++
            val m = d.data ?: emptyMap<String, Any?>()

            val gid = (m["guardianId"] as? String) ?: d.id
            val due = (m["amountDueCents"] as? Number)?.toLong() ?: 0L
            val paid = sumPayments(m["paymentsPlan"]) + sumPayments(m["paymentsReceived"])
            val remaining = (due - paid).coerceAtLeast(0L)

            val memberIds = (m["memberIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            membersCount += memberIds.size

            totalDue += due
            totalPaid += paid
            totalRemaining += remaining

            baseRef.document(gid).set(
                mapOf(
                    "guardianDisplay" to gid, // le ReportScreen remplacera par vrai nom via guardians si besoin
                    "membersCount" to memberIds.size,
                    "status" to (m["status"] as? String),
                    "dueCents" to due,
                    "paidCents" to paid,
                    "remainingCents" to remaining
                ),
                SetOptions.merge()
            ).await()
        }

        db.collection("cotisations_reports")
            .document(seasonKey)
            .collection("meta")
            .document("summary")
            .set(
                mapOf(
                    "seasonKey" to seasonKey,
                    "snapshotAt" to snapshotAt,
                    "householdCount" to householdCount,
                    "membersCount" to membersCount,
                    "totalDueCents" to totalDue,
                    "totalPaidCents" to totalPaid,
                    "totalRemainingCents" to totalRemaining
                ),
                SetOptions.merge()
            ).await()
    }

    fun reopenSeasonDev() {
        val s = _ui.value
        val seasonKey = s.seasonKey

        // ✅ Diagnostics "avant" l'appel réseau
        if (seasonKey.isBlank()) {
            _ui.update { it.copy(error = "DEV: seasonKey vide → impossible de réouvrir") }
            return
        }
        if (s.status != "CLOSED") {
            _ui.update { it.copy(error = "DEV: statut actuel = ${s.status} (attendu CLOSED) → rollback non applicable") }
            return
        }
        if (!s.canRollbackDev) {
            // rollbackInfo contient déjà la raison (flag off, fenêtre dépassée, permissions flags…)
            _ui.update { it.copy(error = "DEV: rollback refusé par checks UI → ${s.rollbackInfo ?: "raison inconnue"}") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, message = null) }

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid.isNullOrBlank()) {
                _ui.update { it.copy(loading = false, error = "DEV: user UID null → réouverture interdite (auth)") }
                return@launch
            }

            val now = System.currentTimeMillis()

            try {
                db.collection("finance_seasons")
                    .document(seasonKey)
                    .set(
                        mapOf(
                            "seasonKey" to seasonKey,
                            "status" to "OPEN",
                            "locked" to false,
                            "reopenedAt" to now,
                            "reopenedByUid" to uid
                        ),
                        SetOptions.merge()
                    )
                    .await()

                // reload pour recalculer canRollbackDev/info
                load(seasonKey)

                _ui.update { it.copy(message = "Saison réouverte (DEV) ✔") }

            } catch (e: Exception) {
                val raw = e.message ?: e.toString()
                val lower = raw.lowercase()

                // Heuristiques utiles : on ne peut pas connaître "validator" à coup sûr,
                // mais on peut pointer la cause la plus probable.
                val hint = when {
                    "permission" in lower || "denied" in lower -> {
                        buildString {
                            append("DEV: PERMISSION_DENIED → rules Firestore bloquent l'update.\n")
                            append("Vérifie :\n")
                            append("• flag devAllowSeasonRollback=true\n")
                            append("• withinRollbackWindow OK (closedAt + window)\n")
                            append("• rule finance_seasons autorise CLOSED->OPEN (rollback)\n")
                            append("• si un validator global existe, il bloque probablement (hasOnly/keys strict).")
                        }
                    }
                    "failed-precondition" in lower -> "DEV: FAILED_PRECONDITION → index/transaction ou précondition Firestore (rare ici)"
                    "unavailable" in lower -> "DEV: UNAVAILABLE → réseau / Firestore indisponible"
                    else -> "DEV: erreur inconnue → $raw"
                }

                _ui.update { it.copy(loading = false, error = hint) }
            }
        }
    }
}
