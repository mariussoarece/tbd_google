package ro.vishrama.tobedone

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.notifications.ReminderScheduler
import ro.vishrama.tobedone.ui.AppRoot
import ro.vishrama.tobedone.ui.Screen
import ro.vishrama.tobedone.ui.theme.ToBeDoneTheme
import ro.vishrama.tobedone.ui.theme.isDarkTheme
import ro.vishrama.tobedone.widget.TodayWidget

class MainActivity : ComponentActivity() {

    /** Screen asked for by the widget, e.g. "TODO" for the + button. */
    private val requestedScreen = mutableStateOf<Screen?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // iOS asks for notification permission at launch; do the same on Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && savedInstanceState == null) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (savedInstanceState == null) requestedScreen.value = screenFrom(intent)

        val store = AppStore.get(this)
        setContent {
            val settings by store.settings.collectAsStateWithLifecycle()
            val dark = isDarkTheme(settings.theme)

            // Keep status/navigation bar icons readable when Light or Dark is forced
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            ToBeDoneTheme(settings.theme) {
                AppRoot(store, requestedScreen.value) { requestedScreen.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedScreen.value = screenFrom(intent)
    }

    private fun screenFrom(intent: Intent?): Screen? =
        intent?.getStringExtra(EXTRA_OPEN)?.let { name -> Screen.entries.firstOrNull { it.name == name } }

    override fun onResume() {
        super.onResume()
        // Like iOS: rebuild reminders every time the app comes to the screen
        ReminderScheduler.refresh(this)
        TodayWidget.requestUpdate(this)
    }

    companion object {
        /** Intent extra with a [Screen] name to open, used by the widget. */
        const val EXTRA_OPEN = "open"
    }
}
