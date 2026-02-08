package com.antechrist.adherentsapp.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.ExperimentalFoundationApi
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.FractionalThreshold
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.rememberSwipeableState
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.swipeable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import com.antechrist.adherentsapp.ui.theme.extraColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableAdherentRow(
    adherentId: String,
    openedAdherentId: String?,
    onOpenChange: (String?) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Conversion correcte de Dp en pixels
    val actionSizePx = with(LocalDensity.current) { 160.dp.toPx() }

    // 0 = fermé, 1 = ouvert
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val anchors = mapOf(0f to 0, -actionSizePx to 1)

    // Si un autre item est ouvert, on ferme celui-ci
    LaunchedEffect(openedAdherentId) {
        if (openedAdherentId != adherentId && swipeableState.currentValue == 1) {
            swipeableState.animateTo(0, anim = tween(durationMillis = 200))
        }
    }

    // Quand cet item s'ouvre, on notifie le parent
    LaunchedEffect(swipeableState.currentValue) {
        if (swipeableState.currentValue == 1) {
            onOpenChange(adherentId)
        } else if (swipeableState.currentValue == 0 && openedAdherentId == adherentId) {
            // Si on referme cet item, on notifie aussi
            onOpenChange(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                enabled = enabled
            )
    ) {
        // ✅ 3. MODIFIER L'ARRIÈRE-PLAN POUR AFFICHER DEUX BOUTONS
        Row(
            modifier = Modifier
                .matchParentSize()
                // ✅ 1. Découper la forme avec les coins arrondis
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 12.dp,       // Arrondi en haut à droite
                        bottomStart = 0.dp,
                        bottomEnd = 12.dp     // Arrondi en bas à droite
                    )
                )
                .background(MaterialTheme.extraColors.chrome)
                .padding(horizontal = 16.dp), // Un peu moins de padding pour centrer les boutons
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BOUTON "SUPPRIMER" (le plus à droite)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(64.dp) // Un peu plus grand pour être une cible facile
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.extraColors.rougeTatamie,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("Suppr.", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // BOUTON "ARCHIVER"
            IconButton(
                onClick = onArchive,
                modifier = Modifier.size(64.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archiver",
                        tint = MaterialTheme.extraColors.certifiedBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("Archiver", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Contenu principal qui se déplace
        Box(
            modifier = Modifier
                .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}