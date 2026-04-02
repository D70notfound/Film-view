package net.nepuview.download

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManagerProvider @Inject constructor() {

    private var downloadManager: DownloadManager? = null
    private var cache: SimpleCache? = null

    @Synchronized
    fun get(context: Context, downloadDir: File = File(context.filesDir, "downloads")): DownloadManager {
        if (downloadManager == null) {
            val databaseProvider = StandaloneDatabaseProvider(context)
            downloadDir.mkdirs()

            val localCache = SimpleCache(downloadDir, NoOpCacheEvictor(), databaseProvider)
            cache = localCache

            val dataSourceFactory = OkHttpDataSource.Factory(OkHttpClient.Builder().build())

            downloadManager = DownloadManager(
                context,
                databaseProvider,
                localCache,
                dataSourceFactory,
                Executors.newFixedThreadPool(3)
            ).apply {
                maxParallelDownloads = 3
            }
        }
        return checkNotNull(downloadManager)
    }

    fun getCache(context: Context): SimpleCache {
        get(context) // ensure initialized
        return checkNotNull(cache)
    }
}
