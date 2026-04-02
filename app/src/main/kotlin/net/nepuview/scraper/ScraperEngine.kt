package net.nepuview.scraper

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.nepuview.data.Film
import net.nepuview.data.FilmType
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScraperEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ScraperEngine"
        private const val BASE_URL = "https://nepu.to"
        private const val SEARCH_URL = "$BASE_URL/search/"
        private const val TIMEOUT_MS = 30_000L
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    fun loadHome(mood: String? = null): Flow<List<Film>> = callbackFlow {
        val url = if (mood != null) "$BASE_URL/?genre=${mood}" else BASE_URL
        var webViewRef: WebView? = null
        val timeoutRef = scrapeFilmList(url,
            onResult = { films -> trySend(films) },
            onError = { msg ->
                Log.e(TAG, "loadHome($mood) failed: $msg")
                trySend(emptyList())
            },
            onWebViewCreated = { webViewRef = it }
        )
        awaitClose {
            val mainHandler = Handler(Looper.getMainLooper())
            timeoutRef?.let { mainHandler.removeCallbacks(it) }
            mainHandler.post {
                webViewRef?.destroy()
                webViewRef = null
            }
        }
    }

    fun search(query: String): Flow<List<Film>> = callbackFlow {
        val url = "$SEARCH_URL${Uri.encode(query.trim())}"
        var webViewRef: WebView? = null
        val timeoutRef = scrapeFilmList(url,
            onResult = { films -> trySend(films) },
            onError = { msg ->
                Log.e(TAG, "search($query) failed: $msg")
                trySend(emptyList())
            },
            onWebViewCreated = { webViewRef = it }
        )
        awaitClose {
            val mainHandler = Handler(Looper.getMainLooper())
            timeoutRef?.let { mainHandler.removeCallbacks(it) }
            mainHandler.post {
                webViewRef?.destroy()
                webViewRef = null
            }
        }
    }

    fun loadDetail(detailUrl: String): Flow<Film?> = callbackFlow {
        var webViewRef: WebView? = null
        val timeoutRef = scrapeDetail(detailUrl,
            onResult = { film -> trySend(film) },
            onError = { msg ->
                Log.e(TAG, "loadDetail($detailUrl) failed: $msg")
                trySend(null)
            },
            onWebViewCreated = { webViewRef = it }
        )
        awaitClose {
            val mainHandler = Handler(Looper.getMainLooper())
            timeoutRef?.let { mainHandler.removeCallbacks(it) }
            mainHandler.post {
                webViewRef?.destroy()
                webViewRef = null
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun scrapeFilmList(
        url: String,
        onResult: (List<Film>) -> Unit,
        onError: (String) -> Unit,
        onWebViewCreated: ((WebView) -> Unit)? = null
    ): Runnable {
        val mainHandler = Handler(Looper.getMainLooper())
        val completed = AtomicBoolean(false)
        var webViewRef: WebView? = null

        val timeoutRunnable = Runnable {
            if (completed.compareAndSet(false, true)) {
                Log.w(TAG, "Timeout scraping film list: $url")
                onError("Timeout beim Laden der Film-Liste")
                webViewRef?.destroy()
                webViewRef = null
            }
        }
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)

        mainHandler.post {
            val webView = WebView(context)
            webViewRef = webView
            onWebViewCreated?.invoke(webView)
            try {
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    userAgentString = USER_AGENT
                }

                val bridge = object {
                    @JavascriptInterface
                    fun onFilmsReady(json: String) {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        if (completed.compareAndSet(false, true)) {
                            val films = parseFilmListJson(json)
                            Log.d(TAG, "Scraped ${films.size} films from $url")
                            onResult(films)
                            mainHandler.post {
                                webView.destroy()
                                webViewRef = null
                            }
                        }
                    }
                }
                webView.addJavascriptInterface(bridge, "Android")

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, pageUrl: String) {
                        view.evaluateJavascript(FILM_LIST_JS, null)
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            if (completed.compareAndSet(false, true)) {
                                val msg = "WebView Fehler: ${error.description}"
                                Log.e(TAG, msg)
                                onError(msg)
                                mainHandler.post {
                                    webView.destroy()
                                    webViewRef = null
                                }
                            }
                        }
                    }
                }
                webView.loadUrl(url)
            } catch (e: Exception) {
                mainHandler.removeCallbacks(timeoutRunnable)
                if (completed.compareAndSet(false, true)) {
                    Log.e(TAG, "Exception scraping $url", e)
                    onError(e.message ?: "Unbekannter Fehler")
                    webView.destroy()
                    webViewRef = null
                }
            }
        }
        return timeoutRunnable
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun scrapeDetail(
        url: String,
        onResult: (Film?) -> Unit,
        onError: (String) -> Unit,
        onWebViewCreated: ((WebView) -> Unit)? = null
    ): Runnable {
        val mainHandler = Handler(Looper.getMainLooper())
        val completed = AtomicBoolean(false)
        var webViewRef: WebView? = null

        val timeoutRunnable = Runnable {
            if (completed.compareAndSet(false, true)) {
                Log.w(TAG, "Timeout scraping detail: $url")
                onError("Timeout beim Laden der Film-Details")
                webViewRef?.destroy()
                webViewRef = null
            }
        }
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)

        mainHandler.post {
            val webView = WebView(context)
            webViewRef = webView
            onWebViewCreated?.invoke(webView)
            try {
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    userAgentString = USER_AGENT
                }

                val bridge = object {
                    @JavascriptInterface
                    fun onDetailReady(json: String) {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        if (completed.compareAndSet(false, true)) {
                            val film = parseDetailJson(json)
                            Log.d(TAG, "Scraped detail: ${film?.title} from $url")
                            onResult(film)
                            mainHandler.post {
                                webView.destroy()
                                webViewRef = null
                            }
                        }
                    }
                }
                webView.addJavascriptInterface(bridge, "Android")

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, pageUrl: String) {
                        view.evaluateJavascript(DETAIL_JS, null)
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            if (completed.compareAndSet(false, true)) {
                                val msg = "WebView Fehler: ${error.description}"
                                Log.e(TAG, msg)
                                onError(msg)
                                mainHandler.post {
                                    webView.destroy()
                                    webViewRef = null
                                }
                            }
                        }
                    }
                }
                webView.loadUrl(url)
            } catch (e: Exception) {
                mainHandler.removeCallbacks(timeoutRunnable)
                if (completed.compareAndSet(false, true)) {
                    Log.e(TAG, "Exception scraping detail $url", e)
                    onError(e.message ?: "Unbekannter Fehler")
                    webView.destroy()
                    webViewRef = null
                }
            }
        }
        return timeoutRunnable
    }

    private fun parseFilmListJson(json: String): List<Film> {
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<Film>()
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title").trim()
                    if (title.isBlank()) continue
                    result.add(
                        Film(
                            id = obj.optString("id").ifBlank {
                                title.hashCode().toString()
                            },
                            title = title,
                            posterUrl = obj.optString("poster"),
                            detailUrl = obj.optString("url"),
                            year = obj.optString("year"),
                            rating = obj.optDouble("rating", 0.0).toFloat()
                                .coerceIn(0f, 10f),
                            type = parseFilmType(obj.optString("type"))
                        )
                    )
                } catch (itemEx: Exception) {
                    Log.w(TAG, "Skipping malformed film item at index $i", itemEx)
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse film list JSON", e)
            emptyList()
        }
    }

    private fun parseDetailJson(json: String): Film? {
        return try {
            val obj = JSONObject(json)
            val title = obj.optString("title").trim()
            if (title.isBlank()) {
                Log.w(TAG, "Detail JSON has no title")
                return null
            }
            val genreArr = obj.optJSONArray("genres")
            val genres = buildList {
                if (genreArr != null) {
                    for (i in 0 until genreArr.length()) {
                        val g = genreArr.optString(i).trim()
                        if (g.isNotBlank()) add(g)
                    }
                }
            }
            Film(
                id = obj.optString("id").ifBlank { title.hashCode().toString() },
                title = title,
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse detail JSON", e)
            null
        }
    }

    private fun parseFilmType(raw: String): FilmType = when (raw.lowercase().trim()) {
        "series", "serie", "tv" -> FilmType.SERIES
        "anime" -> FilmType.ANIME
        else -> FilmType.MOVIE
    }

    // ── JavaScript injected into film-list pages ──────────────────────────────
    // NOTE: These selectors target the most common nepu.to DOM patterns.
    // If nepu.to changes its HTML structure, update the selector chains below.
    private val FILM_LIST_JS = """
        (function() {
            try {
                var cards = document.querySelectorAll(
                    '.film-poster, .flw-item, .item, article.item, .movies .item, ' +
                    '[class*="film-item"], [class*="movie-item"], [class*="item-card"]'
                );
                var results = [];
                cards.forEach(function(card) {
                    try {
                        var a = card.querySelector('a[href]');
                        var img = card.querySelector('img[data-src], img[src]');
                        var titleEl = card.querySelector(
                            '.film-name, .name, .title, [class*="title"], h2, h3, h4'
                        );
                        var yearEl = card.querySelector(
                            '.fdi-item, .year, [class*="year"], [class*="date"]'
                        );
                        var ratingEl = card.querySelector(
                            '.film-poster-quality, .rating, [class*="rating"], [class*="score"]'
                        );
                        var typeEl = card.querySelector(
                            '.fdi-type, .type, [class*="type"]'
                        );
                        if (!a || !a.href) return;
                        var title = titleEl
                            ? titleEl.textContent.trim()
                            : (img ? img.alt : '');
                        if (!title) return;
                        results.push({
                            id: a.href,
                            title: title,
                            url: a.href,
                            poster: img ? (img.dataset.src || img.getAttribute('data-lazy') || img.src) : '',
                            year: yearEl ? yearEl.textContent.trim() : '',
                            rating: ratingEl ? parseFloat(ratingEl.textContent) || 0 : 0,
                            type: typeEl ? typeEl.textContent.trim().toLowerCase() : 'movie'
                        });
                    } catch(itemErr) {}
                });
                Android.onFilmsReady(JSON.stringify(results));
            } catch(e) {
                Android.onFilmsReady('[]');
            }
        })();
    """.trimIndent()

    private val DETAIL_JS = """
        (function() {
            try {
                var title = (
                    document.querySelector('h2.film-name, .heading-name, h1.title, h1') ||
                    { textContent: '' }
                ).textContent.trim();

                var posterEl = document.querySelector(
                    '.film-poster img, .detail-infor img, [class*="poster"] img, ' +
                    'meta[property="og:image"]'
                );
                var poster = posterEl
                    ? (posterEl.src || posterEl.content || '')
                    : '';

                var desc = (
                    document.querySelector(
                        '.film-description .text, .description, .overview, ' +
                        '[class*="synopsis"], [class*="description"]'
                    ) || { textContent: '' }
                ).textContent.trim();

                var year = (
                    document.querySelector(
                        '.item.mr-3, [itemprop="dateCreated"], [class*="year"]'
                    ) || { textContent: '' }
                ).textContent.trim();

                var duration = (
                    document.querySelector(
                        '.item:not(.mr-3):not(.type), [class*="duration"], [class*="runtime"]'
                    ) || { textContent: '' }
                ).textContent.trim();

                var ratingEl = document.querySelector(
                    '.item.rating, .vote_average, [class*="rating"], [itemprop="ratingValue"]'
                );
                var rating = ratingEl ? parseFloat(ratingEl.textContent) || 0 : 0;

                var playerEl = document.querySelector(
                    'a.btn-play, a[href*="watch"], .play-btn, [class*="play"] a'
                );
                var playerUrl = playerEl ? playerEl.href : '';

                var genres = Array.from(
                    document.querySelectorAll(
                        '.item a[href*="genre"], .genres a, [class*="genre"] a'
                    )
                ).map(function(a) { return a.textContent.trim(); })
                 .filter(function(g) { return g.length > 0; });

                var typeEl = document.querySelector('.item.type, [class*="type"]');
                var type = typeEl ? typeEl.textContent.trim().toLowerCase() : 'movie';

                Android.onDetailReady(JSON.stringify({
                    title: title,
                    poster: poster,
                    description: desc,
                    year: year,
                    duration: duration,
                    rating: rating,
                    playerUrl: playerUrl,
                    genres: genres,
                    type: type,
                    url: window.location.href
                }));
            } catch(e) {
                Android.onDetailReady('{}');
            }
        })();
    """.trimIndent()
}
