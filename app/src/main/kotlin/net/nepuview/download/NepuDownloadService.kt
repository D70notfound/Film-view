package net.nepuview.download

import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import dagger.hilt.android.AndroidEntryPoint
import net.nepuview.R
import net.nepuview.repository.DownloadRepository
import javax.inject.Inject

@AndroidEntryPoint
class NepuDownloadService : DownloadService(
    NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_desc
) {
    @Inject lateinit var downloadManagerProvider: DownloadManagerProvider
    @Inject lateinit var downloadRepository: DownloadRepository

    override fun getDownloadManager(): DownloadManager =
        downloadManagerProvider.get(this, downloadRepository.getDownloadDirectory())

    override fun getScheduler(): Scheduler =
        PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(
        downloads: MutableList<androidx.media3.exoplayer.offline.Download>,
        notMetRequirements: Int
    ) = DownloadNotificationHelper(this).buildProgressNotification(
        downloads, NOTIFICATION_ID
    )

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "nepu_downloads"
        const val JOB_ID = 1
    }
}
