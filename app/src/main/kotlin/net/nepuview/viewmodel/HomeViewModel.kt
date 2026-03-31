package net.nepuview.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.nepuview.data.Film
import net.nepuview.repository.FilmRepository
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val films: List<Film>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: FilmRepository
) : ViewModel() {

    private val _selectedMood = MutableStateFlow<String?>(null)
    val selectedMood: StateFlow<String?> = _selectedMood.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFilms()
    }

    fun selectMood(mood: String?) {
        _selectedMood.value = mood
        loadFilms()
    }

    fun loadFilms() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            repo.loadHome(_selectedMood.value)
                .catch { e -> _uiState.value = HomeUiState.Error(e.message ?: "Fehler") }
                .collect { films -> _uiState.value = HomeUiState.Success(films) }
        }
    }
}
