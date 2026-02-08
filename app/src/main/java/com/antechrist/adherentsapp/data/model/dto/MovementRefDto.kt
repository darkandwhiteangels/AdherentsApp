package com.antechrist.adherentsapp.data.model.dto

data class MovementRefDto(
    val id: String? = null,
    val nameFr: String? = null,
    val nameJa: String? = null,
    val category: String? = null,     // e.g. STATIC/STEP/SLIDE/PIVOT/LUNGE
    val angleDeg: Int? = null,
    val direction: String? = null     // e.g. AVANT/ARRIERE/GAUCHE/DROITE/...
)
