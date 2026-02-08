package com.antechrist.adherentsapp.ui.components.signature

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Feuille plein écran pour capturer la signature.
 * - Bouton retour (fermer)
 * - Bouton valider (sauvegarder PNG)
 * - Actions : Annuler dernier trait / Effacer tout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureCaptureSheet(
    title: String = "Signature du président",
    suggestedExportWidthPx: Int = 1200,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val padState = rememberSignaturePadState(strokeWidth = 3.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fermer")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val bmp = padState.exportBitmap(suggestedExportWidthPx)
                            onSave(bmp)
                        },
                        enabled = !padState.isEmpty()
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Enregistrer")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { padState.undo() }, enabled = !padState.isEmpty()) {
                        Icon(Icons.Filled.Undo, contentDescription = "Annuler le dernier trait")
                    }
                    IconButton(onClick = { padState.clear() }, enabled = !padState.isEmpty()) {
                        Icon(Icons.Filled.Delete, contentDescription = "Effacer tout")
                    }
                }
            )
        }
    ) { paddings: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Signez ci-dessous (stylet ou doigt).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            SignaturePad(
                state = padState,
                modifier = Modifier.fillMaxWidth(),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // Boutons en renfort (optionnels, en plus de la BottomAppBar)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val enabled = !padState.isEmpty()
                OutlinedButton(
                    onClick = { padState.clear() },
                    enabled = enabled
                ) { Text("Effacer") }

                // Exemple de bouton secondaire pour valider (si tu ne veux pas utiliser l'icône du TopAppBar)
                // Button(
                //     onClick = {
                //         val bmp = padState.exportBitmap(suggestedExportWidthPx)
                //         onSave(bmp)
                //     },
                //     enabled = enabled,
                //     modifier = Modifier.align(Alignment.CenterEnd)
                // ) { Text("Enregistrer") }
            }
        }
    }
}
