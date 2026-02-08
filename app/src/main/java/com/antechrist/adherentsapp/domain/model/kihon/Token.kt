package com.antechrist.adherentsapp.domain.model.kihon

import java.util.UUID

/**
 * Un item élémentaire saisi dans la séquence (catalog-driven).
 * L’affichage résout refId -> labels via le catalogue.
 */
enum class TokenType { TECHNIQUE, POSITION, MOVEMENT, OPTION, LEVEL }

data class Token(
    val tokenId: String = UUID.randomUUID().toString(),
    val type: TokenType,
    val refId: String,          // ex: "TECH.DEF.SHUTO_UKE", "MOV.AYUMI_ASHI_FORWARD", "OPT.RELATION.GYAKU", "LVL.CHUDAN"
    val order: Int = 0,         // recalculé 0..n-1 après chaque mutation
    val meta: Map<String, Any?>? = null,
    val ts: Long = System.currentTimeMillis()
)

/** Constantes projet (defaults) */
object KihonDefaults {
    const val CATALOG_VERSION = "2025.1"
    // Règle métier: Hidari kamae en premier si rien n'est précisé
    const val DEFAULT_OPENING_POSITION_ID = "TECH.POS.HIDARI_KAMAE"
}
