// com.antechrist.adherentsapp/ui/screens/kihon/KihonGradeListScreen.kt
package com.antechrist.adherentsapp.ui.screens.kihon

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.model.kihon.KihonStep
import com.antechrist.adherentsapp.ui.components.kihon.KihonStatusChip
import com.antechrist.adherentsapp.ui.theme.extraColors
import kotlinx.coroutines.launch
import com.antechrist.adherentsapp.domain.model.kihon.Movement


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KihonGradeListScreen(
    gradeKey: String,
    onBack: () -> Unit,
    onOpenDetail: (String, String) -> Unit,  // (gradeKey, sequenceId)
    onOpenEditor: (String, String) -> Unit,  // (gradeKey, sequenceId)
    vm: KihonGradeListViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var pendingDelete by remember { mutableStateOf<KihonSequence?>(null) }

    LaunchedEffect(gradeKey) { vm.setGradeKey(gradeKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Séquenceur — Grade $gradeKey") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        vm.createDraftSequence()
                            .onSuccess { newId ->
                                onOpenEditor(gradeKey, newId)
                            }
                            .onFailure { e ->
                                snackbar.showSnackbar(e.message ?: "Création impossible")
                            }
                    }
                }
            ) { Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Nouvelle séquence",
                tint = MaterialTheme.extraColors.dorureDojo
            ) }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))

            // 🔎 Recherche sur toute la largeur
            SearchField(
                value = ui.query,
                onValueChange = vm::setQuery,
                onClear = vm::clearQuery,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // 🧪 Filtres en dessous (scroll horizontal si besoin)
            StatusFilterRow(
                current = ui.statusFilter,
                onSelect = vm::setStatusFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            )

            Spacer(Modifier.height(8.dp))

            ui.error?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null)
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ui.items, key = { it.id }) { seq ->
                    SequenceListItem(
                        seq = seq,
                        onOpenDetail = { onOpenDetail(gradeKey, seq.id) },
                        onOpenEditor = { onOpenEditor(gradeKey, seq.id) },
                        onRequestDelete = { pendingDelete = seq }
                    )
                }
            }
        }
    }

    // 🗑️ Dialogue de confirmation de suppression
    pendingDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer la séquence ?") },
            text = {
                Text("Voulez-vous vraiment supprimer « ${toDelete.name} » ? Cette action est irréversible.")
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val id = toDelete.id
                        pendingDelete = null
                        scope.launch {
                            vm.deleteSequence(id)
                                .onSuccess {
                                    snackbar.showSnackbar("Séquence supprimée.")
                                }
                                .onFailure { e ->
                                    snackbar.showSnackbar(e.message ?: "Suppression impossible.")
                                }
                        }
                    }
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            }
        )
    }
}

/* ————————————————————— Helpers UI ————————————————————— */

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Clear, contentDescription = "Effacer")
                }
            }
        },
        placeholder = { Text("Rechercher une séquence…") },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun StatusFilterRow(
    current: KihonSequence.Status?,
    onSelect: (KihonSequence.Status?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        FilterChip(
            selected = current == null,
            onClick = { onSelect(null) },
            label = { Text("Tous") }
        )
        FilterChip(
            selected = current == KihonSequence.Status.DRAFT,
            onClick = { onSelect(KihonSequence.Status.DRAFT) },
            label = { Text("Brouillon") }
        )
        FilterChip(
            selected = current == KihonSequence.Status.READY,
            onClick = { onSelect(KihonSequence.Status.READY) },
            label = { Text("Prêt") }
        )
        FilterChip(
            selected = current == KihonSequence.Status.PUBLISHED,
            onClick = { onSelect(KihonSequence.Status.PUBLISHED) },
            label = { Text("Publié") }
        )
    }
}

@Composable
private fun SequenceListItem(
    seq: KihonSequence,
    onOpenDetail: () -> Unit,
    onOpenEditor: () -> Unit,
    onRequestDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() }
    ) {
        Column(Modifier.padding(12.dp)) {

            // Ligne 1 — Titre à gauche, actions à droite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = seq.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Row {
                    IconButton(onClick = onOpenEditor) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Éditer",
                            tint = MaterialTheme.extraColors.certifiedBlue
                        )
                    }
                    IconButton(onClick = onRequestDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.extraColors.rougeTatamie
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Lignes 2 & 3 — Séquence JP puis FR
            val jpLine = joinTechLine(seq, isJapanese = true)
            val frLine = joinTechLine(seq, isJapanese = false)

            if (jpLine.isNotBlank()) {
                Text(
                    text = jpLine,
                    style = MaterialTheme.typography.bodyLarge, // si tu n'as pas de typo perso, remplace par typography.bodyLarge
                    color = MaterialTheme.extraColors.certifiedBlue
                )
            }
            if (frLine.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = frLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extraColors.rougeTatamie
                )
            }

            // Ligne 4 — Statut aligné à droite
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                KihonStatusChip(seq.status)
            }
        }
    }
}

/* ————————————————————— Helpers ————————————————————— */

private fun joinTechLine(seq: KihonSequence, isJapanese: Boolean): String {
    // Concatène: "<position> [mouvement] <technique>" pour chaque step
    // JP: symbole (→ ← ↔ » ↺)
    // FR: libellé ("avancer", "reculer", "latéral", "pas chassé", ...)
    val parts = seq.steps.mapNotNull { step ->
        val tech = if (isJapanese) step.technique.nameJa else step.technique.nameFr
        val pos = if (isJapanese) step.startPosition.nameJa else step.startPosition.nameFr

        val techLabel = tech.trim()
        val posLabel = pos.trim()

        val m = step.movement
        val moveChunk = when {
            m == Movement.NONE -> ""                       // rien à afficher
            isJapanese -> " ${movementLabelJa(m)} "                     // JA
            else -> " ${movementLabelFr(m)} "                           // FR
        }

        when {
            techLabel.isNotEmpty() && posLabel.isNotEmpty() -> moveChunk + posLabel + techLabel
            techLabel.isNotEmpty() -> techLabel                       // fallback si position manquante
            posLabel.isNotEmpty() -> posLabel                         // fallback si technique manquante
            else -> null
        }
    }
    return parts.joinToString(separator = " · ")
}

private fun movementLabelJa(m: Movement): String = when (m) {
    Movement.NONE          -> "—"
    Movement.ON_PLACE      -> "Sono-ba"
    Movement.FORWARD       -> "Ayumi-ashi"          // avancer
    Movement.BACKWARD      -> "Iki-ashi"            // reculer
    Movement.LATERAL       -> "Yoko-ashi"           // latéral
    Movement.CHASSE_FORWARD -> "Tsugi-ashi"         // pas chassé en avançant
    Movement.TIRES_FORWARD  -> "Yori-ashi"          // pas tiré / glissé en avançant
    Movement.CHASSE_BACK    -> "Tsugi-ashi"         // pas chassé en reculant
    Movement.TIRES_BACK     -> "Yori-ashi"          // pas tiré / glissé en reculant
    Movement.PIVOT_90_IN    -> "Mawari-ashi"        // pivot jambe avant
    Movement.PIVOT_90_OUT   -> "Ushiro mawari-ashi" // pivot jambe arrière
    Movement.PIVOT_180      -> "Ushiro mawari-ashi" // idem 180°
}

private fun movementLabelFr(m: Movement): String = when (m) {
    Movement.NONE          -> "—"
    Movement.ON_PLACE      -> "Sur place"
    Movement.FORWARD       -> "avancer"
    Movement.BACKWARD      -> "reculer"
    Movement.LATERAL       -> "latéral"
    Movement.CHASSE_FORWARD -> "pas chassé avancer"
    Movement.TIRES_FORWARD  -> "pas tirés avancer"
    Movement.CHASSE_BACK    -> "pas chassé reculer"
    Movement.TIRES_BACK     -> "pas tirés reculer"
    Movement.PIVOT_90_IN    -> "pivot 90° intérieur"
    Movement.PIVOT_90_OUT   -> "pivot 90° extérieur"
    Movement.PIVOT_180      -> "pivot 180°"
}
