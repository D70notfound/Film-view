package net.nepuview.repository

import kotlinx.coroutines.flow.Flow
import net.nepuview.data.*
import net.nepuview.scraper.ScraperEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilmRepository @Inject constructor(
    private val scraper: ScraperEngine,
    private val dao: FilmDao
) {
    fun loadHome(mood: String? = null): Flow<List<Film>> = scraper.loadHome(mood)

    fun search(query: String): Flow<List<Film>> = scraper.search(query)

    fun loadDetail(detailUrl: String): Flow<Film?> = scraper.loadDetail(detailUrl)

    // ── Watch Progress ──────────────────────────────────────
    suspend fun saveProgress(progress: WatchProgress) = dao.upsertProgress(progress)
    suspend fun getProgress(filmId: String): WatchProgress? = dao.getProgress(filmId)

    // ── History ─────────────────────────────────────────────
    fun observeHistory(): Flow<List<WatchHistory>> = dao.observeHistory()
    suspend fun addHistory(h: WatchHistory) = dao.insertHistory(h)
    suspend fun deleteHistoryItem(id: Long) = dao.deleteHistoryItem(id)
    suspend fun clearHistory() = dao.clearHistory()

    // ── Favorites ────────────────────────────────────────────
    fun observeFavorites(): Flow<List<FavoriteFilm>> = dao.observeFavorites()
    fun isFavorite(filmId: String): Flow<Boolean> = dao.isFavorite(filmId)
    suspend fun toggleFavorite(film: FavoriteFilm) {
        if (dao.isFavorite(film.filmId).equals(true)) dao.deleteFavorite(film)
        else dao.upsertFavorite(film)
    }
    suspend fun addFavorite(film: FavoriteFilm) = dao.upsertFavorite(film)
    suspend fun removeFavorite(filmId: String) = dao.deleteFavoriteById(filmId)

    // ── Watchlist ────────────────────────────────────────────
    fun observeWatchlist(): Flow<List<WatchlistFilm>> = dao.observeWatchlist()
    suspend fun addToWatchlist(film: WatchlistFilm) = dao.upsertWatchlist(film)
    suspend fun removeFromWatchlist(filmId: String) = dao.deleteWatchlistItem(filmId)

    // ── Downloads (metadata only) ─────────────────────────────
    fun observeDownloads(): Flow<List<DownloadedFilm>> = dao.observeDownloads()
    fun totalDownloadSize(): Flow<Long?> = dao.totalDownloadSize()
    fun isDownloaded(filmId: String): Flow<Boolean> = dao.isDownloaded(filmId)
    suspend fun addDownloadRecord(film: DownloadedFilm) = dao.upsertDownload(film)
    suspend fun removeDownloadRecord(filmId: String) = dao.deleteDownload(filmId)
    suspend fun clearAllDownloads() = dao.clearAllDownloads()
}
