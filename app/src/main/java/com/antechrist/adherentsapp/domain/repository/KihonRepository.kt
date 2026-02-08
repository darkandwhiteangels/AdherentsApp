// com/antechrist/adherentsapp/domain/repository/KihonRepository.kt
package com.antechrist.adherentsapp.domain.repository

import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.model.LevelRef
import com.antechrist.adherentsapp.domain.model.MovementRef
import com.antechrist.adherentsapp.domain.model.OptionRef
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction du module Kihon.
 *
 * NB: On expose des Flows pour le catalogue et les séquences par grade,
 * afin d'avoir le temps réel Firestore dans l'UI.
 */
interface KihonRepository {

    /** Flux du catalogue Kihon (clé = techniqueId). */
    fun streamCatalog(): Flow<Map<String, TechniqueRef>>

    /** Flux des séquences pour un grade donné (résolues en Domain via le catalogue courant). */
    fun streamSequencesByGrade(gradeKey: String): Flow<List<KihonSequence>>

    /** Sauvegarde (create/update) d'une séquence. Retourne son id. */
    //suspend fun saveSequence(sequence: KihonSequence): String
    suspend fun saveSequence(seq: KihonSequence): Unit


    /** Publication d'une séquence (statut PUBLISHED + horodatage + acteur). */
    suspend fun publishSequence(id: String, actorUid: String)

    /** Compteurs par statut (DRAFT/READY/PUBLISHED). */
    suspend fun getBoardCountsByStatus(): Map<KihonSequence.Status, Long>

    /** Compteurs par grade (si gradeKeys = null => total global sous clé "__ALL__"). */
    suspend fun countByGrade(gradeKeys: List<String>? = null): Map<String, Long>

    /* (Optionnel) Upsert d'une technique du catalogue. */
    suspend fun upsertTechnique(ref: TechniqueRef): String

    /* Supprimer une technique (si pas utilisée). */
    suspend fun deleteTechnique(id: String)

    /* Upsert en lot (seed). Retourne nb insérés/à jour. */
    suspend fun upsertTechniques(refs: List<TechniqueRef>): Int

    /* Le catalogue est-il vide ? */
    suspend fun isCatalogEmpty(): Boolean

    suspend fun deleteSequence(id: String): Result<Unit>

    // — NOUVEAU : MOUVEMENTS —
    fun streamMovements(): Flow<Map<String, MovementRef>>
    suspend fun upsertMovement(ref: MovementRef): String
    suspend fun upsertMovements(refs: List<MovementRef>): Int
    suspend fun deleteMovement(id: String)
    suspend fun isMovementCatalogEmpty(): Boolean

    // — NOUVEAU : OPTIONS —
    fun streamOptions(): Flow<Map<String, OptionRef>>
    suspend fun upsertOption(ref: OptionRef): String
    suspend fun upsertOptions(refs: List<OptionRef>): Int
    suspend fun deleteOption(id: String)
    suspend fun isOptionCatalogEmpty(): Boolean

    // — NOUVEAU : NIVEAUX —
    fun streamLevels(): Flow<Map<String, LevelRef>>
    suspend fun upsertLevel(ref: LevelRef): String
    suspend fun upsertLevels(refs: List<LevelRef>): Int
    suspend fun deleteLevel(id: String)
    suspend fun isLevelCatalogEmpty(): Boolean
}
