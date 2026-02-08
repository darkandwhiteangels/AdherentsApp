package com.antechrist.adherentsapp.ui.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfOpener {

    // --- Public APIs ---------------------------------------------------------

    /** Ouvre un PDF stocké dans res/raw (ex: R.raw.ri) */
    fun openRaw(context: Context, @RawRes resId: Int, outName: String = "document.pdf"): Boolean {
        val input = context.resources.openRawResource(resId)
        return openFromStream(context, input, outName)
    }

    /** Ouvre un PDF stocké dans assets/ (ex: "docs/ri.pdf") */
    fun openAsset(context: Context, assetPath: String, outName: String = File(assetPath).name): Boolean {
        val input = context.assets.open(assetPath)
        return openFromStream(context, input, outName)
    }

    /** Ouvre un PDF déjà présent en local */
    fun openFile(context: Context, file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            launchViewer(context, uri)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible d’ouvrir le PDF (${e.message})", Toast.LENGTH_LONG).show()
            false
        }
    }

    /** Ouvre un PDF via un Uri (content://, file://, http(s)://…) */
    fun openUri(context: Context, uri: Uri): Boolean {
        return try {
            launchViewer(context, uri)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Aucun lecteur PDF installé.", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible d’ouvrir le PDF (${e.message})", Toast.LENGTH_LONG).show()
            false
        }
    }

    // --- Impl commune --------------------------------------------------------

    private fun openFromStream(context: Context, input: InputStream, outName: String): Boolean {
        input.use {
            val safeName = outName.ifBlank { "document.pdf" }
            val outFile = File(context.cacheDir, safeName)
            FileOutputStream(outFile).use { output -> it.copyTo(output) }
            return openFile(context, outFile)
        }
    }

    private fun launchViewer(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Ouvrir le PDF"))
    }
}
