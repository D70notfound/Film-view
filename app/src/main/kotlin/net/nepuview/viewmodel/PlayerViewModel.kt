package net.nepuview.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.nepuview.data.WatchHistory
import net.nepuview.data.WatchProgress
import net.nepuview.repository.FilmRepository
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repo: FilmRepository
) : ViewModel() {

    private val _m3u8Url = MutableStateFlow<String?>(null)
    val m3u8Url: StateFlow<String?> = _m3u8Url.asStateFlow()

    private val _currentFilmId = MutableStateFlow<String?>(null)
    private val _currentFilmTitle = MutableStateFlow<String>("")
    private val _currentPosterUrl = MutableStateFlow<String>("")
    private val _currentPlayerUrl = MutableStateFlow<String>("")

    fun onM3u8Found(url: String) {
        if (_m3u8Url.value == null) { // only capture the first one
            _m3u8Url.value = url
        }
    }

    fun setCurrentFilm(filmId: String, title: String, posterUrl: String, playerUrl: String) {
        _currentFilmId.value = filmId
        _currentFilmTitle.value = title
        _currentPosterUrl.value = posterUrl
        _currentPlayerUrl.value = playerUrl
        _m3u8Url.value = null
        loadSavedProgress(filmId)
    }

    private val _savedPositionMs = MutableStateFlow(0L)
    val savedPositionMs: StateFlow<Long> = _savedPositionMs.asStateFlow()

    private fun loadSavedProgress(filmId: String) {
        viewModelScope.launch {
            val progress = repo.getProgress(filmId)
            _savedPositionMs.value = progress?.positionMs ?: 0L
        }
    }

    /** Called every 5 seconds from PlayerFragment. */
    fun saveProgress(positionMs: Long, durationMs: Long) {
        val filmId = _currentFilmId.value ?: return
        viewModelScope.launch {
            repo.saveProgress(
                WatchProgress(
                    filmId = filmId,
                    filmTitle = _currentFilmTitle.value,
                    posterUrl = _currentPosterUrl.value,
                    playerUrl = _currentPlayerUrl.value,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            )
        }
    }

    fun addToHistory() {
        val filmId = _currentFilmId.value ?: return
        viewModelScope.launch {
            repo.addHistory(
                WatchHistory(
                    filmId = filmId,
                    filmTitle = _currentFilmTitle.value,
                    posterUrl = _currentPosterUrl.value,
                    playerUrl = _currentPlayerUrl.value
                )
            )
        }
    }

    fun clearM3u8() {
        _m3u8Url.value = null
    }
}
