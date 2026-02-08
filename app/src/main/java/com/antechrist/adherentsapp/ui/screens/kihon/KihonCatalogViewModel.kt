//package com.antechrist.adherentsapp.ui.screens.kihon
//
//import androidx.lifecycle.viewModelScope
//import com.antechrist.adherentsapp.domain.model.TechniqueRef
//import com.antechrist.adherentsapp.domain.usecase.DeleteTechnique
//import com.antechrist.adherentsapp.domain.usecase.GetKihonCatalog
//import com.antechrist.adherentsapp.domain.usecase.SeedKihonCatalogIfEmpty
//import com.antechrist.adherentsapp.domain.usecase.UpsertTechnique
//import com.antechrist.adherentsapp.domain.usecase.UpsertTechniques
//import dagger.hilt.android.lifecycle.HiltViewModel
//import javax.inject.Inject
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//
//@HiltViewModel
//class KihonCatalogViewModel @Inject constructor(
//    getCatalog: GetKihonCatalog,
//    private val upsert: UpsertTechnique,
//    private val upsertMany: UpsertTechniques,                                 // ✅ nouveau
//    private val deleteTech: DeleteTechnique,
//    private val seed: SeedKihonCatalogIfEmpty,
//    private val seedProvider: SeedKihonCatalogIfEmpty.KihonSeedProvider       // ✅ accès aux modèles
//) : androidx.lifecycle.ViewModel() {
//
//    data class UiState(
//        val items: List<TechniqueRef> = emptyList(),
//        val missingTemplates: List<TechniqueRef> = emptyList(),               // ✅
//        val query: String = "",
//        val kindFilter: TechniqueRef.Kind? = null,
//        val loading: Boolean = true,
//        val error: String? = null
//    )
//
//    private val _ui = MutableStateFlow(UiState())
//    val ui: StateFlow<UiState> = _ui
//
//    init {
//        viewModelScope.launch {
//            getCatalog()
//                .combine(_ui.map { it.query to it.kindFilter }) { map, (q, k) ->
//                    val base = map.values.toList()
//                    val filtered = base.filter { ref ->
//                        (k == null || ref.kind == k) &&
//                                (q.isBlank() ||
//                                        ref.nameFr.contains(q, true) ||
//                                        ref.nameJa.contains(q, true) ||
//                                        ref.id.contains(q, true) ||
//                                        ref.aliases.any { it.contains(q, true) })
//                    }.sortedWith(compareBy<TechniqueRef> { it.kind.ordinal }
//                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nameFr })
//
//                    // calcule les modèles “manquants” par rapport au seed
//                    val currentIds = map.keys
//                    val missing = seedProvider.getSeed().filter { it.id !in currentIds }
//
//                    filtered to missing
//                }
//                .collect { (list, missing) ->
//                    _ui.update { it.copy(items = list, missingTemplates = missing, loading = false, error = null) }
//                }
//        }
//    }
//
//    fun setQuery(q: String) { _ui.update { it.copy(query = q) } }
//    fun setKindFilter(k: TechniqueRef.Kind?) { _ui.update { it.copy(kindFilter = k) } }
//
//    fun seedNow(onDone: (Int) -> Unit) {
//        viewModelScope.launch {
//            seed(force = false).onSuccess { onDone(it) }.onFailure { e ->
//                _ui.update { it.copy(error = e.message ?: "Échec import") }
//            }
//        }
//    }
//
//    suspend fun save(ref: TechniqueRef): Result<String> = upsert(ref)
//
//    suspend fun saveMany(list: List<TechniqueRef>): Result<Int> = upsertMany(list)    // ✅
//
//    suspend fun delete(id: String): Result<Unit> = deleteTech(id)
//}

package com.antechrist.adherentsapp.ui.screens.kihon

import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.LevelRef
import com.antechrist.adherentsapp.domain.model.MovementCategory
import com.antechrist.adherentsapp.domain.model.MovementRef
import com.antechrist.adherentsapp.domain.model.OptionGroup
import com.antechrist.adherentsapp.domain.model.OptionRef
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import com.antechrist.adherentsapp.domain.usecase.DeleteTechnique
import com.antechrist.adherentsapp.domain.usecase.GetKihonCatalog
import com.antechrist.adherentsapp.domain.usecase.SeedKihonCatalogIfEmpty
import com.antechrist.adherentsapp.domain.usecase.UpsertTechnique
import com.antechrist.adherentsapp.domain.usecase.UpsertTechniques
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CatalogFamily { TECHNIQUES, MOUVEMENTS, OPTIONS, NIVEAUX }

@HiltViewModel
class KihonCatalogViewModel @Inject constructor(
    // Techniques via use-case existant (garde ta logique d’avant)
    getCatalog: GetKihonCatalog,
    private val upsert: UpsertTechnique,
    private val upsertMany: UpsertTechniques,
    private val deleteTech: DeleteTechnique,
    private val seed: SeedKihonCatalogIfEmpty,
    private val seedProvider: SeedKihonCatalogIfEmpty.KihonSeedProvider,
    // Le reste directement via le repo
    private val repo: KihonRepository
) : androidx.lifecycle.ViewModel() {

    data class UiState(
        val family: CatalogFamily = CatalogFamily.TECHNIQUES,

        // Recherche commune
        val query: String = "",

        // Filtres spécifiques par famille
        val kindFilter: TechniqueRef.Kind? = null,                 // TECHNIQUES
        val movementCategory: MovementCategory? = null,            // MOUVEMENTS
        val optionGroup: OptionGroup? = null,                      // OPTIONS

        // Données par famille (après filtrage + tri)
        val techniques: List<TechniqueRef> = emptyList(),
        val movements: List<MovementRef> = emptyList(),
        val options: List<OptionRef> = emptyList(),
        val levels: List<LevelRef> = emptyList(),

        // Seulement pour Techniques : modèles seed manquants
        val missingTemplates: List<TechniqueRef> = emptyList(),

        // State
        val loading: Boolean = true,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            // flux techniques (use-case) + filtres communs
            val techniquesFlow = getCatalog().combine(_ui.map { it.query to it.kindFilter }) { map, (q, k) ->
                val base = map.values.toList()
                val filtered = base.filter { ref ->
                    (k == null || ref.kind == k) &&
                            (q.isBlank() ||
                                    ref.nameFr.contains(q, true) ||
                                    ref.nameJa.contains(q, true) ||
                                    ref.id.contains(q, true) ||
                                    ref.aliases.any { it.contains(q, true) })
                }.sortedWith(compareBy<TechniqueRef> { it.kind.ordinal }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nameFr })
                val currentIds = map.keys
                val missing = seedProvider.getSeed().filter { it.id !in currentIds }
                filtered to missing
            }

            // flux mouvements
            val movementsFlow = repo.streamMovements()
                .combine(_ui.map { it.query to it.movementCategory }) { map, (q, cat) ->
                    map.values.filter { m ->
                        (cat == null || m.category == cat) &&
                                (q.isBlank() ||
                                        m.nameFr.contains(q, true) ||
                                        m.nameJa.contains(q, true) ||
                                        m.id.contains(q, true))
                    }.sortedWith(compareBy<String> { it }.let { cmp ->
                        compareBy<MovementRef> { it.category.ordinal }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nameFr }
                    })
                }

            // flux options
            val optionsFlow = repo.streamOptions()
                .combine(_ui.map { it.query to it.optionGroup }) { map, (q, grp) ->
                    map.values.filter { o ->
                        (grp == null || o.group == grp) &&
                                (q.isBlank() ||
                                        o.nameFr.contains(q, true) ||
                                        o.nameJa.contains(q, true) ||
                                        o.id.contains(q, true))
                    }.sortedWith(compareBy<OptionRef> { it.group.ordinal }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nameFr })
                }

            // flux levels
            val levelsFlow = repo.streamLevels()
                .map { map ->
                    map.values.sortedWith(compareBy<LevelRef> { it.order }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.labelFr })
                }
                .combine(_ui.map { it.query }) { list, q ->
                    list.filter { lvl ->
                        q.isBlank() ||
                                lvl.labelFr.contains(q, true) ||
                                lvl.nameJa.contains(q, true) ||
                                lvl.id.contains(q, true)
                    }
                }

            combine(techniquesFlow, movementsFlow, optionsFlow, levelsFlow) { techPair, mov, opt, lvl ->
                val (techList, missing) = techPair
                Quad(techList, missing, mov, opt, lvl)
            }.collect { (techList, missing, mov, opt, lvl) ->
                _ui.update {
                    it.copy(
                        techniques = techList,
                        missingTemplates = missing,
                        movements = mov,
                        options = opt,
                        levels = lvl,
                        loading = false,
                        error = null
                    )
                }
            }
        }
    }

    // Helpers de MAJ d'état
    fun setQuery(q: String) { _ui.update { it.copy(query = q) } }
    fun setFamily(f: CatalogFamily) { _ui.update { it.copy(family = f) } }

    fun setKindFilter(k: TechniqueRef.Kind?) { _ui.update { it.copy(kindFilter = k) } }
    fun setMovementCategory(cat: MovementCategory?) { _ui.update { it.copy(movementCategory = cat) } }
    fun setOptionGroup(g: OptionGroup?) { _ui.update { it.copy(optionGroup = g) } }

    // Seed all (use-case élargi)
    fun seedNow(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            seed(force = false).onSuccess { onDone(it) }.onFailure { e ->
                _ui.update { it.copy(error = e.message ?: "Échec import") }
            }
        }
    }

    // CRUD Techniques (seule famille éditable ici)
    suspend fun save(ref: TechniqueRef): Result<String> = upsert(ref)
    suspend fun saveMany(list: List<TechniqueRef>): Result<Int> = upsertMany(list)
    suspend fun delete(id: String): Result<Unit> = deleteTech(id)
}

/** Petit type utilitaire pour combine à 4 sources. */
private data class Quad<A,B,C,D,E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
