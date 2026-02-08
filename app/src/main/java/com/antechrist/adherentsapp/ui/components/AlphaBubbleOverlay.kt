package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Affiche une bulle 72dp à la position verticale correspondant à [fraction] (0f..1f),
 * décalée du bord droit de [rightPadding].
 */
@Composable
fun AlphaBubbleOverlay(
    current: Char?,
    fraction: Float?,     // 0f (haut) .. 1f (bas)
    rightPadding: Dp = 40.dp
) {
    var parentW by remember { mutableIntStateOf(0) }
    var parentH by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bubbleSize = 72.dp

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                parentW = coords.size.width
                parentH = coords.size.height
            }
    ) {
        if (current != null && fraction != null && parentH > 0 && parentW > 0) {
            val xPx = with(density) { (parentW - rightPadding.toPx() - bubbleSize.toPx()).roundToInt() }
            val yAvailable = with(density) { parentH - bubbleSize.toPx() }
            val yPx = (yAvailable * fraction).roundToInt()

            Surface(
                tonalElevation = 6.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.offset { IntOffset(x = xPx, y = yPx) }
            ) {
                Box(Modifier.size(bubbleSize), contentAlignment = Alignment.Center) {
                    Text(
                        text = current.toString(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}
