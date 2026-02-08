package com.antechrist.adherentsapp.ui.screens.cotisations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.painter.Painter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.domain.model.Role
import com.antechrist.adherentsapp.ui.role.RoleViewModel
import com.antechrist.adherentsapp.domain.model.finance.HouseholdStatus
import com.antechrist.adherentsapp.domain.model.finance.PaymentMethod
import com.antechrist.adherentsapp.domain.model.finance.AidType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.core.net.toUri
import android.content.Intent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import com.antechrist.adherentsapp.ui.theme.extraColors
import java.text.NumberFormat
import java.util.Locale
import com.antechrist.adherentsapp.ui.screens.guardian.GuardianEditScreen
import com.antechrist.adherentsapp.R
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotisationHouseholdScreen(
    guardianId: String,
    seasonKey: String,
    onBack: () -> Unit,
    // 🔹 Ouvre l’éditeur responsable depuis l’en-tête + callback optionnel
    onEditGuardian: (guardianId: String) -> Unit = {},
    vm: CotisationHouseholdViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val euro = remember { NumberFormat.getCurrencyInstance(Locale.FRANCE) }
    val context = LocalContext.current

    // Rôle (gating des attestations)
    val roleVm: RoleViewModel = hiltViewModel()
    LaunchedEffect(Unit) { roleVm.refresh() }
    val currentRole by roleVm.role.collectAsState()
    val canSendAttestations = remember(currentRole) { currentRole.canSendCertificates() }

    // Etats dialogues/editions
    var confirmDeletePlan by remember { mutableStateOf<Int?>(null) }      // index chèque 1..3
    var confirmDeleteReceived by remember { mutableStateOf<Int?>(null) }  // position liste reçus
    var editPlanIndex by remember { mutableStateOf<Int?>(null) }          // index chèque en édition
    var editAmountInput by remember { mutableStateOf("") }

    // 🔹 Remise manuelle
    var showManualDiscountDialog by remember { mutableStateOf(false) }
    var manualDiscountInput by remember { mutableStateOf("") }
    var manualDiscountReasonInput by remember { mutableStateOf("") }

    var editReceivedPos by remember { mutableStateOf<Int?>(null) }
    var editReceivedAmount by remember { mutableStateOf("") }
    var editReceivedDate by remember { mutableStateOf("") }
    var editReceivedMethod by remember { mutableStateOf<PaymentMethod?>(null) }

    // 🔹 Overlay éditeur de responsable
    var showEditGuardian by remember { mutableStateOf(false) }

    val familyName = remember(ui.guardianName) {
        val full = ui.guardianName.orEmpty().trim()
        val lastWord = full.split(Regex("\\s+")).lastOrNull().orEmpty()
        if (lastWord.isNotBlank()) lastWord.uppercase() else "(RESPONSABLE)"
    }

    val displaySeason = remember(ui.seasonKey) { formatSeasonShort(ui.seasonKey) }

    // Chargement des données
    LaunchedEffect(guardianId, seasonKey) { vm.load(guardianId, seasonKey) }

    Scaffold(
        topBar = {
            TopAppBar(
//                title = { Text("Famille $familyName • ${ui.seasonKey}") },
                title = { Text("Famille $familyName • $displaySeason") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddings ->

        if (ui.loading) {
            Box(Modifier.fillMaxSize().padding(paddings), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        ui.error?.let {
            Box(Modifier.fillMaxSize().padding(paddings), contentAlignment = Alignment.Center) {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        // ----- LISTE -----
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddings),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // En-tête responsable
            item {
                StripeCard(
                    stripe = stripeColorsForStatus(ui.status),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // --- Colonne gauche ---
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Avatar + Nom complet
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = CircleShape,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = initialsFromName(ui.guardianName.orEmpty()),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    ui.guardianName.ifBlank { "(Responsable)" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            // Nb d’adhérents du foyer
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text("${ui.memberIds.size} adhérent(s)", style = MaterialTheme.typography.bodyMedium)
                            }

                            // Email cliquable
                            if (!ui.guardianEmail.isNullOrBlank()) {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_SENDTO,
                                            "mailto:${ui.guardianEmail}".toUri()
                                        )
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(Icons.Filled.Email, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(ui.guardianEmail!!)
                                }
                            }
                        }

                        // --- Colonne droite ---
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .height(IntrinsicSize.Min)
                        ) {
                            // Bouton édition (en haut)
                            IconButton(onClick = {
                                showEditGuardian = true
                                onEditGuardian(guardianId)
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Éditer le responsable")
                            }

                            Spacer(Modifier.weight(1f))

                            // Bouton Appel (en bas)
                            FilledTonalIconButton(
                                onClick = {
                                    if (!ui.guardianTel.isNullOrBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, "tel:${ui.guardianTel}".toUri())
                                        context.startActivity(intent)
                                    }
                                },
                                enabled = !ui.guardianTel.isNullOrBlank()
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = "Appeler")
                            }
                        }
                    }
                }
            }


            // Résumé montants (aides déduites + remise manuelle + exonération + licence traçable)
            item {
                StripeCard(
                    stripe = stripeColorsForStatus(ui.status),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        // --- Montants ---
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cotisation (Licence incluse)")
                                Text(euro.format(ui.amountBaseCents / 100.0))
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remise 1 cours / sem.")
                                Text("- " + euro.format(ui.discountCents / 100.0))
                            }

                            if (ui.controlledDiscountCents > 0L) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Remises:")
                                    Text("- " + euro.format(ui.controlledDiscountCents / 100.0))
                                }
                            }

                            // ✅ Remises contrôlées (affichage si activées)
                            val controlledDiscountLabels = buildList {
                                if (ui.discountBlackBeltEnabled) add("Ceinture noire")
                                if (ui.discountFamilyGradedEnabled) add("Famille gradés")
                                if (ui.discountAssistantProfEnabled) add("Jeune assistant prof")
                            }
                            if (
                                ui.discountBlackBeltEnabled ||
                                ui.discountFamilyGradedEnabled ||
                                ui.discountAssistantProfEnabled
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        "Détail :",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (ui.discountBlackBeltEnabled) {
                                        Text(
                                            "¤ Ceinture noire",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (ui.discountFamilyGradedEnabled) {
                                        Text(
                                            "¤ Famille gradés",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (ui.discountAssistantProfEnabled) {
                                        Text(
                                            "¤ Jeune assistant",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }


                            if (ui.manualDiscountCents > 0L) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Remise spéciale (manuelle)")
                                    Text("- " + euro.format(ui.manualDiscountCents / 100.0))
                                }
                                if (ui.manualDiscountReason.isNotBlank()) {
                                    Text(
                                        ui.manualDiscountReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Aides")
                                Text("- " + euro.format(ui.aidsTotalCents / 100.0))
                            }

                            HorizontalDivider()

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total à payer (foyer)")
                                Text(
                                    euro.format(ui.amountDueCents / 100.0),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Déjà réglé")
                                Text(euro.format(ui.totalPaidCents / 100.0))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Reste à payer")
                                Text(
                                    euro.format(ui.remainingCents / 100.0),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            // Traçabilité licence (toujours) — compta FFK
                            Text(
                                "Traçabilité FFK: ${ui.licenceCount} × ${euro.format(ui.licenceAmountCents / 100.0)} = ${euro.format(ui.licencesTotalCents / 100.0)} à reverser à la FFK",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (ui.exemptFromFee) {
                                Text(
                                    "Exonéré de cotisation : la licence est incluse dans le tarif et reste comptée dans l’effectif.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // --- Options ---
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            // 1 cours / semaine
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 24.dp) {
                                    Switch(
                                        checked = ui.oneClassPerWeek,
                                        onCheckedChange = { vm.toggleOneClassPerWeek(it) },
                                        modifier = Modifier.scale(0.7f),
                                        enabled = !ui.exemptFromFee // optionnel : grise si exonéré
                                    )
                                }
                                Text("1 cours / sem. (−30 € / foyer)")
                            }

                            // Exonération (membre du bureau / prof)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = ui.exemptFromFee,
                                    onCheckedChange = { vm.toggleExemptFromFee(it) }
                                )
                                Column {
                                    Text("Membre du bureau / prof (exonéré cotisation)")
                                    Text(
                                        "La licence reste comptée dans l’effectif (licence incluse).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // ✅ Remises contrôlées (cases à cocher)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Remises contrôlées", style = MaterialTheme.typography.titleSmall)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = ui.discountBlackBeltEnabled,
                                        onCheckedChange = { vm.toggleBlackBeltDiscount(it) },
                                        enabled = !ui.exemptFromFee
                                    )
                                    Text("Ceinture noire")
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = ui.discountFamilyGradedEnabled,
                                        onCheckedChange = { vm.toggleFamilyGradedDiscount(it) },
                                        enabled = !ui.exemptFromFee
                                    )
                                    Text("Famille gradés")
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = ui.discountAssistantProfEnabled,
                                        onCheckedChange = { vm.toggleAssistantProfDiscount(it) },
                                        enabled = !ui.exemptFromFee
                                    )
                                    Text("Jeune assistant prof")
                                }

                                Text(
                                    "Ces remises s’appliquent sur la cotisation (licence incluse).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Remise spéciale (manuelle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text("Remise spéciale")
                                    Text(
                                        "Remise exceptionnelle avec motif (traçable).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        manualDiscountInput =
                                            if (ui.manualDiscountCents > 0L) (ui.manualDiscountCents / 100.0).toString() else ""
                                        manualDiscountReasonInput = ui.manualDiscountReason
                                        showManualDiscountDialog = true
                                    },
                                    modifier = Modifier.widthIn(min = 140.dp)
                                ) {
                                    Text(if (ui.manualDiscountCents > 0L) "Modifier" else "Ajouter")
                                }
                            }
                        }

                        Text(
                            "Les aides et remises diminuent le montant à payer. La licence est incluse dans les tarifs et reste comptée pour la FFK.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }


            // AIDES / SUBVENTIONS — Liste + saisie
            item {
                StripeCard(
                    stripe = stripeColorsForStatus(ui.status),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Aides / subventions", style = MaterialTheme.typography.titleMedium)

                        // Liste des aides déjà saisies
                        if (ui.aids.isEmpty()) {
                            Text(
                                "Aucune aide saisie.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ui.aids.forEachIndexed { index, aid ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val label = aidLabel(aid.type)
                                        val code = aid.code?.let { " ($it)" }.orEmpty()
                                        Text("$label$code — ${euro.format(aid.amountCents / 100.0)}")
                                        IconButton(onClick = { vm.removeAidAt(index) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                                        }
                                    }
                                }
                            }
                        }

                        // Formulaire de saisie d'une aide
                        AidForm(
                            selected = ui.aidType,
                            //code = ui.aidCode,
                            amountCents = ui.aidAmountCents,
                            onType = vm::setAidType,
                            //onCode = vm::setAidCode,
                            onAmount = vm::setAidAmountCents,
                            onAdd = vm::addAid
                        )
                    }
                }
            }

            // Saisie règlement
            item {
                StripeCard(
                    stripe = stripeColorsForStatus(ui.status),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Saisir un règlement", style = MaterialTheme.typography.titleMedium)
                        // Choix méthode
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MethodIconChip(
                                painter = painterResource(methodIconRes(PaymentMethod.CHQ)),
                                contentDesc = "Chèque",
                                selected = ui.selectedMethod == PaymentMethod.CHQ
                            ) { vm.setSelectedMethod(PaymentMethod.CHQ) }

                            MethodIconChip(
                                painter = painterResource(methodIconRes(PaymentMethod.VIR)),
                                contentDesc = "Virement",
                                selected = ui.selectedMethod == PaymentMethod.VIR
                            ) { vm.setSelectedMethod(PaymentMethod.VIR) }

                            MethodIconChip(
                                painter = painterResource(methodIconRes(PaymentMethod.ESP)),
                                contentDesc = "Espèces",
                                selected = ui.selectedMethod == PaymentMethod.ESP
                            ) { vm.setSelectedMethod(PaymentMethod.ESP) }

                            MethodIconChip(
                                painter = painterResource(methodIconRes(PaymentMethod.CB)),
                                contentDesc = "Carte bancaire",
                                selected = ui.selectedMethod == PaymentMethod.CB
                            ) { vm.setSelectedMethod(PaymentMethod.CB) }
                        }

                        when (ui.selectedMethod) {
                            PaymentMethod.CHQ -> ChequeForm(
                                ui = ui,
                                onAmount = vm::setInputAmountCents,
                                onCount = vm::setChequeCount,
                                onValidate = {
                                    val amt = ui.inputAmountCents ?: 0L
                                    vm.addChequePlan(amt)
                                }
                            )
                            PaymentMethod.VIR -> SimplePaymentForm(
                                label = "Virement",
                                ui = ui,
                                onAmount = vm::setInputAmountCents,
                                onDate = vm::setInputDateIso,
                                withDate = true,
                                onValidate = {
                                    val amt = ui.inputAmountCents ?: 0L
                                    vm.addReceived(PaymentMethod.VIR, amt, ui.inputDateIso)
                                }
                            )
                            PaymentMethod.ESP -> SimplePaymentForm(
                                label = "Espèces",
                                ui = ui,
                                onAmount = vm::setInputAmountCents,
                                onDate = vm::setInputDateIso,
                                withDate = true,
                                onValidate = {
                                    val amt = ui.inputAmountCents ?: 0L
                                    vm.addReceived(PaymentMethod.ESP, amt, ui.inputDateIso)
                                }
                            )
                            PaymentMethod.CB -> SimplePaymentForm(
                                label = "Carte bancaire",
                                ui = ui,
                                onAmount = vm::setInputAmountCents,
                                withDate = false,
                                onValidate = {
                                    val amt = ui.inputAmountCents ?: 0L
                                    vm.addReceived(PaymentMethod.CB, amt, null)
                                }
                            )
                            null -> {
                                Text(
                                    "Choisir une méthode ci-dessus puis saisir le montant.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Paiements reçus
            if (ui.paymentsReceived.isNotEmpty()) {
                item {
                    StripeCard(
                        stripe = stripeColorsForStatus(ui.status),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Paiements reçus", style = MaterialTheme.typography.titleMedium)
                            ui.paymentsReceived.forEachIndexed { idx, pe ->
                                val label = when (pe.type) {
                                    PaymentMethod.VIR -> "Virement"
                                    PaymentMethod.ESP -> "Espèces"
                                    PaymentMethod.CB  -> "CB"
                                    PaymentMethod.CHQ -> "Chèque"
                                }
                                val date = pe.date?.let { " ($it)" } ?: ""
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$label$date — ${euro.format(pe.amountCents / 100.0)}")
                                    Row {
                                        IconButton(onClick = {
                                            editReceivedPos = idx
                                            editReceivedMethod = pe.type
                                            editReceivedAmount = (pe.amountCents / 100.0).toString()
                                            editReceivedDate = pe.date ?: ""
                                        }) { Icon(Icons.Filled.Edit, contentDescription = "Éditer") }
                                        IconButton(onClick = { confirmDeleteReceived = idx }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chèques planifiés
            if (ui.paymentsPlan.isNotEmpty()) {
                item {
                    StripeCard(
                        stripe = stripeColorsForStatus(ui.status),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Chèques planifiés", style = MaterialTheme.typography.titleMedium)
                            ui.paymentsPlan.sortedBy { it.index ?: 0 }.forEach { pe ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Chèque ${pe.index ?: "?"} — ${euro.format(pe.amountCents / 100.0)}")
                                    Row {
                                        IconButton(onClick = {
                                            editPlanIndex = pe.index
                                            editAmountInput = (pe.amountCents / 100.0).toString()
                                        }) { Icon(Icons.Filled.Edit, contentDescription = "Éditer") }
                                        IconButton(onClick = { pe.index?.let { confirmDeletePlan = it } }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Statut dossier
            item {
                StripeCard(
                    stripe = stripeColorsForStatus(ui.status),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Statut du dossier", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusIconChip(
                                status = HouseholdStatus.A_REGLER,
                                selected = ui.status == HouseholdStatus.A_REGLER
                            ) { vm.setStatusManual(HouseholdStatus.A_REGLER) }

                            StatusIconChip(
                                status = HouseholdStatus.EN_RETARD,
                                selected = ui.status == HouseholdStatus.EN_RETARD
                            ) { vm.setStatusManual(HouseholdStatus.EN_RETARD) }

                            StatusIconChip(
                                status = HouseholdStatus.SOLDE,
                                selected = ui.status == HouseholdStatus.SOLDE
                            ) { vm.setStatusManual(HouseholdStatus.SOLDE) }

                            StatusIconChip(
                                status = HouseholdStatus.ANNULE,
                                selected = ui.status == HouseholdStatus.ANNULE
                            ) { vm.setStatusManual(HouseholdStatus.ANNULE) }
                        }
                        Text(
                            "Règle : soldé si (chèques planifiés + paiements reçus) ≥ montant dû.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Attestations (si SOLDÉ + rôle autorisé)
            if (ui.status == HouseholdStatus.SOLDE && canSendAttestations) {
                item {
                    StripeCard(
                        stripe = stripeColorsForStatus(ui.status),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Attestation de paiement", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = {
                                    vm.generateAndEmailAttestations(
                                        context = context,
                                        mode = CotisationHouseholdViewModel.AttestationMode.HOUSEHOLD
                                    )
                                }) { Text("Foyer (total)") }

                                Button(onClick = {
                                    vm.generateAndEmailAttestations(
                                        context = context,
                                        mode = CotisationHouseholdViewModel.AttestationMode.PER_MEMBER
                                    )
                                }) { Text("Par adhérent") }
                            }
                            Text(
                                "Astuce : la répartition “par adhérent” se fait au pro-rata simple (part égale par membre).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- Dialogues ---

        // Supprimer paiement reçu
        confirmDeleteReceived?.let { pos ->
            AlertDialog(
                onDismissRequest = { confirmDeleteReceived = null },
                title = { Text("Supprimer le paiement ?") },
                confirmButton = {
                    TextButton(onClick = {
                        vm.removeReceivedAt(pos)
                        confirmDeleteReceived = null
                    }) { Text("Supprimer") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteReceived = null }) { Text("Annuler") }
                }
            )
        }

        // Supprimer chèque planifié
        confirmDeletePlan?.let { idx ->
            AlertDialog(
                onDismissRequest = { confirmDeletePlan = null },
                title = { Text("Supprimer le chèque $idx ?") },
                confirmButton = {
                    TextButton(onClick = {
                        vm.removePlanCheque(idx)
                        confirmDeletePlan = null
                    }) { Text("Supprimer") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeletePlan = null }) { Text("Annuler") }
                }
            )
        }

        // Éditer chèque planifié
        editPlanIndex?.let { idx ->
            AlertDialog(
                onDismissRequest = { editPlanIndex = null },
                title = { Text("Modifier chèque $idx") },
                text = {
                    OutlinedTextField(
                        value = editAmountInput,
                        onValueChange = { editAmountInput = it },
                        label = { Text("Montant (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val cents = editAmountInput.toCentsOrNull() ?: 0L
                        vm.editPlanCheque(idx, cents)
                        editPlanIndex = null
                    }) { Text("Enregistrer") }
                },
                dismissButton = {
                    TextButton(onClick = { editPlanIndex = null }) { Text("Annuler") }
                }
            )
        }

        // Éditer paiement reçu
        editReceivedPos?.let { pos ->
            AlertDialog(
                onDismissRequest = { editReceivedPos = null },
                title = { Text("Modifier le paiement") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MethodChip("Virement", editReceivedMethod == PaymentMethod.VIR) { editReceivedMethod = PaymentMethod.VIR }
                            MethodChip("Espèces", editReceivedMethod == PaymentMethod.ESP) { editReceivedMethod = PaymentMethod.ESP }
                            MethodChip("CB", editReceivedMethod == PaymentMethod.CB) { editReceivedMethod = PaymentMethod.CB }
                        }
                        OutlinedTextField(
                            value = editReceivedAmount,
                            onValueChange = { editReceivedAmount = it },
                            label = { Text("Montant (€)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            )
                        )

                        // Date requise seulement pour VIR/ESP
                        val needDate = editReceivedMethod == PaymentMethod.VIR || editReceivedMethod == PaymentMethod.ESP
                        if (needDate) {
                            // Par défaut: aujourd’hui si vide
                            LaunchedEffect(editReceivedMethod) {
                                if (editReceivedDate.isBlank()) {
                                    editReceivedDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                }
                            }
                            DatePickerField(
                                value = editReceivedDate.ifBlank { null },
                                onValueChange = { editReceivedDate = it ?: "" },
                                label = "Date"
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val method = editReceivedMethod ?: PaymentMethod.VIR
                        val cents = editReceivedAmount.toCentsOrNull() ?: 0L
                        val date = if (method == PaymentMethod.VIR || method == PaymentMethod.ESP) editReceivedDate.ifBlank { null } else null
                        vm.editReceivedAt(pos, method, cents, date)
                        editReceivedPos = null
                    }) { Text("Enregistrer") }
                },
                dismissButton = {
                    TextButton(onClick = { editReceivedPos = null }) { Text("Annuler") }
                }
            )
        }

        // Remise manuelle (montant + motif)
        if (showManualDiscountDialog) {
            AlertDialog(
                onDismissRequest = { showManualDiscountDialog = false },
                title = { Text("Remise manuelle") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = manualDiscountInput,
                            onValueChange = { manualDiscountInput = it },
                            label = { Text("Montant (€)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                        OutlinedTextField(
                            value = manualDiscountReasonInput,
                            onValueChange = { manualDiscountReasonInput = it },
                            label = { Text("Motif (obligatoire si montant > 0)") },
                            singleLine = false
                        )
                        Text(
                            "Astuce : mets 0€ pour supprimer la remise.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val cents = manualDiscountInput.toCentsOrNull() ?: 0L
                        vm.setManualDiscount(cents, manualDiscountReasonInput)
                        showManualDiscountDialog = false
                    }) { Text("Valider") }
                },
                dismissButton = {
                    TextButton(onClick = { showManualDiscountDialog = false }) { Text("Annuler") }
                }
            )
        }
    }

    // 🔹 Overlay éditeur de responsable (plein écran)
    if (showEditGuardian) {
        GuardianEditScreen(
            guardianId = guardianId,
            onClose = {
                showEditGuardian = false
                // Rafraîchir l’en-tête (nom, email, tel) après une modification
                vm.load(guardianId, seasonKey)
            }
        )
    }
}

// ---------- UI helpers ----------

@Composable
private fun MethodChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}

@Composable
private fun StatusRadio(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun ChequeForm(
    ui: HouseholdDetailUi,
    onAmount: (Long?) -> Unit,
    onCount: (Int) -> Unit,
    onValidate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = ui.inputAmountCents?.let { (it / 100.0).toString() } ?: "",
            onValueChange = { onAmount(it.toCentsOrNull()) },
            label = { Text("Montant total (€)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nombre de chèques : ${ui.chequeCount}")

            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedIconButton(
                        onClick = { onCount((ui.chequeCount - 1).coerceAtLeast(1)) },
                        modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Filled.Remove, contentDescription = "Moins") }

                    OutlinedIconButton(
                        onClick = { onCount((ui.chequeCount + 1).coerceAtMost(3)) },
                        modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Filled.Add, contentDescription = "Plus") }
                }
            }
        }

        Button(onClick = onValidate, enabled = (ui.inputAmountCents ?: 0L) > 0L) {
            Text("Générer ${ui.chequeCount} chèque(s)")
        }
        Text(
            "Les chèques sont planifiés (de 1 à 3). Ils comptent dans le calcul du statut mais ne sont pas “reçus”.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SimplePaymentForm(
    label: String,
    ui: HouseholdDetailUi,
    onAmount: (Long?) -> Unit,
    onDate: ((String?) -> Unit)? = null,
    withDate: Boolean,
    onValidate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = ui.inputAmountCents?.let { (it / 100.0).toString() } ?: "",
            onValueChange = { onAmount(it.toCentsOrNull()) },
            label = { Text("$label — Montant (€)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            )
        )
        if (withDate) {
            // Si rien saisi, on initialise automatiquement à aujourd’hui (ISO: yyyy-MM-dd)
            LaunchedEffect(ui.inputDateIso) {
                if (ui.inputDateIso.isNullOrBlank()) {
                    onDate?.invoke(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                }
            }

            DatePickerField(
                value = ui.inputDateIso,
                onValueChange = { onDate?.invoke(it) },
                label = "Date"
            )
        }
        Button(onClick = onValidate, enabled = (ui.inputAmountCents ?: 0L) > 0L) {
            Text("Ajouter")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AidForm(
    selected: AidType?,
    //code: String,
    amountCents: Long?,
    onType: (AidType?) -> Unit,
    //onCode: (String) -> Unit,
    onAmount: (Long?) -> Unit,
    onAdd: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                value = selected?.let { aidLabel(it) } ?: "Sélectionner un type d’aide",
                onValueChange = {},
                label = { Text("Type d’aide") }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AidType.entries.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(aidLabel(t)) },
                        onClick = {
                            onType(t)
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = amountCents?.let { (it / 100.0).toString() } ?: "",
            onValueChange = { onAmount(it.toCentsOrNull()) },
            label = { Text("Montant (€)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            )
        )

        Button(
            onClick = onAdd,
            enabled = selected != null && (amountCents ?: 0L) > 0L
        ) {
            Text("Ajouter cette aide")
        }
    }
}
@DrawableRes
private fun statusIconRes(status: HouseholdStatus): Int = when (status) {
    HouseholdStatus.A_REGLER -> R.drawable.ic_status_due
    HouseholdStatus.EN_RETARD -> R.drawable.ic_status_late
    HouseholdStatus.SOLDE    -> R.drawable.ic_status_paid
    HouseholdStatus.ANNULE   -> R.drawable.ic_status_cancel
}

private fun statusContentDesc(status: HouseholdStatus): String = when (status) {
    HouseholdStatus.A_REGLER -> "À régler"
    HouseholdStatus.EN_RETARD -> "En retard"
    HouseholdStatus.SOLDE    -> "Soldé"
    HouseholdStatus.ANNULE   -> "Annulé"
}

@Composable
private fun StatusIconChip(
    status: HouseholdStatus,
    selected: Boolean,
    labelOverride: String? = null,
    onClick: () -> Unit
) {
    val label = labelOverride ?: status.label()
    val painter = painterResource(id = statusIconRes(status))
    val tintColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            // Boîte fixe pour uniformiser la taille d’affichage des images
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = statusContentDesc(status),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,         // conserve le ratio, s’adapte à 24dp
                    colorFilter = ColorFilter.tint(tintColor) // retire ce paramètre si tu ne veux PAS de teinte
                )
            }
        }
    )
}

@DrawableRes
private fun methodIconRes(method: PaymentMethod): Int = when (method) {
    PaymentMethod.CHQ -> R.drawable.ic_pay_cheque
    PaymentMethod.VIR -> R.drawable.ic_pay_transfer
    PaymentMethod.ESP -> R.drawable.ic_pay_cash
    PaymentMethod.CB  -> R.drawable.ic_pay_card
}

@Composable
private fun MethodIconChip(
    painter: Painter,
    contentDesc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            // PNG/SVG déjà “noir” → pas de teinte forcée
            Icon(
                painter = painter,
                contentDescription = contentDesc,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
        }
    )
}

private fun String.toCentsOrNull(): Long? {
    val cleaned = replace("€", "").replace(" ", "").trim()
    if (cleaned.isBlank()) return null
    val norm = cleaned.replace(",", ".")
    return runCatching {
        (norm.toBigDecimal() * BigDecimal(100)).toLong()
    }.getOrNull()
}

private fun aidLabel(t: AidType): String = when (t) {
    AidType.PASS_SPORT -> "Pass'Sport"
    AidType.CAF        -> "CAF / Aide sociale"
    AidType.AUTRE      -> "Autre aide"
}

private fun Role?.canSendCertificates(): Boolean =
    this == Role.SUPER_ADMIN || this == Role.ADMIN || this == Role.SECRETAIRE

private fun formatSeasonShort(season: String?): String {
    if (season.isNullOrBlank()) return ""
    val rg = Regex("""^\s*(\d{4})\s*[-/]\s*(\d{4})\s*$""")
    val m = rg.find(season) ?: return season
    val (y1, y2) = m.destructured
    return "${y1.takeLast(2)}/${y2.takeLast(2)}"
}

@Composable
private fun StripeCard(
    stripe: Pair<Color, Color>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Liseret vertical à gauche
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(stripe.first, stripe.second)),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            // Contenu
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                content = content
            )
        }
    }
}

private fun lighten(color: Color, fraction: Float): Color =
    lerp(color, Color.White, fraction.coerceIn(0f, 1f))

private fun darken(color: Color, fraction: Float): Color =
    lerp(color, Color.Black, fraction.coerceIn(0f, 1f))

@Composable
private fun lightenTowardSurface(color: Color, fraction: Float): Color =
    lerp(color, MaterialTheme.colorScheme.surface, fraction.coerceIn(0f, 1f))

private fun initialsFromName(full: String?): String {
    val parts = full.orEmpty().trim()
        .replace(Regex("\\s+"), " ")
        .split(" ")
        .filter { it.isNotBlank() }

    if (parts.isEmpty()) return "?"

    val first = parts.first().firstOrNull()
    val last  = parts.last().firstOrNull()

    return when {
        first == null && last == null -> "?"
        parts.size == 1 || last == null -> first!!.uppercase()
        else -> "${first?.uppercaseChar()}${last.uppercaseChar()}"
    }
}


/** Dégradé du liseret en fonction du statut (mêmes couleurs que HouseholdRow) */
@Composable
private fun stripeColorsForStatus(status: HouseholdStatus?): Pair<Color, Color> = when (status) {
    HouseholdStatus.SOLDE -> MaterialTheme.extraColors.greenTatamie to
            lighten(MaterialTheme.extraColors.greenTatamie, 0.40f)
//            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f)

    HouseholdStatus.EN_RETARD -> MaterialTheme.extraColors.dorureDojo to
//             MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.90f)
            lighten(MaterialTheme.extraColors.dorureDojo, 0.40f) // ~20% plus clair
    // darken(MaterialTheme.colorScheme.tertiaryContainer, 0.20f) // 20% plus sombre

    HouseholdStatus.A_REGLER -> MaterialTheme.extraColors.certifiedBlue to
//            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.90f)
            lighten(MaterialTheme.extraColors.certifiedBlue, 0.40f)

    HouseholdStatus.ANNULE -> MaterialTheme.extraColors.rougeTatamie to
//            MaterialTheme.colorScheme.surfaceVariant
            lighten(MaterialTheme.extraColors.rougeTatamie, 0.40f)

    null -> MaterialTheme.colorScheme.outlineVariant to
            MaterialTheme.colorScheme.surfaceVariant
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    value: String?,                       // ISO en entrée (yyyy-MM-dd) ou null
    onValueChange: (String?) -> Unit,     // ISO en sortie (yyyy-MM-dd) ou null
    label: String
) {
    var open by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val fmtIso = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val fmtUi  = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    // Date initiale du picker (value ISO -> millis) sinon aujourd’hui
    val initialMillis = remember(value) {
        val date = try {
            if (!value.isNullOrBlank()) LocalDate.parse(value, fmtIso) else today
        } catch (_: Exception) {
            today
        }
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    // Valeur affichée en FR
    val displayValue = remember(value) {
        val d = try {
            if (!value.isNullOrBlank()) LocalDate.parse(value, fmtIso) else today
        } catch (_: Exception) { today }
        d.format(fmtUi)
    }

    OutlinedTextField(
        value = displayValue,            // 👈 affichage FR
        onValueChange = {},              // readOnly
        label = { Text(label) },
        singleLine = true,
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Choisir une date")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    val selected = millis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    } ?: today
                    onValueChange(selected.format(fmtIso))  // 👈 renvoie ISO
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Annuler") } }
        ) {
            DatePicker(state = state)
        }
    }
}