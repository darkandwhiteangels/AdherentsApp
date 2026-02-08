package com.antechrist.adherentsapp.ui.utils

import java.text.Normalizer
import java.util.Locale

/** Normalise (minuscules + accents supprimés) */
fun normalize(text: String): String {
    val n = Normalizer.normalize(text.lowercase(Locale.FRENCH), Normalizer.Form.NFD)
    return n.replace(Regex("\\p{Mn}+"), "")
}

/** Première lettre normalisée ou '#' si rien */
fun firstLetterOrHash(text: String): Char {
    val n = normalize(text).trim()
    if (n.isEmpty()) return '#'
    val c = n.first()
    return if (c in 'a'..'z') c.uppercaseChar() else '#'
}

/** Nom en majuscules (FR) → "pereira" => "PEREIRA" */
fun toUpperNameFR(input: String?): String? =
    input?.trim()?.takeIf { it.isNotEmpty() }?.uppercase(Locale.FRANCE)

/** Prénom UcFirst → "jean-luc d'artagnan" => "Jean-Luc d'Artagnan" */
fun toUcFirstNameFR(input: String?): String? {
    val src = input?.trim()?.lowercase(Locale.FRANCE) ?: return null
    if (src.isEmpty()) return null
    val separators = charArrayOf(' ', '-', '’', '\'')
    val out = StringBuilder(src.length)
    var capNext = true
    for (ch in src) {
        if (separators.contains(ch)) {
            out.append(ch)
            // Après séparateur (espace, tiret, apostrophe), on capitalise la prochaine lettre
            capNext = true
        } else {
            out.append(if (capNext) ch.titlecase(Locale.FRANCE) else ch)
            capNext = false
        }
    }
    return out.toString()
}

/** Ville UcFirst simple (garde la casse après le 1er char) */
fun toUcFirstCityFR(input: String?): String? {
    val t = input?.trim() ?: return null
    if (t.isEmpty()) return null
    return t.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.FRANCE) else ch.toString()
    }
}

/** Relation (mère/père/tuteur…) → UcFirst avec mêmes règles que prénom */
fun toUcFirstRelationFR(input: String?): String? = toUcFirstNameFR(input)

/** Email → minuscule + trim */
fun normalizeEmailLower(input: String?): String? =
    input?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }

/** Téléphone: garder uniquement les chiffres (stockage) */
fun phoneDigitsOnly(input: String?): String {
    if (input.isNullOrBlank()) return ""
    return buildString(input.length) {
        for (c in input) if (c.isDigit()) append(c)
    }
}

/**
 * Téléphone FR pour affichage: "0600000000" -> "06.00.00.00.00"
 * - Accepte longueurs < 10 (n’affiche que ce qui existe)
 * - N’explose pas si vide
 */
fun phonePrettyFR(digitsOnly: String?): String {
    val d = phoneDigitsOnly(digitsOnly).take(10)
    if (d.isEmpty()) return ""
    val parts = mutableListOf<String>()
    var i = 0
    while (i < d.length) {
        val end = (i + 2).coerceAtMost(d.length)
        parts += d.substring(i, end)
        i = end
    }
    return parts.joinToString(".")
}
