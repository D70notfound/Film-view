package net.nepuview.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.nepuview.data.DownloadedFilm
import net.nepuview.repository.DownloadRepository
import net.nepuview.repository.FilmRepository
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadRepo: DownloadRepository,
    private val filmRepo: FilmRepository
) : ViewModel() {

    val downloads: StateFlow<List<DownloadedFilm>> = filmRepo.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSize: StateFlow<Long> = filmRepo.totalDownloadSize()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val isLowStorage: StateFlow<Boolean> = MutableStateFlow(downloadRepo.isLowStorage())

    fun startDownload(filmId: String, filmTitle: String, posterUrl: String, m3u8Url: String) {
        downloadRepo.startDownload(filmId, m3u8Url)
        viewModelScope.launch {
            filmRepo.addDownloadRecord(
                DownloadedFilm(
                    filmId = filmId,
                    filmTitle = filmTitle,
                    posterUrl = posterUrl,
                    m3u8Url = m3u8Url
                )
            )
        }
    }

    fun removeDownload(filmId: String) {
        downloadRepo.removeDownload(filmId)
        viewModelScope.launch { filmRepo.removeDownloadRecord(filmId) }
    }

    fun clearAll() {
        viewModelScope.launch {
            downloads.value.forEach { downloadRepo.removeDownload(it.filmId) }
            filmRepo.clearAllDownloads()
        }
    }

    fun freeStorageFormatted(): String {
        val bytes = downloadRepo.freeStorageBytes()
        return when {
            bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
            else -> "%.0f MB".format(bytes / 1_048_576.0)
        }
    }
}
