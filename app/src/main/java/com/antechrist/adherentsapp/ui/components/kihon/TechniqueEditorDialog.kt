package com.antechrist.adherentsapp.ui.components.kihon

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.TechniqueRef

@Composable
fun TechniqueEditorDialog(
    initial: TechniqueRef?,
    onDismiss: () -> Unit,
    onConfirm: (TechniqueRef) -> Unit
) {
    var id by remember { mutableStateOf(initial?.id ?: "") }
    var nameJa by remember { mutableStateOf(initial?.nameJa ?: "") }
    var nameFr by remember { mutableStateOf(initial?.nameFr ?: "") }
    var subType by remember { mutableStateOf(initial?.subType ?: "") }
    var aliasesText by remember { mutableStateOf(initial?.aliases?.joinToString(", ") ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var kind by remember { mutableStateOf(initial?.kind ?: TechniqueRef.Kind.POSITION) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val aliases = aliasesText.split(",").mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }
                    onConfirm(
                        TechniqueRef(
                            id = id.trim().uppercase(),
                            kind = kind,
                            nameJa = nameJa.trim(),
                            nameFr = nameFr.trim(),
                            aliases = aliases,
                            subType = subType.trim().ifEmpty { null },
                            notes = notes.trim().ifEmpty { null }
                        )
                    )
                },
                enabled = id.isNotBlank() && nameFr.isNotBlank() && nameJa.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text(if (initial == null) "Nouvelle technique" else "Modifier la technique") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Kind selector
                KindChips(kind = kind, onChange = { kind = it })

                OutlinedTextField(value = id, onValueChange = { id = it },
                    label = { Text("ID (UPPER_SNAKE)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nameJa, onValueChange = { nameJa = it },
                    label = { Text("Nom japonais (romaji)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nameFr, onValueChange = { nameFr = it },
                    label = { Text("Nom français") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subType, onValueChange = { subType = it },
                    label = { Text("Sous-type (ex: zuki, uke, geri, dachi)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = aliasesText, onValueChange = { aliasesText = it },
                    label = { Text("Alias (séparés par des virgules)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        }
    )
}

@Composable
private fun KindChips(kind: TechniqueRef.Kind, onChange: (TechniqueRef.Kind) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = kind == TechniqueRef.Kind.POSITION, onClick = { onChange(TechniqueRef.Kind.POSITION) }, label = { Text("Position") })
        FilterChip(selected = kind == TechniqueRef.Kind.DEFENSE,  onClick = { onChange(TechniqueRef.Kind.DEFENSE) },  label = { Text("Défense") })
        FilterChip(selected = kind == TechniqueRef.Kind.PUNCH,    onClick = { onChange(TechniqueRef.Kind.PUNCH) },    label = { Text("Poings") })
        FilterChip(selected = kind == TechniqueRef.Kind.KICK,     onClick = { onChange(TechniqueRef.Kind.KICK) },     label = { Text("Pieds") })
    }
}
