package net.nepuview.scraper

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.nepuview.data.Film
import net.nepuview.data.FilmType
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScraperEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BASE_URL = "https://nepu.to"
        private const val SEARCH_URL = "$BASE_URL/search/"
    }

    fun loadHome(mood: String? = null): Flow<List<Film>> = callbackFlow {
        val url = if (mood != null) "$BASE_URL/?genre=${mood}" else BASE_URL
        scrapeFilmList(url) { films -> trySend(films) }
        awaitClose()
    }

    fun search(query: String): Flow<List<Film>> = callbackFlow {
        val url = "$SEARCH_URL${query.trim().replace(" ", "+")}"
        scrapeFilmList(url) { films -> trySend(films) }
        awaitClose()
    }

    fun loadDetail(detailUrl: String): Flow<Film?> = callbackFlow {
        scrapeDetail(detailUrl) { film -> trySend(film) }
        awaitClose()
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun scrapeFilmList(url: String, onResult: (List<Film>) -> Unit) {
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
        }

        val bridge = object {
            @JavascriptInterface
            fun onFilmsReady(json: String) {
                val films = parseFilmListJson(json)
                onResult(films)
            }
        }
        webView.addJavascriptInterface(bridge, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, pageUrl: String) {
                view.evaluateJavascript(FILM_LIST_JS, null)
            }
        }
        webView.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun scrapeDetail(url: String, onResult: (Film?) -> Unit) {
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
        }

        val bridge = object {
            @JavascriptInterface
            fun onDetailReady(json: String) {
                val film = parseDetailJson(json)
                onResult(film)
            }
        }
        webView.addJavascriptInterface(bridge, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, pageUrl: String) {
                view.evaluateJavascript(DETAIL_JS, null)
            }
        }
        webView.loadUrl(url)
    }

    private fun parseFilmListJson(json: String): List<Film> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Film(
                    id = obj.optString("id", obj.optString("title").hashCode().toString()),
                    title = obj.optString("title"),
                    posterUrl = obj.optString("poster"),
                    detailUrl = obj.optString("url"),
                    year = obj.optString("year"),
                    rating = obj.optDouble("rating", 0.0).toFloat(),
                    type = when (obj.optString("type").lowercase()) {
                        "series", "serie" -> FilmType.SERIES
                        "anime" -> FilmType.ANIME
                        else -> FilmType.MOVIE
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseDetailJson(json: String): Film? {
        return try {
            val obj = org.json.JSONObject(json)
            val genreArr = obj.optJSONArray("genres")
            val genres = if (genreArr != null) {
                (0 until genreArr.length()).map { genreArr.getString(it) }
            } else emptyList()
            Film(
                id = obj.optString("id", obj.optString("title").hashCode().toString()),
                title = obj.optString("title"),
                posterUrl = obj.optString("poster"),
                detailUrl = obj.optString("url"),
                playerUrl = obj.optString("playerUrl"),
                year = obj.optString("year"),
                duration = obj.optString("duration"),
                rating = obj.optDouble("rating", 0.0).toFloat(),
                description = obj.optString("description"),
                genre = genres,
                type = when (obj.optString("type").lowercase()) {
                    "series", "serie" -> FilmType.SERIES
                    "anime" -> FilmType.ANIME
                    else -> FilmType.MOVIE
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    // JS injected into the film-list page to extract card data
    private val FILM_LIST_JS = """
        (function() {
            try {
                var cards = document.querySelectorAll('.film-poster, .flw-item, article.item, .movies .item');
                var results = [];
                cards.forEach(function(card) {
                    var a = card.querySelector('a[href]');
                    var img = card.querySelector('img');
                    var title = card.querySelector('.film-name, .name, h2, h3');
                    var year = card.querySelector('.fdi-item, .year');
                    var rating = card.querySelector('.film-poster-quality, .rating');
                    var type = card.querySelector('.fdi-type, .type');
                    if (!a) return;
                    results.push({
                        id: a.href,
                        title: title ? title.textContent.trim() : (img ? img.alt : ''),
                        url: a.href,
                        poster: img ? (img.dataset.src || img.src) : '',
                        year: year ? year.textContent.trim() : '',
                        rating: rating ? parseFloat(rating.textContent) || 0 : 0,
                        type: type ? type.textContent.trim().toLowerCase() : 'movie'
                    });
                });
                Android.onFilmsReady(JSON.stringify(results));
            } catch(e) {
                Android.onFilmsReady('[]');
            }
        })();
    """.trimIndent()

    // JS injected into a detail page to extract film metadata
    private val DETAIL_JS = """
        (function() {
            try {
                var title = document.querySelector('h2.film-name, .heading-name, h1')?.textContent?.trim() || '';
                var poster = document.querySelector('.film-poster img, .detail-infor .film-poster img')?.src || '';
                var desc = document.querySelector('.film-description .text, .description, .overview')?.textContent?.trim() || '';
                var year = document.querySelector('.item.mr-3, [itemprop="dateCreated"]')?.textContent?.trim() || '';
                var duration = document.querySelector('.item:not(.mr-3)')?.textContent?.trim() || '';
                var rating = parseFloat(document.querySelector('.item.rating, .vote_average')?.textContent) || 0;
                var playerLink = document.querySelector('a.btn-play, a[href*="watch"], .play-btn')?.href || '';
                var genres = Array.from(document.querySelectorAll('.item a[href*="genre"], .genres a')).map(a => a.textContent.trim());
                var type = document.querySelector('.item.type')?.textContent?.trim()?.toLowerCase() || 'movie';
                Android.onDetailReady(JSON.stringify({
                    title, poster, description: desc, year, duration,
                    rating, playerUrl: playerLink, genres, type,
                    url: window.location.href
                }));
            } catch(e) {
                Android.onDetailReady('{}');
            }
        })();
    """.trimIndent()
}
