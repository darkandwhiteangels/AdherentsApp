// com/antechrist/adherentsapp/domain/model/MovementRef.kt
package com.antechrist.adherentsapp.domain.model

data class MovementRef(
    val id: String,
    val nameFr: String,
    val nameJa: String,
    val category: MovementCategory,
    val angleDeg: Int?,          // ex: 0, 90, 180 (null si non pertinent)
    val direction: MovementDirection // avant, arrière, gauche, droite, neutre, diagonal…
)

enum class MovementCategory { STATIC, STEP, SLIDE, PIVOT }

enum class MovementDirection {
    NEUTRE, AVANT, ARRIERE, AVANT_DIAGONAL, ARRIERE_DIAGONAL
}
