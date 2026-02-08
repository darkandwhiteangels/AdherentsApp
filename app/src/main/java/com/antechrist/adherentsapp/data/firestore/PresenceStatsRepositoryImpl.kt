package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.domain.repository.PresenceStatsRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class PresenceStatsRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : PresenceStatsRepository {

    override fun streamCountsForSeason(seasonKey: String): Flow<Map<String, Int>> = callbackFlow {
        val ref = db.collection("presence_stats")
            .document(seasonKey)
            .collection("members")

        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null || snap == null) {
                trySend(emptyMap())
                return@addSnapshotListener
            }
            val map: Map<String, Int> = snap.documents.associate { d ->
                d.id to ((d.getLong("presentCount") ?: 0L).toInt())
            }
            trySend(map)
        }
        awaitClose { reg.remove() }
    }
}
