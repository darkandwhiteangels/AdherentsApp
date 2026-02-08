package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.ui.grade.BeltCatalog

/**
 * Sélecteur de ceinture (v1 Karaté) + barrettes (0..3 si autorisées).
 *
 * @param valueCode code actuel ("karate:jaune_orange", null si aucune)
 * @param valueStripes nombre de barrettes (0..3) — sera clampé selon le grade
 * @param onChange callback quand code/stripes changent
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeltPicker(
    valueCode: String?,
    valueStripes: Int,
    onChange: (code: String?, stripes: Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Ceinture"
) {
    // v1: uniquement Karaté
    val all = remember { BeltCatalog.Karate.specs }
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue("")) }

    val selected = remember(valueCode) { BeltCatalog.Karate.specOf(valueCode) }
    val filtered = remember(query.text, all) {
        val q = query.text.trim().lowercase()
        if (q.isEmpty()) all
        else all.filter { it.label.lowercase().contains(q) }
    }

    val allowsStripes = remember(valueCode) { BeltCatalog.Karate.allowsStripes(valueCode) }
    val clampedStripes = remember(valueCode, valueStripes) {
        BeltCatalog.Karate.clampStripeCount(valueCode, valueStripes)
    }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected?.label ?: "—",
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Champ de recherche
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Rechercher…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Option "Aucune"
                DropdownMenuItem(
                    text = { Text("Aucune ceinture") },
                    onClick = {
                        expanded = false
                        onChange(null, 0)
                    }
                )

                // Liste des grades
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    filtered.forEach { spec ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // ✅ Mini visuel avec insigne (1–5 Dan)
                                    BeltListBadge(
                                        beltCode = spec.code,
                                        stripeCount = 0,
                                        height = 20.dp,
                                        showInsignia = true
                                    )
                                    Text(spec.label, style = MaterialTheme.typography.bodyMedium)
                                }
                            },
                            onClick = {
                                expanded = false
                                val clamped = BeltCatalog.Karate.clampStripeCount(spec.code, valueStripes)
                                onChange(spec.code, clamped)
                            }
                        )
                    }
                }
            }
        }

        // Barrettes 0..3 si autorisées
        Spacer(Modifier.height(10.dp))
        Text("Barrettes", style = MaterialTheme.typography.labelMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            (0..3).forEach { n ->
                val enabled = allowsStripes && n <= 3
                FilterChip(
                    selected = clampedStripes == n,
                    onClick = { if (enabled) onChange(valueCode, n) },
                    enabled = enabled,
                    label = { Text("$n") }
                )
            }
        }

        // Aperçu en direct (✅ insigne inclus)
        Spacer(Modifier.height(12.dp))
        Text("Aperçu", style = MaterialTheme.typography.labelMedium)
        BeltListBadge(
            beltCode = valueCode,
            stripeCount = clampedStripes,
            height = 28.dp,
            showInsignia = true
        )
    }
}
