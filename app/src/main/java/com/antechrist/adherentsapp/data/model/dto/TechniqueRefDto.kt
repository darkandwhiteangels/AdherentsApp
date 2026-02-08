// com/antechrist/adherentsapp/data/model/dto/TechniqueRefDto.kt
package com.antechrist.adherentsapp.data.model.dto

import androidx.annotation.Keep
import com.antechrist.adherentsapp.data.model.TechniqueKind

/**
 * Technique du catalogue Kihon (POSITION, DEFENSE, PUNCH, KICK).
 * DTO Firestore : champs nullable pour tolérer les migrations/versions.
 */
@Keep
data class TechniqueRefDto(
    val id: String? = null,                 // ex: "ZENKUTSU_DACHI"
    val kind: TechniqueKind? = null,        // POSITION / DEFENSE / PUNCH / KICK
    val nameJa: String? = null,             // Nom japonais canonique
    val nameFr: String? = null,             // Traduction FR
    val aliases: List<String>? = null,      // Variantes orthographiques
    val subType: String? = null,            // ex: "zuki", "uchi", "geri", "keage", ...
    val notes: String? = null               // Notes pédagogiques
)

/* ========================================================================== */
/* =========== Helper d'écriture "safe" de TechniqueRefDto vers FS ==========*/
/* ========================================================================== */

/**
 * Construit une Map STRICTEMENT conforme aux règles :
 * - id/kind/nameFr OBLIGATOIRES (string)
 * - nameJa/aliases/subType/notes ABSENTS si vides
 * - aliases n'est jamais null : absente si vide
 */
fun TechniqueRefDto.asFirestoreMap(): Map<String, Any> {
    val _id   = requireNotNull(id)   { "TechniqueRefDto.id is null" }
    val _kind = requireNotNull(kind) { "TechniqueRefDto.kind is null" }

    val out = LinkedHashMap<String, Any>(6)
    out["id"]      = _id
    out["kind"]    = _kind.name                  // règles: string
    out["nameFr"]  = (nameFr ?: "")              // règles: string (autorise vide)
    if (!nameJa.isNullOrBlank()) out["nameJa"] = nameJa

    // aliases : ABSENT si vide (les règles refusent null)
    val a = aliases ?: emptyList()
    if (a.isNotEmpty()) out["aliases"] = a

    if (!subType.isNullOrBlank()) out["subType"] = subType
    if (!notes.isNullOrBlank())   out["notes"]   = notes
    return out
}
