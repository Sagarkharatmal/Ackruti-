package com.ssk.chhupanchhupai

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** "Your Chhupan Chhupai night starts in 1 hour" reminders for RSVPed events. */
object Reminders {
    private const val EXTRA_ID = "eventId"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_STARTS = "startsAt"
    private const val HOUR = 60 * 60_000L

    /** Reminds 1 hour before (or 10 minutes before if that's already passed). */
    fun schedule(context: Context, eventId: String, title: String, startsAt: Long) {
        val now = System.currentTimeMillis()
        val at = listOf(startsAt - HOUR, startsAt - 10 * 60_000L).firstOrNull { it > now } ?: return
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context, eventId, title, startsAt))
        val prefs = (context.applicationContext as ChhupanApp).prefs
        prefs.reminders = prefs.reminders + (eventId to startsAt)
        prefs.reminderTitles = prefs.reminderTitles + (eventId to title)
    }

    fun cancel(context: Context, eventId: String) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context, eventId, "", 0))
        val prefs = (context.applicationContext as ChhupanApp).prefs
        prefs.reminders = prefs.reminders - eventId
        prefs.reminderTitles = prefs.reminderTitles - eventId
    }

    fun isScheduled(context: Context, eventId: String): Boolean =
        (context.applicationContext as ChhupanApp).prefs.reminders.containsKey(eventId)

    /** Alarms are wiped when the phone restarts; put them back. */
    fun reschedule(context: Context) {
        val prefs = (context.applicationContext as ChhupanApp).prefs
        val titles = prefs.reminderTitles
        for ((id, startsAt) in prefs.reminders) {
            if (startsAt < System.currentTimeMillis()) {
                prefs.reminders = prefs.reminders - id
            } else {
                schedule(context, id, titles[id] ?: "Chhupan Chhupai night", startsAt)
            }
        }
    }

    private fun pending(context: Context, eventId: String, title: String, startsAt: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            Intent(context, ReminderReceiver::class.java)
                .putExtra(EXTRA_ID, eventId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_STARTS, startsAt),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    internal fun show(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Chhupan Chhupai night"
        val startsAt = intent.getLongExtra(EXTRA_STARTS, 0)
        val minutes = ((startsAt - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val open = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (minutes > 0) "Starts in $minutes min. Charge your phone and head to the meeting point!" else "Starting now!"
        val notification = NotificationCompat.Builder(context, Alerts.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(3000 + Math.floorMod(eventId.hashCode(), 1000), notification)
        } catch (_: SecurityException) {
        }
        val prefs = (context.applicationContext as ChhupanApp).prefs
        prefs.reminders = prefs.reminders - eventId
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Reminders.reschedule(context) else Reminders.show(context, intent)
    }
}
