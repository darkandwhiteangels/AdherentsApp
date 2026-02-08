package com.antechrist.adherentsapp.domain.model

import com.antechrist.adherentsapp.domain.model.kihon.KihonStep

/**
 * Modèle temporaire représentant une séquence Kihon en cours de création ou d’édition.
 *
 * Contient les métadonnées de la séquence (titre, description, tags, etc.)
 * ainsi que la liste des steps (KihonStep).
 *
 * Le grade n'est PAS stocké ici car il est déjà connu dans le contexte
 * du séquenceur (via KihonSequenceEditorViewModel / GetKihonSequencesByGrade).
 */
data class SequenceDraft(
    var title: String = "",
    var description: String? = null,
    var tags: List<String> = emptyList(),

    // Position initiale (par défaut : Hidari Kamae)
    var startPosition: TechniqueRef = TechniqueRef(
        id = "HIDARI_KAMAE",
        kind = TechniqueRef.Kind.POSITION,
        nameJa = "Hidari Kamae",
        nameFr = "Garde gauche"
    ),

    // Liste mutable des étapes de la séquence
    val steps: MutableList<KihonStep> = mutableListOf(),
) {

    /** Ajoute un step à la séquence. */
    fun addStep(step: KihonStep) {
        val computedStep = if (steps.isEmpty()) {
            step.copy(startPosition = startPosition)
        } else {
            val prevEnd = steps.last().endPosition
            step.copy(startPosition = prevEnd)
        }
        steps.add(computedStep)
    }

    /** Supprime un step à un index donné. */
    fun removeStep(index: Int) {
        if (index in steps.indices) steps.removeAt(index)
    }

    /** Met à jour un step existant. */
    fun updateStep(index: Int, newStep: KihonStep) {
        if (index in steps.indices) {
            steps[index] = newStep.copy(
                startPosition = if (index == 0) startPosition else steps[index - 1].endPosition
            )
        }
    }

    /** Réordonne la liste des steps (drag & drop). */
    fun moveStep(fromIndex: Int, toIndex: Int) {
        if (fromIndex in steps.indices && toIndex in steps.indices) {
            val step = steps.removeAt(fromIndex)
            steps.add(toIndex, step)
        }
    }

    /** Vérifie si la séquence est complète. */
    val isComplete: Boolean
        get() = title.isNotBlank() && steps.isNotEmpty()

    /** Génère un résumé textuel rapide (utile pour debug ou aperçu). */
    fun toReadableSummary(): String {
        if (steps.isEmpty()) return ""
        val parts = steps.mapIndexed { i, step ->
            "${i + 1}. ${step.technique.nameFr} (${step.movement.name.lowercase()})"
        }
        return parts.joinToString(separator = "\n")
    }

    /** Réinitialise complètement le draft. */
    fun reset() {
        title = ""
        description = null
        tags = emptyList()
        steps.clear()
        startPosition = TechniqueRef(
            id = "HIDARI_KAMAE",
            kind = TechniqueRef.Kind.POSITION,
            nameJa = "Hidari Kamae",
            nameFr = "Garde gauche"
        )
    }
}
