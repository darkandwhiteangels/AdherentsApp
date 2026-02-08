//package com.antechrist.adherentsapp.data.firestore
//
//import com.antechrist.adherentsapp.domain.model.finance.*
//import com.antechrist.adherentsapp.domain.repository.CotisationsHouseholdsRepository
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.SetOptions
//import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.callbackFlow
//import kotlinx.coroutines.tasks.await
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class CotisationsHouseholdsRepositoryImpl @Inject constructor(
//    private val db: FirebaseFirestore
//) : CotisationsHouseholdsRepository {
//
//    // ---------------- Refs ----------------
//
//    private fun householdsRef(seasonKey: String) =
//        db.collection("cotisations_families")
//            .document(seasonKey)
//            .collection("households")
//
//    private fun financeMemberRef(seasonKey: String, adherentId: String) =
//        db.collection("finance_status")
//            .document(seasonKey)
//            .collection("members")
//            .document(adherentId)
//
//    // ---------------- Lecture ----------------
//
//    override fun stream(seasonKey: String, guardianId: String): Flow<CotisationHousehold?> = callbackFlow {
//        val reg = householdsRef(seasonKey).document(guardianId)
//            .addSnapshotListener { snap, err ->
//                if (err != null) {
//                    trySend(null); return@addSnapshotListener
//                }
//                if (snap == null || !snap.exists()) {
//                    trySend(null); return@addSnapshotListener
//                }
//                val map = snap.data ?: emptyMap<String, Any?>()
//                trySend(mapToDoc(map))
//            }
//        awaitClose { reg.remove() }
//    }
//
//    override suspend fun get(seasonKey: String, guardianId: String): CotisationHousehold? {
//        val doc = householdsRef(seasonKey).document(guardianId).get().await()
//        if (!doc.exists()) return null
//        return mapToDoc(doc.data ?: emptyMap())
//    }
//
//    // ---------------- Écriture ----------------
//
//    /**
//     * Upsert d’un dossier foyer.
//     * ➜ IMPORTANT : synchronise aussi le miroir liseret (finance_status) selon le statut.
//     */
//    override suspend fun upsert(doc: CotisationHousehold) {
//        householdsRef(doc.seasonKey).document(doc.guardianId)
//            .set(docToMap(doc), SetOptions.merge())
//            .await()
//
//        // Propagation miroir liseret-bleu
//        syncMirrorsAndAdherentsPaid(doc, paid = (doc.status == HouseholdStatus.SOLDE))
//    }
//
//    /**
//     * Met à jour le statut du foyer (A_REGLER / EN_RETARD / SOLDE / ANNULE).
//     * ➜ Recharge le doc pour récupérer memberIds, puis propage le miroir.
//     */
//    override suspend fun setStatus(seasonKey: String, guardianId: String, status: HouseholdStatus) {
//        val ref = householdsRef(seasonKey).document(guardianId)
//
//        // 1) Update status
//        ref.update("status", status.name).await()
//
//        // 2) Reload to get memberIds
//        val snap = ref.get().await()
//        if (!snap.exists()) return
//        val updated = mapToDoc(snap.data ?: emptyMap()).copy(status = status)
//
//        // 3) Propagation miroir liseret-bleu
//        syncMirrorsAndAdherentsPaid(updated, paid = (status == HouseholdStatus.SOLDE))
//    }
//
//    /**
//     * Écrit / met à jour /finance_status/{season}/members/{adherentId} pour chaque memberId du foyer.
//     * Non sensible : { isSettled, updatedAt, householdKey }.
//     */
//    override suspend fun syncMirrorsAndAdherentsPaid(doc: CotisationHousehold, paid: Boolean) {
//        val members = doc.memberIds.orEmpty()
//        if (members.isEmpty()) return
//
//        val season = doc.seasonKey
//        val now = System.currentTimeMillis()
//
//        val payload = mapOf(
//            "isSettled" to paid,
//            "updatedAt" to now,
//            "householdKey" to doc.guardianId
//        )
//
//        for (memberId in members) {
//            financeMemberRef(season, memberId)
//                .set(payload, SetOptions.merge())
//                .await()
//        }
//    }
//
//    // ---------------- Mapping ----------------
//
//    private fun PaymentEntry.toMap(): Map<String, Any?> = mapOf(
//        "type" to type.name,
//        "amountCents" to amountCents,
//        "date" to date,
//        "index" to index,
//        "note" to note
//    )
//
//    private fun AidEntry.toMap(): Map<String, Any?> = mapOf(
//        "type" to type.name,
//        "code" to code,
//        "amountCents" to amountCents
//    )
//
//    private fun docToMap(d: CotisationHousehold): Map<String, Any?> = mapOf(
//        "guardianId" to d.guardianId,
//        "seasonKey" to d.seasonKey,
//        "memberIds" to d.memberIds,
//        "method" to d.method?.name,
//        "oneClassPerWeek" to d.oneClassPerWeek,
//        "status" to d.status.name,
//        "amountBaseCents" to d.amountBaseCents,
//        "discountCents" to d.discountCents,
//        "amountDueCents" to d.amountDueCents,
//        "licenceCount" to d.licenceCount,
//        "licenceAmountCents" to d.licenceAmountCents,
//        "chequesPlanCount" to d.chequesPlanCount,
//        "paymentsPlan" to d.paymentsPlan.map { it.toMap() },
//        "paymentsReceived" to d.paymentsReceived.map { it.toMap() },
//        "aids" to d.aids.map { it.toMap() },
//        "updatedAt" to d.updatedAt,
//        "updatedBy" to d.updatedBy
//    )
//
//    @Suppress("UNCHECKED_CAST")
//    private fun mapToDoc(m: Map<String, Any?>): CotisationHousehold {
//        val methodStr = m["method"] as? String
//        val statusStr = m["status"] as? String
//
//        fun listPaymentEntries(key: String): List<PaymentEntry> {
//            val raw = (m[key] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()
//            return raw.map { mm ->
//                PaymentEntry(
//                    type = (mm["type"] as? String)?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() } ?: PaymentMethod.CB,
//                    amountCents = (mm["amountCents"] as? Number)?.toLong() ?: 0L,
//                    date = mm["date"] as? String,
//                    index = (mm["index"] as? Number)?.toInt(),
//                    note = mm["note"] as? String
//                )
//            }
//        }
//
//        fun listAidEntries(): List<AidEntry> {
//            val raw = (m["aids"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()
//            return raw.mapNotNull { mm ->
//                val type = (mm["type"] as? String)?.let { runCatching { AidType.valueOf(it) }.getOrNull() } ?: return@mapNotNull null
//                val amount = (mm["amountCents"] as? Number)?.toLong() ?: return@mapNotNull null
//                val code = mm["code"] as? String
//                AidEntry(type = type, code = code, amountCents = amount)
//            }
//        }
//
//        return CotisationHousehold(
//            guardianId = (m["guardianId"] as? String).orEmpty(),
//            seasonKey = (m["seasonKey"] as? String).orEmpty(),
//            memberIds = ((m["memberIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()),
//            method = methodStr?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() },
//            oneClassPerWeek = m["oneClassPerWeek"] as? Boolean ?: false,
//            status = statusStr?.let { runCatching { HouseholdStatus.valueOf(it) }.getOrNull() } ?: HouseholdStatus.A_REGLER,
//            amountBaseCents = (m["amountBaseCents"] as? Number)?.toLong() ?: 0L,
//            discountCents = (m["discountCents"] as? Number)?.toLong() ?: 0L,
//            amountDueCents = (m["amountDueCents"] as? Number)?.toLong() ?: 0L,
//            licenceCount = (m["licenceCount"] as? Number)?.toInt() ?: 0,
//            licenceAmountCents = (m["licenceAmountCents"] as? Number)?.toLong() ?: 0L,
//            chequesPlanCount = (m["chequesPlanCount"] as? Number)?.toInt(),
//            paymentsPlan = listPaymentEntries("paymentsPlan"),
//            paymentsReceived = listPaymentEntries("paymentsReceived"),
//            aids = listAidEntries(),
//            updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
//            updatedBy = m["updatedBy"] as? String
//        )
//    }
//}
package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.domain.model.finance.*
import com.antechrist.adherentsapp.domain.repository.CotisationsHouseholdsRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CotisationsHouseholdsRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : CotisationsHouseholdsRepository {

    private fun householdsRef(seasonKey: String) =
        db.collection("cotisations_families")
            .document(seasonKey)
            .collection("households")

    private fun financeMemberRef(seasonKey: String, adherentId: String) =
        db.collection("finance_status")
            .document(seasonKey)
            .collection("members")
            .document(adherentId)

    override fun stream(seasonKey: String, guardianId: String): Flow<CotisationHousehold?> = callbackFlow {
        val reg = householdsRef(seasonKey).document(guardianId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(null); return@addSnapshotListener
                }
                if (snap == null || !snap.exists()) {
                    trySend(null); return@addSnapshotListener
                }
                val map = snap.data ?: emptyMap<String, Any?>()
                trySend(mapToDoc(map))
            }
        awaitClose { reg.remove() }
    }

    override suspend fun get(seasonKey: String, guardianId: String): CotisationHousehold? {
        val doc = householdsRef(seasonKey).document(guardianId).get().await()
        if (!doc.exists()) return null
        return mapToDoc(doc.data ?: emptyMap())
    }

    override suspend fun upsert(doc: CotisationHousehold) {
        householdsRef(doc.seasonKey).document(doc.guardianId)
            .set(docToMap(doc), SetOptions.merge())
            .await()

        syncMirrorsAndAdherentsPaid(doc, paid = (doc.status == HouseholdStatus.SOLDE))
    }

    override suspend fun setStatus(seasonKey: String, guardianId: String, status: HouseholdStatus) {
        val ref = householdsRef(seasonKey).document(guardianId)

        ref.update("status", status.name).await()

        val snap = ref.get().await()
        if (!snap.exists()) return
        val updated = mapToDoc(snap.data ?: emptyMap()).copy(status = status)

        syncMirrorsAndAdherentsPaid(updated, paid = (status == HouseholdStatus.SOLDE))
    }

    override suspend fun syncMirrorsAndAdherentsPaid(doc: CotisationHousehold, paid: Boolean) {
        val members = doc.memberIds.orEmpty()
        if (members.isEmpty()) return

        val season = doc.seasonKey
        val now = System.currentTimeMillis()

        val payload = mapOf(
            "isSettled" to paid,
            "updatedAt" to now,
            "householdKey" to doc.guardianId
        )

        for (memberId in members) {
            financeMemberRef(season, memberId)
                .set(payload, SetOptions.merge())
                .await()
        }
    }

    private fun PaymentEntry.toMap(): Map<String, Any?> = mapOf(
        "type" to type.name,
        "amountCents" to amountCents,
        "date" to date,
        "index" to index,
        "note" to note
    )

    private fun AidEntry.toMap(): Map<String, Any?> = mapOf(
        "type" to type.name,
        "code" to code,
        "amountCents" to amountCents
    )

    private fun docToMap(d: CotisationHousehold): Map<String, Any?> = mapOf(
        "guardianId" to d.guardianId,
        "seasonKey" to d.seasonKey,
        "memberIds" to d.memberIds,

        "method" to d.method?.name,
        "oneClassPerWeek" to d.oneClassPerWeek,

        // ✅ Nouveau champ (on n'écrit plus l'ancien)
        "exemptFromFee" to d.exemptFromFee,

        "status" to d.status.name,

        "amountBaseCents" to d.amountBaseCents,
        "discountCents" to d.discountCents,

        // ✅ Remise manuelle + motif
        "manualDiscountCents" to d.manualDiscountCents,
        "manualDiscountReason" to d.manualDiscountReason,

        "amountDueCents" to d.amountDueCents,

        // Licences : traçabilité
        "licenceCount" to d.licenceCount,
        "licenceAmountCents" to d.licenceAmountCents,
        "licencesTotalCents" to d.licencesTotalCents,

        "chequesPlanCount" to d.chequesPlanCount,
        "paymentsPlan" to d.paymentsPlan.map { it.toMap() },
        "paymentsReceived" to d.paymentsReceived.map { it.toMap() },

        "aids" to d.aids.map { it.toMap() },

        "updatedAt" to d.updatedAt,
        "updatedBy" to d.updatedBy
    )


    @Suppress("UNCHECKED_CAST")
    private fun mapToDoc(m: Map<String, Any?>): CotisationHousehold {
        val methodStr = m["method"] as? String
        val statusStr = m["status"] as? String

        fun listPaymentEntries(key: String): List<PaymentEntry> {
            val raw = (m[key] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()
            return raw.map { mm ->
                PaymentEntry(
                    type = (mm["type"] as? String)
                        ?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() }
                        ?: PaymentMethod.CB,
                    amountCents = (mm["amountCents"] as? Number)?.toLong() ?: 0L,
                    date = mm["date"] as? String,
                    index = (mm["index"] as? Number)?.toInt(),
                    note = mm["note"] as? String
                )
            }
        }

        fun listAidEntries(): List<AidEntry> {
            val raw = (m["aids"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()
            return raw.mapNotNull { mm ->
                val type = (mm["type"] as? String)
                    ?.let { runCatching { AidType.valueOf(it) }.getOrNull() }
                    ?: return@mapNotNull null
                val amount = (mm["amountCents"] as? Number)?.toLong() ?: return@mapNotNull null
                val code = mm["code"] as? String
                AidEntry(type = type, code = code, amountCents = amount)
            }
        }

        val licenceCount = (m["licenceCount"] as? Number)?.toInt() ?: 0
        val licenceAmountCents = (m["licenceAmountCents"] as? Number)?.toLong() ?: 0L

        // ✅ Migration douce : nouveau champ exemptFromFee, fallback sur l’ancien exemptClubCotisation
        val exemptFromFee =
            (m["exemptFromFee"] as? Boolean)
                ?: (m["exemptClubCotisation"] as? Boolean ?: false)

        // ✅ Nouveau champ licencesTotalCents, fallback calculé
        val licencesTotalCents =
            (m["licencesTotalCents"] as? Number)?.toLong()
                ?: (licenceCount.toLong() * licenceAmountCents)

        return CotisationHousehold(
            guardianId = (m["guardianId"] as? String).orEmpty(),
            seasonKey = (m["seasonKey"] as? String).orEmpty(),
            memberIds = ((m["memberIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()),

            method = methodStr?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() },

            oneClassPerWeek = m["oneClassPerWeek"] as? Boolean ?: false,
            exemptFromFee = exemptFromFee,

            status = statusStr?.let { runCatching { HouseholdStatus.valueOf(it) }.getOrNull() }
                ?: HouseholdStatus.A_REGLER,

            amountBaseCents = (m["amountBaseCents"] as? Number)?.toLong() ?: 0L,
            discountCents = (m["discountCents"] as? Number)?.toLong() ?: 0L,

            manualDiscountCents = (m["manualDiscountCents"] as? Number)?.toLong() ?: 0L,
            manualDiscountReason = m["manualDiscountReason"] as? String,

            amountDueCents = (m["amountDueCents"] as? Number)?.toLong() ?: 0L,

            licenceCount = licenceCount,
            licenceAmountCents = licenceAmountCents,
            licencesTotalCents = licencesTotalCents,

            chequesPlanCount = (m["chequesPlanCount"] as? Number)?.toInt(),
            paymentsPlan = listPaymentEntries("paymentsPlan"),
            paymentsReceived = listPaymentEntries("paymentsReceived"),

            aids = listAidEntries(),

            updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedBy = m["updatedBy"] as? String
        )
    }
}
