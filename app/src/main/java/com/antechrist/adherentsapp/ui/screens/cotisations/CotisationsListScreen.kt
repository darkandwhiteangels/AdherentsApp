package com.antechrist.adherentsapp.ui.screens.cotisations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.domain.model.finance.HouseholdStatus
import java.text.NumberFormat
import java.util.Locale
import com.antechrist.adherentsapp.core.utils.SeasonUtils
import com.antechrist.adherentsapp.ui.theme.extraColors
import com.antechrist.adherentsapp.ui.screens.guardian.GuardianEditScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotisationsListScreen(
    onBack: () -> Unit,
    onOpenHousehold: (guardianId: String) -> Unit,
    onOpenFinanceSeason: (seasonKey: String) -> Unit = {},
    onOpenLicencesReport: (seasonKey: String) -> Unit,
    onOpenCotisationsReport: (seasonKey: String) -> Unit,
    onOpenSeasonClosure: (seasonKey: String) -> Unit,
    // laissé pour compat : on l’appelle aussi quand on ouvre l’overlay
    onOpenGuardians: () -> Unit,
    onEditGuardian: (guardianId: String) -> Unit = {},
    vm: CotisationsListViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val season = ui.seasonKey.ifBlank { SeasonUtils.currentSeasonKey() }

    // 🔹 Overlay local d’édition
    var editGuardianId by remember { mutableStateOf<String?>(null) }

    val appBarGradient = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to MaterialTheme.extraColors.certifiedBlue,
            0.7f to MaterialTheme.extraColors.rougeTatamie,
            1.0f to MaterialTheme.extraColors.rougeTatamie
        ),
        start = Offset(0f, 0f),          // haut-gauche
        end = Offset(800f, 800f)         // bas-droit (diagonale)
    )

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(appBarGradient)
            ) {
                TopAppBar(
                    title = { Text("Cotisations ($season)") },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour"
                            )
                        }
                    },
                    actions = {
                        var open by remember { mutableStateOf(false) }

                        IconButton(onClick = { open = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Actions")
                        }

                        DropdownMenu(
                            expanded = open,
                            onDismissRequest = { open = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Créer dossiers manquants") },
                                onClick = {
                                    open = false
                                    vm.createMissingHouseholds { }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Recalculer montants (non destructif)") },
                                onClick = {
                                    open = false
                                    vm.recalcAmountsForAll { }
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Liste des responsables") },
                                onClick = {
                                    open = false
                                    onOpenGuardians()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Paramètres de la saison…") },
                                onClick = {
                                    open = false
                                    onOpenFinanceSeason(season)
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Rapport cotisations") },
                                onClick = {
                                    open = false
                                    onOpenCotisationsReport(season)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Rapport licences FFK") },
                                onClick = {
                                    open = false
                                    onOpenLicencesReport(season)
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Fin de saison (clôture)") },
                                onClick = {
                                    open = false
                                    onOpenSeasonClosure(season)
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .consumeWindowInsets(paddings)
        ) {
            // Barre de recherche + Clear
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                label = { Text("Rechercher (nom, email, tél.)") },
                singleLine = true,
                trailingIcon = {
                    if (ui.query.isNotBlank()) {
                        IconButton(onClick = { vm.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                }
            )

            if (ui.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val items = vm.filteredItems()
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucun responsable trouvé. Assurez-vous que les adhérents ont un responsable principal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.guardianId }) { item ->
                    HouseholdRow(
                        item = item,
                        onClick = { onOpenHousehold(item.guardianId) },
                        onEdit = {
                            // ➜ Ouvre l’éditeur intégré ET appelle le callback (si utilisé ailleurs)
                            editGuardianId = item.guardianId
                            onEditGuardian(item.guardianId)
                        }
                    )
                }
            }
        }
    }

    // 🔹 Éditeur en overlay (plein écran)
    editGuardianId?.let { gid ->
        GuardianEditScreen(
            guardianId = gid,
            onClose = {
                editGuardianId = null
                // Optionnel : si tu veux rafraîchir la liste après édition :
                // vm.refresh()  // (utilise la méthode réelle de ton VM si elle existe)
            }
        )
    }
}

@Composable
private fun HouseholdRow(
    item: HouseholdUi,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val euro = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }

    val stripeA: Color
    val stripeB: Color
    val chipColors = when (item.status) {
        HouseholdStatus.SOLDE -> {
            stripeA = MaterialTheme.extraColors.greenTatamie
            stripeB = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f)
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.extraColors.greenTatamie,
                labelColor = MaterialTheme.extraColors.blanc
            )
        }
        HouseholdStatus.EN_RETARD -> {
            stripeA = MaterialTheme.extraColors.dorureDojo
            stripeB = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.90f)
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.extraColors.dorureDojo,
                labelColor = MaterialTheme.extraColors.blanc
            )
        }
        HouseholdStatus.A_REGLER -> {
            stripeA = MaterialTheme.extraColors.certifiedBlue
            stripeB = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.90f)
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.extraColors.certifiedBlue,
                labelColor = MaterialTheme.extraColors.blanc
            )
        }
        HouseholdStatus.ANNULE -> {
            stripeA = MaterialTheme.extraColors.rougeTatamie
            stripeB = MaterialTheme.colorScheme.surfaceVariant
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.extraColors.rougeTatamie,
                labelColor = MaterialTheme.extraColors.blanc
            )
        }
        null -> {
            stripeA = MaterialTheme.colorScheme.outlineVariant
            stripeB = MaterialTheme.colorScheme.surfaceVariant
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.extraColors.blanc
            )
        }
    }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
        ) {
            // Liseret
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(stripeA, stripeB)),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Titre + bouton Éditer + badge montant/état
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        item.displayName.ifBlank { "(Responsable inconnu)" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Éditer le responsable")
                    }

                    val badgeText = when {
                        item.amountDueCents != null && item.amountDueCents > 0 ->
                            euro.format(item.amountDueCents / 100.0)
                        item.status == HouseholdStatus.SOLDE -> "À jour"
                        else -> null
                    }
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(25.dp))
                                .background(MaterialTheme.extraColors.chrome)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.extraColors.noir
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 🔹 2ᵉ ligne : icône + nombre + noms/prénoms des adhérents
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "${item.memberCount} adhérent(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Noms/prénoms chacun sur sa propre ligne
                        item.memberNames.forEach { name ->
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val label = when (item.status) {
                        HouseholdStatus.SOLDE -> "Soldé"
                        HouseholdStatus.EN_RETARD -> "En retard"
                        HouseholdStatus.A_REGLER -> "À régler"
                        HouseholdStatus.ANNULE -> "Annulé"
                        null -> "Non évalué"
                    }
                    AssistChip(
                        onClick = { /* no-op */ },
                        label = { Text(label) },
                        colors = chipColors,
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = Color.Transparent
                        )
                    )
                }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    if (!item.telephone.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                item.telephone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!item.email.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                item.email,
                                style = MaterialTheme.typography.bodySmall,
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
}
