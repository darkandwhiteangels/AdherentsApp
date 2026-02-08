package com.antechrist.adherentsapp.ui.screens.whatsapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.ui.res.painterResource
import com.antechrist.adherentsapp.R
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.ui.theme.extraColors
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppParentsScreen(
    onBack: () -> Unit,
    vm: WhatsAppParentsViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = remember(context) { context.getSystemService(ClipboardManager::class.java) }
    val density = LocalDensity.current

    var tabIndex by remember { mutableIntStateOf(0) } // 0 = SMS, 1 = Email

    // SMS
    var smsMessage by remember {
        mutableStateOf("Bonjour, voici le groupe WhatsApp du club OKES pour la saison 2025-2026 : ")
    }
    var currentBatchIndex by remember { mutableIntStateOf(0) }

    // Email
    var emailSubject by remember { mutableStateOf("Groupe WhatsApp du club") }
    var emailBody by remember { mutableStateOf("Bonjour,\n\nVoici le lien du groupe WhatsApp du club :\n\n") }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.copiedMessage) {
        val msg = state.copiedMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
        vm.clearCopiedMessage()
    }

    LaunchedEffect(state.selectedIds) { currentBatchIndex = 0 }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.extraColors.greenTatamie,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = { Text("Création Groupes") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        enabled = state.parents.isNotEmpty(),
                        onClick = {
                            val txt = vm.buildTexteLisible()
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Liste parents", txt))
                            vm.onCopiedListeLisible()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copier la liste des parents")
                    }

                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPaddings ->

        // ✅ IMPORTANT : pas de padding(16.dp) ici, sinon tu crées un "jour" entre tabs et card.
        Column(
            modifier = Modifier
                .padding(innerPaddings)
                .fillMaxSize()
        ) {
            //Spacer(modifier = Modifier.height(8.dp))

            val indicatorColor = if (tabIndex == 0) {
                MaterialTheme.extraColors.rougeTatamie
            } else {
                MaterialTheme.extraColors.certifiedBlue
            }

            // Mesure réelle de la hauteur du TabRow (pour placer le badge pile sur la jonction)
            var tabRowHeightDp by remember { mutableStateOf(48.dp) }
            val badgeSize = 64.dp

            // ✅ Bloc : TabRow + contenu COLLÉS + badge en overlay au-dessus des 2
            Box(modifier = Modifier.fillMaxWidth()) {

                // 1) Colonne "structure" : TabRow puis contenu (collés, sans espace)
                Column(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                ) {

                    TabRow(
                        selectedTabIndex = tabIndex,
                        containerColor = MaterialTheme.extraColors.chrome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { size ->
                                tabRowHeightDp = with(density) { size.height.toDp() }
                            },
                        indicator = { tabPositions ->
                            val current = tabPositions[tabIndex]
                            val w = current.width
                            TabRowDefaults.Indicator(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[tabIndex])
                                    .height(4.dp)
                                    .wrapContentSize(Alignment.BottomCenter)
                                    .width(w * 0.80f),
                                color = indicatorColor
                            )
                        }
                    ) {
                        Tab(
                            selected = tabIndex == 0,
                            onClick = { tabIndex = 0 },
                            text = {
                                Text(
                                    "SMS",
                                    color = if (tabIndex == 0) MaterialTheme.extraColors.rougeTatamie
                                    else MaterialTheme.extraColors.noir
                                )
                            }
                        )
                        Tab(
                            selected = tabIndex == 1,
                            onClick = { tabIndex = 1 },
                            text = {
                                Text(
                                    "Email",
                                    color = if (tabIndex == 1) MaterialTheme.extraColors.certifiedBlue
                                    else MaterialTheme.extraColors.noir
                                )
                            }
                        )
                    }

                    // 2) Contenu sous les tabs : AUCUN Spacer ici → collé.
                    when {
                        state.isLoading -> {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        state.errorMessage != null -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Erreur",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = state.errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Button(onClick = { vm.refresh() }) {
                                        Text("Réessayer")
                                    }
                                }
                            }
                        }

                        else -> {
                            if (tabIndex == 0) {
                                SmsTabContent(
                                    state = state,
                                    vm = vm,
                                    smsMessage = smsMessage,
                                    onSmsMessageChange = { smsMessage = it },
                                    currentBatchIndex = currentBatchIndex,
                                    onBatchIndexChange = { currentBatchIndex = it },
                                    // 👇 on passe la place nécessaire pour que le badge ne masque pas le contenu
                                    topPaddingForBadge = (badgeSize / 2) + 14.dp
                                )
                            } else {
                                EmailTabContent(
                                    state = state,
                                    vm = vm,
                                    emailSubject = emailSubject,
                                    onEmailSubjectChange = { emailSubject = it },
                                    emailBody = emailBody,
                                    onEmailBodyChange = { emailBody = it },
                                    topPaddingForBadge = (badgeSize / 2) + 14.dp
                                )
                            }
                        }
                    }
                }

                // 3) Badge overlay : pile sur la jonction TabRow / Card (au-dessus des 3 éléments)
                // Son CENTRE est posé sur la ligne du bas du TabRow.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = tabRowHeightDp - (badgeSize / 2))
                        .zIndex(50f)
                ) {
                    ParentsCountBadge(count = state.parents.size)
                }
            }
        }
    }
}

/**
 * ✅ NOTE : ici j’ai juste ajouté un paramètre topPaddingForBadge
 * pour éviter que le badge recouvre ton titre/champs.
 * Le reste de ta logique est identique.
 */
@Composable
private fun SmsTabContent(
    state: WhatsAppParentsUiState,
    vm: WhatsAppParentsViewModel,
    smsMessage: String,
    onSmsMessageChange: (String) -> Unit,
    currentBatchIndex: Int,
    onBatchIndexChange: (Int) -> Unit,
    topPaddingForBadge: Dp
) {
    val context = LocalContext.current

    val allCount = state.parents.size
    val selectedCount = state.selectedIds.size
    val allSelected = allCount > 0 && selectedCount == allCount

    val batches = remember(state.selectedIds, state.parents) { vm.buildSmsBatches(batchSize = 20) }
//    val totalBatches = batches.size

    // Card collée aux tabs (aucun offset, aucun spacer au-dessus)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp, vertical = 8.dp
                )
        ) {
            val totalBatches = batches.size
            val hasBatches = totalBatches > 0
            val canSend = !state.isLoading && state.selectedIds.isNotEmpty() && hasBatches

            // ✅ Header 2 lignes :
            // Ligne 1 : Titre centré + icône envoyer à droite
            // Ligne 2 : <  Lot X/Y  > centré sous le titre (si plusieurs lots)
            Column(modifier = Modifier.fillMaxWidth()) {
                // Ligne 1
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SMS grouper",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center),
                        maxLines = 1
                    )

                    IconButton(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        enabled = canSend,
                        onClick = {
                            val safeIndex =
                                if (currentBatchIndex in batches.indices) currentBatchIndex else 0

                            onBatchIndexChange(safeIndex)
                            val recipients = batches[safeIndex]

                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "smsto:$recipients".toUri()
                                putExtra("sms_body", smsMessage.trim())
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_planer_send),
                            contentDescription = "Envoyer",
                            modifier = Modifier.size(28.dp),
                            tint = if (canSend)
                                MaterialTheme.extraColors.certifiedBlue
                            else
                                MaterialTheme.extraColors.chrome.copy(alpha = 0.60f)
                        )
                    }
                }

                // Ligne 2 (centrée) : < Lot x/y >
                if (totalBatches > 1) {
                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            enabled = currentBatchIndex > 0,
                            onClick = { onBatchIndexChange((currentBatchIndex - 1).coerceAtLeast(0)) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Précédent"
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Lot ${currentBatchIndex + 1}/$totalBatches",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        IconButton(
                            enabled = currentBatchIndex < totalBatches - 1,
                            onClick = { onBatchIndexChange((currentBatchIndex + 1).coerceAtMost(totalBatches - 1)) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Suivant"
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = smsMessage,
                onValueChange = onSmsMessageChange,
                label = { Text("Votre message SMS") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                minLines = 2
            )

            Spacer(Modifier.height(8.dp))

            // ✅ Ligne compacte "Tout" + "X/Y" entre message et liste
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable(enabled = state.parents.isNotEmpty()) {
                            vm.setSelectAll(!allSelected)
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked -> vm.setSelectAll(checked) },
                        enabled = state.parents.isNotEmpty()
                    )
                    Text(
                        text = "Tout",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$selectedCount/$allCount",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp) // optionnel
    ) {
        items(items = state.parents, key = { it.id }) { p ->
            ParentRow(
                p = p,
                selected = state.selectedIds.contains(p.id),
                onToggle = { vm.toggleSelect(p.id) }
            )
        }
    }
}

// --- Modèles ---
enum class EmailTemplate(val label: String) {
    LIBRE("Libre"),
    LIEN_WHATSAPP("Lien WhatsApp"),
    INFO_CLUB("Info club"),
    CHANGEMENT("Changement entraînement"),
    AG_CONVOC("Convocation AG")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailTabContent(
    state: WhatsAppParentsUiState,
    vm: WhatsAppParentsViewModel,
    emailSubject: String,
    onEmailSubjectChange: (String) -> Unit,
    emailBody: String,
    onEmailBodyChange: (String) -> Unit,
    topPaddingForBadge: Dp
) {
    val context = LocalContext.current

    // --- Sélection ---
    val allCount = state.parents.size
    val selectedCount = state.selectedIds.size
    val allSelected = allCount > 0 && selectedCount == allCount

    // --- Emails sélectionnés (BCC) ---
    val selectedEmails = remember(state.parents, state.selectedIds) {
        state.parents
            .asSequence()
            .filter { state.selectedIds.contains(it.id) }
            .map { it.email.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    // --- Expéditeur depuis season_admin/{seasonKey}.emailSenderName ---
    var senderName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            val seasonKey = com.antechrist.adherentsapp.domain.utils.SeasonUtils.currentSeasonKey()
            val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("season_admin")
                .document(seasonKey)
                .get()
                .await()
            senderName = (doc.getString("emailSenderName") ?: "").trim()
        } catch (_: Exception) {
            senderName = ""
        }
    }

    var template by remember { mutableStateOf(EmailTemplate.LIBRE) }

    // UX : on choisit le modèle, puis on n’affiche plus le dropdown (bouton Changer)
    var choosingTemplate by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }

    // --- Champs (selon modèle) ---
    var whatsappLink by remember { mutableStateOf("") }

    var infoMessage by remember { mutableStateOf("") }

    var changeDayOrDate by remember { mutableStateOf("") }  // obligatoire
    var changeTime by remember { mutableStateOf("") }       // optionnel
    var changePlace by remember { mutableStateOf("") }      // optionnel
    var changeReason by remember { mutableStateOf("") }     // optionnel
    var changeExtra by remember { mutableStateOf("") }      // optionnel

    var agDate by remember { mutableStateOf("") }           // obligatoire
    var agTime by remember { mutableStateOf("") }           // obligatoire
    var agPlace by remember { mutableStateOf("") }          // obligatoire
    var agAgenda by remember { mutableStateOf("") }         // optionnel
    var agExtra by remember { mutableStateOf("") }          // optionnel

    val clubName = "OKES"
    val seasonKey = com.antechrist.adherentsapp.domain.utils.SeasonUtils.currentSeasonKey()

    fun signatureBlock(): String {
        val sign = senderName.trim()
        return buildString {
            append("\n\n—\n")
            if (sign.isNotBlank()) append(sign).append("\n")
            append("$clubName – Saison $seasonKey")
        }
    }

    fun renderSubject(): String = when (template) {
        EmailTemplate.LIBRE -> emailSubject.trim()
        EmailTemplate.LIEN_WHATSAPP -> "Groupe WhatsApp $clubName – saison $seasonKey"
        EmailTemplate.INFO_CLUB -> "Info club $clubName"
        EmailTemplate.CHANGEMENT -> "Changement entraînement – $clubName"
        EmailTemplate.AG_CONVOC -> "Convocation AG – $clubName"
    }

    fun renderBody(): String = when (template) {
        EmailTemplate.LIBRE -> emailBody.trim()

        EmailTemplate.LIEN_WHATSAPP -> buildString {
            append("Bonjour,\n\n")
            append("Pour faciliter les informations du club, voici le lien du groupe WhatsApp des parents :\n")
            append(whatsappLink.trim())
            append("\n\n")
            append("Merci de rejoindre le groupe uniquement si vous êtes responsable légal d’un adhérent.")
            append(signatureBlock())
        }

        EmailTemplate.INFO_CLUB -> buildString {
            append("Bonjour,\n\n")
            append(infoMessage.trim())
            append(signatureBlock())
        }

        EmailTemplate.CHANGEMENT -> buildString {
            append("Bonjour,\n\n")
            append("Changement pour l’entraînement du ").append(changeDayOrDate.trim()).append(" :\n")

            val t = changeTime.trim()
            val p = changePlace.trim()
            val r = changeReason.trim()

            if (t.isNotBlank()) append("- Horaire : ").append(t).append("\n")
            if (p.isNotBlank()) append("- Lieu : ").append(p).append("\n")
            if (r.isNotBlank()) append("- Motif : ").append(r).append("\n")

            val extra = changeExtra.trim()
            if (extra.isNotBlank()) {
                append("\n").append(extra).append("\n")
            }

            append("\nMerci de votre compréhension.")
            append(signatureBlock())
        }

        EmailTemplate.AG_CONVOC -> buildString {
            append("Bonjour,\n\n")
            append("Vous êtes convoqués à l’Assemblée Générale du club ").append(clubName).append(".\n\n")
            append("Date : ").append(agDate.trim()).append("\n")
            append("Heure : ").append(agTime.trim()).append("\n")
            append("Lieu : ").append(agPlace.trim()).append("\n")

            val agenda = agAgenda.trim()
            if (agenda.isNotBlank()) {
                append("\nOrdre du jour :\n")
                append(agenda).append("\n")
            }

            val extra = agExtra.trim()
            if (extra.isNotBlank()) {
                append("\n").append(extra).append("\n")
            }

            append("\nMerci de confirmer votre présence.")
            append(signatureBlock())
        }
    }

    fun isFormValid(): Boolean {
        if (state.isLoading) return false
        if (selectedEmails.isEmpty()) return false

        return when (template) {
            EmailTemplate.LIBRE -> (renderSubject().isNotBlank() || renderBody().isNotBlank())
            EmailTemplate.LIEN_WHATSAPP -> whatsappLink.trim().isNotBlank()
            EmailTemplate.INFO_CLUB -> infoMessage.trim().isNotBlank()
            EmailTemplate.CHANGEMENT -> changeDayOrDate.trim().isNotBlank()
            EmailTemplate.AG_CONVOC -> agDate.trim().isNotBlank() && agTime.trim().isNotBlank() && agPlace.trim().isNotBlank()
        }
    }

    val canSendEmail = isFormValid()

    // Scroll interne du formulaire (pour ne jamais perdre la liste)
    val formScroll = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(max = 380.dp)
                .verticalScroll(formScroll)
        ) {
            // Header : titre centré + envoyer à droite
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Email grouper",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                    maxLines = 1
                )

                IconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enabled = canSendEmail,
                    onClick = {
                        val subject = renderSubject()
                        val body = renderBody()

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "message/rfc822"
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, body)
                            putExtra(Intent.EXTRA_BCC, selectedEmails.toTypedArray())
                        }
                        context.startActivity(Intent.createChooser(intent, "Envoyer email"))
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_planer_send),
                        contentDescription = "Envoyer",
                        modifier = Modifier.size(22.dp),
                        tint = if (canSendEmail)
                            MaterialTheme.extraColors.certifiedBlue
                        else
                            MaterialTheme.extraColors.chrome.copy(alpha = 0.60f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Choix modèle / Changer
            if (choosingTemplate) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = template.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Modèle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        EmailTemplate.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = {
                                    expanded = false
                                    template = t
                                    choosingTemplate = false
                                }
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Modèle : ${template.label}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { choosingTemplate = true }) {
                        Text("Changer")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ✅ Ici : champs uniquement (sauf Libre)
            when (template) {
                EmailTemplate.LIBRE -> {
                    OutlinedTextField(
                        value = emailSubject,
                        onValueChange = onEmailSubjectChange,
                        label = { Text("Objet") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emailBody,
                        onValueChange = onEmailBodyChange,
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6
                    )
                }

                EmailTemplate.LIEN_WHATSAPP -> {
                    OutlinedTextField(
                        value = whatsappLink,
                        onValueChange = { whatsappLink = it },
                        label = { Text("Lien WhatsApp (obligatoire)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                EmailTemplate.INFO_CLUB -> {
                    OutlinedTextField(
                        value = infoMessage,
                        onValueChange = { infoMessage = it },
                        label = { Text("Information (obligatoire)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6
                    )
                }

                EmailTemplate.CHANGEMENT -> {
                    OutlinedTextField(
                        value = changeDayOrDate,
                        onValueChange = { changeDayOrDate = it },
                        label = { Text("Jour / Date (obligatoire)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = changeTime,
                        onValueChange = { changeTime = it },
                        label = { Text("Horaire (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = changePlace,
                        onValueChange = { changePlace = it },
                        label = { Text("Lieu (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = changeReason,
                        onValueChange = { changeReason = it },
                        label = { Text("Motif (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = changeExtra,
                        onValueChange = { changeExtra = it },
                        label = { Text("Complément (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                EmailTemplate.AG_CONVOC -> {
                    OutlinedTextField(
                        value = agDate,
                        onValueChange = { agDate = it },
                        label = { Text("Date (obligatoire)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = agTime,
                        onValueChange = { agTime = it },
                        label = { Text("Heure (obligatoire)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = agPlace,
                        onValueChange = { agPlace = it },
                        label = { Text("Lieu (obligatoire)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = agAgenda,
                        onValueChange = { agAgenda = it },
                        label = { Text("Ordre du jour (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = agExtra,
                        onValueChange = { agExtra = it },
                        label = { Text("Complément (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Ligne "Tout" + "X/Y" avant la liste
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable(enabled = state.parents.isNotEmpty()) { vm.setSelectAll(!allSelected) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked -> vm.setSelectAll(checked) },
                        enabled = state.parents.isNotEmpty()
                    )
                    Text("Tout", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$selectedCount/$allCount",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Emails valides (sélection) : ${selectedEmails.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))
        }
    }

    Spacer(Modifier.height(12.dp))

    // Liste accessible (ne pas rajouter un padding horizontal ici si déjà géré au parent)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = state.parents, key = { it.id }) { p ->
            ParentRow(
                p = p,
                selected = state.selectedIds.contains(p.id),
                onToggle = { vm.toggleSelect(p.id) }
            )
        }
    }
}


@Composable
private fun ParentRow(
    p: ParentContact,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    val titleColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    val subtitleColor = titleColor.copy(alpha = 0.75f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {


            Column(modifier = Modifier.weight(1f)) {
                val identite = listOf(p.nom, p.prenom).filter { it.isNotBlank() }.joinToString(" ")
                Text(
                    text = identite.ifBlank { "Parent" },
                    style = MaterialTheme.typography.titleSmall,
                    color = titleColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = p.telephone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            }
            if (selected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_poing_val),
                    contentDescription = "Sélectionné",
                    modifier = Modifier.size(24.dp) // ajuste ici (20–24.dp recommandé)
                )
                Spacer(Modifier.width(12.dp))
            } else {
                Spacer(Modifier.width(36.dp))
            }
        }
    }
}

@Composable
private fun ParentsCountBadge(count: Int) {
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.People,
            contentDescription = "Parents",
            modifier = Modifier
                .size(42.dp)
                .offset(y = (-6).dp),
            tint = MaterialTheme.colorScheme.primary
        )

        if (count > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6).dp, y = (-10).dp),
                shape = CircleShape,
                color = MaterialTheme.extraColors.rougeTatamie,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extraColors.blanc
                )
            }
        }
    }
}
