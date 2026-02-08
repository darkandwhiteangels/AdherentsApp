//package com.antechrist.adherentsapp.ui.screens.kihon
//
//import android.util.Log
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.animateContentSize
//import androidx.compose.animation.core.spring
//import androidx.compose.animation.expandVertically
//import androidx.compose.animation.shrinkVertically
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material.icons.filled.ExpandLess
//import androidx.compose.material.icons.filled.ExpandMore
//import androidx.compose.material.icons.filled.Publish
//import androidx.compose.material.icons.filled.Save
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.antechrist.adherentsapp.domain.model.DraftToken
//import com.antechrist.adherentsapp.domain.model.KihonSequence
//import com.antechrist.adherentsapp.domain.model.kihon.OptionKind
//import com.antechrist.adherentsapp.domain.model.StepDraft
//import com.antechrist.adherentsapp.domain.model.TechniqueRef
//import com.antechrist.adherentsapp.domain.model.kihon.*
//import com.antechrist.adherentsapp.ui.components.kihon.KihonEditorToolBar
//import com.antechrist.adherentsapp.ui.components.kihon.StepEditorRow
//import com.antechrist.adherentsapp.ui.theme.extraColors
//import kotlinx.coroutines.launch
//import androidx.compose.foundation.layout.FlowRow
//import androidx.compose.foundation.layout.ExperimentalLayoutApi
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.ui.text.style.TextOverflow
//
//private const val TAG_SCREEN = "KihonEditorScreen"
//private const val TAG_SAVE = "[KihonSave]"
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun KihonSequenceEditorScreen(
//    gradeKey: String,
//    sequenceId: String,
//    onBack: () -> Unit,
//    onOpenDetail: (String, String) -> Unit,
//    vm: KihonSequenceEditorViewModel = hiltViewModel()
//) {
//    // Vérif d’identité d’instance VM (doit matcher partout)
//    val vmId = remember { System.identityHashCode(vm) }
//    LaunchedEffect(vmId) { }
//
//    val ui by vm.ui.collectAsState()
//    val snackbar = remember { SnackbarHostState() }
//    val scope = rememberCoroutineScope()
//
//    var metaExpanded by rememberSaveable { mutableStateOf(true) }
//    var boExpanded by rememberSaveable { mutableStateOf(false) }
//
//    LaunchedEffect(gradeKey, sequenceId) {
//        vm.setArgs(gradeKey, sequenceId)
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Éditer séquence") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
//                    }
//                },
//                actions = {
//                    IconButton(
//                        enabled = ui.sequence?.isEditable == true,
//                        onClick = {
//                            Log.d(TAG_SAVE, "UI: save click → calling vm.saveSequence()")
//                            scope.launch {
//                                vm.saveNow()
//                                    .onSuccess { snackbar.showSnackbar("Séquence enregistrée") }
//                                    .onFailure { e ->
//                                        snackbar.showSnackbar(e.message ?: "Échec enregistrement")
//                                    }
//                            }
//                        }
//                    ) { Icon(Icons.Default.Save, contentDescription = "Enregistrer") }
//
//                    val canPublish = ui.sequence?.canPublish == true
//                    IconButton(
//                        enabled = canPublish,
//                        onClick = {
//                            scope.launch {
//                                vm.publishNow()
//                                    .onSuccess {
//                                        snackbar.showSnackbar("Séquence publiée")
//                                        onOpenDetail(gradeKey, sequenceId)
//                                    }
//                                    .onFailure { e ->
//                                        snackbar.showSnackbar(e.message ?: "Échec publication")
//                                    }
//                            }
//                        }
//                    ) { Icon(Icons.Default.Publish, contentDescription = "Publier") }
//                }
//            )
//        },
//        snackbarHost = { SnackbarHost(snackbar) },
//        bottomBar = {
//            // ─── Barre d’outils (branchée sur le même VM) ─────────────
//            val positions: List<TechniqueRef> = ui.catalog.values
//                .filter { it.kind == TechniqueRef.Kind.POSITION }
//                .sortedBy { it.nameJa }
//
//            val techniquesPoing: List<TechniqueRef> = ui.catalog.values
//                .filter { it.kind == TechniqueRef.Kind.PUNCH }
//                .sortedBy { it.nameJa }
//
//            val techniquesPied: List<TechniqueRef> = ui.catalog.values
//                .filter { it.kind == TechniqueRef.Kind.KICK }
//                .sortedBy { it.nameJa }
//
//            val techniquesDefense: List<TechniqueRef> = ui.catalog.values
//                .filter { it.kind == TechniqueRef.Kind.DEFENSE }
//                .sortedBy { it.nameJa }
//
//            // 👉 OBSERVE le draft via StateFlow (pas vm.stepDraft direct)
//            val draft by vm.draftState.collectAsState()
//            LaunchedEffect(draft.tokens.size, draft.selectedIndex) {
//
//            }
//
//            KihonEditorToolBar(
//                draft = draft,
//                positions = positions,
//                techniquesPied = techniquesPied,
//                techniquesPoing = techniquesPoing,
//                techniquesDefense = techniquesDefense,
//                onPickMovement = vm::onPickMovement,
//                onPickPosition = vm::onPickPosition,
//                onPickTechnique = vm::onPickTechnique,
//                onPickHeight = vm::onPickHeight,
//                onPickOptions = vm::onPickOptions,
//                expanded = boExpanded,
//                onExpandedChange = { expanded ->
//                    boExpanded = expanded
//                    if (expanded) metaExpanded = false  // ouvrir BO => refermer métadonnées
//                },
//                onPickExecutingLimb = vm::onPickExecutingLimb,
//                onPickFootLanding = vm::onPickFootLanding,
//                onToggleSameArm = vm::toggleSameArm,
//                onToggleSameLeg = vm::toggleSameLeg,
//                //onToggleKiai = vm::toggleKiai
//            )
//        }
//    ) { padding ->
//
//        if (ui.loading) {
//            Column(
//                modifier = Modifier
//                    .padding(padding)
//                    .fillMaxSize()
//                    .padding(16.dp)
//            ) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
//            return@Scaffold
//        }
//
//        val seq = ui.sequence
//        if (seq == null) {
//            Column(
//                modifier = Modifier
//                    .padding(padding)
//                    .fillMaxSize()
//                    .padding(16.dp),
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) { Text(ui.error ?: "Erreur", color = MaterialTheme.colorScheme.error) }
//            return@Scaffold
//        }
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(horizontal = 16.dp)
//        ) {
//            // ─── Métadonnées séquence ──────────────────────────────
//            ElevatedCard(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .animateContentSize()
//            ) {
//                Column(Modifier.padding(12.dp)) {
//
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clip(RoundedCornerShape(12.dp))
//                            .clickable {
//                                val next = !metaExpanded
//                                metaExpanded = next
//                                if (next) boExpanded = false
//                            }
//                            .padding(horizontal = 8.dp, vertical = 6.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.spacedBy(10.dp)
//                        ) {
//                            Text(
//                                "Métadonnées",
//                                style = MaterialTheme.typography.titleMedium,
//                                fontWeight = FontWeight.SemiBold
//                            )
//                            AssistChip(onClick = { }, label = { Text("Grade: ${seq.gradeKey}") })
//                        }
//                        Icon(
//                            imageVector = if (metaExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
//                            contentDescription = if (metaExpanded) "Replier" else "Déplier"
//                        )
//                    }
//
//                    AnimatedVisibility(
//                        visible = metaExpanded,
//                        enter = expandVertically(animationSpec = spring()),
//                        exit = shrinkVertically(animationSpec = spring())
//                    ) {
//                        Column {
//                            Spacer(Modifier.height(8.dp))
//
//                            OutlinedTextField(
//                                value = ui.nameDraft,
//                                onValueChange = vm::onNameChange,
//                                label = { Text("Nom de la séquence") },
//                                modifier = Modifier.fillMaxWidth(),
//                                singleLine = true
//                            )
//
//                            Spacer(Modifier.height(8.dp))
//
//                            OutlinedTextField(
//                                value = ui.objectiveDraft,
//                                onValueChange = vm::onObjectiveChange,
//                                label = { Text("Objectif pédagogique") },
//                                modifier = Modifier.fillMaxWidth()
//                            )
//
//                            Spacer(Modifier.height(8.dp))
//
//                            val tagsText = remember(ui.tagsDraft) { ui.tagsDraft.joinToString(", ") }
//                            OutlinedTextField(
//                                value = tagsText,
//                                onValueChange = { input ->
//                                    val tags = input.split(",")
//                                        .map { it.trim() }
//                                        .filter { it.isNotEmpty() }
//                                    vm.onTagsChange(tags)
//                                },
//                                label = { Text("Tags (séparés par des virgules)") },
//                                modifier = Modifier.fillMaxWidth()
//                            )
//
//                            Spacer(Modifier.height(8.dp))
//
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(8.dp)
//                            ) {
//                                Text("Exigible passage")
//                                Switch(
//                                    checked = ui.examDraft,
//                                    onCheckedChange = vm::onExamRequiredChange
//                                )
//
//                                FilterChip(
//                                    selected = seq.status == KihonSequence.Status.READY,
//                                    onClick = { vm.markReady() },
//                                    label = {
//                                        Text(
//                                            if (seq.status == KihonSequence.Status.READY) "Prêt" else "Marquer prêt"
//                                        )
//                                    },
//                                    enabled = seq.steps.isNotEmpty() && seq.status != KihonSequence.Status.PUBLISHED
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//
//            Spacer(Modifier.height(12.dp))
//
//            val draft by vm.draftState.collectAsState()
//            LaunchedEffect(draft.tokens.size, draft.selectedIndex) { }
//
//            val isDraftZoneExpanded = !boExpanded && !metaExpanded
//            val draftZoneModifier = if (isDraftZoneExpanded) {
//                Modifier.weight(1f, fill = true)   // ⬅️ prend de la place quand tout est fermé
//            } else {
//                Modifier.wrapContentHeight()       // ⬅️ comportement tiroir sinon
//            }
//
//            //StepDraftPreview(draft = draft , vm = vm)
//            StepDraftPreview(
//                draft = draft,
//                vm = vm,
//                boExpanded = boExpanded,
//                metaExpanded = metaExpanded,
//                modifier = draftZoneModifier        // ⬅️ clé : on passe le poids ici
//            )
//
//            HorizontalDivider(Modifier, DividerDefaults.Thickness, MaterialTheme.extraColors.certifiedBlue)
//
//            Spacer(Modifier.height(8.dp))
//
//            // ─── Liste des steps validés ────────────────────────────────────────
//            LazyColumn(
//                contentPadding = PaddingValues(bottom = 96.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp),
//                modifier = Modifier.weight(1f)
//            ) {
//                itemsIndexed(
//                    seq.steps,
//                    key = { idx, s -> "${idx}:${s.technique.id}" }
//                ) { index, step ->
//                    StepEditorRow(
//                        step = step,
//                        catalog = ui.catalog,
//                        onChange = { vm.updateStep(index, it) },
//                        onRemove = { vm.removeStep(index) },
//                        onMoveUp = { vm.moveStepUp(index) },
//                        onMoveDown = { vm.moveStepDown(index) },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }
//            }
//        }
//    }
//}
//
///* ─────────────────────────────── Sous-composants ─────────────────────────────── */
//
//@OptIn(ExperimentalLayoutApi::class)
//@Composable
//private fun StepDraftPreview(
//    modifier: Modifier = Modifier,
//    draft: StepDraft,
//    vm: KihonSequenceEditorViewModel,
//    boExpanded: Boolean,          // ⬅️ ajout
//    metaExpanded: Boolean         // ⬅️ ajout
//) {
//    val minHeight = 168.dp
//
//    LaunchedEffect(draft.tokens.size, draft.selectedIndex) {}
//
//    Surface(
//        tonalElevation = 1.dp,
//        shape = RoundedCornerShape(12.dp),
////        modifier = Modifier
////            .fillMaxWidth()
////            .padding(horizontal = 16.dp, vertical = 8.dp)
////            .heightIn(min = minHeight)
////            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
//        modifier = modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp)
//            .heightIn(min = 168.dp)
//            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(horizontal = 4.dp, vertical = 8.dp)
//        ) {
//            Text(
//                "Étape en cours",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//
//            val isExpanded = !boExpanded && !metaExpanded
//            val draftScroll = rememberScrollState()
//
//            val chipsContainer =
//                if (isExpanded) {
//                    // Mode étendu : NE PAS borner en hauteur ici, le parent nous a déjà donné de la place
//                    Modifier
//                        .fillMaxWidth()
//                        .verticalScroll(draftScroll)
//                    // pas de heightIn(max=...) ni de weight ici
//                } else {
//                    // Mode tiroir : borne la hauteur pour éviter l’ascenseur global
//                    Modifier
//                        .fillMaxWidth()
//                        .heightIn(max = 168.dp)
//                        .verticalScroll(draftScroll)
//                }
//
//            if (draft.tokens.isEmpty()) {
//                Box(
//                    modifier = chipsContainer,
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        "Choisis des éléments dans la barre d’outils pour construire l’étape.",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            } else {
//                Box(modifier = chipsContainer) {
//                    FlowRow(
//                        horizontalArrangement = Arrangement.spacedBy(8.dp),
//                        verticalArrangement = Arrangement.spacedBy(8.dp),
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        draft.tokens.forEachIndexed { index, token ->
//                            val isSelected = draft.selectedIndex == index
//                            var offsetX by remember { mutableFloatStateOf(0f) }
//
//                            AssistChip(
//                                onClick = { vm.selectDraftToken(index) },
//                                label = {
//                                    Text(
//                                        text = tokenLabelSafe(token),
//                                        maxLines = 1,
//                                        softWrap = false,
//                                        overflow = TextOverflow.Ellipsis
//                                    )
//                                },
//                                leadingIcon = {
//                                    if (isSelected) {
//                                        Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
//                                    }
//                                },
//                                trailingIcon = {
//                                    Icon(
//                                        imageVector = Icons.Default.Close,
//                                        contentDescription = "Supprimer",
//                                        modifier = Modifier
//                                            .padding(start = 4.dp)
//                                            .size(18.dp)
//                                            .clickable { vm.removeDraftToken(index) }
//                                    )
//                                },
//                                colors = tokenColors(token, isSelected),
//                                modifier = Modifier
//                                    .heightIn(min = 36.dp)
//                                    .pointerInput(draft.tokens.size) {
//                                        detectDragGestures(
//                                            onDrag = { change, dragAmount ->
//                                                change.consume()
//                                                offsetX += dragAmount.x
//                                                val last = draft.tokens.lastIndex
//                                                if (offsetX > 40 && index < last) {
//                                                    vm.moveDraftToken(index, index + 1); offsetX = 0f
//                                                } else if (offsetX < -40 && index > 0) {
//                                                    vm.moveDraftToken(index, index - 1); offsetX = 0f
//                                                }
//                                            },
//                                            onDragEnd = { offsetX = 0f },
//                                            onDragCancel = { offsetX = 0f }
//                                        )
//                                    }
//                            )
//                        }
//                    }
//                }
//            }
//
//            Spacer(Modifier.height(8.dp))
//
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
//                Button(onClick = { vm.commitDraftAsStep() }, enabled = draft.isComplete) {
//                    Text("Ajouter l’étape")
//                }
//            }
//        }
//    }
//}
//
//
///* ───────── Helpers d’affichage (labels + couleurs par catégorie) ───────── */
//
//@Composable
//private fun tokenColors(token: DraftToken, selected: Boolean): ChipColors {
//    // Déplacements = doré, Positions = rouge, Techniques = bleu, Options = vert
//    val base: Color = when (token) {
//        is DraftToken.MovementToken -> Color(0xFFD4AF37) // Gold
//        is DraftToken.PositionToken -> Color(0xFFD32F2F) // Red 700
//        is DraftToken.TechniqueToken -> Color(0xFF1976D2) // Blue 700
//        is DraftToken.OptionToken -> Color(0xFF388E3C) // Green 700
//    }
//
//    val container = if (selected) base else base.copy(alpha = 0.18f)
//    val label = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
//    val icon = label
//
//    return AssistChipDefaults.assistChipColors(
//        containerColor = container,
//        labelColor = label,
//        leadingIconContentColor = icon,
//        trailingIconContentColor = icon
//    )
//}
//
//private fun tokenLabelSafe(token: DraftToken): String = when (token) {
//    is DraftToken.MovementToken -> when (token.movement) {
//        Movement.ON_PLACE       -> "Sur place"
//        Movement.FORWARD        -> "En avançant"
//        Movement.BACKWARD       -> "En reculant"
//        Movement.CHASSE_FORWARD -> "Pas chassé avant"
//        Movement.CHASSE_BACK    -> "Pas chassé arrière"
//        Movement.TIRES_FORWARD  -> "Pas tiré avant"
//        Movement.TIRES_BACK     -> "Pas tiré arrière"
//        Movement.NONE           -> "—"
//        Movement.LATERAL        -> "Sur le côté"
//        Movement.PIVOT_90_IN    -> "Tourné 90° gauche"
//        Movement.PIVOT_90_OUT   -> "Tourné 90° droite"
//        Movement.PIVOT_180      -> "Demi-tour"
//    }
//
//    is DraftToken.PositionToken ->
//        token.position.nameJa.takeIf { it.isNotBlank() } ?: token.position.nameFr
//
//    is DraftToken.TechniqueToken ->
//        token.technique.nameJa.takeIf { it.isNotBlank() } ?: token.technique.nameFr
//
//    is DraftToken.OptionToken -> when (token.option) {
//        OptionKind.KIAI -> "Kiai"
//
//        OptionKind.FOOT_LANDING -> when (token.value as? FootLanding) {
////            FootLanding.FRONT      -> "Repose devant"
////            FootLanding.BACK       -> "Repose derrière"
////            FootLanding.SAME_PLACE -> "Même place"
//            FootLanding.DEVANT -> "Repose devant"
//            FootLanding.DERRIERE -> "Repose derrière"
//            FootLanding.MEME_ENDROIT -> "Même place"
//            FootLanding.NONE, null -> "Pied"
//        }
//
//        OptionKind.EXECUTING_LIMB -> when (token.value as? ExecutingLimbRole) {
////            ExecutingLimbRole.FRONT_SIDE -> "Bras avant"
////            ExecutingLimbRole.BACK_SIDE  -> "Bras arrière"
//            ExecutingLimbRole.BOTH       -> "Opposé"
//            ExecutingLimbRole.NONE, null -> "Membre"
//            //ExecutingLimbRole.LEAD_LEG   -> "Jambe directrice"
//            //ExecutingLimbRole.TRAIL_LEG  -> "Jambe suiveuse"
//            ExecutingLimbRole.BRAS_AVANT -> "Bras avant"
//            ExecutingLimbRole.BRAS_ARRIERE -> "Bras arrière"
//            ExecutingLimbRole.JAMBE_AVANT -> "Jambe avant"
//            ExecutingLimbRole.JAMBE_ARRIERE -> "Jambe arrière"
//        }
//
//        OptionKind.DIRECTION -> when (token.value as? Direction) {
//            null -> "Direction"
//            else -> "Direction" // à détailler si besoin
//        }
//
//        OptionKind.TEMPO -> when (token.value as? Tempo) {
//            null -> "Tempo"
//            else -> "Tempo"     // à détailler si besoin
//        }
//
//        OptionKind.HEIGHT -> when (token.value as? Height) {
//            Height.JODAN  -> "Jodan"
//            Height.CHUDAN -> "Chudan"
//            Height.GEDAN  -> "Gedan"
//            null          -> "Niveau"
//        }
//
//        OptionKind.SAME_ARM -> "Même bras"
//        OptionKind.SAME_LEG -> "Même jambe"
//    }
//}
//
////@Composable
////private fun tokenLabel(t: DraftToken): String = when (t) {
////    is DraftToken.MovementToken -> "Dépl.: " + movementLabel(t.movement) // FR
////    is DraftToken.PositionToken -> "Pos.: " + (t.position.nameJa.ifBlank { t.position.nameFr })
////    is DraftToken.TechniqueToken -> buildString {
////        append("Tech.: ")
////        append(t.technique.nameJa.ifBlank { t.technique.nameFr })
////        t.height?.let { append(" · ${it.name}") }
////        t.direction?.let { append(" · ${it.name.lowercase()}") }
////        t.executingLimbRole?.let { append(" · ${it.name.lowercase()}") }
////    }
////    is DraftToken.OptionToken -> when (t.option) {
////        OptionKind.KIAI -> "Kiai"
////        OptionKind.FOOT_LANDING -> "Pied: ${(t.value as? FootLanding)?.name?.lowercase() ?: ""}"
////        OptionKind.EXECUTING_LIMB -> "Membre: ${(t.value as? ExecutingLimbRole)?.name?.lowercase() ?: ""}"
////        OptionKind.DIRECTION -> "Dir.: ${(t.value as? Direction)?.name?.lowercase() ?: ""}"
////        OptionKind.TEMPO -> "Tempo: ${(t.value as? Tempo)?.name?.lowercase() ?: ""}"
////        OptionKind.HEIGHT -> "Niveau: ${(t.value as? Height)?.name ?: ""}"
////    }
////}
package com.antechrist.adherentsapp.ui.screens.kihon

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.model.kihon.*
import com.antechrist.adherentsapp.ui.components.kihon.KihonEditorToolBar
import com.antechrist.adherentsapp.ui.components.kihon.StepEditorRow
import com.antechrist.adherentsapp.ui.theme.extraColors
import kotlinx.coroutines.launch

// ▼▼▼ V2 (tokens) : imports du contrat VM/intent + TokenList UI
import com.antechrist.adherentsapp.domain.editor.*
import com.antechrist.adherentsapp.domain.model.kihon.TokenType
import com.antechrist.adherentsapp.ui.components.kihon.TokenList

private const val TAG_SCREEN = "KihonEditorScreen"
private const val TAG_SAVE = "[KihonSave]"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KihonSequenceEditorScreen(
    gradeKey: String,
    sequenceId: String,
    onBack: () -> Unit,
    onOpenDetail: (String, String) -> Unit,
    vm: KihonSequenceEditorViewModel = hiltViewModel()
) {
    val vmId = remember { System.identityHashCode(vm) }
    LaunchedEffect(vmId) { }

    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var metaExpanded by rememberSaveable { mutableStateOf(true) }
    var boExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(gradeKey, sequenceId) {
        vm.setArgs(gradeKey, sequenceId)
    }

    // ─────────────────────────────────────────────────────────────
    // V2: VM token-by-token (locale), + mapping de ton catalogue actuel
    // ─────────────────────────────────────────────────────────────
    val vmV2 = remember { KihonSequenceEditorViewModelV2(sequenceId = sequenceId) }
    val editorState by vmV2.state.collectAsState()

    // Mappe les techniques connues vers CatalogState (on branchera movements/options/levels ensuite)
    LaunchedEffect(ui.catalog) {
        val techniquesMap = ui.catalog.values.associate { ref ->
            ref.id to TechniqueUiRef(id = ref.id, nameFr = ref.nameFr, nameJa = ref.nameJa)
        }
        vmV2.updateCatalog(
            CatalogState(
                isReady = true, // au moins pour les techniques ; mouvements/options/levels viendront après
                techniques = techniquesMap,
                movements = emptyMap(),
                options = emptyMap(),
                levels = emptyMap()
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Éditer séquence") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        enabled = ui.sequence?.isEditable == true,
                        onClick = {
                            Log.d(TAG_SAVE, "UI: save click → calling vm.saveSequence()")
                            scope.launch {
                                vm.saveNow()
                                    .onSuccess { snackbar.showSnackbar("Séquence enregistrée") }
                                    .onFailure { e ->
                                        snackbar.showSnackbar(e.message ?: "Échec enregistrement")
                                    }
                            }
                        }
                    ) { Icon(Icons.Default.Save, contentDescription = "Enregistrer") }

                    val canPublish = ui.sequence?.canPublish == true
                    IconButton(
                        enabled = canPublish,
                        onClick = {
                            scope.launch {
                                vm.publishNow()
                                    .onSuccess {
                                        snackbar.showSnackbar("Séquence publiée")
                                        onOpenDetail(gradeKey, sequenceId)
                                    }
                                    .onFailure { e ->
                                        snackbar.showSnackbar(e.message ?: "Échec publication")
                                    }
                            }
                        }
                    ) { Icon(Icons.Default.Publish, contentDescription = "Publier") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // ⚠️ Barre d’outils legacy (encore branchée sur vm “steps”)
            // Prochain micro-lot: on rebranchera cette BO sur vmV2.addX(...)
            val positions: List<TechniqueRef> = ui.catalog.values
                .filter { it.kind == TechniqueRef.Kind.POSITION }
                .sortedBy { it.nameJa }

            val techniquesPoing: List<TechniqueRef> = ui.catalog.values
                .filter { it.kind == TechniqueRef.Kind.PUNCH }
                .sortedBy { it.nameJa }

            val techniquesPied: List<TechniqueRef> = ui.catalog.values
                .filter { it.kind == TechniqueRef.Kind.KICK }
                .sortedBy { it.nameJa }

            val techniquesDefense: List<TechniqueRef> = ui.catalog.values
                .filter { it.kind == TechniqueRef.Kind.DEFENSE }
                .sortedBy { it.nameJa }

            val draft by vm.draftState.collectAsState()

            KihonEditorToolBar(
                draft = draft,
                positions = positions,
                techniquesPied = techniquesPied,
                techniquesPoing = techniquesPoing,
                techniquesDefense = techniquesDefense,
                onPickMovement = vm::onPickMovement,
                onPickPosition = vm::onPickPosition,
                onPickTechnique = vm::onPickTechnique,
                onPickHeight = vm::onPickHeight,
                onPickOptions = vm::onPickOptions,
                expanded = boExpanded,
                onExpandedChange = { expanded ->
                    boExpanded = expanded
                    if (expanded) metaExpanded = false
                },
                onPickExecutingLimb = vm::onPickExecutingLimb,
                onPickFootLanding = vm::onPickFootLanding,
                onToggleSameArm = vm::toggleSameArm,
                onToggleSameLeg = vm::toggleSameLeg,
            )
        }
    ) { padding ->

        if (ui.loading) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            return@Scaffold
        }

        val seq = ui.sequence
        if (seq == null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { Text(ui.error ?: "Erreur", color = MaterialTheme.colorScheme.error) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // ─── Métadonnées séquence ──────────────────────────────
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(Modifier.padding(12.dp)) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val next = !metaExpanded
                                metaExpanded = next
                                if (next) boExpanded = false
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Métadonnées",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            AssistChip(onClick = { }, label = { Text("Grade: ${seq.gradeKey}") })
                        }
                        Icon(
                            imageVector = if (metaExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (metaExpanded) "Replier" else "Déplier"
                        )
                    }

                    AnimatedVisibility(
                        visible = metaExpanded,
                        enter = expandVertically(animationSpec = spring()),
                        exit = shrinkVertically(animationSpec = spring())
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = ui.nameDraft,
                                onValueChange = vm::onNameChange,
                                label = { Text("Nom de la séquence") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = ui.objectiveDraft,
                                onValueChange = vm::onObjectiveChange,
                                label = { Text("Objectif pédagogique") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            val tagsText = remember(ui.tagsDraft) { ui.tagsDraft.joinToString(", ") }
                            OutlinedTextField(
                                value = tagsText,
                                onValueChange = { input ->
                                    val tags = input.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                    vm.onTagsChange(tags)
                                },
                                label = { Text("Tags (séparés par des virgules)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Exigible passage")
                                Switch(
                                    checked = ui.examDraft,
                                    onCheckedChange = vm::onExamRequiredChange
                                )

                                FilterChip(
                                    selected = seq.status == KihonSequence.Status.READY,
                                    onClick = { vm.markReady() },
                                    label = {
                                        Text(
                                            if (seq.status == KihonSequence.Status.READY) "Prêt" else "Marquer prêt"
                                        )
                                    },
                                    enabled = seq.steps.isNotEmpty() && seq.status != KihonSequence.Status.PUBLISHED
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─────────────────────────────────────────────────────────────
            // ZONE D'ÉDITION (remplace StepDraftPreview) → TokenList (V2)
            // ─────────────────────────────────────────────────────────────
            val isDraftZoneExpanded = !boExpanded && !metaExpanded
            val draftZoneModifier = if (isDraftZoneExpanded) {
                Modifier.weight(1f, fill = true)
            } else {
                Modifier.wrapContentHeight()
            }

            TokenList(
                state = editorState,
                onSetCursor = { vmV2.dispatch(EditorIntent.SetCursor(it)) },
                onMove = { id, to -> vmV2.dispatch(EditorIntent.MoveToken(id, to)) },
                onRemove = { id -> vmV2.dispatch(EditorIntent.RemoveToken(id)) },
                onReplaceRequest = { id, type ->
                    // Prochain lot : ouvrir le picker correspondant (tech/mvt/option/level)
                    // et appeler ReplaceToken(id, newRefId)
                    scope.launch { snackbar.showSnackbar("Remplacer ${type.name.lowercase()} (picker à venir)") }
                },
                modifier = draftZoneModifier.fillMaxWidth()
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, MaterialTheme.extraColors.certifiedBlue)

            Spacer(Modifier.height(8.dp))

            // ─── Liste des steps validés (legacy) ─────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(
                    seq.steps,
                    key = { idx, s -> "${idx}:${s.technique.id}" }
                ) { index, step ->
                    StepEditorRow(
                        step = step,
                        catalog = ui.catalog,
                        onChange = { vm.updateStep(index, it) },
                        onRemove = { vm.removeStep(index) },
                        onMoveUp = { vm.moveStepUp(index) },
                        onMoveDown = { vm.moveStepDown(index) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
