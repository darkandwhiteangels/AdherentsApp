package com.antechrist.adherentsapp.domain.model

import android.util.Log
import com.antechrist.adherentsapp.domain.model.kihon.KihonStep

private const val TAG_KIHON_SEQUENCE = "KihonSequence"

/**
 * Enchaînement Kihon (manuel, par grade).
 *
 * - Pas d’auto-builder : l’ordre et le contenu des Steps sont décidés par l’enseignant.
 * - Versionnement : version référence le référentiel Kihon (ex: "KIHON_V1").
 * - Statuts :
 *     DRAFT      -> en cours de préparation/édition
 *     READY      -> validé par l'équipe, prêt à publier
 *     PUBLISHED  -> publié, immuable (sauf méta non-critique)
 *
 * - gradeKey : clé de grade provenant du module ceinture (ordre déjà existant).
 *              Choisir la même convention que votre Belt/BeltCatalog (ex: "BLANCHE", "JAUNE", ...).
 */
data class KihonSequence(
    val id: String,                    // UUID/ULID, généré côté repo
    val name: String,                  // Nom court (ex: "Défenses chudan — base")
    val gradeKey: String,              // Référence au grade (clé ceinture)
    val version: String,               // Ex: "KIHON_V1"
    val status: Status = Status.DRAFT,
    val objective: String? = null,     // Objectif pédagogique (court)
    val tags: List<String> = emptyList(), // Ex: ["precision","hanche","timing"]
    val steps: List<KihonStep> = emptyList(),
    val isExamRequired: Boolean = false,  // Marqué "exigible passage de grade"

    // Traçabilité
    val authorId: String,              // UID auteur (enseignant/admin)
    val createdAt: Long,               // epochMillis
    val updatedAt: Long? = null,       // epochMillis
    val publishedAt: Long? = null,     // epochMillis
    val publishedBy: String? = null    // UID publieur
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(gradeKey.isNotBlank()) { "gradeKey must not be blank" }
        require(version.isNotBlank()) { "version must not be blank" }
        if (status == Status.PUBLISHED) {
            require(steps.isNotEmpty()) { "A published sequence must contain at least 1 step" }
        }

        // 🔎 Trace de création de l'instance (utile pour suivre les mutations immuables successives)
        try {
            Log.d(
                TAG_KIHON_SEQUENCE,
                buildString {
                    append("init: id=").append(id)
                    append(" name=").append(name)
                    append(" gradeKey=").append(gradeKey)
                    append(" version=").append(version)
                    append(" status=").append(status)
                    append(" steps=").append(steps.size)
                    append(" isExamRequired=").append(isExamRequired)
                    append(" authorId=").append(authorId)
                    append(" createdAt=").append(createdAt)
                    append(" updatedAt=").append(updatedAt)
                    append(" publishedAt=").append(publishedAt)
                    append(" publishedBy=").append(publishedBy)
                }
            )
        } catch (_: Throwable) { /* no-op */ }
    }

    enum class Status { DRAFT, READY, PUBLISHED }

    /** Nombre total de steps. */
    val stepCount: Int get() = steps.size

    /** Ensemble des IDs de techniques utilisées (positions exclues) pour analytics/couverture. */
    val usedTechniqueIds: Set<String> get() =
        steps.map { it.technique.id }.toSet()

    /** True si modifiable. */
    val isEditable: Boolean get() = status != Status.PUBLISHED

    /** Publishable si non publié et contient au moins 1 step. */
    val canPublish: Boolean get() = status != Status.PUBLISHED && steps.isNotEmpty()

    /**
     * Normalise la liste des steps après insert/suppressions/déplacements.
     * Dans cette version sans champ `index` dans KihonStep,
     * ça revient simplement à renvoyer la séquence avec la liste actuelle.
     */
    fun withNormalizedIndices(): KihonSequence {
        try {
            Log.d(TAG_KIHON_SEQUENCE, "withNormalizedIndices: steps=${steps.size}")
        } catch (_: Throwable) { /* no-op */ }
        return copy(steps = steps.toList())
    }

    /**
     * Ajoute ou remplace un step à un index donné, puis renvoie une nouvelle séquence.
     * - si atIndex < size : remplace
     * - si atIndex == size : append
     * - sinon -> erreur
     */
    fun upsertStep(atIndex: Int, step: KihonStep): KihonSequence {
        require(atIndex >= 0) { "atIndex must be >= 0" }

        try {
            Log.d(
                TAG_KIHON_SEQUENCE,
                "upsertStep: atIndex=$atIndex sizeBefore=${steps.size} step.technique=${step.technique.id}"
            )
        } catch (_: Throwable) { /* no-op */ }

        val mutable = steps.toMutableList()

        if (atIndex < mutable.size) {
            mutable[atIndex] = step
        } else if (atIndex == mutable.size) {
            mutable.add(step)
        } else {
            Log.e(TAG_KIHON_SEQUENCE, "upsertStep: out of bounds atIndex=$atIndex size=${mutable.size}")
            throw IndexOutOfBoundsException("Cannot insert at $atIndex (size=${mutable.size})")
        }

        val result = copy(steps = mutable).withNormalizedIndices()

        try {
            Log.d(TAG_KIHON_SEQUENCE, "upsertStep: sizeAfter=${result.steps.size}")
        } catch (_: Throwable) { /* no-op */ }

        return result
    }

    /**
     * Supprime un step à un index donné.
     */
    fun removeStep(atIndex: Int): KihonSequence {
        require(atIndex in steps.indices) { "Index $atIndex out of range" }

        try {
            Log.d(TAG_KIHON_SEQUENCE, "removeStep: atIndex=$atIndex sizeBefore=${steps.size}")
        } catch (_: Throwable) { /* no-op */ }

        val mutable = steps.toMutableList()
        mutable.removeAt(atIndex)
        val result = copy(steps = mutable).withNormalizedIndices()

        try {
            Log.d(TAG_KIHON_SEQUENCE, "removeStep: sizeAfter=${result.steps.size}")
        } catch (_: Throwable) { /* no-op */ }

        return result
    }

    /**
     * Déplace un step de fromIndex vers toIndex (reordering simple).
     */
    fun moveStep(fromIndex: Int, toIndex: Int): KihonSequence {
        require(fromIndex in steps.indices) { "fromIndex out of range" }
        require(toIndex in steps.indices) { "toIndex out of range" }
        if (fromIndex == toIndex) return this

        try {
            Log.d(TAG_KIHON_SEQUENCE, "moveStep: from=$fromIndex to=$toIndex size=${steps.size}")
        } catch (_: Throwable) { /* no-op */ }

        val mutable = steps.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)

        val result = copy(steps = mutable).withNormalizedIndices()

        try {
            Log.d(TAG_KIHON_SEQUENCE, "moveStep: done newOrderSize=${result.steps.size}")
        } catch (_: Throwable) { /* no-op */ }

        return result
    }

    /**
     * Met à jour le statut avec garde-fous simples.
     * - READY nécessite au moins 1 step.
     * - PUBLISHED nécessite au moins 1 step.
     */
    fun withStatus(newStatus: Status, nowMillis: Long, actorUid: String?): KihonSequence {
        if (newStatus != Status.DRAFT) {
            require(steps.isNotEmpty()) { "Sequence must have steps before moving to $newStatus" }
        }

        try {
            Log.d(
                TAG_KIHON_SEQUENCE,
                "withStatus: from=$status to=$newStatus steps=${steps.size} now=$nowMillis actor=$actorUid"
            )
        } catch (_: Throwable) { /* no-op */ }

        return when (newStatus) {
            Status.DRAFT -> copy(status = Status.DRAFT, updatedAt = nowMillis)
            Status.READY -> copy(status = Status.READY, updatedAt = nowMillis)
            Status.PUBLISHED -> copy(
                status = Status.PUBLISHED,
                updatedAt = nowMillis,
                publishedAt = nowMillis,
                publishedBy = actorUid ?: publishedBy
            )
        }
    }

    /**
     * 🔎 Utilitaire : à appeler juste avant la conversion en DTO / l'appel repo.save()
     * pour tracer l'état de la séquence envoyée à l’enregistrement.
     */
    fun logForSave(tag: String = TAG_KIHON_SEQUENCE) {
        try {
            val preview = steps.take(3).joinToString(prefix = "[", postfix = "]") { s ->
                "${s.technique.id}:${s.movement}/${s.height}/${s.direction}"
            }
            Log.d(
                tag,
                buildString {
                    append("logForSave: id=").append(id)
                    append(" name=").append(name)
                    append(" gradeKey=").append(gradeKey)
                    append(" version=").append(version)
                    append(" status=").append(status)
                    append(" steps=").append(steps.size)
                    append(" preview=").append(preview)
                    append(" isExamRequired=").append(isExamRequired)
                    append(" authorId=").append(authorId)
                    append(" createdAt=").append(createdAt)
                    append(" updatedAt=").append(updatedAt)
                    append(" publishedAt=").append(publishedAt)
                    append(" publishedBy=").append(publishedBy)
                }
            )
        } catch (_: Throwable) { /* no-op */ }
    }
}
