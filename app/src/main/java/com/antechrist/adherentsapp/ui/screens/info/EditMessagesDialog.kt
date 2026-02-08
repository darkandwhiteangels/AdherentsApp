package com.antechrist.adherentsapp.ui.screens.info

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.InfoMessage

@Composable
fun EditMessageDialog(
    message: InfoMessage,
    onDismiss: () -> Unit,
    onConfirm: (title: String, body: String) -> Unit
) {
    var title by remember { mutableStateOf(message.title) }
    var body by remember { mutableStateOf(message.body) }

    val canEdit = message.canBeEdited()
    val hoursLeft = message.hoursUntilEditExpires()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Edit, contentDescription = null)
        },
        title = {
            Column {
                Text("Modifier le message")
                if (canEdit) {
                    Text(
                        "Temps restant : ${hoursLeft}h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!canEdit) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "⚠️ Le délai de 24h pour modifier ce message est dépassé",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Contenu") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 6
                    )
                }
            }
        },
        confirmButton = {
            if (canEdit) {
                Button(
                    onClick = {
                        if (title.isNotBlank() && body.isNotBlank()) {
                            onConfirm(title, body)
                        }
                    },
                    enabled = title.isNotBlank() && body.isNotBlank()
                ) {
                    Text("Enregistrer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (canEdit) "Annuler" else "Fermer")
            }
        }
    )
}