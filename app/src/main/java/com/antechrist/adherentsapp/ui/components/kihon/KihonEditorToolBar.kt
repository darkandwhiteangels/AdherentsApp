//package com.antechrist.adherentsapp.ui.components.kihon
//
//import android.util.Log
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.spring
//import androidx.compose.animation.expandVertically
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.shrinkVertically
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.DirectionsRun
//import androidx.compose.material.icons.filled.Construction
//import androidx.compose.material.icons.filled.Handyman
//import androidx.compose.material.icons.filled.SportsMma
//import androidx.compose.material.icons.filled.VerticalAlignCenter
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.painter.Painter
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import com.antechrist.adherentsapp.domain.model.DraftToken
//import com.antechrist.adherentsapp.domain.model.kihon.OptionKind
//import com.antechrist.adherentsapp.domain.model.StepDraft
//import com.antechrist.adherentsapp.domain.model.TechniqueRef
//import com.antechrist.adherentsapp.domain.model.kihon.*
//import com.antechrist.adherentsapp.ui.theme.extraColors
//
//private enum class KihonToolTab { Deplacement, Positions, Techniques, Niveaux, Options }
//private enum class TechCat { DEFENSE, MAIN, PIED, ATTAQUE }
//
//private val TOOL_PANEL_HEIGHT = 260.dp
//private const val TAG_TOOL = "KihonEditorToolBar"
//
//@Composable
//fun KihonEditorToolBar(
//    draft: StepDraft,
//    modifier: Modifier = Modifier,
//    positions: List<TechniqueRef>,
//    techniquesPied: List<TechniqueRef>,
//    techniquesPoing: List<TechniqueRef>,
//    techniquesDefense: List<TechniqueRef>,
//    niveaux: List<Height> = listOf(Height.GEDAN, Height.CHUDAN, Height.JODAN),
//    onPickMovement: (Movement) -> Unit,
//    onPickPosition: (TechniqueRef) -> Unit,
//    onPickTechnique: (TechniqueRef) -> Unit,
//    onPickHeight: (Height) -> Unit,
//    onPickOptions: (ExecutingLimbRole, FootLanding) -> Unit,
//    iconDeplacement: Painter? = null,
//    iconPositions: Painter? = null,
//    iconTechniques: Painter? = null,
//    iconNiveaux: Painter? = null,
//    iconOptions: Painter? = null,
//    initiallyExpanded: Boolean = true,
//    onOpenBO: () -> Unit = {},
//    expanded: Boolean? = null,
//    onExpandedChange: ((Boolean) -> Unit)? = null,
//    onPickExecutingLimb: (ExecutingLimbRole) -> Unit,
//    onPickFootLanding: (FootLanding) -> Unit,
//    onToggleSameArm: () -> Unit,
//    onToggleSameLeg: () -> Unit,
//) {
//    // --- Instrumentation : la BO "voit-elle" les changements du draft ? ---
//    LaunchedEffect(draft.tokens.size, draft.selectedIndex) {
//        Log.d(TAG_TOOL, "BO sees draft size=${draft.tokens.size} selected=${draft.selectedIndex}")
//    }
//
//    // Mode contrôlé si expanded + onExpandedChange fournis, sinon état interne
//    val isControlled = expanded != null && onExpandedChange != null
//    var localExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
//    var activeTab by rememberSaveable { mutableStateOf<KihonToolTab?>(null) }
//
//    val curExpanded = if (isControlled) expanded else localExpanded
//
//    fun setExpanded(value: Boolean) {
//        if (curExpanded == value) return
//        if (isControlled) onExpandedChange.invoke(value) else localExpanded = value
//        if (value) onOpenBO()
//        Log.d(TAG_TOOL, "setExpanded=$value activeTab=$activeTab")
//    }
//
//    Surface(
//        tonalElevation = 8.dp,
//        shadowElevation = 4.dp,
//        color = MaterialTheme.colorScheme.surface,
//        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
//        modifier = modifier
//            .fillMaxWidth()
//            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
//    ) {
//        Column {
//            // Handle pour ouvrir/fermer
//            Box(
//                Modifier
//                    .align(Alignment.CenterHorizontally)
//                    .padding(top = 6.dp)
//                    .clip(RoundedCornerShape(12.dp))
//                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
//                    .size(width = 42.dp, height = 6.dp)
//                    .clickable { setExpanded(!curExpanded) }
//            )
//
//            // Onglets
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 6.dp),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                ToolIcon("Dépl.", iconDeplacement, Icons.AutoMirrored.Filled.DirectionsRun, activeTab == KihonToolTab.Deplacement && curExpanded) {
//                    setExpanded(true)
//                    activeTab = if (activeTab == KihonToolTab.Deplacement) null else KihonToolTab.Deplacement
//                    Log.d(TAG_TOOL, "TAB -> Deplacement, expanded=$curExpanded")
//                }
//                ToolIcon("Positions", iconPositions, Icons.Filled.VerticalAlignCenter, activeTab == KihonToolTab.Positions && curExpanded) {
//                    setExpanded(true)
//                    activeTab = if (activeTab == KihonToolTab.Positions) null else KihonToolTab.Positions
//                    Log.d(TAG_TOOL, "TAB -> Positions, expanded=$curExpanded")
//                }
//                ToolIcon("Techniques", iconTechniques, Icons.Filled.Handyman, activeTab == KihonToolTab.Techniques && curExpanded) {
//                    setExpanded(true)
//                    activeTab = if (activeTab == KihonToolTab.Techniques) null else KihonToolTab.Techniques
//                    Log.d(TAG_TOOL, "TAB -> Techniques, expanded=$curExpanded")
//                }
//                ToolIcon("Niveaux", iconNiveaux, Icons.Filled.SportsMma, activeTab == KihonToolTab.Niveaux && curExpanded) {
//                    setExpanded(true)
//                    activeTab = if (activeTab == KihonToolTab.Niveaux) null else KihonToolTab.Niveaux
//                    Log.d(TAG_TOOL, "TAB -> Niveaux, expanded=$curExpanded")
//                }
//                ToolIcon("Options", iconOptions, Icons.Filled.Construction, activeTab == KihonToolTab.Options && curExpanded) {
//                    setExpanded(true)
//                    activeTab = if (activeTab == KihonToolTab.Options) null else KihonToolTab.Options
//                    Log.d(TAG_TOOL, "TAB -> Options, expanded=$curExpanded")
//                }
//            }
//
//            AnimatedVisibility(
//                visible = curExpanded && activeTab != null,
//                enter = fadeIn() + expandVertically(animationSpec = spring()),
//                exit  = fadeOut() + shrinkVertically(animationSpec = spring())
//            ) {
//                Box(
//                    Modifier
//                        .fillMaxWidth()
//                        .height(TOOL_PANEL_HEIGHT)
//                ) {
//                    when (activeTab) {
//                        KihonToolTab.Deplacement -> {
//                            PanelDeplacement {
//                                onPickMovement(it)
//                            }
//                        }
//                        KihonToolTab.Positions -> {
//                            PanelByImages(
//                                items = positions,
//                                onPick = {
//                                    onPickPosition(it)
//                                }
//                            ) { it.nameJa }
//                        }
//                        KihonToolTab.Techniques -> {
//                            PanelTechniquesImages(
//                                itemsPoing = techniquesPoing,
//                                itemsPied = techniquesPied,
//                                itemsDefense = techniquesDefense,
//                                onPick = {
//                                    onPickTechnique(it)
//                                }
//                            )
//                        }
//                        KihonToolTab.Niveaux -> {
//                            PanelNiveaux(niveaux) {
//                                onPickHeight(it)
//                            }
//                        }
//                        KihonToolTab.Options -> {
//                            PanelOptions(
//                                draft = draft,
//                                onPickExecutingLimb = onPickExecutingLimb,
//                                onPickFootLanding = onPickFootLanding,
//                                onToggleSameArm = onToggleSameArm,
//                                onToggleSameLeg = onToggleSameLeg
//                            )
//                            Spacer(Modifier.height(8.dp))
//                        }
//                        null -> { /* no-op */ }
//                    }
//                }
//            }
//        }
//    }
//}
//
///* ---------- sous-composants ---------- */
//
//@Composable
//private fun ToolIcon(
//    label: String,
//    painter: Painter?,
//    fallback: ImageVector,
//    selected: Boolean,
//    onClick: () -> Unit
//) {
//    val tint = if (selected) MaterialTheme.extraColors.certifiedBlue else MaterialTheme.colorScheme.onSurfaceVariant
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier
//            .clip(RoundedCornerShape(12.dp))
//            .clickable { onClick() }
//            .padding(4.dp)
//    ) {
//        if (painter != null) {
//            Icon(
//                painter = painter,
//                contentDescription = label,
//                tint = tint,
//                modifier = Modifier.size(24.dp)
//            )
//        } else {
//            Icon(fallback, label, tint = tint)
//        }
//        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
//    }
//}
//
//@Composable
//private fun PanelDeplacement(onPick: (Movement) -> Unit) {
//    val items = listOf(
//        Movement.FORWARD, Movement.BACKWARD,
//        Movement.CHASSE_FORWARD, Movement.TIRES_FORWARD,
//        Movement.CHASSE_BACK, Movement.TIRES_BACK,
//        Movement.ON_PLACE
//    )
//    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(12.dp)) {
//        items(items.size) { idx ->
//            val m = items[idx]
//            SquareTile(
//                label = movementLabel(m), // FR
//                selected = false,
//                onClick = { onPick(m) },
//                modifier = Modifier.padding(4.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun PanelByImages(
//    items: List<TechniqueRef>,
//    onPick: (TechniqueRef) -> Unit,
//    label: (TechniqueRef) -> String = { it.nameJa }
//) {
//    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(12.dp)) {
//        items(items.size) { idx ->
//            val t = items[idx]
//            SquareTile(
//                label = label(t),   // Positions/Techniques en JA
//                selected = false,
//                onClick = { onPick(t) },
//                modifier = Modifier.padding(4.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun PanelTechniquesImages(
//    itemsPoing: List<TechniqueRef>,
//    itemsPied: List<TechniqueRef>,
//    itemsDefense: List<TechniqueRef>,
//    onPick: (TechniqueRef) -> Unit
//) {
//    var cat by remember { mutableStateOf(TechCat.MAIN) }
//
//    Column {
//        // 4 segments: Défense / Main / Pied / Attaque
//        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//            SegmentedButton(cat == TechCat.DEFENSE, { cat = TechCat.DEFENSE }, "Défense")
//            Spacer(Modifier.width(8.dp))
//            SegmentedButton(cat == TechCat.MAIN, { cat = TechCat.MAIN }, "Main")
//            Spacer(Modifier.width(8.dp))
//            SegmentedButton(cat == TechCat.PIED, { cat = TechCat.PIED }, "Pied")
//            Spacer(Modifier.width(8.dp))
//            SegmentedButton(cat == TechCat.ATTAQUE, { cat = TechCat.ATTAQUE }, "Attaque")
//        }
//
//        val list = when (cat) {
//            TechCat.DEFENSE -> itemsDefense
//            TechCat.MAIN    -> itemsPoing
//            TechCat.PIED    -> itemsPied
//            TechCat.ATTAQUE -> itemsPoing + itemsPied
//        }
//
//        // Affichage en JA
//        PanelByImages(list, onPick) { it.nameJa }
//    }
//}
//
//@Composable
//private fun PanelNiveaux(niveaux: List<Height>, onPick: (Height) -> Unit) {
//    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(12.dp)) {
//        items(niveaux.size) { idx ->
//            val h = niveaux[idx]
//            SquareTile(
//                label = heightLabel(h),
//                selected = false,
//                onClick = { onPick(h) },
//                modifier = Modifier.padding(4.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun PanelOptions(
//    draft: StepDraft,
//    onPickExecutingLimb: (ExecutingLimbRole) -> Unit,
//    onPickFootLanding: (FootLanding) -> Unit,
//    onToggleSameArm: () -> Unit,
//    onToggleSameLeg: () -> Unit
//) {
//    // Valeurs courantes lues depuis le draft
//    val currentExec: ExecutingLimbRole = draft.tokens.asReversed()
//        .firstOrNull { it is DraftToken.OptionToken && it.option == OptionKind.EXECUTING_LIMB }
//        ?.let { (it as DraftToken.OptionToken).value as? ExecutingLimbRole }
//        ?: ExecutingLimbRole.NONE
//
//    val currentLanding: FootLanding = draft.tokens.asReversed()
//        .firstOrNull { it is DraftToken.OptionToken && it.option == OptionKind.FOOT_LANDING }
//        ?.let { (it as DraftToken.OptionToken).value as? FootLanding }
//        ?: FootLanding.NONE
//
//    val sameArmActive = draft.tokens.any { it is DraftToken.OptionToken && it.option == OptionKind.SAME_ARM }
//    val sameLegActive = draft.tokens.any { it is DraftToken.OptionToken && it.option == OptionKind.SAME_LEG }
//
//    Column(Modifier.fillMaxWidth()) {
//        // Bloc 1 : Bras / Jambe (avant / arrière)
//        Text(
//            text = "Options choix multiples",
//            style = MaterialTheme.typography.labelSmall,
//            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 6.dp)
//        )
//        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(horizontal = 12.dp)) {
//            item{
//                SquareTile(
//                    label = "Opposé",
//                    selected = currentExec == ExecutingLimbRole.BOTH,
//                    onClick = { onPickExecutingLimb(ExecutingLimbRole.BOTH)},
//                    modifier = Modifier.padding(4.dp)
//                )
//            }
//            item {
//                SquareTile(
//                    label = "Bras avant",
//                    selected = currentExec == ExecutingLimbRole.BRAS_AVANT,
//                    onClick = { onPickExecutingLimb(ExecutingLimbRole.BRAS_AVANT) },
//                    modifier = Modifier.padding(4.dp)
//                )
//            }
//            item {
//                SquareTile(
//                    label = "Bras arrière",
//                    selected = currentExec == ExecutingLimbRole.BRAS_ARRIERE,
//                    onClick = { onPickExecutingLimb(ExecutingLimbRole.BRAS_ARRIERE) },
//                    modifier = Modifier.padding(4.dp)
//                )
//            }
//            item {
//                SquareTile(
//                    label = "Jambe avant",
//                    selected = currentExec == ExecutingLimbRole.JAMBE_AVANT,
//                    onClick = { onPickExecutingLimb(ExecutingLimbRole.JAMBE_AVANT) },
//                    modifier = Modifier.padding(4.dp)
//                )
//            }
//            item {
//                SquareTile(
//                    label = "Jambe arrière",
//                    selected = currentExec == ExecutingLimbRole.JAMBE_ARRIERE,
//                    onClick = { onPickExecutingLimb(ExecutingLimbRole.JAMBE_ARRIERE) },
//                    modifier = Modifier.padding(4.dp)
//                )
//            }
//            item {
//                SquareTile(
//                    label = "Repose devant",
//                    selected = currentLanding == FootLanding.DEVANT,
//                    onClick = { onPickFootLanding(FootLanding.DEVANT) },
//                    modifier = Modifier.padding(4.dp)
//                )
//            }
//            item {
//                SquareTile(
//                    label = "Repose derrière",
//                    selected = currentLanding == FootLanding.DERRIERE,
//                    onClick = { onPickFootLanding(FootLanding.DERRIERE) },
//                    modifier = Modifier.padding(4.dp)
//                )
//            }
//            item{
//                SquareTile(
//                    label = "Même Bras",
//                    selected = sameArmActive,
//                    onClick = {onToggleSameArm()},
//                    modifier = Modifier.padding(4.dp)
//                )
//            }
//            item{
//                SquareTile(
//                    label = "Même jambe",
//                    selected = sameLegActive,
//                    onClick = {onToggleSameLeg()},
//                    modifier = Modifier.padding(4.dp)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun SegmentedButton(selected: Boolean, onClick: () -> Unit, label: String) {
//    val bg = if (selected) MaterialTheme.extraColors.certifiedBlue.copy(alpha = 0.65f)
//    else MaterialTheme.extraColors.rougeTatamie.copy(alpha = 0.45f)
//    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
//    Box(
//        Modifier
//            .clip(RoundedCornerShape(10.dp))
//            .background(bg)
//            .clickable { onClick() }
//            .padding(horizontal = 12.dp, vertical = 8.dp)
//    ) { Text(label, color = fg) }
//}
//
//@Composable
//private fun SquareTile(
//    modifier: Modifier = Modifier,
//    label: String,
//    selected: Boolean = false,
//    onClick: () -> Unit,
//    background: Color = MaterialTheme.colorScheme.surface
//) {
//    val bg = if (selected) background.copy(alpha = 0.75f) else background
//    Box(
//        modifier
//            .aspectRatio(1f)
//            .clip(RoundedCornerShape(10.dp))
//            .background(bg)
//            .clickable(onClick = onClick)
//            .padding(8.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = label,
//            textAlign = TextAlign.Center,
//            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
//        )
//    }
//}
package com.antechrist.adherentsapp.ui.components.kihon

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.DraftToken
import com.antechrist.adherentsapp.domain.model.kihon.OptionKind
import com.antechrist.adherentsapp.domain.model.StepDraft
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.model.kihon.*
import com.antechrist.adherentsapp.ui.theme.extraColors

private enum class KihonToolTab { Deplacement, Positions, Techniques, Niveaux, Options }

private const val TAG_TOOL = "KihonEditorTool"
private val TOOL_PANEL_HEIGHT = 240.dp

@Composable
fun KihonEditorToolBar(
    draft: StepDraft,
    modifier: Modifier = Modifier,
    positions: List<TechniqueRef>,
    techniquesPied: List<TechniqueRef>,
    techniquesPoing: List<TechniqueRef>,
    techniquesDefense: List<TechniqueRef>,
    niveaux: List<Height> = listOf(Height.GEDAN, Height.CHUDAN, Height.JODAN),
    onPickMovement: (Movement) -> Unit,
    onPickPosition: (TechniqueRef) -> Unit,
    onPickTechnique: (TechniqueRef) -> Unit,
    onPickHeight: (Height) -> Unit,
    onPickOptions: (ExecutingLimbRole, FootLanding) -> Unit,
    iconDeplacement: Painter? = null,
    iconPositions: Painter? = null,
    iconTechniques: Painter? = null,
    iconNiveaux: Painter? = null,
    iconOptions: Painter? = null,
    initiallyExpanded: Boolean = false,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onPickExecutingLimb: (ExecutingLimbRole) -> Unit,
    onPickFootLanding: (FootLanding) -> Unit,
    onToggleSameArm: () -> Unit,
    onToggleSameLeg: () -> Unit,

    // V2 token-based optional callbacks (catalog-driven). If provided, toolbar will push IDs into the new editor.
    onAddPositionId: ((String) -> Unit)? = null,
    onAddTechniqueId: ((String) -> Unit)? = null,
    onAddMovementId: ((String) -> Unit)? = null,
    onAddOptionId: ((String) -> Unit)? = null,
    onAddLevelId: ((String) -> Unit)? = null,
) {
    // --- Instrumentation : la BO "voit-elle" les changements du draft ? ---
    LaunchedEffect(draft.tokens.size, draft.selectedIndex) {
        Log.d(TAG_TOOL, "BO sees draft size=${draft.tokens.size} selected=${draft.selectedIndex}")
    }

    // Mode contrôlé si expanded + onExpandedChange fournis, sinon état interne
    val isControlled = expanded != null && onExpandedChange != null
    var localExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    var activeTab by rememberSaveable { mutableStateOf<KihonToolTab?>(null) }

    val curExpanded = if (isControlled) expanded else localExpanded

    fun setExpanded(value: Boolean) {
        if (curExpanded == value) return
        if (isControlled) onExpandedChange.invoke(value) else localExpanded = value
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Bandeau d’icônes
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolIcon(
                    selected = activeTab == KihonToolTab.Deplacement,
                    onClick = { setExpanded(true); activeTab = KihonToolTab.Deplacement },
                    painter = iconDeplacement,
                    fallback = Icons.AutoMirrored.Filled.DirectionsWalk,
                    label = "Dépl."
                )
                ToolIcon(
                    selected = activeTab == KihonToolTab.Positions,
                    onClick = { setExpanded(true); activeTab = KihonToolTab.Positions },
                    painter = iconPositions,
                    fallback = Icons.Default.SelfImprovement,
                    label = "Positions"
                )
                ToolIcon(
                    selected = activeTab == KihonToolTab.Techniques,
                    onClick = { setExpanded(true); activeTab = KihonToolTab.Techniques },
                    painter = iconTechniques,
                    fallback = Icons.Default.FitnessCenter,
                    label = "Techniques"
                )
                ToolIcon(
                    selected = activeTab == KihonToolTab.Niveaux,
                    onClick = { setExpanded(true); activeTab = KihonToolTab.Niveaux },
                    painter = iconNiveaux,
                    fallback = Icons.Default.Stairs,
                    label = "Niveaux"
                )
                ToolIcon(
                    selected = activeTab == KihonToolTab.Options,
                    onClick = { setExpanded(true); activeTab = KihonToolTab.Options },
                    painter = iconOptions,
                    fallback = Icons.Default.Tune,
                    label = "Options"
                )
            }

            AnimatedVisibility(
                visible = curExpanded && activeTab != null,
                enter = fadeIn() + expandVertically(animationSpec = spring()),
                exit  = fadeOut() + shrinkVertically(animationSpec = spring())
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(TOOL_PANEL_HEIGHT)
                ) {
                    when (activeTab) {
                        KihonToolTab.Deplacement -> {
                            PanelDeplacement {
                                onPickMovement(it)
                                onAddMovementId?.invoke(mapMovementToCatalogId(it))
                            }
                        }
                        KihonToolTab.Positions -> {
                            PanelByImages(
                                items = positions,
                                onPick = {
                                    onPickPosition(it)
                                    onAddPositionId?.invoke(it.id)
                                }
                            ) { it.nameJa }
                        }
                        KihonToolTab.Techniques -> {
                            PanelTechniquesImages(
                                itemsPoing = techniquesPoing,
                                itemsPied = techniquesPied,
                                itemsDefense = techniquesDefense,
                                onPick = {
                                    onPickTechnique(it)
                                    onAddTechniqueId?.invoke(it.id)
                                }
                            )
                        }
                        KihonToolTab.Niveaux -> {
                            PanelNiveaux(niveaux) {
                                onPickHeight(it)
                                onAddLevelId?.invoke(mapHeightToLevelId(it))
                            }
                        }
                        KihonToolTab.Options -> {
                            Column(Modifier.fillMaxSize()) {
                                PanelOptions(
                                    draft = draft,
                                    onPickExecutingLimb = {
                                        onPickExecutingLimb(it)
                                        // (Optionnel) Mapping EXECUTING_LIMB -> catalogue non défini dans OptionSeed : on laisse legacy-only
                                    },
                                    onPickFootLanding = {
                                        onPickFootLanding(it)
                                        // Foot landing -> DIRECTION (avant/arrière) si tu veux tracer dans la séquence V2
                                        when (it) {
                                            FootLanding.DEVANT -> onAddOptionId?.invoke("OPT.DIRECTION.FORWARD")
                                            FootLanding.DERRIERE -> onAddOptionId?.invoke("OPT.DIRECTION.BACKWARD")
                                            else -> {}
                                        }
                                    },
                                    onToggleSameArm = {
                                        onToggleSameArm()
                                        onAddOptionId?.invoke("OPT.RELATION.SAME_ARM")
                                    },
                                    onToggleSameLeg = {
                                        onToggleSameLeg()
                                        onAddOptionId?.invoke("OPT.RELATION.SAME_LEG")
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun PanelDeplacement(onPick: (Movement) -> Unit) {
    val items = listOf(
        Movement.FORWARD, Movement.BACKWARD,
        Movement.CHASSE_FORWARD, Movement.TIRES_FORWARD,
        Movement.CHASSE_BACK, Movement.TIRES_BACK,
        Movement.ON_PLACE
    )
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(12.dp)) {
        items(items.size) { idx ->
            val m = items[idx]
            SquareTile(
                label = movementLabel(m), // FR (fonction existante)
                selected = false,
                onClick = { onPick(m) },
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
private fun PanelByImages(
    items: List<TechniqueRef>,
    onPick: (TechniqueRef) -> Unit,
    label: (TechniqueRef) -> String = { it.nameJa }
) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(12.dp)) {
        items(items.size) { idx ->
            val t = items[idx]
            SquareTile(
                label = label(t),
                selected = false,
                onClick = { onPick(t) },
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

private enum class TechCat { DEFENSE, MAIN, PIED, ATTAQUE }

@Composable
private fun PanelTechniquesImages(
    itemsPoing: List<TechniqueRef>,
    itemsPied: List<TechniqueRef>,
    itemsDefense: List<TechniqueRef>,
    onPick: (TechniqueRef) -> Unit
) {
    var cat by remember { mutableStateOf(TechCat.MAIN) }

    Column {
        // 4 segments: Défense / Main / Pied / Attaque
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SegmentedButton(cat == TechCat.DEFENSE, { cat = TechCat.DEFENSE }, "Défense")
            Spacer(Modifier.width(8.dp))
            SegmentedButton(cat == TechCat.MAIN, { cat = TechCat.MAIN }, "Main")
            Spacer(Modifier.width(8.dp))
            SegmentedButton(cat == TechCat.PIED, { cat = TechCat.PIED }, "Pied")
            Spacer(Modifier.width(8.dp))
            SegmentedButton(cat == TechCat.ATTAQUE, { cat = TechCat.ATTAQUE }, "Attaque")
        }
        Spacer(Modifier.height(8.dp))

        val items = when (cat) {
            TechCat.DEFENSE -> itemsDefense
            TechCat.MAIN -> itemsPoing
            TechCat.PIED -> itemsPied
            TechCat.ATTAQUE -> itemsPoing // placeholder si tu as une catégorie dédiée
        }
        PanelByImages(items, onPick = onPick) { it.nameJa }
    }
}

@Composable
private fun PanelNiveaux(
    niveaux: List<Height>,
    onPick: (Height) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        niveaux.forEach { level ->
            AssistChip(
                onClick = { onPick(level) },
                label = { Text(level.name.lowercase().replaceFirstChar { it.titlecase() }) }
            )
        }
    }
}

@Composable
private fun PanelOptions(
    draft: StepDraft,
    onPickExecutingLimb: (ExecutingLimbRole) -> Unit,
    onPickFootLanding: (FootLanding) -> Unit,
    onToggleSameArm: () -> Unit,
    onToggleSameLeg: () -> Unit
) {
    // Valeurs courantes lues depuis le draft
    val currentExec: ExecutingLimbRole = draft.tokens.asReversed()
        .firstOrNull { it is DraftToken.OptionToken && it.option == OptionKind.EXECUTING_LIMB }
        ?.let { (it as DraftToken.OptionToken).value as? ExecutingLimbRole }
        ?: ExecutingLimbRole.NONE

    val currentLanding: FootLanding = draft.tokens.asReversed()
        .firstOrNull { it is DraftToken.OptionToken && it.option == OptionKind.FOOT_LANDING }
        ?.let { (it as DraftToken.OptionToken).value as? FootLanding }
        ?: FootLanding.NONE

    val sameArmActive = draft.tokens.any { it is DraftToken.OptionToken && it.option == OptionKind.SAME_ARM }
    val sameLegActive = draft.tokens.any { it is DraftToken.OptionToken && it.option == OptionKind.SAME_LEG }

    Column(Modifier.fillMaxWidth()) {
        // Bloc 1 : Bras / Jambe (avant / arrière)
        Text(
            text = "Options choix multiples",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 6.dp)
        )
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(horizontal = 12.dp)) {
            item{
                SquareTile(
                    label = "Bras avant",
                    selected = currentExec == ExecutingLimbRole.BRAS_AVANT,
                    onClick = { onPickExecutingLimb(ExecutingLimbRole.BRAS_AVANT) },
                    modifier = Modifier.padding(4.dp)
                )
            }
            item {
                SquareTile(
                    label = "Bras arrière",
                    selected = currentExec == ExecutingLimbRole.BRAS_ARRIERE,
                    onClick = { onPickExecutingLimb(ExecutingLimbRole.BRAS_ARRIERE) },
                    modifier = Modifier.padding(4.dp)
                )
            }
            item {
                SquareTile(
                    label = "Jambe avant",
                    selected = currentExec == ExecutingLimbRole.JAMBE_AVANT,
                    onClick = { onPickExecutingLimb(ExecutingLimbRole.JAMBE_AVANT) },
                    modifier = Modifier.padding(4.dp)
                )
            }
            item {
                SquareTile(
                    label = "Jambe arrière",
                    selected = currentExec == ExecutingLimbRole.JAMBE_ARRIERE,
                    onClick = { onPickExecutingLimb(ExecutingLimbRole.JAMBE_ARRIERE) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Atterrissage du pied",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 6.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SquareTile(
                label = "Avant",
                selected = currentLanding == FootLanding.DEVANT,
                onClick = { onPickFootLanding(FootLanding.DEVANT) },
                modifier = Modifier.weight(1f)
            )
            SquareTile(
                label = "Arrière",
                selected = currentLanding == FootLanding.DERRIERE,
                onClick = { onPickFootLanding(FootLanding.DERRIERE) },
                modifier = Modifier.weight(1f)
            )
            SquareTile(
                label = "Aucun",
                selected = currentLanding == FootLanding.NONE,
                onClick = { onPickFootLanding(FootLanding.NONE) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Toggles",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 6.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SquareTile(
                label = "Même bras",
                selected = sameArmActive,
                onClick = onToggleSameArm,
                modifier = Modifier.weight(1f)
            )
            SquareTile(
                label = "Même jambe",
                selected = sameLegActive,
                onClick = onToggleSameLeg,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToolIcon(
    selected: Boolean,
    onClick: () -> Unit,
    painter: Painter?,
    fallback: ImageVector,
    label: String
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    val border = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = (painter ?: Icons.Default.Build) as Painter, // fallback icon if no painter provided
            contentDescription = label
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SegmentedButton(selected: Boolean, onClick: () -> Unit, text: String) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    val border = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
    )
}

@Composable
private fun SquareTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

// ---------- Helpers de mapping vers IDs catalogue (V2) ----------

private fun mapMovementToCatalogId(m: Movement): String = when (m) {
    Movement.FORWARD -> "MOV.AYUMI_ASHI_FORWARD"
    Movement.BACKWARD -> "MOV.AYUMI_ASHI_BACKWARD"
    Movement.CHASSE_FORWARD -> "MOV.YORI_ASHI_FORWARD"
    Movement.CHASSE_BACK -> "MOV.YORI_ASHI_BACKWARD"
    Movement.TIRES_FORWARD -> "MOV.TSUGI_ASHI_FORWARD"
    Movement.TIRES_BACK -> "MOV.TSUGI_ASHI_BACKWARD"
    Movement.ON_PLACE -> "MOV.STATIC"
    Movement.NONE -> TODO()
    Movement.LATERAL -> TODO()
    Movement.PIVOT_90_IN -> TODO()
    Movement.PIVOT_90_OUT -> TODO()
    Movement.PIVOT_180 -> TODO()
}

private fun mapHeightToLevelId(h: Height): String = when (h) {
    Height.JODAN -> "LVL.JODAN"
    Height.CHUDAN -> "LVL.CHUDAN"
    Height.GEDAN -> "LVL.GEDAN"
}
