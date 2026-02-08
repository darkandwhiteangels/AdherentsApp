package com.antechrist.adherentsapp.ui.screens.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.BottomNavigationDefaults.windowInsets
import androidx.compose.material.icons.Icons
import com.antechrist.adherentsapp.R
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.ui.components.AdherentListItem
import com.antechrist.adherentsapp.ui.components.AlphaBubbleOverlay
import com.antechrist.adherentsapp.ui.components.AlphaIndexBar
import com.antechrist.adherentsapp.ui.components.SectionHeader
import kotlinx.coroutines.launch
import com.antechrist.adherentsapp.ui.role.RoleViewModel
import com.antechrist.adherentsapp.domain.model.Role
import com.antechrist.adherentsapp.ui.components.SwipeableAdherentRow
import com.antechrist.adherentsapp.ui.theme.extraColors

private val NeutralPastel = Color(0xFFAFAFAF)
private val OrangePastel  = Color(0xFFFDBB69)
private val RedPastel     = Color(0xFFFF6868)
private val BrownPastel   = Color(0xFF6C2F00)
private val BluePastel    = Color(0xFF7FDCFF)

private val IndexBarGutterRight = 30.dp
private val FabDiameter = 56.dp
private val FabSpacing = 16.dp
private val PresenceBarWidth = 8.dp

private fun cardColorFor(presentCount: Int): Color = when {
    presentCount > 2   -> BrownPastel
    presentCount >= 2  -> RedPastel
    presentCount == 1  -> OrangePastel
    presentCount == 0  -> NeutralPastel
    else               -> NeutralPastel
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdherentsListScreen(
    onLogout: () -> Unit,
    onAdd: (() -> Unit)? = null,
    // MODIFIÉ : La fonction de navigation accepte maintenant l'ID cliqué et la liste de tous les IDs affichés.
    onOpenDetail: (adherentId: String, orderedIds: List<String>) -> Unit,
    onTakePresence: () -> Unit,
    onViewReports: () -> Unit,
    onCreateWhatsAppList: () -> Unit,
    isAdmin: Boolean,
    vm: AdherentsListViewModel = hiltViewModel(),
    onOpenKihon: (() -> Unit)? = null,
    onOpenCotisations: (() -> Unit)? = null,
    onOpenInfoMessages: () -> Unit,
    onOpenClubConfig: () -> Unit,
    roleVm: RoleViewModel = hiltViewModel()
) {
    val haptics = LocalHapticFeedback.current
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val totalAdherentsCount = remember(state.sections) {
        state.sections.sumOf { it.items.size }
    }

    val presentLetters by remember(state.sections) {
        mutableStateOf(state.sections.map { it.letter }.toSet())
    }

    val sectionStartIndex by remember(state.sections) {
        mutableStateOf(
            buildMap<Char, Int> {
                var idx = 0
                state.sections.forEach { sec ->
                    put(sec.letter, idx)
                    idx += 1 + sec.items.size
                }
            }
        )
    }

    // MODIFIÉ : On prépare la liste ordonnée des IDs pour la passer à l'écran de détail.
    // `remember` garantit que la liste n'est recalculée que si les sections changent.
    val orderedAdherentIds = remember(state.sections) {
        state.sections.flatMap { section -> section.items.map { item -> item.id } }
    }

    var bubbleChar by remember { mutableStateOf<Char?>(null) }
    var bubbleFrac by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(bubbleChar) {
        if (bubbleChar != null) {
            kotlinx.coroutines.delay(800)
            bubbleChar = null
            bubbleFrac = null
        }
    }
    val orderedLetters = remember { ('A'..'Z').toList() + '#' }
    val role by roleVm.role.collectAsState()
    val canOpenConfig = role == Role.SUPER_ADMIN || role == Role.ADMIN

    fun targetIndexFor(letter: Char): Int? {
        sectionStartIndex[letter]?.let { return it }
        val startPos = orderedLetters.indexOf(letter).coerceAtLeast(0)
        for (i in startPos + 1..orderedLetters.lastIndex) {
            sectionStartIndex[orderedLetters[i]]?.let { return it }
        }
        for (i in startPos - 1 downTo 0) {
            sectionStartIndex[orderedLetters[i]]?.let { return it }
        }
        return null
    }

    var menuExpanded by remember { mutableStateOf(false) }

    var openedAdherentId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.extraColors.certifiedBlue,  // Couleur de fond (ton rouge NavRed)
                    titleContentColor = Color.White,      // Couleur du titre
                    navigationIconContentColor = Color.White,  // Couleur de l'icône de navigation
                    actionIconContentColor = Color.White  // Couleur des icônes d'action
                ),
                title = { Text("$totalAdherentsCount Adhérents") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    if (isAdmin) {
                        onOpenKihon?.let { goKihon ->
                            IconButton(onClick = goKihon) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_kihon),
                                    contentDescription = "Kihon",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Menu",
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Notification Push") },
                            onClick = {
                                menuExpanded = false
                                onOpenInfoMessages()
                            }
                        )

                        if (isAdmin) {

                            DropdownMenuItem(
                                text = { Text("Consulter / Exporter") },
                                onClick = { menuExpanded = false; onViewReports() }
                            )
                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Listing parents") },
                                onClick = {
                                    menuExpanded = false;
                                    onCreateWhatsAppList()
                                }
                            )
                        }
//                        if (canOpenConfig) {
//                            DropdownMenuItem(
//                                text = { Text("Configuration du club") },
//                                onClick = { menuExpanded = false; onOpenClubConfig() }
//
//                            )
//                        }
                        DropdownMenuItem(
                            text = { Text("Déconnexion") },
                            onClick = { menuExpanded = false; onLogout() }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            onAdd?.let {
                FloatingActionButton(
                    onClick = it,
                    modifier = Modifier.padding(end = IndexBarGutterRight)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter")
                }
            }
        },
    ) { innerPaddings ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp)
                .padding(innerPaddings)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    label = { Text("Rechercher nom, prénom, email") },
                    singleLine = true,
                    trailingIcon = {
                        if (state.query.isNotBlank()) {
                            IconButton(onClick = { vm.onQueryChange("") }) {
                                Icon(Icons.Filled.HighlightOff, contentDescription = "Effacer la recherche")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))

                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.error != null -> Text("Erreur : ${state.error}")
                    state.sections.isEmpty() -> Text("Aucun adhérent")
                    else -> Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = IndexBarGutterRight),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            state.sections.forEach { section ->
                                stickyHeader { SectionHeader(letter = section.letter) }

                                items(
                                    items = section.items,
                                    key = { it.id }
                                ) { item ->
                                    //val isEligibleForArchive = (state.presentCounts[item.id] ?: 0) >= 2 && item.cotisationPaid != true

                                    val isEligibleForArchive = true

                                    // ON UTILISE VOTRE COMPOSANT ADAPTÉ !
                                    SwipeableAdherentRow(
                                        adherentId = item.id,
                                        openedAdherentId = openedAdherentId,
                                        onOpenChange = { openedAdherentId = it },
                                        enabled = isEligibleForArchive,
                                        onArchive = {
                                            // On referme le swipe avant d'archiver
                                            openedAdherentId = null
                                            // On déclenche l'archivage via le ViewModel
                                            vm.onArchiveAdherent(item.id)
                                        },
                                        onDelete = {
                                            openedAdherentId = null
                                            vm.onDeleteAdherent(item.id)
                                        }
                                        //modifier = Modifier.animateItemPlacement() // Garde l'animation de réorganisation
                                    ) {
                                        // Le contenu est votre Row, qui ne change pas.
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min)
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            val count = state.presentCounts[item.id] ?: 0
                                            val barColor = if (item.cotisationPaid == true) BluePastel else cardColorFor(count)
                                            Box(
                                                Modifier
                                                    .width(PresenceBarWidth)
                                                    .fillMaxHeight()
                                                    .background(barColor)
                                            )
                                            Box(Modifier.weight(1f)) {
                                                AdherentListItem(
                                                    item = item,
                                                    onClick = { onOpenDetail(item.id, orderedAdherentIds) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        AlphaIndexBar(
                            presentLetters = presentLetters,
                            onLetterChange = { c, frac ->
                                bubbleChar = c
                                bubbleFrac = frac
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                targetIndexFor(c)?.let { target ->
                                    scope.launch { listState.animateScrollToItem(target) }
                                }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .align(Alignment.TopEnd)
                                .padding(end = 2.dp)
                        )
                        AlphaBubbleOverlay(
                            current = bubbleChar,
                            fraction = bubbleFrac,
                            rightPadding = IndexBarGutterRight
                        )
                    }
                }
            }
        }
    }
}