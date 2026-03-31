package net.nepuview.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.nepuview.data.Film
import net.nepuview.data.FilmCategory
import net.nepuview.data.WatchlistFilm
import net.nepuview.repository.FilmRepository
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val heroFilm: Film,
        val categories: List<FilmCategory>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: FilmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Ordered category list matching scraper genre params
    private val categoryDefs = listOf(
        "Neu & Beliebt" to null,
        "Action"        to "Action",
        "Anime"         to "Anime",
        "Serien"        to "Serie",
        "Komödie"       to "Komödie",
        "Thriller"      to "Thriller"
    )

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val categories = coroutineScope {
                    categoryDefs.map { (label, genre) ->
                        async {
                            val films = repo.loadHome(genre).firstOrNull() ?: emptyList()
                            FilmCategory(label, films)
                        }
                    }.map { it.await() }
                        .filter { it.films.isNotEmpty() }
                }
                val hero = categories.firstOrNull()?.films?.firstOrNull()
                if (hero != null && categories.isNotEmpty()) {
                    _uiState.value = HomeUiState.Success(hero, categories)
                } else {
                    _uiState.value = HomeUiState.Error("Keine Inhalte gefunden")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }

    fun addToWatchlist(film: Film) {
        viewModelScope.launch {
            repo.addToWatchlist(
                WatchlistFilm(
                    filmId = film.id,
                    filmTitle = film.title,
                    posterUrl = film.posterUrl,
                    detailUrl = film.detailUrl
                )
            )
        }
    }
}
