package com.antechrist.adherentsapp.ui.screens.config

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.antechrist.adherentsapp.ui.components.signature.SignatureCaptureSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubConfigScreen(
    onBack: () -> Unit,
    vm: ClubConfigViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var showSignatureSheet by remember { mutableStateOf(false) }
    var showSecretarySignatureSheet by remember { mutableStateOf(false) }


    // Pickers
    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.uploadLogo(it) }
    }
    // On garde "Importer" comme option secondaire en plus de la signature manuscrite
    val pickSignature = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.uploadSignature(uri) }
    }

    val pickSecretarySignature = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.uploadSecretarySignature(uri) }
    }


    LaunchedEffect(ui.savedSnack) {
        ui.savedSnack?.let { snackbar.showSnackbar(it); vm.clearSnack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration du club") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.save() }, enabled = !ui.saving) {
                        Icon(Icons.Filled.Save, contentDescription = "Enregistrer")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPaddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPaddings)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
            //verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ui.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = ui.clubName,
                onValueChange = vm::setClubName,
                label = { Text("Nom du club") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.clubCity,
                onValueChange = vm::setClubCity,
                label = { Text("Ville (pour « Fait à … »)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ui.presidentName,
                    onValueChange = vm::setPresident,
                    label = { Text("Nom du président") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ui.secretaryName,
                    onValueChange = vm::setSecretary,
                    label = { Text("Nom du secrétaire") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Logo
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Logo du club", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ui.logoUrl,
                            contentDescription = "Logo",
                            modifier = Modifier.size(96.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = { pickLogo.launch("image/*") }) {
                            Text(if (ui.logoUrl == null) "Importer" else "Remplacer")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Signature Président (capture manuscrite + import secondaire)
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Signature du président", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ui.signatureUrl,
                            contentDescription = "Signature",
                            modifier = Modifier
                                .height(64.dp)
                                .fillMaxWidth(0.4f),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showSignatureSheet = true }, enabled = !ui.saving) {
                                Text(if (ui.signatureUrl == null) "Signer…" else "Re-signer…")
                            }
                            OutlinedButton(onClick = { pickSignature.launch("image/*") }, enabled = !ui.saving) {
                                Text(if (ui.signatureUrl == null) "Importer" else "Remplacer (import)")
                            }
                        }
                    }
                    Text(
                        "Conseil : fond transparent (PNG) idéal. La signature manuscrite est capturée au doigt/stylet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Signature Secrétaire (capture manuscrite + import secondaire)
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Signature du secrétaire", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ui.secretarySignatureUrl,
                            contentDescription = "Signature secrétaire",
                            modifier = Modifier
                                .height(64.dp)
                                .fillMaxWidth(0.4f),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showSecretarySignatureSheet = true }, enabled = !ui.saving) {
                                Text(if (ui.secretarySignatureUrl == null) "Signer…" else "Re-signer…")
                            }
                            OutlinedButton(onClick = { pickSecretarySignature.launch("image/*") }, enabled = !ui.saving) {
                                Text(if (ui.secretarySignatureUrl == null) "Importer" else "Remplacer (import)")
                            }
                        }
                    }
                    Text(
                        "Conseil : fond transparent (PNG) idéal. La signature manuscrite est capturée au doigt/stylet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showSignatureSheet) {
        SignatureCaptureSheet(
            title = "Signature du président",
            onDismiss = { showSignatureSheet = false },
            onSave = { bitmap ->
                // Upload bitmap vers Storage + Firestore
                vm.uploadSignatureBitmap(bitmap)
                showSignatureSheet = false
            }
        )
    }

    if (showSecretarySignatureSheet) {
        SignatureCaptureSheet(
            title = "Signature du secrétaire",
            onDismiss = { showSecretarySignatureSheet = false },
            onSave = { bitmap ->
                vm.uploadSecretarySignatureBitmap(bitmap)
                showSecretarySignatureSheet = false
            }
        )
    }
}
