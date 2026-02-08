package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Index A..Z + #, renvoie la lettre ET la position verticale (fraction 0f..1f).
 * @param presentLetters lettres réellement présentes (sert uniquement à griser les absentes)
 * @param onLetterChange callback (letter, yFraction)
 */
@Composable
fun AlphaIndexBar(
    presentLetters: Set<Char>,
    onLetterChange: (Char, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val letters = remember { ('A'..'Z').toList() + '#' }

    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    fun yToIndex(y: Float): Int {
        if (boxSize.height == 0) return 0
        val slot = boxSize.height.toFloat() / letters.size
        val idx = floor(y / slot).toInt()
        return min(max(idx, 0), letters.lastIndex)
    }

    fun yToFraction(y: Float): Float {
        if (boxSize.height == 0) return 0f
        val clamped = y.coerceIn(0f, boxSize.height.toFloat())
        return clamped / boxSize.height.toFloat()
    }

    Box(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight()
            .onGloballyPositioned { boxSize = it.size }
            .pointerInput(Unit) {
                detectTapGestures { offset: Offset ->
                    val idx = yToIndex(offset.y)
                    onLetterChange(letters[idx], yToFraction(offset.y))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        val idx = yToIndex(start.y)
                        onLetterChange(letters[idx], yToFraction(start.y))
                    },
                    onDrag = { change, _ ->
                        val idx = yToIndex(change.position.y)
                        onLetterChange(letters[idx], yToFraction(change.position.y))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // ✅ clé : la colonne prend TOUTE la hauteur + espace uniforme entre lettres
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { c ->
                val enabled = presentLetters.contains(c)
                val color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

                Text(
                    text = c.toString(),
                    color = color,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}
