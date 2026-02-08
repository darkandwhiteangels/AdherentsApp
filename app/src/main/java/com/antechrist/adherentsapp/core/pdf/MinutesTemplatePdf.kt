package com.antechrist.adherentsapp.core.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MinutesTemplatePdf {

    enum class Mode { FULL, SHORT }

    suspend fun generate(
        context: Context,
        mode: Mode,
        seasonKey: String,
        draftId: String,
        version: Int,
        generatedAtMillis: Long,
        values: Map<String, String>,
        signatureUrl: String?
    ): File {
        val templatePath = when (mode) {
            Mode.FULL -> "templates/cr_complet.txt"
            Mode.SHORT -> "templates/cr_synthese.txt"
        }

        val content = TextTemplateRenderer.renderAssetTemplate(context, templatePath, values)
        val signatureBitmap = signatureUrl?.takeIf { it.startsWith("http") }?.let { url ->
            loadBitmap(url)
        }

        // A4 @72dpi
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val tinyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val lineH = 15
        val maxWidth = pageWidth - margin * 2

        val doc = PdfDocument()

        fun newPage(pageIndex: Int): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            return doc.startPage(info)
        }

        fun drawFooter(c: Canvas) {
            val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
            val genLabel = df.format(Date(generatedAtMillis))
            val footerY = pageHeight - margin
            c.drawText(
                "DraftId: $draftId  •  Version: $version  •  Généré le $genLabel",
                margin.toFloat(),
                footerY.toFloat(),
                tinyPaint
            )
        }

        fun wrapLine(line: String, paint: Paint): List<String> {
            if (line.isBlank()) return listOf("")
            val words = line.split(" ")
            val out = mutableListOf<String>()
            var current = ""
            for (w in words) {
                val test = if (current.isBlank()) w else "$current $w"
                if (paint.measureText(test) <= maxWidth) {
                    current = test
                } else {
                    if (current.isNotBlank()) out.add(current)
                    current = w
                }
            }
            if (current.isNotBlank()) out.add(current)
            return out
        }

        val lines = content.replace("\r\n", "\n").split("\n")

        var pageIndex = 1
        var page = newPage(pageIndex)
        var c = page.canvas
        var y = margin

        fun ensureSpace(needed: Int) {
            if (y + needed >= pageHeight - margin - 20) {
                drawFooter(c)
                doc.finishPage(page)
                pageIndex++
                page = newPage(pageIndex)
                c = page.canvas
                y = margin
            }
        }

        // Petit titre PDF (haut)
        val pdfTitle = if (mode == Mode.FULL) "CR annuel (complet)" else "CR annuel (synthèse)"
        c.drawText(pdfTitle, margin.toFloat(), y.toFloat(), titlePaint)
        y += lineH + 6

        for (raw in lines) {
            val isHeading = raw.isNotBlank() && raw == raw.uppercase(Locale.FRANCE) // ex: "EFFECTIFS"
            val paint = if (isHeading) titlePaint else textPaint

            val wrapped = wrapLine(raw, paint)
            for (w in wrapped) {
                ensureSpace(lineH)
                c.drawText(w, margin.toFloat(), y.toFloat(), paint)
                y += lineH
            }
        }

        // Signature image (si dispo) : on la met en bas si place, sinon nouvelle page
        signatureBitmap?.let { bmp ->
            val targetH = 60
            val scale = targetH.toFloat() / bmp.height.toFloat()
            val targetW = (bmp.width * scale).toInt()
            ensureSpace(targetH + 40)
            y += 10
            c.drawText("Signature :", margin.toFloat(), y.toFloat(), textPaint)
            y += 10
            val rect = Rect(margin, y, margin + targetW, y + targetH)
            c.drawBitmap(bmp, null, rect, null)
            y += targetH + 10
        }

        drawFooter(c)
        doc.finishPage(page)

        val fileName = when (mode) {
            Mode.FULL -> "cr_complet_${seasonKey}_${draftId}_v${version}.pdf"
            Mode.SHORT -> "cr_synthese_${seasonKey}_${draftId}_v${version}.pdf"
        }
        val out = File(context.cacheDir, fileName)
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()

        return out
    }

    private suspend fun loadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = URL(url).openStream().use { it.readBytes() }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
