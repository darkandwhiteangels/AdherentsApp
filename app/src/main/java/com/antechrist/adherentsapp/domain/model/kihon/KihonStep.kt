//package com.antechrist.adherentsapp.domain.model.kihon
//
//import com.antechrist.adherentsapp.domain.model.TechniqueRef
//
///**
// * Représente une action complète d’un kihon :
// * départ → mouvement → technique → position finale.
// *
// * Chaque step est autonome et lisible pédagogiquement,
// * avec une logique biomécanique (avant/arrière, bras avant, jambe arrière, etc.)
// * au lieu de gauche/droite fixes.
// */
//data class KihonStep(
//
//    val id: String? = null,
//
//    // Position ou garde de départ (hidari kamae, migi zenkutsu, etc.)
//    val startPosition: TechniqueRef,
//
//    // Type de déplacement (avancer, reculer, sur place, pivot, etc.)
//    val movement: Movement = Movement.ON_PLACE,
//
//    // Technique exécutée (gyaku zuki, uchi uke, mae geri, etc.)
//    val technique: TechniqueRef,
//
//    // Rôle biomécanique du membre exécutant (avant, arrière, jambe avant, jambe arrière, etc.)
//    val executingLimbRole: ExecutingLimbRole = ExecutingLimbRole.NONE,
//
//    // Jambe active (utile pour les déplacements ou coups de pied)
//    val activeFoot: ActiveFoot = ActiveFoot.NONE,
//
//    // Où repose le pied actif après l’action (front, back, same place, none)
//    val footLanding: FootLanding = FootLanding.NONE,
//
//    // Position finale après l’action (zenkutsu, neko, moto, etc.)
//    val endPosition: TechniqueRef,
//
//    // Niveau de la technique (jodan, chudan, gedan)
//    val height: Height? = null,
//
//    // Rythme d’exécution (lent, normal, rapide, enchaîné)
//    val tempo: Tempo = Tempo.NORMAL,
//
//    // Direction ou orientation du mouvement (avant, arrière, pivot, etc.)
//    val direction: Direction? = Direction.FORWARD,
//
//    // Relation avec le step précédent (garde conservée, inversée)
//    val relationToPrevious: RelationToPrevious = RelationToPrevious.NONE,
//
//    // Groupe logique d’action (ex: “blocage + contre”)
//    val comboGroup: String? = null,
//
//    // Indique si un kiai est effectué sur ce step
//    val kiai: Boolean = false,
//
//    // Commentaire ou consigne libre (usage pédagogique ou interne)
//    val notes: String? = null,
//)
//
///* -----------------------------------------------------------
// * ENUMS
// * -----------------------------------------------------------
// */
//
///**
// * Type de mouvement entre le départ et la fin du step.
// *
// * Aligné avec le catalogue UI (KihonEditorToolBar / movementItems)
// */
//enum class Movement {
//    NONE,               // pas encore défini dans le draft
//    ON_PLACE,           // sur place
//
//    FORWARD,            // avancer (Ayumi-ashi)
//    BACKWARD,           // reculer
//    LATERAL,            // déplacement latéral (Yoko-ashi)
//
//    CHASSE_FORWARD,     // Tsugi-ashi avant
//    TIRES_FORWARD,      // Yori-ashi avant
//    CHASSE_BACK,        // Tsugi-ashi arrière
//    TIRES_BACK,         // Yori-ashi arrière
//
//    PIVOT_90_IN,        // pivot 90° intérieur (mawari-ashi)
//    PIVOT_90_OUT,       // pivot 90° extérieur (ushiro mawari-ashi)
//    PIVOT_180           // pivot 180° (ushiro mawari-ashi complet)
//}
//
///**
// * Décrit quel membre agit dans le step (bras ou jambe)
// * en fonction de sa position biomécanique.
// */
//enum class ExecutingLimbRole {
//    FRONT_SIDE,     // même côté que la jambe avant finale (oi)
//    BACK_SIDE,      // opposé à la jambe avant finale (gyaku)
//    LEAD_LEG,       // jambe avant qui agit
//    TRAIL_LEG,      // jambe arrière qui agit
//    BOTH,           // deux membres en simultané
//    NONE            // aucun membre dominant (déplacement seul)
//}
//
///**
// * Décrit quel pied agit durant le déplacement.
// */
//enum class ActiveFoot {
//    LEAD_LEG,
//    TRAIL_LEG,
//    NONE
//}
//
///**
// * Spécifie où repose le pied après une technique de jambe.
// */
//enum class FootLanding {
//    FRONT,
//    BACK,
//    SAME_PLACE,
//    NONE
//}
//
///**
// * Niveau de la technique.
// */
//enum class Height {
//    JODAN,      // haut
//    CHUDAN,     // moyen
//    GEDAN       // bas
//}
//
///**
// * Rythme du mouvement.
// */
//enum class Tempo {
//    LENT,
//    NORMAL,
//    RAPIDE,
//    ENCHAINE
//}
//
///**
// * Direction du mouvement.
// */
//enum class Direction {
//    FORWARD,
//    BACKWARD,
//    LEFT,
//    RIGHT,
//    PIVOT_IN,
//    PIVOT_OUT,
//    NONE
//}
//
///**
// * Relation biomécanique avec le step précédent.
// */
//enum class RelationToPrevious {
//    SAME_SIDE,
//    OPPOSITE_SIDE,
//    NONE
//}
//
///**
// * Type d’option que l’on peut adjoindre à une étape (token d’option).
// * KIAI : simple flag booléen
// * Les autres : value portée dans DraftToken.OptionToken.value
// */
//enum class OptionKind {
//    KIAI,               // bool (true = kiai)
//    FOOT_LANDING,       // FootLanding
//    EXECUTING_LIMB,     // ExecutingLimbRole
//    DIRECTION,          // Direction
//    TEMPO,              // Tempo
//    HEIGHT              // Height (JODAN/CHUDAN/GEDAN)
//}

package com.antechrist.adherentsapp.domain.model.kihon

import com.antechrist.adherentsapp.domain.model.TechniqueRef

/**
 * Représente une action complète d’un kihon :
 * départ → mouvement → technique → position finale.
 *
 * Chaque step est autonome et lisible pédagogiquement,
 * avec une logique biomécanique (avant/arrière, bras/jambe, même bras/jambe, etc.)
 * au lieu de gauche/droite fixes.
 */
data class KihonStep(

    val id: String? = null,

    // Position ou garde de départ (hidari kamae, migi zenkutsu, etc.)
    val startPosition: TechniqueRef,

    // Type de déplacement (avancer, reculer, sur place, pivot, etc.)
    val movement: Movement = Movement.ON_PLACE,

    // Technique exécutée (gyaku zuki, uchi uke, mae geri, etc.)
    val technique: TechniqueRef,

    /**
     * Rôle biomécanique du membre exécutant :
     * - BRAS_AVANT / BRAS_ARRIERE
     * - JAMBE_AVANT / JAMBE_ARRIERE
     * - BOTH (les deux en simultané)
     * - NONE (déplacement seul / non pertinent)
     */
    val executingLimbRole: ExecutingLimbRole = ExecutingLimbRole.NONE,

    // Jambe active (utile pour les déplacements ou coups de pied)
    val activeFoot: ActiveFoot = ActiveFoot.NONE,

    /**
     * Où repose le pied actif après l’action :
     * - DEVANT (front)
     * - DERRIERE (back)
     * - MEME_ENDROIT (same place)
     * - NONE (non applicable)
     */
    val footLanding: FootLanding = FootLanding.NONE,

    // Position finale après l’action (zenkutsu, neko, moto, etc.)
    val endPosition: TechniqueRef,

    // Niveau de la technique (jodan, chudan, gedan)
    val height: Height? = null,

    // Rythme d’exécution (lent, normal, rapide, enchaîné)
    val tempo: Tempo = Tempo.NORMAL,

    // Direction ou orientation du mouvement (avant, arrière, pivot, etc.)
    val direction: Direction? = Direction.FORWARD,

    // Relation avec le step précédent (garde conservée, inversée)
    val relationToPrevious: RelationToPrevious = RelationToPrevious.NONE,

    // Préférence par rapport au step précédent : même bras / même jambe / aucune
    val sameLimbPreference: SameLimbPreference = SameLimbPreference.NONE,

    // Groupe logique d’action (ex: “blocage + contre”)
    val comboGroup: String? = null,

    // Indique si un kiai est effectué sur ce step
    val kiai: Boolean = false,

    // Commentaire ou consigne libre (usage pédagogique ou interne)
    val notes: String? = null,
)

/* -----------------------------------------------------------
 * ENUMS
 * -----------------------------------------------------------
 */

/**
 * Type de mouvement entre le départ et la fin du step.
 *
 * Aligné avec le catalogue UI (KihonEditorToolBar / movementItems)
 */
enum class Movement {
    NONE,               // pas encore défini dans le draft
    ON_PLACE,           // sur place

    FORWARD,            // avancer (Ayumi-ashi)
    BACKWARD,           // reculer
    LATERAL,            // déplacement latéral (Yoko-ashi)

    CHASSE_FORWARD,     // Tsugi-ashi avant
    TIRES_FORWARD,      // Yori-ashi avant
    CHASSE_BACK,        // Tsugi-ashi arrière
    TIRES_BACK,         // Yori-ashi arrière

    PIVOT_90_IN,        // pivot 90° intérieur (mawari-ashi)
    PIVOT_90_OUT,       // pivot 90° extérieur (ushiro mawari-ashi)
    PIVOT_180           // pivot 180° (ushiro mawari-ashi complet)
}

/**
 * Rôle du membre exécutant (bras/jambe + avant/arrière).
 *
 * Couvre exactement :
 * - bras avant / bras arrière
 * - jambe avant / jambe arrière
 * + BOTH / NONE
 */
enum class ExecutingLimbRole {
    BRAS_AVANT,
    BRAS_ARRIERE,
    JAMBE_AVANT,
    JAMBE_ARRIERE,
    BOTH,
    NONE
}

/**
 * Décrit quel pied agit durant le déplacement.
 * (utile pour certains déplacements/enchaînements)
 */
enum class ActiveFoot {
    LEAD_LEG,
    TRAIL_LEG,
    NONE
}

/**
 * Spécifie où repose le pied après une technique de jambe.
 * (repose devant / repose derrière / même endroit)
 */
enum class FootLanding {
    DEVANT,
    DERRIERE,
    MEME_ENDROIT,
    NONE
}

/**
 * Niveau de la technique.
 */
enum class Height {
    JODAN,      // haut
    CHUDAN,     // moyen
    GEDAN       // bas
}

/**
 * Rythme du mouvement.
 */
enum class Tempo {
    LENT,
    NORMAL,
    RAPIDE,
    ENCHAINE
}

/**
 * Direction du mouvement.
 */
enum class Direction {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT,
    PIVOT_IN,
    PIVOT_OUT,
    NONE
}

/**
 * Relation biomécanique avec le step précédent (garde).
 */
enum class RelationToPrevious {
    SAME_SIDE,
    OPPOSITE_SIDE,
    NONE
}

/**
 * Préférence d’enchaînement par rapport au step précédent.
 * (même bras ou même jambe que l’étape précédente)
 */
enum class SameLimbPreference {
    SAME_ARM,
    SAME_LEG,
    NONE
}

/**
 * Type d’option (token) que l’on peut adjoindre à une étape.
 * KIAI : simple flag booléen
 * Les autres portent une value (via DraftToken.OptionToken côté draft).
 */
enum class OptionKind {
    KIAI,               // bool (true = kiai)
    FOOT_LANDING,       // FootLanding (DEVANT/DERRIERE/MEME_ENDROIT/NONE)
    EXECUTING_LIMB,     // ExecutingLimbRole (BRAS/JAMBE + AVANT/ARRIERE/BOTH/NONE)
    DIRECTION,          // Direction
    TEMPO,              // Tempo
    HEIGHT,             // Height (JODAN/CHUDAN/GEDAN)
    SAME_ARM,           // bool → applique une préférence "même bras que l’étape précédente"
    SAME_LEG            // bool → applique une préférence "même jambe que l’étape précédente"
}
