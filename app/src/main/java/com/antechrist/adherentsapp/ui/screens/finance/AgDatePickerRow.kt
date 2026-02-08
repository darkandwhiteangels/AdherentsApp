package com.antechrist.adherentsapp.ui.screens.finance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgDatePickerRow(
    currentAgMillis: Long?,
    onSave: (Long) -> Unit
) {
    val df = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }
    val label = currentAgMillis?.let { df.format(Date(it)) } ?: "Non définie"

    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    // on garde une date “en cours de choix”
    var selectedMillis by remember { mutableLongStateOf(currentAgMillis ?: System.currentTimeMillis()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Date AG : $label", style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }) { Text("Choisir date") }
                OutlinedButton(onClick = { showTime = true }) { Text("Choisir heure") }
                Button(onClick = { onSave(selectedMillis) }) { Text("Enregistrer") }
            }

            Text(
                "Astuce: définis la date/heure, puis Enregistrer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDate) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = selectedMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val d = dateState.selectedDateMillis
                        if (d != null) {
                            // conserve l'heure actuelle de selectedMillis
                            val calOld = Calendar.getInstance().apply { timeInMillis = selectedMillis }
                            val cal = Calendar.getInstance().apply { timeInMillis = d }
                            cal.set(Calendar.HOUR_OF_DAY, calOld.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, calOld.get(Calendar.MINUTE))
                            selectedMillis = cal.timeInMillis
                        }
                        showDate = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Annuler") } }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = selectedMillis }
                        c.set(Calendar.HOUR_OF_DAY, timeState.hour)
                        c.set(Calendar.MINUTE, timeState.minute)
                        selectedMillis = c.timeInMillis
                        showTime = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Annuler") } },
            title = { Text("Choisir l'heure") },
            text = { TimePicker(state = timeState) }
        )
    }
}
