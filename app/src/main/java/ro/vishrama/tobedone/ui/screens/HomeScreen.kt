package ro.vishrama.tobedone.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.R
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.ui.GroupCard
import ro.vishrama.tobedone.ui.IconTile
import ro.vishrama.tobedone.ui.RowDivider
import ro.vishrama.tobedone.ui.Screen
import ro.vishrama.tobedone.ui.compactNumber
import ro.vishrama.tobedone.ui.theme.IOS
import ro.vishrama.tobedone.ui.theme.LocalGrouped
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Home screen ("A · Clean iOS" design): greeting, Today card, PLAN / DO / GROW. */
@Composable
fun HomeScreen(store: AppStore, onOpen: (Screen) -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    val settings by store.settings.collectAsStateWithLifecycle()
    val g = LocalGrouped.current

    val zone = AppStore.zone()
    val today = LocalDate.now(zone)
    val endOfToday = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    // ---- numbers (same rules as iOS HomeView) ----
    val openTasks = data.tasks.count { !it.isCompleted && it.dueAt < endOfToday }
    val todayTasks = data.tasks.filter { AppStore.dayOf(it.dueAt) == today }
    val todayActivities = data.activities.filter { AppStore.dayOf(it.date) == today }
    val activitiesLeft = todayActivities.count { !it.isCompleted }
    val todayTotal = todayTasks.size + todayActivities.size
    val todayDone = todayTasks.count { it.isCompleted } + todayActivities.count { it.isCompleted }
    val progress = if (todayTotal > 0) todayDone.toFloat() / todayTotal else 0f
    val toBuy = data.shoppingItems.count { !it.isCompleted } + data.groceryItems.count { !it.isBought }

    val monthlyText = if (data.monthlyTasks.isEmpty()) null
    else "${compactNumber(data.monthlyTasks.sumOf { it.price })} ${settings.currency}"

    val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val recentConfidence = data.confidenceEntries.filter { it.date >= weekAgo }
    val confidenceText = if (recentConfidence.isEmpty()) null
    else String.format("%.1f", recentConfidence.map { it.score }.average())

    val focusText = data.focusResults.maxOfOrNull { it.duration }?.let { "Best ${formatBest(it)}" }

    fun count(n: Int) = if (n > 0) "$n" else null

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(g.background).windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Greeting + settings
        item {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")).uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = g.secondaryText,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Hi, ${settings.userName.ifBlank { "Friend" }}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = IOS.Pink, modifier = Modifier.size(26.dp))
                    }
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(g.card)
                        .clickable { onOpen(Screen.SETTINGS) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Settings", tint = IOS.Blue)
                }
            }
        }

        // Today card
        item {
            GroupCard {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Today", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (todayTotal > 0) {
                            Text("${(progress * 100).roundToInt()}%", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IOS.Blue)
                        }
                    }
                    if (todayTotal > 0) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = IOS.Blue,
                            trackColor = g.fill,
                            strokeCap = StrokeCap.Round,
                            drawStopIndicator = {},
                        )
                    } else {
                        Text("Nothing planned for today.", fontSize = 15.sp, color = g.secondaryText)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Stat(openTasks, "tasks due")
                        Stat(activitiesLeft, "activities left")
                        Stat(toBuy, "to buy")
                    }
                }
            }
        }

        item { HomeHeader("PLAN") }
        item {
            GroupCard {
                HomeRow("To Do", Icons.Filled.Checklist, IOS.Blue, count(openTasks)) { onOpen(Screen.TODO) }
                RowDivider(58.dp)
                HomeRow("Monthly Expenses", Icons.Filled.CalendarMonth, IOS.Orange, monthlyText) { onOpen(Screen.MONTHLY) }
                RowDivider(58.dp)
                HomeRow("Projects", Icons.Filled.TrackChanges, IOS.Green, count(data.projects.count { !it.isCompleted })) { onOpen(Screen.PROJECTS) }
            }
        }

        item { HomeHeader("DO") }
        item {
            GroupCard {
                HomeRow("Shopping", Icons.Filled.ShoppingCart, IOS.Red, count(data.shoppingItems.count { !it.isCompleted })) { onOpen(Screen.SHOPPING) }
                RowDivider(58.dp)
                HomeRow("Grocery", Icons.Filled.ShoppingBasket, IOS.Yellow, count(data.groceryItems.count { !it.isBought })) { onOpen(Screen.GROCERY) }
            }
        }

        item { HomeHeader("GROW") }
        item {
            GroupCard {
                HomeRow("Daily Routine", Icons.Filled.Schedule, IOS.Indigo, if (activitiesLeft > 0) "$activitiesLeft left" else null) { onOpen(Screen.ROUTINE) }
                RowDivider(58.dp)
                HomeRow("Confidence", Icons.Filled.Favorite, IOS.Pink, confidenceText) { onOpen(Screen.CONFIDENCE) }
                RowDivider(58.dp)
                HomeRow("Focus Test", Icons.Filled.Visibility, IOS.Blue, focusText) { onOpen(Screen.FOCUS) }
            }
        }

        // Handwritten footer
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(R.drawable.you_got_this),
                    contentDescription = "You got this!",
                    modifier = Modifier.width(152.dp),
                    colorFilter = if (g.isDark) ColorFilter.tint(Color(0xFF8AB4F8)) else null,
                )
            }
        }
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    val g = LocalGrouped.current
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) { append("$value") }
            withStyle(SpanStyle(color = g.secondaryText)) { append(" $label") }
        },
        fontSize = 15.sp,
    )
}

@Composable
private fun HomeHeader(text: String) {
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = LocalGrouped.current.secondaryText,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun HomeRow(title: String, icon: ImageVector, color: Color, detail: String?, onClick: () -> Unit) {
    val g = LocalGrouped.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, color)
        Text(title, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (detail != null) Text(detail, fontSize = 17.sp, color = g.secondaryText)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = g.separator)
    }
}

/** Best result as minutes:seconds, e.g. "3:19s" (iOS FocusResult.formatBest). */
fun formatBest(seconds: Double): String {
    val total = seconds.roundToInt()
    return String.format("%d:%02ds", total / 60, total % 60)
}

/** Seconds, e.g. "42.3 s" (iOS FocusResult.format). */
fun formatSeconds(seconds: Double): String = String.format("%.1f s", seconds)
