package com.antechrist.adherentsapp.data.firestore

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    /** Upload dans le cloud : /adherents/{id}/profile.jpg → retourne le download URL (https://...) */
    suspend fun uploadAdherentPhoto(adherentId: String, fileUri: Uri): String {
        val ref = storage.reference.child("adherents/$adherentId/profile.jpg")
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }

    // data/firestore/StorageDataSource.kt
    suspend fun uploadAdherentPhotoForForm(adherentId: String?, fileUri: Uri): String {
        val path = if (!adherentId.isNullOrBlank()) {
            "adherents/$adherentId/profile.jpg"
        } else {
            "adherents/_new/${java.util.UUID.randomUUID()}.jpg"
        }
        val ref = storage.reference.child(path)
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }
}
