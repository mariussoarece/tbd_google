@file:OptIn(ExperimentalLayoutApi::class)

package ro.vishrama.tobedone.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.ExportImport
import ro.vishrama.tobedone.data.JournalEntry
import ro.vishrama.tobedone.data.Mood
import ro.vishrama.tobedone.data.RichText
import ro.vishrama.tobedone.data.TextAttr
import ro.vishrama.tobedone.ui.DetailScreen
import ro.vishrama.tobedone.ui.GroupCard
import ro.vishrama.tobedone.ui.GroupRow
import ro.vishrama.tobedone.ui.ProminentButton
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.formatDateTime
import ro.vishrama.tobedone.ui.groupedList
import ro.vishrama.tobedone.ui.theme.IOS
import ro.vishrama.tobedone.ui.theme.LocalGrouped

/** Red used for coloured journal text (readable in light and dark). */
private val JournalRed = Color(0xFFE53935)

/** Journaling: mood + formatted text per entry, search through all entries, export. */
@Composable
fun JournalScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val g = LocalGrouped.current

    var editing by rememberSaveable { mutableStateOf<String?>(null) }   // "new" or an entry id
    var query by rememberSaveable { mutableStateOf("") }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(ExportImport.exportJournalHtml(store.data.value.journalEntries).toByteArray(Charsets.UTF_8))
                }
            }.isSuccess
            Toast.makeText(context, if (ok) "Journal exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    val editingKey = editing
    if (editingKey != null) {
        val entry = data.journalEntries.firstOrNull { it.id == editingKey }
        JournalEditor(
            entry = entry,
            onCancel = { editing = null },
            onSave = { mood, body ->
                if (entry == null) store.addJournal(mood, body) else store.updateJournal(entry.id, mood, body)
                editing = null
            },
        )
        return
    }

    val all = data.journalEntries.sortedByDescending { it.date }
    val q = query.trim()
    val shown = if (q.isEmpty()) all else all.filter {
        it.body.text.contains(q, ignoreCase = true) || it.mood.label.contains(q, ignoreCase = true)
    }

    DetailScreen("Journaling", IOS.Teal, onBack) {
        // New Entry: blue button with white text and the pencil-on-paper icon
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(IOS.Blue)
                        .clickable { editing = "new" }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Entry", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Search through all entries
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(g.fill)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = g.secondaryText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search in all entries", color = g.secondaryText, fontSize = 16.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(IOS.Blue),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Close, contentDescription = "Clear search", tint = g.secondaryText,
                        modifier = Modifier.size(20.dp).clip(CircleShape).clickable { query = "" },
                    )
                }
            }
        }

        item {
            SectionHeader(
                if (q.isEmpty()) "Entries"
                else "${shown.size} entr" + (if (shown.size == 1) "y" else "ies") + " with “$q”",
            )
        }
        groupedList(
            shown, { it.id },
            emptyText = if (q.isEmpty()) "No entries yet. Tap New Entry to write about your day." else "Nothing found.",
            onDelete = { store.deleteJournal(it.id) },
        ) { e ->
            GroupRow(onClick = { editing = e.id }) {
                Text(e.mood.emoji, fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.mood.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(formatDateTime(e.date), fontSize = 12.sp, color = g.secondaryText)
                    }
                    Text(
                        e.body.toAnnotated(highlight = q),
                        fontSize = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        item {
            Row(Modifier.padding(top = 16.dp)) {
                ProminentButton("Export Journal", enabled = all.isNotEmpty()) { exporter.launch("journal_export.html") }
            }
        }
        item {
            Text(
                "Swipe left on an entry to delete it. Export saves all entries as a web page (HTML) you can open anywhere.",
                fontSize = 13.sp, color = g.secondaryText,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}

/** Builds the styled text; [highlight] marks search matches in yellow. */
fun RichText.toAnnotated(highlight: String = ""): AnnotatedString = buildAnnotatedString {
    append(text)
    val n = text.length
    fun each(list: List<ro.vishrama.tobedone.data.StyleRange>, style: SpanStyle) =
        list.forEach { r ->
            val s = r.start.coerceIn(0, n)
            val e = r.end.coerceIn(0, n)
            if (s < e) addStyle(style, s, e)
        }
    each(bold, SpanStyle(fontWeight = FontWeight.Bold))
    each(italic, SpanStyle(fontStyle = FontStyle.Italic))
    each(underline, SpanStyle(textDecoration = TextDecoration.Underline))
    each(red, SpanStyle(color = JournalRed))
    if (highlight.isNotEmpty()) {
        var i = text.indexOf(highlight, ignoreCase = true)
        while (i >= 0) {
            addStyle(SpanStyle(background = IOS.Yellow.copy(alpha = 0.45f)), i, i + highlight.length)
            i = text.indexOf(highlight, i + highlight.length, ignoreCase = true)
        }
    }
}

// =====================================================================
//  New / edit entry
// =====================================================================

@Composable
private fun JournalEditor(entry: JournalEntry?, onCancel: () -> Unit, onSave: (Mood, RichText) -> Unit) {
    val g = LocalGrouped.current
    var mood by remember { mutableStateOf(entry?.mood ?: Mood.GOOD) }
    var rich by remember { mutableStateOf(entry?.body ?: RichText()) }
    var selection by remember { mutableStateOf(TextRange(rich.text.length)) }
    var composition by remember { mutableStateOf<TextRange?>(null) }
    // Styles for the next characters typed (when nothing is selected)
    var typing by remember { mutableStateOf(rich.attrsAt(rich.text.length)) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val changed = rich != (entry?.body ?: RichText()) || mood != (entry?.mood ?: Mood.GOOD)
    fun close() { if (changed) confirmDiscard = true else onCancel() }
    BackHandler { close() }

    fun isOn(a: TextAttr): Boolean =
        if (selection.collapsed) a in typing else rich.covers(a, selection.min, selection.max)

    fun toggle(a: TextAttr) {
        if (selection.collapsed) typing = if (a in typing) typing - a else typing + a
        else rich = rich.toggle(a, selection.min, selection.max)
    }

    fun setRed(on: Boolean) {
        if (selection.collapsed) typing = if (on) typing + TextAttr.RED else typing - TextAttr.RED
        else rich = rich.set(TextAttr.RED, selection.min, selection.max, on)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(g.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { close() }) { Text("Cancel", color = IOS.Blue, fontSize = 17.sp) }
            Text(
                if (entry == null) "New Entry" else "Edit Entry",
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            ProminentButton("Save", enabled = rich.text.isNotBlank()) { onSave(mood, rich) }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionHeader("How is your mood?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.entries.forEach { m ->
                    val sel = m == mood
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) IOS.Blue else g.card)
                            .clickable { mood = m }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(m.emoji, fontSize = 16.sp)
                        Spacer(Modifier.width(5.dp))
                        Text(m.label, fontSize = 14.sp, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            SectionHeader("Your thoughts")
            // Formatting bar: Bold, Italic, Underline, black / red text
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(g.card)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FormatButton("B", isOn(TextAttr.BOLD), TextStyle(fontWeight = FontWeight.Bold)) { toggle(TextAttr.BOLD) }
                FormatButton("I", isOn(TextAttr.ITALIC), TextStyle(fontStyle = FontStyle.Italic)) { toggle(TextAttr.ITALIC) }
                FormatButton("U", isOn(TextAttr.UNDERLINE), TextStyle(textDecoration = TextDecoration.Underline)) { toggle(TextAttr.UNDERLINE) }
                Spacer(Modifier.weight(1f))
                ColorDot(MaterialTheme.colorScheme.onSurface, "Black text", selected = !isOn(TextAttr.RED)) { setRed(false) }
                ColorDot(JournalRed, "Red text", selected = isOn(TextAttr.RED)) { setRed(true) }
            }
            Spacer(Modifier.padding(top = 8.dp))

            // The text itself
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(g.card)
                    .padding(14.dp),
            ) {
                if (rich.text.isEmpty()) {
                    Text("Write about your day…", color = g.secondaryText, fontSize = 17.sp)
                }
                BasicTextField(
                    value = TextFieldValue(rich.toAnnotated(), selection, composition),
                    onValueChange = { v ->
                        if (v.text != rich.text) {
                            rich = rich.edited(v.text, typing)
                        } else if (v.selection != selection && v.selection.collapsed) {
                            // Cursor moved: continue with the style of the text there
                            typing = rich.attrsAt(v.selection.start)
                        }
                        selection = v.selection
                        composition = v.composition
                    },
                    textStyle = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 24.sp),
                    cursorBrush = SolidColor(IOS.Blue),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 232.dp),
                )
            }
            Text(
                "Select text and tap B, I, U or a colour to change it, or tap them first and then type.",
                fontSize = 12.sp, color = g.secondaryText,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("What you wrote in this entry will not be saved.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onCancel() }) { Text("Discard", color = IOS.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun FormatButton(label: String, on: Boolean, style: TextStyle, onClick: () -> Unit) {
    val g = LocalGrouped.current
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) IOS.Blue.copy(alpha = 0.2f) else g.fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 18.sp, style = style, color = if (on) IOS.Blue else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ColorDot(color: Color, description: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(if (selected) 2.5.dp else 0.dp, if (selected) IOS.Blue else Color.Transparent, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(6.dp)
            .clip(CircleShape)
            .background(color),
    )
}
