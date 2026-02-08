package com.antechrist.adherentsapp.ui.screens.presence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.platform.LocalContext
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresenceReportsScreen(
    onBack: () -> Unit,
    onOpenTake: (dateKey: String, groupDisplay: String) -> Unit,
    vm: PresenceReportsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    // événements (CSV prêt / erreurs / ouvrir prise)
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is ReportsEvent.Error -> scope.launch { snackbar.showSnackbar(ev.message) }
                is ReportsEvent.CsvReady -> scope.launch { snackbar.showSnackbar("CSV prêt : ${ev.filename}") }
                is ReportsEvent.OpenTake -> onOpenTake(ev.dateKey, ev.groupDisplay)
            }
        }
    }

    val fmtShow = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var modeUnique by remember { mutableStateOf(true) } // Date unique / Plage

    fun dateMillisOf(d: LocalDate) =
        d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val startState = rememberDatePickerState(initialSelectedDateMillis = dateMillisOf(ui.start))
    val endState = rememberDatePickerState(initialSelectedDateMillis = dateMillisOf(ui.end))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rapports présences") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.exportCsv() },
                        enabled = ui.rows.isNotEmpty() && !ui.loading
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "Exporter CSV")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { paddings ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sélecteur de mode (Date unique / Plage)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = modeUnique, onClick = { modeUnique = true })
                    Text("Date unique")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !modeUnique, onClick = { modeUnique = false })
                    Text("Plage de dates")
                }
            }

            // Champs Date
            if (modeUnique) {
                OutlinedTextField(
                    value = ui.start.format(fmtShow),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = { showStartPicker = true }) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "Choisir la date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = ui.start.format(fmtShow),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Du") },
                    trailingIcon = {
                        IconButton(onClick = { showStartPicker = true }) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "Choisir la date de début")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ui.end.format(fmtShow),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Au") },
                    trailingIcon = {
                        IconButton(onClick = { showEndPicker = true }) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "Choisir la date de fin")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Champ Groupe
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = ui.groupDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Groupe") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    singleLine = true,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    vm.groups.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g) },
                            onClick = {
                                vm.onGroupChanged(g)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Bouton Valider
            Button(
                onClick = {
                    if (modeUnique) vm.onEndChanged(ui.start) // start=end
                    vm.load()
                },
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (ui.loading) "Chargement..." else "Valider")
            }

            // Résultats
            if (ui.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (ui.rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (ui.start == LocalDate.now() && ui.end == LocalDate.now()) {
                        Text("Faites votre appel pour l’afficher ici")
                    } else {
                        Text("Aucun résultat pour ces filtres")
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(ui.rows, key = { it.dateKey }) { r ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.openTake(r.dateKey) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "${r.dateKey} — ${r.groupDisplay}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Présents ${r.present} • Absents ${r.absent} • Retards ${r.retard} • Total ${r.total}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    // 👉 Bouton Export PDF détaillé (liste Nom/Prénom/Statut/Note)
                                    IconButton(
                                        onClick = {
                                            vm.exportPdfFor(
                                                context = context,
                                                dateKey = r.dateKey,
                                                groupDisplay = r.groupDisplay
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exporter PDF")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DatePickers
    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startState.selectedDateMillis?.let { millis ->
                        vm.onStartChanged(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                        if (modeUnique) vm.onEndChanged(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                    }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Annuler") } }
        ) {
            DatePicker(state = startState)
        }
    }
    if (showEndPicker && !modeUnique) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endState.selectedDateMillis?.let { millis ->
                        vm.onEndChanged(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                    }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Annuler") } }
        ) {
            DatePicker(state = endState)
        }
    }
}
