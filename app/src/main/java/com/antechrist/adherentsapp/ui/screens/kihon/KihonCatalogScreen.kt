//package com.antechrist.adherentsapp.ui.screens.kihon
//
//import android.util.Log
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Download
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.antechrist.adherentsapp.domain.model.TechniqueRef
//import com.antechrist.adherentsapp.ui.components.kihon.TechniqueEditorDialog
//import com.antechrist.adherentsapp.ui.components.MiniFilterChip
//import com.antechrist.adherentsapp.ui.components.kihon.TechniqueTemplatePickerSheet
//import com.antechrist.adherentsapp.ui.theme.extraColors
//import com.google.firebase.auth.FirebaseAuth
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun KihonCatalogScreen(
//    onBack: () -> Unit,
//    vm: KihonCatalogViewModel = hiltViewModel()
//) {
//    val ui by vm.ui.collectAsState()
//    var showEditor by remember { mutableStateOf<TechniqueRef?>(null) }
//    var showConfirmDelete by remember { mutableStateOf<TechniqueRef?>(null) }
//    var showTemplates by remember { mutableStateOf(false) }                      // ✅
//    val snackbar = remember { SnackbarHostState() }
//    val scope = rememberCoroutineScope()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Catalogue Kihon") },
//                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
//                actions = {
//                    IconButton(onClick = {
//                        val user = FirebaseAuth.getInstance().currentUser
//                        user?.getIdToken(false)?.addOnSuccessListener { result ->
//                            val claims = result.claims
//                            Log.d("KihonCatalog", "Firestore ID Token claims = $claims")
//                        }
//
//                        vm.seedNow { count ->
//                            scope.launch { snackbar.showSnackbar("Import réalisé ($count éléments)") }
//                        }
//                    }) { Icon(Icons.Default.Download, contentDescription = "Importer la liste club") }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = { showTemplates = true }) {         // ✅ FAB ouvre le picker
//                Icon(Icons.Default.Add, contentDescription = "Ajouter")
//            }
//        },
//        snackbarHost = { SnackbarHost(snackbar) }
//    ) { padding ->
//        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
//
//            // Filtres
//            // 1) Recherche sur toute la largeur
//            OutlinedTextField(
//                value = ui.query,
//                onValueChange = vm::setQuery,
//                placeholder = { Text("Rechercher (id/JA/FR/alias)") },
//                modifier = Modifier.fillMaxWidth()
//            )
//
//// 2) Chips qui WRAP correctement
//            FlowRow(
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp),
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                MiniFilterChip(label = "Tous",   selected = ui.kindFilter == null,                       onClick = { vm.setKindFilter(null) },                          tint = MaterialTheme.extraColors.grisDojo)
//                MiniFilterChip(label = "Pos",    selected = ui.kindFilter == TechniqueRef.Kind.POSITION, onClick = { vm.setKindFilter(TechniqueRef.Kind.POSITION) },    tint = MaterialTheme.extraColors.certifiedBlue)
//                MiniFilterChip(label = "Def",    selected = ui.kindFilter == TechniqueRef.Kind.DEFENSE,  onClick = { vm.setKindFilter(TechniqueRef.Kind.DEFENSE) },     tint = MaterialTheme.extraColors.rougeTatamie)
//                MiniFilterChip(label = "Poings", selected = ui.kindFilter == TechniqueRef.Kind.PUNCH,    onClick = { vm.setKindFilter(TechniqueRef.Kind.PUNCH) },       tint = MaterialTheme.extraColors.dorureDojo)
//                MiniFilterChip(label = "Pieds",  selected = ui.kindFilter == TechniqueRef.Kind.KICK,     onClick = { vm.setKindFilter(TechniqueRef.Kind.KICK) },        tint = MaterialTheme.extraColors.greenTatamie)
//            }
//
//            Spacer(Modifier.height(8.dp))
//
//            if (ui.loading) {
//                LinearProgressIndicator(Modifier.fillMaxWidth()); return@Column
//            }
//
//            ui.error?.let {
//                Text(it, color = MaterialTheme.colorScheme.error)
//                Spacer(Modifier.height(8.dp))
//            }
//
//            if (ui.items.isEmpty()) {
//                Text("Catalogue vide. Utilise l’import (télécharger) ou le bouton +.", style = MaterialTheme.typography.bodyMedium)
//            }
//
//            LazyColumn(
//                contentPadding = PaddingValues(bottom = 96.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp),
//                modifier = Modifier.weight(1f)
//            ) {
//                items(ui.items, key = { it.id }) { ref ->
//                    ElevatedCard(onClick = { showEditor = ref }) {
//                        Column(Modifier.padding(12.dp)) {
//                            Row(
//                                Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.SpaceBetween,
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Text("${ref.nameFr}  •  ${ref.nameJa}")
//                                Row {
//                                    IconButton(onClick = { showEditor = ref }) {
//                                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
//                                    }
//                                    IconButton(onClick = { showConfirmDelete = ref }) {
//                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer")
//                                    }
//                                }
//                            }
//                            val sub = buildString {
//                                append(ref.kind.name.lowercase().replaceFirstChar { it.titlecase() })
//                                ref.subType?.let { append(" • $it") }
//                                if (ref.aliases.isNotEmpty()) append(" • alias: ${ref.aliases.joinToString()}")
//                            }
//                            Text(sub, style = MaterialTheme.typography.bodySmall)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    // ✅ Picker de modèles (positions/défenses/poings/pieds)
//    TechniqueTemplatePickerSheet(
//        show = showTemplates,
//        templates = ui.missingTemplates,
//        onAddSelected = { chosen ->
//            scope.launch {
//                vm.saveMany(chosen)
//                    .onSuccess { added ->
//                        showTemplates = false
//                        snackbar.showSnackbar("Ajouté: $added élément(s)")
//                    }
//                    .onFailure { e -> snackbar.showSnackbar(e.message ?: "Échec ajout") }
//            }
//        },
//        onCreateManual = {
//            showTemplates = false
//            // ouvre le formulaire vierge (création manuelle)
//            showEditor = TechniqueRef(
//                id = "",
//                kind = TechniqueRef.Kind.POSITION,
//                nameJa = "",
//                nameFr = "",
//                aliases = emptyList(),
//                subType = null,
//                notes = null
//            )
//        },
//        onDismiss = { showTemplates = false }
//    )
//
//    // Dialog édition
//    showEditor?.let { init ->
//        TechniqueEditorDialog(
//            initial = init.takeIf { it.id.isNotEmpty() },
//            onDismiss = { showEditor = null },
//            onConfirm = { edited ->
//                scope.launch {
//                    vm.save(edited)
//                        .onSuccess {
//                            showEditor = null
//                            snackbar.showSnackbar("Technique enregistrée")
//                        }
//                        .onFailure { e -> snackbar.showSnackbar(e.message ?: "Échec enregistrement") }
//                }
//            }
//        )
//    }
//
//    // Confirm delete
//    showConfirmDelete?.let { ref ->
//        AlertDialog(
//            onDismissRequest = { showConfirmDelete = null },
//            confirmButton = {
//                TextButton(onClick = {
//                    scope.launch {
//                        vm.delete(ref.id)
//                            .onSuccess {
//                                showConfirmDelete = null
//                                snackbar.showSnackbar("Supprimé")
//                            }
//                            .onFailure { e -> snackbar.showSnackbar(e.message ?: "Échec suppression") }
//                    }
//                }) { Text("Supprimer") }
//            },
//            dismissButton = { TextButton(onClick = { showConfirmDelete = null }) { Text("Annuler") } },
//            title = { Text("Supprimer ${ref.nameFr} ?") },
//            text = { Text("Assure-toi qu’aucune séquence ne l’utilise.") }
//        )
//    }
//}

package com.antechrist.adherentsapp.ui.screens.kihon

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.domain.model.MovementCategory
import com.antechrist.adherentsapp.domain.model.OptionGroup
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.ui.components.kihon.TechniqueEditorDialog
import com.antechrist.adherentsapp.ui.components.MiniFilterChip
import com.antechrist.adherentsapp.ui.components.kihon.TechniqueTemplatePickerSheet
import com.antechrist.adherentsapp.ui.theme.extraColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KihonCatalogScreen(
    onBack: () -> Unit,
    vm: KihonCatalogViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var showEditor by remember { mutableStateOf<TechniqueRef?>(null) }
    var showConfirmDelete by remember { mutableStateOf<TechniqueRef?>(null) }
    var showTemplates by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catalogue Kihon") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().currentUser
                            ?.getIdToken(false)
                            ?.addOnSuccessListener { Log.d("KihonCatalog", "Claims=${it.claims}") }

                        vm.seedNow { count ->
                            scope.launch { snackbar.showSnackbar("Import réalisé ($count éléments)") }
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Importer")
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB seulement pour TECHNIQUES (templates)
            if (ui.family == CatalogFamily.TECHNIQUES) {
                FloatingActionButton(onClick = { showTemplates = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },

        // ─────────── Bottom bar pour les 4 familles ───────────
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = ui.family == CatalogFamily.TECHNIQUES,
                    onClick = { vm.setFamily(CatalogFamily.TECHNIQUES) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Techniques") },
                    label = { Text("Techniques") }
                )
                NavigationBarItem(
                    selected = ui.family == CatalogFamily.MOUVEMENTS,
                    onClick = { vm.setFamily(CatalogFamily.MOUVEMENTS) },
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Mouvements") },
                    label = { Text("Mouvements") }
                )
                NavigationBarItem(
                    selected = ui.family == CatalogFamily.OPTIONS,
                    onClick = { vm.setFamily(CatalogFamily.OPTIONS) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Options") },
                    label = { Text("Options") }
                )
                NavigationBarItem(
                    selected = ui.family == CatalogFamily.NIVEAUX,
                    onClick = { vm.setFamily(CatalogFamily.NIVEAUX) },
                    icon = { Icon(Icons.Default.School, contentDescription = "Niveaux") },
                    label = { Text("Niveaux") }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Recherche (commune)
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Rechercher (id/JA/FR/alias)") },
                modifier = Modifier.fillMaxWidth()
            )

            // === Chips secondaires selon la famille ===
            Spacer(Modifier.height(8.dp))
            when (ui.family) {
                CatalogFamily.TECHNIQUES -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MiniFilterChip(
                            label = "Tous",
                            selected = ui.kindFilter == null,
                            onClick = { vm.setKindFilter(null) },
                            tint = MaterialTheme.extraColors.grisDojo
                        )
                        MiniFilterChip(
                            label = "Pos",
                            selected = ui.kindFilter == TechniqueRef.Kind.POSITION,
                            onClick = { vm.setKindFilter(TechniqueRef.Kind.POSITION) },
                            tint = MaterialTheme.extraColors.certifiedBlue
                        )
                        MiniFilterChip(
                            label = "Def",
                            selected = ui.kindFilter == TechniqueRef.Kind.DEFENSE,
                            onClick = { vm.setKindFilter(TechniqueRef.Kind.DEFENSE) },
                            tint = MaterialTheme.extraColors.rougeTatamie
                        )
                        MiniFilterChip(
                            label = "Poings",
                            selected = ui.kindFilter == TechniqueRef.Kind.PUNCH,
                            onClick = { vm.setKindFilter(TechniqueRef.Kind.PUNCH) },
                            tint = MaterialTheme.extraColors.dorureDojo
                        )
                        MiniFilterChip(
                            label = "Pieds",
                            selected = ui.kindFilter == TechniqueRef.Kind.KICK,
                            onClick = { vm.setKindFilter(TechniqueRef.Kind.KICK) },
                            tint = MaterialTheme.extraColors.greenTatamie
                        )
                    }
                }

                CatalogFamily.MOUVEMENTS -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MiniFilterChip(
                            label = "Tous",
                            selected = ui.movementCategory == null,
                            onClick = { vm.setMovementCategory(null) },
                            tint = MaterialTheme.extraColors.grisDojo
                        )
                        MiniFilterChip(
                            label = "Static",
                            selected = ui.movementCategory == MovementCategory.STATIC,
                            onClick = { vm.setMovementCategory(MovementCategory.STATIC) },
                            tint = MaterialTheme.extraColors.certifiedBlue
                        )
                         MiniFilterChip(
                            label = "Step",
                            selected = ui.movementCategory == MovementCategory.STEP,
                            onClick = { vm.setMovementCategory(MovementCategory.STEP) },
                            tint = MaterialTheme.extraColors.rougeTatamie
                        )
                        MiniFilterChip(
                            label = "Slide",
                            selected = ui.movementCategory == MovementCategory.SLIDE,
                            onClick = { vm.setMovementCategory(MovementCategory.SLIDE) },
                            tint = MaterialTheme.extraColors.dorureDojo
                        )
                        MiniFilterChip(
                            label = "Pivot",
                            selected = ui.movementCategory == MovementCategory.PIVOT,
                            onClick = { vm.setMovementCategory(MovementCategory.PIVOT) },
                            tint = MaterialTheme.extraColors.greenTatamie
                        )
                    }
                }

                CatalogFamily.OPTIONS -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        @Composable
                        fun chip(label: String, g: OptionGroup?) =
                            MiniFilterChip(
                                label = label,
                                selected = ui.optionGroup == g,
                                onClick = { vm.setOptionGroup(g) },
                                tint = MaterialTheme.extraColors.dorureDojo
                            )
                        MiniFilterChip(
                            label = "Tous",
                            selected = ui.optionGroup == null,
                            onClick = { vm.setOptionGroup(null) },
                            tint = MaterialTheme.extraColors.grisDojo
                        )
                        MiniFilterChip(
                            label = "Side",
                            selected = ui.optionGroup == OptionGroup.SIDE,
                            onClick = { vm.setOptionGroup(OptionGroup.SIDE) },
                            tint = MaterialTheme.extraColors.certifiedBlue
                        )
                        MiniFilterChip(
                            label = "Target",
                            selected = ui.optionGroup == OptionGroup.REPOSE,
                            onClick = { vm.setOptionGroup(OptionGroup.REPOSE) },
                            tint = MaterialTheme.extraColors.rougeTatamie
                        )
                        MiniFilterChip(
                            label = "Distance",
                            selected = ui.optionGroup == OptionGroup.RELATION,
                            onClick = { vm.setOptionGroup(OptionGroup.RELATION) },
                            tint = MaterialTheme.extraColors.dorureDojo
                        )
                        MiniFilterChip(
                            label = "Direction",
                            selected = ui.optionGroup == OptionGroup.DIRECTION,
                            onClick = { vm.setOptionGroup(OptionGroup.DIRECTION) },
                            tint = MaterialTheme.extraColors.greenTatamie
                        )
                    }
                }

                CatalogFamily.NIVEAUX -> {
                    // pas de chips spécifiques ; la recherche suffit
                }
            }

            Spacer(Modifier.height(8.dp))

            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth()); return@Column
            }

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            // === Liste selon la famille ===
            when (ui.family) {
                CatalogFamily.TECHNIQUES -> {
                    if (ui.techniques.isEmpty()) {
                        Text(
                            "Aucune technique. Utilise l’import ou le bouton +.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(ui.techniques, key = { it.id }) { ref ->
                            TechniqueCard(
                                ref = ref,
                                onEdit = { showEditor = ref },
                                onDelete = { showConfirmDelete = ref }
                            )
                        }
                    }
                }

                CatalogFamily.MOUVEMENTS -> {
                    if (ui.movements.isEmpty()) {
                        Text("Aucun mouvement dans le catalogue.", style = MaterialTheme.typography.bodyMedium)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(ui.movements, key = { it.id }) { m ->
                            ElevatedCard {
                                Column(Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)) {
                                    Text("${m.nameFr}  •  ${m.nameJa}")
                                    Text(
                                        m.category.name.lowercase().replaceFirstChar { it.titlecase() } +
                                                (m.angleDeg?.let { " • ${it}°" } ?: "") +
                                                " • ${m.direction.name.lowercase().replaceFirstChar { it.titlecase() }}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                CatalogFamily.OPTIONS -> {
                    if (ui.options.isEmpty()) {
                        Text("Aucune option dans le catalogue.", style = MaterialTheme.typography.bodyMedium)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(ui.options, key = { it.id }) { o ->
                            ElevatedCard {
                                Column(Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)) {
                                    Text("${o.nameFr}  •  ${o.nameJa}")
                                    Text(
                                        "${o.group.name.lowercase().replaceFirstChar { it.titlecase() }} • value: ${o.value}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                CatalogFamily.NIVEAUX -> {
                    if (ui.levels.isEmpty()) {
                        Text("Aucun niveau dans le catalogue.", style = MaterialTheme.typography.bodyMedium)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(ui.levels, key = { it.id }) { lvl ->
                            ElevatedCard {
                                Column(Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)) {
                                    Text("${lvl.labelFr}  •  ${lvl.nameJa}")
                                    Text(
                                        "Ordre: ${lvl.order}" + (lvl.description?.let { " • $it" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ───── Templates + Édition + Delete : uniquement pour TECHNIQUES ─────

    if (ui.family == CatalogFamily.TECHNIQUES) {
        TechniqueTemplatePickerSheet(
            show = showTemplates,
            templates = ui.missingTemplates,
            onAddSelected = { chosen ->
                scope.launch {
                    vm.saveMany(chosen)
                        .onSuccess { added ->
                            showTemplates = false
                            snackbar.showSnackbar("Ajouté: $added élément(s)")
                        }
                        .onFailure { e -> snackbar.showSnackbar(e.message ?: "Échec ajout") }
                }
            },
            onCreateManual = {
                showTemplates = false
                showEditor = TechniqueRef(
                    id = "",
                    kind = TechniqueRef.Kind.POSITION,
                    nameJa = "",
                    nameFr = "",
                    aliases = emptyList(),
                    subType = null,
                    notes = null
                )
            },
            onDismiss = { showTemplates = false }
        )

        // Dialog édition technique
        showEditor?.let { init ->
            TechniqueEditorDialog(
                initial = init.takeIf { it.id.isNotEmpty() },
                onDismiss = { showEditor = null },
                onConfirm = { edited ->
                    scope.launch {
                        vm.save(edited)
                            .onSuccess {
                                showEditor = null
                                snackbar.showSnackbar("Technique enregistrée")
                            }
                            .onFailure { e -> snackbar.showSnackbar(e.message ?: "Échec enregistrement") }
                    }
                }
            )
        }

        // Confirm delete
        showConfirmDelete?.let { ref ->
            AlertDialog(
                onDismissRequest = { showConfirmDelete = null },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            vm.delete(ref.id)
                                .onSuccess {
                                    showConfirmDelete = null
                                    snackbar.showSnackbar("Supprimé")
                                }
                                .onFailure { e -> snackbar.showSnackbar(e.message ?: "Échec suppression") }
                        }
                    }) { Text("Supprimer") }
                },
                dismissButton = { TextButton(onClick = { showConfirmDelete = null }) { Text("Annuler") } },
                title = { Text("Supprimer ${ref.nameFr} ?") },
                text = { Text("Assure-toi qu’aucune séquence ne l’utilise.") }
            )
        }
    }
}

@Composable
private fun TechniqueCard(
    ref: TechniqueRef,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(onClick = onEdit) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${ref.nameFr}  •  ${ref.nameJa}")
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                    }
                }
            }
            val sub = buildString {
                append(ref.kind.name.lowercase().replaceFirstChar { it.titlecase() })
                ref.subType?.let { append(" • $it") }
                if (ref.aliases.isNotEmpty()) append(" • alias: ${ref.aliases.joinToString()}")
            }
            Text(sub, style = MaterialTheme.typography.bodySmall)
        }
    }
}
