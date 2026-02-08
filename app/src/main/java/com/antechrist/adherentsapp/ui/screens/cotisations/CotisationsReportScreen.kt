package com.antechrist.adherentsapp.ui.screens.cotisations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.ui.theme.extraColors
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotisationsReportScreen(
    seasonKey: String,
    onBack: () -> Unit,
    vm: CotisationsReportViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(seasonKey) { vm.load(seasonKey) }

    val euro = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }
    fun eur(cents: Long) = euro.format(cents / 100.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Rapport cotisations",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "→ $seasonKey ←",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.exportCsvAndShare(context) }) {
                        Icon(Icons.Default.Download, contentDescription = "Exporter CSV")
                    }
                    IconButton(onClick = { vm.exportPdfAndShare(context) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exporter PDF")
                    }
                    IconButton(onClick = { vm.generateSnapshot() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Générer snapshot")
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

            if (ui.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            AssistChip(
                onClick = {},
                label = {
                    Text(
                        if (ui.fromSnapshot) "Snapshot chargé (ts=${ui.snapshotAt ?: "-"})"
                        else "Mode live (pas de snapshot)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.extraColors.certifiedBlue
                    )
                }
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(ui.clubName.ifBlank { "Club" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                    Text("Foyers : ${ui.households.size}")
                    Text("Total dû : ${eur(ui.households.sumOf { it.dueCents })}")
                    Text("Total payé : ${eur(ui.households.sumOf { it.paidCents })}")
                    Text("Total restant : ${eur(ui.households.sumOf { it.remainingCents })}")
                }
            }

            if (ui.households.isEmpty()) {
                Text("Aucun foyer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(ui.households, key = { it.guardianId }) { h ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(h.guardianDisplay, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Membres: ${h.membersCount} • Statut: ${h.status?.name ?: "-"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Dû: ${eur(h.dueCents)} • Payé: ${eur(h.paidCents)} • Reste: ${eur(h.remainingCents)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.extraColors.rougeTatamie
                            )
                        }
                    }
                }
            }
        }
    }
}
