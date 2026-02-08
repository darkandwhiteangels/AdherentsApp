package com.antechrist.adherentsapp.ui.utils

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Styles & mesures pour PDF. On applique un SCALE pour augmenter la
 * définition effective du rendu (PdfDocument = 72 dpi).
 */
object PdfStyles {
    // ====== Échelle globale (2x = lisible/nerveux ; 3x si vous imprimez souvent) ======
    const val SCALE = 2f

    // A4 @72dpi de base (points), puis * SCALE
    val PAGE_WIDTH  = 595f * SCALE
    val PAGE_HEIGHT = 842f * SCALE

    // Marges
    val MARGIN_H = 32f * SCALE
    val MARGIN_V = 32f * SCALE

    // Tailles texte (après SCALE)
    private val HEADER_TEXT_SIZE = 18f * SCALE
    private val TITLE_TEXT_SIZE  = 22f * SCALE
    private val BODY_TEXT_SIZE   = 13f * SCALE
    private val FOOTER_TEXT_SIZE = 10f * SCALE

    // Hauteurs de lignes
    val HEADER_ROW_HEIGHT = 28f * SCALE
    val ROW_HEIGHT        = 26f * SCALE

    // Utilitaires
    fun s(v: Float) = v * SCALE

    // ========== Paints ==========
    fun titlePaint() = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = TITLE_TEXT_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun headerPaint() = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = HEADER_TEXT_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun bodyPaint() = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = BODY_TEXT_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    fun boldBodyPaint() = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = BODY_TEXT_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun footerPaint() = Paint().apply {
        isAntiAlias = true
        color = Color.DKGRAY
        textSize = FOOTER_TEXT_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }

    fun linePaint() = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        strokeWidth = 1.2f * SCALE
        style = Paint.Style.STROKE
    }

    fun headerFillPaint() = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(235, 235, 235)
        style = Paint.Style.FILL
    }

    fun zebraFillPaint() = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(248, 248, 248)
        style = Paint.Style.FILL
    }

    fun statusFillPaintPresent() = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(212, 245, 214) // vert pâle
        style = Paint.Style.FILL
    }
    fun statusFillPaintAbsent() = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(255, 220, 220) // rouge pâle
        style = Paint.Style.FILL
    }
    fun statusFillPaintRetard() = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(255, 242, 204) // jaune pâle
        style = Paint.Style.FILL
    }

    fun statusTextPaint() = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = BODY_TEXT_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Calcul du baseline pour centrer verticalement un texte dans une cellule (top + height)
    fun centeredBaseline(top: Float, height: Float, p: Paint): Float {
        val textHeight = p.descent() - p.ascent()
        return top + (height - textHeight) / 2f - p.ascent()
    }
}
