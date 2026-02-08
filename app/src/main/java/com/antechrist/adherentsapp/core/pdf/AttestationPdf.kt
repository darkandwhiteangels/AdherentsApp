package com.antechrist.adherentsapp.core.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

/**
 * Générateur PDF – mise en page “classique v1” conforme au gabarit :
 * 1 logo + saison, 2 titre, 3 phrase intro, 4/5 deux colonnes, 6/7 cp+ville,
 * 8 phrase scolarité, 9 montant, 10 Fait à … le …, 11 signature prés.
 */
object AttestationPdf {

    data class ClubProfile(
        val presidentName: String = "Le/La Président(e)",
        val secretaryName: String? = null,
        val clubName: String = "Club",
        val clubCity: String = "Ville",
        val logoBitmap: Bitmap? = null,
        val presidentSignatureBitmap: Bitmap? = null   // 👈 nouveau : image de signature
    )

    data class Person(
        val nom: String,
        val prenom: String,
        val dateNaissance: String?, // dd/MM/yyyy
        val adresse: String?,
        val codePostal: String?,
        val ville: String?
    )

    // ---------- API publique ----------

    fun generateForMember(
        context: Context,
        seasonKey: String,
        club: ClubProfile,
        person: Person,
        amountCents: Long
    ): File {
        val file = File(
            context.cacheDir,
            "attestation_${seasonKey}_${sanitize("${person.prenom}_${person.nom}")}_${System.currentTimeMillis()}.pdf"
        )

        val doc = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
            val page = doc.startPage(pageInfo)

            drawClassicV1(
                canvas = page.canvas,
                seasonKey = seasonKey,
                club = club,
                person = person,
                totalAmountCents = amountCents
            )

            doc.finishPage(page)

            FileOutputStream(file).use { out ->
                doc.writeTo(out)
            }
        } finally {
            doc.close()
        }
        return file
    }

    fun generateForHousehold(
        context: Context,
        seasonKey: String,
        club: ClubProfile,
        householdDisplayName: String,
        postalAddress: String?,
        totalAmountCents: Long
    ): File {
        val file = File(
            context.cacheDir,
            "attestation_${seasonKey}_${sanitize(householdDisplayName)}_${System.currentTimeMillis()}.pdf"
        )

        val doc = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
            val page = doc.startPage(pageInfo)

            val person = Person(
                nom = householdDisplayName,
                prenom = "",
                dateNaissance = null,
                adresse = postalAddress,
                codePostal = null,
                ville = null
            )

            drawClassicV1(
                canvas = page.canvas,
                seasonKey = seasonKey,
                club = club,
                person = person,
                totalAmountCents = totalAmountCents,
                isHousehold = true
            )

            doc.finishPage(page)

            FileOutputStream(file).use { out ->
                doc.writeTo(out)
            }
        } finally {
            doc.close()
        }
        return file
    }

    // ---------- Mise en page “classic v1” ----------

    private fun drawClassicV1(
        canvas: Canvas,
        seasonKey: String,
        club: ClubProfile,
        person: Person,
        totalAmountCents: Long,
        isHousehold: Boolean = false
    ) {
        // Grille
        val margin = 36f // 0.5"
        val contentLeft = margin
        val contentRight = A4_WIDTH - margin
        val contentTop = margin
        val contentWidth = contentRight - contentLeft

        // Peintures
        val black = Color.BLACK
        val gray = Color.rgb(80, 80, 80)

        val h1 = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = black
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 26f
        }
        val h2 = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = black
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 18f
        }
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = black
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textSize = 12.5f
        }
        val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textSize = 11.5f
        }
        val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textSize = 10f
        }
        val bold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = black
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 12.5f
        }

        var y = contentTop

        // 1) Logo + saison en haut gauche
        run {
            val logoBoxSize = 110f
            val logoTop = y
            val logoLeft = contentLeft
            drawLogoWithSeason(
                canvas = canvas,
                x = logoLeft,
                y = logoTop,
                boxSize = logoBoxSize,
                seasonKey = seasonKey,
                bmp = club.logoBitmap
            )

            // 2) Titre en haut à droite de la zone logo
            val titleX = logoLeft + logoBoxSize + 16f
            val titleWidth = contentRight - titleX
            y = logoTop + 10f
            drawParagraph(
                canvas = canvas,
                text = "ATTESTATION\nDE PAIEMENT",
                paint = h1,
                x = titleX,
                top = y,
                width = titleWidth,
                align = Layout.Alignment.ALIGN_NORMAL // aligner à gauche de cette colonne
            )
            y = logoTop + logoBoxSize + 18f
        }

        // 3) Phrase d’intro centrée
        run {
            val president = club.presidentName.ifBlank { "Le/La Président(e)" }
            val clubName = club.clubName.ifBlank { "Club" }
            val intro = "Je, soussigné $president, président de l'association $clubName, certifie que :"
            y += drawParagraphCentered(canvas, intro, p, contentLeft, y, contentWidth) + 18f
        }

        // 4/5) Deux colonnes : labels / valeurs
        run {
            val colGap = 14f
            val colLeftW = min(180f, contentWidth * 0.35f)
            val colRightW = contentWidth - colLeftW - colGap

            fun row(labelText: String, valueText: String, topY: Float): Float {
                val rowHeight = maxOf(
                    measureParagraphHeight(labelText, label, colLeftW),
                    measureParagraphHeight(valueText, p, colRightW)
                )
                drawParagraph(canvas, labelText, label, contentLeft, topY, colLeftW, Layout.Alignment.ALIGN_NORMAL)
                drawParagraph(canvas, valueText, p, contentLeft + colLeftW + colGap, topY, colRightW, Layout.Alignment.ALIGN_NORMAL)
                return rowHeight + 8f
            }

            val nom = (person.nom).ifBlank { "—" }
            val prenom = (person.prenom).ifBlank { "—" }
            val dn = person.dateNaissance ?: "—"
            val adresse = person.adresse ?: "—"

            y += row("Nom", nom, y)
            y += row("Prénom", prenom, y)
            y += row("Date de naissance", dn, y)
            y += row("Adresse complète", adresse, y)

            // 6/7) Code postal — Ville (même ligne)
            val cpLabel = "Code postal :"
            val villeLabel = "Ville :"
            val cp = person.codePostal ?: "—"
            val ville = person.ville ?: "—"

            val cpLabelW = label.measureText(cpLabel)
            val villeLabelW = label.measureText(villeLabel)
            val cpX = contentLeft
            val cpValueX = cpX + cpLabelW + 6f
            val villeX = contentLeft + colLeftW + colGap + colRightW * 0.5f
            val villeValueX = villeX + villeLabelW + 6f

            // ligne − on aligne sur baseline
            val baseline = y + p.textSize
            canvas.drawText(cpLabel, cpX, baseline, label)
            canvas.drawText(cp, cpValueX, baseline, p)
            canvas.drawText(villeLabel, villeX, baseline, label)
            canvas.drawText(ville, villeValueX, baseline, p)

            y = baseline + 18f
        }

        // 8) Phrase scolarité centrée
        run {
            val txt = "est inscrit(e) dans notre club et suit les cours dispensés par nos professeurs."
            y += drawParagraphCentered(canvas, txt, p, contentLeft, y, contentWidth) + 10f
        }

        // 9) Montant centré (montant en gras)
        run {
            val euro = NumberFormat.getCurrencyInstance(Locale.FRANCE)
            val amount = euro.format(totalAmountCents / 100.0)
            val left = "Le coût total de son inscription s'élève à "
            val right = " tous frais compris."

            // On compose “left [amount] right” en centrant le tout
            val amountW = bold.measureText(amount)
            val leftW = p.measureText(left)
            val rightW = p.measureText(right)
            val totalW = leftW + amountW + rightW

            val startX = contentLeft + (contentWidth - totalW) / 2f
            val base = y + p.textSize

            canvas.drawText(left, startX, base, p)
            canvas.drawText(amount, startX + leftW, base, bold)
            canvas.drawText(right, startX + leftW + amountW, base, p)

            y = base + 18f
        }

        // 10) Fait à [ville], le [date]
        run {
            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val city = club.clubCity.ifBlank { "Ville" }
            val txt = "Fait à $city, le $date"
            y += drawParagraphCentered(canvas, txt, p, contentLeft, y, contentWidth) + 40f
        }

        // 11) Signature président en bas droite (image + nom / ligne)
        run {
            val sigTop = A4_HEIGHT - margin - 110f
            val sigLeft = contentRight - 260f
            val sigWidth = 240f

            // image de signature (si présente)
            val usedH = drawSignatureBitmap(
                canvas = canvas,
                bmp = club.presidentSignatureBitmap,
                x = sigLeft,
                y = sigTop,
                maxW = sigWidth,
                maxH = 70f
            )

            // ligne positionnée sous l'image (ou zone)
            val lineY = sigTop + usedH + 6f
            canvas.drawLine(
                sigLeft, lineY, sigLeft + sigWidth, lineY,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = black
                    strokeWidth = 1.2f
                }
            )

            val pres = club.presidentName.ifBlank { "Le/La Président(e)" }
            val caption = "Signature du président\n$pres"
            drawParagraph(
                canvas = canvas,
                text = caption,
                paint = small,
                x = sigLeft,
                top = lineY + 6f,
                width = sigWidth,
                align = Layout.Alignment.ALIGN_CENTER
            )
        }

        // Pied de page discret
        val footer = "Ce document fait valoir ce que de droit."
        val fw = small.measureText(footer)
        canvas.drawText(
            footer,
            contentRight - fw,
            A4_HEIGHT - margin / 2f,
            small
        )
    }

    // ---------- Helpers dessin ----------

    private fun drawLogoWithSeason(
        canvas: Canvas,
        x: Float,
        y: Float,
        boxSize: Float,
        seasonKey: String,
        bmp: Bitmap?
    ) {
        val rect = RectF(x, y, x + boxSize, y + boxSize)

        if (bmp != null) {
            val srcRatio = bmp.width.toFloat() / bmp.height
            val dstRatio = rect.width() / rect.height()
            val dst = if (srcRatio >= dstRatio) {
                val h = rect.width() / srcRatio
                RectF(rect.left, rect.top + (rect.height() - h) / 2f, rect.right, rect.top + (rect.height() + h) / 2f)
            } else {
                val w = rect.height() * srcRatio
                RectF(rect.left + (rect.width() - w) / 2f, rect.top, rect.left + (rect.width() + w) / 2f, rect.bottom)
            }
            canvas.drawBitmap(bmp, null, dst, null)
        } else {
            // Fallback typographique : cercle + sigle club
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.color = Color.BLACK
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            canvas.drawOval(rect, p)

            p.style = Paint.Style.FILL
            p.color = Color.rgb(230, 230, 230)
            val inner = RectF(rect.left + 6f, rect.top + 6f, rect.right - 6f, rect.bottom - 6f)
            canvas.drawOval(inner, p)

            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val txt = "OKES"
            val tw = tp.measureText(txt)
            canvas.drawText(txt, inner.centerX() - tw / 2f, inner.centerY() + tp.textSize / 3f, tp)
        }

        // Saison superposée (ex: “25/26”)
        val season = seasonShort(seasonKey)
        val tag = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val pad = 6f
        val tw = tag.measureText(season)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(160, 0, 0, 0) }
        val tx = rect.left + 8f
        val ty = rect.bottom - 10f
        canvas.drawRoundRect(RectF(tx - pad, ty - tag.textSize - pad / 2f, tx + tw + pad, ty + pad), 6f, 6f, bg)
        canvas.drawText(season, tx, ty, tag)
    }

    /**
     * Dessine la signature alignée à droite de la zone [maxW x maxH].
     * @return la hauteur utilisée (pour positionner la ligne/caption ensuite).
     */
    private fun drawSignatureBitmap(
        canvas: Canvas,
        bmp: Bitmap?,
        x: Float,
        y: Float,
        maxW: Float,
        maxH: Float
    ): Float {
        if (bmp == null) return 0f
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        if (w <= 0f || h <= 0f) return 0f

        val scale = min(maxW / w, maxH / h)
        val dw = w * scale
        val dh = h * scale

        // alignée à droite dans la zone
        val left = x + (maxW - dw)
        val rect = RectF(left, y, left + dw, y + dh)
        canvas.drawBitmap(bmp, null, rect, null)
        return dh
    }

    private fun drawParagraphCentered(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        left: Float,
        top: Float,
        width: Float
    ): Float {
        return drawParagraph(canvas, text, paint, left, top, width, Layout.Alignment.ALIGN_CENTER)
    }

    private fun drawParagraph(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        top: Float,
        width: Float,
        align: Layout.Alignment
    ): Float {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.toInt())
            .setAlignment(align)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x, top)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    private fun measureParagraphHeight(text: String, paint: TextPaint, width: Float): Float {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
        return layout.height.toFloat()
    }

    private fun seasonShort(key: String): String {
        // "2025-2026" -> "25/26"
        val parts = key.split("-")
        return if (parts.size == 2 && parts[0].length >= 4 && parts[1].length >= 4) {
            "${parts[0].takeLast(2)}/${parts[1].takeLast(2)}"
        } else key
    }

    private fun sanitize(s: String): String =
        s.lowercase(Locale.ROOT).replace("[^a-z0-9._-]+".toRegex(), "_")

    // ---------- Constantes ----------
    private const val A4_WIDTH = 595 // points (72 dpi)
    private const val A4_HEIGHT = 842
}
