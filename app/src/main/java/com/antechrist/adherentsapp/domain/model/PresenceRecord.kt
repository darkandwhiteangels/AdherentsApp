package com.antechrist.adherentsapp.domain.model

import androidx.annotation.Keep

@Keep
/** Lecture (ce qu’on reçoit de Firestore) */
data class PresenceRecord(
    val status: String,            // "present" | "absent"
    val note: String? = null,
    val updatedBy: String? = null,
    val updatedAt: Long? = null,
    val trialCounted: Boolean = false // indique si cette séance compte pour l’essai
)

/** Écriture (ce qu’on envoie à Firestore) */
data class PresenceUpsert(
    val status: String,             // "present" | "absent"
    val note: String? = null,       // optionnel
    val updatedBy: String? = null,  // optionnel
    val updatedAt: Long? = null,    // optionnel (timestamp ms)
    val trialCounted: Boolean? = null // optionnel (si précisé)
)
