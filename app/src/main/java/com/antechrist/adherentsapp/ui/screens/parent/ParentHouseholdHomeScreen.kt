package com.antechrist.adherentsapp.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.model.Guardian
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentHouseholdHomeScreen(
    onSelectAdherent: (String) -> Unit,
    onOpenPayment: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit,
    vm: ParentHouseholdHomeViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParentDrawerContent(
                onOpenPayment = {
                    scope.launch { drawerState.close() }
                    onOpenPayment()
                },
                onOpenMessages = {
                    scope.launch { drawerState.close() }
                    onOpenMessages()
                },
                onOpenProfile = {
                    scope.launch { drawerState.close() }
                    onOpenProfile()
                },
                onLogout = {
                    vm.logout {
                        scope.launch { drawerState.close() }
                        onLogout()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Espace Parent V2") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    is ParentHomeUiState.Loading -> {
                        LoadingContent()
                    }
                    is ParentHomeUiState.Success -> {
                        HouseholdContent(
                            guardian = state.guardian,
                            adherents = state.adherents,
                            onSelectAdherent = onSelectAdherent
                        )
                    }
                    is ParentHomeUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = { vm.retry() }
                        )
                    }
                    is ParentHomeUiState.NoGuardianLinked -> {
                        NoGuardianContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Erreur",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Réessayer")
        }
    }
}

@Composable
private fun NoGuardianContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PersonOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Aucun compte parent associé",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Contactez le club pour lier votre compte à un responsable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HouseholdContent(
    guardian: Guardian,
    adherents: List<Adherent>,
    onSelectAdherent: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // En-tête du foyer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Famille ${guardian.nom}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${adherents.size} membre${if (adherents.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Titre section
        item {
            Text(
                "Sélectionner un membre :",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Liste des adhérents
        if (adherents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Aucun adhérent lié",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Contactez le club pour ajouter des membres à votre foyer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(adherents) { adherent ->
                AdherentCard(
                    adherent = adherent,
                    onClick = { onSelectAdherent(adherent.id) }
                )
            }
        }
    }
}

@Composable
private fun AdherentCard(
    adherent: Adherent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo
            AsyncImage(
                model = adherent.photoUri ?: "https://via.placeholder.com/80",
                contentDescription = "Photo de ${adherent.prenom}",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(16.dp))

            // Infos
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "${adherent.prenom} ${adherent.nom}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Âge
                adherent.dateNaissance?.let { dateNaissanceStr ->
                    val age = calculateAge(dateNaissanceStr)
                    if (age != null) {
                        Text(
                            "$age ans",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Grade/ceinture
                adherent.beltCode?.let { beltCode ->
                    val beltName = beltCode.substringAfter(":").replace("_", " ").capitalize()
                    Text(
                        beltName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Voir le profil",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ParentDrawerContent(
    onOpenPayment: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                "Menu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Divider()

            Spacer(Modifier.height(8.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Payment, contentDescription = null) },
                label = { Text("Cotisations") },
                selected = false,
                onClick = onOpenPayment
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Message, contentDescription = null) },
                label = { Text("Messages du club") },
                selected = false,
                onClick = onOpenMessages
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("Mon profil") },
                selected = false,
                onClick = onOpenProfile
            )

            Spacer(Modifier.weight(1f))

            Divider()

            Spacer(Modifier.height(8.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                label = { Text("Se déconnecter") },
                selected = false,
                onClick = onLogout,
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedIconColor = MaterialTheme.colorScheme.error,
                    unselectedTextColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

// Fonction utilitaire pour calculer l'âge
private fun calculateAge(dateNaissanceStr: String): Int? {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val birthDate = LocalDate.parse(dateNaissanceStr, formatter)
        val now = LocalDate.now()
        Period.between(birthDate, now).years
    } catch (e: Exception) {
        null
    }
}

private fun String.capitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercaseChar() }
}