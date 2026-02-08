package com.antechrist.adherentsapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import coil.compose.AsyncImage
import com.antechrist.adherentsapp.core.media.createImageUri

/**
 * Affiche l’aperçu (initialUri) et propose :
 * - Prendre une photo (caméra)
 * - Choisir dans la galerie (Photo Picker AndroidX, sans permission)
 */
@Composable
fun AdherentPhotoCaptureRow(
    initialUri: String? = null,
    onPhotoCaptured: (Uri) -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    // Caméra : TakePicture vers un Uri MediaStore
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingUri != null) {
            onPhotoCaptured(pendingUri!!)
        }
        pendingUri = null
    }

    // Permission caméra (uniquement pour le bouton "Prendre une photo")
    val camPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* l’utilisateur re-cliquera après accord */ }

    // Galerie : AndroidX Photo Picker (aucune permission)
    val pickMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onPhotoCaptured(uri)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!initialUri.isNullOrBlank()) {
            AsyncImage(
                model = initialUri,
                contentDescription = "Photo adhérent",
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                        CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
        }

        // Bouton Caméra
        OutlinedButton(
            onClick = {
                if (!enabled) return@OutlinedButton

                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (!granted) {
                    camPermissionLauncher.launch(Manifest.permission.CAMERA)
                    return@OutlinedButton
                }

                val uri = createImageUri(context)
                if (uri != null) {
                    pendingUri = uri
                    takePictureLauncher.launch(uri)
                }
            },
            enabled = enabled
        ) { Text(if (initialUri.isNullOrBlank()) "Prendre une photo" else "Changer la photo (caméra)") }

        Spacer(Modifier.height(8.dp))

        // Bouton Galerie (Photo Picker AndroidX)
        OutlinedButton(
            onClick = {
                if (!enabled) return@OutlinedButton
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = enabled
        ) { Text("Choisir dans la galerie") }
    }
}
