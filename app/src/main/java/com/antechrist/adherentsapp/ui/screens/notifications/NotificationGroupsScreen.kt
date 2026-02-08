package com.antechrist.adherentsapp.ui.screens.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.ui.theme.extraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationGroupsScreen(
    onBack: () -> Unit,
    currentUserUid: String,
    vm: NotificationGroupsViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()

    var editingGroup by remember { mutableStateOf<com.antechrist.adherentsapp.domain.model.NotificationGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groupes Personnalisés") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.openCreateDialog() },
                containerColor = MaterialTheme.extraColors.certifiedBlue,
                contentColor = MaterialTheme.extraColors.grisDojo
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Créer un groupe"
                )
            }
        }
    ) { padding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.groups.isEmpty()) {
                    item {
                        Text(
                            "Aucun groupe pour l’instant. Clique sur + pour en créer un.",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                items(state.groups, key = { it.id }) { g ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.extraColors.greenTatamie
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        onClick = { editingGroup = g }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(g.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.extraColors.blanc
                                )
                                Text(
                                    "${g.memberGuardianIds.size} responsable(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.extraColors.blanc
                                )
                            }
                            IconButton(onClick = { vm.deleteGroup(g.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateNotificationGroupDialog(
            guardians = state.guardians,
            onDismiss = { vm.closeCreateDialog() },
            onCreate = { name, guardianIds ->
                vm.createGroup(currentUserUid, name, guardianIds)
            }
        )
    }

    if (editingGroup != null) {
        EditNotificationGroupDialog(
            group = editingGroup!!,
            guardians = state.guardians,
            onDismiss = { editingGroup = null },
            onSave = { newName, selectedGuardianIds ->
                vm.updateGroup(
                    groupId = editingGroup!!.id,
                    name = newName,
                    memberGuardianIds = selectedGuardianIds
                )
                editingGroup = null
            }
        )
    }
}

@Composable
private fun CreateNotificationGroupDialog(
    guardians: List<Guardian>,
    onDismiss: () -> Unit,
    onCreate: (name: String, memberGuardianIds: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un groupe") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .imePadding()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du groupe") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                if (guardians.isEmpty()) {
                    Text("Aucun responsable disponible.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Sélectionner les responsables", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))

                    val eligibleGuardians = guardians.sortedWith(
                        compareBy<Guardian>({ (it.nom ?: "").lowercase() }, { (it.prenom ?: "").lowercase() })
                    )

                    eligibleGuardians.forEach { g ->
                        val key = g.id
                        val checked = selected[key] ?: false

                        val displayName = buildString {
                            val p = (g.prenom ?: "").trim()
                            val n = (g.nom ?: "").trim()
                            if (p.isNotEmpty()) append(p)
                            if (n.isNotEmpty()) {
                                if (isNotEmpty()) append(" ")
                                append(n)
                            }
                            val rel = (g.relation ?: "").trim()
                            if (rel.isNotEmpty()) {
                                if (isNotEmpty()) append(" ")
                                append("($rel)")
                            }
                        }.ifBlank { "Responsable ($key)" }

                        val contact = when {
                            !g.email.isNullOrBlank() -> g.email!!.trim()
                            !g.telephone.isNullOrBlank() -> g.telephone!!.trim()
                            else -> null
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = checked,
                                onCheckedChange = { selected[key] = it }
                            )
                            Spacer(Modifier.width(8.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (contact != null) {
                                    Text(
                                        text = contact,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ids = selected.filterValues { it }.keys.toList()
                    onCreate(name.trim(), ids)
                },
                enabled = name.trim().isNotEmpty() && selected.any { it.value }
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun EditNotificationGroupDialog(
    group: com.antechrist.adherentsapp.domain.model.NotificationGroup,
    guardians: List<com.antechrist.adherentsapp.domain.model.Guardian>,
    onDismiss: () -> Unit,
    onSave: (name: String, memberGuardianIds: List<String>) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    val scroll = rememberScrollState()

    // ✅ pré-cocher les membres existants
    LaunchedEffect(group.id) {
        selected.clear()
        group.memberGuardianIds.forEach { gid -> selected[gid] = true }
    }

    val selectedIds = selected.filterValues { it }.keys.toList()
    val canSave = name.trim().isNotEmpty() && selectedIds.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le groupe") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .imePadding()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du groupe") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text("Membres (responsables)", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))

                val list = guardians.sortedWith(
                    compareBy<com.antechrist.adherentsapp.domain.model.Guardian>(
                        { (it.nom ?: "").lowercase() },
                        { (it.prenom ?: "").lowercase() }
                    )
                )

                if (list.isEmpty()) {
                    Text("Aucun responsable disponible.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    list.forEach { g ->
                        val checked = selected[g.id] ?: false

                        val displayName = buildString {
                            val p = (g.prenom ?: "").trim()
                            val n = (g.nom ?: "").trim()
                            if (p.isNotEmpty()) append(p)
                            if (n.isNotEmpty()) {
                                if (isNotEmpty()) append(" ")
                                append(n)
                            }
                            val rel = (g.relation ?: "").trim()
                            if (rel.isNotEmpty()) {
                                if (isNotEmpty()) append(" ")
                                append("($rel)")
                            }
                        }.ifBlank { "Responsable (${g.id})" }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { selected[g.id] = it }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), selectedIds) },
                enabled = canSave
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
