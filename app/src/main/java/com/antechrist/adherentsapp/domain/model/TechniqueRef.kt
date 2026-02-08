// com/antechrist/adherentsapp/domain/model/TechniqueRef.kt
package com.antechrist.adherentsapp.domain.model

/**
 * Référence d'une technique du catalogue Kihon.
 * Utilisée par les Steps d'un enchaînement.
 *
 * NB: Les positions (dachi) du référentiel sont aussi représentées
 *     via kind = POSITION pour unifier le catalogue (défenses/poings/pieds/positions).
 */
data class TechniqueRef(
    val id: String,          // Identifiant stable (ex: "ZENKUTSU_DACHI", "GEDAN_BARAI")
    val kind: Kind,          // Défense / Poing / Pied / Position
    val nameJa: String,      // Nom japonais canonique
    val nameFr: String,      // Traduction FR courte
    val aliases: List<String> = emptyList(), // Ex: ["Kosa-dachi", "Kake-dachi"]
    val subType: String? = null,             // Ex: "zuki", "uchi", "geri", "keage", "keikomi"
    val notes: String? = null                // Notes pédagogiques optionnelles
) {
    enum class Kind { DEFENSE, PUNCH, KICK, POSITION }
    /** Raccourci lisible par l’UI (affichage préférentiel FR) */
    val label: String
        get() = nameFr.ifBlank { nameJa }

    companion object {
        val Undefined = TechniqueRef(
            id = "undefined",
            kind = Kind.DEFENSE,
            nameJa = "Undefined",
            nameFr = "Indéfini"
        )
    }
}
