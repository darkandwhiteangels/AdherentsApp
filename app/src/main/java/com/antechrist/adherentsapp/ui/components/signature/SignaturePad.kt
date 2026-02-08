package com.antechrist.adherentsapp.ui.components.signature

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

/**
 * État contrôlant le SignaturePad.
 */
class SignaturePadState internal constructor(
    initialStrokeWidthPx: Float,
    initialColor: Color
) {
    internal var viewSizePx: IntSize by mutableStateOf(IntSize.Zero)
    internal val strokes = mutableStateListOf<StrokeData>()
    internal var inProgress: MutableList<Offset>? = null
    internal var strokeWidthPx: Float by mutableFloatStateOf(initialStrokeWidthPx)
    internal var color: Color by mutableStateOf(initialColor)

    fun isEmpty(): Boolean = strokes.isEmpty() && (inProgress?.isEmpty() != false)

    fun clear() {
        strokes.clear()
        inProgress = null
    }

    fun undo() {
        if (inProgress != null && inProgress!!.isNotEmpty()) {
            inProgress = null
            return
        }
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.lastIndex)
        }
    }

    /**
     * Exporte la signature vers un Bitmap ARGB_8888 à fond transparent.
     * @param targetWidthPx largeur cible en pixels (hauteur conservant le ratio)
     */
    fun exportBitmap(targetWidthPx: Int = 1200): Bitmap {
        val vw = viewSizePx.width.coerceAtLeast(1)
        val vh = viewSizePx.height.coerceAtLeast(1)
        val scale = targetWidthPx.toFloat() / vw.toFloat()
        val targetHeightPx = (vh * scale).roundToInt().coerceAtLeast(1)

        val bmp = createBitmap(targetWidthPx, targetHeightPx)
        val canvas = AndroidCanvas(bmp)

        // Peinture des traits terminés
        for (s in strokes) {
            val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = s.color.toArgb()
                style = AndroidPaint.Style.STROKE
                strokeWidth = s.widthPx * scale
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
            }
            val path = AndroidPath()
            val pts = s.points
            if (pts.isNotEmpty()) {
                path.moveTo(pts.first().x * scale, pts.first().y * scale)
                for (i in 1 until pts.size) {
                    val o = pts[i]
                    path.lineTo(o.x * scale, o.y * scale)
                }
                canvas.drawPath(path, p)
            }
        }

        // Trait en cours (si présent) — utiliser une couleur sûre
        inProgress?.let { cur ->
            if (cur.isNotEmpty()) {
                val curStrokeColor = strokes.lastOrNull()?.color ?: Color.Black
                val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = curStrokeColor.toArgb()
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = strokeWidthPx * scale
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                }
                val path = AndroidPath().apply {
                    moveTo(cur.first().x * scale, cur.first().y * scale)
                    for (i in 1 until cur.size) {
                        val o = cur[i]
                        lineTo(o.x * scale, o.y * scale)
                    }
                }
                canvas.drawPath(path, p)
            }
        }
        return bmp
    }
}

@Immutable
data class StrokeData(
    val points: MutableList<Offset>,
    val widthPx: Float,
    val color: Color
)

/**
 * Crée un état mémorisé pour SignaturePad.
 */
@Composable
fun rememberSignaturePadState(
    strokeWidth: Dp = 3.dp,
    color: Color = Color.Black
): SignaturePadState {
    val density = LocalDensity.current
    val widthPx = remember(strokeWidth, density) {
        with(density) { strokeWidth.toPx() }
    }
    return remember { SignaturePadState(widthPx, color) }
}

/**
 * Surface de signature au doigt/stylet. Aucun bouton ici ; la barre d'actions vit dans la feuille.
 */
@Composable
fun SignaturePad(
    state: SignaturePadState,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 3.dp,
    color: Color = Color.Black,
    cornerRadius: Dp = 16.dp,
    showSubtleGrid: Boolean = true
) {
    val density = LocalDensity.current
    val strokePx by rememberUpdatedState(with(density) { strokeWidth.toPx() })
    val colorNow by rememberUpdatedState(color)

    // Synchronise les paramètres dynamiques
    LaunchedEffect(strokePx) { state.strokeWidthPx = strokePx }
    LaunchedEffect(colorNow) { state.color = colorNow }

    val shape = remember { RoundedCornerShape(cornerRadius) }
    var drawCount by remember { mutableIntStateOf(0) } // pour invalider en cas d'undo/clear

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3.2f) // ratio large confortable pour signer
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFDDDDDD), shape)
            .onGloballyPositioned {
                state.viewSizePx = it.size
            }
            .pointerInput(Unit) {
                val tracker = VelocityTracker()
                detectDrag(drawCount, state, tracker) { drawCount++ }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(3.2f)) {
            val w = size.width
            val h = size.height

            // Grille légère optionnelle (repères)
            if (showSubtleGrid) {
                val gridPaint = androidx.compose.ui.graphics.Paint().apply {
                    this.color = Color(0xFFEFEFEF)
                }
                val step = 24f
                var x = 0f
                while (x <= w) {
                    drawLine(
                        color = Color(0xFFF2F2F2),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                    x += step
                }
                var y = 0f
                while (y <= h) {
                    drawLine(
                        color = Color(0xFFF2F2F2),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                    y += step
                }
            }

            // Traits validés
            for (s in state.strokes) {
                if (s.points.size < 2) continue
                val path = Path().apply {
                    moveTo(s.points.first().x, s.points.first().y)
                    for (i in 1 until s.points.size) {
                        val o = s.points[i]
                        lineTo(o.x, o.y)
                    }
                }
                drawPath(
                    path = path,
                    color = s.color,
                    style = Stroke(width = s.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Trait en cours
            state.inProgress?.let { cur ->
                if (cur.size >= 2) {
                    val path = Path().apply {
                        moveTo(cur.first().x, cur.first().y)
                        for (i in 1 until cur.size) {
                            val o = cur[i]
                            lineTo(o.x, o.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = state.color,
                        style = Stroke(width = state.strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

/* -------------------------- Gestures -------------------------- */

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectDrag(
    drawCount: Int,
    state: SignaturePadState,
    tracker: VelocityTracker,
    onChanged: () -> Unit
) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
            val start = down.position
            tracker.resetTracking()
            tracker.addPosition(down.uptimeMillis, start)
            state.inProgress = mutableListOf(start)
            down.consume()

            while (true) {
                val event = awaitPointerEvent()
                val anyUp = event.changes.any { !it.pressed }
                val change = event.changes.firstOrNull() ?: break

                val pos = change.position
                tracker.addPosition(change.uptimeMillis, pos)
                state.inProgress?.add(pos)
                change.consume()

                if (anyUp) {
                    // commit le trait
                    val committed = state.inProgress ?: mutableListOf()
                    if (committed.isNotEmpty()) {
                        state.strokes.add(
                            StrokeData(
                                points = committed.toMutableList(),
                                widthPx = state.strokeWidthPx,
                                color = state.color
                            )
                        )
                    }
                    state.inProgress = null
                    onChanged()
                    break
                }
            }
        }
    }
}
