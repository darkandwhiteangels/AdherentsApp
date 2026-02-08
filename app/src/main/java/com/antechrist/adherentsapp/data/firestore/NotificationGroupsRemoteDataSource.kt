package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.domain.model.NotificationGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationGroupsRemoteDataSource @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val col get() = db.collection("notification_groups")

    fun streamGroups(): Flow<List<NotificationGroup>> = callbackFlow {
        val sub = col.addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.e("GroupsRemote", "streamGroups error", err)
                trySend(emptyList())  // ✅ ne pas close(err)
                return@addSnapshotListener
            }

            val list = snap?.documents?.mapNotNull { doc ->
                val name = (doc.getString("name") ?: "").trim()
                if (name.isBlank()) return@mapNotNull null

//                val memberIds =
//                    (doc.get("memberAdherentIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val memberIds =
                    (doc.get("memberGuardianIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                val active = doc.getBoolean("active") ?: true // ✅ défaut true si champ absent

                NotificationGroup(
                    id = doc.id,
                    name = name,
                    // memberAdherentIds = memberIds,
                    memberGuardianIds = memberIds,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    createdByUid = doc.getString("createdByUid") ?: "",
                    active = active
                )
            } ?: emptyList()

            // ✅ si tu veux filtrer soft-delete uniquement :
            // val visible = list.filter { it.active }
            // trySend(visible.sortedBy { it.name.lowercase() })

            trySend(list.sortedBy { it.name.lowercase() })
        }

        awaitClose { sub.remove() }
    }

    suspend fun createGroup(
        name: String,
        memberGuardianIds: List<String>,
        createdByUid: String
    ): String {
        val doc = col.document()
        val data = mapOf(
            "name" to name.trim(),
            "memberGuardianIds" to memberGuardianIds.distinct(),
            "createdAt" to System.currentTimeMillis(),
            "createdByUid" to createdByUid,
            "active" to true
        )
        doc.set(data, SetOptions.merge()).await()
        return doc.id
    }

    suspend fun updateMembers(groupId: String, memberGuardianIds: List<String>) {
        col.document(groupId).set(
            mapOf("memberGuardianIds" to memberGuardianIds.distinct()),
            SetOptions.merge()
        ).await()
    }

    suspend fun rename(groupId: String, name: String) {
        col.document(groupId).set(
            mapOf("name" to name.trim()),
            SetOptions.merge()
        ).await()
    }

    suspend fun deleteGroup(groupId: String) {
        col.document(groupId).delete().await()
    }
}
