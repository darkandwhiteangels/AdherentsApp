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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.core.utils.SeasonUtils
import com.antechrist.adherentsapp.domain.model.finance.HouseholdStatus
import com.antechrist.adherentsapp.ui.components.StatusIconChip
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicencesReportScreen(
    seasonKey: String = SeasonUtils.currentSeasonKey(),
    onBack: () -> Unit,
    vm: LicencesReportViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val euro = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }
    val context = LocalContext.current

    LaunchedEffect(seasonKey) { vm.load(seasonKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rapport licences FFK") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.exportPdf(context) }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exporter PDF")
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
            if (ui.error != null) {
                Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            }

            // Totaux
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Saison : ${ui.seasonKey}", style = MaterialTheme.typography.titleSmall)
                    Text("Total licenciés (filtre) : ${ui.totalLicences}")
                    Text("Total à reverser FFK (filtre) : ${euro.format(ui.totalLicencesAmountCents / 100.0)}")
                }
            }

            // Filtres
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusIconChip(
                    status = HouseholdStatus.A_REGLER,
                    selected = ui.filter == LicencesFilter.A_REGLER,
                    compact = true
                ) {
                    vm.setFilter(LicencesFilter.A_REGLER)
                }

                StatusIconChip(
                    status = HouseholdStatus.SOLDE,
                    selected = ui.filter == LicencesFilter.SOLDE,
                    compact = true
                ) {
                    vm.setFilter(LicencesFilter.SOLDE)
                }

                StatusIconChip(
                    status = HouseholdStatus.EN_RETARD,
                    selected = ui.filter == LicencesFilter.EN_RETARD,
                    compact = true
                ) {
                    vm.setFilter(LicencesFilter.EN_RETARD)
                }

                // Optionnel : "Tous"
                StatusIconChip(
                    status = HouseholdStatus.A_REGLER, // icône neutre
                    selected = ui.filter == LicencesFilter.ALL,
                    labelOverride = "Tous",
                    compact = true
                ) {
                    vm.setFilter(LicencesFilter.ALL)
                }
            }

            if (ui.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val filtered = remember(ui.filter, ui.rows) {
                when (ui.filter) {
                    LicencesFilter.ALL -> ui.rows
                    LicencesFilter.SOLDE -> ui.rows.filter { it.status == HouseholdStatus.SOLDE }
                    LicencesFilter.A_REGLER -> ui.rows.filter { it.status == HouseholdStatus.A_REGLER }
                    LicencesFilter.EN_RETARD -> ui.rows.filter { it.status == HouseholdStatus.EN_RETARD }
                    LicencesFilter.ANNULE -> ui.rows.filter { it.status == HouseholdStatus.ANNULE }
                }
            }

            // Liste
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.guardianId }) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(row.guardianName, style = MaterialTheme.typography.titleSmall)
                            Text("Statut : ${row.status.label()}")
                            Text("Licences : ${row.licenceCount}")
                            Text("À reverser : ${euro.format(row.licencesTotalCents / 100.0)}")
                        }
                    }
                }
            }
        }
    }
}
