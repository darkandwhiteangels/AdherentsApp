package com.antechrist.adherentsapp.core.utils

import java.time.LocalDate
import java.time.Month

/**
 * Saison sportive: du 1er SEPTEMBRE (année N) au 30 JUIN (année N+1).
 * Clé de saison: "YYYY-YYYY" (ex: "2025-2026").
 */
object SeasonUtils {

    /** Retourne la clé de saison pour la date donnée (ex: "2025-2026"). */
    fun seasonKeyFor(date: LocalDate): String {
        val year = date.year
        return if (date.month.value >= Month.SEPTEMBER.value) {
            // Septembre..Décembre => saison year-(year+1)
            "${year}-${year + 1}"
        } else {
            // Janvier..Juin => saison (year-1)-year
            "${year - 1}-$year"
        }
    }

    /** Retourne la clé de la saison "courante" (basée sur aujourd'hui). */
    fun currentSeasonKey(today: LocalDate = LocalDate.now()): String = seasonKeyFor(today)

    /** Bornes inclusives [start, end] (ex: 2025-09-01 .. 2026-06-30) pour la date fournie. */
    fun seasonBoundsFor(date: LocalDate): Pair<LocalDate, LocalDate> {
        val year = date.year
        return if (date.month.value >= Month.SEPTEMBER.value) {
            // Septembre..Décembre -> saison N-(N+1)
            LocalDate.of(year, Month.SEPTEMBER, 1) to LocalDate.of(year + 1, Month.JUNE, 30)
        } else {
            // Janvier..Juin -> saison (N-1)-N
            LocalDate.of(year - 1, Month.SEPTEMBER, 1) to LocalDate.of(year, Month.JUNE, 30)
        }
    }

    /** Bornes inclusives [start, end] (ex: 2025-09-01 .. 2026-06-30) pour une clé "YYYY-YYYY". */
    fun seasonBoundsForKey(seasonKey: String): Pair<LocalDate, LocalDate> {
        // format attendu "2025-2026"
        val parts = seasonKey.split("-")
        require(parts.size == 2) { "seasonKey invalide: $seasonKey" }
        val startYear = parts[0].toInt()
        val endYear = parts[1].toInt()
        require(endYear == startYear + 1) { "seasonKey non cohérente: $seasonKey" }

        val start = LocalDate.of(startYear, Month.SEPTEMBER, 1)
        val end = LocalDate.of(endYear, Month.JUNE, 30)
        return start to end
    }

    /** true si la date appartient à la saison correspondant à seasonKey. */
    fun isInSeason(date: LocalDate, seasonKey: String): Boolean {
        val (start, end) = seasonBoundsForKey(seasonKey)
        return !(date.isBefore(start) || date.isAfter(end))
    }
}
