package com.antechrist.adherentsapp.ui.screens.guardian

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianEditScreen(
    guardianId: String,
    onClose: () -> Unit,
    vm: GuardianEditViewModel = hiltViewModel()
) {
    val state by vm.ui.collectAsState()
    val focus = LocalFocusManager.current
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(guardianId) { vm.load(guardianId) }
    LaunchedEffect(state.success) {
        if (state.success) {
            snack.showSnackbar("Modifications enregistrées.")
            //vm.consumeSuccess()
            onClose()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snack.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Éditer le responsable") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onClose, enabled = !state.saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            focus.clearFocus()
                            vm.save()
                        },
                        enabled = !state.saving && !state.loading
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = "Enregistrer")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { paddings ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(paddings), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddings.calculateTopPadding() + 16.dp,
                    bottom = paddings.calculateBottomPadding() + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // NOM — majuscules à l'enregistrement
            OutlinedTextField(
                value = state.lastName,
                onValueChange = vm::onLastName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom") },
                singleLine = true,
                isError = state.eLast != null,
                supportingText = { state.eLast?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                )
            )

            // PRÉNOM — UcFirst à l'enregistrement
            OutlinedTextField(
                value = state.firstName,
                onValueChange = vm::onFirstName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Prénom") },
                singleLine = true,
                isError = state.eFirst != null,
                supportingText = { state.eFirst?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            // RELATION — UcFirst à l'enregistrement
            OutlinedTextField(
                value = state.relation,
                onValueChange = vm::onRelation,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Relation (mère, père, tuteur…)") },
                singleLine = true,
                isError = state.eRelation != null,
                supportingText = { state.eRelation?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            // TÉLÉPHONE — digits only (clavier chiffres)
            OutlinedTextField(
                value = state.phoneDigits,
                onValueChange = vm::onPhone,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Téléphone (10 chiffres)") },
                singleLine = true,
                isError = state.ePhone != null,
                supportingText = { state.ePhone?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            // EMAIL — minuscule automatique
            OutlinedTextField(
                value = state.email,
                onValueChange = vm::onEmail,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                isError = state.eEmail != null,
                supportingText = { state.eEmail?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            // ADRESSE (optionnel)
            OutlinedTextField(
                value = state.address,
                onValueChange = vm::onAddress,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Adresse (optionnel)") },
                singleLine = false,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )

            // CODE POSTAL — chiffres uniquement
            OutlinedTextField(
                value = state.postalCode,
                onValueChange = vm::onPostalCode,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Code postal") },
                singleLine = true,
                isError = state.ePostal != null,
                supportingText = { state.ePostal?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            // VILLE — UcFirst à l'enregistrement
            OutlinedTextField(
                value = state.city,
                onValueChange = vm::onCity,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ville") },
                singleLine = true,
                isError = state.eCity != null,
                supportingText = { state.eCity?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // ❌ ne pas appeler LocalFocusManager.current ici
                        focus.clearFocus()
                        vm.save()
                    }
                )
            )

            Button(
                onClick = {
                    // ❌ ne pas appeler LocalFocusManager.current ici
                    focus.clearFocus()
                    vm.save()
                },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.saving) CircularProgressIndicator() else Text("Enregistrer")
            }
        }
    }
}
