package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.ui.grade.BeltCatalog

/**
 * Rendu Canvas d’une ceinture (v1 : Karaté).
 *
 * Recommandations d’usage :
 * - Header (remplacement avatar overlap) : par ex. Modifier.height(112.dp).fillMaxWidth()
 * - Mini liste : Modifier.height(24.dp) (ou 28/32.dp) + largeur ~2.4x la hauteur
 *
 * Le composant s’adapte au taille via [modifier].
 */
@Composable
fun BeltRenderer(
    beltCode: String?,
    stripeCount: Int?,
    modifier: Modifier = Modifier,
    /** Bordure M3 autour de la ceinture. */
    showOutline: Boolean = true,
    /** Petite ombre peinte sous la ceinture (subtile). */
    showShadow: Boolean = true,
    /** Rayon des coins ; par défaut = pill (moitié de la hauteur). */
    cornerRadius: Dp? = null,
    /** Description accessibilité ; par défaut générée via le catalog. */
    contentDescriptionOverride: String? = null,
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val neutral = MaterialTheme.colorScheme.surfaceVariant

    // Récupération des specs (uniquement karaté pour v1)
    val spec = remember(beltCode) {
        when {
            beltCode?.startsWith("${BeltCatalog.Arts.KARATE}:") == true ->
                BeltCatalog.Karate.specOf(beltCode)
            else -> null
        }
    }

    // Couleurs principales
    val primary = spec?.primary ?: neutral
    // ATTENTION: ne pas confondre "secondary" (couleur supérieure / 2e couleur)
    // avec le "bi-couleur". On doit détecter explicitement les grades bi-couleur karaté.
    val isBiColor = remember(beltCode) {
        when (beltCode) {
            BeltCatalog.Karate.Codes.BLANCHE_JAUNE,
            BeltCatalog.Karate.Codes.JAUNE_ORANGE,
            BeltCatalog.Karate.Codes.ORANGE_VERT,
            BeltCatalog.Karate.Codes.VERT_BLEU,
            BeltCatalog.Karate.Codes.BLEU_MARRON -> true
            else -> false
        }
    }
    val secondaryForBi = if (isBiColor) spec?.secondary else null

    // Liserés horizontaux (blanche_2liseres => 2)
    val liseresCount = remember(beltCode) { BeltCatalog.Karate.liseresCount(beltCode) }

    // Barrettes (verticales) autorisées jusqu’à vert-bleue inclus
    val stripesClamped = remember(beltCode, stripeCount) {
        BeltCatalog.Karate.clampStripeCount(beltCode, stripeCount)
    }
    val stripeColor = remember(beltCode) { BeltCatalog.Karate.stripeColor(beltCode) }

    // Accessibilité
    val a11yText = contentDescriptionOverride
        ?: remember(beltCode, stripesClamped) { BeltCatalog.a11yLabel(beltCode, stripesClamped) }

    val layoutDir = LocalLayoutDirection.current

    Box(
        modifier = modifier
            .semantics { contentDescription = a11yText }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Coin arrondi par défaut : pill (moitié de la hauteur)
            val r = (cornerRadius?.toPx() ?: (h / 2f)).coerceAtMost(h / 2f)

            // Ombre douce sous la ceinture (optionnelle)
            if (showShadow) {
                val shadowAlpha = 0.08f
                val shadowOffset = 2.dp.toPx()
                drawRoundRect(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    topLeft = Offset(0f, shadowOffset),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(r, r)
                )
            }

            // Fond principal (mono ou bi-couleur, split 50/50)
            if (isBiColor && secondaryForBi != null) {
                val halfW = w / 2f
                // gauche = primary
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(0f, 0f),
                    size = Size(halfW, h),
                    cornerRadius = CornerRadius(r, r)
                )
                // droite = secondary
                drawRoundRect(
                    color = secondaryForBi,
                    topLeft = Offset(halfW, 0f),
                    size = Size(halfW, h),
                    cornerRadius = CornerRadius(r, r)
                )
                // couture centrale (subtile)
                drawLine(
                    color = outlineColor.copy(alpha = 0.35f),
                    start = Offset(halfW, h * 0.08f),
                    end = Offset(halfW, h * 0.92f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                )
            } else {
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(r, r),
                    style = Fill
                )
            }

            // Liserés horizontaux (seulement sur blanche_2liseres)
            if (liseresCount > 0) {
                val lisereHeight = (h * 0.10f).coerceAtLeast(1.dp.toPx())
                val insetY = h * 0.18f
                val gap = h - 2 * insetY - lisereHeight
                val topY = insetY
                val bottomY = insetY + gap

                val lisereColor = outlineColor.copy(alpha = 0.85f)

                // Lisere 1 (haut)
                drawRoundRect(
                    color = lisereColor,
                    topLeft = Offset(0f, topY),
                    size = Size(w, lisereHeight),
                    cornerRadius = CornerRadius(lisereHeight / 2f, lisereHeight / 2f)
                )
                if (liseresCount >= 2) {
                    // Lisere 2 (bas)
                    drawRoundRect(
                        color = lisereColor,
                        topLeft = Offset(0f, bottomY),
                        size = Size(w, lisereHeight),
                        cornerRadius = CornerRadius(lisereHeight / 2f, lisereHeight / 2f)
                    )
                }
            }

            // Barrettes verticales (0..3) à l’extrémité "libre" de la ceinture.
            if (stripesClamped > 0) {
                val barW = (h * 0.16f).coerceAtLeast(2.dp.toPx())
                val barH = h * 0.72f
                val gap = h * 0.10f
                val topY = (h - barH) / 2f

                // Position de départ côté extrémité (droite en LTR, gauche en RTL)
                val startXBase = if (layoutDir == LayoutDirection.Ltr) {
                    w - (gap + barW) // première barrette collée à droite avec marge
                } else {
                    gap // en RTL on dessine à gauche
                }

                repeat(stripesClamped) { i ->
                    val offset = i * (barW + (gap * 0.6f))
                    val x = if (layoutDir == LayoutDirection.Ltr) {
                        (startXBase - offset)
                    } else {
                        (startXBase + offset)
                    }

                    drawRoundRect(
                        color = stripeColor,
                        topLeft = Offset(x, topY),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )

                    // petit liseré sombre pour relief
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.10f),
                        topLeft = Offset(x, topY),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // Bordure générale
            if (showOutline) {
                drawRoundRect(
                    color = outlineColor.copy(alpha = 0.55f),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}
