// ui/utils/FileShare.kt
package com.antechrist.adherentsapp.ui.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object FileShare {

    fun sharePdf(context: Context, file: File, title: String = "Exporter PDF") {
        shareFile(
            context = context,
            file = file,
            mimeType = "application/pdf",
            title = title
        )
    }

    fun shareCsv(context: Context, file: File, title: String = "Exporter CSV") {
        shareFile(
            context = context,
            file = file,
            mimeType = "text/csv",
            title = title
        )
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
