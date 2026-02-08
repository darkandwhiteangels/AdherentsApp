@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.antechrist.adherentsapp.ui.screens.guardian

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.ui.components.GuardianListItem
import com.antechrist.adherentsapp.ui.theme.extraColors
import kotlinx.coroutines.launch

@Composable
fun GuardianListScreen(
    onBack: () -> Unit,
    onEditGuardian: (String) -> Unit,
    vm: GuardianListViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val screenScope = rememberCoroutineScope()

    var toDelete by remember { mutableStateOf<GuardianUi?>(null) }
    var showConfirm by remember { mutableStateOf(false) }

    var showInviteMenu by remember { mutableStateOf(false) }
    var inviteMenuGuardian by remember { mutableStateOf<GuardianUi?>(null) }

    LaunchedEffect(ui.invitationSuccess) {
        ui.invitationSuccess?.let {
            screenScope.launch { snackbar.showSnackbar(it) }
        }
    }

    LaunchedEffect(ui.error) {
        ui.error?.let { screenScope.launch { snackbar.showSnackbar(it) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.extraColors.dorureDojo.copy(alpha = 0.5f),  // Couleur de fond (ton rouge NavRed)
                    titleContentColor = Color.Black,      // Couleur du titre
                    navigationIconContentColor = Color.Black,  // Couleur de l'icône de navigation
                    actionIconContentColor = Color.Black  // Couleur des icônes d'action
                ),
                title = { Text("Responsables") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        floatingActionButton = {
            if (ui.hasSelection) {
                ExtendedFloatingActionButton(
                    onClick = {
                        screenScope.launch {
                            vm.sendInvitations(ui.selectedIds)
                        }
                    },
                    icon = { Icon(Icons.Filled.Send, contentDescription = null) },
                    text = { Text("Envoyer invitations (${ui.selectedCount})") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddings ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddings)
        ) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::onChangeQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Rechercher (nom, email, téléphone, enfants)…") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            )

            if (ui.isSelectionMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${ui.selectedCount} sélectionné(s)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row {
                            TextButton(onClick = vm::selectAll) {
                                Text("Tout")
                            }
                            TextButton(onClick = vm::clearSelection) {
                                Text("Annuler")
                            }
                        }
                    }
                }
            }

            if (ui.loading && ui.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SwipeToRefreshBox(
                    isRefreshing = ui.loading,
                    onRefresh = { vm.refresh() }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(ui.filtered, key = { it.id }) { g ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    when (value) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            onEditGuardian(g.id)
                                            false
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            toDelete = g
                                            showConfirm = true
                                            false
                                        }
                                        else -> false
                                    }
                                }
                            )
                            val itemScope = rememberCoroutineScope()

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = true,
                                enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                        val isDelete = when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> true
                                            else -> false
                                        }
                                    val bgColor = if (isDelete)
                                        MaterialTheme.colorScheme.errorContainer
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer
                                    val icon = if (isDelete) Icons.Filled.Delete else Icons.Filled.Edit
                                    val label = if (isDelete) "Supprimer" else "Éditer"

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        AssistChip(
                                            onClick = {
                                                itemScope.launch {
                                                    if (isDelete) {
                                                        toDelete = g
                                                        showConfirm = true
                                                    } else {
                                                        onEditGuardian(g.id)
                                                    }
                                                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                                }
                                            },
                                            label = { Text(label) },
                                            leadingIcon = { Icon(icon, contentDescription = null) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = bgColor
                                            )
                                        )
                                    }
                                }
                            },
                                content = {
                                    GuardianListItem(
                                        guardian = g,
                                        onClick = {
                                            if (ui.isSelectionMode) {
                                                vm.toggleSelection(g.id)
                                            } else {
                                                vm.toggleSelection(g.id)
                                            }
                                            itemScope.launch {
                                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                            }
                                        },
                                        onLongClick = {
                                            inviteMenuGuardian = g
                                            showInviteMenu = true
                                        },
                                        isSelected = g.id in ui.selectedIds,
                                        showCheckbox = ui.isSelectionMode,
                                        memberNames = g.memberNames  // 🆕 Passer les prénoms des enfants
                                    )
                                }
                            )
                        }
                        if (ui.filtered.isEmpty()) {
                            item {
                                Box(
                                    Modifier
                                        .fillParentMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Aucun responsable")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirm && toDelete != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer le responsable ?") },
            text = {
                Text(
                    "Cette action est définitive.\n" +
                            "La suppression est possible uniquement si aucun adhérent n'est rattaché " +
                            "à ce responsable (ni comme responsable principal, ni dans la liste des responsables)."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        val gid = toDelete!!.id
                        toDelete = null
                        screenScope.launch {
                            val ok = vm.deleteGuardianSafely(gid)
                            if (ok) snackbar.showSnackbar("Responsable supprimé")
                            else snackbar.showSnackbar("Suppression impossible : responsable rattaché à un ou plusieurs adhérents")
                        }
                    }
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Annuler") }
            }
        )
    }

    if (showInviteMenu && inviteMenuGuardian != null) {
        AlertDialog(
            onDismissRequest = { showInviteMenu = false },
            icon = { Icon(Icons.Filled.MarkEmailUnread, contentDescription = null) },
            title = { Text("Invitation parent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${inviteMenuGuardian?.prenom} ${inviteMenuGuardian?.nom}")

                    Divider()

                    if (inviteMenuGuardian?.email.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Aucun email renseigné",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text("Email: ${inviteMenuGuardian?.email}")
                    }

                    if (!inviteMenuGuardian?.authUid.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Compte déjà activé",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            "Enverra un email avec un lien d'activation valide 30 jours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInviteMenu = false
                        screenScope.launch {
                            vm.sendInvitations(setOf(inviteMenuGuardian!!.id))
                        }
                    },
                    enabled = !inviteMenuGuardian?.email.isNullOrBlank()
                            && inviteMenuGuardian?.authUid.isNullOrBlank()
                ) {
                    Text("Envoyer invitation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteMenu = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun SwipeToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        content()
        if (isRefreshing) {
            // overlay léger (optionnel)
        }
    }
}