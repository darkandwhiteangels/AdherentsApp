// domain/model/PresenceRow.kt
package com.antechrist.adherentsapp.domain.model

import androidx.annotation.Keep

enum class PresenceStatus { PRESENT, ABSENT, RETARD }
@Keep
data class PresenceRow(
    val nom: String,            // "DUPONT"
    val prenom: String,         // "Jean"
    val statut: PresenceStatus, // PRESENT / ABSENT / RETARD
    val note: String? = null
)
