package com.antechrist.adherentsapp.ui.screens.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.antechrist.adherentsapp.R
import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.domain.model.Role
import com.antechrist.adherentsapp.ui.components.BeltPaths
import com.antechrist.adherentsapp.ui.components.BeltRendererKnot
import com.antechrist.adherentsapp.ui.role.RoleViewModel
import com.antechrist.adherentsapp.ui.screens.guardian.GuardianEditScreen
import com.antechrist.adherentsapp.ui.theme.extraColors
import com.antechrist.adherentsapp.ui.viewmodels.GuardianLinksViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

private data class GuardianUi(
    val id: String,
    val nom: String,
    val prenom: String,
    val relation: String?,
    // Refactor Guardian
    val email: String?,
    val authUid: String?
)

// =========================================================================
//  MODIFIÉ : NOUVELLE FONCTION D'ENTRÉE AVEC LE PAGER
// =========================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdherentDetailScreen(
    initialAdherentId: String,
    adherentIds: List<String>,
    onBack: () -> Unit,
    onEdit: (currentId: String) -> Unit,
    onDeleted: () -> Unit,
    // MODIFIÉ: On enlève les injections de ViewModel d'ici
) {
    // On charge le rôle une seule fois pour tout l'écran.
    // On peut utiliser hiltViewModel() ici car il sera unique pour AdherentDetailScreen
    val roleVm: RoleViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        roleVm.refresh()
    }

    // 1. Créez un état pour le Pager.
    val initialPageIndex = remember(initialAdherentId, adherentIds) {
        adherentIds.indexOf(initialAdherentId).coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { adherentIds.size }
    )

    // 3. Utilisez HorizontalPager comme conteneur principal.
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = adherentIds.size > 1
    ) { pageIndex ->
        val currentAdherentId = adherentIds.getOrNull(pageIndex) ?: return@HorizontalPager

        // MODIFIÉ : On instancie les ViewModels ICI, en utilisant l'ID de l'adhérent comme clé.
        // Cela garantit que chaque page a son propre ViewModel isolé.
        val adherentDetailViewModel: AdherentDetailViewModel = hiltViewModel(key = currentAdherentId)
        val guardianLinksViewModel: GuardianLinksViewModel = hiltViewModel(key = "${currentAdherentId}_links")

        // MODIFIÉ : On charge les données de l'adhérent spécifique à cette page.
        // Ce LaunchedEffect s'exécutera une seule fois par page.
        LaunchedEffect(currentAdherentId) {
            adherentDetailViewModel.load(currentAdherentId)
        }

        // Le contenu de votre ancien écran est maintenant dans ce Composable
        AdherentDetailPageContent(
            adherentId = currentAdherentId,
            onBack = onBack,
            onEdit = { onEdit(currentAdherentId) },
            onDeleted = onDeleted,
            // On passe les ViewModels spécifiques à cette page
            vm = adherentDetailViewModel,
            linksVm = guardianLinksViewModel,
            roleVm = roleVm // Le roleVm est partagé, ce qui est correct.
        )
    }
}


// =========================================================================
//  MODIFIÉ : ANCIENNE FONCTION RENOMMÉE ET ADAPTÉE
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdherentDetailPageContent(
    adherentId: String, // Reçoit l'ID de la page actuelle
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    vm: AdherentDetailViewModel,
    linksVm: GuardianLinksViewModel,
    roleVm: RoleViewModel
) {
    val state by vm.state.collectAsState()
    var showAddGuardian by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var editGuardianId by remember { mutableStateOf<String?>(null) }
    var guardiansReloadTick by remember { mutableIntStateOf(0) }

    // MODIFIÉ : On récupère simplement la valeur du rôle. Le `refresh` est maintenant géré par le parent.
    val currentRole by roleVm.role.collectAsState()
    val isSuperAdmin = currentRole == Role.SUPER_ADMIN
    var roleMenuOpen by remember { mutableStateOf(false) }

    // MODIFIÉ : Le LaunchedEffect qui faisait le refresh ici est supprimé.

    LaunchedEffect(state.error) {
        state.error?.let { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.adherent?.fullName ?: "Fiche adhérent") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (state.adherent != null) {
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Modifier") }
                        IconButton(onClick = vm::onTryDelete) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }

                        // Votre condition devrait maintenant fonctionner correctement
                        // Affiche le menu si on est SuperAdmin ET que l'adhérent a un email OU que son responsable principal a un email.
                        if (isSuperAdmin && (!state.adherent!!.email.isNullOrBlank() || !state.primaryGuardian?.email.isNullOrBlank())) {
                            IconButton(onClick = { roleMenuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Rôles") }
                            // ...
                            DropdownMenu(expanded = roleMenuOpen, onDismissRequest = { roleMenuOpen = false }) {
                                // PREMIER ITEM : Rendre Admin
                                DropdownMenuItem(
                                    text = { Text("Rendre Admin") },
                                    onClick = {
                                        roleMenuOpen = false
                                        // On cherche le premier email valide : celui du responsable d'abord, sinon celui de l'adhérent.
                                        val targetEmail = state.primaryGuardian?.email?.takeIf { it.isNotBlank() }
                                            ?: state.adherent?.email?.takeIf { it.isNotBlank() }

                                        if (targetEmail == null) {
                                            scope.launch { snackbar.showSnackbar("Aucun email valide trouvé pour cette action.") }
                                        } else {
                                            scope.launch {
                                                try {
                                                    val outcome = com.antechrist.adherentsapp.data.remote.RolesRemote.setRoleByEmail(targetEmail, "admin")
                                                    if (outcome.created) {
                                                        FirebaseAuth.getInstance().sendPasswordResetEmail(targetEmail).await()
                                                        snackbar.showSnackbar("Rôle admin appliqué. Email envoyé à $targetEmail.")
                                                    } else {
                                                        snackbar.showSnackbar("Rôle admin appliqué.")
                                                    }
                                                } catch (e: Exception) {
                                                    snackbar.showSnackbar("Échec : ${e.message ?: "inconnu"}")
                                                }
                                            }
                                        }
                                    }
                                )
                                // DEUXIÈME ITEM : Rendre Secrétaire
                                DropdownMenuItem(
                                    text = { Text("Rendre Secrétaire") },
                                    onClick = {
                                        roleMenuOpen = false
                                        val targetEmail = state.primaryGuardian?.email?.takeIf { it.isNotBlank() }
                                            ?: state.adherent?.email?.takeIf { it.isNotBlank() }

                                        if (targetEmail == null) {
                                            scope.launch { snackbar.showSnackbar("Aucun email valide trouvé.") }
                                        } else {
                                            scope.launch {
                                                try {
                                                    val outcome = com.antechrist.adherentsapp.data.remote.RolesRemote.setRoleByEmail(targetEmail, "secretaire")
                                                    if (outcome.created) {
                                                        FirebaseAuth.getInstance().sendPasswordResetEmail(targetEmail).await()
                                                        snackbar.showSnackbar("Rôle secrétaire appliqué. Email envoyé.")
                                                    } else {
                                                        snackbar.showSnackbar("Rôle secrétaire appliqué.")
                                                    }
                                                } catch (e: Exception) {
                                                    snackbar.showSnackbar("Échec : ${e.message ?: "inconnu"}")
                                                }
                                            }
                                        }
                                    }
                                )
                                // TROISIÈME ITEM : Rendre Trésorier
                                DropdownMenuItem(
                                    text = { Text("Rendre Trésorier") },
                                    onClick = {
                                        roleMenuOpen = false
                                        val targetEmail = state.primaryGuardian?.email?.takeIf { it.isNotBlank() }
                                            ?: state.adherent?.email?.takeIf { it.isNotBlank() }

                                        if (targetEmail == null) {
                                            scope.launch { snackbar.showSnackbar("Aucun email valide trouvé.") }
                                        } else {
                                            scope.launch {
                                                try {
                                                    val outcome = com.antechrist.adherentsapp.data.remote.RolesRemote.setRoleByEmail(targetEmail, "tresorier")
                                                    if (outcome.created) {
                                                        FirebaseAuth.getInstance().sendPasswordResetEmail(targetEmail).await()
                                                        snackbar.showSnackbar("Rôle trésorier appliqué. Email envoyé.")
                                                    } else {
                                                        snackbar.showSnackbar("Rôle trésorier appliqué.")
                                                    }
                                                } catch (e: Exception) {
                                                    snackbar.showSnackbar("Échec : ${e.message ?: "inconnu"}")
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { paddings ->
        when {
            state.loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddings),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.adherent == null -> {
                Box(Modifier.fillMaxSize().padding(paddings), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Adhérent introuvable")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onBack) { Text("Retour") }
                    }
                }
            }
            // `DetailContent` reste identique
            else -> DetailContent(
                adherent = state.adherent!!,
                primaryGuardian = state.primaryGuardian,
                paddings = paddings,
                onAddGuardian = { showAddGuardian = true },
                onEditGuardian = { gid -> editGuardianId = gid },
                reloadKey = guardiansReloadTick,
                linksVm = linksVm,
                onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                onReload = { guardiansReloadTick++ }
            )
        }
    }

    editGuardianId?.let { gid ->
        GuardianEditScreen(
            guardianId = gid,
            onClose = {
                editGuardianId = null
                guardiansReloadTick++
            }
        )
    }

    if (showAddGuardian && state.adherent != null) {
        // Le `AddGuardianDialog` reste identique
        AddGuardianDialog(
            adherentId = state.adherent!!.id,
            onDismiss = { showAddGuardian = false },
            linksVm = linksVm,
            onLinked = { _, created, emailOrTel ->
                val msg = if (created) "Responsable créé et lié ($emailOrTel)"
                else "Responsable lié ($emailOrTel)"
                scope.launch { snackbar.showSnackbar(msg) }
                showAddGuardian = false
                guardiansReloadTick++
            },
            onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
        )
    }

    // MODIFIÉ : La boîte de dialogue de suppression lit l'état depuis le ViewModel
    if (state.askDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = vm::onCancelDelete,
            title = { Text("Supprimer l’adhérent ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    // Pas besoin de 'showDeleteConfirm = false'
                    scope.launch {
                        // Utilise l'ID de l'adhérent actuellement chargé
                        val ok = vm.deleteSafe(state.adherent!!.id)
                        if (ok) onDeleted() else snackbar.showSnackbar("Suppression refusée")
                    }
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = vm::onCancelDelete) { Text("Annuler") } }
        )
    }
}

// =========================================================================
//  AUCUN CHANGEMENT NÉCESSAIRE DANS LES FONCTIONS CI-DESSOUS
// =========================================================================
@Composable
private fun DetailContent(
    adherent: Adherent,
    primaryGuardian: Guardian?,
    paddings: PaddingValues,
    onAddGuardian: () -> Unit,
    onEditGuardian: (guardianId: String) -> Unit,
    reloadKey: Int,
    linksVm: GuardianLinksViewModel,
    onError: (String) -> Unit,
    onReload: () -> Unit
) {
    // ... tout votre code de DetailContent reste exactement le même
    val isMinor = remember(adherent.dateNaissance) {
        adherent.dateNaissance?.let { computeAgeYears(it) }?.let { it < 18 } == true
    }
    val adresseStr = listOfNotNull(adherent.adresse, adherent.codePostal, adherent.ville)
        .filter { it.isNotBlank() }
        .joinToString(" ")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddings)
            .consumeWindowInsets(paddings)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { HeroHeader(adherent = adherent, primaryGuardian = primaryGuardian) }

        if (isMinor) {
            item {
                GuardiansSection(
                    adherentId = adherent.id,
                    guardianIds = adherent.guardianIds ?: emptyList(),
                    primaryGuardianId = adherent.primaryGuardianId,
                    isStaff = true,
                    onAddClick = onAddGuardian,
                    onEditGuardian = onEditGuardian,
                    reloadKey = reloadKey,
                    linksVm = linksVm,
                    onError = onError,
                    onReload = onReload
                )
            }
        } else {
            item {
                AdultHouseholdSection(
                    adherent = adherent,
                    linksVm = linksVm,
                    onEditGuardian = onEditGuardian,
                    onError = onError,
                    onReload = onReload
                )
            }
        }

        item {
            SectionCard {
                LabeledRow("Date de naissance", adherent.dateNaissance ?: "—")
                LabeledRow("Téléphone", formatPhoneForDisplay(adherent.telephone) ?: "—", leadingIcon = { Icon(Icons.Filled.Call, null) })
                LabeledRow("Email", adherent.email ?: "—", leadingIcon = { Icon(Icons.Filled.Email, null) })
                LabeledRow("Adresse", adresseStr.ifBlank { "—" })
            }
        }
    }
}


// ... TOUT LE RESTE DU FICHIER (GuardiansSection, AddGuardianDialog, HeroHeader, etc.) reste inchangé.
// Collez le reste de votre fichier original ici.

@Composable
private fun GuardiansSection(
    adherentId: String,
    guardianIds: List<String>,
    primaryGuardianId: String?,
    isStaff: Boolean,
    onAddClick: () -> Unit,
    onEditGuardian: (guardianId: String) -> Unit,
    reloadKey: Int,
    linksVm: GuardianLinksViewModel,
    onError: (String) -> Unit,
    onReload: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var guardians by remember(guardianIds, reloadKey) { mutableStateOf<List<GuardianUi>>(emptyList()) }
    var loading by remember(guardianIds, reloadKey) { mutableStateOf(false) }

    LaunchedEffect(guardianIds, reloadKey) {
        if (guardianIds.isEmpty()) {
            guardians = emptyList()
            return@LaunchedEffect
        }
        loading = true
        try {
            val snap = Firebase.firestore
                .collection("guardians")
                .whereIn(FieldPath.documentId(), guardianIds.take(10))
                .get()
                .await()

            val list = snap.documents.map { doc ->
                GuardianUi(
                    id = doc.id,
                    nom = (doc.getString("nom") ?: "").trim(),
                    prenom = (doc.getString("prenom") ?: "").trim(),
                    relation = doc.getString("relation"),
                    // Refactor Guardian
                    email = doc.getString("emailLower") ?: doc.getString("email"),
                    authUid = doc.getString("authUid")
                )
            }
            guardians = list.sortedWith(
                compareByDescending<GuardianUi> { it.id == primaryGuardianId }
                    .thenBy { it.nom.lowercase() }
                    .thenBy { it.prenom.lowercase() }
            )
        } catch (_: Exception) {
            guardians = emptyList()
        } finally {
            loading = false
        }
    }

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Responsables", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onAddClick, enabled = isStaff) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Ajouter")
            }
        }

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            return@SectionCard
        }

        if (guardianIds.isEmpty()) {
            AssistChip(onClick = { }, enabled = false, label = { Text("Responsable à ajouter") })
            Text("Aucun responsable lié pour l’instant.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            guardians.forEach { g ->
                var rowMenuOpen by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${g.prenom} ${g.nom}".trim(), style = MaterialTheme.typography.bodyLarge)
                        if (!g.relation.isNullOrBlank()) {
                            Text(g.relation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (g.id == primaryGuardianId) {
                            AssistChip(onClick = {}, enabled = false, label = { Text("Principal") })
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(onClick = { onEditGuardian(g.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Éditer le responsable") }
                        IconButton(onClick = { rowMenuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Actions") }
                        DropdownMenu(expanded = rowMenuOpen, onDismissRequest = { rowMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Définir comme principal") },
                                enabled = g.id != primaryGuardianId,
                                onClick = {
                                    rowMenuOpen = false
                                    scope.launch {
                                        try {
                                            linksVm.makePrimary(adherentId, g.id)
                                            onReload()
                                        } catch (e: Exception) {
                                            onError(e.message ?: "Erreur")
                                        }
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Délier") },
                                onClick = {
                                    rowMenuOpen = false
                                    scope.launch {
                                        try {
                                            linksVm.unlink(adherentId, g.id)
                                            onReload()
                                        } catch (e: Exception) {
                                            onError(e.message ?: "Erreur")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddGuardianDialog(
    adherentId: String,
    onDismiss: () -> Unit,
    linksVm: GuardianLinksViewModel,
    onLinked: (guardianId: String, created: Boolean, emailOrTel: String) -> Unit,
    onError: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<Guardian?>(null) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        confirmButton = {
            val canCreate = (email.trim().isNotEmpty() || telephone.trim().isNotEmpty()) &&
                    nom.trim().isNotEmpty() && prenom.trim().isNotEmpty()

            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            loading = true; error = null
                            val eOrT = email.ifBlank { telephone }

                            if (found != null) {
                                linksVm.link(adherentId, found!!.id, setPrimaryIfEmpty = true)
                                onLinked(found!!.id, false, eOrT)
                                return@launch
                            }

                            val existed = linksVm.findByEmailOrTel(
                                email.trim().lowercase().ifBlank { null },
                                telephone.filter { it.isDigit() }.ifBlank { null }
                            )
                            if (existed != null) {
                                found = existed
                                linksVm.link(adherentId, existed.id, setPrimaryIfEmpty = true)
                                onLinked(existed.id, false, eOrT)
                            } else {
                                if (!canCreate) {
                                    error = "Renseigne au moins nom, prénom et email ou téléphone."
                                } else {
                                    val id = linksVm.create(
                                        nom = nom.trim(),
                                        prenom = prenom.trim(),
                                        email = email.trim().lowercase().ifBlank { null },
                                        telDigits = telephone.filter { it.isDigit() }.ifBlank { null },
                                        relation = relation.trim().ifBlank { null }
                                    )
                                    linksVm.link(adherentId, id, setPrimaryIfEmpty = true)
                                    onLinked(id, true, eOrT)
                                }
                            }
                        } catch (e: Exception) {
                            val msg = e.message ?: "Échec de l’opération"
                            error = msg
                            onError(msg)
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading
            ) {
                Text(if (found != null) "Lier" else "Créer & lier")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                loading = true; error = null
                                found = linksVm.findByEmailOrTel(email.trim().lowercase().ifBlank { null }, telephone.filter { it.isDigit() }.ifBlank { null })
                                if (found == null) error = "Aucun responsable trouvé, complète les champs pour créer."
                            } catch (e: Exception) {
                                val msg = e.message ?: "Erreur de recherche"
                                error = msg
                                onError(msg)
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading
                ) { Text("Rechercher") }

                Spacer(Modifier.width(8.dp))

                TextButton(onClick = { if (!loading) onDismiss() }) { Text("Annuler") }
            }
        },
        title = { Text("Ajouter un responsable") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it.trim() }, label = { Text("Email (prioritaire)") }, singleLine = true)
                OutlinedTextField(value = telephone, onValueChange = { telephone = it.trim() }, label = { Text("Téléphone (ou)") }, singleLine = true)
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, singleLine = true)
                OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") }, singleLine = true)
                OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relation (mère, père...)") }, singleLine = true)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                if (found != null) Text("Trouvé : ${found!!.prenom ?: ""} ${found!!.nom ?: ""}${found!!.relation?.let { " ($it)" } ?: ""}", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

/* ==== Helpers (inchangés) ==== */

private fun formatPhoneForDisplay(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val digits = raw.filter { it.isDigit() }.take(10)
    return if (digits.length == 10) digits.chunked(2).joinToString(".") else raw
}

@Composable private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable private fun LabeledRow(label: String, value: String, leadingIcon: (@Composable () -> Unit)? = null) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (leadingIcon != null) leadingIcon()
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun microCategoryByAge(years: Int): String = when (years) {
    in 0..3   -> "Baby"
    in 4..5   -> "Mini poussins"
    in 6..7   -> "Poussins"
    in 8..9   -> "Pupilles"
    in 10..11 -> "Benjamins"
    in 12..13 -> "Minimes"
    in 14..15 -> "Cadets"
    in 16..17 -> "Juniors"
    in 18..20 -> "Espoirs"
    in 21..34 -> "Séniors"
    in 35..45 -> "Vétérans 1"
    in 46..55 -> "Vétérans 2"
    in 56..65 -> "Vétérans 3"
    else      -> "Vétérans 4"
}

@Composable private fun HeroHeader(adherent: Adherent, primaryGuardian: Guardian?) {
    val context = LocalContext.current
    val photoUrl = adherent.photoUri ?: R.drawable.ic_avatar_default
    val years = remember(adherent.dateNaissance) { adherent.dateNaissance?.let { computeAgeYears(it) } }
    val microCat = years?.let { microCategoryByAge(it) } ?: "—"

    val contactTel = (primaryGuardian?.telephone ?: adherent.telephone)?.takeIf { it.isNotBlank() }
    val contactMail = (primaryGuardian?.email ?: adherent.email)?.takeIf { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(Modifier.fillMaxWidth()) {
            AsyncImage(model = photoUrl, contentDescription = "Photo adhérent", contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(20.dp))
            )

            BeltRendererKnot(
                pathData = BeltPaths.Karate.KNOT,
                beltCode = adherent.beltCode,
                stripeCount = adherent.stripeCount,
                patternScale = 1.2f,
                modifier = Modifier.align(Alignment.TopEnd).offset(y = (-10).dp, x = (-10).dp).padding(4.dp).size(width = 56.dp, height = 40.dp)
            )

            Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 4.dp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 12.dp)) {
                Text(text = "${adherent.prenom} ${adherent.nom}".trim(), modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.extraColors.rougeTatamie
                )
            }

            Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 4.dp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 12.dp)) {
                Text(
                    text = "${adherent.prenom} ${adherent.nom}".trim(),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.extraColors.rougeTatamie
                )
            }

            Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 4.dp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 12.dp)) {
                Text(
                    text = microCat,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.extraColors.certifiedBlue
                )
            }
        }
        val g0 = adherent.groups?.firstOrNull()

        Spacer(Modifier.height(12.dp))
        MacroCategoryStrip(macro = macroFromGroupe(g0), useInternalStyle = true)
        Spacer(Modifier.height(4.dp))
        StatusIconsRow(adherent = adherent)

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val callBg = MaterialTheme.extraColors.greenTatamie
            FilledTonalIconButton(
                onClick = {
                    val uri = "tel:$contactTel".toUri()
                    context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                },
                enabled = contactTel != null,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = callBg,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Icon(Icons.Filled.Phone, contentDescription = "Appeler") }

            val mailBg = MaterialTheme.extraColors.certifiedBlue
            FilledTonalIconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, "mailto:$contactMail".toUri())
                    context.startActivity(intent)
                },
                enabled = contactMail != null,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = mailBg,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Icon(Icons.Filled.Mail, contentDescription = "Envoyer un email") }
        }
    }
}

private fun computeAgeYears(dateStr: String): Int? {
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return try {
        val birth = LocalDate.parse(dateStr, fmt)
        Period.between(birth, LocalDate.now()).years
    } catch (_: Exception) { null }
}

@Composable
private fun StatusIconsRow(adherent: Adherent) {
    val years = remember(adherent.dateNaissance) {
        adherent.dateNaissance?.let {
            val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            try {
                Period.between(LocalDate.parse(it, fmt), LocalDate.now()).years
            } catch (_: Exception) { null }
        }
    }
    val isMinor = years?.let { it < 18 } == true

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusIcon(icon = Icons.Filled.Cake, tint = MaterialTheme.extraColors.certifiedBlue, badge = years?.toString(), contentDescription = "Âge")
        StatusIcon(icon = Icons.Filled.Group, tint = if (isMinor) MaterialTheme.extraColors.rougeTatamie else MaterialTheme.extraColors.certifiedBlue, contentDescription = if (isMinor) "Mineur" else "Majeur")
    }
}

@Composable
private fun StatusIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color,
    badge: String? = null,
    contentDescription: String
) {
    BadgedBox(badge = { if (!badge.isNullOrBlank()) { Badge { Text(badge) } } }, modifier = modifier) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun AdultHouseholdSection(
    adherent: Adherent,
    linksVm: GuardianLinksViewModel,
    onEditGuardian: (guardianId: String) -> Unit,
    onError: (String) -> Unit,
    onReload: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var primary by remember(adherent.primaryGuardianId) { mutableStateOf<GuardianUi?>(null) }
    var loading by remember(adherent.primaryGuardianId) { mutableStateOf(false) }
    LaunchedEffect(adherent.primaryGuardianId) {
        val pid = adherent.primaryGuardianId ?: return@LaunchedEffect
        loading = true
        try {
            val doc = Firebase.firestore.collection("guardians").document(pid).get().await()
            primary = if (doc.exists()) {
                GuardianUi(
                    id = doc.id,
                    nom = (doc.getString("nom") ?: "").trim(),
                    prenom = (doc.getString("prenom") ?: "").trim(),
                    relation = doc.getString("relation"),
                    // Refactor Guardian
                    email = doc.getString("emailLower") ?: doc.getString("email"),
                    authUid = doc.getString("authUid")
                )
            } else null
        } catch (_: Exception) {
            primary = null
        } finally {
            loading = false
        }
    }

    SectionCard {
        Text("Foyer", style = MaterialTheme.typography.titleMedium)

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (primary != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(Modifier.weight(1f)) {
                    Text("${primary!!.prenom} ${primary!!.nom}".trim(), style = MaterialTheme.typography.bodyLarge)

                    // ✅ Affiche la relation
                    if (!primary!!.relation.isNullOrBlank()) {
                        Text(primary!!.relation!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // ✅ NOUVEAU : Indicateur d'accès parent
                    if (!primary!!.authUid.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.extraColors.certifiedBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Accès parent activé",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.extraColors.certifiedBlue
                            )
                        }
                    }
                }

                Row {
                    AssistChip(onClick = { }, enabled = false, label = { Text("Responsable") }, leadingIcon = { Icon(Icons.Filled.Person, null) })
                    Spacer(Modifier.width(8.dp))

                    IconButton(onClick = { onEditGuardian(primary!!.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Éditer")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
                OutlinedIconButton(
                    onClick = {
                        scope.launch {
                            try {
                                val email = adherent.email?.trim()?.lowercase()?.ifBlank { null }
                                val telDigits = adherent.telephone?.filter { it.isDigit() }?.ifBlank { null }
                                val existed = linksVm.findByEmailOrTel(email, telDigits)
                                val gid = existed?.id ?: linksVm.create(
                                    nom = adherent.nom,
                                    prenom = adherent.prenom,
                                    email = email,
                                    telDigits = telDigits,
                                    relation = "Responsable"
                                )
                                linksVm.link(adherent.id, gid, setPrimaryIfEmpty = true)
                                linksVm.makePrimary(adherent.id, gid)
                                onReload()
                            } catch (e: Exception) {
                                onError(e.message ?: "Échec de la déclaration responsable")
                            }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) { Icon(Icons.Filled.PersonOutline, contentDescription = "Se déclarer responsable") }

                Text("Responsable", style = MaterialTheme.typography.labelLarge)

                var showAttach by remember { mutableStateOf(false) }
                OutlinedIconButton(
                    onClick = { showAttach = true },
                    modifier = Modifier.size(36.dp)
                ) { Icon(Icons.Filled.GroupAdd, contentDescription = "Rattacher à un foyer") }

                Text("Rattacher", style = MaterialTheme.typography.labelLarge)

                if (showAttach) {
                    AddGuardianDialog(
                        adherentId = adherent.id,
                        onDismiss = { showAttach = false },
                        linksVm = linksVm,
                        onLinked = { gid, _, _ ->
                            scope.launch {
                                try {
                                    linksVm.makePrimary(adherent.id, gid)
                                    onReload()
                                } catch (e: Exception) {
                                    onError(e.message ?: "Échec rattachement")
                                } finally {
                                    showAttach = false
                                }
                            }
                        },
                        onError = { msg -> onError(msg) }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "Un adulte peut être adhérent et responsable du foyer. Sinon, rattache-le à un foyer existant pour des cotisations correctes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


private enum class MacroCat { BABY, ENFANT, ADULTE }

@Composable
private fun MacroCategoryStrip(macro: MacroCat?, useInternalStyle: Boolean, modifier: Modifier = Modifier) {
    val sel = if (useInternalStyle) MaterialTheme.extraColors.rougeTatamie else MaterialTheme.extraColors.certifiedBlue
    val selOn = if (useInternalStyle) MaterialTheme.extraColors.certifiedBlue else MaterialTheme.colorScheme.onPrimary
    val uns = MaterialTheme.extraColors.grisDojo
    val shape = RoundedCornerShape(12.dp)

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(shape).background(uns),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            @Composable fun Segment(selected: Boolean, weight: Float = 1f) {
                Box(
                    modifier = Modifier.weight(weight).fillMaxHeight().background(if (selected) sel else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) { if (selected && useInternalStyle) Box(Modifier.size(4.dp).clip(androidx.compose.foundation.shape.CircleShape).background(selOn.copy(alpha = 0.9f))) }
            }
            Segment(selected = macro == MacroCat.BABY)
            Segment(selected = macro == MacroCat.ENFANT)
            Segment(selected = macro == MacroCat.ADULTE)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Baby", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Enfant (<14)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Adulte", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun macroFromGroupe(g: String?): MacroCat? {
    val s = g?.trim()?.lowercase() ?: return null

    return when (s) {
        "baby" -> MacroCat.BABY
        "enfant_u14" -> MacroCat.ENFANT
        "adult_14p" -> MacroCat.ADULTE
        else -> {
            when {
                s.startsWith("baby") -> MacroCat.BABY
                s.contains("enfant") || s.contains("<14") || s.contains("-14")
                        || s.contains("junior") || s.contains("cadet")
                        || s.contains("poussin") || s.contains("benjamin") || s.contains("minime") -> MacroCat.ENFANT
                else -> MacroCat.ADULTE
            }
        }
    }
}

private fun currentSeasonKey(): String {
    val zone = java.time.ZoneId.of("Europe/Paris")
    val d = java.time.LocalDate.now(zone)
    val y = d.year
    return if (d.monthValue >= 9) "$y-${y + 1}" else "${y - 1}-$y"
}
