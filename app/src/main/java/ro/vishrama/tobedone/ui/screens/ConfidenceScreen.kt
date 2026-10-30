package ro.vishrama.tobedone.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.ConfidenceEntry
import ro.vishrama.tobedone.ui.DetailScreen
import ro.vishrama.tobedone.ui.GroupCard
import ro.vishrama.tobedone.ui.LineChart
import ro.vishrama.tobedone.ui.ProminentButton
import ro.vishrama.tobedone.ui.formatDateTime
import ro.vishrama.tobedone.ui.theme.IOS
import ro.vishrama.tobedone.ui.theme.LocalGrouped
import java.time.LocalDate

@Composable
fun ConfidenceScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val g = LocalGrouped.current

    var score by rememberSaveable { mutableIntStateOf(7) }
    var note by rememberSaveable { mutableStateOf("") }
    var showNote by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }

    val entries = data.confidenceEntries.sortedByDescending { it.date }

    DetailScreen(
        "Confidence", IOS.Red, onBack,
        actions = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Options", tint = IOS.Blue)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete all", color = IOS.Red) },
                        onClick = { menuOpen = false; confirmDeleteAll = true },
                    )
                }
            }
        },
    ) {
        // Header
        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("How confident do you feel right now?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("Tap a number or use the slider, then Save.", fontSize = 16.sp, color = g.secondaryText)
            }
        }

        // Input card
        item {
            Card {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (n in 1..10) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (score == n) IOS.Blue.copy(alpha = 0.25f) else g.fill)
                                .clickable { score = n }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("$n", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("1")
                    Slider(
                        value = score.toFloat(),
                        onValueChange = { score = it.toInt().coerceIn(1, 10) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = IOS.Blue, activeTrackColor = IOS.Blue),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text("10")
                }
                Text("Selected: $score / 10", fontSize = 13.sp, color = g.secondaryText)

                if (showNote) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(10.dp),
                    ) {
                        if (note.isEmpty()) Text("Optional note (e.g. why you feel this way)", color = g.secondaryText)
                        BasicTextField(
                            value = note,
                            onValueChange = { note = it },
                            textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(IOS.Blue),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showNote = !showNote }) {
                        Text(if (showNote) "Hide note" else "Add note", color = IOS.Blue)
                    }
                    Spacer(Modifier.weight(1f))
                    ProminentButton("✓  Save") {
                        store.addConfidence(score, note)
                        note = ""
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Chart: daily averages for the last 30 days
        item {
            val zone = AppStore.zone()
            val start = LocalDate.now(zone).minusDays(29)
            val daily = entries
                .filter { !AppStore.dayOf(it.date).isBefore(start) }
                .groupBy { AppStore.dayOf(it.date) }
                .map { (day, list) -> day.atStartOfDay(zone).toInstant().toEpochMilli().toDouble() to list.map { it.score }.average() }
            Card {
                Text("Last 30 Days", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (daily.isEmpty()) {
                    Text("Your daily averages will appear here.", color = g.secondaryText, fontSize = 15.sp)
                } else {
                    LineChart(daily, yMin = 1.0, yMax = 10.0, target = 7.0, targetLabel = "Target 7+")
                }
            }
        }

        // Stats
        item {
            val avg7 = averageSince(entries, 7)
            val avg30 = averageSince(entries, 30)
            val best = entries.maxOfOrNull { it.score }?.toDouble() ?: 0.0
            val worst = entries.minOfOrNull { it.score }?.toDouble() ?: 0.0
            val streak = streak(entries, 7)
            Card {
                Text("Stats", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Row { StatCell("7‑day avg", "%.1f".format(avg7)); StatCell("30‑day avg", "%.1f".format(avg30)) }
                Row { StatCell("Best day", "%.1f".format(best)); StatCell("Worst day", "%.1f".format(worst)) }
                Row { StatCell("Streak ≥7", "$streak day" + if (streak == 1) "" else "s") }
            }
        }

        // Recent entries
        item {
            Card {
                Text("Recent", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (entries.isEmpty()) {
                    Text("No entries yet. Log your first confidence check above.", color = g.secondaryText)
                } else {
                    entries.take(12).forEach { e -> RecentRow(e) { store.deleteConfidence(e.id) } }
                }
            }
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all confidence entries?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { store.deleteAllConfidence(); confirmDeleteAll = false }) {
                    Text("Delete All", color = IOS.Red)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        GroupCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatCell(title: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(title, fontSize = 12.sp, color = LocalGrouped.current.secondaryText)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecentRow(e: ConfidenceEntry, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val g = LocalGrouped.current
    Row(verticalAlignment = Alignment.Top) {
        Text("${e.score}", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f)) {
            Text(formatDateTime(e.date), fontSize = 15.sp, color = g.secondaryText)
            e.note?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 16.sp) }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "More", tint = g.secondaryText)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Delete", color = IOS.Red) }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

private fun averageSince(entries: List<ConfidenceEntry>, days: Long): Double {
    val start = LocalDate.now(AppStore.zone()).minusDays(days - 1)
    val vals = entries.filter { !AppStore.dayOf(it.date).isBefore(start) }.map { it.score }
    return if (vals.isEmpty()) 0.0 else vals.average()
}

/** Days in a row, up to today, with at least one score ≥ [min]. */
private fun streak(entries: List<ConfidenceEntry>, min: Int): Int {
    val byDay = entries.groupBy { AppStore.dayOf(it.date) }
    var day = LocalDate.now(AppStore.zone())
    var n = 0
    while (byDay[day]?.any { it.score >= min } == true) {
        n++
        day = day.minusDays(1)
    }
    return n
}
