// com/antechrist/adherentsapp/ui/navigation/KihonDestinations.kt
package com.antechrist.adherentsapp.ui.navigation

/**
 * Routes de navigation pour le module Kihon.
 *
 * Remarque:
 * - Pour les écrans Séquence (detail/editor), on transporte aussi gradeKey
 *   afin de pouvoir résoudre la séquence via GetKihonSequencesByGrade(gradeKey).
 */
object KihonDestinations {

    /* Board global (stats + raccourcis par grade) */
    const val BOARD = "kihon/board"

    const val CATALOG = "kihon/catalog"

    /* Liste des séquences pour un grade donné */
    const val GRADE_LIST = "kihon/grade/{gradeKey}"
    fun gradeList(gradeKey: String) = "kihon/grade/$gradeKey"

    /* Détail d’une séquence (lecture seule) */
    const val SEQUENCE_DETAIL = "kihon/grade/{gradeKey}/sequence/{sequenceId}"
    fun sequenceDetail(gradeKey: String, sequenceId: String) =
        "kihon/grade/$gradeKey/sequence/$sequenceId"

    /* Éditeur d’une séquence (création/édition) */
    const val SEQUENCE_EDITOR = "kihon/grade/{gradeKey}/sequence/{sequenceId}/edit"
    fun sequenceEditor(gradeKey: String, sequenceId: String) =
        "kihon/grade/$gradeKey/sequence/$sequenceId/edit"

    /* Arguments nav */
    const val ARG_GRADE_KEY = "gradeKey"
    const val ARG_SEQUENCE_ID = "sequenceId"
}
