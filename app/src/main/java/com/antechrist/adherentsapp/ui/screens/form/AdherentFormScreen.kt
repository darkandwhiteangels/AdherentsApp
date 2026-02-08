package com.antechrist.adherentsapp.ui.screens.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChipDefaults.filterChipColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.ui.components.AdherentPhotoCaptureRow
import com.antechrist.adherentsapp.ui.components.BeltPicker
import com.antechrist.adherentsapp.ui.components.FrenchPhoneVisualTransformation
import com.antechrist.adherentsapp.ui.components.sanitizePhoneDigits
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.platform.LocalContext
import com.antechrist.adherentsapp.R
import com.antechrist.adherentsapp.ui.utils.PdfOpener
import androidx.compose.runtime.rememberCoroutineScope // 🆕 import nécessaire
import com.antechrist.adherentsapp.ui.theme.extraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherentFormScreen(
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    vm: AdherentFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val ui by vm.ui.collectAsState()
    val focus = LocalFocusManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    // Feedback
    LaunchedEffect(ui.saved) { if (ui.saved) { snackbar.showSnackbar("Enregistré"); onSaved() } }
    LaunchedEffect(ui.error) { ui.error?.let { scope.launch { snackbar.showSnackbar(it) } } }

    // Date picker
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()
    val humanFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Tel visuel (majeur / bureau)
    var phoneDigits by remember(ui.telephone) { mutableStateOf(sanitizePhoneDigits(ui.telephone)) }
    // Tel visuel (responsable)
    var gPhoneDigits by remember(ui.gTelephone) { mutableStateOf(sanitizePhoneDigits(ui.gTelephone)) }

    // Catégories (display -> key)
    val groupOptions = listOf(
        "Baby" to "baby",
        "Enfants < 14 ans" to "enfant_u14",
        "14+ / Adultes" to "adult_14p"
    )

    // Responsable: relation
    val relations = listOf("Père" to "pere", "Mère" to "mere")
    var relationExpanded by remember { mutableStateOf(false) }
    var relationDisplay by remember(ui.gRelation) {
        mutableStateOf(relations.firstOrNull { it.second == ui.gRelation }?.first ?: "")
    }

    // 🆕 Attribution (tri-switch)
    val attribItems = listOf(
        "Pussay" to "pussay",
        "Saclas" to "saclas",
        "Encadrant" to "encadrant"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui.id == null) "Nouvel adhérent" else "Modifier adhérent") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (ui.isPractitioner) "Pratiquant" else "Bureau")
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = ui.isPractitioner,
                            onCheckedChange = vm::onToggleIsPractitioner
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = { focus.clearFocus(); onCancel() }, enabled = !ui.loading) { Text("Annuler") }
                    Button(
                        onClick = { focus.clearFocus(); vm.save() },
                        enabled = !ui.loading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (ui.loading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Text("Enregistrer")
                    }
                }
            }
        }
    ) { paddings ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(horizontal = 16.dp)
                .verticalScroll(scroll)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ========== 1) Bloc de départ (toujours visible) ==========
            OutlinedTextField(
                value = ui.nom, onValueChange = vm::onChangeNom,
                label = { Text("Nom*") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ui.prenom, onValueChange = vm::onChangePrenom,
                label = { Text("Prénom*") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.dateNaissance, onValueChange = {},
                readOnly = true,
                label = { Text("Date de naissance*") }, placeholder = { Text("JJ/MM/AAAA") },
                trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, contentDescription = "Choisir une date") } },
                modifier = Modifier.fillMaxWidth()
            )
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            dateState.selectedDateMillis?.let { sel -> vm.onChangeDateNaissance(humanFmt.format(Date(sel))) }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } }
                ) { DatePicker(state = dateState) }
            }

            // 🆕 Tri-switch Attribution
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Attribution", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    attribItems.forEach { (label, key) ->
                        FilterChip(
                            selected = ui.attribution == key,
                            onClick = { vm.onChangeAttribution(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Catégorie — seulement pour Pratiquant
            if (ui.isPractitioner) {

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                Text("Catégorie* (jusqu’à 2)", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupOptions.forEach { (label, key) ->
                        // checked si la clé est déjà dans le pending (approx via ui.groupe pour rétro-affichage)
                        val checked = ui.selectedGroupKeys.contains(key)
                        val canToggleOn = checked || ui.selectedGroupKeys.size < 2
                        FilterChip(
                            selected = checked,
                            enabled = canToggleOn,
                            onClick = {
                                vm.onToggleGroup(key, !checked)
                            },
                            label = { Text(label) },
                            colors = filterChipColors(
                                selectedContainerColor = MaterialTheme.extraColors.certifiedBlue,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Ceinture — seulement pour Pratiquant
            if (ui.isPractitioner) {
                Text("Ceinture (optionnel)", style = MaterialTheme.typography.titleMedium)
                BeltPicker(
                    valueCode = ui.beltCode,
                    valueStripes = ui.stripeCount,
                    onChange = { code, stripes -> vm.onChangeBeltCode(code); vm.onChangeStripeCount(stripes) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Ceinture"
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }

            Text("Photo (optionnel)", style = MaterialTheme.typography.titleMedium)
            AdherentPhotoCaptureRow(
                initialUri = ui.photoUri,
                onPhotoCaptured = { uri -> vm.onPhotoSelected(uri.toString()) },
                enabled = true
            )

            // ========== 2) Après DOB ==========
            if (ui.dateNaissance.isNotBlank()) {
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                // RI — seulement pour Pratiquant
                if (ui.isPractitioner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { PdfOpener.openRaw(context, R.raw.ri, "Reglement_Interieur.pdf") }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = "Règlement intérieur (PDF)")
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Règlement Intérieur (PDF)",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (ui.isPractitioner) {
                    // ====== Branche PRATIQUANT ======
                    if (ui.isMinor) {
                        // ------------------- MINEUR -------------------
                        Text("Responsable (obligatoire pour mineur)", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = ui.gNom, onValueChange = vm::onChangeGNom,
                            label = { Text("Nom du responsable*") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ui.gPrenom, onValueChange = vm::onChangeGPrenom,
                            label = { Text("Prénom du responsable*") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = gPhoneDigits,
                            onValueChange = { v -> gPhoneDigits = sanitizePhoneDigits(v); vm.onChangeGTelephone(gPhoneDigits) },
                            label = { Text("Téléphone du responsable*") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = FrenchPhoneVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ui.gEmail, onValueChange = vm::onChangeGEmail,
                            label = { Text("Email du responsable*") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Lien de parenté
                        ExposedDropdownMenuBox(expanded = relationExpanded, onExpandedChange = { relationExpanded = !relationExpanded }) {
                            OutlinedTextField(
                                value = relationDisplay, onValueChange = {}, readOnly = true,
                                label = { Text("Lien de parenté*") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(relationExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable) // ← nouveau API
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = relationExpanded, onDismissRequest = { relationExpanded = false }) {
                                relations.forEach { (label, key) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        relationDisplay = label; vm.onChangeGRelation(key); relationExpanded = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = ui.gAdresse, onValueChange = vm::onChangeGAdresse,
                            label = { Text("Adresse du responsable*") }, modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = ui.gCodePostal, onValueChange = vm::onChangeGCodePostal,
                                label = { Text("Code postal*") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f)
                            )
                            OutlinedTextField(
                                value = ui.gVille, onValueChange = vm::onChangeGVille,
                                label = { Text("Ville*") }, singleLine = true, modifier = Modifier.weight(1.2f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("Autorisations (responsable)", style = MaterialTheme.typography.titleMedium)

                        ConsentSwitchRow(
                            title = "Autorise, en cas d'accident mineur, l'évacuation de mon enfant.",
                            checked = ui.minorConsentAccidentEvac,
                            onCheckedChange = vm::onToggleMinorAccidentEvac
                        )
                        ConsentSwitchRow(
                            title = "Autorise la prise et diffusion de photos de mon enfant sur les réseaux sociaux et le site du club.",
                            checked = ui.minorConsentPhotoSocial,
                            onCheckedChange = vm::onToggleMinorPhotoSocial
                        )
                        ConsentSwitchRow(
                            title = "Autorise la prise et diffusion de photos de mon enfant dans les journaux officiels (FFK, mairies).",
                            checked = ui.minorConsentPhotoOfficial,
                            onCheckedChange = vm::onToggleMinorPhotoOfficial
                        )

                        ConsentSwitchRow(
                            title = "Déclare avoir pris connaissance du Règlement Intérieur (ci-joint et affiché). *",
                            checked = ui.gRiAccepted,
                            onCheckedChange = vm::onToggleGuardianRiAccepted
                        )
                    } else {
                        // ------------------- MAJEUR -------------------
                        Text("Coordonnées (majeur)", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = phoneDigits,
                            onValueChange = { v -> phoneDigits = sanitizePhoneDigits(v); vm.onChangeTelephone(phoneDigits) },
                            label = { Text("Téléphone*") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = FrenchPhoneVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ui.email, onValueChange = vm::onChangeEmail,
                            label = { Text("Email*") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ui.adresse, onValueChange = vm::onChangeAdresse,
                            label = { Text("Adresse*") }, modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = ui.codePostal, onValueChange = vm::onChangeCodePostal,
                                label = { Text("Code postal*") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f)
                            )
                            OutlinedTextField(
                                value = ui.ville, onValueChange = vm::onChangeVille,
                                label = { Text("Ville*") }, singleLine = true, modifier = Modifier.weight(1.2f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("Autorisations", style = MaterialTheme.typography.titleMedium)

                        ConsentSwitchRow(
                            title = "Autorise, en cas d'accident mineur, à m'évacuer.",
                            checked = ui.adultConsentAccidentEvac,
                            onCheckedChange = vm::onToggleAdultAccidentEvac
                        )
                        ConsentSwitchRow(
                            title = "Autorise la prise et diffusion de mes photos sur les réseaux sociaux et le site du club.",
                            checked = ui.adultConsentPhotoSocial,
                            onCheckedChange = vm::onToggleAdultPhotoSocial
                        )
                        ConsentSwitchRow(
                            title = "Autorise la prise et diffusion de mes photos dans les journaux officiels (FFK, mairies).",
                            checked = ui.adultConsentPhotoOfficial,
                            onCheckedChange = vm::onToggleAdultPhotoOfficial
                        )

                        ConsentSwitchRow(
                            title = "Déclare avoir pris connaissance du Règlement Intérieur (ci-joint et affiché). *",
                            checked = ui.riAccepted,
                            onCheckedChange = vm::onToggleRiAccepted
                        )
                    }
                } else {
                    // ====== Branche BUREAU (non pratiquant) ======
                    Text("Coordonnées", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = phoneDigits,
                        onValueChange = { v -> phoneDigits = sanitizePhoneDigits(v); vm.onChangeTelephone(phoneDigits) },
                        label = { Text("Téléphone*") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = FrenchPhoneVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ui.email, onValueChange = vm::onChangeEmail,
                        label = { Text("Email*") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ui.adresse, onValueChange = vm::onChangeAdresse,
                        label = { Text("Adresse*") }, modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = ui.codePostal, onValueChange = vm::onChangeCodePostal,
                            label = { Text("Code postal*") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f)
                        )
                        OutlinedTextField(
                            value = ui.ville, onValueChange = vm::onChangeVille,
                            label = { Text("Ville*") }, singleLine = true, modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun ConsentSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
