package com.antechrist.adherentsapp.domain.model.finance

data class CotisationHousehold(
    val guardianId: String = "",
    val seasonKey: String = "",
    val memberIds: List<String> = emptyList(),

    val method: PaymentMethod? = null,
    val oneClassPerWeek: Boolean = false,
    val exemptFromFee: Boolean = false,          // ✅ aligné calculateur

    val discountBlackBeltEnabled: Boolean = false,
    val discountFamilyGradedEnabled: Boolean = false,
    val discountAssistantProfEnabled: Boolean = false,

    val status: HouseholdStatus = HouseholdStatus.A_REGLER,

    // --- Montants ---
    val amountBaseCents: Long = 0L,

    /** Remise automatique (ex: 1 cours/semaine) */
    val discountCents: Long = 0L,

    /** Remise exceptionnelle saisie manuellement */
    val manualDiscountCents: Long = 0L,
    val manualDiscountReason: String? = null,

    /** Montant final à payer :
     * (base ou 0 si exonéré) - remises - aides + licences si non payées par le club
     */
    val amountDueCents: Long = 0L,

    // --- Licence FFK (traçabilité) ---
    val licenceCount: Int = 0,
    val licenceAmountCents: Long = 0L,
    val licencesTotalCents: Long = 0L,           // ✅ clé pour rapports FFK

    // --- Paiements ---
    val chequesPlanCount: Int? = null,
    val paymentsPlan: List<PaymentEntry> = emptyList(),
    val paymentsReceived: List<PaymentEntry> = emptyList(),

    // --- Aides ---
    val aids: List<AidEntry> = emptyList(),

    val updatedAt: Long? = null,
    val updatedBy: String? = null
)
