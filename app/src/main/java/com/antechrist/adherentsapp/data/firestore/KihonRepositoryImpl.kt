// com/antechrist/adherentsapp/data/firestore/KihonRepositoryImpl.kt
package com.antechrist.adherentsapp.data.firestore

import android.util.Log
import com.antechrist.adherentsapp.data.model.SequenceStatus
import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.model.LevelRef
import com.antechrist.adherentsapp.domain.model.MovementRef
import com.antechrist.adherentsapp.domain.model.OptionRef
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implémentation Repository pour le module Kihon, adossée à Firestore.
 */
class KihonRepositoryImpl @Inject constructor(
    private val ds: KihonDataSource
) : KihonRepository {

    private companion object {
        private const val TAG = "KihonRepo"
    }

    /** Petit résumé lisible pour Logcat. */
    private fun seqSummary(seq: KihonSequence): String =
        "id=${seq.id}, grade=${seq.gradeKey}, name='${seq.name}', steps=${seq.steps.size}, exam=${seq.isExamRequired}"

    /** Flux du catalogue (DTO -> Domain). */
    override fun streamCatalog(): Flow<Map<String, TechniqueRef>> =
        ds.streamCatalog().map { dtoMap ->
            val out = LinkedHashMap<String, TechniqueRef>(dtoMap.size)
            dtoMap.forEach { (id, dto) ->
                // Mapping strict: si une entrée est invalide, l'erreur remontera dans la couche supérieure
                out[id] = dto.toDomainStrict()
            }
            out
        }

    /** Flux des séquences par grade, résolues avec le catalogue courant. */
    override fun streamSequencesByGrade(gradeKey: String): Flow<List<KihonSequence>> =
        combine(
            streamCatalog(),                             // Map<String, TechniqueRef>
            ds.streamSequencesByGrade(gradeKey)          // List<Pair<docId, KihonSequenceDto>>
        ) { catalogDomain, listDto ->
            listDto.map { (docId, dto) ->
                dto.toDomainStrict(catalog = catalogDomain, docId = docId)
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }

    override suspend fun saveSequence(seq: KihonSequence) {
        Log.d(TAG, "saveSequence(domain): ${seqSummary(seq)}")
        val dto = seq.toDto()
        Log.d(TAG, "saveSequence(dto): id=${dto.id}, steps=${dto.steps.size}, grade=${dto.gradeKey}")
        ds.upsertSequence(dto)
        Log.d(TAG, "saveSequence(): OK upsert path=kihon_sequences/${dto.id}")
    }

    /** Publication d'une séquence (statut PUBLISHED + horodatage + actor). */
    override suspend fun publishSequence(id: String, actorUid: String) {
        Log.d(TAG, "publishSequence(id=$id, actor=$actorUid)")
        ds.publishSequence(id, actorUid)
        Log.d(TAG, "publishSequence(): OK id=$id")
    }

    /** Compteurs par statut pour le Board. */
    override suspend fun getBoardCountsByStatus(): Map<KihonSequence.Status, Long> {
        val raw = ds.countSequencesByStatus()
        return buildMap {
            raw.forEach { (k, v) -> put(k.toDomain(), v) }
        }
    }

    /** Compteurs par grade (ou global si null). */
    override suspend fun countByGrade(gradeKeys: List<String>?): Map<String, Long> =
        ds.countSequencesByGrade(gradeKeys)

    /** Upsert d'une technique dans le catalogue. */
    override suspend fun upsertTechnique(ref: TechniqueRef): String {
        Log.d(TAG, "upsertTechnique(id=${ref.id}, kind=${ref.kind}, nameJa='${ref.nameJa}', nameFr='${ref.nameFr}')")
        val id = ds.upsertTechnique(ref.toDto())
        Log.d(TAG, "upsertTechnique(): OK id=$id")
        return id
    }

    override suspend fun deleteTechnique(id: String) {
        Log.d(TAG, "deleteTechnique(id=$id)")
        ds.deleteTechnique(id)
        Log.d(TAG, "deleteTechnique(): OK id=$id")
    }

    override suspend fun upsertTechniques(refs: List<TechniqueRef>): Int {
        Log.d(TAG, "upsertTechniques(count=${refs.size})")
        val n = ds.upsertTechniques(refs.map { it.toDto() })
        Log.d(TAG, "upsertTechniques(): OK inserted=$n")
        return n
    }

    override suspend fun isCatalogEmpty(): Boolean = ds.isCatalogEmpty()

    override suspend fun deleteSequence(id: String): Result<Unit> {
        Log.d(TAG, "deleteSequence(id=$id)")
        val r = ds.deleteSequence(id)
        if (r.isSuccess) Log.d(TAG, "deleteSequence(): OK id=$id")
        else Log.e(TAG, "deleteSequence(): FAIL id=$id ${r.exceptionOrNull()?.message}", r.exceptionOrNull())
        return r
    }

    // Movements
    override fun streamMovements() = ds.streamMovements().map { map ->
        LinkedHashMap<String, MovementRef>(map.size).also { out ->
            map.forEach { (id,dto) -> out[id] = dto.toDomainStrict() }
        }
    }
    override suspend fun upsertMovement(ref: MovementRef): String = ds.upsertMovement(ref.toDto())
    override suspend fun upsertMovements(refs: List<MovementRef>): Int = ds.upsertMovements(refs.map { it.toDto() })
    override suspend fun deleteMovement(id: String) = ds.deleteMovement(id)
    override suspend fun isMovementCatalogEmpty(): Boolean = ds.isMovementCatalogEmpty()

    // Options
    override fun streamOptions() = ds.streamOptions().map { map ->
        LinkedHashMap<String, OptionRef>(map.size).also { out ->
            map.forEach { (id,dto) -> out[id] = dto.toDomainStrict() }
        }
    }
    override suspend fun upsertOption(ref: OptionRef): String = ds.upsertOption(ref.toDto())
    override suspend fun upsertOptions(refs: List<OptionRef>): Int = ds.upsertOptions(refs.map { it.toDto() })
    override suspend fun deleteOption(id: String) = ds.deleteOption(id)
    override suspend fun isOptionCatalogEmpty(): Boolean = ds.isOptionCatalogEmpty()

    // Levels
    override fun streamLevels() = ds.streamLevels().map { map ->
        LinkedHashMap<String, LevelRef>(map.size).also { out ->
            map.forEach { (id,dto) -> out[id] = dto.toDomainStrict() }
        }
    }
    override suspend fun upsertLevel(ref: LevelRef): String = ds.upsertLevel(ref.toDto())
    override suspend fun upsertLevels(refs: List<LevelRef>): Int = ds.upsertLevels(refs.map { it.toDto() })
    override suspend fun deleteLevel(id: String) = ds.deleteLevel(id)
    override suspend fun isLevelCatalogEmpty(): Boolean = ds.isLevelCatalogEmpty()

}

/* ──────────────────────────────────────────────
 * Mappers statut DTO -> Domain
 * (les autres mappers sont dans FirestoreMappersKihon.kt)
 * ────────────────────────────────────────────── */

private fun SequenceStatus.toDomain(): KihonSequence.Status = when (this) {
    SequenceStatus.DRAFT     -> KihonSequence.Status.DRAFT
    SequenceStatus.READY     -> KihonSequence.Status.READY
    SequenceStatus.PUBLISHED -> KihonSequence.Status.PUBLISHED
}
