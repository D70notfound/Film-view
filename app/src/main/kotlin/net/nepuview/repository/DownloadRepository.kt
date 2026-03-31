package net.nepuview.repository

import android.content.Context
import android.os.StatFs
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import net.nepuview.download.DownloadManagerProvider
import net.nepuview.download.NepuDownloadService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManagerProvider: DownloadManagerProvider
) {
    fun startDownload(filmId: String, m3u8Url: String) {
        val mediaItem = MediaItem.fromUri(m3u8Url)
        val request = DownloadRequest.Builder(filmId, mediaItem.localConfiguration!!.uri)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        DownloadService.sendAddDownload(
            context,
            NepuDownloadService::class.java,
            request,
            false
        )
    }

    fun pauseDownload(filmId: String) {
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
