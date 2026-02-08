package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.R
import com.antechrist.adherentsapp.ui.screens.guardian.GuardianUi
import com.antechrist.adherentsapp.ui.theme.extraColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GuardianListItem(
    modifier: Modifier = Modifier,
    guardian: GuardianUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
    showCheckbox: Boolean = false,
    memberNames: List<String> = emptyList(),
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 0.dp //if (isSelected) 6.dp else 2.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.extraColors.dorureDojo.copy(alpha = 0.3f)
            } else {
                MaterialTheme.extraColors.dorureDojo.copy(alpha = 0.7f)
            }
        )

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // === PARTIE GAUCHE: Avatar + Icône genre ===
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    tonalElevation = 2.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val initials = buildString {
                            append(guardian.nom.firstOrNull() ?: '#')
                            append(guardian.prenom.firstOrNull() ?: ' ')
                        }.uppercase()

                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                val (genderIcon, genderColor) = when (guardian.relation?.lowercase()) {
                    "père", "pere", "papa", "daddy" -> Icons.Filled.Male to Color(0xFF64B5F6)
                    "mère", "mere", "maman", "mother" -> Icons.Filled.Female to Color(0xFFF48FB1)
                    else -> null to null
                }

                genderIcon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = guardian.relation,
                        modifier = Modifier.size(36.dp),
                        tint = genderColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // === PARTIE DROITE: Infos du guardian ===
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${guardian.nom} ${guardian.prenom}".trim(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                guardian.email?.takeIf { it.isNotBlank() }?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                guardian.telephone?.takeIf { it.isNotBlank() }?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (memberNames.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.FamilyRestroom,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = memberNames.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                guardian.ville?.takeIf { it.isNotBlank() }?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!guardian.authUid.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Compte actif",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (showCheckbox && isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_poing_val),
                    contentDescription = "Sélectionné",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.extraColors.rougeTatamie
                )
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}
