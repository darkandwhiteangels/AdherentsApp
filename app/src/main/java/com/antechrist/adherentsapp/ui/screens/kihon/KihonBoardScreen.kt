package com.antechrist.adherentsapp.ui.screens.kihon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.LineStyle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.ui.navigation.KihonDestinations
import com.antechrist.adherentsapp.domain.usecase.GetKihonBoardStats
import com.antechrist.adherentsapp.ui.theme.extraColors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/* ============================== ViewModel ============================== */

@HiltViewModel
class KihonBoardViewModel @Inject constructor(
    private val getStats: GetKihonBoardStats
) : androidx.lifecycle.ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val byStatus: Map<KihonSequence.Status, Long> = emptyMap(),
        val byGrade: Map<String, Long> = emptyMap(),
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    suspend fun load(gradeKeys: List<String>?) {
        _ui.update { it.copy(loading = true, error = null) }
        val res = getStats(gradeKeys)
        _ui.update {
            res.fold(
                onSuccess = { s -> UiState(loading = false, byStatus = s.byStatus, byGrade = s.byGrade) },
                onFailure = { e -> it.copy(loading = false, error = e.message ?: "Erreur inconnue") }
            )
        }
    }
}

/* ============================== Screen ============================== */

/**
 * Écran Board Kihon :
 * - Compteurs par statut (Draft/Ready/Published)
 * - Compteurs par grade + navigation vers la liste du grade
 *
 * @param allGradeKeys liste des clés de grade (provenant du module ceintures, dans l’ordre)
 * @param onNavigate route -> Unit (utilise KihonDestinations.*)
 * @param onBack action retour (NavController::navigateUp)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KihonBoardScreen(
    allGradeKeys: List<String>,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,                       // ⟵ ajouté pour la flèche retour
    vm: KihonBoardViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(allGradeKeys) {
        vm.load(gradeKeys = allGradeKeys)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Kihon — Board") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = {
                IconButton(onClick = { onNavigate(KihonDestinations.CATALOG) }) {
                    Icon(
                        Icons.Outlined.AccountTree,
                        contentDescription = "Catalogue",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        // Statuts (3 cartes égales)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BandedStatCard(
                icon = { Icon(Icons.Filled.HourglassBottom, contentDescription = null) },
                label = "Brouillons",
                value = ui.byStatus[KihonSequence.Status.DRAFT] ?: 0L,
                // rougeTatamie
                stripeA = MaterialTheme.extraColors.rougeTatamie,
                stripeB = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 96.dp)
            )
            BandedStatCard(
                icon = { Icon(Icons.Filled.Assessment, contentDescription = null) },
                label = "Prêts",
                value = ui.byStatus[KihonSequence.Status.READY] ?: 0L,
                // certifiedBlue
                stripeA = MaterialTheme.extraColors.certifiedBlue,
                stripeB = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.90f),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 96.dp)
            )
            BandedStatCard(
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                label = "Publiés",
                value = ui.byStatus[KihonSequence.Status.PUBLISHED] ?: 0L,
                // dorureDojo
                stripeA = MaterialTheme.extraColors.dorureDojo,
                stripeB = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.90f),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 96.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text("Par grade", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allGradeKeys, key = { it }) { grade ->
                val count = ui.byGrade[grade] ?: 0L
                val color = beltColorFor(grade)
                BandedGradeTile(
                    gradeKey = grade,
                    count = count,
                    beltColor = color, 
                    onOpenList = { onNavigate(KihonDestinations.gradeList(grade)) }
                )
            }
        }
    }
}

/* ============================== UI bits ============================== */

@Composable
private fun BandedStatCard(
    icon: @Composable () -> Unit,
    label: String,
    value: Long,
    stripeA: androidx.compose.ui.graphics.Color,
    stripeB: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 96.dp)
        ) {
            // Bande verticale gauche (2 couleurs)
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(stripeA, stripeB)),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                // LIGNE 1 : Icône | Count (2 colonnes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Colonne 1 : Icône
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                stripeA.copy(alpha = 0.12f),
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }

                    Spacer(Modifier.weight(1f))

                    // Colonne 2 : Count (aligné à droite)
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // LIGNE 2 : Label (sous le count, aligné à droite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun BandedGradeTile(
    gradeKey: String,
    count: Long,
    beltColor: Color,
    onOpenList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val beltLabel = beltLabelFor(gradeKey)

    OutlinedCard(
        onClick = onOpenList,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
        ) {
            // --- Bande verticale gauche TEINTÉE PAR LA CEINTURE ---
            val stripeA = beltColor
            val stripeB = beltColor.copy(alpha = 0.65f)

            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    // ✅ Clip AVANT background pour forcer le rendu et éviter le masquage
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(
                        Brush.verticalGradient(listOf(stripeA, stripeB))
                    )
            )

            // ✅ Utiliser weight(1f) (pas fillMaxWidth) pour éviter d'écraser la bande
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // LIGNE 1 : icône (gauche) | "Grade : Couleur" (droite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenList,
                        modifier = Modifier.size(44.dp) // touch-target
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LineStyle,
                            contentDescription = "Ouvrir la liste",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(beltColor, shape = MaterialTheme.shapes.small)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = " $beltLabel ",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // LIGNE 2 : "Séquences : Count"
                Text(
                    text = "Séquences : $count",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}


/**
 * Tente de déduire la couleur ceinture depuis la clé de grade.
 * Si tes clés sont différentes, adapte les `contains(...)` ci-dessous
 * ou remplace par une vraie map (gradeKey -> color) si tu en as une centralisée.
 */
@Composable
private fun beltColorFor(gradeKey: String): Color {
    val k = gradeKey.trim().lowercase()

    // 1) Dan -> Noir direct
    if ("dan" in k) return Color(0xFF222222)

    // 2) Tenter d'extraire un numéro (kyu)
    val num = Regex("""\d+""").find(k)?.value?.toIntOrNull()

    if ("kyu" in k || k.startsWith("k_") || k.startsWith("k-") || k.startsWith("k")) {
        return when (num) {
            9 -> Color(0xFFF7F7F7) // WHITE
            8 -> Color(0xFFD2B21B) // YELLOW
            7 -> Color(0xFFF58214) // ORANGE
            6 -> Color(0xFF23A24C) // GREEN
            5 -> Color(0xFF0A6AD0) // BLUE
            4, 3, 2, 1 -> Color(0xFF70462B) // BROWN
            else -> Color(0xFFF7F7F7) // défaut kyu inconnu → blanc
        }
    }

    // 3) Sinon, essayer les alias couleur FR/EN
    return when {
        listOf("white","blanc").any { it in k }   -> Color(0xFFF7F7F7)
        listOf("yellow","jaune").any { it in k }  -> Color(0xFFD2B21B)
        "orange" in k                              -> Color(0xFFF58214)
        listOf("green","vert").any { it in k }    -> Color(0xFF23A24C)
        listOf("blue","bleu").any { it in k }     -> Color(0xFF0A6AD0)
        listOf("brown","marron").any { it in k }  -> Color(0xFF70462B)
        listOf("black","noir").any { it in k }    -> Color(0xFF222222)
        else                                       -> MaterialTheme.colorScheme.primary // dernier recours
    }
}


/**
 * Libellé lisible de la ceinture selon la clé.
 */
private fun beltLabelFor(gradeKey: String): String {
    val k = gradeKey.trim().lowercase()
    val num = Regex("""\d+""").find(k)?.value?.toIntOrNull()

    if ("dan" in k) return "Noire"
    if ("kyu" in k || k.startsWith("k")) {
        return when (num) {
            9 -> "Blanche"
            8 -> "Jaune"
            7 -> "Orange"
            6 -> "Verte"
            5 -> "Bleue"
            4, 3, 2, 1 -> "Marron"
            else -> "Blanche"
        }
    }
    return when {
        listOf("white","blanc").any { it in k }   -> "Blanche"
        listOf("yellow","jaune").any { it in k }  -> "Jaune"
        "orange" in k                              -> "Orange"
        listOf("green","vert").any { it in k }    -> "Verte"
        listOf("blue","bleu").any { it in k }     -> "Bleue"
        listOf("brown","marron").any { it in k }  -> "Marron"
        listOf("black","noir").any { it in k }    -> "Noire"
        else                                       -> gradeKey
    }
}
