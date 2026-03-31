package net.nepuview.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistFilm(
    @PrimaryKey val filmId: String,
    val filmTitle: String,
    val posterUrl: String,
    val detailUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)
