package com.antechrist.adherentsapp.core.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

fun createImageUri(
    context: Context,
    fileName: String = "adherent_${System.currentTimeMillis()}.jpg"
): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AdherentsApp")
        put(MediaStore.Images.Media.IS_PENDING, 0)
    }
    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    )
}
