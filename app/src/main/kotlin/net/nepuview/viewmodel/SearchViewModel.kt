package net.nepuview.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.nepuview.data.Film
import net.nepuview.repository.FilmRepository
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: FilmRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Film>>(emptyList())
    val results: StateFlow<List<Film>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun search(q: String) {
        if (q.isBlank()) return
        _query.value = q
        _isLoading.value = true
        saveHistory(q)
        viewModelScope.launch {
            repo.search(q)
                .catch { _isLoading.value = false }
                .collect { films ->
                    _results.value = films
                    _isLoading.value = false
                }
        }
    }

    fun clearQuery() {
        _query.value = ""
        _results.value = emptyList()
    }

    fun removeHistory(term: String) {
        val list = _history.value.toMutableList()
        list.remove(term)
        _history.value = list
        persistHistory(list)
    }

    private fun saveHistory(term: String) {
        val list = _history.value.toMutableList()
        list.remove(term)
        list.add(0, term)
        val trimmed = list.take(10)
        _history.value = trimmed
        persistHistory(trimmed)
    }

    private fun loadHistory(): List<String> {
        val raw = prefs.getString("terms", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("|").filter { it.isNotBlank() }
    }

    private fun persistHistory(list: List<String>) {
        prefs.edit().putString("terms", list.joinToString("|")).apply()
    }
}
