package com.antechrist.adherentsapp.data.firestore

import android.util.Log
import com.antechrist.adherentsapp.core.utils.SeasonUtils
import com.antechrist.adherentsapp.domain.model.PresenceRecord
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

private const val TAG = "PresenceDataSource"

class PresenceDataSource(
    private val db: FirebaseFirestore
) {
    /** Clé de date 'yyyy-MM-dd' */
    fun dateKey(date: LocalDate): String = date.toString()

    /** Clé de groupe safe pour path Firestore */
    fun groupKey(display: String): String =
        display.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    // --------------------- LECTURE ----------------------

    /** Liste les dates existantes via l’ID (yyyy-MM-dd). */
    suspend fun listDateKeysInRange(startKey: String, endKey: String): List<String> = runCatching {
        Log.d(TAG, "listDateKeysInRange $startKey..$endKey")
        val snap = db.collection("presence")
            .orderBy(FieldPath.documentId())
            .startAt(startKey)
            .endAt(endKey)
            .get()
            .await()
        val ids = snap.documents.map { it.id }
        Log.d(TAG, "listDateKeysInRange -> ${ids.size} ids")
        ids
    }.getOrElse { e ->
        Log.e(TAG, "listDateKeysInRange error: ${e.message}", e)
        emptyList()
    }

    /** Récupère toutes les présences pour un (dateKey, groupKey) => Map<adherentId, PresenceRecord> */
    suspend fun getDayGroupPresence(dateKey: String, groupKey: String): Map<String, PresenceRecord> = runCatching {
        val ref = db.collection("presence").document(dateKey).collection(groupKey)
        val snap = ref.get().await()
        Log.d(TAG, "getDayGroupPresence presence/$dateKey/$groupKey -> docs=${snap.size()}")
        snap.documents.associate { d ->
            d.id to PresenceRecord(
                status = (d.getString("status") ?: "absent").lowercase(), // défaut cohérent
                // champ note supprimé dans le modèle de l’app
                updatedBy = d.getString("updatedBy"),
                updatedAt = d.getLong("updatedAt"),
                trialCounted = d.getBoolean("trialCounted") ?: false
            )
        }
    }.getOrElse { e ->
        Log.e(TAG, "getDayGroupPresence error: ${e.message}", e)
        emptyMap()
    }

    // --------------------- STREAM ----------------------

    /** Flux temps réel d’un (dateKey, groupKey) */
    fun stream(dateKey: String, groupKey: String): Flow<Map<String, PresenceRecord>> = callbackFlow {
        val ref = db.collection("presence").document(dateKey).collection(groupKey)
        Log.d(TAG, "stream -> presence/$dateKey/$groupKey")

        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "stream error: ${err.message}", err)
                trySend(emptyMap())
                return@addSnapshotListener
            }
            if (snap == null) {
                Log.w(TAG, "stream snapshot null")
                trySend(emptyMap())
                return@addSnapshotListener
            }

            val map: Map<String, PresenceRecord> = snap.documents.associate { d ->
                d.id to PresenceRecord(
                    status = (d.getString("status") ?: "absent").lowercase(), // défaut cohérent
                    updatedBy = d.getString("updatedBy"),
                    updatedAt = d.getLong("updatedAt"),
                    trialCounted = d.getBoolean("trialCounted") ?: false
                )
            }
            Log.d(TAG, "stream emit docs=${snap.size()}")
            trySend(map)
        }

        awaitClose { reg.remove() }
    }

    // --------------------- SAVE (legacy, conservé pour compat) ----------------------

    suspend fun saveBatch(
        dateKey: String,
        groupKey: String,
        updates: Map<String, PresenceRecord>,
        overrideUpdatedBy: String? = null,
        addGroupField: Boolean = true
    ) {
        val path = "presence/$dateKey/$groupKey"
        Log.d(TAG, "saveBatch $path count=${updates.size}")

        val batch = db.batch()
        val now = System.currentTimeMillis()

        for ((adherentId, rec) in updates) {
            val doc = db.collection("presence")
                .document(dateKey)
                .collection(groupKey)
                .document(adherentId)

            val status = rec.status.trim().lowercase()
            val data = hashMapOf<String, Any?>(
                "status" to status,
                "updatedBy" to (overrideUpdatedBy ?: rec.updatedBy),
                "updatedAt" to (rec.updatedAt ?: now)
            )
            if (addGroupField) data["groupe"] = groupKey

            batch.set(doc, data, SetOptions.merge())
        }

        runCatching { batch.commit().await() }
            .onSuccess { Log.d(TAG, "saveBatch OK for $path") }
            .onFailure { e ->
                Log.e(TAG, "saveBatch ERROR for $path: ${e.message}", e)
                throw e
            }
    }

    // --------------------- SAVE with STATS (batch saison) ----------------------

    suspend fun saveBatchWithStats(
        dateKey: String,
        groupKey: String,
        updates: Map<String, PresenceRecord>,
        adminUid: String,
        addGroupField: Boolean = true
    ) {
        val seasonKey = SeasonUtils.seasonKeyFor(LocalDate.parse(dateKey))
        val basePath = "presence/$dateKey/$groupKey"
        Log.d(TAG, "saveBatchWithStats $basePath season=$seasonKey count=${updates.size}")

        for ((adherentId, recNew) in updates) {
            val newStatus = recNew.status.trim().lowercase()

            runCatching {
                db.runTransaction { tx ->
                    val presenceRef = db.collection("presence")
                        .document(dateKey)
                        .collection(groupKey)
                        .document(adherentId)

                    val statsRef = db.collection("presence_stats")
                        .document(seasonKey)
                        .collection("members")
                        .document(adherentId)

                    // --- READS
                    val prevSnap = tx.get(presenceRef)
                    val prevStatus = prevSnap.getString("status")?.lowercase()

                    val delta = when {
                        prevStatus != "present" && newStatus == "present" -> 1
                        prevStatus == "present" && newStatus != "present" -> -1
                        else -> 0
                    }

                    val statsSnap = tx.get(statsRef)
                    val currentCount = (statsSnap.getLong("presentCount") ?: 0L).toInt()
                    val newCount = (currentCount + delta).coerceAtLeast(0)

                    // --- WRITES
                    val now = System.currentTimeMillis()
                    val presenceData = hashMapOf<String, Any?>(
                        "status" to newStatus,
                        "updatedBy" to adminUid,
                        "updatedAt" to now
                    ).apply { if (addGroupField) put("groupe", groupKey) }
                    tx.set(presenceRef, presenceData, SetOptions.merge())

                    if (delta != 0) {
                        val statsData = hashMapOf<String, Any?>(
                            "presentCount" to newCount,
                            "updatedBy" to adminUid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).apply {
                            if (delta > 0) {
                                put("lastPresentAt", FieldValue.serverTimestamp())
                                put("lastDateKey", dateKey)
                            }
                        }
                        tx.set(statsRef, statsData, SetOptions.merge())
                    }

                    null
                }.await()
            }.onSuccess {
                Log.d(TAG, "saveBatchWithStats OK: $dateKey/$groupKey/$adherentId -> $newStatus (season=$seasonKey)")
            }.onFailure { e ->
                Log.e(TAG, "saveBatchWithStats ERROR for $dateKey/$groupKey/$adherentId: ${e.message}", e)
                throw e
            }
        }
    }

    // --------------------- UNITAIRE: update + stats + essai ----------------------

    /**
     * Met à jour la présence pour un adhérent et maintient:
     *  - le doc du jour (presence)
     *  - le **cumul saison** (presence_stats)
     *  - l'**essai** (trialPresentCount, trialCompleted) sur le doc adhérent
     *
     * @param newStatus "present" | "absent"
     */
    suspend fun updateStatusWithTrial(
        dateKey: String,
        groupKey: String,
        adherentId: String,
        newStatus: String,
        overrideUpdatedBy: String? = null,
        groupDisplay: String? = null
    ) {
        require(newStatus == "present" || newStatus == "absent")

        val presenceRef = db.collection("presence")
            .document(dateKey)
            .collection(groupKey)
            .document(adherentId)

        val adherentRef = db.collection("adherents").document(adherentId)

        // NEW: ref stats saison
        val seasonKey = SeasonUtils.seasonKeyFor(LocalDate.parse(dateKey))
        val statsRef = db.collection("presence_stats")
            .document(seasonKey)
            .collection("members")
            .document(adherentId)

        val now = System.currentTimeMillis()
        val adminUid = overrideUpdatedBy

        runCatching {
            db.runTransaction { tx ->
                // ---- READ presence doc
                val prevSnap = tx.get(presenceRef)
                val prevStatus = prevSnap.getString("status")?.lowercase()
                val prevTrial = prevSnap.getBoolean("trialCounted") ?: false

                // ---- READ adherent aggregate (essai)
                val aSnap = tx.get(adherentRef)
                val curCount = (aSnap.getLong("trialPresentCount") ?: 0L).toInt().coerceAtLeast(0)
                val curCompleted = aSnap.getBoolean("trialCompleted") ?: (curCount >= 2)

                // ---- READ stats saison (pour delta)
                val statsSnap = tx.get(statsRef)
                val currentSeasonCount = (statsSnap.getLong("presentCount") ?: 0L).toInt()

                // ---- Delta pour presence_stats
                val delta = when {
                    prevStatus != "present" && newStatus == "present" -> 1
                    prevStatus == "present" && newStatus != "present" -> -1
                    else -> 0
                }
                val newSeasonCount = (currentSeasonCount + delta).coerceAtLeast(0)

                // ---- Essai (trial) logic
                var trialCounted = prevTrial
                var newTrialCount = curCount
                var newTrialCompleted = curCompleted

                when (newStatus) {
                    "present" -> {
                        if (!curCompleted && !prevTrial) {
                            trialCounted = true
                            newTrialCount = (curCount + 1).coerceAtLeast(0)
                            newTrialCompleted = newTrialCount >= 2
                        }
                        if (curCompleted) {
                            trialCounted = false
                        }
                    }
                    "absent" -> {
                        if (prevTrial) {
                            trialCounted = false
                            newTrialCount = (curCount - 1).coerceAtLeast(0)
                            newTrialCompleted = newTrialCount >= 2
                        }
                    }
                }

                // ---- WRITE presence doc
                val presenceData = hashMapOf<String, Any?>(
                    "status" to newStatus,
                    "trialCounted" to trialCounted,
                    "updatedAt" to now,
                    "updatedBy" to adminUid
                ).also {
                    if (groupDisplay != null) it["groupe"] = groupDisplay
                }
                tx.set(presenceRef, presenceData, SetOptions.merge())

                // ---- WRITE presence_stats (si delta != 0)
                if (delta != 0) {
                    val statsData = hashMapOf<String, Any?>(
                        "presentCount" to newSeasonCount,
                        "updatedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).apply {
                        if (delta > 0) {
                            put("lastPresentAt", FieldValue.serverTimestamp())
                            put("lastDateKey", dateKey)
                        }
                    }
                    tx.set(statsRef, statsData, SetOptions.merge())
                }

                // ---- WRITE adherent aggregate (essai)
                val adherentData = hashMapOf<String, Any?>(
                    "trialPresentCount" to newTrialCount,
                    "trialCompleted" to newTrialCompleted
                )
                tx.set(adherentRef, adherentData, SetOptions.merge())

                null
            }.await()
        }.onSuccess {
            Log.d(TAG, "updateStatusWithTrial OK: $dateKey/$groupKey/$adherentId -> $newStatus (stats delta applied)")
        }.onFailure { e ->
            Log.e(TAG, "updateStatusWithTrial ERROR for $dateKey/$groupKey/$adherentId: ${e.message}", e)
            throw e
        }
    }
}
