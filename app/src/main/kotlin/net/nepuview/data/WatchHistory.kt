package net.nepuview.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filmId: String,
    val filmTitle: String,
    val posterUrl: String,
    val playerUrl: String,
    val watchedAt: Long = System.currentTimeMillis(),
    val progressPercent: Int = 0
)
