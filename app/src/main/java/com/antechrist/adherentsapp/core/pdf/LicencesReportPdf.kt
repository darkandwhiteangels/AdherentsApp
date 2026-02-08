package com.antechrist.adherentsapp.core.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

data class LicenceReportRowPdf(
    val guardianName: String,
    val statusLabel: String,
    val licenceCount: Int,
    val licencesTotalCents: Long
)

object LicencesReportPdf {

    fun generate(
        context: Context,
        seasonKey: String,
        filterLabel: String,
        totalLicences: Int,
        totalAmountCents: Long,
        rows: List<LicenceReportRowPdf>
    ): File {
        val euro = NumberFormat.getCurrencyInstance(Locale.FRANCE)

        val doc = PdfDocument()

        val pageWidth = 595    // A4 ~ 72dpi
        val pageHeight = 842

        val margin = 36
        val lineH = 18

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        fun Canvas.drawLineText(x: Int, y: Int, text: String, paint: Paint = textPaint) {
            drawText(text, x.toFloat(), y.toFloat(), paint)
        }

        var pageNumber = 1
        var y = margin

        var currentPage: PdfDocument.Page? = null

        fun newPage(): Canvas {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = doc.startPage(pageInfo)
            currentPage = page
            pageNumber++
            y = margin
            return page.canvas
        }


        var canvas = newPage()

        fun drawHeader(c: Canvas) {
            c.drawLineText(margin, y, "Rapport licences FFK", titlePaint)
            y += lineH + 4

            c.drawLineText(margin, y, "Saison : $seasonKey", textPaint)
            y += lineH
            c.drawLineText(margin, y, "Filtre : $filterLabel", textPaint)
            y += lineH

            c.drawLineText(margin, y, "Total licenciés : $totalLicences", hPaint)
            y += lineH
            c.drawLineText(margin, y, "Total à reverser FFK : ${euro.format(totalAmountCents / 100.0)}", hPaint)
            y += lineH + 6

            // Table header
            c.drawLineText(margin, y, "Responsable", hPaint)
            c.drawLineText(330, y, "Statut", hPaint)
            c.drawLineText(420, y, "Lic.", hPaint)
            c.drawLineText(470, y, "Montant", hPaint)
            y += lineH

            // petite séparation
            c.drawLine( margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), Paint().apply { strokeWidth = 1f } )
            y += lineH
        }

        fun ensureSpace(linesNeeded: Int) {
            val needed = linesNeeded * lineH
            if (y + needed > pageHeight - margin) {
                currentPage?.let { doc.finishPage(it) }   // ✅ finishPage(Page)
                canvas = newPage()
                drawHeader(canvas)
            }
        }


        // 1ère page header
        drawHeader(canvas)

        rows.forEach { r ->
            ensureSpace(2)

            val amount = euro.format(r.licencesTotalCents / 100.0)

            // Nom tronqué si long
            val name = if (r.guardianName.length > 34) r.guardianName.take(33) + "…" else r.guardianName
            val status = if (r.statusLabel.length > 10) r.statusLabel.take(10) + "…" else r.statusLabel

            canvas.drawLineText(margin, y, name, textPaint)
            canvas.drawLineText(330, y, status, textPaint)
            canvas.drawLineText(425, y, r.licenceCount.toString(), textPaint)
            canvas.drawLineText(470, y, amount, textPaint)

            y += lineH
        }

        // footer simple (sur la dernière page)
        ensureSpace(2)
        y += 8
        canvas.drawLineText(margin, y, "Document généré par AdherentsApp", textPaint)

        // Fin dernière page
        currentPage?.let { doc.finishPage(it) }

        val outFile = File(
            context.cacheDir,
            "rapport_licences_ffk_${seasonKey}_${System.currentTimeMillis()}.pdf"
        )

        FileOutputStream(outFile).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()

        return outFile
    }
}
