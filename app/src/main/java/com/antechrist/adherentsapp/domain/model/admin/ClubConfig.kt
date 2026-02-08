package com.antechrist.adherentsapp.domain.model.admin

data class ClubConfig(
    val id: String = "main",                 // unique
    val clubName: String = "",
    val clubCity: String = "",
    val presidentName: String = "",
    val secretaryName: String = "",
    val logoUrl: String? = null,
    val presidentSignatureUrl: String? = null,
    val secretarySignatureUrl: String? = null,
    val updatedAt: Long = 0L,
    val updatedBy: String? = null
)
