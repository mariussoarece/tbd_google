package ro.vishrama.tobedone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import ro.vishrama.tobedone.ui.formatPrice
import ro.vishrama.tobedone.ui.parsePrice
import ro.vishrama.tobedone.ui.theme.DoneGreen

@Composable
fun ShoppingScreen(store: AppStore, padding: PaddingValues) {
    val data by store.data.collectAsStateWithLifecycle()
    val currency by store.currency.collectAsStateWithLifecycle()

    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIUM) }

    val parsedPrice = parsePrice(price)
    val canAdd = name.isNotBlank() && parsedPrice != null

    fun add() {
        val p = parsePrice(price) ?: return
        if (name.isBlank()) return
        store.addShoppingItem(name, p, currency, priority)
        name = ""
        price = ""
    }

    val open = data.shoppingItems.filter { !it.isCompleted }
    val done = data.shoppingItems.filter { it.isCompleted }
    // Total of items still to buy, per currency.
    val totals = open.groupBy { it.currency }.mapValues { (_, list) -> list.sumOf { it.price } }

    ScreenList(padding) {
        item { SectionHeader("Add New Item") }
        item {
            FormCard {
                TextInput(name, { name = it }, "Item Name")
                TextInput(price, { price = it }, "Price ($currency)", decimal = true, onDone = ::add)
                PriorityPicker(priority) { priority = it }
                Button(onClick = ::add, enabled = canAdd, modifier = Modifier.fillMaxWidth()) {
                    Text("Add Item")
                }
            }
        }

        item { SectionHeader("Shopping List") }
        if (open.isEmpty()) item { EmptyHint("Nothing to buy. Tap the cart when you've bought an item.") }
        items(open, key = { it.id }) { item ->
            SwipeToDelete(onDelete = { store.deleteShoppingItem(item.id) }) {
                RowCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${formatPrice(item.price)} ${item.currency}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        PriorityBadge(item.priority)
                        IconButton(onClick = { store.completeShoppingItem(item.id) }) {
                            Icon(Icons.Filled.AddShoppingCart, contentDescription = "Mark as bought", tint = DoneGreen)
                        }
                    }
                }
            }
        }

        item { SectionHeader("Total Cost") }
        item {
            FormCard {
                if (totals.isEmpty()) {
                    TotalText("${formatPrice(0.0)} $currency")
                } else {
                    totals.forEach { (cur, sum) -> TotalText("${formatPrice(sum)} $cur") }
                }
            }
        }

        if (done.isNotEmpty()) {
            item { SectionHeader("Completed Purchases") }
            items(done, key = { it.id }) { item ->
                SwipeToDelete(onDelete = { store.deleteShoppingItem(item.id) }) {
                    RowCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                item.name,
                                textDecoration = TextDecoration.LineThrough,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Bought", tint = DoneGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}
