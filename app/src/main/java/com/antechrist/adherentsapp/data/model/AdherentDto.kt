package com.antechrist.adherentsapp.data.model

import androidx.annotation.Keep

@Keep
data class AdherentDto(
    val nom: String? = null,
    val prenom: String? = null,
    val dateNaissance: String? = null,
    val adresse: String? = null,
    val codePostal: String? = null,
    val ville: String? = null,
    val email: String? = null,
    val telephone: String? = null,
    val photoUri: String? = null, // ✅ même nom que domain
    val photoUpdatedAt: Long? = null,
    val cotisationPaid: Boolean? = null,
    val guardianIds: List<String>? = null,
    val primaryGuardianId: String? = null,
    val householdId: String? = null,
    val beltCode: String? = null,
    val stripeCount: Int? = null,

    val isPractitioner: Boolean? = null,

    val groups: List<String>? = null,

    val attribution: String? = null,

    val consents: Map<String, @JvmSuppressWildcards Any?>? = null,
    val isArchived: Boolean? = false
)
