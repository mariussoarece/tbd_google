package ro.vishrama.tobedone.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.R
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.ui.DetailScreen
import ro.vishrama.tobedone.ui.GroupCard
import ro.vishrama.tobedone.ui.GroupRow
import ro.vishrama.tobedone.ui.LineChart
import ro.vishrama.tobedone.ui.ProminentButton
import ro.vishrama.tobedone.ui.RowDivider
import ro.vishrama.tobedone.ui.SectionFooter
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.SwipeToDelete
import ro.vishrama.tobedone.ui.formatDate
import ro.vishrama.tobedone.ui.groupClip
import ro.vishrama.tobedone.ui.formatTime
import ro.vishrama.tobedone.ui.theme.IOS
import ro.vishrama.tobedone.ui.theme.LocalGrouped
import ro.vishrama.tobedone.ui.toLocalDateTime

private const val INSTRUCTIONS = """Look at the red dot without blinking and without moving your eyes as much as you can.

When you blink, tap the screen. The time is recorded so you can compare it with future tests.

Come back in a week and test again to see how your attention span improves."""

private const val EXPLANATION = """Look at the red dot without blinking and without moving your eyes as much as you can.

When you blink, tap the screen. The time will be recorded and you can compare it with your future tests.

Come again in a week and retest your attention span. After 1 week of using this app, you will see improvements in your focus.

Maintaining gaze on a fixed point improves:
• continuous micro‑attention (microsaccades)
• suppression of reflexive eye movements
• resistance to boredom
• mental stability

When your mind wanders, your eyes drift.
When you're tired, your microsaccades increase.
When you're stressed, fixation becomes unstable."""

// =====================================================================
//  First launch: Caveat page → explanation → red dot test → Home
// =====================================================================

@Composable
fun IntroFlow(store: AppStore) {
    var step by rememberSaveable { mutableStateOf(0) }
    when (step) {
        0 -> IntroPage(store) { step = 1 }
        1 -> FocusExplanation(onBack = { step = 0 }) { step = 2 }
        else -> RedDotTest(
            onFinish = { seconds ->
                store.addFocusResult(seconds)
                store.setHasSeenIntro(true)
            },
            onCancel = { step = 1 },
        )
    }
}

@Composable
private fun IntroPage(store: AppStore, onContinue: () -> Unit) {
    val settings by store.settings.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf(settings.userName) }
    val g = LocalGrouped.current

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Caveat", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(
            buildAnnotatedString {
                append("This app will help you — it really will — but the real change starts with you.\nIf you decide ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("right now") }
                append(" to consciously guide your thoughts and actions, the app becomes a powerful tool instead of just doing half the work.\n\n")
                append("Motivate your mind to act on the things you want to do.\nEvery positive thought builds an action, every positive action builds confidence.\nThis is the flow that you need to carry on all your life.")
            },
            fontSize = 17.sp,
        )
        Text("Let's begin! What should we call you?", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(g.card)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (name.isEmpty()) Text("Enter your name", color = g.secondaryText, fontSize = 17.sp)
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(IOS.Blue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { store.setUserName(name.trim()) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ProminentButton("Continue") {
            store.setUserName(name.trim())
            onContinue()
        }
        Image(
            painterResource(R.drawable.small_steps_big_changes),
            contentDescription = "Small steps, big changes",
            modifier = Modifier.width(150.dp),
        )
    }
}

@Composable
private fun FocusExplanation(onBack: () -> Unit, onStart: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Focus Test Instructions", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(EXPLANATION, fontSize = 17.sp)
        ProminentButton("Start Test", onClick = onStart)
    }
}

// =====================================================================
//  The test itself: black screen, red dot, tap when you blink
// =====================================================================

@Composable
fun RedDotTest(onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    var elapsed by remember { mutableDoubleStateOf(0.0) }
    var finished by remember { mutableStateOf(false) }
    val start = remember { System.nanoTime() }

    // Keep the screen awake during the test
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Real elapsed time, updated every frame
    LaunchedEffect(Unit) {
        while (!finished) {
            withFrameMillis { }
            elapsed = (System.nanoTime() - start) / 1e9
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!finished) {
                    finished = true
                    onFinish((System.nanoTime() - start) / 1e9)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(150.dp).clip(CircleShape).background(Color.Red))
        Text(
            String.format("%.2f s", elapsed),
            color = Color.White,
            fontSize = 34.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
        )
    }
}

// =====================================================================
//  Focus Test screen (from Home): best result, start button, history
// =====================================================================

@Composable
fun FocusTestScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    var testing by rememberSaveable { mutableStateOf(false) }
    var showInstructions by rememberSaveable { mutableStateOf(false) }
    val g = LocalGrouped.current

    if (testing) {
        RedDotTest(
            onFinish = { store.addFocusResult(it); testing = false },
            onCancel = { testing = false },
        )
        return
    }

    val results = data.focusResults.sortedByDescending { it.date }
    val best = results.maxOfOrNull { it.duration }

    DetailScreen("Focus Test", IOS.Blue, onBack) {
        item {
            GroupCard {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    FocusStat("Best", best?.let { formatBest(it) } ?: "–", Modifier.weight(1f))
                    FocusStat("Last", results.firstOrNull()?.let { formatSeconds(it.duration) } ?: "–", Modifier.weight(1f))
                    FocusStat("Tests", "${results.size}", Modifier.weight(1f))
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ProminentButton("Start Test", modifier = Modifier.fillMaxWidth().height(48.dp)) { testing = true }
                TextButton(onClick = { showInstructions = true }) { Text("How the test works", color = IOS.Blue) }
            }
        }

        if (results.size >= 2) {
            item { SectionHeader("Progress") }
            item {
                GroupCard {
                    LineChart(
                        points = results.map { it.date.toDouble() to it.duration },
                        height = 160.dp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        item { SectionHeader("History") }
        if (results.isEmpty()) {
            item {
                GroupCard { GroupRow { Text("No results yet. Start your first test above.", color = g.secondaryText) } }
            }
        } else {
            itemsIndexed(results, key = { _, r -> r.id }) { i, r ->
                if (i > 0) RowDivider()
                SwipeToDelete(onDelete = { store.deleteFocusResult(r.id) }, modifier = Modifier.groupClip(i, results.size)) {
                    GroupRow {
                        Column(Modifier.weight(1f)) {
                            Text(formatDate(r.date), fontSize = 17.sp)
                            Text(formatTime(toLocalDateTime(r.date).toLocalTime()), fontSize = 12.sp, color = g.secondaryText)
                        }
                        if (r.duration == best) Icon(Icons.Filled.Star, contentDescription = "Best result", tint = IOS.Yellow)
                        Text(formatSeconds(r.duration), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            item { SectionFooter("Swipe left on a result to delete it.") }
        }
    }

    if (showInstructions) {
        AlertDialog(
            onDismissRequest = { showInstructions = false },
            title = { Text("How the test works") },
            text = { Text(INSTRUCTIONS) },
            confirmButton = { TextButton(onClick = { showInstructions = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun FocusStat(title: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(title, fontSize = 12.sp, color = LocalGrouped.current.secondaryText)
    }
}
