// com/antechrist/adherentsapp/ui/screens/kihon/KihonSequenceDetailScreen.kt
package com.antechrist.adherentsapp.ui.screens.kihon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.model.kihon.*
import com.antechrist.adherentsapp.ui.components.kihon.KihonStatusChip
import com.antechrist.adherentsapp.domain.usecase.GetKihonSequencesByGrade
import com.antechrist.adherentsapp.domain.usecase.PublishKihonSequence
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG_SAVE = "[KihonSave]"

@HiltViewModel
class KihonSequenceDetailViewModel @Inject constructor(
    private val getByGrade: GetKihonSequencesByGrade,
    private val publish: PublishKihonSequence,
    private val auth: FirebaseAuth
) : androidx.lifecycle.ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val sequence: KihonSequence? = null,
        val error: String? = null
    )

    private val gradeKey = MutableStateFlow<String?>(null)
    private val sequenceId = MutableStateFlow<String?>(null)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            combine(gradeKey, sequenceId) { g, id -> g to id }
                .collect { (g, id) ->
                    if (g.isNullOrBlank() || id.isNullOrBlank()) return@collect
                    _ui.value = UiState(loading = true)
                    getByGrade(g).collect { list ->
                        val seq = list.firstOrNull { it.id == id }
                        _ui.value = if (seq != null) UiState(loading = false, sequence = seq)
                        else UiState(loading = false, error = "Séquence introuvable")
                    }
                }
        }
    }

    fun setArgs(grade: String, id: String) {
        gradeKey.value = grade
        sequenceId.value = id
    }

    suspend fun publishNow(): Result<Unit> {
        val seq = _ui.value.sequence ?: return Result.failure(IllegalStateException("Aucune séquence"))
        val uid = auth.currentUser?.uid ?: "unknown"
        return publish(seq.id, uid)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KihonSequenceDetailScreen(
    gradeKey: String,
    sequenceId: String,
    onBack: () -> Unit,
    onEdit: (String, String) -> Unit,
    vm: KihonSequenceDetailViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gradeKey, sequenceId) {
        vm.setArgs(gradeKey, sequenceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail séquence") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    val seq = ui.sequence
                    if (seq != null) {
                        IconButton(onClick = { onEdit(gradeKey, seq.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Éditer")
                        }
                        if (seq.canPublish) {
                            IconButton(onClick = {
                                scope.launch {
                                    vm.publishNow()
                                        .onSuccess { snackbar.showSnackbar("Séquence publiée") }
                                        .onFailure { e -> snackbar.showSnackbar(e.message ?: "Échec publication") }
                                }
                            }) {
                                Icon(Icons.Default.Publish, contentDescription = "Publier")
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                return@Column
            }
            val seq = ui.sequence
            if (seq == null) {
                Text(ui.error ?: "Erreur", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            // Header
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(seq.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        KihonStatusChip(seq.status)
                        AssistChip(onClick = {}, label = { Text("Grade: ${seq.gradeKey}") })
                        if (seq.isExamRequired) AssistChip(onClick = {}, label = { Text("Exigible") })
                    }
                    if (!seq.objective.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Objectif: ${seq.objective}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (seq.tags.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Tags: ${seq.tags.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Étapes (${seq.stepCount})", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ✅ plus de s.index : on clé sur idx + technique.id
                itemsIndexed(seq.steps, key = { idx, s -> "$idx:${s.technique.id}" }) { idx, s ->
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            Text("Étape ${idx + 1}", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text("Position: ${s.startPosition.nameFr} (${s.startPosition.nameJa})")
                            Text("Déplacement: ${movementLabel(s.movement)}")
                            Text("Technique: ${s.technique.nameFr} (${s.technique.nameJa})")

                            val details = buildList {
                                s.height?.let { add("Hauteur: ${heightLabel(it)}") }
                                s.direction?.let { add("Direction: ${directionLabel(it)}") }
                                s.executingLimbRole?.let { add("Membre actif: ${executingRoleLabel(it)}") }
                                s.footLanding?.let { add("Pose du pied: ${footLandingLabel(it)}") }
                                s.tempo?.let { add("Tempo: ${tempoLabel(it)}") }
                                if (s.kiai) add("Kiai")
                            }.joinToString(" • ")

                            if (details.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(details, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

/* ───────────── Labels utilitaires (domain enums top-level) ───────────── */

private fun movementLabel(m: Movement) = when (m) {
    Movement.NONE          -> "—"
    Movement.ON_PLACE      -> "Sur place"
    Movement.FORWARD       -> "Avancer"
    Movement.BACKWARD      -> "Reculer"
    Movement.LATERAL       -> "Latéral"
    Movement.CHASSE_FORWARD -> "Pas chassé (→)"
    Movement.TIRES_FORWARD  -> "Pas tiré (→)"
    Movement.CHASSE_BACK    -> "Pas chassé (←)"
    Movement.TIRES_BACK     -> "Pas tiré (←)"
    Movement.PIVOT_90_IN    -> "Pivot 90° (int.)"
    Movement.PIVOT_90_OUT   -> "Pivot 90° (ext.)"
    Movement.PIVOT_180      -> "Pivot 180°"
}

private fun heightLabel(h: Height) = when (h) {
    Height.JODAN  -> "Jōdan"
    Height.CHUDAN -> "Chūdan"
    Height.GEDAN  -> "Gedan"
}

private fun directionLabel(d: Direction) = when (d) {
    Direction.NONE      -> "—"
    Direction.FORWARD   -> "Avant"
    Direction.BACKWARD  -> "Arrière"
    Direction.LEFT      -> "Gauche"
    Direction.RIGHT     -> "Droite"
    Direction.PIVOT_IN  -> "Pivot intérieur"
    Direction.PIVOT_OUT -> "Pivot extérieur"
}

private fun tempoLabel(t: Tempo) = when (t) {
    Tempo.LENT     -> "Lent"
    Tempo.NORMAL   -> "Normal"
    Tempo.RAPIDE   -> "Rapide"
    Tempo.ENCHAINE -> "Enchaîné"
}

private fun executingRoleLabel(r: ExecutingLimbRole) = when (r) {
    ExecutingLimbRole.NONE       -> "—"
//    ExecutingLimbRole.FRONT_SIDE -> "Côté avant"
//    ExecutingLimbRole.BACK_SIDE  -> "Côté arrière"
//    ExecutingLimbRole.LEAD_LEG   -> "Jambe directrice"
//    ExecutingLimbRole.TRAIL_LEG  -> "Jambe arrière"
    ExecutingLimbRole.BOTH       -> "Opposé"
    ExecutingLimbRole.BRAS_AVANT -> "Bras avant"
    ExecutingLimbRole.BRAS_ARRIERE -> "Bras arrière"
    ExecutingLimbRole.JAMBE_AVANT -> "Jambe avant"
    ExecutingLimbRole.JAMBE_ARRIERE -> "Jambe arrière"
}

private fun footLandingLabel(f: FootLanding) = when (f) {
    FootLanding.NONE        -> "—"
//    FootLanding.FRONT       -> "Pied avant"
//    FootLanding.BACK        -> "Pied arrière"
//    FootLanding.SAME_PLACE  -> "Même place"
    FootLanding.DEVANT -> "Pied avant"
    FootLanding.DERRIERE -> "Pied arrière"
    FootLanding.MEME_ENDROIT -> "Même place"
}
