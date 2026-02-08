package com.antechrist.adherentsapp.domain.model

data class NotificationGroup(
    val id: String,
    val name: String,
    // val memberAdherentIds: List<String>,
    val memberGuardianIds: List<String>,
    val createdAt: Long,
    val createdByUid: String,
    val active: Boolean = true
)
