package com.antechrist.adherentsapp.domain.model.finance

data class AidEntry(
    val type: AidType,
    val amountCents: Long,
    val code: String? = null,
    val date: String? = null,
    val note: String? = null
)
