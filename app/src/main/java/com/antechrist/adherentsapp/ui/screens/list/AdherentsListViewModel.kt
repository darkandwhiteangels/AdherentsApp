package com.antechrist.adherentsapp.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.core.utils.SeasonUtils
import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import com.antechrist.adherentsapp.domain.usecase.GetAdherentsStreamUseCase
import com.antechrist.adherentsapp.domain.usecase.SearchAdherentsUseCase
import com.antechrist.adherentsapp.domain.repository.PresenceStatsRepository // ✅ compteurs de présence
import com.antechrist.adherentsapp.ui.utils.firstLetterOrHash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Section(val letter: Char, val items: List<Adherent>)

data class ListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val query: String = "",
    val sections: List<Section> = emptyList(),
    // ✅ compteurs de présence (saison courante)
    val presentCounts: Map<String, Int> = emptyMap(),
    val seasonKey: String = SeasonUtils.currentSeasonKey()
)

@HiltViewModel
class AdherentsListViewModel @Inject constructor(
    getStream: GetAdherentsStreamUseCase,
    private val search: SearchAdherentsUseCase,
    // ✅ stream des compteurs de présence par saison
    private val statsRepo: PresenceStatsRepository,
    private val repo: AdherentsRepository,
    private val adherentsRepository: AdherentsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _source = MutableStateFlow<List<Adherent>>(emptyList())
    private val _state = MutableStateFlow(ListUiState())

    val state: StateFlow<ListUiState> = _state.asStateFlow()

    init {
        // Flux 1 : adhérents
        viewModelScope.launch {
            getStream()
                .onStart { _state.update { it.copy(loading = true, error = null) } }
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collect { list ->
                    // tri primaire par nom, puis prénom
                    _source.value = list.sortedWith(
                        compareBy<Adherent> { it.nom.lowercase() }.thenBy { it.prenom.lowercase() }
                    )
                    applyFilter()
                    _state.update { it.copy(loading = false) }
                }
        }

        // Flux 2 : recherche
        viewModelScope.launch {
            _query.collect { applyFilter() }
        }

        // Flux 3 : compteurs de présence de la saison courante
        viewModelScope.launch {
            val season = _state.value.seasonKey
            statsRepo.streamCountsForSeason(season)
                .catch { /* on garde vide en cas d'erreur */ }
                .collect { counts ->
                    _state.update { it.copy(presentCounts = counts) }
                }
        }
    }

    fun onQueryChange(q: String) { _query.value = q }

    private fun applyFilter() {
        val filtered = search(_source.value, _query.value)
        val grouped = filtered.groupBy { firstLetterOrHash(it.nom) }
        val letters = grouped.keys.sortedWith(compareBy<Char> { if (it == '#') 'Z' + 1 else it }) // '#' en dernier
        val sections = letters.map { letter ->
            val items = grouped[letter].orEmpty().sortedWith(
                compareBy<Adherent> { it.nom.lowercase() }.thenBy { it.prenom.lowercase() }
            )
            Section(letter, items)
        }
        _state.update { it.copy(sections = sections, query = _query.value) }
    }
    fun onArchiveAdherent(adherentId: String) {
        viewModelScope.launch {
            try {
                // Ici, vous appelez une fonction de votre repository
                // qui va mettre à jour le document dans Firestore.
                repo.archiveAdherent(adherentId)
                // La liste se mettra à jour automatiquement si votre flux `getAdherents()`
                // filtre les adhérents archivés.
            } catch (e: Exception) {
                // Gérer l'erreur, par exemple en affichant un message
                _state.update { it.copy(error = "Erreur lors de l'archivage : ${e.message}") }
            }
        }
    }

    fun onDeleteAdherent(id: String) {
        viewModelScope.launch {
            // Suppose que votre repository a une fonction deleteAdherent
            adherentsRepository.delete(id)
        }
    }
}
