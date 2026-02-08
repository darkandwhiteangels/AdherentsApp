package com.antechrist.adherentsapp.data.model.dto

data class OptionRefDto(
    val id: String? = null,
    val group: String? = null,       // e.g. SIDE/TARGET/DISTANCE/DIRECTION
    val nameFr: String? = null,
    val nameJa: String? = null,
    val value: String? = null
)
