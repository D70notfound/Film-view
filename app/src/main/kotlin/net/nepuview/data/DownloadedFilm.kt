package net.nepuview.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloaded_films",
    indices = [Index(value = ["downloadedAt"])]
)
data class DownloadedFilm(
    @PrimaryKey val filmId: String,
    val filmTitle: String,
    val posterUrl: String,
    val m3u8Url: String,
    val fileSizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
    val quality: String = "720p"
)
