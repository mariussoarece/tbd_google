package ro.vishrama.tobedone.data

import java.time.LocalDate
import java.time.LocalTime

enum class AgendaKind { TASK, MONTHLY, ACTIVITY }

/**
 * One thing happening on a given day: a To Do task, a Monthly Expense or a
 * Daily Routine activity. Used by the Today card, the weekly view and the widget.
 */
data class AgendaItem(
    val kind: AgendaKind,
    val id: String,
    val title: String,
    val start: Long,               // epoch milliseconds
    val durationMinutes: Int = 0,  // only activities have a duration
    val done: Boolean = false,
    val price: Double? = null,     // monthly expenses
) {
    val time: LocalTime get() = AppStore.timeOf(start)
}

object Agenda {

    /** Everything scheduled on [day], ordered by time. */
    fun forDay(data: AppData, day: LocalDate): List<AgendaItem> {
        val zone = AppStore.zone()
        val items = mutableListOf<AgendaItem>()

        data.tasks.filter { AppStore.dayOf(it.dueAt) == day }.forEach {
            items += AgendaItem(AgendaKind.TASK, it.id, it.title, it.dueAt, done = it.isCompleted)
        }

        data.monthlyTasks.filter { it.dayOfMonth == day.dayOfMonth }.forEach {
            val start = day.atTime(it.hour, it.minute).atZone(zone).toInstant().toEpochMilli()
            items += AgendaItem(AgendaKind.MONTHLY, it.id, it.title, start, price = it.price)
        }

        data.activities.filter { AppStore.dayOf(it.date) == day }.forEach {
            items += AgendaItem(
                AgendaKind.ACTIVITY, it.id, it.name, it.date,
                durationMinutes = it.durationMinutes, done = it.isCompleted,
            )
        }

        return items.sortedBy { it.start }
    }

    /**
     * Today's items from the start of the current hour onwards (used by the widget).
     * Activities that started earlier but are still going on are kept too.
     */
    fun fromCurrentHour(data: AppData, now: Long = System.currentTimeMillis()): List<AgendaItem> {
        val zone = AppStore.zone()
        val nowTime = java.time.Instant.ofEpochMilli(now).atZone(zone)
        val hourStart = nowTime.truncatedTo(java.time.temporal.ChronoUnit.HOURS).toInstant().toEpochMilli()
        return forDay(data, nowTime.toLocalDate()).filter {
            it.start >= hourStart || it.start + it.durationMinutes * 60_000L > now
        }
    }
}
