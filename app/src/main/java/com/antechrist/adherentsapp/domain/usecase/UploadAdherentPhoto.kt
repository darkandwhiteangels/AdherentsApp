package com.antechrist.adherentsapp.domain.usecase

import android.net.Uri
import com.antechrist.adherentsapp.data.firestore.StorageDataSource
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import javax.inject.Inject

class UploadAdherentPhoto @Inject constructor(
    private val storage: StorageDataSource,
    private val repo: AdherentsRepository
) {
    suspend operator fun invoke(adherentId: String, fileUri: Uri) {
        val url = storage.uploadAdherentPhoto(adherentId, fileUri) // cloud
        val now = System.currentTimeMillis()
        repo.setAdherentPhoto(adherentId, url, now)                // Firestore
    }
}
