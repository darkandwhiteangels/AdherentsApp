// com/antechrist/adherentsapp/ui/components/kihon/StepEditorRow.kt
package com.antechrist.adherentsapp.ui.components.kihon

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.model.kihon.*

@Composable
fun StepEditorRow(
    modifier: Modifier = Modifier,
    step: KihonStep,
    catalog: Map<String, TechniqueRef>,
    onChange: (KihonStep) -> Unit,
    onRemove: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    stepNumber: Int? = null
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showTechPicker by remember { mutableStateOf(false) }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(buildString {
                    append("Étape")
                    stepNumber?.let { append(" ${it + 1}") }
                })
                Row {
                    onMoveUp?.let { IconButton(onClick = it) { Icon(Icons.Default.ArrowUpward, null) } }
                    onMoveDown?.let { IconButton(onClick = it) { Icon(Icons.Default.ArrowDownward, null) } }
                    onRemove?.let { IconButton(onClick = it) { Icon(Icons.Default.Close, null) } }
                }
            }

            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedAssistChip(
                    onClick = { showStartPicker = true },
                    label = { Text(step.startPosition.nameFr) },
                    leadingIcon = { Icon(Icons.Default.Flag, null) },
                    colors = AssistChipDefaults.elevatedAssistChipColors( // rouge doux
                        containerColor = Color(0xFFD32F2F).copy(alpha = 0.15f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                ElevatedAssistChip(
                    onClick = { showTechPicker = true },
                    label = { Text(step.technique.nameFr) },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    colors = AssistChipDefaults.elevatedAssistChipColors( // bleu doux
                        containerColor = Color(0xFF1976D2).copy(alpha = 0.15f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(Modifier.height(8.dp))
            // Movement: non-null dans le domain → on passe direct
            MovementPickerInline(
                value = step.movement,
                onChange = { onChange(step.copy(movement = it)) }
            )


            Spacer(Modifier.height(8.dp))
            // Height: si nullable côté domain, on force une valeur par défaut d’affichage
            HeightSelector(
                value = step.height,
                onChange = { h -> onChange(step.copy(height = h)) }
            )

            Spacer(Modifier.height(4.dp))
            // Direction: selectors attend Direction non-null → coalesce
            DirectionSelector(
                value = step.direction,
                onChange = { d -> onChange(step.copy(direction = d ?: Direction.NONE)) }
            )

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // ExecutingLimbRole: non-null côté selectors
                ExecutingLimbRoleSelector(
                    value = step.executingLimbRole,
                    onChange = { role: ExecutingLimbRole -> onChange(step.copy(executingLimbRole = role)) }
                )
                // FootLanding: non-null côté selectors
                FootSelector(
                    value = step.footLanding,
                    onChange = { f: FootLanding -> onChange(step.copy(footLanding = f)) }
                )
            }

            Spacer(Modifier.height(4.dp))
            // Tempo: coalesce vers NORMAL pour le contrôle
            TempoSelector(
                value = step.tempo ?: Tempo.NORMAL,
                onChange = { t: Tempo -> onChange(step.copy(tempo = t)) }
            )
        }
    }

    TechniquePickerSheet(
        show = showStartPicker,
        catalog = catalog,
        includeKinds = setOf(TechniqueRef.Kind.POSITION),
        title = "Position de départ",
        onPick = { if (it.kind == TechniqueRef.Kind.POSITION) onChange(step.copy(startPosition = it)) },
        onDismiss = { showStartPicker = false }
    )

    TechniquePickerSheet(
        show = showTechPicker,
        catalog = catalog,
        includeKinds = setOf(TechniqueRef.Kind.DEFENSE, TechniqueRef.Kind.PUNCH, TechniqueRef.Kind.KICK),
        title = "Technique",
        onPick = { if (it.kind != TechniqueRef.Kind.POSITION) onChange(step.copy(technique = it)) },
        onDismiss = { showTechPicker = false }
    )
}

@Composable
private fun MovementPickerInline(
    value: Movement,
    onChange: (Movement) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = listOf(
        Movement.ON_PLACE,
        Movement.FORWARD,
        Movement.BACKWARD,
        Movement.LATERAL
    )
    val advanced = listOf(
        Movement.CHASSE_FORWARD,
        Movement.TIRES_FORWARD,
        Movement.CHASSE_BACK,
        Movement.TIRES_BACK,
        Movement.PIVOT_90_IN,
        Movement.PIVOT_90_OUT,
        Movement.PIVOT_180
    )

    Column(modifier) {
        // Ligne principale
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Déplacement :")
            primary.forEach { m ->
                ElevatedAssistChip(
                    onClick = { onChange(m) },
                    label = { Text(m.labelFr()) },
                    leadingIcon = { if (value == m) Icon(Icons.Default.Check, contentDescription = null) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Avancés (on découpe pour éviter FlowRow/experiments)
        val chunk = 4
        advanced.chunked(chunk).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                row.forEach { m ->
                    ElevatedAssistChip(
                        onClick = { onChange(m) },
                        label = { Text(m.labelFr()) },
                        leadingIcon = { if (value == m) Icon(Icons.Default.Check, contentDescription = null) }
                    )
                }
            }
        }
    }
}

// Petit helper d’affichage (tu peux l’emmener dans une extension utils si tu préfères)
private fun Movement.labelFr(): String = when (this) {
    Movement.NONE          -> "—"
    Movement.ON_PLACE      -> "Sur place"
    Movement.FORWARD       -> "Avancer"
    Movement.BACKWARD      -> "Reculer"
    Movement.LATERAL       -> "Latéral"
    Movement.CHASSE_FORWARD -> "Pas chassé +"
    Movement.TIRES_FORWARD  -> "Pas tiré +"
    Movement.CHASSE_BACK    -> "Pas chassé −"
    Movement.TIRES_BACK     -> "Pas tiré −"
    Movement.PIVOT_90_IN    -> "Pivot 90° int."
    Movement.PIVOT_90_OUT   -> "Pivot 90° ext."
    Movement.PIVOT_180      -> "Pivot 180°"
}
