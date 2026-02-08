package com.antechrist.adherentsapp.ui.components.kihon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.TechniqueRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniqueTemplatePickerSheet(
    show: Boolean,
    templates: List<TechniqueRef>,
    onAddSelected: (List<TechniqueRef>) -> Unit,
    onCreateManual: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var query by remember { mutableStateOf("") }
    var kindFilter by remember { mutableStateOf<TechniqueRef.Kind?>(null) }

    // ✅ on stocke uniquement les IDs sélectionnés (recomposition fiable)
    val selectedIds = remember { mutableStateListOf<String>() }

    val filtered = remember(query, kindFilter, templates) {
        val q = query.trim().lowercase()
        templates
            .asSequence()
            .filter { kindFilter == null || it.kind == kindFilter }
            .filter {
                q.isEmpty() ||
                        it.id.lowercase().contains(q) ||
                        it.nameJa.lowercase().contains(q) ||
                        it.nameFr.lowercase().contains(q) ||
                        it.aliases.any { a -> a.lowercase().contains(q) }
            }
            .sortedWith(compareBy<TechniqueRef> { it.kind.ordinal }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nameFr })
            .toList()
    }

    // ✅ calcul SANS remember → recompute à chaque recomposition
    val chosen = filtered.filter { it.id in selectedIds }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Ajouter depuis le référentiel", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Rechercher (id/JA/FR/alias)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(selected = kindFilter == null, onClick = { kindFilter = null }, label = { Text("Tous") })
                FilterChip(selected = kindFilter == TechniqueRef.Kind.POSITION, onClick = { kindFilter = TechniqueRef.Kind.POSITION }, label = { Text("Pos") })
                FilterChip(selected = kindFilter == TechniqueRef.Kind.DEFENSE,  onClick = { kindFilter = TechniqueRef.Kind.DEFENSE },  label = { Text("Def") })
                FilterChip(selected = kindFilter == TechniqueRef.Kind.PUNCH,    onClick = { kindFilter = TechniqueRef.Kind.PUNCH },    label = { Text("Poings") })
                FilterChip(selected = kindFilter == TechniqueRef.Kind.KICK,     onClick = { kindFilter = TechniqueRef.Kind.KICK },     label = { Text("Pieds") })
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 480.dp)
            ) {
                items(filtered, key = { it.id }) { ref ->
                    val checked = ref.id in selectedIds
                    ElevatedCard(
                        onClick = {
                            if (checked) selectedIds.remove(ref.id) else selectedIds.add(ref.id)
                        }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) selectedIds.add(ref.id) else selectedIds.remove(ref.id)
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text("${ref.nameFr} • ${ref.nameJa}")
                                Text(
                                    ref.kind.name.lowercase().replaceFirstChar { it.titlecase() } +
                                            (ref.subType?.let { " • $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onCreateManual) { Text("Créer manuellement") }
                ElevatedButton(
                    onClick = { onAddSelected(chosen) },
                    enabled = chosen.isNotEmpty()                   // ✅ se met à jour correctement
                ) { Text("Ajouter (${chosen.size})") }
            }
        }
    }
}