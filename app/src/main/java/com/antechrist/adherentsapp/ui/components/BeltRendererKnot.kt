package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.ui.grade.BeltCatalog
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.min

/**
 * Renderer "nœud" basé sur un path SVG (attribut d=).
 * - Remplissage EvenOdd
 * - Motif bi-couleur (argyle) + barrettes sur les pans
 * - Insigne japonais 1–5 Dan placé **entre les deux pans** (sous le nœud), en doré.
 *
 * Recommandé pour ≥ ~80dp (ex. header 112dp).
 */
@Composable
fun BeltRendererKnot(
    pathData: String,
    beltCode: String?,
    stripeCount: Int,
    modifier: Modifier = Modifier,
    showOutline: Boolean = true,
    contentDescriptionOverride: String? = null,
    // Motif
    patternCellDp: Dp? = null,
    patternScale: Float = 1f,
    patternMinDp: Dp = 4.dp,
    patternMaxOfHeight: Float = 0.33f,
    stripeAngleDeg: Float = 140f,
    // Insigne (1–5 Dan) entre pans (zone basse centrale)
    showInsignia: Boolean = true,
    insigniaRotationDeg: Float = 0f,
    // Réglage de la fenêtre "entre pans"
    betweenPansTopFraction: Float = -0.12f,   // démarre sous le nœud
    betweenPansBottomFraction: Float = 0.94f,// s'arrête avant le bord bas
    betweenPansWidthFraction: Float = 0.18f, // bande étroite au centre
    insigniaHeightFraction: Float = 0.20f    // taille du texte vs hauteur totale du shape
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val neutral = MaterialTheme.colorScheme.surfaceVariant

    val spec = BeltCatalog.Karate.specOf(beltCode)
    val primary = spec?.primary ?: neutral
    val isBi = BeltCatalog.Karate.isBiColor(beltCode)
    val secondary = if (isBi) spec?.secondary else null

    val stripesClamped = BeltCatalog.Karate.clampStripeCount(beltCode, stripeCount)
    val stripeColor = BeltCatalog.Karate.stripeColor(beltCode)
    val liseresCount = BeltCatalog.Karate.liseresCount(beltCode)

    // Insigne JP (1–5 Dan)
    val insignia = BeltCatalog.Karate.insigniaJp(beltCode)
    val insigniaColor = BeltCatalog.Karate.insigniaColor(beltCode) ?: Color(0xFFFFD700)

    val a11y = contentDescriptionOverride ?: BeltCatalog.a11yLabel(beltCode, stripesClamped)
    val layoutDir = LocalLayoutDirection.current

    Box(modifier = modifier.semantics { contentDescription = a11y }) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1) Path complet + EvenOdd
            val raw = PathParser().parsePathString(pathData).toPath().apply {
                fillType = PathFillType.EvenOdd
            }
            val rb = raw.getBounds()
            if (rb.width <= 0f || rb.height <= 0f) return@Canvas

            // 2) Fit & centrage
            val pad = min(w, h) * 0.04f
            val target = Rect(pad, pad, w - pad, h - pad)
            val s = min(target.width / rb.width, target.height / rb.height)
            val m = Matrix().apply {
                translate(-rb.left, -rb.top)
                scale(s, s)
                val newW = rb.width * s
                val newH = rb.height * s
                translate(
                    target.left + (target.width - newW) / 2f,
                    target.top + (target.height - newH) / 2f
                )
            }
            val shape = Path().apply {
                addPath(raw); transform(m); fillType = PathFillType.EvenOdd
            }
            val sb = shape.getBounds()

            fun lighten(c: Color, f: Float) = Color(
                (c.red + (1f - c.red) * f).coerceIn(0f, 1f),
                (c.green + (1f - c.green) * f).coerceIn(0f, 1f),
                (c.blue + (1f - c.blue) * f).coerceIn(0f, 1f),
                c.alpha
            )
            fun darken(c: Color, f: Float) = Color(
                (c.red * (1f - f)).coerceIn(0f, 1f),
                (c.green * (1f - f)).coerceIn(0f, 1f),
                (c.blue * (1f - f)).coerceIn(0f, 1f),
                c.alpha
            )

            // Helper: motif argyle couvrant
            fun DrawScope.drawArgyleCovering(bounds: Rect, cellSize: Float, a: Color, b: Color) {
                val cx = bounds.center.x
                val cy = bounds.center.y
                val side = hypot(bounds.width, bounds.height) * 1.2f
                val cols = ceil(side / cellSize).toInt() + 2
                val rows = ceil(side / cellSize).toInt() + 2
                val startX = cx - side / 2f
                val startY = cy - side / 2f
                withTransform({ rotate(45f, Offset(cx, cy)) }) {
                    for (iy in 0..rows) for (ix in 0..cols) {
                        val color = if ((ix + iy) % 2 == 0) a else b
                        drawRect(
                            color = color,
                            topLeft = Offset(startX + ix * cellSize, startY + iy * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }

            // 3) Remplissage (fond + motif si bi-couleur)
            val textilePrimary = Brush.verticalGradient(
                listOf(lighten(primary, .08f), primary, darken(primary, .10f)),
                startY = sb.top, endY = sb.bottom
            )
            clipPath(shape) {
                drawPath(shape, brush = textilePrimary, style = Fill)
                if (isBi && secondary != null) {
                    val rowsTarget = 9f
                    val colsTarget = 12f
                    val auto = min(sb.height / rowsTarget, sb.width / colsTarget) * patternScale
                    val minPx = patternMinDp.toPx()
                    val maxPx = (sb.height * patternMaxOfHeight).coerceAtLeast(minPx * 2f)
                    val cell = (patternCellDp?.toPx() ?: auto).coerceIn(minPx, maxPx)
                    drawArgyleCovering(
                        bounds = sb,
                        cellSize = cell,
                        a = lighten(primary, 0.02f),
                        b = lighten(secondary, 0.02f)
                    )
                }
            }

            // 4) Liserés (blanche 2 liserés)
            if (liseresCount > 0) {
                val lisereH = (h * 0.06f).coerceAtLeast(1.dp.toPx())
                val yInset = sb.height * 0.22f
                val y1 = sb.top + yInset
                val y2 = sb.bottom - yInset
                val lisereColor = Color(0xFFD32F2F)
                clipPath(shape) {
                    drawRect(
                        color = lisereColor,
                        topLeft = Offset(sb.left + sb.width * 0.05f, y1 - lisereH / 2f),
                        size = Size(sb.width * 0.90f, lisereH)
                    )
                    if (liseresCount >= 2) {
                        drawRect(
                            color = lisereColor,
                            topLeft = Offset(sb.left + sb.width * 0.05f, y2 - lisereH / 2f),
                            size = Size(sb.width * 0.90f, lisereH)
                        )
                    }
                }
            }

            // 5) Barrettes (sur un pan)
            if (stripesClamped > 0) {
                val onRight = (layoutDir == LayoutDirection.Ltr)
                val anchorW = sb.width * 0.24f
                val anchorH = sb.height * 0.55f
                val anchorTop = sb.bottom - anchorH - (sb.height * 0.04f)
                val anchorLeft = if (onRight)
                    sb.right - anchorW - (sb.width * 0.02f)
                else
                    sb.left + (sb.width * 0.02f)

                val barW = (anchorW * 0.22f).coerceAtLeast(2.dp.toPx())
                val barH = anchorH * 0.85f
                val gap = barW * 0.70f
                val startX = if (onRight) anchorLeft + anchorW - barW else anchorLeft
                val topY = anchorTop + (anchorH - barH) / 2f
                val angle = if (onRight) -stripeAngleDeg else stripeAngleDeg

                repeat(stripesClamped) { i ->
                    val x = if (onRight) startX - i * (barW + gap) else startX + i * (barW + gap)
                    val cx = x + barW / 2f
                    val cy = topY + barH / 2f
                    val rr = RoundRect(
                        left = x, top = topY, right = x + barW, bottom = topY + barH,
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                    clipPath(shape) {
                        withTransform({ rotate(angle, Offset(cx, cy)) }) {
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.10f),
                                topLeft = Offset(rr.left, rr.top + 1.dp.toPx()),
                                size = Size(rr.width, rr.height),
                                cornerRadius = rr.topLeftCornerRadius
                            )
                            drawRoundRect(
                                color = stripeColor,
                                topLeft = Offset(rr.left, rr.top),
                                size = Size(rr.width, rr.height),
                                cornerRadius = rr.topLeftCornerRadius
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.18f),
                                topLeft = Offset(rr.left, rr.top),
                                size = Size(rr.width, rr.height),
                                cornerRadius = rr.topLeftCornerRadius,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
            }

            // 6) Insigne japonais (1–5 Dan) ENTRE LES PANS (zone basse, centrée)
            if (showInsignia && !insignia.isNullOrBlank()) {
                // Fenêtre "entre pans" (pas de clip → le texte reste visible dans l'entre-deux)
                val topY = sb.top + sb.height * betweenPansTopFraction
                val bottomY = sb.top + sb.height * betweenPansBottomFraction
                val gapH = (bottomY - topY).coerceAtLeast(8f)
                val gapW = (sb.width * betweenPansWidthFraction).coerceAtLeast(8f)
                val cx = sb.center.x
                val cy = (topY + bottomY) / 2f
                val rect = Rect(cx - gapW / 2f, topY, cx + gapW / 2f, bottomY)

                withTransform({
                    rotate(insigniaRotationDeg, Offset(rect.center.x, rect.center.y))
                }) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.argb(
                                (insigniaColor.alpha * 255).toInt(),
                                (insigniaColor.red * 255).toInt(),
                                (insigniaColor.green * 255).toInt(),
                                (insigniaColor.blue * 255).toInt()
                            )
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = (sb.height * insigniaHeightFraction)
                                .coerceIn(10f, gapH * 0.9f) // borne par la fenêtre
                            setShadowLayer(
                                textSize * 0.08f,
                                0f, 0f,
                                android.graphics.Color.argb(100, 0, 0, 0)
                            )
                            // gras pour lisibilité ; on pourra brancher une font jap si tu en ajoutes une
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        drawText(
                            insignia!!,
                            rect.center.x,
                            rect.center.y + (paint.textSize * 0.36f), // compensation visuelle
                            paint
                        )
                    }
                }
            }

            // 7) Outline
            if (showOutline) {
                drawPath(shape, color = outline.copy(alpha = 0.75f), style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}
