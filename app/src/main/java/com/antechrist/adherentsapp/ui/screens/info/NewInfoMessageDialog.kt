package com.antechrist.adherentsapp.ui.screens.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.Audience
import com.antechrist.adherentsapp.domain.model.MessagePriority
import com.antechrist.adherentsapp.domain.model.NotificationGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInfoMessageDialog(
    onDismiss: () -> Unit,
    availableGroups: List<NotificationGroup>, // ✅ NEW
    onSubmit: (
        title: String,
        body: String,
        audience: Audience,
        priority: MessagePriority,
        color: String?,
        targetGroupIds: List<String>
    ) -> Unit
) {
    var titleState by remember { mutableStateOf(TextFieldValue("")) }
    var bodyState by remember { mutableStateOf(TextFieldValue("")) }

    var expanded by remember { mutableStateOf(false) }
    var selectedAudience by remember { mutableStateOf(Audience.ALL_REGISTERED) }

    var selectedPriority by remember { mutableStateOf(MessagePriority.NORMAL) }

    // ✅ sélection des groupes (id -> bool)
    val selectedGroupIds = remember { mutableStateMapOf<String, Boolean>() }

    val scrollState = rememberScrollState()

    val audienceLabel: (Audience) -> String = {
        when (it) {
            Audience.ALL_REGISTERED -> "Tous (adultes + parents)"
            Audience.ADULTS_ONLY -> "Adultes uniquement"
            Audience.GUARDIANS -> "Parents / Tuteurs uniquement"
            Audience.CUSTOM_GROUPS -> "Groupes personnalisés"
        }
    }

    val chipW = 140.dp
    val chipH = 38.dp

    fun chipLabel(p: MessagePriority): String = when (p) {
        MessagePriority.LOW -> "Faible"
        MessagePriority.NORMAL -> "Normal"
        MessagePriority.HIGH -> "Important"
        MessagePriority.URGENT -> "Urgent"
    }

    fun chipSelectedColor(p: MessagePriority): Color = when (p) {
        MessagePriority.LOW -> Color(0xFF81C784)    // ✅ vert clair
        MessagePriority.NORMAL -> Color(0xFF2196F3) // bleu
        MessagePriority.HIGH -> Color(0xFFFF9800)   // orange
        MessagePriority.URGENT -> Color(0xFFF44336) // rouge
    }

    // ✅ validation
    val titleOk = titleState.text.trim().isNotEmpty()
    val bodyOk = bodyState.text.trim().isNotEmpty()
    val selectedIds = selectedGroupIds.filterValues { it }.keys.toList()
    val groupsOk = selectedAudience != Audience.CUSTOM_GROUPS || selectedIds.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau message") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding()
            ) {
                OutlinedTextField(
                    value = titleState,
                    onValueChange = { titleState = it },
                    singleLine = true,
                    label = { Text("Titre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = bodyState,
                    onValueChange = { bodyState = it },
                    label = { Text("Message") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(bottom = 8.dp),
                    maxLines = 10
                )

                // Sélecteur d'audience
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = audienceLabel(selectedAudience),
                        onValueChange = {},
                        label = { Text("Destinataires") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Audience.entries.forEach { aud ->
                            DropdownMenuItem(
                                text = { Text(audienceLabel(aud)) },
                                onClick = {
                                    selectedAudience = aud
                                    expanded = false

                                    // ✅ si on change d'audience, on reset la sélection des groupes
                                    if (aud != Audience.CUSTOM_GROUPS) {
                                        selectedGroupIds.clear()
                                    }
                                }
                            )
                        }
                    }
                }

                // ✅ Choix des groupes si CUSTOM_GROUPS
                if (selectedAudience == Audience.CUSTOM_GROUPS) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Choisir les groupes",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (availableGroups.isEmpty()) {
                        Text(
                            "Aucun groupe disponible. Crée un groupe dans l’écran Groupes.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        availableGroups.forEach { g ->
                            val checked = selectedGroupIds[g.id] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { selectedGroupIds[g.id] = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = g.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Priorité",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityChip(
                            label = chipLabel(MessagePriority.LOW),
                            selected = selectedPriority == MessagePriority.LOW,
                            selectedColor = chipSelectedColor(MessagePriority.LOW),
                            width = chipW,
                            height = chipH,
                            onClick = { selectedPriority = MessagePriority.LOW }
                        )
                        PriorityChip(
                            label = chipLabel(MessagePriority.NORMAL),
                            selected = selectedPriority == MessagePriority.NORMAL,
                            selectedColor = chipSelectedColor(MessagePriority.NORMAL),
                            width = chipW,
                            height = chipH,
                            onClick = { selectedPriority = MessagePriority.NORMAL }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityChip(
                            label = chipLabel(MessagePriority.HIGH),
                            selected = selectedPriority == MessagePriority.HIGH,
                            selectedColor = chipSelectedColor(MessagePriority.HIGH),
                            width = chipW,
                            height = chipH,
                            onClick = { selectedPriority = MessagePriority.HIGH }
                        )
                        PriorityChip(
                            label = chipLabel(MessagePriority.URGENT),
                            selected = selectedPriority == MessagePriority.URGENT,
                            selectedColor = chipSelectedColor(MessagePriority.URGENT),
                            width = chipW,
                            height = chipH,
                            onClick = { selectedPriority = MessagePriority.URGENT }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val title = titleState.text.trim()
                    val body = bodyState.text.trim()
                    if (title.isNotEmpty() && body.isNotEmpty() && groupsOk) {
                        onSubmit(
                            title,
                            body,
                            selectedAudience,
                            selectedPriority,
                            null, // pas de palette
                            if (selectedAudience == Audience.CUSTOM_GROUPS) selectedIds else emptyList()
                        )
                    }
                },
                enabled = titleOk && bodyOk && groupsOk
            ) {
                Text("Publier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun PriorityChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        },
        modifier = Modifier
            .width(width)
            .height(height),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor,
            selectedLabelColor = Color.White,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
