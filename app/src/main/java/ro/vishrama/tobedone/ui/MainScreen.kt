@file:OptIn(ExperimentalMaterial3Api::class)

package ro.vishrama.tobedone.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.ui.screens.DueTasksScreen
import ro.vishrama.tobedone.ui.screens.GroceryScreen
import ro.vishrama.tobedone.ui.screens.MonthlyTasksScreen
import ro.vishrama.tobedone.ui.screens.OtherTasksScreen
import ro.vishrama.tobedone.ui.screens.SettingsScreen
import ro.vishrama.tobedone.ui.screens.ShoppingScreen

private enum class Tab(val title: String, val short: String, val icon: ImageVector) {
    DUE("Due Tasks", "Due", Icons.Filled.Event),
    SHOPPING("Shopping Tasks", "Shopping", Icons.Filled.ShoppingCart),
    MONTHLY("Monthly Tasks", "Monthly", Icons.Filled.CalendarMonth),
    GROCERY("Grocery List", "Grocery", Icons.Filled.ShoppingBag),
    OTHER("Other Tasks", "Other", Icons.Filled.Checklist),
    SETTINGS("Settings", "Settings", Icons.Filled.Settings),
}

/** Same six tabs as the iOS TabView. */
@Composable
fun MainScreen(store: AppStore) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tab = Tab.entries[selected]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tab.title) },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = index == selected,
                        onClick = { selected = index },
                        icon = { Icon(t.icon, contentDescription = t.title) },
                        label = {
                            Text(t.short, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Clip)
                        },
                    )
                }
            }
        },
    ) { padding: PaddingValues ->
        when (tab) {
            Tab.DUE -> DueTasksScreen(store, padding)
            Tab.SHOPPING -> ShoppingScreen(store, padding)
            Tab.MONTHLY -> MonthlyTasksScreen(store, padding)
            Tab.GROCERY -> GroceryScreen(store, padding)
            Tab.OTHER -> OtherTasksScreen(store, padding)
            Tab.SETTINGS -> SettingsScreen(store, padding)
        }
    }
}
