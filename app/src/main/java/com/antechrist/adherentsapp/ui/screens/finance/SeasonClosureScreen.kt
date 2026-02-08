package com.antechrist.adherentsapp.ui.screens.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.ui.theme.extraColors
import com.antechrist.adherentsapp.ui.utils.formatCentsToEuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonClosureScreen(
    seasonKey: String,
    onBack: () -> Unit,
    vm: SeasonClosureViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(seasonKey) { vm.load(seasonKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fin de saison") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddings ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val statutColor =
                when (ui.status) {
                    "OPEN" -> AssistChipDefaults.assistChipColors(MaterialTheme.extraColors.greenTatamie)
                    "CLOSING" -> AssistChipDefaults.assistChipColors(MaterialTheme.extraColors.warning) // si tu l’as
                    "CLOSED" -> AssistChipDefaults.assistChipColors(MaterialTheme.extraColors.rougeTatamie)
                    else -> AssistChipDefaults.assistChipColors()
                }
            Text("Saison : $seasonKey", style = MaterialTheme.typography.titleLarge)
            AssistChip(
                onClick = {},
                label = { Text("Statut saison : ${ui.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extraColors.blanc) },
                colors = statutColor
            )

//            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ui.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            ui.message?.let { Text(it) }

            ui.rollbackInfo?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (ui.canRollbackDev) {
                OutlinedButton(
                    onClick = { vm.reopenSeasonDev() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Réouvrir la saison (DEV)")
                }
            }

            Button(
                onClick = { vm.runCloseSeasonStep1() },
                enabled = !ui.closing && ui.status != "CLOSED",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (ui.closing) "Clôture en cours..." else "Lancer la clôture (Étape 1)")
            }

            Text(
                "Étape 1 : vérifie que toutes les cotisations sont soldées, génère le snapshot, puis passe la saison en CLOSED.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (ui.unpaid.isNotEmpty()) {
                Text("Foyers non soldés (top ${ui.unpaid.size})", style = MaterialTheme.typography.titleSmall)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ui.unpaid, key = { it.guardianId }) { u ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(u.guardianDisplay)
                                Text(
                                    "Reste : ${formatCentsToEuro(u.remainingCents)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else if (ui.status == "CLOSED") {
                ui.snapshotAt?.let {
                    Text("Snapshot : $it", style = MaterialTheme.typography.bodySmall)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Saison clôturée ✅", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
