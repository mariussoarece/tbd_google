package ro.vishrama.tobedone.ui.screens

import ro.vishrama.tobedone.ui.SectionFooter
import ro.vishrama.tobedone.ui.AddedConfirmation
import ro.vishrama.tobedone.ui.EditDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.Activity
import ro.vishrama.tobedone.data.Agenda
import ro.vishrama.tobedone.data.AgendaItem
import ro.vishrama.tobedone.data.AgendaKind
import ro.vishrama.tobedone.data.AppData
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.ExportImport
import ro.vishrama.tobedone.data.describeMinutes
import ro.vishrama.tobedone.data.newId
import ro.vishrama.tobedone.ui.CheckCircle
import ro.vishrama.tobedone.ui.DateTimeRow
import ro.vishrama.tobedone.ui.DetailScreen
import ro.vishrama.tobedone.ui.ExportImportButtons
import ro.vishrama.tobedone.ui.FieldRow
import ro.vishrama.tobedone.ui.GroupCard
import ro.vishrama.tobedone.ui.GroupRow
import ro.vishrama.tobedone.ui.ProminentButton
import ro.vishrama.tobedone.ui.ReminderRows
import ro.vishrama.tobedone.ui.RowDivider
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.Segmented
import ro.vishrama.tobedone.ui.Stepper
import ro.vishrama.tobedone.ui.formatDateTime
import ro.vishrama.tobedone.ui.formatTime
import ro.vishrama.tobedone.ui.money
import ro.vishrama.tobedone.ui.groupedList
import ro.vishrama.tobedone.ui.rememberExportImport
import ro.vishrama.tobedone.ui.theme.IOS
import ro.vishrama.tobedone.ui.theme.LocalGrouped
import ro.vishrama.tobedone.ui.toLocalDateTime
import ro.vishrama.tobedone.ui.toMillis
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

private enum class RoutineView(val label: String) { MONTHLY("Monthly"), WEEKLY("Weekly") }

private fun firstDayOfWeek(): DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
private fun weekStartOf(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(firstDayOfWeek()))

/** Score 0…10 for a day: done / total × 10 (null when the day has no activities). */
private fun scoreFor(items: List<Activity>): Int? =
    if (items.isEmpty()) null else (items.count { it.isCompleted }.toDouble() / items.size * 10).roundToInt()

/** Daily Routine (iOS DailyActivitiesView). */
@Composable
fun DailyRoutineScreen(store: AppStore, onBack: () -> Unit, startWeekly: Boolean = false) {
    val data by store.data.collectAsStateWithLifecycle()
    val io = rememberExportImport(store, ExportImport.Kind.ACTIVITIES)
    val g = LocalGrouped.current

    val today = LocalDate.now(AppStore.zone())
    var view by rememberSaveable { mutableStateOf(if (startWeekly) RoutineView.WEEKLY else RoutineView.MONTHLY) }
    var month by rememberSaveable { mutableStateOf(today.withDayOfMonth(1).toEpochDay()) }
    var selected by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var weekStart by rememberSaveable { mutableStateOf(weekStartOf(today).toEpochDay()) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }   // "hold to edit"
    var toast by remember { mutableStateOf("") }
    var toastCount by remember { mutableIntStateOf(0) }
    fun say(text: String) { toast = text; toastCount++ }
    var markerKey by rememberSaveable { mutableStateOf<String?>(null) }   // "TASK:id" or "MONTHLY:id"

    val monthDate = LocalDate.ofEpochDay(month)
    val selectedDate = LocalDate.ofEpochDay(selected)
    val byDay = data.activities.groupBy { AppStore.dayOf(it.date) }
    val dayItems = (byDay[selectedDate] ?: emptyList()).sortedBy { it.date }

    DetailScreen("Daily Activities", IOS.Indigo, onBack) {
        item {
            Segmented(RoutineView.entries, view, { it.label }, Modifier.padding(vertical = 8.dp)) { view = it }
        }

        if (view == RoutineView.MONTHLY) {
            // Month header
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = monthDate.minusMonths(1).toEpochDay() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", tint = IOS.Blue)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${monthDate.year}", fontSize = 12.sp, color = g.secondaryText)
                        Text(
                            monthDate.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                                .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                            fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = { month = monthDate.plusMonths(1).toEpochDay() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", tint = IOS.Blue)
                    }
                }
            }
            item {
                MonthGrid(
                    month = YearMonth.from(monthDate),
                    selected = selectedDate,
                    today = today,
                    scores = byDay.mapValues { scoreFor(it.value) },
                    onSelect = { selected = it.toEpochDay() },
                )
            }

            // Selected day
            item {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " · " +
                            selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                    )
                    scoreFor(dayItems)?.let { Text("♥ $it/10", fontSize = 15.sp) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    ProminentButton("Add Activity") { showAdd = true }
                }
            }
            groupedList(dayItems, { it.id }, emptyText = "No activities. Add one with the Add Activity button.", onDelete = { store.deleteActivity(it.id) }) { a ->
                ActivityRow(a, onToggle = { store.toggleActivity(a.id) }, onEdit = { editId = a.id })
            }
            if (dayItems.isNotEmpty()) item { SectionFooter("Tap to tick off, hold to edit, swipe left to delete.") }
            item { ExportImportButtons(io, "Activities") }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    ProminentButton("Add Activity") {
                        val ws = LocalDate.ofEpochDay(weekStart)
                        selected = (if (ws.isAfter(today)) ws else today).toEpochDay()
                        showAdd = true
                    }
                }
            }
            item {
                val ws = LocalDate.ofEpochDay(weekStart)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { weekStart = ws.minusWeeks(1).toEpochDay() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week", tint = IOS.Blue)
                    }
                    Text(
                        ws.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { weekStart = ws.plusWeeks(1).toEpochDay() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week", tint = IOS.Blue)
                    }
                }
            }
            item {
                WeeklyTimeline(LocalDate.ofEpochDay(weekStart), byDay, data, onTap = { detailId = it.id }, onMarkerTap = { markerKey = it.kind.name + ":" + it.id })
            }
        }
    }

    if (showAdd) {
        AddActivityDialog(
            defaultDay = selectedDate,
            existing = data.activities,
            onDismiss = { showAdd = false },
            onSave = {
                store.addActivities(it)
                showAdd = false
                say(if (it.size == 1) "Activity added" else "${it.size} activities added")
            },
        )
    }

    // Tapped a To Do / Monthly line in the weekly view
    markerKey?.let { key ->
        val (kind, id) = key.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val task = if (kind == AgendaKind.TASK.name) data.tasks.firstOrNull { it.id == id } else null
        val monthly = if (kind == AgendaKind.MONTHLY.name) data.monthlyTasks.firstOrNull { it.id == id } else null
        if (task != null || monthly != null) {
            AlertDialog(
                onDismissRequest = { markerKey = null },
                title = { Text(task?.title ?: monthly!!.title) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (task != null) {
                            Text("To Do · due ${formatDateTime(task.dueAt)}")
                            Text("Priority: ${task.priority.label}", color = g.secondaryText)
                        } else if (monthly != null) {
                            Text("Monthly expense · every month on day ${monthly.dayOfMonth} at ${formatTime(LocalTime.of(monthly.hour, monthly.minute))}")
                            if (monthly.price > 0) Text(money(monthly.price), color = g.secondaryText)
                        }
                    }
                },
                confirmButton = {
                    if (task != null) {
                        TextButton(onClick = { store.toggleTask(task.id); markerKey = null }) {
                            Text(if (task.isCompleted) "Mark not done" else "Mark done")
                        }
                    } else {
                        TextButton(onClick = { markerKey = null }) { Text("OK") }
                    }
                },
                dismissButton = {
                    if (task != null) TextButton(onClick = { markerKey = null }) { Text("Close") }
                },
            )
        }
    }

    data.activities.firstOrNull { it.id == detailId }?.let { a ->
        AlertDialog(
            onDismissRequest = { detailId = null },
            title = { Text(a.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Starts at: ${formatTime(toLocalDateTime(a.date).toLocalTime())}")
                    Text("Duration: ${a.durationMinutes} minutes")
                    if (a.details.isNotBlank()) Text(a.details, color = g.secondaryText)
                }
            },
            confirmButton = {
                TextButton(onClick = { store.toggleActivity(a.id); detailId = null }) {
                    Text(if (a.isCompleted) "Mark not done" else "Mark done")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { editId = a.id; detailId = null }) { Text("Edit") }
                    TextButton(onClick = { detailId = null }) { Text("Close") }
                }
            },
        )
    }

    data.activities.firstOrNull { it.id == editId }?.let { a ->
        EditActivityDialog(
            a,
            repeats = store.seriesSize(a.id) > 1,
            onDismiss = { editId = null },
        ) { scope, name, details, start, duration, reminder ->
            val n = store.editActivity(a.id, scope, name, details, start, duration, reminder)
            editId = null
            say(if (n == 1) "Activity saved" else "$n activities saved")
        }
    }

    AddedConfirmation(toast, toastCount)
}

/**
 * Edits one activity. For a repeating activity, Save asks: only this one,
 * this and the following ones, or all in the series (same as iOS).
 */
@Composable
private fun EditActivityDialog(
    a: Activity,
    repeats: Boolean,
    onDismiss: () -> Unit,
    onSave: (AppStore.EditScope, String, String, Long, Int, Int?) -> Unit,
) {
    var name by rememberSaveable(a.id) { mutableStateOf(a.name) }
    var details by rememberSaveable(a.id) { mutableStateOf(a.details) }
    var date by rememberSaveable(a.id) { mutableStateOf(a.date) }
    var duration by rememberSaveable(a.id) { mutableIntStateOf(a.durationMinutes) }
    var remind by rememberSaveable(a.id) { mutableStateOf((a.reminderMinutes ?: 0) > 0) }
    var remindBefore by rememberSaveable(a.id) { mutableIntStateOf(a.reminderMinutes ?: 30) }
    var askScope by rememberSaveable(a.id) { mutableStateOf(false) }
    val reminder = if (remind && remindBefore > 0) remindBefore else null

    EditDialog(
        "Edit Activity",
        canSave = name.isNotBlank() && (!remind || remindBefore > 0),
        onDismiss = onDismiss,
        onSave = {
            if (repeats) {
                askScope = true
            } else {
                onSave(AppStore.EditScope.THIS_ONE, name, details, date, duration, reminder)
            }
        },
        footer = if (repeats) "This activity repeats. When you save, you can change only this day, this and the following days, or all of them." else null,
    ) {
        FieldRow(name, { name = it }, "Name")
        RowDivider()
        FieldRow(details, { details = it }, "Description", singleLine = false)
        RowDivider()
        DateTimeRow("Date", toLocalDateTime(date), { date = toMillis(it) })
        RowDivider()
        GroupRow {
            Text("Duration", fontSize = 17.sp, modifier = Modifier.weight(1f))
            Text("$duration min", fontSize = 17.sp)
            Stepper(onMinus = { duration = (duration - 5).coerceAtLeast(0) }, onPlus = { duration = (duration + 5).coerceAtMost(480) })
        }
        RowDivider()
        ReminderRows(remind, { remind = it }, toLocalDateTime(date), remindBefore) { remindBefore = it }
    }

    if (askScope) {
        AlertDialog(
            onDismissRequest = { askScope = false },
            title = { Text("This activity repeats") },
            text = {
                Column {
                    Text("Which days should change?")
                    Spacer(Modifier.height(12.dp))
                    ScopeOption("Only this one", "Just this day.") {
                        askScope = false; onSave(AppStore.EditScope.THIS_ONE, name, details, date, duration, reminder)
                    }
                    ScopeOption("This and following", "This day and the later ones. Past ones you already ticked off stay unchanged, so your history and Confidence scores stay accurate.") {
                        askScope = false; onSave(AppStore.EditScope.FOLLOWING, name, details, date, duration, reminder)
                    }
                    ScopeOption("All in the series", "Every one, including past ones.") {
                        askScope = false; onSave(AppStore.EditScope.ALL, name, details, date, duration, reminder)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { askScope = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ScopeOption(title: String, description: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Text(title, fontSize = 17.sp, color = IOS.Blue, fontWeight = FontWeight.SemiBold)
        Text(description, fontSize = 13.sp, color = LocalGrouped.current.secondaryText)
    }
}

@Composable
private fun ActivityRow(a: Activity, onToggle: () -> Unit, onEdit: () -> Unit) {
    val g = LocalGrouped.current
    GroupRow(onClick = onToggle, onLongClick = onEdit) {
        CheckCircle(a.isCompleted, onToggle)
        Text(formatTime(toLocalDateTime(a.date).toLocalTime()), fontSize = 12.sp, color = g.secondaryText)
        Column(Modifier.weight(1f)) {
            Text(
                a.name, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                textDecoration = if (a.isCompleted) TextDecoration.LineThrough else null,
            )
            if (a.details.isNotBlank()) Text(a.details, fontSize = 12.sp, color = g.secondaryText)
            a.reminderMinutes?.let {
                Text("🔔 ${describeMinutes(it.toLong())} before", fontSize = 12.sp, color = g.secondaryText)
            }
        }
        if (a.durationMinutes > 0) Text("${a.durationMinutes}m", fontSize = 16.sp, color = g.secondaryText)
    }
}

// ---------------------------------------------------------------------
//  Month calendar (iOS MonthCalendarGrid)
// ---------------------------------------------------------------------

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    scores: Map<LocalDate, Int?>,
    onSelect: (LocalDate) -> Unit,
) {
    val g = LocalGrouped.current
    val first = month.atDay(1)
    val gridStart = weekStartOf(first)
    val days = (0 until 42).map { gridStart.plusDays(it.toLong()) }
    val weekdays = (0 until 7).map { firstDayOfWeek().plus(it.toLong()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekdays.forEach {
                Text(
                    it.getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase(),
                    fontSize = 11.sp, color = g.secondaryText,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
            }
        }
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { day ->
                    val inMonth = YearMonth.from(day) == month
                    val isToday = day == today
                    val isSel = day == selected
                    val bg = when {
                        isSel -> if (g.isDark) Color(0xFF48484A) else Color(0xFFD1D1D6)
                        isToday -> IOS.Blue.copy(alpha = 0.18f)
                        else -> g.card
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1.1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .then(
                                if (isToday) Modifier.border(2.dp, IOS.Blue, RoundedCornerShape(10.dp))
                                else if (isSel) Modifier.border(1.5.dp, Color(0xFF8E8E93), RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .clickable { onSelect(day) },
                    ) {
                        Text(
                            "${day.dayOfMonth}",
                            fontSize = 15.sp,
                            fontWeight = if (isSel || isToday) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (inMonth) MaterialTheme.colorScheme.onSurface else g.secondaryText,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        scores[day]?.let { s ->
                            Text(
                                "$s",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(g.fill)
                                    .padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
//  Weekly timeline (iOS WeeklyTimelineView): 7 days × 24 hours
// ---------------------------------------------------------------------

private val HOUR_HEIGHT = 60.dp      // 1 dp per minute
private val DAY_WIDTH = 120.dp
private val DAY_HEADER = 20.dp

@Composable
private fun WeeklyTimeline(
    weekStart: LocalDate,
    byDay: Map<LocalDate, List<Activity>>,
    data: AppData,
    onTap: (Activity) -> Unit,
    onMarkerTap: (AgendaItem) -> Unit,
) {
    val g = LocalGrouped.current
    val density = LocalDensity.current
    // Start scrolled to 07:00, like iOS
    val vScroll = rememberScrollState(with(density) { (HOUR_HEIGHT * 7).roundToPx() })
    val hScroll = rememberScrollState()

    GroupCard {
        Box(Modifier.height(520.dp).verticalScroll(vScroll)) {
            Row(Modifier.horizontalScroll(hScroll).padding(end = 8.dp)) {
                // Hours column
                Column(Modifier.width(44.dp)) {
                    Spacer(Modifier.height(DAY_HEADER))
                    for (h in 0 until 24) {
                        Text(
                            String.format("%02d:00", h),
                            fontSize = 10.sp, color = g.secondaryText,
                            modifier = Modifier.height(HOUR_HEIGHT).padding(start = 4.dp),
                        )
                    }
                }
                for (offset in 0 until 7) {
                    val day = weekStart.plusDays(offset.toLong())
                    // To Do tasks and Monthly Expenses are drawn as thin lines
                    val markers = Agenda.forDay(data, day).filter { it.kind != AgendaKind.ACTIVITY }
                    DayColumn(day, byDay[day] ?: emptyList(), markers, onTap, onMarkerTap)
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    day: LocalDate,
    items: List<Activity>,
    markers: List<AgendaItem>,
    onTap: (Activity) -> Unit,
    onMarkerTap: (AgendaItem) -> Unit,
) {
    val g = LocalGrouped.current
    Column(Modifier.width(DAY_WIDTH)) {
        Text(
            day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + day.dayOfMonth,
            fontSize = 12.sp,
            modifier = Modifier.height(DAY_HEADER).fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Box(Modifier.height(HOUR_HEIGHT * 24).fillMaxWidth()) {
            // Hour grid; daytime (07–18) slightly shaded
            Column {
                for (h in 0 until 24) {
                    Column(
                        Modifier
                            .height(HOUR_HEIGHT)
                            .fillMaxWidth()
                            .background(if (h in 7 until 18) Color.Gray.copy(alpha = 0.15f) else Color.Transparent),
                    ) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)))
                    }
                }
            }
            // Activities; overlapping ones share the width side by side
            overlapGroups(items).forEach { group ->
                val count = group.size
                val colWidth = DAY_WIDTH / count
                group.forEachIndexed { i, a ->
                    val t = toLocalDateTime(a.date).toLocalTime()
                    val top = (t.hour * 60 + t.minute).dp
                    val height = maxOf(a.durationMinutes, 20).dp
                    val fill = when {
                        a.isCompleted -> IOS.Green.copy(alpha = 0.3f)
                        a.durationMinutes > 60 -> IOS.Red.copy(alpha = 0.3f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    }
                    Box(
                        Modifier
                            .offset(x = colWidth * i, y = top)
                            .size(width = colWidth - 4.dp, height = height)
                            .clip(RoundedCornerShape(6.dp))
                            .background(fill)
                            .border(1.dp, IOS.Purple, RoundedCornerShape(6.dp))
                            .clickable { onTap(a) }
                            .padding(4.dp),
                    ) {
                        Text(a.name, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // To Do / Monthly: a coloured line at the exact time with the title on it
            markers.forEach { m ->
                val minutes = m.time.hour * 60 + m.time.minute
                val color = kindColor(m.kind)
                Box(
                    Modifier
                        .offset(y = minutes.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (m.done) color.copy(alpha = 0.4f) else color),
                )
                Text(
                    (if (m.done) "✓ " else "") + m.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (m.done) g.secondaryText else color,
                    textDecoration = if (m.done) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .offset(y = (minutes - 15).coerceAtLeast(0).dp)
                        .padding(start = 2.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(g.card.copy(alpha = 0.85f))
                        .clickable { onMarkerTap(m) }
                        .padding(horizontal = 3.dp),
                )
            }
        }
    }
}

private fun overlapGroups(items: List<Activity>): List<List<Activity>> {
    val groups = mutableListOf<MutableList<Activity>>()
    for (item in items.sortedBy { it.date }) {
        val group = groups.firstOrNull { gr -> gr.any { it.overlaps(item) } }
        if (group != null) group.add(item) else groups.add(mutableListOf(item))
    }
    return groups
}

// ---------------------------------------------------------------------
//  New Activity (iOS AddActivitySheet)
// ---------------------------------------------------------------------

private const val MAX_DAYS = 100
private const val MAX_WEEKS = 52

/** "For [ 7 ] days  − +" with a number you can type; red message when it's too high. */
@Composable
private fun RepeatCountRow(text: String, onText: (String) -> Unit, unit: String, max: Int, error: String) {
    val g = LocalGrouped.current
    val value = text.toIntOrNull()
    GroupRow {
        Text("For", fontSize = 17.sp)
        Box(
            Modifier
                .width(64.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(g.fill)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { new -> onText(new.filter { it.isDigit() }.take(4)) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(IOS.Blue),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(unit, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Stepper(
            onMinus = { onText(((value ?: 1) - 1).coerceAtLeast(1).toString()) },
            onPlus = { onText(((value ?: 0) + 1).coerceAtMost(max).toString()) },
        )
    }
    when {
        value != null && value > max -> GroupRow { Text(error, color = IOS.Red, fontSize = 14.sp) }
        value == null || value < 1 -> GroupRow { Text("Enter a number from 1 to $max", color = IOS.Red, fontSize = 14.sp) }
    }
}

@Composable
private fun AddActivityDialog(
    defaultDay: LocalDate,
    existing: List<Activity>,
    onDismiss: () -> Unit,
    onSave: (List<Activity>) -> Unit,
) {
    val g = LocalGrouped.current
    // New activities start at 10:00 on the selected day
    var date by rememberSaveable { mutableStateOf(toMillis(defaultDay.atTime(LocalTime.of(10, 0)))) }
    var repeatWeekly by rememberSaveable { mutableStateOf(false) }
    var weeksText by rememberSaveable { mutableStateOf("4") }
    var repeatDaily by rememberSaveable { mutableStateOf(false) }
    var daysText by rememberSaveable { mutableStateOf("7") }
    val weeks = weeksText.toIntOrNull() ?: 0
    val days = daysText.toIntOrNull() ?: 0
    val weeksOk = !repeatWeekly || weeks in 1..MAX_WEEKS
    val daysOk = !repeatDaily || days in 1..MAX_DAYS
    var name by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableIntStateOf(60) }
    var remind by rememberSaveable { mutableStateOf(false) }
    var remindBefore by rememberSaveable { mutableIntStateOf(30) }   // default: 30 min before
    var pending by remember { mutableStateOf<List<Activity>?>(null) }

    fun build(): List<Activity> {
        val count = when {
            repeatWeekly -> weeks
            repeatDaily -> days
            else -> 1
        }
        val series = if (repeatWeekly || repeatDaily) newId() else null
        val start = toLocalDateTime(date)
        return (0 until count).map { n ->
            Activity(
                date = toMillis(if (repeatDaily) start.plusDays(n.toLong()) else start.plusWeeks(n.toLong())),
                name = name.trim(),
                details = details,
                durationMinutes = duration,
                seriesId = series,
                repeatsWeekly = repeatWeekly,
                repeatWeeks = if (repeatWeekly) weeks else 0,
                repeatsDaily = repeatDaily,
                repeatDays = if (repeatDaily) days else 0,
                reminderMinutes = if (remind && remindBefore > 0) remindBefore else null,
            )
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(g.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = IOS.Blue) }
                Text("New Activity", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center)
                ProminentButton("Save", enabled = name.isNotBlank() && (!remind || remindBefore > 0) && weeksOk && daysOk) {
                    val items = build()
                    // If any occurrence overlaps, ask once for the whole set
                    if (items.any { n -> existing.any { it.overlaps(n) } }) {
                        pending = items
                    } else {
                        onSave(items)
                    }
                }
            }

            SectionHeader("When")
            GroupCard {
                DateTimeRow("Date", toLocalDateTime(date), { date = toMillis(it) })
                RowDivider()
                // Repeat daily and Repeat weekly: only one of them can be on
                GroupRow {
                    Text("Repeat daily", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = repeatDaily,
                        onCheckedChange = { repeatDaily = it; if (it) repeatWeekly = false },
                        colors = SwitchDefaults.colors(checkedTrackColor = IOS.Green),
                    )
                }
                if (repeatDaily) {
                    RowDivider()
                    RepeatCountRow(daysText, { daysText = it }, unit = "days", max = MAX_DAYS,
                        error = "Number of days should be less than $MAX_DAYS")
                }
                RowDivider()
                GroupRow {
                    Text("Repeat weekly", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = repeatWeekly,
                        onCheckedChange = { repeatWeekly = it; if (it) repeatDaily = false },
                        colors = SwitchDefaults.colors(checkedTrackColor = IOS.Green),
                    )
                }
                if (repeatWeekly) {
                    RowDivider()
                    RepeatCountRow(weeksText, { weeksText = it }, unit = "weeks", max = MAX_WEEKS,
                        error = "Number of weeks should be less than $MAX_WEEKS")
                }
                RowDivider()
                ReminderRows(remind, { remind = it }, toLocalDateTime(date), remindBefore) { remindBefore = it }
            }

            SectionHeader("Activity")
            GroupCard {
                FieldRow(name, { name = it }, "Name")
                RowDivider()
                FieldRow(details, { details = it }, "Description", singleLine = false)
                RowDivider()
                GroupRow {
                    Text("Duration", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text("$duration min", fontSize = 17.sp)
                    Stepper(onMinus = { duration = (duration - 5).coerceAtLeast(0) }, onPlus = { duration = (duration + 5).coerceAtMost(480) })
                }
            }
        }
    }

    pending?.let { items ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Activity Overlaps") },
            text = { Text("This activity overlaps an existing one.") },
            confirmButton = { TextButton(onClick = { pending = null; onSave(items) }) { Text("Create Anyway") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
}
