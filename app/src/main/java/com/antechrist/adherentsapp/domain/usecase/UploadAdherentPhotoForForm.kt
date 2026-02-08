// domain/usecase/UploadAdherentPhotoForForm.kt
package com.antechrist.adherentsapp.domain.usecase

import android.net.Uri
import com.antechrist.adherentsapp.data.firestore.StorageDataSource
import javax.inject.Inject

class UploadAdherentPhotoForForm @Inject constructor(
    private val storage: StorageDataSource
) {
    suspend operator fun invoke(adherentId: String?, fileUri: Uri): String {
        return storage.uploadAdherentPhotoForForm(adherentId, fileUri)
    }
}
