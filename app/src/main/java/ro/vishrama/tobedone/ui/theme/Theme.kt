package ro.vishrama.tobedone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ro.vishrama.tobedone.data.ThemeSetting

/** The iOS system colours used by the iPhone app, so both apps look the same. */
object IOS {
    val Blue = Color(0xFF007AFF)
    val Orange = Color(0xFFFF9500)
    val Green = Color(0xFF34C759)
    val Red = Color(0xFFFF3B30)
    val Yellow = Color(0xFFFFCC00)
    val Indigo = Color(0xFF5856D6)
    val Pink = Color(0xFFFF2D55)
    val Purple = Color(0xFFAF52DE)
}

/** Grouped-list colours (iOS "systemGroupedBackground" family). */
@Immutable
data class GroupedColors(
    val background: Color,
    val card: Color,
    val fill: Color,           // small chips, unselected buttons
    val separator: Color,
    val secondaryText: Color,
    val isDark: Boolean,
)

private val LightGrouped = GroupedColors(
    background = Color(0xFFF2F2F7),
    card = Color.White,
    fill = Color(0xFFE5E5EA),
    separator = Color(0xFFC6C6C8),
    secondaryText = Color(0xFF8A8A8E),
    isDark = false,
)

private val DarkGrouped = GroupedColors(
    background = Color.Black,
    card = Color(0xFF1C1C1E),
    fill = Color(0xFF2C2C2E),
    separator = Color(0xFF38383A),
    secondaryText = Color(0xFF8D8D93),
    isDark = true,
)

val LocalGrouped = staticCompositionLocalOf { LightGrouped }

private val LightColors = lightColorScheme(
    primary = IOS.Blue,
    onPrimary = Color.White,
    background = LightGrouped.background,
    surface = LightGrouped.card,
    surfaceContainer = LightGrouped.card,
    surfaceContainerHigh = LightGrouped.card,
    surfaceContainerHighest = LightGrouped.fill,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = LightGrouped.secondaryText,
    outline = LightGrouped.separator,
    outlineVariant = LightGrouped.separator,
    error = IOS.Red,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    background = DarkGrouped.background,
    surface = DarkGrouped.card,
    surfaceContainer = DarkGrouped.card,
    surfaceContainerHigh = DarkGrouped.card,
    surfaceContainerHighest = DarkGrouped.fill,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = DarkGrouped.secondaryText,
    outline = DarkGrouped.separator,
    outlineVariant = DarkGrouped.separator,
    error = Color(0xFFFF453A),
)

@Composable
fun isDarkTheme(setting: ThemeSetting): Boolean = when (setting) {
    ThemeSetting.DARK -> true
    ThemeSetting.LIGHT -> false
    ThemeSetting.DEFAULT -> isSystemInDarkTheme()
}

@Composable
fun ToBeDoneTheme(setting: ThemeSetting, content: @Composable () -> Unit) {
    // "Default" follows the phone; "Light"/"Dark" force a mode — same as iOS.
    val dark = isDarkTheme(setting)
    CompositionLocalProvider(LocalGrouped provides if (dark) DarkGrouped else LightGrouped) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
    }
}
