package com.antechrist.adherentsapp.domain.usecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.antechrist.adherentsapp.domain.model.PresenceRow
import com.antechrist.adherentsapp.domain.model.PresenceStatus
import com.antechrist.adherentsapp.ui.utils.PdfStyles
import com.antechrist.adherentsapp.ui.utils.PdfStyles.s
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.min
import javax.inject.Inject

class ExportPresenceToPdf @Inject constructor() {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.FRANCE)
    private val dateHumanFmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE)

    fun invoke(
        context: Context,
        date: LocalDate,
        groupLabel: String,
        rows: List<PresenceRow>
    ): File {
        val fileName = "presence_${date.format(dateFmt)}_${sanitizeFileName(groupLabel)}.pdf"
        val outFile = File(context.cacheDir, fileName)

        val pageInfo = PdfDocument.PageInfo.Builder(
            PdfStyles.PAGE_WIDTH.toInt(),
            PdfStyles.PAGE_HEIGHT.toInt(),
            1
        ).create()

        val doc = PdfDocument()
        try {
            val left = PdfStyles.MARGIN_H
            val top = PdfStyles.MARGIN_V
            val right = PdfStyles.PAGE_WIDTH - PdfStyles.MARGIN_H
            val bottom = PdfStyles.PAGE_HEIGHT - PdfStyles.MARGIN_V

            // Colonnes en pourcentage pour s'adapter facilement
            val tableWidth = right - left
            val colNomW    = tableWidth * 0.35f
            val colPrenomW = tableWidth * 0.25f
            val colStatutW = tableWidth * 0.14f
            val colNoteW   = tableWidth - (colNomW + colPrenomW + colStatutW)

            val headerHeight = s(18f) + PdfStyles.HEADER_ROW_HEIGHT + s(12f) // titre + infos + esp.
            val footerHeight = s(24f)
            val availableHeight = (bottom - top) - headerHeight - footerHeight

            val rowsPerPage = floor(availableHeight / PdfStyles.ROW_HEIGHT).toInt().coerceAtLeast(1)

            var currentIndex = 0
            var pageNumber = 1

            while (currentIndex < rows.size || (rows.isEmpty() && pageNumber == 1)) {
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                // === En-tête (titre + date + groupe) ===
                drawHeader(canvas, date, groupLabel, left, top, right)

                // === Table header (fond gris + libellés) ===
                val tableTop = top + headerHeight
                drawTableHeader(canvas, left, tableTop, colNomW, colPrenomW, colStatutW, colNoteW)

                // === Corps ===
                val startY = tableTop + PdfStyles.ROW_HEIGHT
                var y = startY
                val end = if (rows.isEmpty()) 0 else min(currentIndex + rowsPerPage, rows.size)

                if (rows.isEmpty()) {
                    val p = PdfStyles.bodyPaint()
                    val base = PdfStyles.centeredBaseline(y, PdfStyles.ROW_HEIGHT, p)
                    canvas.drawText("Aucune donnée pour cette sélection.", left + s(6f), base, p)
                } else {
                    for ((rowIdx, i) in (currentIndex until end).withIndex()) {
                        val r = rows[i]
                        drawRow(
                            canvas = canvas,
                            rowTop = y,
                            left = left,
                            colNomW = colNomW,
                            colPrenomW = colPrenomW,
                            colStatutW = colStatutW,
                            colNoteW = colNoteW,
                            row = r,
                            isEven = rowIdx % 2 == 1
                        )
                        y += PdfStyles.ROW_HEIGHT
                    }
                }

                // === Grille externe ===
                val gridRight = left + colNomW + colPrenomW + colStatutW + colNoteW
                val renderedCount = if (rows.isEmpty()) 1 else (end - currentIndex)
                val bottomY = startY + renderedCount * PdfStyles.ROW_HEIGHT
                drawOuterGrid(canvas, left, gridRight, tableTop, bottomY, colNomW, colPrenomW, colStatutW)

                // === Pied de page ===
                drawFooter(canvas, pageNumber, left, right, bottom)

                doc.finishPage(page)
                currentIndex = end
                pageNumber++
            }

            FileOutputStream(outFile).use { fos -> doc.writeTo(fos) }
            return outFile
        } finally {
            doc.close()
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        date: LocalDate,
        groupLabel: String,
        left: Float,
        top: Float,
        right: Float
    ) {
        val title = "Cahier de présence"
        val tp = PdfStyles.titlePaint()
        val h2 = PdfStyles.headerPaint()
        val body = PdfStyles.bodyPaint()

        // Titre centré
        val titleW = tp.measureText(title)
        canvas.drawText(title, left + (right - left - titleW) / 2f, top + s(4f) + tp.textSize, tp)

        // Infos date + groupe
        val yInfo = top + tp.textSize + s(10f) + h2.textSize
        val dateHuman = date.format(dateHumanFmt).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() }
        canvas.drawText("Date : $dateHuman", left, yInfo, h2)
        canvas.drawText("Groupe : $groupLabel", left, yInfo + s(18f), body)
    }

    private fun drawTableHeader(
        canvas: Canvas,
        left: Float,
        y: Float,
        colNomW: Float,
        colPrenomW: Float,
        colStatutW: Float,
        colNoteW: Float
    ) {
        // Fond gris
        val headerRect = RectF(
            left,
            y,
            left + colNomW + colPrenomW + colStatutW + colNoteW,
            y + PdfStyles.HEADER_ROW_HEIGHT
        )
        canvas.drawRect(headerRect, PdfStyles.headerFillPaint())

        val p = PdfStyles.boldBodyPaint()
        val base = PdfStyles.centeredBaseline(y, PdfStyles.HEADER_ROW_HEIGHT, p)

        canvas.drawText("Nom",    left + s(8f), base, p)
        canvas.drawText("Prénom", left + colNomW + s(8f), base, p)
        canvas.drawText("Statut", left + colNomW + colPrenomW + s(8f), base, p)
        canvas.drawText("Note",   left + colNomW + colPrenomW + colStatutW + s(8f), base, p)

        // Ligne inférieure épaisse
        canvas.drawLine(
            left,
            y + PdfStyles.HEADER_ROW_HEIGHT,
            left + colNomW + colPrenomW + colStatutW + colNoteW,
            y + PdfStyles.HEADER_ROW_HEIGHT,
            PdfStyles.linePaint()
        )
    }

    private fun drawRow(
        canvas: Canvas,
        rowTop: Float,
        left: Float,
        colNomW: Float,
        colPrenomW: Float,
        colStatutW: Float,
        colNoteW: Float,
        row: PresenceRow,
        isEven: Boolean
    ) {
        // Zebra background
        if (isEven) {
            val zebraRect = RectF(
                left,
                rowTop,
                left + colNomW + colPrenomW + colStatutW + colNoteW,
                rowTop + PdfStyles.ROW_HEIGHT
            )
            canvas.drawRect(zebraRect, PdfStyles.zebraFillPaint())
        }

        val p = PdfStyles.bodyPaint()
        val base = PdfStyles.centeredBaseline(rowTop, PdfStyles.ROW_HEIGHT, p)

        // Colonnes texte
        canvas.drawText(row.nom,    left + s(8f), base, p)
        canvas.drawText(row.prenom, left + colNomW + s(8f), base, p)

        // Statut "chip" (fond coloré + texte)
        val chipText = statusLabel(row.statut)
        val chipPadH = s(8f)
        val chipPadV = s(6f)
        val textW = PdfStyles.statusTextPaint().measureText(chipText)
        val chipLeft = left + colNomW + colPrenomW + s(8f)
        val chipRight = chipLeft + textW + chipPadH * 2
        val chipTop = rowTop + chipPadV
        val chipBottom = rowTop + PdfStyles.ROW_HEIGHT - chipPadV
        val chipRect = RectF(chipLeft, chipTop, chipRight, chipBottom)

        val fill = when (row.statut) {
            PresenceStatus.PRESENT -> PdfStyles.statusFillPaintPresent()
            PresenceStatus.ABSENT  -> PdfStyles.statusFillPaintAbsent()
            PresenceStatus.RETARD  -> PdfStyles.statusFillPaintRetard()
        }
        canvas.drawRoundRect(chipRect, s(8f), s(8f), fill)
        val chipTextBase = PdfStyles.centeredBaseline(chipTop, chipBottom - chipTop, PdfStyles.statusTextPaint())
        canvas.drawText(chipText, chipLeft + chipPadH, chipTextBase, PdfStyles.statusTextPaint())

        // Note (tronquée)
        val noteText = ellipsizeToWidth(row.note.orEmpty(), p, colNoteW - s(16f))
        canvas.drawText(noteText, left + colNomW + colPrenomW + colStatutW + s(8f), base, p)
    }

    private fun drawOuterGrid(
        canvas: Canvas,
        left: Float,
        right: Float,
        headerTop: Float,
        bottomY: Float,
        colNomW: Float,
        colPrenomW: Float,
        colStatutW: Float
    ) {
        val lp = PdfStyles.linePaint()
        // Bordures extérieures
        canvas.drawRect(RectF(left, headerTop, right, bottomY), lp)

        // Colonnes
        val c1 = left + colNomW
        val c2 = c1 + colPrenomW
        val c3 = c2 + colStatutW
        canvas.drawLine(c1, headerTop, c1, bottomY, lp)
        canvas.drawLine(c2, headerTop, c2, bottomY, lp)
        canvas.drawLine(c3, headerTop, c3, bottomY, lp)
        // (lignes horizontales internes non nécessaires avec zebra + lisibilité, mais ajoutables si tu veux)
    }

    private fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        left: Float,
        right: Float,
        bottom: Float
    ) {
        val p = PdfStyles.footerPaint()
        val text = "Page $pageNumber"
        val w = p.measureText(text)
        canvas.drawText(text, right - w, bottom, p)
    }

    private fun statusLabel(s: PresenceStatus): String = when (s) {
        PresenceStatus.PRESENT -> "présent"
        PresenceStatus.ABSENT  -> "absent"
        PresenceStatus.RETARD  -> "retard"
    }

    private fun ellipsizeToWidth(text: String, paint: android.graphics.Paint, maxWidth: Float): String {
        if (text.isBlank()) return ""
        var end = paint.breakText(text, true, maxWidth, null)
        if (end >= text.length) return text
        var candidate = text.substring(0, end).trimEnd()
        val ellipsis = "…"
        while (candidate.isNotEmpty() && paint.measureText("$candidate$ellipsis") > maxWidth) {
            candidate = candidate.dropLast(1)
        }
        return "$candidate$ellipsis"
    }

    private fun sanitizeFileName(input: String): String =
        input.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9._-]+"), "_").trim('_')
}
