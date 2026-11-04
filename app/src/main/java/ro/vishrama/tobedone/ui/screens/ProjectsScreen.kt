package ro.vishrama.tobedone.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.ExportImport
import ro.vishrama.tobedone.data.Project
import ro.vishrama.tobedone.ui.AddButtonRow
import ro.vishrama.tobedone.ui.CheckCircle
import ro.vishrama.tobedone.ui.DetailScreen
import ro.vishrama.tobedone.ui.ExportImportButtons
import ro.vishrama.tobedone.ui.FieldRow
import ro.vishrama.tobedone.ui.GroupCard
import ro.vishrama.tobedone.ui.GroupRow
import ro.vishrama.tobedone.ui.ProminentButton
import ro.vishrama.tobedone.ui.RowDivider
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.groupedList
import ro.vishrama.tobedone.ui.rememberExportImport
import ro.vishrama.tobedone.ui.theme.IOS
import ro.vishrama.tobedone.ui.theme.LocalGrouped

/** Projects (iOS OtherTasksView): projects with subtasks. */
@Composable
fun ProjectsScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    val io = rememberExportImport(store, ExportImport.Kind.PROJECTS)
    var title by rememberSaveable { mutableStateOf("") }

    fun add() {
        if (title.isBlank()) return
        store.addProject(title)
        title = ""
    }

    val sorted = data.projects.sortedBy { it.title.lowercase() }
    val active = sorted.filter { !it.isCompleted }
    val completed = sorted.filter { it.isCompleted }

    DetailScreen("Project Tasks", IOS.Green, onBack) {
        item { SectionHeader("Add New Project") }
        item {
            GroupCard {
                FieldRow(title, { title = it }, "Project Title", onDone = ::add)
                RowDivider()
                AddButtonRow("Add Project", enabled = title.isNotBlank(), onClick = ::add)
            }
        }

        item { SectionHeader("Projects with Subtasks") }
        groupedList(active, { it.id }, emptyText = "No projects yet. Tap the circle to complete a project, tap the row to see its subtasks.") { p ->
            ProjectRow(store, p)
        }

        item { ExportImportButtons(io, "Projects") }

        item { SectionHeader("Completed Projects") }
        groupedList(completed, { it.id }, emptyText = "Swipe left on a completed project to delete it.", onDelete = { store.deleteProject(it.id) }) { p ->
            ProjectRow(store, p)
        }
    }
}

/** Android version of SubtaskSectionView (a SwiftUI DisclosureGroup). */
@Composable
private fun ProjectRow(store: AppStore, p: Project) {
    val g = LocalGrouped.current
    var expanded by rememberSaveable(p.id) { mutableStateOf(false) }
    var newSubtask by rememberSaveable(p.id) { mutableStateOf("") }

    fun addSubtask() {
        if (newSubtask.isBlank()) return
        store.addSubtask(p.id, newSubtask)
        newSubtask = ""
    }

    Column(Modifier.fillMaxWidth().background(g.card)) {
        GroupRow(onClick = { expanded = !expanded }) {
            CheckCircle(p.isCompleted) { store.toggleProject(p.id) }
            Column(Modifier.weight(1f)) {
                Text(
                    p.title, fontSize = 17.sp,
                    textDecoration = if (p.isCompleted) TextDecoration.LineThrough else null,
                    color = if (p.isCompleted) g.secondaryText else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                )
                if (p.subtasks.isNotEmpty()) {
                    val done = p.subtasks.count { it.isCompleted }
                    Text("$done of ${p.subtasks.size} subtasks done", fontSize = 12.sp, color = g.secondaryText)
                }
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (expanded) "Hide subtasks" else "Show subtasks",
                tint = IOS.Blue,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 34.dp)) {
                p.subtasks.forEach { s ->
                    GroupRow(onClick = { store.toggleSubtask(p.id, s.id) }) {
                        CheckCircle(s.isCompleted) { store.toggleSubtask(p.id, s.id) }
                        Text(
                            s.title, fontSize = 16.sp,
                            textDecoration = if (s.isCompleted) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        FieldRow(newSubtask, { newSubtask = it }, "New Subtask", onDone = ::addSubtask)
                    }
                    ProminentButton("Add", enabled = newSubtask.isNotBlank(), onClick = ::addSubtask)
                }
            }
        }
    }
}
