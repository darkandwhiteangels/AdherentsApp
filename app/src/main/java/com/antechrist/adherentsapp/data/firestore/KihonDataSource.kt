// com/antechrist/adherentsapp/data/firestore/KihonDataSource.kt
package com.antechrist.adherentsapp.data.firestore

import android.util.Log
import com.antechrist.adherentsapp.data.model.SequenceStatus
import com.antechrist.adherentsapp.data.model.dto.*
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * DataSource Firestore pour le module Kihon.
 *
 * Collections :
 * - "kihon_catalog"   : TechniqueRefDto (id = techniqueId)
 * - "kihon_sequences" : KihonSequenceDto (id = sequenceId)
 *
 * Les Steps référencent les techniques par leur id (pas de duplication).
 */
class KihonDataSource(
    private val db: FirebaseFirestore
) {

    private val colCatalog get() = db.collection("kihon_catalog")
    private val colSequences get() = db.collection("kihon_sequences")
    private val colMovements get() = db.collection("kihon_movements")
    private val colOptions   get() = db.collection("kihon_options")
    private val colLevels    get() = db.collection("kihon_levels")


    private val sequences = db.collection("kihon_sequences")
    private val TAG = "KihonDataSource"

    /* ───────────────────────── Helpers Log ───────────────────────── */

    private fun mapKeysString(map: Map<String, *>): String =
        map.keys.sorted().joinToString(prefix = "[", postfix = "]")

    private fun dtoSummary(dto: KihonSequenceDto): String =
        "id=${dto.id}, grade=${dto.gradeKey}, name='${dto.name}', steps=${dto.steps.size}, status=${dto.status}"

    /* ─────────── CATALOG ─────────── */

    /** Flux temps réel du catalogue complet (clé = id). */
    fun streamCatalog(): Flow<Map<String, TechniqueRefDto>> = callbackFlow {
        val reg = colCatalog.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "streamCatalog: snapshot error=${err.message}", err)
                close(err)
                return@addSnapshotListener
            }
            val map = LinkedHashMap<String, TechniqueRefDto>()

            snap?.documents?.forEach { doc ->
                val dto = doc.toObject(TechniqueRefDto::class.java) ?: return@forEach
                val consolidatedId = (dto.id ?: "").ifBlank { doc.id }
                map[consolidatedId] = dto.copy(id = consolidatedId)
            }

            Log.d(TAG, "streamCatalog: emit size=${map.size}")
            trySend(map).isSuccess
        }
        awaitClose { reg.remove() }
    }

    /** Upsert d'une entrée de catalogue (id obligatoire). */
    suspend fun upsertTechnique(dto: TechniqueRefDto): String {
        val id = requireNotNull(dto.id) { "TechniqueRefDto.id required for upsertTechnique" }
        val t0 = System.currentTimeMillis()
        val map = dto.copy(id = id).asFirestoreMap()   // ✅ pas de null, pas de clés vides
        Log.d(TAG, "upsertTechnique(SET) id=$id keys=${map.keys}")

        return suspendCancellableCoroutine { cont ->
            colCatalog.document(id)
                .set(map) // ✅ PAS de merge: on écrit un doc propre conforme aux règles
                .addOnSuccessListener {
                    val dt = System.currentTimeMillis() - t0
                    Log.d(TAG, "upsertTechnique: OK id=$id (${dt}ms)")
                    cont.resume(id)
                }
                .addOnFailureListener { e ->
                    val dt = System.currentTimeMillis() - t0
                    Log.e(TAG, "upsertTechnique: FAIL id=$id (${dt}ms) ${e.message}", e)
                    cont.resumeWithException(e)
                }
        }
    }

    /** Suppression d’une technique du catalogue. */
    suspend fun deleteTechnique(id: String) {
        val t0 = System.currentTimeMillis()
        Log.d(TAG, "deleteTechnique id=$id")
        return suspendCancellableCoroutine { cont ->
            colCatalog.document(id)
                .delete()
                .addOnSuccessListener {
                    val dt = System.currentTimeMillis() - t0
                    Log.d(TAG, "deleteTechnique: OK id=$id (${dt}ms)")
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    val dt = System.currentTimeMillis() - t0
                    Log.e(TAG, "deleteTechnique: FAIL id=$id (${dt}ms) ${e.message}", e)
                    cont.resumeWithException(e)
                }
        }
    }

    /** Vrai si le catalogue est vide (server aggregate). */
    suspend fun isCatalogEmpty(): Boolean {
        val count = colCatalog.count().get(AggregateSource.SERVER).awaitCount()
        Log.d(TAG, "isCatalogEmpty -> $count")
        return count == 0L
    }

    /** Upsert en lot (sans transaction ici; simple pour seed). Retourne le nb écrit. */
    suspend fun upsertTechniques(list: List<TechniqueRefDto>): Int {
        var ok = 0
        Log.d(TAG, "upsertTechniques: start count=${list.size}")
        for (dto in list) {
            runCatching { upsertTechnique(dto) }
                .onSuccess { ok++ }
                .onFailure { e -> Log.e(TAG, "upsertTechniques: item FAIL id=${dto.id} ${e.message}") }
        }
        Log.d(TAG, "upsertTechniques: done ok=$ok/${list.size}")
        return ok
    }

    /* ─────────── SEQUENCES ─────────── */

    /** Flux des séquences par gradeKey. */
    fun streamSequencesByGrade(gradeKey: String): Flow<List<Pair<String, KihonSequenceDto>>> = callbackFlow {
        Log.d(TAG, "streamSequencesByGrade grade=$gradeKey")
        val reg = colSequences
            .whereEqualTo("gradeKey", gradeKey)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "streamSequencesByGrade: snapshot error=${err.message}", err)
                    close(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    val dto = doc.toObject(KihonSequenceDto::class.java) ?: return@mapNotNull null
                    val id = dto.id.ifBlank { doc.id }
                    id to dto.copy(id = id)
                }?.sortedBy { it.second.name } ?: emptyList()
                Log.d(TAG, "streamSequencesByGrade emit size=${list.size}")
                trySend(list).isSuccess
            }
        awaitClose { reg.remove() }
    }

    /** Récupération one-shot d'une séquence par id (DTO + docId consolidé). */
    suspend fun getSequence(id: String): KihonSequenceDto? {
        Log.d(TAG, "getSequence(id=$id)")
        val doc = suspendCancellableCoroutine { cont ->
            colSequences.document(id).get()
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        val dto = doc.toObject(KihonSequenceDto::class.java) ?: run {
            Log.w(TAG, "getSequence(id=$id) -> null")
            return null
        }
        val out = dto.copy(id = dto.id.ifBlank { doc.id })
        Log.d(TAG, "getSequence OK -> ${dtoSummary(out)}")
        return out
    }

    /**
     * Enregistre (create/update) une séquence.
     * - Création : set(map) complet conforme aux règles (pas update()).
     * - MAJ : set(map) (ou merge) ; on force updatedAt.
     */
    suspend fun saveSequence(dto: KihonSequenceDto, explicitId: String? = null): String {
        val now = System.currentTimeMillis()
        val targetId = dto.id.ifBlank { explicitId ?: colSequences.document().id }
        val created = if (dto.createdAt == 0L) now else dto.createdAt
        val toSave = dto.copy(
            id = targetId,
            createdAt = created,
            updatedAt = now
        )
        val map = toSave.asFirestoreMap()

        val keys = mapKeysString(map)
        Log.d(TAG, "saveSequence(SET) path=kihon_sequences/$targetId keys=$keys createdAt=${toSave.createdAt} updatedAt=${toSave.updatedAt}")
        Log.d(TAG, "saveSequence dto=(${dtoSummary(toSave)})")

        val t0 = System.currentTimeMillis()
        return suspendCancellableCoroutine { cont ->
            colSequences.document(targetId)
                .set(map) // ✅ envoie uniquement les clés autorisées par les règles
                .addOnSuccessListener {
                    val dt = System.currentTimeMillis() - t0
                    Log.d(TAG, "saveSequence: OK id=$targetId (${dt}ms)")
                    cont.resume(targetId)
                }
                .addOnFailureListener { e ->
                    val dt = System.currentTimeMillis() - t0
                    Log.e(TAG, "saveSequence: FAIL id=$targetId (${dt}ms) ${e.message}", e)
                    cont.resumeWithException(e)
                }
        }
    }

    /**
     * Upsert direct (utilisé par certains flows) — identique à saveSequence mais sans id auto.
     */
    suspend fun upsertSequence(dto: KihonSequenceDto) {
        val id = dto.id
        if (id.isBlank()) {
            Log.e(TAG, "upsertSequence: id BLANK, dto=$dto")
            throw IllegalArgumentException("KihonSequenceDto.id is blank")
        }

        val map = dto.asFirestoreMap()
        val keys = mapKeysString(map)
        Log.d(TAG, "upsertSequence(SET) path=kihon_sequences/$id keys=$keys")

        val t0 = System.currentTimeMillis()
        return suspendCancellableCoroutine { cont ->
            colSequences.document(id)
                .set(map)
                .addOnSuccessListener {
                    val dt = System.currentTimeMillis() - t0
                    Log.d(TAG, "upsertSequence: OK id=$id (${dt}ms)")
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    val dt = System.currentTimeMillis() - t0
                    Log.e(TAG, "upsertSequence: FAIL id=$id (${dt}ms) ${e.message}", e)
                    cont.resumeWithException(e)
                }
        }
    }

    /**
     * Publication d'une séquence :
     * - status = "PUBLISHED"
     * - publishedAt/by + updatedAt
     */
    suspend fun publishSequence(id: String, actorUid: String) {
        val now = System.currentTimeMillis()
        val updates = mapOf(
            // 🔑 IMPORTANT : écrire une STRING, pas un enum Firestore
            "status" to SequenceStatus.PUBLISHED.name,
            "publishedAt" to now,
            "publishedBy" to actorUid,
            "updatedAt" to now
        )
        val keys = mapKeysString(updates)
        Log.d(TAG, "publishSequence(MERGE) path=kihon_sequences/$id keys=$keys")

        val t0 = System.currentTimeMillis()
        return suspendCancellableCoroutine { cont ->
            colSequences.document(id)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    val dt = System.currentTimeMillis() - t0
                    Log.d(TAG, "publishSequence: OK id=$id (${dt}ms)")
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    val dt = System.currentTimeMillis() - t0
                    Log.e(TAG, "publishSequence: FAIL id=$id (${dt}ms) ${e.message}", e)
                    cont.resumeWithException(e)
                }
        }
    }

    /**
     * Soft update de champs méta (ex: name, objective, tags, isExamRequired).
     * Conformes à isValidKihonSequencePartial (règles).
     */
    suspend fun patchSequenceMeta(
        id: String,
        name: String? = null,
        objective: String? = null,
        tags: List<String>? = null,
        isExamRequired: Boolean? = null
    ) {
        val updates = mutableMapOf<String, Any?>(
            "updatedAt" to System.currentTimeMillis()
        )
        if (name != null) updates["name"] = name
        if (objective != null) updates["objective"] = objective
        if (tags != null) updates["tags"] = tags
        if (isExamRequired != null) updates["isExamRequired"] = isExamRequired

        val keys = mapKeysString(updates)
        Log.d(TAG, "patchSequenceMeta(MERGE) path=kihon_sequences/$id keys=$keys")

        val t0 = System.currentTimeMillis()
        return suspendCancellableCoroutine { cont ->
            colSequences.document(id)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    val dt = System.currentTimeMillis() - t0
                    Log.d(TAG, "patchSequenceMeta: OK id=$id (${dt}ms)")
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    val dt = System.currentTimeMillis() - t0
                    Log.e(TAG, "patchSequenceMeta: FAIL id=$id (${dt}ms) ${e.message}", e)
                    cont.resumeWithException(e)
                }
        }
    }

    /**
     * Ajoute un événement d'audit en sous-collection:
     * /kihon_sequences/{id}/audit/{autoId}
     * Règles: voir match /kihon_sequences/{seqId}/audit/{eventId}
     */
    suspend fun appendAuditEvent(id: String, action: String, byUid: String) {
        val event = mapOf(
            "at" to System.currentTimeMillis(),
            "by" to byUid,
            "action" to action
        )
        Log.d(TAG, "appendAuditEvent path=kihon_sequences/$id/audit payloadKeys=${mapKeysString(event)}")

        val t0 = System.currentTimeMillis()
        return suspendCancellableCoroutine { cont ->
            colSequences.document(id)
                .collection("audit")
                .add(event)
                .addOnSuccessListener {
                    val dt = System.currentTimeMillis() - t0
                    Log.d(TAG, "appendAuditEvent: OK id=$id (${dt}ms)")
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    val dt = System.currentTimeMillis() - t0
                    Log.e(TAG, "appendAuditEvent: FAIL id=$id (${dt}ms) ${e.message}", e)
                    cont.resumeWithException(e)
                }
        }
    }

    /* ─────────── BOARD STATS (agrégations simples) ─────────── */

    /** Compte global par statut (server-side aggregation). */
    suspend fun countSequencesByStatus(): Map<SequenceStatus, Long> {
        Log.d(TAG, "countSequencesByStatus()")
        val resDraft = colSequences.whereEqualTo("status", SequenceStatus.DRAFT.name)
            .count().get(AggregateSource.SERVER).awaitCount()
        val resReady = colSequences.whereEqualTo("status", SequenceStatus.READY.name)
            .count().get(AggregateSource.SERVER).awaitCount()
        val resPublished = colSequences.whereEqualTo("status", SequenceStatus.PUBLISHED.name)
            .count().get(AggregateSource.SERVER).awaitCount()
        val out = mapOf(
            SequenceStatus.DRAFT to resDraft,
            SequenceStatus.READY to resReady,
            SequenceStatus.PUBLISHED to resPublished
        )
        Log.d(TAG, "countSequencesByStatus -> $out")
        return out
    }

    /** Compte par gradeKey (liste fournie). Si null => aucun filtre (global). */
    suspend fun countSequencesByGrade(gradeKeys: List<String>?): Map<String, Long> {
        Log.d(TAG, "countSequencesByGrade keys=${gradeKeys?.size ?: 0}")
        val result = LinkedHashMap<String, Long>()
        if (gradeKeys.isNullOrEmpty()) {
            val total = colSequences.count().get(AggregateSource.SERVER).awaitCount()
            result["__ALL__"] = total
        } else {
            for (g in gradeKeys) {
                val c = colSequences.whereEqualTo("gradeKey", g)
                    .count().get(AggregateSource.SERVER).awaitCount()
                result[g] = c
            }
        }
        Log.d(TAG, "countSequencesByGrade -> $result")
        return result
    }

    suspend fun deleteSequence(id: String): Result<Unit> = runCatching {
        Log.d(TAG, "deleteSequence id=$id")
        sequences.document(id).delete().await()
        Log.d(TAG, "deleteSequence: OK id=$id")
        Unit
    }

    fun streamMovements(): Flow<Map<String, MovementRefDto>> = callbackFlow {
        val reg = colMovements.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val out = LinkedHashMap<String, MovementRefDto>()
            snap?.documents?.forEach { d ->
                val dto = d.toObject(MovementRefDto::class.java)?.copy(id = d.id)
                if (dto != null) out[d.id] = dto
            }
            trySend(out).isSuccess
        }
        awaitClose { reg.remove() }
    }

    fun streamOptions(): Flow<Map<String, OptionRefDto>> = callbackFlow {
        val reg = colOptions.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val out = LinkedHashMap<String, OptionRefDto>()
            snap?.documents?.forEach { d ->
                val dto = d.toObject(OptionRefDto::class.java)?.copy(id = d.id)
                if (dto != null) out[d.id] = dto
            }
            trySend(out).isSuccess
        }
        awaitClose { reg.remove() }
    }

    fun streamLevels(): Flow<Map<String, LevelRefDto>> = callbackFlow {
        val reg = colLevels.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val out = LinkedHashMap<String, LevelRefDto>()
            snap?.documents?.forEach { d ->
                val dto = d.toObject(LevelRefDto::class.java)?.copy(id = d.id)
                if (dto != null) out[d.id] = dto
            }
            trySend(out).isSuccess
        }
        awaitClose { reg.remove() }
    }

    suspend fun upsertMovement(dto: MovementRefDto): String {
        val id = requireNotNull(dto.id) { "upsertMovement: id required" }
        colMovements.document(id).set(dto, SetOptions.merge()).await()
        return id
    }
    suspend fun upsertMovements(list: List<MovementRefDto>): Int {
        if (list.isEmpty()) return 0
        val batch = db.batch()
        list.forEach { dto ->
            val id = requireNotNull(dto.id) { "upsertMovements: id required" }
            batch.set(colMovements.document(id), dto, SetOptions.merge())
        }
        batch.commit().await()
        return list.size
    }
    suspend fun deleteMovement(id: String) { colMovements.document(id).delete().await() }
    suspend fun isMovementCatalogEmpty(): Boolean {
        val agg = colMovements.count().get(AggregateSource.SERVER).await()
        return agg.count == 0L
    }

    suspend fun upsertOption(dto: OptionRefDto): String {
        val id = requireNotNull(dto.id) { "upsertOption: id required" }
        colOptions.document(id).set(dto, SetOptions.merge()).await()
        return id
    }
    suspend fun upsertOptions(list: List<OptionRefDto>): Int {
        if (list.isEmpty()) return 0
        val batch = db.batch()
        list.forEach { dto ->
            val id = requireNotNull(dto.id) { "upsertOptions: id required" }
            batch.set(colOptions.document(id), dto, SetOptions.merge())
        }
        batch.commit().await()
        return list.size
    }
    suspend fun deleteOption(id: String) { colOptions.document(id).delete().await() }
    suspend fun isOptionCatalogEmpty(): Boolean {
        val agg = colOptions.count().get(AggregateSource.SERVER).await()
        return agg.count == 0L
    }

    suspend fun upsertLevel(dto: LevelRefDto): String {
        val id = requireNotNull(dto.id) { "upsertLevel: id required" }
        colLevels.document(id).set(dto, SetOptions.merge()).await()
        return id
    }
    suspend fun upsertLevels(list: List<LevelRefDto>): Int {
        if (list.isEmpty()) return 0
        val batch = db.batch()
        list.forEach { dto ->
            val id = requireNotNull(dto.id) { "upsertLevels: id required" }
            batch.set(colLevels.document(id), dto, SetOptions.merge())
        }
        batch.commit().await()
        return list.size
    }
    suspend fun deleteLevel(id: String) { colLevels.document(id).delete().await() }
    suspend fun isLevelCatalogEmpty(): Boolean {
        val agg = colLevels.count().get(AggregateSource.SERVER).await()
        return agg.count == 0L
    }

}

/* ──────────────────────────────────────────────
 * Helpers
 * ────────────────────────────────────────────── */

private suspend fun com.google.android.gms.tasks.Task<com.google.firebase.firestore.AggregateQuerySnapshot>.awaitCount(): Long =
    suspendCancellableCoroutine { cont ->
        this
            .addOnSuccessListener { snap -> cont.resume(snap.count) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }
