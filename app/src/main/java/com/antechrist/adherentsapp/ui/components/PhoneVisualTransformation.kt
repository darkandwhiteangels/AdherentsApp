package com.antechrist.adherentsapp.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.math.min

/**
 * Affiche "0612345678" comme "06.12.34.56.78".
 * IMPORTANT : la valeur du champ doit être les **digits uniquement** (0-9, max 10).
 */
class FrenchPhoneVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        // valeur originale attendue = chiffres uniquement (on coupe à 10 au cas où)
        val digits = text.text.filter { it.isDigit() }.take(10)

        // chaîne transformée avec points toutes les 2
        val transformed = if (digits.isEmpty()) "" else digits.chunked(2).joinToString(".")

        val n = digits.length
        val tLen = transformed.length
        val totalDots = n / 2 // nb de points au max dans la version transformée

        val mapping = object : OffsetMapping {
            // original [0..n] -> transformed [0..tLen]
            override fun originalToTransformed(offset: Int): Int {
                if (n == 0) return 0
                val o = offset.coerceIn(0, n)
                // nb de points avant la position 'o'
                val dotsBefore = o / 2
                val mapped = o + dotsBefore
                return mapped.coerceIn(0, tLen)
            }

            // transformed [0..tLen] -> original [0..n]
            override fun transformedToOriginal(offset: Int): Int {
                if (n == 0) return 0
                val t = offset.coerceIn(0, tLen)
                // chaque groupe "XX." fait 3 caractères ; nb de points avant 't' ~ t/3
                val dotsBeforeHere = min(t / 3, totalDots)
                val mapped = t - dotsBeforeHere
                return mapped.coerceIn(0, n)
            }
        }

        return TransformedText(AnnotatedString(transformed), mapping)
    }
}

/** Nettoie: conserve uniquement 10 chiffres max */
fun sanitizePhoneDigits(input: String): String =
    input.filter { it.isDigit() }.take(10)

/** Formatte "0612345678" -> "06.12.34.56.78" (utile côté affichage détail si besoin) */
fun formatPhoneDigits(digits: String): String =
    digits.filter { it.isDigit() }.take(10).chunked(2).joinToString(".")
