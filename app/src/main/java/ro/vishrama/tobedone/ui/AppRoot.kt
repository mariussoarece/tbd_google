package ro.vishrama.tobedone.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.ui.screens.ConfidenceScreen
import ro.vishrama.tobedone.ui.screens.DailyRoutineScreen
import ro.vishrama.tobedone.ui.screens.FocusTestScreen
import ro.vishrama.tobedone.ui.screens.GroceryScreen
import ro.vishrama.tobedone.ui.screens.HomeScreen
import ro.vishrama.tobedone.ui.screens.IntroFlow
import ro.vishrama.tobedone.ui.screens.MonthlyScreen
import ro.vishrama.tobedone.ui.screens.ProjectsScreen
import ro.vishrama.tobedone.ui.screens.SettingsScreen
import ro.vishrama.tobedone.ui.screens.ShoppingScreen
import ro.vishrama.tobedone.ui.screens.TodoScreen

enum class Screen { HOME, TODO, MONTHLY, PROJECTS, SHOPPING, GROCERY, ROUTINE, CONFIDENCE, FOCUS, SETTINGS }

/** Home screen with a list of sections, each opening its own screen (iOS NavigationStack). */
@Composable
fun AppRoot(store: AppStore) {
    val settings by store.settings.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val back = { screen = Screen.HOME }

    if (!settings.hasSeenIntro) {
        IntroFlow(store)
        return
    }

    when (screen) {
        Screen.HOME -> HomeScreen(store, onOpen = { screen = it })
        Screen.TODO -> TodoScreen(store, back)
        Screen.MONTHLY -> MonthlyScreen(store, back)
        Screen.PROJECTS -> ProjectsScreen(store, back)
        Screen.SHOPPING -> ShoppingScreen(store, back)
        Screen.GROCERY -> GroceryScreen(store, back)
        Screen.ROUTINE -> DailyRoutineScreen(store, back)
        Screen.CONFIDENCE -> ConfidenceScreen(store, back)
        Screen.FOCUS -> FocusTestScreen(store, back)
        Screen.SETTINGS -> SettingsScreen(store, back)
    }
}
