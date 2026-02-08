package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.finance.HouseholdStatus

@Composable
fun StatusIconChip(
    status: HouseholdStatus,
    selected: Boolean,
    labelOverride: String? = null,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val label = labelOverride ?: status.label()

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = if (compact)
                    MaterialTheme.typography.labelSmall
                else
                    MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = when (status) {
                    HouseholdStatus.A_REGLER -> Icons.Default.Schedule
                    HouseholdStatus.SOLDE -> Icons.Default.CheckCircle
                    HouseholdStatus.EN_RETARD -> Icons.Default.Warning
                    HouseholdStatus.ANNULE -> Icons.Default.Cancel
                },
                contentDescription = null,
                modifier = Modifier.size(if (compact) 16.dp else 20.dp)
            )
        },
        modifier = if (compact) {
            Modifier.height(32.dp)
        } else {
            Modifier
        }
    )
}

