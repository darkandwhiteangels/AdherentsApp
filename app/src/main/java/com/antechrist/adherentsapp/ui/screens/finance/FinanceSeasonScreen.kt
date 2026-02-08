package com.antechrist.adherentsapp.ui.screens.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.R
import com.antechrist.adherentsapp.domain.model.finance.FinanceSeasonConfig
import com.antechrist.adherentsapp.domain.model.isAdmin
import com.antechrist.adherentsapp.domain.model.isSuperAdmin
import com.antechrist.adherentsapp.ui.role.RoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceSeasonScreen(
    seasonKey: String,
    onBack: () -> Unit,
    onOpenAgCr: () -> Unit = {}, // ✅ nouveau : navigation vers AG/CR
    onChange: (FinanceSeasonConfig) -> Unit = {},
    vm: FinanceSeasonViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val roleVm: RoleViewModel = hiltViewModel()
    val role = roleVm.role.collectAsState().value
    val isSuperAdmin = role?.isSuperAdmin() == true
    val isAdmin = role?.isAdmin() == true

    var showLockDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }

    // cfg local reflétant ui.config ; remis à zéro quand ui.config change
    var cfg by remember(ui.config) { mutableStateOf(ui.config) }
    val locked = cfg.locked

    LaunchedEffect(ui.saved) {
        if (ui.saved) snackbarHostState.showSnackbar("Configuration enregistrée ✔")
    }

    LaunchedEffect(seasonKey) { vm.load(seasonKey) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Config saison $seasonKey") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    val canTogglePricingLock = isSuperAdmin || isAdmin
                    if (canTogglePricingLock) {
                        IconButton(
                            onClick = {
                                if (locked) showUnlockDialog = true else showLockDialog = true
                            },
                            enabled = !locked || isSuperAdmin // déverrouiller : superadmin only
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (locked) R.drawable.ic_lock_closed else R.drawable.ic_lock_open
                                ),
                                contentDescription = if (locked) "Tarifs verrouillés" else "Tarifs modifiables"
                            )
                        }
                    }
                }
            )
        }
    ) { paddings ->
        if (ui.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddings),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        ui.error?.let {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddings),
                contentAlignment = Alignment.Center
            ) { Text(it, color = MaterialTheme.colorScheme.error) }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (locked) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Saison verrouillée", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Les montants ne peuvent plus être modifiés. Déverrouillage réservé au SuperAdmin.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ===================== Tarification =====================

            Text("Cotisations licence inclus", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CentsField(
                    valueCents = cfg.priceAdultCents,
                    label = "Adultes +17",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(priceAdultCents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )
                CentsField(
                    valueCents = cfg.priceChild6to16Cents,
                    label = "Enfants 6–16",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(priceChild6to16Cents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )
                CentsField(
                    valueCents = cfg.priceBabyUnder6Cents,
                    label = "Baby < 6",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(priceBabyUnder6Cents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CentsField(
                    valueCents = cfg.bundle2AllOver5Cents,
                    label = "Forfait 2 ( > 5 ans)",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(bundle2AllOver5Cents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )
                CentsField(
                    valueCents = cfg.bundle3AllOver5Cents,
                    label = "Forfait 3 ( > 5 ans)",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(bundle3AllOver5Cents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )
            }

            HorizontalDivider()

            // ===================== Remises =====================

            Text("Remises contrôlées", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CentsField(
                    valueCents = cfg.oneClassPerWeekDiscountCents,
                    label = "1 cours / semaine",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(oneClassPerWeekDiscountCents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )
                CentsField(
                    valueCents = cfg.discountBlackBeltCents,
                    label = "Ceinture noire",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(discountBlackBeltCents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CentsField(
                    valueCents = cfg.discountFamilyGradedCents,
                    label = "Famille gradés",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(discountFamilyGradedCents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )
                CentsField(
                    valueCents = cfg.discountAssistantProfCents,
                    label = "Jeune assistant prof",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(discountAssistantProfCents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )
            }

            HorizontalDivider()

            // ===================== Licence =====================

            Text("Prix Licence FFK", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CentsField(
                    valueCents = cfg.licenceAmountCents,
                    label = "Montant licence",
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    onChangeCents = {
                        cfg = cfg.copy(licenceAmountCents = it)
                        vm.update(cfg); onChange(cfg)
                    }
                )

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = cfg.licencePaidByClub,
                        onCheckedChange = {
                            cfg = cfg.copy(licencePaidByClub = it)
                            vm.update(cfg); onChange(cfg)
                        },
                        enabled = !locked
                    )
                    Text("Payée par le club", style = MaterialTheme.typography.bodySmall)
                }
            }

            val datePattern = Regex("""^\d{4}-\d{2}-\d{2}$""")
            val dateError = cfg.defaultDueDate?.let { it.isNotBlank() && !datePattern.matches(it) } ?: false

            OutlinedTextField(
                value = cfg.defaultDueDate.orEmpty(),
                onValueChange = {
                    val v = it.take(10)
                    cfg = cfg.copy(defaultDueDate = v.ifBlank { null })
                    vm.update(cfg); onChange(cfg)
                },
                isError = dateError,
                supportingText = { if (dateError) Text("Format attendu : yyyy-MM-dd") },
                label = { Text("Échéance (yyyy-MM-dd)") },
                singleLine = true,
                enabled = !locked,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { vm.save() },
                enabled = !locked,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer") }

            if (locked) {
                AssistChip(onClick = { }, label = { Text("Tarifs verrouillés") })
                if (isSuperAdmin) {
                    OutlinedButton(
                        onClick = { showUnlockDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Déverrouiller") }
                }
            }

            if (ui.saved) {
                Text("Sauvegardé ✔", color = MaterialTheme.colorScheme.primary)
            }

            HorizontalDivider()

            // ===================== AG / CR (redirigé vers Bureau) =====================

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("AG / Compte-rendu", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Accès au module Assemblée Générale et Compte-rendu (draft + validation).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenAgCr,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ouvrir") }
                }
            }
        }

        // ===================== Dialogs lock/unlock =====================

        if (showLockDialog) {
            AlertDialog(
                onDismissRequest = { showLockDialog = false },
                title = { Text("Verrouiller la saison ?") },
                text = {
                    Text(
                        "Une fois verrouillée, la configuration tarifaire ne pourra plus être modifiée.\n\n" +
                                "Le déverrouillage restera possible uniquement par un Super Admin."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLockDialog = false
                        vm.lockSeason()
                    }) { Text("Verrouiller") }
                },
                dismissButton = {
                    TextButton(onClick = { showLockDialog = false }) { Text("Annuler") }
                }
            )
        }

        if (showUnlockDialog) {
            AlertDialog(
                onDismissRequest = { showUnlockDialog = false },
                title = { Text("Déverrouiller la saison ?") },
                text = {
                    Text(
                        "Attention : déverrouiller permet de modifier les montants.\n\n" +
                                "Recommandation : ne déverrouiller que pour corriger une erreur, puis reverrouiller."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showUnlockDialog = false
                        vm.unlockSeason()
                    }) { Text("Déverrouiller") }
                },
                dismissButton = {
                    TextButton(onClick = { showUnlockDialog = false }) { Text("Annuler") }
                }
            )
        }
    }
}

/* ===================== Helpers monétaires ===================== */

@Composable
private fun CentsField(
    modifier: Modifier = Modifier,
    valueCents: Long,
    onChangeCents: (Long) -> Unit,
    label: String,
    enabled: Boolean = true
) {
    var text by remember(valueCents) { mutableStateOf(centsToEditableEuros(valueCents)) }
    val parsed = parseEurosToCentsOrNull(text)
    val isError = parsed == null

    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            parseEurosToCentsOrNull(new)?.let(onChangeCents)
        },
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        enabled = enabled,
        modifier = modifier,
        trailingIcon = {
            Text(
                "€",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

private fun centsToEditableEuros(cents: Long): String {
    val bd = java.math.BigDecimal(cents).divide(java.math.BigDecimal(100))
    return bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
}

private fun parseEurosToCentsOrNull(raw: String): Long? {
    val cleaned = raw.replace("€", "")
        .replace(" ", "")
        .trim()
        .replace(",", ".")
    if (cleaned.isBlank()) return null
    return runCatching {
        (java.math.BigDecimal(cleaned) * java.math.BigDecimal(100))
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .toLong()
    }.getOrNull()
}
