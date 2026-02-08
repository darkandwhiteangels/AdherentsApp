package com.antechrist.adherentsapp.ui.screens.bureau

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.ui.theme.extraColors
import com.antechrist.adherentsapp.core.utils.SeasonUtils
import com.antechrist.adherentsapp.domain.model.isAdmin
import com.antechrist.adherentsapp.domain.model.isSuperAdmin
import com.antechrist.adherentsapp.ui.role.RoleViewModel
import com.antechrist.adherentsapp.ui.screens.finance.MinutesForm
import com.antechrist.adherentsapp.ui.screens.finance.SeasonAdminViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonAdminScreen(
    seasonKey: String = SeasonUtils.currentSeasonKey(),
    onBack: () -> Unit,
    adminVm: SeasonAdminViewModel = hiltViewModel(),
) {
    val adminUi by adminVm.ui.collectAsState()
    val scrollState = rememberScrollState()


    val roleVm: RoleViewModel = hiltViewModel()
    val role = roleVm.role.collectAsState().value
    val isSuperAdmin = role?.isSuperAdmin() == true
    val isAdmin = role?.isAdmin() == true

    val context = LocalContext.current

    LaunchedEffect(seasonKey) {
        adminVm.load(seasonKey)
    }

    val now = System.currentTimeMillis()
    val df = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val tf = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }
    val dtf = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.FRANCE) }

    val agMillisCurrent = adminUi.agAt?.toDate()?.time
    val agPassed = agMillisCurrent != null && now >= agMillisCurrent
    val crValidated = adminUi.minutesApprovedAt != null

    // ---- (Optionnel) récupérer Nom/Prénom depuis users/{uid} pour affichage ----
    val db = remember { FirebaseFirestore.getInstance() }
    var approvedByName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(adminUi.minutesApprovedByUid) {
        val uid = adminUi.minutesApprovedByUid ?: run {
            approvedByName = null
            return@LaunchedEffect
        }
        try {
            val snap = db.collection("users").document(uid).get().await()
            val m = snap.data
            val displayName = m?.get("displayName") as? String
            val firstName = m?.get("firstName") as? String
            val lastName = m?.get("lastName") as? String
            val composed = listOfNotNull(firstName, lastName).joinToString(" ").trim()

            approvedByName = when {
                false -> displayName
                composed.isNotBlank() -> composed
                else -> uid
            }
        } catch (_: Exception) {
            approvedByName = uid
        }
    }

    // ---- Pickers : Date / Heure séparés + bouton Enregistrer ----
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }

    var pendingDateMillis by remember(agMillisCurrent) {
        mutableLongStateOf(agMillisCurrent ?: now)
    }
    var pendingHour by remember(agMillisCurrent) {
        val cal = Calendar.getInstance().apply { timeInMillis = agMillisCurrent ?: now }
        mutableLongStateOf(cal.get(Calendar.HOUR_OF_DAY).toLong())
    }
    var pendingMinute by remember(agMillisCurrent) {
        val cal = Calendar.getInstance().apply { timeInMillis = agMillisCurrent ?: now }
        mutableLongStateOf(cal.get(Calendar.MINUTE).toLong())
    }

    fun combinedAgMillis(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = pendingDateMillis
        cal.set(Calendar.HOUR_OF_DAY, pendingHour.toInt())
        cal.set(Calendar.MINUTE, pendingMinute.toInt())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AG / Compte-rendu") },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Header infos
            Text(
                text = "Saison : $seasonKey",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = { Text(if (agPassed) "AG passée" else "AG à venir") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (agPassed)
                            MaterialTheme.extraColors.greenTatamie
                        else
                            MaterialTheme.extraColors.rougeTatamie,
                        labelColor = MaterialTheme.extraColors.blanc
                    )
                )
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = { Text(if (crValidated) "CR validé" else "CR non validé") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (crValidated)
                            MaterialTheme.extraColors.greenTatamie
                        else
                            MaterialTheme.extraColors.rougeTatamie,
                        labelColor = MaterialTheme.extraColors.blanc
                    )
                )
            }

            // ===================== AG =====================
            Text("Assemblée Générale (AG)", style = MaterialTheme.typography.titleSmall)

            Text(
                text = if (adminUi.agAt != null) {
                    "Date/heure : ${dtf.format(adminUi.agAt!!.toDate())}"
                } else {
                    "Date/heure : non définie"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Choisir la date") }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Choisir l’heure") }
            }

            val previewMillis = combinedAgMillis()
            val previewText = dtf.format(java.util.Date(previewMillis))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Aperçu date/heure sélectionnée", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        previewText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Tu peux changer date et heure avant d’enregistrer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { adminVm.saveAgDate(seasonKey, combinedAgMillis()) },
                enabled = seasonKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer") }

            HorizontalDivider()

            // ===================== CR COMPLET (FORMULAIRE) =====================
            Text("Compte Rendu (CR) – contenu", style = MaterialTheme.typography.titleSmall)

            var trainingsSummary by remember(adminUi.minutesForm.trainingsSummary) { mutableStateOf(adminUi.minutesForm.trainingsSummary) }
            var technicalFocus by remember(adminUi.minutesForm.technicalFocus) { mutableStateOf(adminUi.minutesForm.technicalFocus) }
            var eventsMultiline by remember(adminUi.minutesForm.eventsList) {
                mutableStateOf(adminUi.minutesForm.eventsList.joinToString("\n"))
            }
            var sportAssessment by remember(adminUi.minutesForm.sportAssessment) { mutableStateOf(adminUi.minutesForm.sportAssessment) }
            var financialStatus by remember(adminUi.minutesForm.financialStatus) { mutableStateOf(adminUi.minutesForm.financialStatus) }
            var financialComment by remember(adminUi.minutesForm.financialComment) { mutableStateOf(adminUi.minutesForm.financialComment) }
            var issuesSummary by remember(adminUi.minutesForm.issuesSummary) { mutableStateOf(adminUi.minutesForm.issuesSummary) }
            var nextSeasonGoals by remember(adminUi.minutesForm.nextSeasonGoals) { mutableStateOf(adminUi.minutesForm.nextSeasonGoals) }
            var generalConclusion by remember(adminUi.minutesForm.generalConclusion) { mutableStateOf(adminUi.minutesForm.generalConclusion) }

            OutlinedTextField(
                value = trainingsSummary,
                onValueChange = { trainingsSummary = it },
                label = { Text("Entraînements assurés") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = technicalFocus,
                onValueChange = { technicalFocus = it },
                label = { Text("Axes de travail") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = eventsMultiline,
                onValueChange = { eventsMultiline = it },
                label = { Text("Événements (1 ligne = 1 événement)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = sportAssessment,
                onValueChange = { sportAssessment = it },
                label = { Text("Bilan sportif") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = financialStatus,
                onValueChange = { financialStatus = it },
                label = { Text("Bilan financier – situation") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = financialComment,
                onValueChange = { financialComment = it },
                label = { Text("Bilan financier – commentaire") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = issuesSummary,
                onValueChange = { issuesSummary = it },
                label = { Text("Difficultés rencontrées") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = nextSeasonGoals,
                onValueChange = { nextSeasonGoals = it },
                label = { Text("Perspectives saison suivante") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = generalConclusion,
                onValueChange = { generalConclusion = it },
                label = { Text("Conclusion") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            val parsedEvents = eventsMultiline
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            LaunchedEffect(Unit) {
                adminVm.debugSave()
            }

            Button(
                onClick = {
                    adminVm.saveMinutesForm(
                        seasonKey = adminUi.seasonKey,
                        form = MinutesForm(
                            trainingsSummary = trainingsSummary,
                            technicalFocus = technicalFocus,
                            eventsList = parsedEvents,
                            sportAssessment = sportAssessment,
                            financialStatus = financialStatus,
                            financialComment = financialComment,
                            issuesSummary = issuesSummary,
                            nextSeasonGoals = nextSeasonGoals,
                            generalConclusion = generalConclusion
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !adminUi.loading && adminUi.seasonKey.isNotBlank() && adminUi.agAt != null
            ) { Text("Enregistrer le CR") }

            adminUi.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            adminUi.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            // ===================== PDF =====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { adminVm.generateDraftPdfShortAndShare(context) },
                    enabled = adminUi.agAt != null && adminUi.canGeneratePdf(System.currentTimeMillis()) && !adminUi.loading,
                    modifier = Modifier.weight(1f)
                ) { Text("PDF synthèse") }

                OutlinedButton(
                    onClick = { adminVm.generateDraftPdfFullAndShare(context) },
                    enabled = adminUi.agAt != null && adminUi.canGeneratePdf(System.currentTimeMillis()) && !adminUi.loading,
                    modifier = Modifier.weight(1f)
                ) { Text("PDF complet") }
            }

            // ===================== CR =====================
            Text("Compte Rendu (CR)", style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
//                Button(
//                    onClick = { adminVm.generateDraftPdfAndShare(context) },
//                    enabled = adminUi.agAt != null && adminUi.canGeneratePdf(now),
//                    modifier = Modifier.weight(1f)
//                ) { Text("Générer PDF") }

                OutlinedButton(
                    onClick = { showApproveDialog = true },
                    enabled = adminUi.agAt != null && adminUi.canApprove(now) && (isSuperAdmin || isAdmin) && !crValidated,
                    modifier = Modifier.weight(1f)
                ) { Text("Valider") }
            }



            if (adminUi.minutesApprovedAt != null) {
                Text(
                    "Validé le : ${dtf.format(adminUi.minutesApprovedAt!!.toDate())} par ${adminUi.minutesApprovedByName ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ===================== Dialogs (AG) =====================
            if (showDatePicker) {
                val dateState = rememberDatePickerState(initialSelectedDateMillis = pendingDateMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                            val selected = dateState.selectedDateMillis
                            if (selected != null) pendingDateMillis = selected
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
                    }
                ) { DatePicker(state = dateState) }
            }

            if (showTimePicker) {
                val timeState = rememberTimePickerState(
                    initialHour = pendingHour.toInt(),
                    initialMinute = pendingMinute.toInt(),
                    is24Hour = true
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showTimePicker = false
                            pendingHour = timeState.hour.toLong()
                            pendingMinute = timeState.minute.toLong()
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Annuler") }
                    },
                    title = { Text("Choisir l’heure") },
                    text = { TimePicker(state = timeState) }
                )
            }

            // ===================== Dialog validation CR =====================
            if (showApproveDialog) {
                AlertDialog(
                    onDismissRequest = { showApproveDialog = false },
                    title = { Text("Valider le compte-rendu") },
                    text = {
                        Text(
                            "Validation possible uniquement après la date de l'AG.\n\n" +
                                    "La validation est autorisé par le Président ou le Secrétaire uniquement.",
                            textAlign = TextAlign.Start
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showApproveDialog = false
                            adminVm.approveDraft(if (isSuperAdmin) "SUPERADMIN" else "ADMIN")
                        }) { Text("Valider") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showApproveDialog = false }) { Text("Annuler") }
                    }
                )
            }
        }
    }
}
