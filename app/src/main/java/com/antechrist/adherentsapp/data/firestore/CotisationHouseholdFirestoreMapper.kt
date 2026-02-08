package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.domain.model.finance.AidEntry
import com.antechrist.adherentsapp.domain.model.finance.AidType
import com.antechrist.adherentsapp.domain.model.finance.CotisationHousehold
import com.antechrist.adherentsapp.domain.model.finance.HouseholdStatus
import com.antechrist.adherentsapp.domain.model.finance.PaymentEntry
import com.antechrist.adherentsapp.domain.model.finance.PaymentMethod
import com.google.firebase.firestore.DocumentSnapshot

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toCotisationHousehold(): CotisationHousehold {
    val guardianId = this.id
    val seasonKey = this.reference.parent?.parent?.id ?: ""

    val memberIds = (get("memberIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

    val method = (getString("method"))?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() }
    val oneClassPerWeek = getBoolean("oneClassPerWeek") ?: false

    val status = (getString("status"))?.let { runCatching { HouseholdStatus.valueOf(it) }.getOrNull() }
        ?: HouseholdStatus.A_REGLER

    val amountBaseCents = getLong("amountBaseCents") ?: 0L
    val discountCents = getLong("discountCents") ?: 0L

    val manualDiscountCents = getLong("manualDiscountCents") ?: 0L
    val manualDiscountReason = getString("manualDiscountReason")

    // ✅ Migration douce : nouveau champ exemptFromFee, fallback sur ancien exemptClubCotisation
    val exemptFromFee =
        (getBoolean("exemptFromFee"))
            ?: (getBoolean("exemptClubCotisation") ?: false)

    val amountDueCents = getLong("amountDueCents") ?: 0L

    val licenceCount = (getLong("licenceCount") ?: 0L).toInt()
    val licenceAmountCents = getLong("licenceAmountCents") ?: 0L

    // ✅ Nouveau champ traçabilité, fallback calculé
    val licencesTotalCents =
        (getLong("licencesTotalCents") ?: (licenceCount.toLong() * licenceAmountCents))

    val chequesPlanCount = (getLong("chequesPlanCount") ?: 0L).toInt().takeIf { it > 0 }

    val paymentsPlan = (get("paymentsPlan") as? List<*>)?.mapNotNull { it.asPaymentEntry() } ?: emptyList()
    val paymentsReceived = (get("paymentsReceived") as? List<*>)?.mapNotNull { it.asPaymentEntry() } ?: emptyList()

    val aids = (get("aids") as? List<*>)?.mapNotNull { it.asAidEntry() } ?: emptyList()

    val updatedAt = getLong("updatedAt")
    val updatedBy = getString("updatedBy")

    return CotisationHousehold(
        guardianId = guardianId,
        seasonKey = seasonKey,
        memberIds = memberIds,
        method = method,
        oneClassPerWeek = oneClassPerWeek,
        exemptFromFee = exemptFromFee,                // ✅ nouveau
        status = status,

        amountBaseCents = amountBaseCents,
        discountCents = discountCents,
        manualDiscountCents = manualDiscountCents,    // ✅ nouveau
        manualDiscountReason = manualDiscountReason,  // ✅ nouveau
        amountDueCents = amountDueCents,

        licenceCount = licenceCount,
        licenceAmountCents = licenceAmountCents,
        licencesTotalCents = licencesTotalCents,      // ✅ nouveau

        chequesPlanCount = chequesPlanCount,
        paymentsPlan = paymentsPlan,
        paymentsReceived = paymentsReceived,

        aids = aids,

        updatedAt = updatedAt,
        updatedBy = updatedBy
    )
}


fun CotisationHousehold.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "guardianId" to guardianId,
        "seasonKey" to seasonKey,
        "memberIds" to memberIds,

        "method" to method?.name,
        "oneClassPerWeek" to oneClassPerWeek,
        "exemptFromFee" to exemptFromFee, // ✅ nouveau champ (et seul champ)
        "status" to status.name,

        "amountBaseCents" to amountBaseCents,
        "discountCents" to discountCents,
        "manualDiscountCents" to manualDiscountCents,
        "manualDiscountReason" to manualDiscountReason,
        "amountDueCents" to amountDueCents,

        "licenceCount" to licenceCount,
        "licenceAmountCents" to licenceAmountCents,
        "licencesTotalCents" to licencesTotalCents,

        "chequesPlanCount" to chequesPlanCount,
        "paymentsPlan" to paymentsPlan.map { it.toMap() },
        "paymentsReceived" to paymentsReceived.map { it.toMap() },

        "aids" to aids.map { it.toMap() },

        "updatedAt" to (updatedAt ?: System.currentTimeMillis()),
        "updatedBy" to updatedBy
    )
}

private fun Any?.asPaymentEntry(): PaymentEntry? {
    val m = this as? Map<*, *> ?: return null
    val type = (m["type"] as? String)?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() } ?: return null
    val amount = (m["amountCents"] as? Number)?.toLong() ?: return null
    val date = m["date"] as? String
    val index = (m["index"] as? Number)?.toInt()
    val note = m["note"] as? String
    return PaymentEntry(type = type, amountCents = amount, date = date, index = index, note = note)
}

private fun PaymentEntry.toMap(): Map<String, Any?> = mapOf(
    "type" to type.name,
    "amountCents" to amountCents,
    "date" to date,
    "index" to index,
    "note" to note
)

private fun Any?.asAidEntry(): AidEntry? {
    val m = this as? Map<*, *> ?: return null
    val type = (m["type"] as? String)?.let { runCatching { AidType.valueOf(it) }.getOrNull() } ?: return null
    val amount = (m["amountCents"] as? Number)?.toLong() ?: return null
    val code = m["code"] as? String
    val date = m["date"] as? String
    val note = m["note"] as? String
    return AidEntry(type = type, amountCents = amount, code = code, date = date, note = note)
}

private fun AidEntry.toMap(): Map<String, Any?> = mapOf(
    "type" to type.name,
    "amountCents" to amountCents,
    "code" to code,
    "date" to date,
    "note" to note
)
