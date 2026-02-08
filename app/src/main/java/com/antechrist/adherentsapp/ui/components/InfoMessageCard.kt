package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.Audience
import com.antechrist.adherentsapp.domain.model.InfoMessage
import com.antechrist.adherentsapp.domain.model.MessagePriority
import com.antechrist.adherentsapp.ui.theme.extraColors
import java.text.DateFormat
import java.util.Date

@Composable
fun InfoMessageCard(
    msg: InfoMessage,
    modifier: Modifier = Modifier,
    canEdit: Boolean = false,
    canDelete: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dateText = rememberPrettyDateTime(msg.createdAt)

    val audienceLabel = when (msg.audience) {
        Audience.ALL_REGISTERED -> "Tous"
        Audience.ADULTS_ONLY -> "Adultes"
        Audience.GUARDIANS -> "Parents / Tuteurs"
        Audience.CUSTOM_GROUPS -> "Groupes"
    }

    // ✅ Couleur de fond par priorité (tu peux garder tes couleurs actuelles)
    val containerColor = when (msg.priority) {
        MessagePriority.LOW -> Color(0xFFE8F5E9)
        MessagePriority.NORMAL -> Color(0xFFD6E9FF)
        MessagePriority.HIGH -> Color(0xFFFFE4C2)
        MessagePriority.URGENT -> Color(0xFFFFCDD2)
    }

    val messageText = msg.body.trim().ellipsizeChars(300)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 0.dp) // on laisse l'angle libre pour le ribbon
            ) {
                // ── Ligne 1 : titre gauche + actions droite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // ✅ Actions "collées à droite"
                    Row(
                        modifier = Modifier
                            .offset(x = 10.dp), // pousse un peu vers le bord droit
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (canEdit && onEdit != null) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(38.dp)       // réduit l'emprise
                                    .offset(x = 6.dp)  // encore plus à droite
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifier",
                                    tint = MaterialTheme.extraColors.certifiedBlue
                                )
                            }
                        }

                        if (canDelete && onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(38.dp)
                                    .offset(x = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.extraColors.rougeTatamie
                                )
                            }
                        }
                    }
                }

                // ── Ligne 2 : message centré, tronqué à 300 chars
                Text(
                    text = messageText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(min = 48.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )

                // ── Bas : date/heure à gauche (on laisse de l’air en bas à droite pour le bandeau)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, end = 70.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Publié le: $dateText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Bandeau diagonal bas droite : fond noir, texte doré
            RibbonBottomRight(
                text = audienceLabel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // 👉 si tu veux "collé angle" sans dépasser, mets x=0,y=0
                    // 👉 si tu veux qu'il "morde" un peu l'angle, mets x>0,y>0
                    .offset(x = 36.dp, y = 0.dp)
            )
        }
    }
}

@Composable
private fun RibbonBottomRight(
    text: String,
    modifier: Modifier = Modifier
) {
    val ribbonWidth = 110.dp
    val ribbonHeight = 20.dp

    Box(
        modifier = modifier
            .width(ribbonWidth)
            .height(ribbonHeight)
            .rotate(-35f)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFD4AF37)
        )
    }
}

@Composable
private fun rememberPrettyDateTime(epochMillis: Long): String {
    val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    return df.format(Date(epochMillis))
}

private fun String.ellipsizeChars(maxChars: Int): String {
    if (length <= maxChars) return this
    return take(maxChars).trimEnd() + "…"
}
