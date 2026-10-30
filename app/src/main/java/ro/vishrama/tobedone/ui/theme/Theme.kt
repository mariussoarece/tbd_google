package ro.vishrama.tobedone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ro.vishrama.tobedone.data.ThemeSetting

// Yellow from the app icon, used as the accent.
private val Yellow = Color(0xFFF5B800)
private val YellowDark = Color(0xFF7A5C00)
private val Ink = Color(0xFF1C1B1F)

private val LightColors = lightColorScheme(
    primary = YellowDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE08A),
    onPrimaryContainer = Ink,
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Yellow,
    onPrimary = Ink,
    primaryContainer = Color(0xFF5C4500),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondaryContainer = Color(0xFF5C4500),
    onSecondaryContainer = Color(0xFFFFE08A),
)

/** Completed-item green, same role as iOS `.green`. */
val DoneGreen = Color(0xFF2E9D4F)

@Composable
fun ToBeDoneTheme(setting: ThemeSetting, content: @Composable () -> Unit) {
    // Same behaviour as the iOS app: "Dark" → dark, "Light" → light,
    // "Default" → follow the phone's system setting.
    val dark = when (setting) {
        ThemeSetting.DARK -> true
        ThemeSetting.LIGHT -> false
        ThemeSetting.DEFAULT -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
