// com/antechrist/adherentsapp/domain/model/OptionRef.kt
package com.antechrist.adherentsapp.domain.model

data class OptionRef(
    val id: String,
    val group: OptionGroup,
    val nameFr: String,
    val nameJa: String,
    val value: String
)

enum class OptionGroup { SIDE, RELATION, REPOSE, LIMB, DIRECTION }
