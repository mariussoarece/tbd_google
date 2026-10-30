package ro.vishrama.tobedone.data

import java.util.UUID

enum class Priority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    companion object {
        fun fromName(name: String?): Priority =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

/** A task with a due date (iOS: TaskModel). [dueEpochDay] is a LocalDate.toEpochDay(). */
data class DueTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueEpochDay: Long,
    val priority: Priority,
    val isCompleted: Boolean = false,
)

/** iOS: ShoppingItemModel */
data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val currency: String,
    val priority: Priority,
    val isCompleted: Boolean = false,
)

/** iOS: MonthlyTaskModel */
data class MonthlyTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val price: Double = 0.0,
)

/** iOS: GroceryItemModel */
data class GroceryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isBought: Boolean = false,
)

/** iOS: SubtaskModel */
data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
)

/** iOS: OtherTaskModel */
data class OtherTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtasks: List<Subtask> = emptyList(),
    val isCompleted: Boolean = false,
)

data class AppData(
    val dueTasks: List<DueTask> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val monthlyTasks: List<MonthlyTask> = emptyList(),
    val groceryItems: List<GroceryItem> = emptyList(),
    val otherTasks: List<OtherTask> = emptyList(),
)

enum class ThemeSetting(val label: String) {
    DEFAULT("Default"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromName(name: String?): ThemeSetting =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
