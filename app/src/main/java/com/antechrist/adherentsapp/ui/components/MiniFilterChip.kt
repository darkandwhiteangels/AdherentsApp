package com.antechrist.adherentsapp.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// -------------------------
// MiniFilterChip (avec tint)
// -------------------------
@Composable
fun MiniFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Couleur de base (teinte) appliquée quand le chip est sélectionné.
     * Si null, on utilise MaterialTheme.colorScheme.primary.
     */
    tint: Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    val base = tint ?: scheme.primary

    val container = if (selected) base.copy(alpha = 0.65f) else base.copy(alpha = 0.05f)
    val border = if (selected) base else scheme.outlineVariant
    val content = if (selected) Color.Black else scheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .heightIn(min = 32.dp)                 // compact mais touchable
            .padding(horizontal = 2.dp),
        color = container,
        shape = RoundedCornerShape(25),
        border = BorderStroke(1.dp, border),
        tonalElevation = if (selected) 1.dp else 0.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall, // texte plus petit
                color = content,
                maxLines = 1
            )
        }
    }
}