package com.antechrist.adherentsapp.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun ParentActivationByTokenScreen(
    token: String,
    onSuccess: () -> Unit,
    onNeedLogin: (returnToken: String) -> Unit,
    onBack: () -> Unit,
    vm: ParentActivationViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    // Démarrer l'activation au montage du composant
    LaunchedEffect(token) {
        vm.activate(token)
    }

    // Navigation automatique en cas de succès
    LaunchedEffect(ui.state) {
        if (ui.state == ActivationState.SUCCESS) {
            delay(2000) // Laisser l'utilisateur voir le message de succès
            onSuccess()
        }
    }

    Scaffold { paddings ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (ui.state) {
                ActivationState.LOADING -> LoadingView()

                ActivationState.NEED_LOGIN -> NeedLoginView(
                    token = token,
                    onLogin = { onNeedLogin(token) }
                )

                ActivationState.EMAIL_MISMATCH -> EmailMismatchView(
                    guardianName = ui.guardianName,
                    expectedEmail = ui.email,
                    currentEmail = ui.currentUserEmail,
                    onBack = onBack
                )

                ActivationState.EXPIRED -> ExpiredView(
                    error = ui.error,
                    onBack = onBack
                )

                ActivationState.INVALID -> InvalidView(
                    error = ui.error,
                    onBack = onBack
                )

                ActivationState.SUCCESS -> SuccessView(
                    guardianName = ui.guardianName
                )

                ActivationState.ERROR -> ErrorView(
                    error = ui.error,
                    onRetry = { vm.activate(token) },
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            "Vérification de l'invitation...",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Veuillez patienter",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NeedLoginView(
    token: String,
    onLogin: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.Login,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            "Connexion requise",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Vous devez être connecté pour accepter cette invitation",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Login, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Se connecter")
        }

        Text(
            "Après connexion, vous serez automatiquement redirigé vers cette page",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmailMismatchView(
    guardianName: String?,
    expectedEmail: String?,
    currentEmail: String?,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            "Email non correspondant",
            style = MaterialTheme.typography.headlineSmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Cette invitation est destinée à :",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    expectedEmail ?: "Email inconnu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Divider(Modifier.padding(vertical = 4.dp))

                Text(
                    "Vous êtes connecté avec :",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    currentEmail ?: "Email inconnu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Text(
            "Pour accepter cette invitation, connectez-vous avec l'email correct ou contactez le club",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Retour")
        }
    }
}

@Composable
private fun ExpiredView(
    error: String?,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.AccessTime,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            "Invitation expirée",
            style = MaterialTheme.typography.headlineSmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    error ?: "Cette invitation n'est plus valide",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Text(
            "Les invitations sont valables 30 jours. Demandez au club de vous envoyer une nouvelle invitation.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Retour")
        }
    }
}

@Composable
private fun InvalidView(
    error: String?,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            "Invitation invalide",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            error ?: "Cette invitation est introuvable ou a déjà été utilisée",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            "Si vous pensez qu'il s'agit d'une erreur, contactez le club",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Retour")
        }
    }
}

@Composable
private fun SuccessView(
    guardianName: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            "Compte activé !",
            style = MaterialTheme.typography.headlineMedium
        )

        guardianName?.let {
            Text(
                "Bienvenue $it",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Vous avez maintenant accès à votre espace parent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Consultez les informations de vos adhérents",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Euro,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Suivez vos cotisations et paiements",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "Redirection vers votre espace...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorView(
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            "Erreur technique",
            style = MaterialTheme.typography.headlineSmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                error ?: "Une erreur est survenue lors de l'activation",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Réessayer")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Retour")
        }
    }
}