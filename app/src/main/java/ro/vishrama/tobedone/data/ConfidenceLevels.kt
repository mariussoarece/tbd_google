package ro.vishrama.tobedone.data

import java.time.LocalDate

/**
 * The Confidence level of each day: the average of that day's To Do + Monthly Expenses +
 * Daily Routine score (done ÷ total × 10) and every rating you saved yourself that day.
 * Days after today never count.
 */
object ConfidenceLevels {

    fun isDerived(e: ConfidenceEntry) = "activities" in e.tags

    /** Level per day, oldest first, up to and including today. */
    fun daily(entries: List<ConfidenceEntry>, today: LocalDate = LocalDate.now(AppStore.zone())): List<Pair<LocalDate, Double>> =
        entries.groupBy { AppStore.dayOf(it.date) }
            .filterKeys { !it.isAfter(today) }
            .map { (day, list) -> day to list.map { it.score }.average() }
            .sortedBy { it.first }

    /** Average daily level over the last [days] days (today included), or null with no levels. */
    fun average(entries: List<ConfidenceEntry>, days: Long, today: LocalDate = LocalDate.now(AppStore.zone())): Double? {
        val start = today.minusDays(days - 1)
        val vals = daily(entries, today).filter { !it.first.isBefore(start) }.map { it.second }
        return if (vals.isEmpty()) null else vals.average()
    }

    /** Today's level, or null when today has nothing yet. */
    fun today(entries: List<ConfidenceEntry>, today: LocalDate = LocalDate.now(AppStore.zone())): Double? =
        daily(entries, today).lastOrNull()?.takeIf { it.first == today }?.second
}
