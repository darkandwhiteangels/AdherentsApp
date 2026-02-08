package com.antechrist.adherentsapp.core.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MinutesDraftPdf {

    fun generate(
        context: Context,
        clubName: String,
        seasonKey: String,
        agDateMillis: Long,
        draftId: String,
        version: Int,
        generatedAtMillis: Long
    ): File {

        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        val agLabel = df.format(Date(agDateMillis))
        val genLabel = df.format(Date(generatedAtMillis))

        // A4 @72dpi
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36
        val lineH = 16

        val doc = PdfDocument()

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val hPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
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
        val linePaint = Paint().apply { strokeWidth = 1f }

        fun Canvas.t(x: Int, y: Int, s: String, p: Paint) = drawText(s, x.toFloat(), y.toFloat(), p)

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas

        var y = margin

        c.t(margin, y, clubName.ifBlank { "Club" }, titlePaint); y += lineH + 4
        c.t(margin, y, "Compte-rendu AG (DRAFT)", hPaint); y += lineH
        c.t(margin, y, "Saison : $seasonKey", textPaint); y += lineH
        c.t(margin, y, "AG prévue : $agLabel", textPaint); y += lineH
        c.drawLine(margin.toFloat(), (y + 6).toFloat(), (pageWidth - margin).toFloat(), (y + 6).toFloat(), linePaint)
        y += lineH + 8

        c.t(margin, y, "Trame (à compléter) :", hPaint); y += lineH + 4
        c.t(margin, y, "1) Présents / Représentés", textPaint); y += lineH
        c.t(margin, y, "2) Rapport moral", textPaint); y += lineH
        c.t(margin, y, "3) Rapport financier", textPaint); y += lineH
        c.t(margin, y, "4) Votes / Résolutions", textPaint); y += lineH
        c.t(margin, y, "5) Questions diverses", textPaint); y += lineH
        y += lineH

        c.t(margin, y, "Signatures (à l'AG) :", hPaint); y += lineH + 4
        c.t(margin, y, "Président : ____________________", textPaint); y += lineH
        c.t(margin, y, "Secrétaire : ____________________", textPaint); y += lineH

        // Footer
        val footerY = pageHeight - margin
        c.t(margin, footerY, "DraftId: $draftId  •  Version: $version  •  Généré le $genLabel", tinyPaint)

        doc.finishPage(page)

        val out = File(context.cacheDir, "cr_ag_draft_${seasonKey}_${draftId}_v${version}.pdf")
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()

        return out
    }
}
