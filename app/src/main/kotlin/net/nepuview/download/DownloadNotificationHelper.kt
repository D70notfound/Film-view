package net.nepuview.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.exoplayer.offline.Download
import net.nepuview.R

class DownloadNotificationHelper(private val context: Context) {

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NepuDownloadService.CHANNEL_ID,
                context.getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.download_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildProgressNotification(
        downloads: List<Download>,
        notificationId: Int
    ): Notification {
        val downloading = downloads.filter { it.state == Download.STATE_DOWNLOADING }
        val progress = if (downloading.isNotEmpty()) {
            (downloading.map { it.percentDownloaded }.average()).toInt()
        } else 0

        return NotificationCompat.Builder(context, NepuDownloadService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.notification_downloading))
            .setContentText("${downloading.size} aktiv · $progress%")
            .setProgress(100, progress, downloading.isEmpty())
            .setOngoing(true)
            .build()
    }
}
