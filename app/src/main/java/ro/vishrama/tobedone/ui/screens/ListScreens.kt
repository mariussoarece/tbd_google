package ro.vishrama.tobedone.ui.screens

import ro.vishrama.tobedone.data.DueTask
import ro.vishrama.tobedone.ui.EditDialog
import ro.vishrama.tobedone.ui.SwipeAction
import ro.vishrama.tobedone.ui.SwipeActionsRow
import ro.vishrama.tobedone.ui.groupClip
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.ExportImport
import ro.vishrama.tobedone.data.MonthlyTask
import ro.vishrama.tobedone.data.describeMinutes
import ro.vishrama.tobedone.data.Priority
import ro.vishrama.tobedone.ui.AddButtonRow
import ro.vishrama.tobedone.ui.AddedConfirmation
import ro.vishrama.tobedone.ui.ErrorRow
import ro.vishrama.tobedone.ui.Chip
import ro.vishrama.tobedone.ui.DateTimeRow
import ro.vishrama.tobedone.ui.DetailScreen
import ro.vishrama.tobedone.ui.ExportImportButtons
import ro.vishrama.tobedone.ui.FieldRow
import ro.vishrama.tobedone.ui.GroupCard
import ro.vishrama.tobedone.ui.GroupRow
import ro.vishrama.tobedone.ui.PriorityBadge
import ro.vishrama.tobedone.ui.PickerRow
import ro.vishrama.tobedone.ui.PriorityRow
import ro.vishrama.tobedone.ui.ReminderRows
import ro.vishrama.tobedone.ui.RowDivider
import ro.vishrama.tobedone.ui.SectionFooter
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.TimePickDialog
import ro.vishrama.tobedone.ui.formatTime
import ro.vishrama.tobedone.ui.formatDateTime
import ro.vishrama.tobedone.ui.groupedList
import ro.vishrama.tobedone.ui.money
import ro.vishrama.tobedone.ui.parsePrice
import ro.vishrama.tobedone.ui.rememberExportImport
import ro.vishrama.tobedone.ui.theme.IOS
import ro.vishrama.tobedone.ui.theme.LocalGrouped
import ro.vishrama.tobedone.ui.toLocalDateTime
import ro.vishrama.tobedone.ui.toMillis
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.Locale

// =====================================================================
//  To Do  (iOS DueTasksView) — now with date AND time
// =====================================================================

@Composable
fun TodoScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    val io = rememberExportImport(store, ExportImport.Kind.TASKS)
    val g = LocalGrouped.current

    var title by rememberSaveable { mutableStateOf("") }
    var dueAt by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIUM) }
    var remind by rememberSaveable { mutableStateOf(false) }
    var remindBefore by rememberSaveable { mutableIntStateOf(30) }   // default: 30 min before
    val canAdd = title.isNotBlank() && (!remind || remindBefore > 0)
    var added by remember { mutableIntStateOf(0) }   // shows "Task added" for 1 second

    fun add() {
        if (!canAdd) return
        store.addTask(title, dueAt, priority, if (remind) remindBefore else null)
        added++
        title = ""
        dueAt = System.currentTimeMillis()
        priority = Priority.MEDIUM
        remind = false
        remindBefore = 30
    }

    val sorted = data.tasks.sortedBy { it.dueAt }
    val open = sorted.filter { !it.isCompleted }
    val done = sorted.filter { it.isCompleted }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var saved by remember { mutableIntStateOf(0) }

    AddedConfirmation("Task added", added)
    AddedConfirmation("Task saved", saved)

    DetailScreen("To Do Tasks", IOS.Blue, onBack) {
        item { SectionHeader("Add New Task") }
        item {
            GroupCard {
                FieldRow(title, { title = it }, "Task Title", onDone = ::add)
                RowDivider()
                DateTimeRow("Due Date & Time", toLocalDateTime(dueAt), { dueAt = toMillis(it) })
                RowDivider()
                PriorityRow(priority) { priority = it }
                RowDivider()
                ReminderRows(remind, { remind = it }, toLocalDateTime(dueAt), remindBefore) { remindBefore = it }
                RowDivider()
                AddButtonRow("Add Task", enabled = canAdd, onClick = ::add)
            }
        }

        item { SectionHeader("Due Tasks – tap to complete, hold to edit") }
        groupedList(open, { it.id }, emptyText = "No tasks due.", onDelete = { store.deleteTask(it.id) }) { t ->
            GroupRow(onClick = { store.toggleTask(t.id) }, onLongClick = { editingId = t.id }) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontSize = 17.sp)
                    Text("Due: ${formatDateTime(t.dueAt)}", fontSize = 12.sp, color = g.secondaryText)
                    t.reminderMinutes?.let {
                        Text("🔔 ${describeMinutes(it.toLong())} before", fontSize = 12.sp, color = g.secondaryText)
                    }
                }
                PriorityBadge(t.priority)
            }
        }

        item { ExportImportButtons(io, "Tasks") }

        item { SectionHeader("Completed Tasks") }
        groupedList(done, { it.id }, emptyText = "Swipe left on a completed task to delete it.", onDelete = { store.deleteTask(it.id) }) { t ->
            GroupRow(onClick = { store.toggleTask(t.id) }, onLongClick = { editingId = t.id }) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontSize = 17.sp, textDecoration = TextDecoration.LineThrough, color = g.secondaryText)
                    Text("Due: ${formatDateTime(t.dueAt)}", fontSize = 12.sp, color = g.secondaryText)
                }
                PriorityBadge(t.priority)
            }
        }
    }

    data.tasks.firstOrNull { it.id == editingId }?.let { t ->
        EditTaskDialog(t, onDismiss = { editingId = null }) { title2, due, pr, rem ->
            store.editTask(t.id, title2, due, pr, rem)
            editingId = null
            saved++
        }
    }
}

@Composable
private fun EditTaskDialog(t: DueTask, onDismiss: () -> Unit, onSave: (String, Long, Priority, Int?) -> Unit) {
    var title by rememberSaveable(t.id) { mutableStateOf(t.title) }
    var due by rememberSaveable(t.id) { mutableStateOf(t.dueAt) }
    var priority by rememberSaveable(t.id) { mutableStateOf(t.priority) }
    var remind by rememberSaveable(t.id) { mutableStateOf((t.reminderMinutes ?: 0) > 0) }
    var remindBefore by rememberSaveable(t.id) { mutableIntStateOf(t.reminderMinutes ?: 30) }
    EditDialog(
        "Edit Task",
        canSave = title.isNotBlank() && (!remind || remindBefore > 0),
        onDismiss = onDismiss,
        onSave = { onSave(title, due, priority, if (remind) remindBefore else null) },
    ) {
        FieldRow(title, { title = it }, "Task Title")
        RowDivider()
        DateTimeRow("Due", toLocalDateTime(due), { due = toMillis(it) })
        RowDivider()
        PriorityRow(priority) { priority = it }
        RowDivider()
        ReminderRows(remind, { remind = it }, toLocalDateTime(due), remindBefore) { remindBefore = it }
    }
}

// =====================================================================
//  Monthly Expenses  (iOS MonthlyTasksView) — prices with currency
// =====================================================================

@Composable
fun MonthlyScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    val settings by store.settings.collectAsStateWithLifecycle()
    val io = rememberExportImport(store, ExportImport.Kind.MONTHLY)
    val g = LocalGrouped.current
    val cur = settings.currency

    val todayDay = LocalDate.now(AppStore.zone()).dayOfMonth.coerceAtMost(28)
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableIntStateOf(todayDay) }
    var hour by rememberSaveable { mutableIntStateOf(10) }      // default 10:00
    var minute by rememberSaveable { mutableIntStateOf(0) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val canAdd = title.isNotBlank()
    var priceMissing by rememberSaveable { mutableStateOf(false) }
    var added by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var toastCount by remember { mutableIntStateOf(0) }
    fun say(text: String) { toast = text; toastCount++ }

    fun add() {
        if (!canAdd) return
        val p = parsePrice(price)
        if (p == null) { priceMissing = true; return }
        priceMissing = false
        store.addMonthlyTask(title, p, day, hour, minute)
        added++
        title = ""
        price = ""
        day = todayDay
        hour = 10
        minute = 0
    }

    // Sorted by day of the month, then time; items without a day at the end
    val sorted = data.monthlyTasks.sortedWith(compareBy({ it.dayOfMonth ?: 99 }, { it.hour * 60 + it.minute }))
    val active = sorted.filter { !it.isCompleted }
    val completed = sorted.filter { it.isCompleted }
    val thisMonth = java.time.YearMonth.now(AppStore.zone())

    AddedConfirmation("Expense added", added)
    AddedConfirmation(toast, toastCount)

    DetailScreen("Monthly Expenses", IOS.Orange, onBack) {
        item { SectionHeader("Add Monthly Expense") }
        item {
            GroupCard {
                FieldRow(title, { title = it }, "Title")
                RowDivider()
                FieldRow(price, { price = it; priceMissing = false }, "Price ($cur)", decimal = true, onDone = ::add)
                if (priceMissing) ErrorRow("Please enter a price")
                RowDivider()
                PickerRow("Day of month", day, (1..28).toList(), { "$it" }) { day = it }
                RowDivider()
                GroupRow {
                    Text("Notification Time", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Chip(formatTime(LocalTime.of(hour, minute))) { pickTime = true }
                }
                RowDivider()
                AddButtonRow("Add Task", enabled = canAdd, onClick = ::add)
            }
        }
        item {
            SectionFooter("You get a reminder every month on this day and time. Days go up to 28 so the reminder comes in every month.")
        }

        item { SectionHeader("Monthly Expenses – tap to mark as paid, hold to edit") }
        monthlyList(active, thisMonth, cur,
            onTap = { m ->
                store.toggleMonthlyPaid(m.id)
                if (!m.isPaid(thisMonth)) say("Paid this month")
            },
            onHold = { editingId = it.id },
            onComplete = { store.toggleMonthlyCompleted(it.id); say("Completed") },
            onDelete = { store.deleteMonthlyTask(it.id) },
            emptyText = "No monthly expenses yet.",
        )
        item { SectionFooter("Swipe left on an expense for Complete (finished for good) or Delete.") }

        item { Spacer(Modifier.padding(top = 12.dp)) }
        item {
            GroupCard {
                // Every active expense; completed ones are not counted
                GroupRow {
                    Text("Total:", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text("${money(active.sumOf { it.price })} $cur", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = IOS.Blue)
                }
                RowDivider()
                // The ones marked as paid this month are deducted
                GroupRow {
                    Text("Still to pay this month:", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text("${money(active.filter { !it.isPaid(thisMonth) }.sumOf { it.price })} $cur", fontSize = 17.sp, color = IOS.Orange)
                }
            }
        }

        item { ExportImportButtons(io, "Expenses") }

        // Finished for good: no reminders, not in the totals
        item { SectionHeader("Completed") }
        monthlyList(completed, thisMonth, cur,
            onTap = {},
            onHold = { editingId = it.id },
            onComplete = { store.toggleMonthlyCompleted(it.id); say("Restored") },
            onDelete = { store.deleteMonthlyTask(it.id) },
            emptyText = "Expenses you complete appear here.",
        )
    }

    if (pickTime) {
        TimePickDialog(LocalTime.of(hour, minute), { pickTime = false }) { hour = it.hour; minute = it.minute }
    }

    data.monthlyTasks.firstOrNull { it.id == editingId }?.let { m ->
        EditMonthlyDialog(m, cur, onDismiss = { editingId = null }) { t, p, d, h, min ->
            store.updateMonthlyTask(m.id, t, p, d, h, min)
            editingId = null
        }
    }
}

/**
 * Monthly rows: tap = paid this month, hold = edit, swipe left = Complete / Delete
 * (on completed ones: Restore / Delete). Completed ones are struck through.
 */
private fun LazyListScope.monthlyList(
    list: List<MonthlyTask>,
    thisMonth: java.time.YearMonth,
    currency: String,
    onTap: (MonthlyTask) -> Unit,
    onHold: (MonthlyTask) -> Unit,
    onComplete: (MonthlyTask) -> Unit,
    onDelete: (MonthlyTask) -> Unit,
    emptyText: String,
) {
    if (list.isEmpty()) {
        item { GroupCard { GroupRow { Text(emptyText, color = LocalGrouped.current.secondaryText, fontSize = 15.sp) } } }
        return
    }
    itemsIndexed(list, key = { _, m -> m.id }) { i, m ->
        val g = LocalGrouped.current
        Column(Modifier.groupClip(i, list.size)) {
            if (i > 0) RowDivider()
            val completeAction = if (m.isCompleted) {
                SwipeAction("Restore", Icons.AutoMirrored.Filled.Undo, Color.Gray, onClick = { onComplete(m) })
            } else {
                SwipeAction("Complete", Icons.Filled.Check, IOS.Green, onClick = { onComplete(m) })
            }
            val deleteAction = SwipeAction("Delete", Icons.Filled.Delete, IOS.Red, onClick = { onDelete(m) })
            SwipeActionsRow(listOf(completeAction, deleteAction)) {
                GroupRow(onClick = { if (!m.isCompleted) onTap(m) }, onLongClick = { onHold(m) }) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            m.title, fontSize = 17.sp,
                            textDecoration = if (m.isCompleted) TextDecoration.LineThrough else null,
                            color = if (m.isCompleted) g.secondaryText else MaterialTheme.colorScheme.onSurface,
                        )
                        if (!m.isCompleted) {
                            val schedule = m.dayOfMonth?.let { "Every month on day $it at ${formatTime(LocalTime.of(m.hour, m.minute))}" }
                            Text(
                                when {
                                    schedule == null -> "No day set – hold to choose one"
                                    m.isPaid(thisMonth) -> "$schedule · ✓ paid this month"
                                    else -> schedule
                                },
                                fontSize = 12.sp,
                                color = if (schedule == null) IOS.Orange else g.secondaryText,
                            )
                        }
                    }
                    Text("${money(m.price)} $currency", fontSize = 17.sp, color = g.secondaryText)
                }
            }
        }
    }
}

@Composable
private fun EditMonthlyDialog(
    m: MonthlyTask,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int, Int, Int) -> Unit,
) {
    var title by rememberSaveable(m.id) { mutableStateOf(m.title) }
    var price by rememberSaveable(m.id) { mutableStateOf(money(m.price)) }
    var day by rememberSaveable(m.id) { mutableIntStateOf(m.dayOfMonth ?: LocalDate.now(AppStore.zone()).dayOfMonth.coerceAtMost(28)) }
    var hour by rememberSaveable(m.id) { mutableIntStateOf(m.hour) }
    var minute by rememberSaveable(m.id) { mutableIntStateOf(m.minute) }
    var pickTime by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Monthly Expense") },
        text = {
            GroupCard(Modifier.verticalScroll(rememberScrollState())) {
                FieldRow(title, { title = it }, "Title")
                RowDivider()
                FieldRow(price, { price = it }, "Price ($currency)", decimal = true)
                if (parsePrice(price) == null) ErrorRow("Please enter a price")
                RowDivider()
                PickerRow("Day of month", day, (1..28).toList(), { "$it" }) { day = it }
                RowDivider()
                GroupRow {
                    Text("Notification Time", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Chip(formatTime(LocalTime.of(hour, minute))) { pickTime = true }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && parsePrice(price) != null,   // the price can't be empty
                onClick = { parsePrice(price)?.let { p -> onSave(title, p, day, hour, minute) } },
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickTime) {
        TimePickDialog(LocalTime.of(hour, minute), { pickTime = false }) { hour = it.hour; minute = it.minute }
    }
}

// =====================================================================
//  Shopping  (iOS ShoppingTasksView)
// =====================================================================

@Composable
fun ShoppingScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    val settings by store.settings.collectAsStateWithLifecycle()
    val io = rememberExportImport(store, ExportImport.Kind.SHOPPING)
    val g = LocalGrouped.current
    val cur = settings.currency

    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIUM) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editedPrice by rememberSaveable { mutableStateOf("") }

    val canAdd = name.isNotBlank()
    var priceMissing by rememberSaveable { mutableStateOf(false) }
    var added by remember { mutableIntStateOf(0) }
    var saved by remember { mutableIntStateOf(0) }
    var detailsId by rememberSaveable { mutableStateOf<String?>(null) }   // "hold to edit"
    fun add() {
        if (name.isBlank()) return
        val p = parsePrice(price)
        if (p == null) { priceMissing = true; return }
        priceMissing = false
        store.addShoppingItem(name, p, priority)
        added++
        name = ""
        price = ""
    }

    val open = data.shoppingItems.filter { !it.isCompleted }
    val done = data.shoppingItems.filter { it.isCompleted }
    val dateFmt = SimpleDateFormat("MM.dd.yyyy", Locale.getDefault())

    AddedConfirmation("Item added", added)
    AddedConfirmation("Item saved", saved)

    DetailScreen("Shopping Tasks", IOS.Red, onBack) {
        item { SectionHeader("Add New Item") }
        item {
            GroupCard {
                FieldRow(name, { name = it }, "Item Name")
                RowDivider()
                FieldRow(price, { price = it; priceMissing = false }, "Price", decimal = true, onDone = ::add)
                if (priceMissing) ErrorRow("Please enter a price")
                RowDivider()
                PriorityRow(priority) { priority = it }
                RowDivider()
                AddButtonRow("Add Item", enabled = canAdd, onClick = ::add)
            }
        }

        item { SectionHeader("Shopping List – tap to complete, hold to edit") }
        groupedList(open, { it.id }, emptyText = "Nothing to buy.", onDelete = { store.deleteShoppingItem(it.id) }) { s ->
            GroupRow(onClick = { editingId = s.id; editedPrice = money(s.price) }, onLongClick = { detailsId = s.id }) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${money(s.price)} $cur", fontSize = 15.sp, color = g.secondaryText)
                }
                Icon(Icons.Filled.AddShoppingCart, contentDescription = "Buy", tint = IOS.Green)
            }
        }

        item { Spacer(Modifier.padding(top = 20.dp)) }
        item {
            GroupCard {
                GroupRow {
                    Text("Total to Buy:", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text("${money(open.sumOf { it.price })} $cur", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = IOS.Blue)
                }
            }
        }

        item { ExportImportButtons(io, "Tasks") }

        item { SectionHeader("Completed Purchases") }
        groupedList(done, { it.id }, emptyText = "Swipe left on a purchase to delete it.", onDelete = { store.deleteShoppingItem(it.id) }) { s ->
            GroupRow(onClick = { store.toggleShoppingItem(s.id) }, onLongClick = { detailsId = s.id }) {
                Text(s.name, fontSize = 17.sp, textDecoration = TextDecoration.LineThrough, modifier = Modifier.weight(1f))
                s.completedAt?.let { Text(dateFmt.format(Date(it)), fontSize = 12.sp, color = g.secondaryText) }
                Icon(Icons.Filled.CheckCircle, contentDescription = "Bought", tint = IOS.Green)
            }
        }

        item { Spacer(Modifier.padding(top = 20.dp)) }
        item {
            GroupCard {
                GroupRow {
                    Text("Total:", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text("${money(data.shoppingItems.sumOf { it.price })} $cur", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
    }

    // "Final Price": confirm what was paid and mark as bought
    val editing = data.shoppingItems.firstOrNull { it.id == editingId }
    if (editing != null) {
        AlertDialog(
            onDismissRequest = { editingId = null },
            title = { Text("Final Price") },
            text = {
                Column {
                    Text(editing.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Change it if you paid a different amount, then tap Complete.",
                        fontSize = 13.sp, color = g.secondaryText,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.padding(top = 12.dp))
                    OutlinedTextField(
                        value = editedPrice,
                        onValueChange = { editedPrice = it },
                        label = { Text("Price") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.buyShoppingItem(editing.id, parsePrice(editedPrice))
                    editingId = null
                }) { Text("Complete") }
            },
            dismissButton = {
                TextButton(onClick = { editingId = null }) { Text("Cancel", color = IOS.Red) }
            },
        )
    }

    data.shoppingItems.firstOrNull { it.id == detailsId }?.let { item ->
        var n by rememberSaveable(item.id) { mutableStateOf(item.name) }
        var pr by rememberSaveable(item.id) { mutableStateOf(money(item.price)) }
        var prio by rememberSaveable(item.id) { mutableStateOf(item.priority) }
        EditDialog(
            "Edit Item",
            canSave = n.isNotBlank() && parsePrice(pr) != null,
            onDismiss = { detailsId = null },
            onSave = {
                parsePrice(pr)?.let { p -> store.editShoppingItem(item.id, n, p, prio) }
                detailsId = null
                saved++
            },
        ) {
            FieldRow(n, { n = it }, "Item Name")
            RowDivider()
            FieldRow(pr, { pr = it }, "Price ($cur)", decimal = true)
            if (parsePrice(pr) == null) ErrorRow("Please enter a price")
            RowDivider()
            PriorityRow(prio) { prio = it }
        }
    }
}

// =====================================================================
//  Grocery  (iOS GroceryTasksView)
// =====================================================================

@Composable
fun GroceryScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var added by remember { mutableIntStateOf(0) }
    var saved by remember { mutableIntStateOf(0) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    fun add() {
        if (name.isBlank()) return
        store.addGroceryItem(name)
        added++
        name = ""
    }

    val sorted = data.groceryItems.sortedBy { it.name.lowercase() }
    val toBuy = sorted.filter { !it.isBought }
    val bought = sorted.filter { it.isBought }

    AddedConfirmation("Item added", added)
    AddedConfirmation("Item saved", saved)

    DetailScreen("Grocery List", IOS.Yellow, onBack) {
        item { SectionHeader("Add Grocery Item") }
        item {
            GroupCard {
                FieldRow(name, { name = it }, "Item Name", onDone = ::add)
                RowDivider()
                AddButtonRow("Add Item", enabled = name.isNotBlank(), onClick = ::add)
            }
        }

        item { SectionHeader("Grocery List – tap when bought, hold to edit") }
        groupedList(toBuy, { it.id }, emptyText = "Your grocery list is empty.", onDelete = { store.deleteGroceryItem(it.id) }) { item ->
            GroupRow(onClick = { store.toggleGroceryItem(item.id) }, onLongClick = { editingId = item.id }) {
                Text(item.name, fontSize = 17.sp, modifier = Modifier.weight(1f))
            }
        }

        item { SectionHeader("Bought Items") }
        groupedList(bought, { it.id }, emptyText = "Swipe left on a bought item to delete it.", onDelete = { store.deleteGroceryItem(it.id) }) { item ->
            GroupRow(onClick = { store.toggleGroceryItem(item.id) }, onLongClick = { editingId = item.id }) {
                Text(
                    item.name, fontSize = 17.sp,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    data.groceryItems.firstOrNull { it.id == editingId }?.let { item ->
        var n by rememberSaveable(item.id) { mutableStateOf(item.name) }
        EditDialog(
            "Edit Item",
            canSave = n.isNotBlank(),
            onDismiss = { editingId = null },
            onSave = { store.editGroceryItem(item.id, n); editingId = null; saved++ },
        ) {
            FieldRow(n, { n = it }, "Item Name")
        }
    }
}
