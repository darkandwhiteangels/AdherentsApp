package com.antechrist.adherentsapp.domain.model.finance

/**
 * Paramétrage financier d’une saison (barèmes, remises, licence).
 * Stocké/chargé depuis la collection Firestore: /finance_seasons/{seasonKey}
 */
data class FinanceSeasonConfig(
    val seasonKey: String,

    // Barèmes (centimes d’euro)
    val priceAdultCents: Long = 18300,         // Adultes 17+
    val priceChild6to16Cents: Long = 16800,    // Enfants 6–16
    val priceBabyUnder6Cents: Long = 11100,    // < 6 ans

    // Forfaits famille si tous >5 ans
    val bundle2AllOver5Cents: Long = 30600,    // 2 adhérents (>5)
    val bundle3AllOver5Cents: Long = 43200,    // 3 adhérents (>5)

    // Remise "1 cours / semaine" (par foyer, appliquée une seule fois)
    val oneClassPerWeekDiscountCents: Long = 3000,

    // REMISES (checkbox contrôlées)
    val discountBlackBeltCents: Long = 0L,
    val discountFamilyGradedCents: Long = 0L,
    val discountAssistantProfCents: Long = 0L,

    // Licence (paramétrable par saison)
    val licencePaidByClub: Boolean = true,     // si true, le club paie la licence
    val licenceAmountCents: Long = 3900,       // montant unitaire de la licence

    // Vérrouillage des paramètres pour la saison
    val locked: Boolean = false,
    val lockedAt: Long? = null,
    val lockedByUid: String? = null,

    // Date d’échéance par défaut (optionnelle, "yyyy-MM-dd")
    val defaultDueDate: String? = null,

    // ✅ Fin de saison
    val status: String = "OPEN",          // OPEN | CLOSING | CLOSED
    val closedAt: Long? = null,
    val closedByUid: String? = null

)