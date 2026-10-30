package ro.vishrama.tobedone.data

import java.util.UUID

/** iOS UUIDs are upper-case strings; using the same keeps export files compatible. */
fun newId(): String = UUID.randomUUID().toString().uppercase()

enum class Priority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    companion object {
        /** Accepts "HIGH" (Android) and "High" (iOS export). */
        fun parse(value: String?): Priority =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}

/** To Do task (iOS TaskModel). [dueAt] = epoch milliseconds, date and time. */
data class DueTask(
    val id: String = newId(),
    val title: String,
    val dueAt: Long,
    val priority: Priority,
    val isCompleted: Boolean = false,
)

/** iOS ShoppingItemModel */
data class ShoppingItem(
    val id: String = newId(),
    val name: String,
    val price: Double,
    val currency: String,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
)

/** Monthly Expenses (iOS MonthlyTaskModel) */
data class MonthlyTask(
    val id: String = newId(),
    val title: String,
    val price: Double = 0.0,
)

/** iOS GroceryItemModel */
data class GroceryItem(
    val id: String = newId(),
    val name: String,
    val isBought: Boolean = false,
)

/** iOS SubtaskModel */
data class Subtask(
    val id: String = newId(),
    val title: String,
    val isCompleted: Boolean = false,
)

/** Projects (iOS OtherTaskModel) */
data class Project(
    val id: String = newId(),
    val title: String,
    val subtasks: List<Subtask> = emptyList(),
    val isCompleted: Boolean = false,
)

/** Daily Routine activity (iOS ActivityItem). [date] = start, epoch milliseconds. */
data class Activity(
    val id: String = newId(),
    val date: Long,
    val name: String,
    val details: String = "",
    val durationMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val seriesId: String? = null,
    val repeatsWeekly: Boolean = false,
    val repeatWeeks: Int = 0,
) {
    val endMillis: Long get() = date + durationMinutes * 60_000L

    fun overlaps(other: Activity): Boolean = date < other.endMillis && other.date < endMillis
}

/** iOS ConfidenceEntry. Entries tagged "activities" are derived from the Daily Routine. */
data class ConfidenceEntry(
    val id: String = newId(),
    val date: Long,
    val score: Int,
    val note: String? = null,
    val tags: List<String> = emptyList(),
)

/** iOS FocusResult. [duration] in seconds. */
data class FocusResult(
    val id: String = newId(),
    val date: Long,
    val duration: Double,
)

data class AppData(
    val tasks: List<DueTask> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val monthlyTasks: List<MonthlyTask> = emptyList(),
    val groceryItems: List<GroceryItem> = emptyList(),
    val projects: List<Project> = emptyList(),
    val activities: List<Activity> = emptyList(),
    val confidenceEntries: List<ConfidenceEntry> = emptyList(),
    val focusResults: List<FocusResult> = emptyList(),
)

enum class ThemeSetting(val label: String) {
    DEFAULT("Default"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun parse(name: String?): ThemeSetting = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

data class Settings(
    val theme: ThemeSetting = ThemeSetting.DEFAULT,
    val currency: String = "USD",
    val userName: String = "",
    val hasSeenIntro: Boolean = false,
    val dailySummaryEnabled: Boolean = true,
    val activityStartEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
)

/** Same list as the iOS Settings screen. */
val CURRENCIES = listOf(
    "USD", "EUR", "GBP", "RON", "JPY", "CNY", "INR", "AUD", "CAD", "CHF",
    "SEK", "NOK", "DKK", "NZD", "ZAR", "RUB", "BRL", "MXN", "SGD", "HKD",
    "TRY", "PLN", "HUF", "CZK", "ARS", "KRW", "THB", "AED", "SAR", "EGP",
    "NGN", "MYR", "IDR", "VND", "ILS", "BGN", "UAH", "PKR",
)
