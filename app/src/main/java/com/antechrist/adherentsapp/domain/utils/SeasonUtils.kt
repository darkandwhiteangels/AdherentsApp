package com.antechrist.adherentsapp.domain.utils

import java.time.LocalDate

/**
 * Exemple simple: saison = "2025-2026" si on est après août 2025.
 * Adapte si tu as déjà une logique de saison ailleurs.
 */
object SeasonUtils {
    fun currentSeasonKey(today: LocalDate = LocalDate.now()): String {
        val startYear = if (today.monthValue >= 8) today.year else today.year - 1
        return "$startYear-${startYear + 1}"
    }
}
