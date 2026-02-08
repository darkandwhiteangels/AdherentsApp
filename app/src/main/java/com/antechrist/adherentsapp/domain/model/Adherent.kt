package com.antechrist.adherentsapp.domain.model

import androidx.annotation.Keep

@Keep
data class Adherent(
    val id: String,               // Firestore docId
    val nom: String,
    val prenom: String,
    val dateNaissance: String?,
    val groups: List<String>? = null,
    val adresse: String?,
    val codePostal: String?,
    val ville: String?,
    val email: String?,
    val telephone: String?,
    val photoUri: String?,
    val photoUpdatedAt: Long? = null,

    // ---- Essai (2 présences max) ----
    val trialPresentCount: Int = 0,
    val trialCompleted: Boolean = false,

    // 🆕 liens vers parents/responsables
    val guardianIds: List<String>? = emptyList(),
    val primaryGuardianId: String? = null,
    val householdId: String? = null,

    /** Code normalisé: "<system>:<grade>", ex: "karate:jaune_orange", "judo:orange". */
    val beltCode: String? = null,
    /** Pour BJJ (0..3 typiquement). */
    val stripeCount: Int = 0,

    // ---- Rôle simple pour le formulaire ----
    val isPractitioner: Boolean = true,

    // ---- Finance : liseret liste adhérents ----
    val cotisationPaid: Boolean? = null,

    // ---- Autorisations / consentements ----
    val consentEvacuation: Boolean? = null,
    val consentImageSocial: Boolean? = null,
    val consentImageOfficial: Boolean? = null,
    val consentRI: Boolean? = null,
    val consentsUpdatedAt: Long? = null,

    // 🆕 Attribution (pour “ribbon” présence)
    /** "pussay" | "saclas" | "encadrant" ; null = pas d’étiquette */
    val attribution: String? = null,
    val isArchived: Boolean = false
) {
    val fullName: String get() = "$nom $prenom"
}
