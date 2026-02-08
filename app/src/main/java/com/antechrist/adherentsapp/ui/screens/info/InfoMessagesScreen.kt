package com.antechrist.adherentsapp.ui.screens.info

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.domain.model.Audience
import com.antechrist.adherentsapp.ui.components.InfoMessageCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoMessagesScreen(
    onBack: () -> Unit,
    onNavigateToGroups: () -> Unit,
    currentUserIsAdultPraticiant: Boolean,
    currentUserIsGuardian: Boolean,
    currentUserCanPublish: Boolean,
    currentUserUid: String,
    vm: InfoMessagesViewModel = hiltViewModel()
) {
    LaunchedEffect(currentUserUid, currentUserIsAdultPraticiant, currentUserIsGuardian, currentUserCanPublish) {
        android.util.Log.d(
            "InfoScreen",
            "InfoMessagesScreen props { uid=$currentUserUid, adult=$currentUserIsAdultPraticiant, guardian=$currentUserIsGuardian, canPublish=$currentUserCanPublish }"
        )
    }

    val state by vm.uiState.collectAsState()

    LaunchedEffect(
        currentUserIsAdultPraticiant,
        currentUserIsGuardian,
        currentUserCanPublish
    ) {
        val aud: Set<Audience> = if (currentUserCanPublish) {
            // Admin/SuperAdmin : voit tout
            setOf(
                Audience.ALL_REGISTERED,
                Audience.ADULTS_ONLY,
                Audience.GUARDIANS,
                Audience.CUSTOM_GROUPS
            )
        } else {
            // Utilisateur normal : voit ce qui le concerne
            buildSet {
                add(Audience.ALL_REGISTERED)
                if (currentUserIsAdultPraticiant) add(Audience.ADULTS_ONLY)
                if (currentUserIsGuardian) add(Audience.GUARDIANS)
            }
        }

        vm.setUserPermissions(
            UserPermissionsUi(
                audienceSet = aud,
                canPublish = currentUserCanPublish
            )
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Infos club") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("<") }
                },
                actions = { // ✅ PAS de @Composable ici
                    if (state.canPublish) {
                        IconButton(onClick = onNavigateToGroups) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Groupes de diffusion",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors()
            )
        },
        floatingActionButton = {
            if (state.canPublish) {
                FloatingActionButton(onClick = { vm.openNewDialog() }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nouveau message")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(
                        contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 88.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                ) {
                    items(items = state.messages, key = { it.id }) { msg ->
                        // ⚠️ compile seulement si InfoMessageCard a bien ces paramètres
                        InfoMessageCard(
                            msg = msg,
                            modifier = Modifier.padding(bottom = 12.dp),
                            canEdit = state.canPublish,
                            canDelete = state.canPublish,
                            onEdit = { vm.openEditDialog(msg) },
                            onDelete = { vm.deleteMessage(msg.id) }
                        )
                    }
                }
            }
        }
    }

    if (state.showNewDialog) {
        NewInfoMessageDialog(
            onDismiss = { vm.closeNewDialog() },
            availableGroups = state.groups,
            onSubmit = { title, body, audience, priority, color, targetGroupIds ->
                vm.submitNewMessage(
                    title = title,
                    body = body,
                    audience = audience,
                    priority = priority,
                    color = color,
                    targetGroupIds = targetGroupIds,
                    createdByUid = currentUserUid
                )
            }
        )
    }


    if (state.showEditDialog && state.selectedMessage != null) {
        EditMessageDialog(
            message = state.selectedMessage!!,
            onDismiss = { vm.closeEditDialog() },
            onConfirm = { title, body ->
                vm.updateMessage(
                    messageId = state.selectedMessage!!.id,
                    title = title,
                    body = body,
                    editorUid = currentUserUid
                )
            }
        )
    }
}
