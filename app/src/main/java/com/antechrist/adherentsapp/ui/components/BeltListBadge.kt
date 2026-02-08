package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.antechrist.adherentsapp.ui.grade.BeltCatalog

/**
 * Mini-ceinture en barre (24–32dp).
 * Bi-couleur : 2 demi-barres jointives, coins arrondis uniquement à l'extérieur.
 *
 * showInsignia: si true, superpose l’insigne japonais (1–5 Dan) au centre en doré.
 */
@Composable
fun BeltListBadge(
    beltCode: String?,
    stripeCount: Int?,
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
    showOutline: Boolean = true,
    cornerRadius: Dp = 6.dp,
    showInsignia: Boolean = false,       // ✅ nouvel argument
) {
    val width = remember(height) { (height * 2.5f) }

    val spec = remember(beltCode) {
        when {
            beltCode?.startsWith("${BeltCatalog.Arts.KARATE}:") == true ->
                BeltCatalog.Karate.specOf(beltCode)
            else -> null
        }
    }
    val cs = MaterialTheme.colorScheme
    val base = cs.surfaceVariant
    val outline = cs.outlineVariant
    val primary = spec?.primary ?: base
    val secondary = spec?.secondary

    // Seules les demi-ceintures sont bi-couleur à l’affichage (+ 6e Dan rouge/blanc).
    val isBi = remember(beltCode) { BeltCatalog.Karate.isBiColor(beltCode) }

    val stripes = remember(beltCode, stripeCount) {
        BeltCatalog.Karate.clampStripeCount(beltCode, stripeCount)
    }
    val stripeColor = remember(beltCode) { BeltCatalog.Karate.stripeColor(beltCode) }
    val liseres = remember(beltCode) { BeltCatalog.Karate.liseresCount(beltCode) }

    // Insigne pour 1–5 Dan
    val insignia = remember(beltCode) { BeltCatalog.Karate.insigniaJp(beltCode) }
    val insigniaColor = remember(beltCode) {
        // Doré par défaut, surcharge possible plus tard via thème si besoin
        BeltCatalog.Karate.insigniaColor(beltCode) ?: Color(0xFFFFD700)
    }

    val a11y = remember(beltCode, stripes) { BeltCatalog.a11yLabel(beltCode, stripes) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .height(height)
            .width(width)
            .semantics { contentDescription = a11y }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = cornerRadius.toPx().coerceIn(0f, h / 2f)

            fun drawRoundedHalf(color: Color, left: Float, right: Float, roundLeft: Boolean, roundRight: Boolean) {
                clipRect(left = left, top = 0f, right = right, bottom = h) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, 0f),
                        size = size,
                        cornerRadius = CornerRadius(r, r)
                    )
                }
            }

            if (isBi && secondary != null) {
                drawRoundedHalf(primary, 0f, w / 2f, roundLeft = true,  roundRight = false)
                drawRoundedHalf(secondary, w / 2f, w, roundLeft = false, roundRight = true)
            } else {
                drawRoundRect(
                    color = primary,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(r, r)
                )
            }

            // Liserés (blanche à 2 liserés)
            if (liseres > 0) {
                val lisereH = (h * 0.12f).coerceAtLeast(1.dp.toPx())
                val insetY = h * 0.24f
                val lisereColor = Color(0xFFD32F2F)   // rouge sobre
                drawRoundRect(lisereColor, Offset(0f, insetY), Size(w, lisereH), CornerRadius(lisereH/2, lisereH/2))
                if (liseres >= 2) {
                    drawRoundRect(lisereColor, Offset(0f, h - insetY - lisereH), Size(w, lisereH), CornerRadius(lisereH/2, lisereH/2))
                }
            }

            // Barrettes (1–3) à droite
            if (stripes > 0) {
                val margin = h * 0.12f
                val barW = (h * 0.18f).coerceAtLeast(2f)
                val barH = h * 0.70f
                val gap = barW * 0.6f
                val startX = w - margin - barW
                val topY = (h - barH) / 2f
                repeat(stripes) { i ->
                    val x = startX - i * (barW + gap)
                    drawRoundRect(Color.Black.copy(alpha = 0.10f), Offset(x, topY + 1.dp.toPx()), Size(barW, barH), CornerRadius(barW/2, barW/2))
                    drawRoundRect(stripeColor, Offset(x, topY), Size(barW, barH), CornerRadius(barW/2, barW/2))
                }
            }

            if (showOutline) {
                drawRoundRect(
                    color = outline.copy(alpha = 0.55f),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // ✅ Insigne japonais (1–5 Dan) superposé au centre si demandé
        if (showInsignia && !insignia.isNullOrBlank()) {
            val fontSp = with(density) { (height * 0.52f).toSp() } // ~50% de la hauteur
            Text(
                text = insignia,
                color = insigniaColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                style = TextStyle(
                    fontSize = fontSp,
                ),
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}
