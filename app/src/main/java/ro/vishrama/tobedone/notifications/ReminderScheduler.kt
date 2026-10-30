package ro.vishrama.tobedone.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import ro.vishrama.tobedone.data.AppData
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.Settings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Android version of the iOS NotificationManager + ReminderScheduler:
 *  • To Do task: a reminder at the task's due date and time.
 *  • Daily summary (default 21:00, set in Settings): lists that day's unfinished
 *    tasks and Daily Routine activities. Nothing is sent if everything is done.
 *  • Activity start: a reminder when each Daily Routine activity begins.
 *
 * Uses normal (inexact) alarms so the app doesn't need the restricted
 * "exact alarm" permission; a reminder can arrive a few minutes late.
 */
object ReminderScheduler {

    const val CHANNEL_TASKS = "task_reminders"
    const val CHANNEL_SUMMARY = "daily_summary"
    const val CHANNEL_ACTIVITY = "activity_start"

    const val KIND_TASK = "task"
    const val KIND_SUMMARY = "summary"
    const val KIND_ACTIVITY = "activity"
    const val EXTRA_KIND = "kind"
    const val EXTRA_ID = "id"

    private const val PREFS = "reminders"
    private const val KEY_SCHEDULED = "scheduled"
    private const val MAX_TASK_ALARMS = 100
    private const val MAX_ACTIVITY_ALARMS = 40
    private const val ACTIVITY_DAYS_AHEAD = 7L

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TASKS, "Task reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "When a To Do task is due" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SUMMARY, "Daily summary", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Evening list of what isn't finished yet" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ACTIVITY, "Activity start", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "When a Daily Routine activity starts" }
        )
    }

    fun refresh(context: Context) {
        val store = AppStore.get(context)
        refresh(context, store.data.value, store.settings.value)
    }

    /** Rebuilds every reminder from the current data. Safe to call often. */
    @Synchronized
    fun refresh(context: Context, data: AppData, settings: Settings) {
        val now = System.currentTimeMillis()
        val wanted = mutableMapOf<String, Long>()   // key -> trigger time

        data.tasks
            .filter { !it.isCompleted && it.dueAt > now }
            .sortedBy { it.dueAt }
            .take(MAX_TASK_ALARMS)
            .forEach { wanted["$KIND_TASK:${it.id}"] = it.dueAt }

        if (settings.dailySummaryEnabled) {
            wanted["$KIND_SUMMARY:next"] = nextSummaryTime(settings, now)
        }

        if (settings.activityStartEnabled) {
            val limit = now + ACTIVITY_DAYS_AHEAD * 24 * 60 * 60 * 1000
            data.activities
                .filter { !it.isCompleted && it.date > now && it.date < limit }
                .sortedBy { it.date }
                .take(MAX_ACTIVITY_ALARMS)
                .forEach { wanted["$KIND_ACTIVITY:${it.id}"] = it.date }
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY_SCHEDULED, emptySet()) ?: emptySet()

        // Cancel reminders that are no longer needed
        for (key in previous - wanted.keys) {
            alarmManager.cancel(pendingIntent(context, key))
        }
        // (Re)schedule the ones we want; the same key replaces the old alarm
        for ((key, at) in wanted) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context, key))
        }
        prefs.edit().putStringSet(KEY_SCHEDULED, wanted.keys.toSet()).apply()
    }

    private fun nextSummaryTime(settings: Settings, now: Long): Long {
        val zone = AppStore.zone()
        val time = LocalTime.of(settings.reminderHour, settings.reminderMinute)
        var at: ZonedDateTime = LocalDate.now(zone).atTime(time).atZone(zone)
        if (at.toInstant().toEpochMilli() <= now) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context, key: String): PendingIntent {
        val (kind, id) = key.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val intent = Intent(context, ReminderReceiver::class.java)
            .setData(Uri.parse("tobedone://reminder/$kind/$id"))   // makes each alarm unique
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
