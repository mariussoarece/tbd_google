package ro.vishrama.tobedone.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.ui.EmptyHint
import ro.vishrama.tobedone.ui.FormCard
import ro.vishrama.tobedone.ui.RowCard
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.SwipeToDelete
import ro.vishrama.tobedone.ui.TextInput
import ro.vishrama.tobedone.ui.formatPrice
import ro.vishrama.tobedone.ui.parsePrice

@Composable
fun MonthlyTasksScreen(store: AppStore, padding: PaddingValues) {
    val data by store.data.collectAsStateWithLifecycle()

    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }

    fun add() {
        if (title.isBlank()) return
        store.addMonthlyTask(title, parsePrice(price) ?: 0.0)
        title = ""
        price = ""
    }

    val tasks = data.monthlyTasks
    val total = tasks.sumOf { it.price }

    ScreenList(padding) {
        item { SectionHeader("Add Monthly Task") }
        item {
            FormCard {
                TextInput(title, { title = it }, "Task Title")
                TextInput(price, { price = it }, "Price", decimal = true, onDone = ::add)
                Button(onClick = ::add, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Add Task")
                }
            }
        }

        item { SectionHeader("Monthly Tasks") }
        if (tasks.isEmpty()) item { EmptyHint("No monthly tasks yet. Swipe left on a task to delete it.") }
        items(tasks, key = { it.id }) { task ->
            SwipeToDelete(onDelete = { store.deleteMonthlyTask(task.id) }) {
                RowCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(task.title, modifier = Modifier.weight(1f))
                        Text(formatPrice(task.price), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { SectionHeader("Total") }
        item {
            FormCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total:", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(formatPrice(total), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
