package ro.vishrama.tobedone.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ro.vishrama.tobedone.data.DueTask
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Android version of the iOS NotificationManager: one reminder per due task,
 * shown on the morning of the due date. Uses inexact alarms, so the app does
 * not need the "exact alarm" permission that Google Play restricts.
 */
object ReminderScheduler {

    const val CHANNEL_ID = "task_reminders"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_TITLE = "title"

    /** Time of day the reminder is shown on the due date. */
    private val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Reminds you on the day a task is due" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, task: DueTask) {
        if (task.isCompleted) return
        val triggerAt = LocalDate.ofEpochDay(task.dueEpochDay)
            .atTime(REMINDER_TIME)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context, task))
    }

    fun cancel(context: Context, taskId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }

    fun rescheduleAll(context: Context, tasks: List<DueTask>) {
        tasks.filter { !it.isCompleted }.forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, task: DueTask): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_TASK_ID, task.id)
            .putExtra(EXTRA_TITLE, task.title)
        return PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
