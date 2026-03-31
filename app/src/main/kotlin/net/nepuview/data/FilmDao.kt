package net.nepuview.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmDao {

    // ── Watch Progress ──────────────────────────────────────
    @Upsert
    suspend fun upsertProgress(progress: WatchProgress)

    @Query("SELECT * FROM watch_progress WHERE filmId = :filmId")
    suspend fun getProgress(filmId: String): WatchProgress?

    @Query("DELETE FROM watch_progress WHERE filmId = :filmId")
    suspend fun deleteProgress(filmId: String)

    // ── Watch History ───────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistory)

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    fun observeHistory(): Flow<List<WatchHistory>>

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Long)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()

    // ── Favorites ────────────────────────────────────────────
    @Upsert
    suspend fun upsertFavorite(film: FavoriteFilm)

    @Delete
    suspend fun deleteFavorite(film: FavoriteFilm)

    @Query("DELETE FROM favorites WHERE filmId = :filmId")
    suspend fun deleteFavoriteById(filmId: String)

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteFilm>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE filmId = :filmId)")
    fun isFavorite(filmId: String): Flow<Boolean>

    // ── Watchlist ─────────────────────────────────────────────
    @Upsert
    suspend fun upsertWatchlist(film: WatchlistFilm)

    @Query("DELETE FROM watchlist WHERE filmId = :filmId")
    suspend fun deleteWatchlistItem(filmId: String)

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeWatchlist(): Flow<List<WatchlistFilm>>

    // ── Downloaded Films ──────────────────────────────────────
    @Upsert
    suspend fun upsertDownload(film: DownloadedFilm)

    @Query("DELETE FROM downloaded_films WHERE filmId = :filmId")
    suspend fun deleteDownload(filmId: String)

    @Query("SELECT * FROM downloaded_films ORDER BY downloadedAt DESC")
    fun observeDownloads(): Flow<List<DownloadedFilm>>

    @Query("SELECT SUM(fileSizeBytes) FROM downloaded_films")
    fun totalDownloadSize(): Flow<Long?>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_films WHERE filmId = :filmId)")
    fun isDownloaded(filmId: String): Flow<Boolean>

    @Query("DELETE FROM downloaded_films")
    suspend fun clearAllDownloads()
}
