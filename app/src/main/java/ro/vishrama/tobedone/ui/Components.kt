@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package ro.vishrama.tobedone.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.ExportImport
import ro.vishrama.tobedone.data.Priority
import ro.vishrama.tobedone.data.describeMinutes
import ro.vishrama.tobedone.ui.theme.IOS
import ro.vishrama.tobedone.ui.theme.LocalGrouped
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// =====================================================================
//  Screen frame: "‹ Back" bar + large coloured title (like the iOS screens)
// =====================================================================

@Composable
fun DetailScreen(
    title: String,
    titleColor: Color,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    BackHandler(onBack = onBack)
    val g = LocalGrouped.current
    Column(
        Modifier
            .fillMaxSize()
            .background(g.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        BackBar(onBack, actions)
        Text(
            title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            content = {
                content(this)
                item { Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) }
            },
        )
    }
}

@Composable
fun BackBar(onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = null, tint = IOS.Blue, modifier = Modifier.size(18.dp))
            Text("Back", color = IOS.Blue, fontSize = 17.sp)
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}

// =====================================================================
//  Grouped sections (iOS Form / insetGrouped List)
// =====================================================================

@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        fontSize = 13.sp,
        color = LocalGrouped.current.secondaryText,
        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 7.dp),
    )
}

@Composable
fun SectionFooter(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = LocalGrouped.current.secondaryText,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp),
    )
}

/** White rounded card; put [GroupRow]s and [RowDivider]s inside. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalGrouped.current.card),
        content = content,
    )
}

@Composable
fun RowDivider(startIndent: Dp = 16.dp) {
    Box(Modifier.fillMaxWidth().background(LocalGrouped.current.card)) {
        HorizontalDivider(
            modifier = Modifier.padding(start = startIndent),
            thickness = 0.5.dp,
            color = LocalGrouped.current.separator,
        )
    }
}

/** One list row with iOS spacing. */
@Composable
fun GroupRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,   // "hold to edit"
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .background(LocalGrouped.current.card)
            .then(
                when {
                    onLongClick != null -> Modifier.combinedClickable(onClick = { onClick?.invoke() }, onLongClick = onLongClick)
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    else -> Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Plain text field inside a row, with grey placeholder (iOS Form style). */
@Composable
fun FieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    decimal: Boolean = false,
    onDone: (() -> Unit)? = null,
    singleLine: Boolean = true,
) {
    GroupRow {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = LocalGrouped.current.secondaryText, fontSize = 17.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(IOS.Blue),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text,
                    imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Label on the left, value + ⌃⌄ on the right; opens a menu (iOS menu Picker). */
@Composable
fun <T> PickerRow(label: String, value: T, options: List<T>, text: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    GroupRow(onClick = { open = true }) {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text(value), color = LocalGrouped.current.secondaryText, fontSize = 17.sp)
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = LocalGrouped.current.secondaryText, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false })
                }
            }
        }
    }
}

@Composable
fun PriorityRow(value: Priority, onSelect: (Priority) -> Unit) =
    PickerRow("Priority", value, Priority.entries, { it.label }, onSelect)

/** Blue capsule button (SwiftUI .borderedProminent). */
@Composable
fun ProminentButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = IOS.Blue, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) color else LocalGrouped.current.fill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) Color.White else LocalGrouped.current.secondaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** Tinted capsule button (SwiftUI .bordered). */
@Composable
fun BorderedButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(IOS.Blue.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = IOS.Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** Button aligned to the right inside a card row, like the iOS "Add" buttons. */
@Composable
fun AddButtonRow(text: String, enabled: Boolean, onClick: () -> Unit) {
    GroupRow {
        Spacer(Modifier.weight(1f))
        ProminentButton(text, enabled = enabled, onClick = onClick)
    }
}

/** iOS segmented control. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, text: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    val g = LocalGrouped.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(g.fill)
            .padding(2.dp),
    ) {
        options.forEach { o ->
            val isSel = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSel) (if (g.isDark) Color(0xFF636366) else Color.White) else Color.Transparent)
                    .clickable { onSelect(o) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text(o), fontSize = 14.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** Coloured rounded-square icon (Home screen rows). */
@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 30.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(7.dp)).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

/** Round "−  value  +" stepper. */
@Composable
fun Stepper(onMinus: () -> Unit, onPlus: () -> Unit) {
    val g = LocalGrouped.current
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(g.fill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("−", fontSize = 22.sp, modifier = Modifier.clickable(onClick = onMinus).padding(horizontal = 14.dp, vertical = 2.dp))
        Box(Modifier.width(0.5.dp).heightIn(min = 18.dp).background(g.separator))
        Text("+", fontSize = 22.sp, modifier = Modifier.clickable(onClick = onPlus).padding(horizontal = 14.dp, vertical = 2.dp))
    }
}

/**
 * Rounds the top corners of the first row and the bottom corners of the last one,
 * so rows that are separate list items still look like one iOS card.
 */
fun Modifier.groupClip(index: Int, count: Int): Modifier {
    val r = 12.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) r else 0.dp,
        topEnd = if (index == 0) r else 0.dp,
        bottomStart = if (index == count - 1) r else 0.dp,
        bottomEnd = if (index == count - 1) r else 0.dp,
    )
    return this.clip(shape)
}

/** Swipe a row left to delete it. */
@Composable
fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(IOS.Red).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
    ) { content() }
}

@Composable
fun CheckCircle(checked: Boolean, onClick: () -> Unit) {
    val g = LocalGrouped.current
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(if (checked) IOS.Green else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        } else {
            Box(Modifier.size(22.dp).clip(CircleShape).background(g.separator).padding(1.5.dp)) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(g.card))
            }
        }
    }
}

// =====================================================================
//  Date & time pickers
// =====================================================================

/**
 * The label on top and the date and time below it ("Due" over "[Nov 5, 2026] [5:28 PM]"),
 * so it never gets squeezed in narrow windows. With an empty label only the date and time show.
 */
@Composable
fun DateTimeRow(label: String, value: LocalDateTime, onChange: (LocalDateTime) -> Unit, showTime: Boolean = true) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    GroupRow {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (label.isNotBlank()) Text(label, fontSize = 17.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(value.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) { pickDate = true }
                if (showTime) Chip(formatTime(value.toLocalTime())) { pickTime = true }
            }
        }
    }
    if (pickDate) DatePickDialog(value.toLocalDate(), { pickDate = false }) { onChange(value.with(it)) }
    if (pickTime) TimePickDialog(value.toLocalTime(), { pickTime = false }) { onChange(value.with(it)) }
}

@Composable
fun Chip(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 16.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(LocalGrouped.current.fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
fun DatePickDialog(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    // DatePicker works in UTC milliseconds
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}

@Composable
fun TimePickDialog(initial: LocalTime, onDismiss: () -> Unit, onPicked: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)); onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}

// =====================================================================
//  Export / Import (same JSON files as the iPhone app)
// =====================================================================

class ExportImportActions(val exportFile: () -> Unit, val importFile: () -> Unit)

@Composable
fun rememberExportImport(store: AppStore, kind: ExportImport.Kind): ExportImportActions {
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(ExportImport.export(kind, store.data.value).toByteArray(Charsets.UTF_8))
                }
            }.isSuccess
            Toast.makeText(context, if (ok) "Exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                store.importItems(kind, ExportImport.parse(text))
            }
            val msg = result.fold({ "$it item${if (it == 1) "" else "s"} imported" }, { "This file couldn't be read" })
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
    return remember(kind) {
        ExportImportActions(
            exportFile = { exporter.launch(kind.fileName) },
            importFile = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        )
    }
}

/** "Export …" and "Import …" buttons under a list, like iOS. */
@Composable
fun ExportImportButtons(actions: ExportImportActions, noun: String) {
    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ProminentButton("Export $noun", onClick = actions.exportFile)
        BorderedButton("Import $noun", onClick = actions.importFile)
    }
}

// =====================================================================
//  Formatting
// =====================================================================

/** Accepts "2.50" and "2,50" (Romanian keyboards show a comma). */
fun parsePrice(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

fun money(value: Double): String = String.format(java.util.Locale.getDefault(), "%.2f", value)

/** 1234.5 → "1234.5" (0–2 decimals), used on the Home screen. */
fun compactNumber(value: Double): String = DecimalFormat("0.##").format(value)

fun formatTime(t: LocalTime): String = t.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(AppStore.zone())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

fun formatDate(millis: Long, style: FormatStyle = FormatStyle.MEDIUM): String =
    Instant.ofEpochMilli(millis).atZone(AppStore.zone()).format(DateTimeFormatter.ofLocalizedDate(style))

fun toLocalDateTime(millis: Long): LocalDateTime = Instant.ofEpochMilli(millis).atZone(AppStore.zone()).toLocalDateTime()

fun toMillis(dt: LocalDateTime): Long = dt.atZone(AppStore.zone()).toInstant().toEpochMilli()

// =====================================================================
//  Lists inside a LazyColumn that look like one iOS section card
// =====================================================================

fun <T> LazyListScope.groupedList(
    items: List<T>,
    key: (T) -> Any,
    emptyText: String? = null,
    onDelete: ((T) -> Unit)? = null,
    row: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) {
        if (emptyText != null) item {
            GroupCard { GroupRow { Text(emptyText, color = LocalGrouped.current.secondaryText, fontSize = 15.sp) } }
        }
        return
    }
    itemsIndexed(items, key = { _, entry -> key(entry) }) { i, entry ->
        Column(Modifier.groupClip(i, items.size)) {
            if (i > 0) RowDivider()
            if (onDelete != null) SwipeToDelete(onDelete = { onDelete(entry) }) { row(entry) } else row(entry)
        }
    }
}

@Composable
fun PriorityBadge(priority: Priority) {
    Text(
        priority.label,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(IOS.Blue.copy(alpha = 0.1f))
            .padding(6.dp),
    )
}

// =====================================================================
//  "Remind me before" rows (Monthly Expenses and Daily Routine)
// =====================================================================

/**
 * A switch plus a date & time picker for an extra reminder before [start].
 * The choice is kept as minutes before the start ([minutesBefore]), so it
 * still fits when the start date or time changes, and for repeating items.
 * Put these rows inside a [GroupCard], after a [RowDivider].
 */
@Composable
fun ReminderRows(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    start: LocalDateTime,
    minutesBefore: Int,
    onMinutesBeforeChange: (Int) -> Unit,
) {
    val g = LocalGrouped.current
    GroupRow {
        Text("Remind me before", fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(checkedTrackColor = IOS.Green),
        )
    }
    if (enabled) {
        RowDivider()
        DateTimeRow("Remind at", start.minusMinutes(minutesBefore.toLong()), { picked ->
            onMinutesBeforeChange(java.time.Duration.between(picked, start).toMinutes().toInt())
        })
        GroupRow {
            if (minutesBefore > 0) {
                Text("${describeMinutes(minutesBefore.toLong())} before the start", fontSize = 13.sp, color = g.secondaryText)
            } else {
                Text("Choose a time before the start", fontSize = 13.sp, color = IOS.Red)
            }
        }
    }
}

// =====================================================================
//  "Task added" confirmation: blue capsule like the Add buttons, for 1 second
// =====================================================================

/** Shows [text] in a black capsule in the middle of the screen for one second every time [trigger] goes up. */
@Composable
fun AddedConfirmation(text: String, trigger: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = true
            kotlinx.coroutines.delay(1000)
            visible = false
        }
    }
    if (visible) {
        Popup(alignment = Alignment.Center) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))   // visible in Dark mode too
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Red line under a field, e.g. "Please enter a price". */
@Composable
fun ErrorRow(text: String) {
    GroupRow { Text(text, color = IOS.Red, fontSize = 14.sp) }
}


// =====================================================================
//  Swipe left to show buttons (Monthly Expenses: Complete and Delete)
// =====================================================================

class SwipeAction(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

/** Drag a row to the left to reveal [actions] (like iOS swipe actions). Tapping one closes the row. */
@Composable
fun SwipeActionsRow(actions: List<SwipeAction>, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val buttonWidth = 84.dp
    val maxReveal = with(density) { (buttonWidth * actions.size).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Row(Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
            actions.forEach { a ->
                Column(
                    Modifier
                        .width(buttonWidth)
                        .fillMaxHeight()
                        .background(a.color)
                        .clickable {
                            scope.launch { offset.animateTo(0f) }
                            a.onClick()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(a.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(a.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(maxReveal) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch { offset.animateTo(if (offset.value < -maxReveal / 2) -maxReveal else 0f) }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + dragAmount).coerceIn(-maxReveal, 0f)) }
                    }
                },
        ) { content() }
    }
}

// =====================================================================
//  Start and end dates (Projects and subtasks)
// =====================================================================

/** Default times, as on iOS: start today at 9:00, end in a week at 18:00. */
fun defaultStart(): Long = toMillis(LocalDate.now(AppStore.zone()).atTime(9, 0))
fun defaultEnd(): Long = toMillis(LocalDate.now(AppStore.zone()).plusDays(7).atTime(18, 0))

/** "Start Date" and "End Date" switches, each with its date & time below. Put inside a [GroupCard]. */
@Composable
fun StartEndRows(
    hasStart: Boolean, onHasStart: (Boolean) -> Unit, start: Long, onStart: (Long) -> Unit,
    hasEnd: Boolean, onHasEnd: (Boolean) -> Unit, end: Long, onEnd: (Long) -> Unit,
) {
    SwitchRow("Start Date", hasStart, onHasStart)
    if (hasStart) {
        // No extra label: the switch above already says "Start Date"
        DateTimeRow("", toLocalDateTime(start), { onStart(toMillis(it)) })
    }
    RowDivider()
    SwitchRow("End Date", hasEnd, onHasEnd)
    if (hasEnd) {
        DateTimeRow("", toLocalDateTime(end), { onEnd(toMillis(it)) })
        if (hasStart && end < start) {
            GroupRow { Text("The end is before the start – it will be set to the start.", color = IOS.Red, fontSize = 13.sp) }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    GroupRow {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = IOS.Green))
    }
}

/** "15 Nov, 09:00 – 30 Nov, 18:00", "From 15 Nov, 09:00", "Until 30 Nov, 18:00", or null. */
fun dateRangeText(start: Long?, end: Long?): String? {
    val f = DateTimeFormatter.ofPattern("d MMM, HH:mm", java.util.Locale.getDefault())
    fun t(ms: Long) = toLocalDateTime(ms).format(f)
    return when {
        start != null && end != null -> "${t(start)} – ${t(end)}"
        start != null -> "From ${t(start)}"
        end != null -> "Until ${t(end)}"
        else -> null
    }
}

// =====================================================================
//  Edit window: Cancel / Save around a form (hold a row to open one)
// =====================================================================

@Composable
fun EditDialog(
    title: String,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                GroupCard(content = content)
                if (footer != null) {
                    Text(footer, fontSize = 13.sp, color = LocalGrouped.current.secondaryText, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = canSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
