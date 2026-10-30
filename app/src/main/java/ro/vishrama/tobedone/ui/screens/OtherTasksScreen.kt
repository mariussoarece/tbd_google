package ro.vishrama.tobedone.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.OtherTask
import ro.vishrama.tobedone.ui.EmptyHint
import ro.vishrama.tobedone.ui.FormCard
import ro.vishrama.tobedone.ui.RowCard
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.SwipeToDelete
import ro.vishrama.tobedone.ui.TextInput
import ro.vishrama.tobedone.ui.theme.DoneGreen

@Composable
fun OtherTasksScreen(store: AppStore, padding: PaddingValues) {
    val data by store.data.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }

    fun add() {
        if (title.isBlank()) return
        store.addOtherTask(title)
        title = ""
    }

    // Incomplete first, then alphabetical — same as iOS.
    val tasks = data.otherTasks.sortedWith(compareBy({ it.isCompleted }, { it.title.lowercase() }))

    ScreenList(padding) {
        item { SectionHeader("Add New Task") }
        item {
            FormCard {
                TextInput(title, { title = it }, "Task Title", onDone = ::add)
                Button(onClick = ::add, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Add Task")
                }
            }
        }

        item { SectionHeader("Tasks with Subtasks") }
        if (tasks.isEmpty()) item { EmptyHint("No tasks yet. Tap the circle to complete a task, tap the arrow to see and add subtasks.") }
        items(tasks, key = { it.id }) { task ->
            SwipeToDelete(onDelete = { store.deleteOtherTask(task.id) }) {
                OtherTaskRow(
                    task = task,
                    onToggle = { store.toggleOtherTask(task.id) },
                    onAddSubtask = { store.addSubtask(task.id, it) },
                )
            }
        }
    }
}

/** Android version of SubtaskSectionView (a DisclosureGroup). */
@Composable
private fun OtherTaskRow(task: OtherTask, onToggle: () -> Unit, onAddSubtask: (String) -> Unit) {
    var expanded by rememberSaveable(task.id) { mutableStateOf(false) }
    var newSubtask by rememberSaveable(task.id) { mutableStateOf("") }

    fun addSubtask() {
        if (newSubtask.isBlank()) return
        onAddSubtask(newSubtask)
        newSubtask = ""
    }

    RowCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggle) {
                    if (task.isCompleted) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Mark as not done", tint = DoneGreen)
                    } else {
                        Icon(Icons.Outlined.Circle, contentDescription = "Mark as done", tint = MaterialTheme.colorScheme.outline)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        task.title,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    if (task.subtasks.isNotEmpty()) {
                        Text(
                            "${task.subtasks.size} subtask" + if (task.subtasks.size == 1) "" else "s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Hide subtasks" else "Show subtasks",
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    task.subtasks.forEach { sub ->
                        Text("- ${sub.title}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSubtask,
                            onValueChange = { newSubtask = it },
                            label = { Text("New Subtask") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addSubtask() }),
                        )
                        TextButton(onClick = ::addSubtask, enabled = newSubtask.isNotBlank()) { Text("Add") }
                    }
                }
            }
        }
    }
}
