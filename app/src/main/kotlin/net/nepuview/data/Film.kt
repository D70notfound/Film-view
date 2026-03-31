package net.nepuview.data

data class Film(
    val id: String,
    val title: String,
    val year: String = "",
    val genre: List<String> = emptyList(),
    val duration: String = "",
    val rating: Float = 0f,
    val description: String = "",
    val posterUrl: String = "",
    val detailUrl: String = "",
    val playerUrl: String = "",
    val type: FilmType = FilmType.MOVIE,
    val mood: List<String> = emptyList()
)

enum class FilmType { MOVIE, SERIES, ANIME }
