@file:OptIn(ExperimentalMaterial3Api::class)

package ro.vishrama.tobedone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.Priority
import ro.vishrama.tobedone.ui.EmptyHint
import ro.vishrama.tobedone.ui.FormCard
import ro.vishrama.tobedone.ui.PriorityBadge
import ro.vishrama.tobedone.ui.PriorityPicker
import ro.vishrama.tobedone.ui.RowCard
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.SwipeToDelete
import ro.vishrama.tobedone.ui.TextInput
import ro.vishrama.tobedone.ui.formatEpochDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun DueTasksScreen(store: AppStore, padding: PaddingValues) {
    val data by store.data.collectAsStateWithLifecycle()

    var title by rememberSaveable { mutableStateOf("") }
    var dueDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIUM) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    fun add() {
        if (title.isBlank()) return
        store.addDueTask(title, dueDay, priority)
        title = ""
        dueDay = LocalDate.now().toEpochDay()
        priority = Priority.MEDIUM
    }

    // Incomplete first, then by due date — same order as the iOS app.
    val tasks = data.dueTasks.sortedWith(compareBy({ it.isCompleted }, { it.dueEpochDay }))

    ScreenList(padding) {
        item { SectionHeader("Add New Task") }
        item {
            FormCard {
                TextInput(title, { title = it }, "Task Title", onDone = ::add)
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    Text("  Due Date: ${formatEpochDay(dueDay)}")
                }
                PriorityPicker(priority) { priority = it }
                Button(onClick = ::add, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Add Task")
                }
            }
        }

        item { SectionHeader("Due Tasks") }
        if (tasks.isEmpty()) item { EmptyHint("No tasks yet. Tap a task to mark it done; swipe left to delete.") }
        items(tasks, key = { it.id }) { task ->
            SwipeToDelete(onDelete = { store.deleteDueTask(task.id) }) {
                RowCard(Modifier.clickable { store.toggleDueTask(task.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                task.title,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.outline
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Due: ${formatEpochDay(task.dueEpochDay)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        PriorityBadge(task.priority)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        // DatePicker works in UTC milliseconds.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(dueDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dueDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}
