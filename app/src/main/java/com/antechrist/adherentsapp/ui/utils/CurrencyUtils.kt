package com.antechrist.adherentsapp.ui.utils

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

fun parseEuroToCents(input: String): Long {
    val cleaned = input.replace(",", ".").replace(Regex("[^0-9\\.]"), "")
    if (cleaned.isBlank()) return 0L
    val parts = cleaned.split(".")
    return when (parts.size) {
        1 -> (parts[0].toLongOrNull() ?: 0L) * 100L
        else -> {
            val euros = parts[0].toLongOrNull() ?: 0L
            val centsStr = (parts.getOrNull(1) ?: "")
                .filter { it.isDigit() }
                .padEnd(2, '0')
                .take(2)
            val cents = centsStr.toLongOrNull() ?: 0L
            euros * 100L + cents
        }
    }
}

fun formatCentsToEuro(cents: Long): String {
    val nf = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    return nf.format(cents / 100.0)
}

fun splitCentsInParts(total: Long, parts: Int): List<Long> {
    val p = max(1, parts)
    val base = total / p                  // Long
    val remainder = total % p             // Long (pas besoin de .toInt())
    return (0 until p).map { idx ->
        base + if (idx == p - 1) remainder else 0L  // tout en Long
    }
}
