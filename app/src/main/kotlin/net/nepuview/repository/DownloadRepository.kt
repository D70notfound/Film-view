package net.nepuview.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.StatFs
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.nepuview.download.DownloadManagerProvider
import net.nepuview.download.NepuDownloadService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManagerProvider: DownloadManagerProvider
) {
    fun startDownload(filmId: String, m3u8Url: String) {
        if (isWifiOnly() && !isOnWifi()) return

        val request = DownloadRequest.Builder(filmId, Uri.parse(m3u8Url))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        DownloadService.sendAddDownload(
            context,
            NepuDownloadService::class.java,
            request,
            false
        )
    }

    fun isWifiOnly(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean("wifi_only", true)
    }

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getDownloadDirectory(): File {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val location = prefs.getString("storage_location", "internal") ?: "internal"
        return when (location) {
            "external" -> {
                val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                extDir ?: File(context.filesDir, "downloads")
            }
            else -> File(context.filesDir, "downloads")
        }.also { it.mkdirs() }
    }

    fun pauseDownload(filmId: String) {
        DownloadService.sendSetStopReason(
            context,
            NepuDownloadService::class.java,
            filmId,
            1, // any value > 0 means "stopped/paused"
            false
        )
    }

    fun resumeDownload(filmId: String) {
        DownloadService.sendSetStopReason(
            context,
            NepuDownloadService::class.java,
            filmId,
            androidx.media3.exoplayer.offline.Download.STOP_REASON_NONE,
            false
        )
    }

    fun removeDownload(filmId: String) {
        DownloadService.sendRemoveDownload(
            context,
            NepuDownloadService::class.java,
            filmId,
            false
        )
    }

    fun getDownloadManager() = downloadManagerProvider.get(context)

    /** Returns free storage in bytes. */
    fun freeStorageBytes(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    fun isLowStorage(): Boolean = freeStorageBytes() < 500L * 1024 * 1024 // < 500 MB
}
