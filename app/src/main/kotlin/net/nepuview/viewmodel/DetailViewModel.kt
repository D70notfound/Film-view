package net.nepuview.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.nepuview.data.FavoriteFilm
import net.nepuview.data.Film
import net.nepuview.data.WatchProgress
import net.nepuview.repository.FilmRepository
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: FilmRepository
) : ViewModel() {

    private val _film = MutableStateFlow<Film?>(null)
    val film: StateFlow<Film?> = _film.asStateFlow()

    private val _progress = MutableStateFlow<WatchProgress?>(null)
    val progress: StateFlow<WatchProgress?> = _progress.asStateFlow()

    /** Loads detail only once; subsequent calls are no-ops if data is already cached. */
    fun loadDetail(detailUrl: String) {
        if (_film.value != null) return
        viewModelScope.launch {
            repo.loadDetail(detailUrl).collect { film ->
                film ?: return@collect
                _film.value = film
                _progress.value = repo.getProgress(film.id)
            }
        }
    }

    fun isFavorite(filmId: String): Flow<Boolean> = repo.isFavorite(filmId)

    fun toggleFavorite(film: FavoriteFilm) {
        viewModelScope.launch { repo.toggleFavorite(film) }
    }
}
