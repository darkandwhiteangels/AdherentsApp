// com/antechrist/adherentsapp/domain/model/LevelRef.kt
package com.antechrist.adherentsapp.domain.model

data class LevelRef(
    val id: String,
    val labelFr: String,
    val nameJa: String,
    val order: Int,
    val description: String? = null
)
