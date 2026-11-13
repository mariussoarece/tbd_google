package ro.vishrama.tobedone.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import ro.vishrama.tobedone.ui.AddedConfirmation
import ro.vishrama.tobedone.ui.StartEndRows
import ro.vishrama.tobedone.ui.dateRangeText
import ro.vishrama.tobedone.ui.defaultEnd
import ro.vishrama.tobedone.ui.defaultStart
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
    var hasStart by rememberSaveable { mutableStateOf(false) }
    var start by rememberSaveable { mutableStateOf(defaultStart()) }
    var hasEnd by rememberSaveable { mutableStateOf(false) }
    var end by rememberSaveable { mutableStateOf(defaultEnd()) }

    var editingProject by rememberSaveable { mutableStateOf<String?>(null) }
    var editingSubtask by rememberSaveable { mutableStateOf<String?>(null) }      // subtask id
    var newSubtaskFor by rememberSaveable { mutableStateOf<String?>(null) }      // project id
    var newSubtaskTitle by rememberSaveable { mutableStateOf("") }

    var toast by remember { mutableStateOf("") }
    var toastCount by remember { mutableIntStateOf(0) }
    fun say(text: String) { toast = text; toastCount++ }

    fun add() {
        if (title.isBlank()) return
        store.addProject(title, if (hasStart) start else null, if (hasEnd) end else null)
        title = ""
        hasStart = false
        hasEnd = false
        start = defaultStart()
        end = defaultEnd()
        say("Project added")
    }

    val sorted = data.projects.sortedBy { it.title.lowercase() }
    val active = sorted.filter { !it.isCompleted }
    val completed = sorted.filter { it.isCompleted }

    AddedConfirmation(toast, toastCount)

    DetailScreen("Project Tasks", IOS.Green, onBack) {
        item { SectionHeader("Add New Project") }
        item {
            GroupCard {
                FieldRow(title, { title = it }, "Project Title", onDone = ::add)
                RowDivider()
                // Optional start and end, with a notification at each
                StartEndRows(
                    hasStart, { hasStart = it }, start, { start = it },
                    hasEnd, { hasEnd = it }, end, { end = it },
                )
                RowDivider()
                AddButtonRow("Add Project", enabled = title.isNotBlank(), onClick = ::add)
            }
        }

        item { SectionHeader("Projects – tap to complete, hold to edit") }
        groupedList(active, { it.id }, emptyText = "No projects yet. Tap the arrow to see and add subtasks.") { p ->
            ProjectRow(
                store, p,
                onEditProject = { editingProject = p.id },
                onEditSubtask = { editingSubtask = it },
                onNewSubtask = { t -> newSubtaskFor = p.id; newSubtaskTitle = t },
            )
        }

        item { ExportImportButtons(io, "Projects") }

        item { SectionHeader("Completed Tasks") }
        groupedList(completed, { it.id }, emptyText = "Swipe left on a completed project to delete it.", onDelete = { store.deleteProject(it.id) }) { p ->
            ProjectRow(
                store, p,
                onEditProject = { editingProject = p.id },
                onEditSubtask = { editingSubtask = it },
                onNewSubtask = { t -> newSubtaskFor = p.id; newSubtaskTitle = t },
            )
        }
    }

    // ---- Edit project ----
    data.projects.firstOrNull { it.id == editingProject }?.let { p ->
        DatesDialog(
            key = p.id, heading = "Edit Project", label = "Project Title",
            initialTitle = p.title, initialStart = p.startDate, initialEnd = p.endDate,
            footer = "You get a notification when the project starts and when it should be finished.",
            onDismiss = { editingProject = null },
        ) { t, st, en ->
            store.editProject(p.id, t, st, en)
            editingProject = null
            say("Project saved")
        }
    }

    // ---- Edit subtask ----
    val sub = data.projects.flatMap { pr -> pr.subtasks.map { pr to it } }.firstOrNull { it.second.id == editingSubtask }
    if (sub != null) {
        val (pr, st) = sub
        DatesDialog(
            key = st.id, heading = "Edit Subtask", label = "Subtask",
            initialTitle = st.title, initialStart = st.startDate, initialEnd = st.endDate,
            footer = "While the subtask is open, you get a notification when it starts and when it should be finished.",
            onDismiss = { editingSubtask = null },
        ) { t, s1, e1 ->
            store.editSubtask(pr.id, st.id, t, s1, e1)
            editingSubtask = null
            say("Subtask saved")
        }
    }

    // ---- New subtask (title typed in the project, plus optional dates) ----
    data.projects.firstOrNull { it.id == newSubtaskFor }?.let { p ->
        DatesDialog(
            key = "new-" + p.id, heading = "New Subtask", label = "Subtask",
            initialTitle = newSubtaskTitle, initialStart = null, initialEnd = null,
            footer = "Optional. While the subtask is open, you get a notification when it starts and when it should be finished.",
            saveLabel = "Add",
            onDismiss = { newSubtaskFor = null },
        ) { t, s1, e1 ->
            store.addSubtask(p.id, t, s1, e1)
            newSubtaskFor = null
            say("Subtask added")
        }
    }
}

/** Title + "Start date" / "End date" (Edit Project, Edit Subtask, New Subtask). */
@Composable
private fun DatesDialog(
    key: String,
    heading: String,
    label: String,
    initialTitle: String,
    initialStart: Long?,
    initialEnd: Long?,
    footer: String,
    saveLabel: String = "Save",
    onDismiss: () -> Unit,
    onSave: (String, Long?, Long?) -> Unit,
) {
    var title by rememberSaveable(key) { mutableStateOf(initialTitle) }
    var hasStart by rememberSaveable(key) { mutableStateOf(initialStart != null) }
    var start by rememberSaveable(key) { mutableStateOf(initialStart ?: defaultStart()) }
    var hasEnd by rememberSaveable(key) { mutableStateOf(initialEnd != null) }
    var end by rememberSaveable(key) { mutableStateOf(initialEnd ?: defaultEnd()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                GroupCard {
                    FieldRow(title, { title = it }, label)
                    RowDivider()
                    StartEndRows(
                        hasStart, { hasStart = it }, start, { start = it },
                        hasEnd, { hasEnd = it }, end, { end = it },
                    )
                }
                Text(footer, fontSize = 13.sp, color = LocalGrouped.current.secondaryText, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onSave(title, if (hasStart) start else null, if (hasEnd) end else null)
            }) { Text(saveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Android version of SubtaskSectionView: tap the project to complete it, hold to edit,
 * the arrow shows the subtasks. Subtasks: tap to tick off, hold to edit.
 */
@Composable
private fun ProjectRow(
    store: AppStore,
    p: Project,
    onEditProject: () -> Unit,
    onEditSubtask: (String) -> Unit,
    onNewSubtask: (String) -> Unit,
) {
    val g = LocalGrouped.current
    var expanded by rememberSaveable(p.id) { mutableStateOf(false) }
    var newSubtask by rememberSaveable(p.id) { mutableStateOf("") }

    fun addSubtask() {
        if (newSubtask.isBlank()) return
        onNewSubtask(newSubtask.trim())   // opens "New Subtask" to set dates
        newSubtask = ""
    }

    Column(Modifier.fillMaxWidth().background(g.card)) {
        GroupRow(onClick = { store.toggleProject(p.id) }, onLongClick = onEditProject) {
            CheckCircle(p.isCompleted) { store.toggleProject(p.id) }
            Column(Modifier.weight(1f)) {
                Text(
                    p.title, fontSize = 17.sp,
                    textDecoration = if (p.isCompleted) TextDecoration.LineThrough else null,
                    color = if (p.isCompleted) g.secondaryText else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                )
                dateRangeText(p.startDate, p.endDate)?.let {
                    Text("📅 $it", fontSize = 12.sp, color = g.secondaryText)
                }
                if (p.subtasks.isNotEmpty()) {
                    val done = p.subtasks.count { it.isCompleted }
                    Text("$done of ${p.subtasks.size} subtasks done", fontSize = 12.sp, color = g.secondaryText)
                }
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "Hide subtasks" else "Show subtasks",
                    tint = IOS.Blue,
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 34.dp)) {
                p.subtasks.forEach { s ->
                    GroupRow(onClick = { store.toggleSubtask(p.id, s.id) }, onLongClick = { onEditSubtask(s.id) }) {
                        CheckCircle(s.isCompleted) { store.toggleSubtask(p.id, s.id) }
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.title, fontSize = 16.sp,
                                textDecoration = if (s.isCompleted) TextDecoration.LineThrough else null,
                            )
                            dateRangeText(s.startDate, s.endDate)?.let {
                                Text(it, fontSize = 12.sp, color = g.secondaryText)
                            }
                        }
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
