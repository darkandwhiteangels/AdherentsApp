// com.antechrist.adherentsapp.ui.components.kihon/TechniquePickerSheet.kt
package com.antechrist.adherentsapp.ui.components.kihon

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.TechniqueRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniquePickerSheet(
    show: Boolean,
    catalog: Map<String, TechniqueRef>,
    includeKinds: Set<TechniqueRef.Kind> = setOf(
        TechniqueRef.Kind.DEFENSE, TechniqueRef.Kind.PUNCH, TechniqueRef.Kind.KICK, TechniqueRef.Kind.POSITION
    ),
    title: String = "Choisir une technique",
    onPick: (TechniqueRef) -> Unit,
    onDismiss: () -> Unit
) {
    val filtered = remember(catalog, includeKinds) {
        catalog.values.filter { it.kind in includeKinds }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nameFr })
    }
    var query by remember { mutableStateOf("") }
    val list = remember(query, filtered) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) filtered
        else filtered.filter {
            it.nameFr.lowercase().contains(q) || it.nameJa.lowercase().contains(q) ||
                    it.aliases.any { a -> a.lowercase().contains(q) }
        }
    }
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = title)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    placeholder = { Text("Rechercher (JA/FR/alias)") }
                )

                LazyColumn(
                    contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
                ) {
                    items(list, key = { it.id }) { ref ->
                        TechniqueRowItem(ref = ref, onPick = {
                            onPick(it)
                            onDismiss()
                        })
                        Divider()
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Fermer") }
            }
        }
    }
}

@Composable
private fun TechniqueRowItem(
    ref: TechniqueRef,
    onPick: (TechniqueRef) -> Unit
) {
    ElevatedCard(
        onClick = { onPick(ref) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text = ref.nameFr)
            Text(text = ref.nameJa)
            if (ref.aliases.isNotEmpty()) {
                Text(text = ref.aliases.joinToString(prefix = "Alias: "))
            }
        }
    }
}
