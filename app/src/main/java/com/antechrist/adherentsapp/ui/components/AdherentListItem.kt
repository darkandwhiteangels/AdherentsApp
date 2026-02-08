package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.ui.theme.extraColors

@Composable
fun AdherentListItem(
    item: Adherent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initials = remember(item.nom, item.prenom) {
        val n = item.nom.takeIf { it.isNotBlank() }?.firstOrNull()?.uppercaseChar()
        val p = item.prenom.takeIf { it.isNotBlank() }?.firstOrNull()?.uppercaseChar()
        when {
            n != null && p != null -> "$n$p"
            n != null -> "$n"
            p != null -> "$p"
            else -> "#"
        }
    }

    val email = item.email?.trim().orEmpty()
    val ville = item.ville?.trim().orEmpty()

    ListItem(
        headlineContent = {
            Text(
                text = "${item.nom} ${item.prenom}".trim(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (email.isNotEmpty()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (ville.isNotEmpty()) {
                    Text(
                        text = ville,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.extraColors.certifiedBlue
                    )
                }
            }
        },
        trailingContent = {
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                BeltListBadge(
//                    beltCode = item.beltCode,
//                    stripeCount = item.stripeCount,
//                    height = 18.dp,               // garde ta taille
//                    showInsignia = true           // ✅ affiche l’insigne JP (1–5 Dan)
//                )
//            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isPractitioner) {
                    BeltListBadge(
                        beltCode = item.beltCode,
                        stripeCount = item.stripeCount,
                        height = 18.dp,               // garde ta taille
                        showInsignia = true           // ✅ affiche l’insigne JP (1–5 Dan)
                    )
                } else {
                    BureauBadge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer, // ex: "bleu club"
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        outline = false
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}