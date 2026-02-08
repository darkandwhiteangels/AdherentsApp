package com.antechrist.adherentsapp.domain.model.finance

data class PaymentEntry(
    val type: PaymentMethod,
    val amountCents: Long,
    val date: String? = null,   // ISO yyyy-MM-dd si présent
    val index: Int? = null,     // pour 1/2/3 chèques
    val note: String? = null
)
