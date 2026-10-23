package com.ssk.chhupanchhupai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ssk.chhupanchhupai.data.Notice

/** Buzzes the phone for game events and shows notifications while the app is in the background. */
class Alerts(private val context: Context) {

    init {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_GAME, "Game running", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while a round is running and your location is shared with the game."
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Game alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pings, tags, zone warnings and results."
                enableVibration(true)
            },
        )
    }

    fun onNotice(notice: Notice, appInForeground: Boolean) {
        val pattern = when (notice.kind) {
            "tag_request" -> longArrayOf(0, 400, 150, 400, 150, 400)
            "revealed", "zone_warning" -> longArrayOf(0, 700, 200, 700)
            "pinged", "ping" -> longArrayOf(0, 250, 120, 250)
            "hunt", "caught", "game_over", "role" -> longArrayOf(0, 500)
            else -> null
        } ?: return
        vibrate(pattern)
        if (!appInForeground) notify(notice)
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun notify(notice: Notice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(notice.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notice.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ALERT_ID_BASE + Math.floorMod(notice.kind.hashCode(), 50), notification)
        } catch (_: SecurityException) {
            // Notifications switched off: the vibration still happened.
        }
    }

    companion object {
        const val CHANNEL_GAME = "game"
        const val CHANNEL_ALERTS = "alerts"
        private const val ALERT_ID_BASE = 2000

        fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
