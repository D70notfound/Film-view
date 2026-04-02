package net.nepuview.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
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

    private val _userMessage = Channel<String>(Channel.BUFFERED)
    val userMessage: kotlinx.coroutines.flow.Flow<String> = _userMessage.receiveAsFlow()

    val isLowStorage: StateFlow<Boolean> = flow {
        while (true) {
            emit(downloadRepo.isLowStorage())
            delay(30_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), downloadRepo.isLowStorage())

    fun startDownload(filmId: String, filmTitle: String, posterUrl: String, m3u8Url: String, quality: String = "auto") {
        if (!downloadRepo.startDownload(filmId, m3u8Url)) {
            viewModelScope.launch {
                _userMessage.send("Kein WLAN verfügbar. Download wird nur über WLAN gestartet.")
            }
            return
        }
        viewModelScope.launch {
            filmRepo.addDownloadRecord(
                DownloadedFilm(
                    filmId = filmId,
                    filmTitle = filmTitle,
                    posterUrl = posterUrl,
                    m3u8Url = m3u8Url,
                    quality = quality
                )
            )
            // Monitor download size and update the record when bytes are downloaded
            updateDownloadSize(filmId)
        }
    }

    private suspend fun updateDownloadSize(filmId: String) {
        val dm = downloadRepo.getDownloadManager()
        repeat(120) { // check for up to 10 minutes
            delay(5_000)
            val dl = dm.downloadIndex.getDownload(filmId) ?: return
            if (dl.bytesDownloaded > 0) {
                filmRepo.addDownloadRecord(
                    downloads.value.find { it.filmId == filmId }?.copy(
                        fileSizeBytes = dl.bytesDownloaded
                    ) ?: return
                )
            }
            if (dl.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) return
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
