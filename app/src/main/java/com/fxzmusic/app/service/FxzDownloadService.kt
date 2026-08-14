package com.fxzmusic.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.fxzmusic.app.R

class FxzDownloadService : DownloadService(
    1234,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    "download_channel",
    R.string.download_channel_name,
    0
) {

    override fun getDownloadManager(): DownloadManager = DownloadUtil.get().getDownloadManager()

    override fun getScheduler(): Scheduler = PlatformScheduler(this, 1234)

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int
    ): Notification {
        val totalCount = downloads.size
        val completedCount = downloads.count { it.state == Download.STATE_COMPLETED }
        val activeCount = downloads.count {
            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED || it.state == Download.STATE_RESTARTING
        }
        val currentStep = (completedCount + 1).coerceAtMost(totalCount)

        val totalPercent = if (downloads.isNotEmpty()) {
            val sum = downloads.fold(0f) { acc, d ->
                acc + when (d.state) {
                    Download.STATE_COMPLETED -> 100f
                    Download.STATE_DOWNLOADING -> d.percentDownloaded.coerceAtLeast(0f)
                    else -> 0f
                }
            }
            (sum / downloads.size).toInt().coerceIn(0, 100)
        } else 0

        val contentText = if (totalCount > 1) {
            "Descargando $currentStep de $totalCount ($totalPercent%)"
        } else {
            "Descargando ($totalPercent%)"
        }

        return NotificationCompat.Builder(this, "download_channel")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargas de FxzMusic")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, totalPercent, totalPercent == 0 && activeCount > 0)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                "download_channel",
                getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            nm?.createNotificationChannel(channel)
        }
    }
}
