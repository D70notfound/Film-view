package net.nepuview.scraper

import net.nepuview.data.FilmType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for JSON-parsing logic extracted from ScraperEngine.
 * Tests run on JVM without Android dependencies.
 */
class ScraperJsonParserTest {

    // ── Film List JSON parsing ────────────────────────────────────────────────

    @Test
    fun `parseFilmList returns correct film count`() {
        val json = buildFilmListJson(3)
        val films = parseFilmListJson(json)
        assertEquals(3, films.size)
    }

    @Test
    fun `parseFilmList maps title correctly`() {
        val json = buildFilmListJson(1, title = "Interstellar")
        val films = parseFilmListJson(json)
        assertEquals("Interstellar", films.first().title)
    }

    @Test
    fun `parseFilmList clamps rating to 0-10`() {
        val json = JSONArray().apply {
            put(JSONObject().apply {
                put("title", "Test Film")
                put("url", "https://nepu.to/film/1")
                put("rating", 99.9)
            })
        }.toString()
        val films = parseFilmListJson(json)
        assertTrue(films.first().rating <= 10f)
    }

    @Test
    fun `parseFilmList skips items with blank title`() {
        val json = JSONArray().apply {
            put(JSONObject().apply { put("title", ""); put("url", "https://nepu.to/1") })
            put(JSONObject().apply { put("title", "Valid Film"); put("url", "https://nepu.to/2") })
        }.toString()
        val films = parseFilmListJson(json)
        assertEquals(1, films.size)
        assertEquals("Valid Film", films.first().title)
    }

    @Test
    fun `parseFilmList returns empty list on invalid JSON`() {
        val films = parseFilmListJson("not valid json {{{{")
        assertTrue(films.isEmpty())
    }

    @Test
    fun `parseFilmList returns empty list on empty array`() {
        val films = parseFilmListJson("[]")
        assertTrue(films.isEmpty())
    }

    @Test
    fun `parseFilmList handles missing optional fields gracefully`() {
        val json = JSONArray().apply {
            put(JSONObject().apply {
                put("title", "Minimal Film")
                put("url", "https://nepu.to/film/minimal")
                // no poster, year, rating, type
            })
        }.toString()
        val films = parseFilmListJson(json)
        assertEquals(1, films.size)
        assertEquals("", films.first().posterUrl)
        assertEquals(0f, films.first().rating)
    }

    @Test
    fun `parseFilmList detects anime type`() {
        val json = buildFilmListJson(1, type = "anime")
        val films = parseFilmListJson(json)
        assertEquals(FilmType.ANIME, films.first().type)
    }

    @Test
    fun `parseFilmList detects series type`() {
        val json = buildFilmListJson(1, type = "serie")
        val films = parseFilmListJson(json)
        assertEquals(FilmType.SERIES, films.first().type)
    }

    // ── Detail JSON parsing ───────────────────────────────────────────────────

    @Test
    fun `parseDetail returns null on empty JSON`() {
        val film = parseDetailJson("{}")
        assertNull(film)
    }

    @Test
    fun `parseDetail returns null on invalid JSON`() {
        val film = parseDetailJson("not json")
        assertNull(film)
    }

    @Test
    fun `parseDetail maps all fields correctly`() {
        val json = JSONObject().apply {
            put("title", "Inception")
            put("poster", "https://cdn.nepu.to/inception.jpg")
            put("description", "A thief who steals corporate secrets.")
            put("year", "2010")
            put("duration", "148 min")
            put("rating", 8.8)
            put("playerUrl", "https://nepu.to/watch/inception")
            put("genres", JSONArray(listOf("Action", "Sci-Fi")))
            put("type", "movie")
            put("url", "https://nepu.to/film/inception")
        }.toString()
        val film = parseDetailJson(json)!!
        assertEquals("Inception", film.title)
        assertEquals("2010", film.year)
        assertEquals(8.8f, film.rating, 0.01f)
        assertEquals(listOf("Action", "Sci-Fi"), film.genre)
        assertEquals(FilmType.MOVIE, film.type)
    }

    @Test
    fun `parseDetail clamps rating to 0-10`() {
        val json = JSONObject().apply {
            put("title", "High Rated")
            put("rating", 150.0)
        }.toString()
        val film = parseDetailJson(json)!!
        assertTrue(film.rating <= 10f)
    }

    @Test
    fun `parseDetail handles empty genres array`() {
        val json = JSONObject().apply {
            put("title", "No Genre Film")
            put("genres", JSONArray())
        }.toString()
        val film = parseDetailJson(json)!!
        assertTrue(film.genre.isEmpty())
    }

    // ── Film type parsing ─────────────────────────────────────────────────────

    @Test
    fun `parseFilmType maps known types`() {
        assertEquals(FilmType.ANIME, parseFilmType("anime"))
        assertEquals(FilmType.SERIES, parseFilmType("series"))
        assertEquals(FilmType.SERIES, parseFilmType("serie"))
        assertEquals(FilmType.SERIES, parseFilmType("tv"))
        assertEquals(FilmType.MOVIE, parseFilmType("movie"))
        assertEquals(FilmType.MOVIE, parseFilmType(""))
        assertEquals(FilmType.MOVIE, parseFilmType("unknown"))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildFilmListJson(
        count: Int,
        title: String = "Film",
        type: String = "movie"
    ): String = JSONArray().apply {
        repeat(count) { i ->
            put(JSONObject().apply {
                put("id", "id_$i")
                put("title", if (count == 1) title else "$title $i")
                put("url", "https://nepu.to/film/$i")
                put("poster", "https://cdn.nepu.to/poster_$i.jpg")
                put("year", "202$i")
                put("rating", 7.0 + i * 0.5)
                put("type", type)
            })
        }
    }.toString()

    // ── Replicated parsing logic (mirrors ScraperEngine private methods) ──────

    private fun parseFilmListJson(json: String): List<net.nepuview.data.Film> {
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<net.nepuview.data.Film>()
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    val t = obj.optString("title").trim()
                    if (t.isBlank()) continue
                    result.add(
                        net.nepuview.data.Film(
                            id = obj.optString("id").ifBlank { t.hashCode().toString() },
                            title = t,
                            posterUrl = obj.optString("poster"),
                            detailUrl = obj.optString("url"),
                            year = obj.optString("year"),
                            rating = obj.optDouble("rating", 0.0).toFloat().coerceIn(0f, 10f),
                            type = parseFilmType(obj.optString("type"))
                        )
                    )
                } catch (_: Exception) {}
            }
            result
        } catch (_: Exception) { emptyList() }
    }

    private fun parseDetailJson(json: String): net.nepuview.data.Film? {
        return try {
            val obj = JSONObject(json)
            val t = obj.optString("title").trim()
            if (t.isBlank()) return null
            val genreArr = obj.optJSONArray("genres")
            val genres = buildList {
                if (genreArr != null) for (i in 0 until genreArr.length()) {
                    val g = genreArr.optString(i).trim()
                    if (g.isNotBlank()) add(g)
                }
            }
            net.nepuview.data.Film(
                id = obj.optString("id").ifBlank { t.hashCode().toString() },
                title = t,
                posterUrl = obj.optString("poster"),
                detailUrl = obj.optString("url"),
                playerUrl = obj.optString("playerUrl"),
                year = obj.optString("year"),
                duration = obj.optString("duration"),
                rating = obj.optDouble("rating", 0.0).toFloat().coerceIn(0f, 10f),
                description = obj.optString("description"),
                genre = genres,
                type = parseFilmType(obj.optString("type"))
            )
        } catch (_: Exception) { null }
    }

    private fun parseFilmType(raw: String): FilmType = when (raw.lowercase().trim()) {
        "series", "serie", "tv" -> FilmType.SERIES
        "anime" -> FilmType.ANIME
        else -> FilmType.MOVIE
    }
}
