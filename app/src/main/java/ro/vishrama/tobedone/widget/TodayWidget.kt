package ro.vishrama.tobedone.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ro.vishrama.tobedone.MainActivity
import ro.vishrama.tobedone.R
import ro.vishrama.tobedone.data.Agenda
import ro.vishrama.tobedone.data.AgendaItem
import ro.vishrama.tobedone.data.AgendaKind
import ro.vishrama.tobedone.data.AppStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Home-screen widget: everything happening today (To Do, Monthly Expenses,
 * Daily Routine) ordered by time, with a + button that opens the Home screen.
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = AppStore.get(context)
        provideContent {
            val data by store.data.collectAsState()
            val settings by store.settings.collectAsState()
            val today = LocalDate.now(AppStore.zone())
            GlanceTheme {
                // Only what's left from the current hour onwards
                WidgetContent(context, today, Agenda.fromCurrentHour(data), settings.currency, faceFor(data))
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** Redraw all ToBeDone widgets (called after every change in the app). */
        fun requestUpdate(context: Context) {
            val app = context.applicationContext
            if (AppStore.get(app).isDemo) return   // screenshot mode
            scope.launch { runCatching { TodayWidget().updateAll(app) } }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

private val Blue = Color(0xFF007AFF)
private val Orange = Color(0xFFFF9500)
private val Indigo = Color(0xFF5856D6)

/**
 * Face for the widget background from the Confidence level (7-day average, as on Home):
 * 8–10 big smile, 5–7 smile, 0–4 serious. No entries in the last 7 days: no face.
 */
private fun faceFor(data: ro.vishrama.tobedone.data.AppData): Int? {
    val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val recent = data.confidenceEntries.filter { it.date >= weekAgo }
    if (recent.isEmpty()) return null
    val level = recent.map { it.score }.average()
    return when {
        level >= 7.5 -> R.drawable.face_big_smile
        level >= 4.5 -> R.drawable.face_smile
        else -> R.drawable.face_serious
    }
}

private fun openIntent(context: Context, screen: String?): Action {
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    if (screen != null) intent.putExtra(MainActivity.EXTRA_OPEN, screen)
    return actionStartActivity(intent)
}

@Composable
private fun WidgetContent(context: Context, today: LocalDate, agenda: List<AgendaItem>, currency: String, face: Int?) {
    val openApp = openIntent(context, null)
    val openHome = openIntent(context, "HOME")   // the + button: Home screen with all tasks

    Box(
        GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(18.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
    // Big faint face in the background, from the Confidence level
    if (face != null) {
        Image(
            provider = ImageProvider(face),
            contentDescription = null,
            modifier = GlanceModifier.size(120.dp).padding(6.dp),
        )
    }
    Column(GlanceModifier.fillMaxSize().padding(12.dp)) {
        // Header: "Today" + date, and the + button
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight().clickable(openApp)) {
                Text(
                    "Today",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
                )
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
            Box(
                GlanceModifier
                    .size(36.dp)
                    .cornerRadius(18.dp)
                    .background(ColorProvider(Blue))
                    .clickable(openHome),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White)))
            }
        }

        Spacer(GlanceModifier.height(8.dp))

        if (agenda.isEmpty()) {
            Text(
                "Nothing more planned for today.",
                modifier = GlanceModifier.clickable(openApp),
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        } else {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(agenda, itemId = { (it.kind.name + it.id).hashCode().toLong() }) { a ->
                    WidgetRow(context, a, currency)
                }
            }
        }
    }
    }
}

@Composable
private fun WidgetRow(context: Context, a: AgendaItem, currency: String) {
    val color = when (a.kind) {
        AgendaKind.TASK -> Blue
        AgendaKind.MONTHLY -> Orange
        AgendaKind.ACTIVITY -> Indigo
    }
    val screen = when (a.kind) {
        AgendaKind.TASK -> "TODO"
        AgendaKind.MONTHLY -> "MONTHLY"
        AgendaKind.ACTIVITY -> "ROUTINE"
    }
    val detail = when (a.kind) {
        AgendaKind.TASK -> "To Do"
        AgendaKind.MONTHLY -> "Monthly" + (if (a.price != null && a.price > 0) " · " + String.format("%.2f", a.price) + " " + currency else "")
        AgendaKind.ACTIVITY -> "Routine" + (if (a.durationMinutes > 0) " · ${a.durationMinutes} min" else "")
    }

    Row(
        GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(openIntent(context, screen)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            a.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
            modifier = GlanceModifier.width(44.dp),
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
        Box(GlanceModifier.width(3.dp).height(26.dp).cornerRadius(2.dp).background(ColorProvider(color))) {}
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                (if (a.done) "✓ " else "") + a.title,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = if (a.done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                    textDecoration = if (a.done) TextDecoration.LineThrough else null,
                ),
            )
            Text(detail, maxLines = 1, style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant))
        }
    }
}
