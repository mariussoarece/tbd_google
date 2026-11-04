package ro.vishrama.tobedone

import android.Manifest
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.notifications.ReminderScheduler
import ro.vishrama.tobedone.ui.AppRoot
import ro.vishrama.tobedone.ui.theme.ToBeDoneTheme
import ro.vishrama.tobedone.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // iOS asks for notification permission at launch; do the same on Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && savedInstanceState == null) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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
                AppRoot(store)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Like iOS: rebuild reminders every time the app comes to the screen
        ReminderScheduler.refresh(this)
    }
}
