package com.example.fxzmusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val prefs   = context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)
        val title   = prefs.getString("last_title", null)  ?: return
        val artist  = prefs.getString("last_artist", null) ?: return

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else
            @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

        val deviceName = try { device?.name ?: "Bluetooth" } catch (_: SecurityException) { "Bluetooth" }

        val channelId = "bt_resume"
        val nm        = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Bluetooth", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("action", "open_player")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, MediaButtonReceiver::class.java).apply {
                action = "com.example.fxzmusic.ACTION_RESUME"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("$deviceName conectado")
            .setContentText("$title · $artist")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_media_play,
                "Reanudar",
                resumeIntent
            )
            .build()

        nm.notify(9001, notification)
    }
}

class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.fxzmusic.ACTION_RESUME") return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            try {
                val controller = future.get()
                if (!controller.isPlaying) controller.play()
                MediaController.releaseFuture(future)
            } catch (_: Exception) {}
        }, context.mainExecutor)
    }
}

