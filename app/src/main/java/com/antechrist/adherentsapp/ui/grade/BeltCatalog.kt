package com.antechrist.adherentsapp.ui.grade

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Catalogues & helpers pour rendre les ceintures (v1 : Karaté).
 *
 * Conventions:
 * - beltCode format: "<art>:<grade>"
 *   ex: "karate:blanche", "karate:jaune_orange", "karate:vert_bleu"
 * - Couleurs: pastels non-fluo, prévues pour fonds M3.
 * - Barrettes (v1 Karaté): autorisées jusqu'à "karate:vert_bleu" inclus.
 *   Règle: la couleur des barrettes = couleur du grade supérieur (ex: autour de la blanche -> jaune).
 * - Liserés: seulement sur la blanche (max 2). On encode "blanche_2liseres" comme grade distinct.
 * - Dan (extension) :
 *   - 1er→5e Dan : noire avec inscription japonaise dorée (insigne utilisé sur badge liste & BeltPicker)
 *   - 6e Dan : bi-couleur rouge/blanc (sans inscription)
 *   - 7e Dan : rouge (sans inscription)
 */
object BeltCatalog {

    // ---- Domaine commun ----

    object Arts {
        const val KARATE = "karate"
        // const val BJJ = "bjj"   // réservé v2
        // const val JUDO = "judo" // réservé v2
    }

    @Immutable
    data class BeltSpec(
        val code: String,        // ex: "karate:jaune_orange"
        val art: String,         // ex: "karate"
        val label: String,       // ex: "Jaune-Orange" ou "Noire — 1er Dan"
        val primary: Color,      // couleur dominante (ou couleur 1 si bi-couleur)
        val secondary: Color?,   // couleur "grade supérieur" / couleur 2 si bi-couleur
        val order: Int,          // ordre absolu de progression
        // --- Métadonnées optionnelles pour affichages texte (badge/picker) ---
        val insigniaJp: String? = null, // ex: "初段", "二段", ... (1–5 Dan)
        val insigniaColor: Color? = null
    )

    /** Palette douce (non fluo). */
    private val WHITE = Color(0xFFF7F7F7)
    private val YELLOW = Color(0xFFD2B21B)
    private val ORANGE = Color(0xFFF58214)
    private val GREEN = Color(0xFF23A24C)
    private val BLUE = Color(0xFF0A6AD0)
    private val BROWN = Color(0xFF70462B)
    private val BLACK = Color(0xFF222222)
    private val PURPLE = Color(0xFF4E3988) // utile si on étend (BJJ)
    private val GRAY_TAPE = Color(0xFFEEEEEE) // rubans / barrettes
    private val RED = Color(0xFFC62828)      // rouge doux, non fluo
    private val GOLD = Color(0xFFD4AF37)     // doré pour les inscriptions Dan

    // ---- KARATE ----
    object Karate {

        object Codes {
            const val BLANCHE            = "karate:blanche"
            const val BLANCHE_2LISERES   = "karate:blanche_2liseres"
            const val BLANCHE_JAUNE      = "karate:blanche_jaune"
            const val JAUNE              = "karate:jaune"
            const val JAUNE_ORANGE       = "karate:jaune_orange"
            const val ORANGE             = "karate:orange"
            const val ORANGE_VERT        = "karate:orange_vert"
            const val VERT               = "karate:vert"
            const val VERT_BLEU          = "karate:vert_bleu"
            const val BLEU               = "karate:bleu"
            const val BLEU_MARRON        = "karate:bleu_marron"
            const val PURPLE             = "karate:purple"
            const val MARRON             = "karate:marron"
            //const val NOIR               = "karate:noir"

            // --- Dan (extension) ---
            const val NOIR_1DAN          = "karate:noir_1dan"
            const val NOIR_2DAN          = "karate:noir_2dan"
            const val NOIR_3DAN          = "karate:noir_3dan"
            const val NOIR_4DAN          = "karate:noir_4dan"
            const val NOIR_5DAN          = "karate:noir_5dan"
            const val ROUGE_BLANC_6DAN   = "karate:rouge_blanc_6dan"
            const val ROUGE_7DAN         = "karate:rouge_7dan"
        }

        /**
         * Ordre dojo classique + paliers demi-couleurs + blanche 2 liserés + DAN.
         * secondary = "couleur supérieure" (sert aux barrettes) ou couleur 2 si bi-couleur.
         */
        val specs: List<BeltSpec> = listOf(
            BeltSpec(Codes.BLANCHE,          Arts.KARATE, "Blanche",               WHITE,  YELLOW, 0),
            BeltSpec(Codes.BLANCHE_2LISERES, Arts.KARATE, "Blanche (2 liserés)",   WHITE,  YELLOW, 1),
            BeltSpec(Codes.BLANCHE_JAUNE,    Arts.KARATE, "Blanche-Jaune",         WHITE,  YELLOW, 2),
            BeltSpec(Codes.JAUNE,            Arts.KARATE, "Jaune",                 YELLOW, ORANGE, 3),
            BeltSpec(Codes.JAUNE_ORANGE,     Arts.KARATE, "Jaune-Orange",          YELLOW, ORANGE, 4),
            BeltSpec(Codes.ORANGE,           Arts.KARATE, "Orange",                ORANGE, GREEN,  5),
            BeltSpec(Codes.ORANGE_VERT,      Arts.KARATE, "Orange-Vert",           ORANGE, GREEN,  6),
            BeltSpec(Codes.VERT,             Arts.KARATE, "Verte",                 GREEN,  BLUE,   7),
            BeltSpec(Codes.VERT_BLEU,        Arts.KARATE, "Vert-Bleue",            GREEN,  BLUE,   8),
            BeltSpec(Codes.BLEU,             Arts.KARATE, "Bleue",                 BLUE,   BROWN,  9),
            BeltSpec(Codes.BLEU_MARRON,      Arts.KARATE, "Bleue-Marron",          BLUE,   BROWN,  10),
            BeltSpec(Codes.PURPLE,           Arts.KARATE, "Violet",                PURPLE, PURPLE, 11),
            BeltSpec(Codes.MARRON,           Arts.KARATE, "Marron",                BROWN,  BLACK,  12),
            //BeltSpec(Codes.NOIR,             Arts.KARATE, "Noire",                 BLACK,  null,   13),

            // --- DAN : 1–5 = noire + inscription japonaise dorée ---
            BeltSpec(Codes.NOIR_1DAN,        Arts.KARATE, "Noire — 1er Dan",       BLACK,  null,   14,
                insigniaJp = "初段", insigniaColor = GOLD),
            BeltSpec(Codes.NOIR_2DAN,        Arts.KARATE, "Noire — 2e Dan",        BLACK,  null,   15,
                insigniaJp = "二段", insigniaColor = GOLD),
            BeltSpec(Codes.NOIR_3DAN,        Arts.KARATE, "Noire — 3e Dan",        BLACK,  null,   16,
                insigniaJp = "三段", insigniaColor = GOLD),
            BeltSpec(Codes.NOIR_4DAN,        Arts.KARATE, "Noire — 4e Dan",        BLACK,  null,   17,
                insigniaJp = "四段", insigniaColor = GOLD),
            BeltSpec(Codes.NOIR_5DAN,        Arts.KARATE, "Noire — 5e Dan",        BLACK,  null,   18,
                insigniaJp = "五段", insigniaColor = GOLD),

            // --- 6e Dan : bi-couleur rouge/blanc, sans inscription ---
            BeltSpec(Codes.ROUGE_BLANC_6DAN, Arts.KARATE, "Rouge/Blanc — 6e Dan",  RED,    WHITE,  19),

            // --- 7e Dan : rouge plein, sans inscription ---
            BeltSpec(Codes.ROUGE_7DAN,       Arts.KARATE, "Rouge — 7e Dan",        RED,    null,   20),
        )

        /** Accès direct par code. */
        val byCode: Map<String, BeltSpec> = specs.associateBy { it.code }

        /** Barrettes autorisées jusqu'à VERT_BLEU inclus. */
        private val maxStripeOrderInclusive: Int =
            byCode[Codes.VERT_BLEU]?.order ?: 8

        fun specOf(code: String?): BeltSpec? = code?.let(byCode::get)

        fun allowsStripes(code: String?): Boolean {
            val o = specOf(code)?.order ?: return false
            return o <= maxStripeOrderInclusive
        }

        /**
         * Couleur des barrettes = "secondary" du grade courant si dispo (recommandé),
         * sinon on tente le grade suivant (secondary > primary), sinon on retombe sur primary.
         * => Donne bien "jaune" pour les paliers autour de la blanche.
         */
        fun stripeColor(code: String?): Color {
            val spec = specOf(code) ?: return BLUE
            spec.secondary?.let { return it }
            val next = specs.firstOrNull { it.order == spec.order + 1 }
            return next?.secondary ?: next?.primary ?: spec.primary
        }

        fun isBiColor(code: String?): Boolean = when (code) {
            Codes.BLANCHE_JAUNE,
            Codes.JAUNE_ORANGE,
            Codes.ORANGE_VERT,
            Codes.VERT_BLEU,
            Codes.BLEU_MARRON,
            Codes.ROUGE_BLANC_6DAN -> true
            else -> false
        }

        /** Prochain code dans la progression, ou null si top. */
        fun nextCode(code: String?): String? {
            val spec = specOf(code) ?: return null
            return specs.firstOrNull { it.order == spec.order + 1 }?.code
        }

        /** Clamp de 0..3 si les barrettes sont autorisées, sinon 0. */
        fun clampStripeCount(code: String?, requested: Int): Int =
            if (allowsStripes(code)) requested.coerceIn(0, 3) else 0

        fun clampStripeCount(code: String?, requested: Int?): Int =
            clampStripeCount(code, requested ?: 0)

        /** Nombre de liserés (horizontaux) codés dans le grade. */
        fun liseresCount(code: String?): Int =
            when (code) {
                Codes.BLANCHE_2LISERES -> 2
                else -> 0
            }

        /** Insigne japonais (1–5 Dan) à dessiner sur badge/picker, sinon null. */
        fun insigniaJp(code: String?): String? = specOf(code)?.insigniaJp

        /** Couleur conseillée pour l’insigne (doré), sinon null. */
        fun insigniaColor(code: String?): Color? = specOf(code)?.insigniaColor
    }

    // ---- Helpers transverses ----

    /** Libellé TalkBack : "Blanche (2 liserés), 2 barrettes" ou "Noire — 3e Dan". */
    fun a11yLabel(code: String?, stripeCount: Int): String {
        val spec = when {
            code?.startsWith("${Arts.KARATE}:") == true -> Karate.specOf(code)
            else -> null
        }
        val base = spec?.label ?: "Aucune ceinture"
        val clamped = when {
            code?.startsWith("${Arts.KARATE}:") == true ->
                Karate.clampStripeCount(code, stripeCount)
            else -> 0
        }
        return if (clamped > 0) {
            "$base, $clamped barrette${if (clamped > 1) "s" else ""}"
        } else base
    }
}
