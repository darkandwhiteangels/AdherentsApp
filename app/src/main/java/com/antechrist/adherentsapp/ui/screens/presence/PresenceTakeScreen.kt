package com.antechrist.adherentsapp.ui.screens.presence

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.ui.grade.BeltCatalog
import com.antechrist.adherentsapp.ui.theme.extraColors
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

enum class PresenceStatus { Present, Absent }

/** Étendu avec beltCode + attribution pour l'UI */
data class PresenceItem(
    val id: String,
    val displayName: String,
    var status: PresenceStatus = PresenceStatus.Absent,
    var note: String = "",
    val beltCode: String? = null,
    val attribution: String? = null // "pussay", "saclas", "encadrant", etc.
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresenceTakeScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: PresenceTakeViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var selectedDate by remember(ui.date) { mutableStateOf(ui.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    var groupExpanded by remember { mutableStateOf(false) }
    var selectedGroup by remember(ui.groupDisplay) { mutableStateOf(ui.groupDisplay) }

    // 🆕 filtre site local (synchro avec ui.siteFilter)
    var localSiteFilter by remember(ui.siteFilter) { mutableStateOf(ui.siteFilter) }

    // Items UI (éditables localement)
    val items = remember(ui.items) {
        mutableStateListOf<PresenceItem>().apply {
            clear(); addAll(ui.items.map { it.copy() })
        }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is PresenceEvent.Saved -> onSaved()
                is PresenceEvent.Error -> scope.launch { snackbar.showSnackbar(ev.message) }
            }
        }
    }

    // Écoute des changements pour propager au VM (statuts uniquement)
    fun onStatusChange(id: String, new: PresenceStatus) {
        items.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { idx ->
            items[idx] = items[idx].copy(status = new)
        }
        vm.updateStatus(id, new)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feuille de présence") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.save() },
                        enabled = !ui.isSaving
                    ) {
                        if (ui.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Done, contentDescription = "Enregistrer")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        // pas d'insets auto
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPaddings ->
        Box(
            Modifier
                .padding(innerPaddings)
                .consumeWindowInsets(innerPaddings)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ligne filtres : Date / Groupe
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDate.format(displayFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        placeholder = { Text("JJ/MM/AAAA") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Choisir une date"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.8f)
                            .heightIn(min = 56.dp),
                    )

                    ExposedDropdownMenuBox(
                        expanded = groupExpanded,
                        onExpandedChange = { groupExpanded = !groupExpanded },
                        modifier = Modifier.weight(0.8f)
                    ) {
                        OutlinedTextField(
                            value = selectedGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Groupe") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .heightIn(min = 56.dp),
                        )
                        ExposedDropdownMenu(
                            expanded = groupExpanded,
                            onDismissRequest = { groupExpanded = false }
                        ) {
                            vm.groups.forEach { g ->
                                DropdownMenuItem(text = { Text(g) }, onClick = {
                                    selectedGroup = g
                                    groupExpanded = false
                                    vm.onGroupChanged(g)
                                })
                            }
                        }
                    }
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    selectedDate =
                                        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                                            .toLocalDate()
                                    vm.onDateChanged(selectedDate)
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDatePicker = false
                            }) { Text("Annuler") }
                        }
                    ) { DatePicker(state = datePickerState) }
                }

                // 🆕 Ligne filtre site rapide : Tous / Pussay / Saclas

//                SiteFilterRow(
//                    current = localSiteFilter,
//                    onChange = { newFilter ->
//                        localSiteFilter = newFilter
//                        vm.onSiteFilterChanged(newFilter)
//                    }
//                )

                // Résumé simple
                //AssistiveCounter(items = items)

                HeaderRow(
                    items = items,
                    currentFilter = localSiteFilter,
                    onFilterSelected = { newFilter ->
                        localSiteFilter = newFilter
                        vm.onSiteFilterChanged(newFilter)
                    }
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                // Loading / empty / list
                when {
                    ui.loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    items.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text("Aucun adhérent pour « ${ui.groupDisplay} »") }

                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f, fill = true),
                        contentPadding = PaddingValues(bottom = 0.dp)
                    ) {
                        items(items, key = { it.id }) { row ->
                            PresenceRow(
                                item = row,
                                onStatusChange = { new -> onStatusChange(row.id, new) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteFilterRow(
    current: SiteFilter,
    onChange: (SiteFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(
            selected = current == SiteFilter.TOUS,
            onClick = { onChange(SiteFilter.TOUS) },
            label = { Text("Tous") }
        )
        FilterChip(
            selected = current == SiteFilter.PUSSAY,
            onClick = { onChange(SiteFilter.PUSSAY) },
            label = { Text("Pussay") }
        )
        FilterChip(
            selected = current == SiteFilter.SACLAS,
            onClick = { onChange(SiteFilter.SACLAS) },
            label = { Text("Saclas") }
        )
    }
}

@Composable
private fun HeaderRowWithFilter(
    items: List<PresenceItem>,
    currentFilter: SiteFilter,
    onFilterChange: (SiteFilter) -> Unit
) {
    val total = items.size
    val presents = items.count { it.status == PresenceStatus.Present }
    val absents = items.count { it.status == PresenceStatus.Absent }

    val cs = MaterialTheme.extraColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Compteur texte à gauche
        Text(
            buildAnnotatedString {
                // présents
                withStyle(SpanStyle(color = cs.certifiedBlue)) {
                    append("$presents présents")
                }
                append(" / ")
                // absents
                withStyle(SpanStyle(color = cs.rougeTatamie)) {
                    append("$absents absents")
                }
                append(" / ")
                // total
                withStyle(SpanStyle(color = cs.dorureDojo)) {
                    append("$total total")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Pastilles filtre à droite
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeltDot(
                color = Color.Black,
                selected = currentFilter == SiteFilter.TOUS,
                onClick = { onFilterChange(SiteFilter.TOUS) }
            )
            BeltDot(
                color = Color.Red,
                selected = currentFilter == SiteFilter.PUSSAY,
                onClick = { onFilterChange(SiteFilter.PUSSAY) }
            )
            BeltDot(
                color = cs.certifiedBlue,
                selected = currentFilter == SiteFilter.SACLAS,
                onClick = { onFilterChange(SiteFilter.SACLAS) }
            )
        }
    }
}

@Composable
private fun HeaderRow(
    items: List<PresenceItem>,
    currentFilter: SiteFilter,
    onFilterSelected: (SiteFilter) -> Unit
) {
    val total = items.size
    val presents = items.count { it.status == PresenceStatus.Present }
    val absents = items.count { it.status == PresenceStatus.Absent }

    val cs = MaterialTheme.extraColors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compteur texte à gauche
        Text(
            buildAnnotatedString {
                pushStyle(SpanStyle(color = cs.certifiedBlue))
                append("$presents présents")
                pop()
                append(" / ")
                pushStyle(SpanStyle(color = cs.rougeTatamie))
                append("$absents absents")
                pop()
                append(" / ")
                pushStyle(SpanStyle(color = cs.dorureDojo))
                append("$total total")
                pop()
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Pastilles filtres à droite
        FilterBeltDot(
            color = MaterialTheme.extraColors.rougeTatamie,           // Pussay
            selected = currentFilter == SiteFilter.PUSSAY,
            onClick = { onFilterSelected(SiteFilter.PUSSAY) }
        )
        FilterBeltDot(
            color = MaterialTheme.extraColors.certifiedBlue,   // Saclas (bleu club ou cs.certifiedBlue si tu préfères)
            selected = currentFilter == SiteFilter.SACLAS,
            onClick = { onFilterSelected(SiteFilter.SACLAS) }
        )
        FilterBeltDot(
            color = Color.Black,         // Tous
            selected = currentFilter == SiteFilter.TOUS,
            onClick = { onFilterSelected(SiteFilter.TOUS) }
        )
    }
}


@Composable
private fun BeltDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    // visuel type ceinture : un rond ou pastille qui grossit / a une bordure quand sélectionné
    val size = if (selected) 20.dp else 14.dp
    val borderWidth = if (selected) 2.dp else 0.dp
    val borderColor = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent

    Box(
        modifier = Modifier
            .size(size)
            .background(color = color, shape = CircleShape)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun FilterBeltDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        tonalElevation = if (selected) 4.dp else 0.dp,
        shadowElevation = if (selected) 4.dp else 0.dp,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
        else BorderStroke(1.dp, Color.Transparent),
        modifier = Modifier
            .height(16.dp)
            .width(if (selected) 32.dp else 28.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color),
        )
    }
}


@Composable
private fun PresenceRow(
    item: PresenceItem,
    onStatusChange: (PresenceStatus) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val innerShape = RoundedCornerShape(18.dp)
    val tint = beltInnerTint(item.beltCode, fallback = cs.surfaceVariant.copy(alpha = 0.06f))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        ),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
        onClick = {
            val toggled = if (item.status == PresenceStatus.Present) PresenceStatus.Absent else PresenceStatus.Present
            onStatusChange(toggled)
        }
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .clip(innerShape)
                .fillMaxSize()
                .background(tint)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(innerShape)
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.10f),
                            0.12f to Color.Transparent
                        )
                    )
            )

            // 🆕 Ribbon attribution en haut-droite (si présent)
            item.attribution?.let { attrib ->
                RibbonAttribution(
                    text = attribDisplay(attrib),
                    color = attribColor(attrib),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(0.dp)
                )
            }

            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = item.status == PresenceStatus.Present,
                        onClick = { onStatusChange(PresenceStatus.Present) },
                        label = { Text("Présent") }
                    )
                    FilterChip(
                        selected = item.status == PresenceStatus.Absent,
                        onClick = { onStatusChange(PresenceStatus.Absent) },
                        label = { Text("Absent") }
                    )
                }
            }
        }
    }
}

/** Teinte douce interne suivant la ceinture (sans toucher à l’ombre du Card). */
@Composable
private fun beltInnerTint(beltCode: String?, fallback: Color): Color {
    val spec = if (beltCode?.startsWith("karate:") == true) {
        BeltCatalog.Karate.specOf(beltCode)
    } else null
    val base = spec?.primary
    return base?.copy(alpha = 0.08f) ?: fallback
}

/* -------------------- Ribbon helpers -------------------- */

@Composable
private fun RibbonAttribution(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Petit ruban en biais : un fond coloré + texte, léger padding
    Box(
        modifier = modifier
            .width(140.dp)
            .height(24.dp)
            .offset(x = 34.dp, y = (24).dp)
            .rotate(45f)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1
        )
    }
}

private fun attribDisplay(raw: String): String = when (raw.lowercase()) {
    "pussay"    -> "Pussay"
    "saclas"    -> "Saclas"
    "encadrant" -> "Encadrant"
    else        -> raw
}

@Composable
private fun attribColor(raw: String): Color {
    val cs = MaterialTheme.extraColors
    return when (raw.lowercase()) {
        "pussay"    -> cs.rougeTatamie   // rouge
        "saclas"    -> cs.certifiedBlue // bleu/teal selon thème
        "encadrant" -> cs.dorureDojo
        else        -> cs.grisDojo
    }
}
