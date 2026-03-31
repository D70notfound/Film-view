package net.nepuview.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteFilm(
    @PrimaryKey val filmId: String,
    val filmTitle: String,
    val posterUrl: String,
    val detailUrl: String,
    val rating: Float = 0f,
    val addedAt: Long = System.currentTimeMillis()
)
