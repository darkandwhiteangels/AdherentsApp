package com.antechrist.adherentsapp.domain.finance

import com.antechrist.adherentsapp.domain.model.finance.AidEntry
import com.antechrist.adherentsapp.domain.model.finance.FinanceSeasonConfig
import kotlin.math.max

data class CalcResult(
    val amountBaseCents: Long,
    val discountCents: Long,              // remise 1 cours/semaine
    val controlledDiscountCents: Long,    // ✅ remises cases à cocher
    val manualDiscountCents: Long,        // remise manuelle
    val aidsTotalCents: Long,             // total aides
    val amountDueCents: Long,             // total à payer
    val licenceCount: Int,
    val licenceAmountCents: Long,
    val licencesTotalCents: Long
)

/**
 * ages : âges en années (nullable si DOB manquante)
 * oneClassPerWeek : remise foyer unique (ex: 30€) si true
 * aids : aides (Pass'Sport, CAF, etc.) -> DÉDUITES du montant dû (nouvelle logique)
 * manualDiscountCents : remise manuelle exceptionnelle (nouveau)
 * exemptFromFee : exonéré de cotisation (bureau/prof) -> cotisation = 0, licence peut rester due (nouveau)
 *
 * Règle (tarifs licence incluse) :
 * MONTANT DÛ = (BaseCotisation - remises - aides)
 * Si exonéré : BaseCotisation = 0
 *
 * La licence est tracée séparément via :
 *  - licenceCount = nombre de pratiquants
 *  - licencesTotalCents = licenceCount × prix licence (montant à reverser à la FFK)
**/
fun calculateHouseholdAmounts(
    ages: List<Int?>,
    cfg: FinanceSeasonConfig,
    oneClassPerWeek: Boolean,
    aids: List<AidEntry>,
    manualDiscountCents: Long = 0L,
    exemptFromFee: Boolean = false,
    discountBlackBeltEnabled: Boolean = false,
    discountFamilyGradedEnabled: Boolean = false,
    discountAssistantProfEnabled: Boolean = false
): CalcResult {
    val baby = ages.count { it != null && it < 6 }
    val sixTo16 = ages.count { it != null && it in 6..16 }
    val adult = ages.count { it != null && it >= 17 }
    val total = baby + sixTo16 + adult

    var base = 0L
    if (baby > 0) {
        // Cas B — au moins un < 6 ans : calcul "au réel"
        base += baby * cfg.priceBabyUnder6Cents
        base += sixTo16 * cfg.priceChild6to16Cents
        base += adult * cfg.priceAdultCents
    } else {
        // Cas A — tous > 5 ans
        when (total) {
            0 -> base = 0
            1 -> {
                base += if (adult == 1) cfg.priceAdultCents else cfg.priceChild6to16Cents
            }
            2 -> base += cfg.bundle2AllOver5Cents
            3 -> base += cfg.bundle3AllOver5Cents
            else -> {
                // MVP : 3-forfait + reste au réel
                base += cfg.bundle3AllOver5Cents
                val rest = total - 3

                // heuristique simple : prioriser adultes en "reste"
                val extraAdults = max(0, adult - 1) // on suppose 1 adulte dans le forfait 3
                val extraKids = rest - extraAdults

                base += extraAdults * cfg.priceAdultCents
                base += extraKids.coerceAtLeast(0) * cfg.priceChild6to16Cents
            }
        }
    }

    val discount = if (oneClassPerWeek) cfg.oneClassPerWeekDiscountCents else 0L


    val aidsTotalCents = aids.sumOf { it.amountCents }

    // ✅ Tarifs "licence incluse" : la licence ne s'ajoute jamais au montant dû
    val togglesDiscountCents =
        (if (discountBlackBeltEnabled) cfg.discountBlackBeltCents else 0L) +
                (if (discountFamilyGradedEnabled) cfg.discountFamilyGradedCents else 0L) +
                (if (discountAssistantProfEnabled) cfg.discountAssistantProfCents else 0L)

    val totalDiscountCents = discount + togglesDiscountCents + manualDiscountCents + aidsTotalCents

// ✅ Tarifs licence incluse : la licence ne s'ajoute jamais au dû
    val feeBase = if (exemptFromFee) 0L else base

    // Montant dû = base (ou 0 si exonéré) - remises - aides
    val due = (feeBase - totalDiscountCents).coerceAtLeast(0L)

    val licencesTotalCents = total.toLong() * cfg.licenceAmountCents

    return CalcResult(
        amountBaseCents = base,
        discountCents = discount,
        controlledDiscountCents = togglesDiscountCents,
        manualDiscountCents = manualDiscountCents,
        aidsTotalCents = aidsTotalCents,
        amountDueCents = due,
        licenceCount = total,
        licenceAmountCents = cfg.licenceAmountCents,
        licencesTotalCents = licencesTotalCents
    )
}
