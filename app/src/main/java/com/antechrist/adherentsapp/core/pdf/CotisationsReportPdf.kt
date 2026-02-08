package com.antechrist.adherentsapp.core.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CotisationReportRowPdf(
    val guardianName: String,
    val membersCount: Int,
    val statusLabel: String,
    val dueCents: Long,
    val paidCents: Long,
    val remainingCents: Long
)

data class CotisationReportTotalsPdf(
    val householdCount: Int,
    val membersCount: Int,
    val totalDueCents: Long,
    val totalPaidCents: Long,
    val totalRemainingCents: Long
)

object CotisationsReportPdf {

    /**
     * PDF A4 - header (club+season+date snapshot) - table paginée - totals en bas.
     * Basé sur snapshot (ou dataset figé fourni).
     */
    fun generate(
        context: Context,
        clubName: String,
        seasonKey: String,
        snapshotAtMillis: Long,
        rows: List<CotisationReportRowPdf>,
        totals: CotisationReportTotalsPdf
    ): File {

        val euro = NumberFormat.getCurrencyInstance(Locale.FRANCE)
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        val snapshotLabel = df.format(Date(snapshotAtMillis))

        // A4 @72dpi
        val pageWidth = 595
        val pageHeight = 842

        val doc = PdfDocument()

        val margin = 36
        val lineH = 16

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
        val linePaint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 1f
        }

        fun Canvas.drawLineText(x: Int, y: Int, text: String, paint: Paint) {
            drawText(text, x.toFloat(), y.toFloat(), paint)
        }

        var pageNumber = 1
        var y = margin
        var currentPage: PdfDocument.Page? = null

        // Colonnes (positions fixes simples)
        val colNameX = margin
        val colMembersX = 260
        val colStatusX = 310
        val colDueX = 390
        val colPaidX = 450
        val colRemainX = 560

        fun newPage(): Canvas {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = doc.startPage(pageInfo)
            currentPage = page
            pageNumber++
            y = margin
            return page.canvas
        }

        fun finishPage() {
            currentPage?.let { doc.finishPage(it) }
            currentPage = null
        }

        fun drawHeader(c: Canvas) {
            c.drawLineText(margin, y, clubName.ifBlank { "Club" }, titlePaint)
            y += lineH + 2
            c.drawLineText(margin, y, "Rapport cotisations", hPaint)
            y += lineH
            c.drawLineText(margin, y, "Saison : $seasonKey", textPaint)
            y += lineH
            c.drawLineText(margin, y, "Snapshot : $snapshotLabel", textPaint)
            y += lineH + 6
            c.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
            y += lineH
        }

        fun drawTableHeader(c: Canvas) {
            c.drawLineText(colNameX, y, "Responsable", hPaint)
            c.drawLineText(colMembersX, y, "Nb", hPaint)
            c.drawLineText(colStatusX, y, "Statut", hPaint)
            c.drawLineText(colDueX, y, "Dû", hPaint)
            c.drawLineText(colPaidX, y, "Payé", hPaint)
            c.drawLineText(colRemainX - 40, y, "Reste", hPaint)
            y += lineH
            c.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
            y += lineH
        }

        fun ensureSpace(linesNeeded: Int, canvasRef: Canvas): Canvas {
            val limit = pageHeight - margin
            return if (y + linesNeeded * lineH > limit) {
                finishPage()
                val c = newPage()
                drawHeader(c)
                drawTableHeader(c)
                c
            } else canvasRef
        }

        var canvas = newPage()
        drawHeader(canvas)
        drawTableHeader(canvas)

        rows.forEach { r ->
            canvas = ensureSpace(2, canvas)

            val name = if (r.guardianName.length > 35) r.guardianName.take(34) + "…" else r.guardianName
            val status = if (r.statusLabel.length > 12) r.statusLabel.take(11) + "…" else r.statusLabel

            val due = euro.format(r.dueCents / 100.0)
            val paid = euro.format(r.paidCents / 100.0)
            val rem = euro.format(r.remainingCents / 100.0)

            canvas.drawLineText(colNameX, y, name, textPaint)
            canvas.drawLineText(colMembersX, y, r.membersCount.toString(), textPaint)
            canvas.drawLineText(colStatusX, y, status, textPaint)

            // montants alignés à droite “à la main” (simple)
            canvas.drawLineText(colDueX, y, due, textPaint)
            canvas.drawLineText(colPaidX, y, paid, textPaint)
            canvas.drawLineText(colRemainX - 40, y, rem, textPaint)

            y += lineH
        }

        // Totaux en bas (sur la dernière page) : on force espace
        canvas = ensureSpace(6, canvas)
        y += 8
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
        y += lineH

        canvas.drawLineText(margin, y, "Totaux", hPaint); y += lineH
        canvas.drawLineText(margin, y, "Foyers : ${totals.householdCount}   •   Adhérents : ${totals.membersCount}", textPaint); y += lineH

        canvas.drawLineText(margin, y, "Total dû : ${euro.format(totals.totalDueCents / 100.0)}", textPaint); y += lineH
        canvas.drawLineText(margin, y, "Total payé : ${euro.format(totals.totalPaidCents / 100.0)}", textPaint); y += lineH
        canvas.drawLineText(margin, y, "Total restant : ${euro.format(totals.totalRemainingCents / 100.0)}", textPaint); y += lineH

        y += 10
        canvas.drawLineText(margin, y, "Document généré par AdherentsApp", tinyPaint)

        finishPage()

        val outFile = File(
            context.cacheDir,
            "rapport_cotisations_${seasonKey}_${snapshotAtMillis}.pdf"
        )

        FileOutputStream(outFile).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()

        return outFile
    }
}
