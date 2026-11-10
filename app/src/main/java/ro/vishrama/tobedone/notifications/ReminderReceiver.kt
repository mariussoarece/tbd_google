package ro.vishrama.tobedone.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ro.vishrama.tobedone.MainActivity
import ro.vishrama.tobedone.R
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.describeMinutes
import ro.vishrama.tobedone.widget.TodayWidget
import java.time.LocalDate

/** Shows a reminder when one of the alarms set by [ReminderScheduler] fires. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: return
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        val data = AppStore.get(context).data.value

        when (kind) {
            ReminderScheduler.KIND_TASK -> {
                val task = data.tasks.firstOrNull { it.id == id }
                if (task != null && !task.isCompleted) {
                    show(context, ReminderScheduler.CHANNEL_TASKS, "task:$id",
                        "Task Reminder", "Don't forget: ${task.title} is due now!")
                }
            }

            ReminderScheduler.KIND_ACTIVITY -> {
                val item = data.activities.firstOrNull { it.id == id }
                if (item != null && !item.isCompleted) {
                    val body = listOfNotNull(
                        item.durationMinutes.takeIf { it > 0 }?.let { "$it min" },
                        item.details.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifEmpty { "Time for your activity." }
                    show(context, ReminderScheduler.CHANNEL_ACTIVITY, "activity:$id",
                        "Starting now: ${item.name}", body)
                }
            }

            ReminderScheduler.KIND_MONTHLY -> {
                val m = data.monthlyTasks.firstOrNull { it.id == id }
                if (m != null && !m.isPaid(java.time.YearMonth.now(AppStore.zone()))) {
                    val currency = AppStore.get(context).settings.value.currency
                    val amount = if (m.price > 0) " (" + String.format("%.2f", m.price) + " $currency)" else ""
                    show(context, ReminderScheduler.CHANNEL_TASKS, "monthly:$id",
                        "Monthly expense", "${m.title}$amount is due today.")
                }
            }

            ReminderScheduler.KIND_WIDGET_HOUR -> TodayWidget.requestUpdate(context)

            ReminderScheduler.KIND_TASK_EARLY -> {
                val t = data.tasks.firstOrNull { it.id == id }
                val now = System.currentTimeMillis()
                if (t != null && !t.isCompleted && t.dueAt > now) {
                    show(context, ReminderScheduler.CHANNEL_TASKS, "taskearly:$id",
                        "Coming up", "${t.title} is starting in ${describeMinutes(minutesUntil(t.dueAt, now))}.")
                }
            }

            ReminderScheduler.KIND_ACTIVITY_EARLY -> {
                val a = data.activities.firstOrNull { it.id == id }
                val now = System.currentTimeMillis()
                if (a != null && !a.isCompleted && a.date > now) {
                    show(context, ReminderScheduler.CHANNEL_ACTIVITY, "activityearly:$id",
                        "Coming up", "${a.name} is starting in ${describeMinutes(minutesUntil(a.date, now))}.")
                }
            }

            ReminderScheduler.KIND_SUMMARY -> {
                val today = LocalDate.now(AppStore.zone())
                val endOfToday = today.plusDays(1).atStartOfDay(AppStore.zone()).toInstant().toEpochMilli()
                // Unfinished tasks due today or earlier, and today's unfinished activities
                val tasks = data.tasks.filter { !it.isCompleted && it.dueAt < endOfToday }.sortedBy { it.dueAt }
                val activities = data.activities
                    .filter { !it.isCompleted && AppStore.dayOf(it.date) == today }
                    .sortedBy { it.date }
                if (tasks.isNotEmpty() || activities.isNotEmpty()) {
                    val parts = listOfNotNull(
                        tasks.size.takeIf { it > 0 }?.let { "$it task" + if (it == 1) "" else "s" },
                        activities.size.takeIf { it > 0 }?.let { "$it activit" + if (it == 1) "y" else "ies" },
                    )
                    val names = tasks.map { it.title } + activities.map { it.name }
                    val lines = mutableListOf("You still have ${parts.joinToString(" and ")} to complete:")
                    lines += names.take(5).map { "• $it" }
                    if (names.size > 5) lines += "+ ${names.size - 5} more"
                    show(context, ReminderScheduler.CHANNEL_SUMMARY, "summary",
                        "Not finished yet", lines.joinToString("\n"))
                }
            }
        }

        // Schedule the next daily summary, drop the alarm that just fired
        ReminderScheduler.refresh(context)
    }

    /** Whole minutes until [start], rounded so 59.9 min shows as 60 min. */
    private fun minutesUntil(start: Long, now: Long): Long = (start - now + 30_000L) / 60_000L

    private fun show(context: Context, channel: String, tag: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ReminderScheduler.createChannels(context)

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        NotificationManagerCompat.from(context).notify(tag, 0, notification)
    }
}
