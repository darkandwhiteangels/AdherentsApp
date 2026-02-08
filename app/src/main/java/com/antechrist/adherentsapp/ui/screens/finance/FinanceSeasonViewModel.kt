package com.antechrist.adherentsapp.ui.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.core.utils.SeasonUtils
import com.antechrist.adherentsapp.domain.model.finance.FinanceSeasonConfig
import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class FinanceSeasonUi(
    val loading: Boolean = true,
    val error: String? = null,
    val seasonKey: String = SeasonUtils.currentSeasonKey(),
    val config: FinanceSeasonConfig = FinanceSeasonConfig(SeasonUtils.currentSeasonKey()),
    val saved: Boolean = false,

//    // ✅ Fin de saison (étape 1)
//    val closing: Boolean = false,
//    val closeResult: CloseSeasonResult? = null
)

//data class CloseSeasonUnpaid(
//    val guardianId: String,
//    val guardianDisplay: String,
//    val remainingCents: Long
//)

// data class CloseSeasonResult(
//    val ok: Boolean,
//    val seasonKey: String,
//    val totalHouseholds: Int,
//    val unpaidCount: Int,
//    val unpaid: List<CloseSeasonUnpaid>,
//    val snapshotAt: Long? = null,
//    val message: String
//)

@HiltViewModel
class FinanceSeasonViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _ui = MutableStateFlow(FinanceSeasonUi())
    val ui: StateFlow<FinanceSeasonUi> = _ui

    fun load(seasonKey: String = SeasonUtils.currentSeasonKey()) {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    loading = true,
                    error = null,
                    seasonKey = seasonKey,
                    saved = false
                )
            }
            try {
                val snap = db.collection("finance_seasons").document(seasonKey).get().await()
                val cfg = if (snap.exists()) {
                    val m = snap.data ?: emptyMap()
                    FinanceSeasonConfig(
                        seasonKey = seasonKey,

                        priceAdultCents = (m["priceAdultCents"] as? Number)?.toLong() ?: 18300,
                        priceChild6to16Cents = (m["priceChild6to16Cents"] as? Number)?.toLong()
                            ?: 16800,
                        priceBabyUnder6Cents = (m["priceBabyUnder6Cents"] as? Number)?.toLong()
                            ?: 11100,

                        bundle2AllOver5Cents = (m["bundle2AllOver5Cents"] as? Number)?.toLong()
                            ?: 30600,
                        bundle3AllOver5Cents = (m["bundle3AllOver5Cents"] as? Number)?.toLong()
                            ?: 43200,

                        oneClassPerWeekDiscountCents = (m["oneClassPerWeekDiscountCents"] as? Number)?.toLong()
                            ?: 3000,

                        discountBlackBeltCents = (m["discountBlackBeltCents"] as? Number)?.toLong()
                            ?: 0L,
                        discountFamilyGradedCents = (m["discountFamilyGradedCents"] as? Number)?.toLong()
                            ?: 0L,
                        discountAssistantProfCents = (m["discountAssistantProfCents"] as? Number)?.toLong()
                            ?: 0L,

                        licencePaidByClub = m["licencePaidByClub"] as? Boolean ?: true,
                        licenceAmountCents = (m["licenceAmountCents"] as? Number)?.toLong() ?: 3900,

                        locked = m["locked"] as? Boolean ?: false,
                        lockedAt = (m["lockedAt"] as? Number)?.toLong(),
                        lockedByUid = m["lockedByUid"] as? String,

                        defaultDueDate = m["defaultDueDate"] as? String,

//                        status = (m["status"] as? String) ?: "OPEN",
//                        closedAt = (m["closedAt"] as? Number)?.toLong(),
//                        closedByUid = m["closedByUid"] as? String,
                    )
                } else FinanceSeasonConfig(seasonKey)
                _ui.update { it.copy(loading = false, config = cfg) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur de chargement") }
            }
        }
    }

    fun update(cfg: FinanceSeasonConfig) {
        _ui.update { it.copy(config = cfg, saved = false) }
    }

    fun save() {
        viewModelScope.launch {
            val s = _ui.value
            try {
                val map = mapOf(
                    "seasonKey" to s.seasonKey, // ✅ AJOUT

                    "priceAdultCents" to s.config.priceAdultCents,
                    "priceChild6to16Cents" to s.config.priceChild6to16Cents,
                    "priceBabyUnder6Cents" to s.config.priceBabyUnder6Cents,

                    "bundle2AllOver5Cents" to s.config.bundle2AllOver5Cents,
                    "bundle3AllOver5Cents" to s.config.bundle3AllOver5Cents,

                    "oneClassPerWeekDiscountCents" to s.config.oneClassPerWeekDiscountCents,

                    "discountBlackBeltCents" to s.config.discountBlackBeltCents,
                    "discountFamilyGradedCents" to s.config.discountFamilyGradedCents,
                    "discountAssistantProfCents" to s.config.discountAssistantProfCents,

                    "licencePaidByClub" to s.config.licencePaidByClub,
                    "licenceAmountCents" to s.config.licenceAmountCents,

                    "locked" to s.config.locked,
                    "lockedAt" to s.config.lockedAt,
                    "lockedByUid" to s.config.lockedByUid,

                    "defaultDueDate" to s.config.defaultDueDate,

//                    "status" to s.config.status,
//                    "closedAt" to s.config.closedAt,
//                    "closedByUid" to s.config.closedByUid
                )
                db.collection("finance_seasons").document(s.seasonKey)
                    .set(map, SetOptions.merge()).await()
                _ui.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Échec enregistrement") }
            }
        }
    }

    fun lockSeason() {
        viewModelScope.launch {
            val s = _ui.value
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val now = System.currentTimeMillis()

                val next = s.config.copy(
                    locked = true,
                    lockedAt = now,
                    lockedByUid = uid
                )

                db.collection("finance_seasons")
                    .document(s.seasonKey)
                    .set(
                        mapOf(
                            "seasonKey" to s.seasonKey,
                            "locked" to true,
                            "lockedAt" to now,
                            "lockedByUid" to uid
                        ),
                        SetOptions.merge()
                    ).await()

                _ui.update { it.copy(config = next, saved = true) }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Échec verrouillage") }
            }
        }
    }

    fun unlockSeason() {
        viewModelScope.launch {
            val s = _ui.value
            try {
                // Le contrôle SuperAdmin est fait par les rules.
                // Côté app on fait juste la demande.
                db.collection("finance_seasons")
                    .document(s.seasonKey)
                    .set(
                        mapOf(
                            "seasonKey" to s.seasonKey,
                            "locked" to false,
                            "lockedAt" to null,
                            "lockedByUid" to null
                        ),
                        SetOptions.merge()
                    ).await()

                val next = s.config.copy(
                    locked = false,
                    lockedAt = null,
                    lockedByUid = null
                )
                _ui.update { it.copy(config = next, saved = true) }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Échec déverrouillage") }
            }
        }
    }

//    fun closeSeason() {
//        viewModelScope.launch {
//            val s = _ui.value
//            try {
//                val uid = FirebaseAuth.getInstance().currentUser?.uid
//                val now = System.currentTimeMillis()
//
//                // 1) status -> CLOSING + lock
//                db.collection("finance_seasons")
//                    .document(s.seasonKey)
//                    .set(
//                        mapOf(
//                            "seasonKey" to s.seasonKey,
//                            "status" to "CLOSING",
//                            "locked" to true,
//                            "lockedAt" to now,
//                            "lockedByUid" to uid
//                        ),
//                        SetOptions.merge()
//                    ).await()
//
//                // 2) Génère snapshot rapport (même logique que l'écran)
//                generateCotisationsReportSnapshot(s.seasonKey, now)
//
//                // 3) status -> CLOSED
//                db.collection("finance_seasons")
//                    .document(s.seasonKey)
//                    .set(
//                        mapOf(
//                            "seasonKey" to s.seasonKey,
//                            "status" to "CLOSED",
//                            "closedAt" to now,
//                            "closedByUid" to uid
//                        ),
//                        SetOptions.merge()
//                    ).await()
//
//                // 4) refresh UI (simple)
//                load(s.seasonKey)
//
//            } catch (e: Exception) {
//                _ui.update { it.copy(error = e.message ?: "Échec clôture") }
//            }
//        }
//    }

//    @Suppress("UNCHECKED_CAST")
//    private suspend fun generateCotisationsReportSnapshot(seasonKey: String, snapshotAt: Long) {
//        // households
//        val hhSnap = db.collection("cotisations_families")
//            .document(seasonKey)
//            .collection("households")
//            .get().await()
//
//        val hhDocs = hhSnap.documents
//        if (hhDocs.isEmpty()) {
//            db.collection("cotisations_reports")
//                .document(seasonKey)
//                .collection("meta")
//                .document("summary")
//                .set(
//                    mapOf(
//                        "seasonKey" to seasonKey,
//                        "snapshotAt" to snapshotAt,
//                        "householdCount" to 0,
//                        "membersCount" to 0,
//                        "totalBaseCents" to 0,
//                        "totalDiscountsCents" to 0,
//                        "totalAidsCents" to 0,
//                        "totalDueCents" to 0,
//                        "totalPaidCents" to 0,
//                        "totalRemainingCents" to 0,
//                        "licencesCount" to 0,
//                        "licencesTotalCents" to 0
//                    ),
//                    SetOptions.merge()
//                ).await()
//            return
//        }
//
//        val guardianIds = hhDocs.mapNotNull { it.getString("guardianId") }.distinct()
//
//        // guardians
//        val guardiansMap = mutableMapOf<String, Map<String, Any?>>()
//        for (chunk in guardianIds.chunked(10)) {
//            val gSnap = db.collection("guardians")
//                .whereIn(FieldPath.documentId(), chunk)
//                .get().await()
//            gSnap.documents.forEach { gDoc ->
//                guardiansMap[gDoc.id] = gDoc.data ?: emptyMap()
//            }
//        }
//
//        fun paySum(listAny: Any?): Long {
//            val raw = (listAny as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: return 0L
//            return raw.sumOf { (it["amountCents"] as? Number)?.toLong() ?: 0L }
//        }
//
//        fun aidsSum(listAny: Any?): Long {
//            val raw = (listAny as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: return 0L
//            return raw.sumOf { (it["amountCents"] as? Number)?.toLong() ?: 0L }
//        }
//
//        var householdCount = 0
//        var membersCount = 0
//        var totalBase = 0L
//        var totalDiscounts = 0L
//        var totalAids = 0L
//        var totalDue = 0L
//        var totalPaid = 0L
//        var totalRemaining = 0L
//        var licencesCount = 0
//        var licencesTotal = 0L
//
//        val baseRef = db.collection("cotisations_reports")
//            .document(seasonKey)
//            .collection("households")
//
//        // purge old
//        val existing = baseRef.get().await()
//        for (d in existing.documents) d.reference.delete().await()
//
//        for (d in hhDocs) {
//            val m = d.data ?: emptyMap<String, Any?>()
//            val gid = (m["guardianId"] as? String) ?: d.id
//            val g = guardiansMap[gid] ?: emptyMap()
//
//            val nom = (g["nom"] as? String)?.trim().orEmpty()
//            val prenom = (g["prenom"] as? String)?.trim().orEmpty()
//            val display = listOf(prenom, nom).joinToString(" ").trim().ifBlank { gid }
//
//            val memberIds =
//                (m["memberIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
//            val mc = memberIds.size
//
//            val base = (m["amountBaseCents"] as? Number)?.toLong() ?: 0L
//            val discount = (m["discountCents"] as? Number)?.toLong() ?: 0L
//            val manualDiscount = (m["manualDiscountCents"] as? Number)?.toLong() ?: 0L
//            val aids = aidsSum(m["aids"])
//            val due = (m["amountDueCents"] as? Number)?.toLong() ?: 0L
//
//            val planned = paySum(m["paymentsPlan"])
//            val received = paySum(m["paymentsReceived"])
//            val paid = planned + received
//            val remaining = (due - paid).coerceAtLeast(0L)
//
//            val licenceC = (m["licenceCount"] as? Number)?.toInt() ?: 0
//            val licencesT = (m["licencesTotalCents"] as? Number)?.toLong()
//                ?: (((m["licenceAmountCents"] as? Number)?.toLong() ?: 0L) * licenceC.toLong())
//
//            baseRef.document(gid).set(
//                mapOf(
//                    "guardianDisplay" to display,
//                    "membersCount" to mc,
//                    "status" to (m["status"] as? String),
//                    "baseCents" to base,
//                    "discountsCents" to (discount + manualDiscount),
//                    "aidsCents" to aids,
//                    "dueCents" to due,
//                    "paidCents" to paid,
//                    "remainingCents" to remaining,
//                    "licencesCount" to licenceC,
//                    "licencesTotalCents" to licencesT
//                ),
//                SetOptions.merge()
//            ).await()
//
//            householdCount += 1
//            membersCount += mc
//            totalBase += base
//            totalDiscounts += (discount + manualDiscount)
//            totalAids += aids
//            totalDue += due
//            totalPaid += paid
//            totalRemaining += remaining
//            licencesCount += licenceC
//            licencesTotal += licencesT
//        }
//
//        db.collection("cotisations_reports")
//            .document(seasonKey)
//            .collection("meta")
//            .document("summary")
//            .set(
//                mapOf(
//                    "seasonKey" to seasonKey,
//                    "snapshotAt" to snapshotAt,
//                    "householdCount" to householdCount,
//                    "membersCount" to membersCount,
//                    "totalBaseCents" to totalBase,
//                    "totalDiscountsCents" to totalDiscounts,
//                    "totalAidsCents" to totalAids,
//                    "totalDueCents" to totalDue,
//                    "totalPaidCents" to totalPaid,
//                    "totalRemainingCents" to totalRemaining,
//                    "licencesCount" to licencesCount,
//                    "licencesTotalCents" to licencesTotal
//                ),
//                SetOptions.merge()
//            ).await()
//    }
}