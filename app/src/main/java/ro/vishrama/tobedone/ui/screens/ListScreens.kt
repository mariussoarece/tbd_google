package ro.vishrama.tobedone.ui.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
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
import ro.vishrama.tobedone.data.Priority
import ro.vishrama.tobedone.ui.AddButtonRow
import ro.vishrama.tobedone.ui.DateTimeRow
import ro.vishrama.tobedone.ui.DetailScreen
import ro.vishrama.tobedone.ui.ExportImportButtons
import ro.vishrama.tobedone.ui.FieldRow
import ro.vishrama.tobedone.ui.GroupCard
import ro.vishrama.tobedone.ui.GroupRow
import ro.vishrama.tobedone.ui.PriorityBadge
import ro.vishrama.tobedone.ui.PriorityRow
import ro.vishrama.tobedone.ui.RowDivider
import ro.vishrama.tobedone.ui.SectionHeader
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

    fun add() {
        if (title.isBlank()) return
        store.addTask(title, dueAt, priority)
        title = ""
        dueAt = System.currentTimeMillis()
        priority = Priority.MEDIUM
    }

    val sorted = data.tasks.sortedBy { it.dueAt }
    val open = sorted.filter { !it.isCompleted }
    val done = sorted.filter { it.isCompleted }

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
                AddButtonRow("Add Task", enabled = title.isNotBlank(), onClick = ::add)
            }
        }

        item { SectionHeader("Due Tasks") }
        groupedList(open, { it.id }, emptyText = "No tasks due. Tap a task to mark it done.", onDelete = { store.deleteTask(it.id) }) { t ->
            GroupRow(onClick = { store.toggleTask(t.id) }) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontSize = 17.sp)
                    Text("Due: ${formatDateTime(t.dueAt)}", fontSize = 12.sp, color = g.secondaryText)
                }
                PriorityBadge(t.priority)
            }
        }

        item { ExportImportButtons(io, "Tasks") }

        item { SectionHeader("Completed Tasks") }
        groupedList(done, { it.id }, emptyText = "Swipe left on a completed task to delete it.", onDelete = { store.deleteTask(it.id) }) { t ->
            GroupRow(onClick = { store.toggleTask(t.id) }) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontSize = 17.sp, textDecoration = TextDecoration.LineThrough, color = g.secondaryText)
                    Text("Due: ${formatDateTime(t.dueAt)}", fontSize = 12.sp, color = g.secondaryText)
                }
                PriorityBadge(t.priority)
            }
        }
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

    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }

    fun add() {
        if (title.isBlank()) return
        store.addMonthlyTask(title, parsePrice(price) ?: 0.0)
        title = ""
        price = ""
    }

    DetailScreen("Monthly Expenses", IOS.Orange, onBack) {
        item { SectionHeader("Add Monthly Expense") }
        item {
            GroupCard {
                FieldRow(title, { title = it }, "Title")
                RowDivider()
                FieldRow(price, { price = it }, "Price ($cur)", decimal = true, onDone = ::add)
                RowDivider()
                AddButtonRow("Add", enabled = title.isNotBlank(), onClick = ::add)
            }
        }

        item { SectionHeader("Monthly Expenses - Swipe to delete") }
        groupedList(data.monthlyTasks, { it.id }, emptyText = "No monthly expenses yet.", onDelete = { store.deleteMonthlyTask(it.id) }) { m ->
            GroupRow {
                Text(m.title, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Text("${money(m.price)} $cur", fontSize = 17.sp, color = g.secondaryText)
            }
        }

        item { Spacer(Modifier.padding(top = 20.dp)) }
        item {
            GroupCard {
                GroupRow {
                    Text("Total:", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text("${money(data.monthlyTasks.sumOf { it.price })} $cur", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = IOS.Blue)
                }
            }
        }

        item { ExportImportButtons(io, "Expenses") }
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

    val canAdd = name.isNotBlank() && parsePrice(price) != null
    fun add() {
        val p = parsePrice(price) ?: return
        if (name.isBlank()) return
        store.addShoppingItem(name, p, priority)
        name = ""
        price = ""
    }

    val open = data.shoppingItems.filter { !it.isCompleted }
    val done = data.shoppingItems.filter { it.isCompleted }
    val dateFmt = SimpleDateFormat("MM.dd.yyyy", Locale.getDefault())

    DetailScreen("Shopping Tasks", IOS.Red, onBack) {
        item { SectionHeader("Add New Item") }
        item {
            GroupCard {
                FieldRow(name, { name = it }, "Item Name")
                RowDivider()
                FieldRow(price, { price = it }, "Price", decimal = true, onDone = ::add)
                RowDivider()
                PriorityRow(priority) { priority = it }
                RowDivider()
                AddButtonRow("Add Item", enabled = canAdd, onClick = ::add)
            }
        }

        item { SectionHeader("Shopping List") }
        groupedList(open, { it.id }, emptyText = "Nothing to buy. Tap an item when you buy it.", onDelete = { store.deleteShoppingItem(it.id) }) { s ->
            GroupRow(onClick = { editingId = s.id; editedPrice = money(s.price) }) {
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
            GroupRow(onClick = { store.toggleShoppingItem(s.id) }) {
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

    // "Edit Price" sheet: confirm the real price and mark as bought
    val editing = data.shoppingItems.firstOrNull { it.id == editingId }
    if (editing != null) {
        AlertDialog(
            onDismissRequest = { editingId = null },
            title = { Text("Edit Price") },
            text = {
                Column {
                    Text(editing.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
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
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { editingId = null }) { Text("Cancel", color = IOS.Red) }
            },
        )
    }
}

// =====================================================================
//  Grocery  (iOS GroceryTasksView)
// =====================================================================

@Composable
fun GroceryScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }

    fun add() {
        if (name.isBlank()) return
        store.addGroceryItem(name)
        name = ""
    }

    val sorted = data.groceryItems.sortedBy { it.name.lowercase() }
    val toBuy = sorted.filter { !it.isBought }
    val bought = sorted.filter { it.isBought }

    DetailScreen("Grocery List", IOS.Yellow, onBack) {
        item { SectionHeader("Add Grocery Item") }
        item {
            GroupCard {
                FieldRow(name, { name = it }, "Item Name", onDone = ::add)
                RowDivider()
                AddButtonRow("Add Item", enabled = name.isNotBlank(), onClick = ::add)
            }
        }

        item { SectionHeader("Grocery List") }
        groupedList(toBuy, { it.id }, emptyText = "Your grocery list is empty. Tap an item when it's in the basket.", onDelete = { store.deleteGroceryItem(it.id) }) { item ->
            GroupRow(onClick = { store.toggleGroceryItem(item.id) }) {
                Text(item.name, fontSize = 17.sp, modifier = Modifier.weight(1f))
            }
        }

        item { SectionHeader("Bought Items") }
        groupedList(bought, { it.id }, emptyText = "Swipe left on a bought item to delete it.", onDelete = { store.deleteGroceryItem(it.id) }) { item ->
            GroupRow(onClick = { store.toggleGroceryItem(item.id) }) {
                Text(
                    item.name, fontSize = 17.sp,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
