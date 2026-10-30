package ro.vishrama.tobedone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.ui.EmptyHint
import ro.vishrama.tobedone.ui.FormCard
import ro.vishrama.tobedone.ui.RowCard
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.SwipeToDelete
import ro.vishrama.tobedone.ui.TextInput
import ro.vishrama.tobedone.ui.theme.DoneGreen

@Composable
fun GroceryScreen(store: AppStore, padding: PaddingValues) {
    val data by store.data.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }

    fun add() {
        if (name.isBlank()) return
        store.addGroceryItem(name)
        name = ""
    }

    // Not-bought first, then alphabetical — same as iOS.
    val groceries = data.groceryItems.sortedWith(compareBy({ it.isBought }, { it.name.lowercase() }))

    ScreenList(padding) {
        item { SectionHeader("Add Grocery Item") }
        item {
            FormCard {
                TextInput(name, { name = it }, "Item Name", onDone = ::add)
                Button(onClick = ::add, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Add")
                }
            }
        }

        item { SectionHeader("Grocery List") }
        if (groceries.isEmpty()) item { EmptyHint("Your grocery list is empty. Tap an item to tick it off.") }
        items(groceries, key = { it.id }) { item ->
            SwipeToDelete(onDelete = { store.deleteGroceryItem(item.id) }) {
                RowCard(Modifier.clickable { store.toggleGroceryItem(item.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.name,
                            textDecoration = if (item.isBought) TextDecoration.LineThrough else null,
                            color = if (item.isBought) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (item.isBought) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Bought", tint = DoneGreen)
                        } else {
                            Icon(
                                Icons.Outlined.Circle,
                                contentDescription = "Not bought",
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }
    }
}
