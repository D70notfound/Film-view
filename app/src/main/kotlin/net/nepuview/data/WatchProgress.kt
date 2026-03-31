package net.nepuview.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_progress",
    indices = [Index(value = ["updatedAt"])]
)
data class WatchProgress(
    @PrimaryKey val filmId: String,
    val filmTitle: String,
    val posterUrl: String,
    val playerUrl: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressPercent: Int
        get() = if (durationMs > 0) ((positionMs * 100) / durationMs).toInt() else 0
}
