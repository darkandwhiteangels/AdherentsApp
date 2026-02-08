package com.antechrist.adherentsapp.data.remote

import android.util.Log
import com.antechrist.adherentsapp.data.model.dto.InfoMessageDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InfoMessageRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun collection(): CollectionReference =
        firestore.collection(COLLECTION_NAME)

    fun listenActiveMessages(): Flow<List<InfoMessageDto>> = callbackFlow {
        val listener = collection()
            .whereEqualTo("active", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("InfoRemote", "listenActiveMessages() error", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snap?.documents?.mapNotNull { doc ->
                    val dto = doc.toObject(InfoMessageDto::class.java)
                    if (dto == null) {
                        null
                    } else {
                        dto.copy(id = doc.id)
                    }
                } ?: emptyList()

                Log.d("InfoRemote", "listenActiveMessages() got ${list.size} docs")
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addMessage(dto: InfoMessageDto): String {
        Log.d("InfoRemote", "addMessage() writing to Firestore in '$COLLECTION_NAME' ...")
        val ref = collection().add(dto).await()
        Log.d("InfoRemote", "addMessage() Firestore id=${ref.id}")
        return ref.id
    }

    suspend fun deactivateMessage(messageId: String) {
        Log.d("InfoRemote", "deactivateMessage($messageId)")
        collection()
            .document(messageId)
            .update("active", false)
            .await()
    }

    companion object {
        private const val COLLECTION_NAME = "info_messages"
    }

    suspend fun updateMessage(messageId: String, title: String, body: String) {
        collection().document(messageId).update(
            mapOf(
                "title" to title,
                "body" to body
            )
        ).await()
    }

    suspend fun deleteMessage(messageId: String) {
        collection().document(messageId).delete().await()
    }

    suspend fun getCreatedAtMillis(messageId: String): Long? {
        val snap = collection().document(messageId).get().await()
        val ts = snap.getTimestamp("createdAt") ?: return null
        return ts.toDate().time
    }
}
