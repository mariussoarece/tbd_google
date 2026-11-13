package ro.vishrama.tobedone.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import ro.vishrama.tobedone.notifications.ReminderScheduler
import ro.vishrama.tobedone.widget.TodayWidget
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Single source of truth for the whole app (replaces SwiftData + @AppStorage).
 * Lists live in memory as a StateFlow and are saved to one JSON file on the phone.
 */
class AppStore private constructor(private val appContext: Context) {

    private val file = AtomicFile(File(appContext.filesDir, "tobedone.json"))
    private val writer = Executors.newSingleThreadExecutor()
    private val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    // ================= Settings =================

    fun setTheme(v: ThemeSetting) = updateSettings { it.copy(theme = v) }
    fun setCurrency(v: String) = updateSettings { it.copy(currency = v) }
    fun setUserName(v: String) = updateSettings { it.copy(userName = v) }
    fun setHasSeenIntro(v: Boolean) = updateSettings { it.copy(hasSeenIntro = v) }
    fun setDailySummary(v: Boolean) = updateSettings(refresh = true) { it.copy(dailySummaryEnabled = v) }
    fun setActivityStart(v: Boolean) = updateSettings(refresh = true) { it.copy(activityStartEnabled = v) }
    fun setReminderTime(hour: Int, minute: Int) =
        updateSettings(refresh = true) { it.copy(reminderHour = hour, reminderMinute = minute) }

    // ================= To Do =================

    fun addTask(title: String, dueAt: Long, priority: Priority, reminderMinutes: Int? = null) = mutate(refresh = true) {
        // Snap to the start of the minute, like iOS
        val t = DueTask(
            title = title.trim(), dueAt = dueAt / 60_000L * 60_000L, priority = priority,
            reminderMinutes = reminderMinutes?.takeIf { m -> m > 0 },
        )
        it.copy(tasks = it.tasks + t)
    }

    fun editTask(id: String, title: String, dueAt: Long, priority: Priority, reminderMinutes: Int?) = mutate(refresh = true) { d ->
        d.copy(tasks = d.tasks.map {
            if (it.id == id) it.copy(
                title = title.trim(), dueAt = dueAt / 60_000L * 60_000L, priority = priority,
                reminderMinutes = reminderMinutes?.takeIf { m -> m > 0 },
            ) else it
        })
    }

    fun toggleTask(id: String) = mutate(refresh = true) { d ->
        d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it })
    }

    fun deleteTask(id: String) = mutate(refresh = true) { d -> d.copy(tasks = d.tasks.filterNot { it.id == id }) }

    // ================= Shopping =================

    fun addShoppingItem(name: String, price: Double, priority: Priority) = mutate {
        val item = ShoppingItem(name = name.trim(), price = price, currency = _settings.value.currency, priority = priority)
        it.copy(shoppingItems = it.shoppingItems + item)
    }

    /** "Edit Price" → Confirm on iOS: optionally change the price and mark as bought. */
    fun buyShoppingItem(id: String, newPrice: Double?) = mutate { d ->
        d.copy(shoppingItems = d.shoppingItems.map {
            if (it.id == id) it.copy(
                price = newPrice ?: it.price,
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
            ) else it
        })
    }

    fun toggleShoppingItem(id: String) = mutate { d ->
        d.copy(shoppingItems = d.shoppingItems.map {
            if (it.id == id) {
                val done = !it.isCompleted
                it.copy(isCompleted = done, completedAt = if (done) System.currentTimeMillis() else null)
            } else it
        })
    }

    fun editShoppingItem(id: String, name: String, price: Double, priority: Priority) = mutate { d ->
        d.copy(shoppingItems = d.shoppingItems.map {
            if (it.id == id) it.copy(name = name.trim(), price = price, priority = priority) else it
        })
    }

    fun deleteShoppingItem(id: String) = mutate { d ->
        d.copy(shoppingItems = d.shoppingItems.filterNot { it.id == id })
    }

    // ================= Monthly =================

    fun addMonthlyTask(title: String, price: Double, dayOfMonth: Int, hour: Int, minute: Int) = mutate(refresh = true) {
        it.copy(monthlyTasks = it.monthlyTasks + MonthlyTask(
            title = title.trim(), price = price,
            dayOfMonth = dayOfMonth.coerceIn(1, 28), hour = hour, minute = minute,
        ))
    }

    fun updateMonthlyTask(id: String, title: String, price: Double, dayOfMonth: Int, hour: Int, minute: Int) =
        mutate(refresh = true) { d ->
            d.copy(monthlyTasks = d.monthlyTasks.map {
                if (it.id == id) it.copy(
                    title = title.trim(), price = price,
                    dayOfMonth = dayOfMonth.coerceIn(1, 28), hour = hour, minute = minute,
                ) else it
            })
        }

    /** Marks a Monthly Expense as done (paid) for this month, or undoes it. */
    fun toggleMonthlyPaid(id: String) = mutate(refresh = true) { d ->
        val month = java.time.YearMonth.now(zone()).toString()
        d.copy(monthlyTasks = d.monthlyTasks.map {
            if (it.id == id) it.copy(paidMonth = if (it.paidMonth == month) null else month) else it
        })
    }

    /** Finished for good (moves to Completed), or back to the active list. */
    fun toggleMonthlyCompleted(id: String) = mutate(refresh = true) { d ->
        d.copy(monthlyTasks = d.monthlyTasks.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it })
    }

    fun deleteMonthlyTask(id: String) = mutate(refresh = true) { d ->
        d.copy(monthlyTasks = d.monthlyTasks.filterNot { it.id == id })
    }

    // ================= Grocery =================

    fun addGroceryItem(name: String) = mutate { it.copy(groceryItems = it.groceryItems + GroceryItem(name = name.trim())) }

    fun toggleGroceryItem(id: String) = mutate { d ->
        d.copy(groceryItems = d.groceryItems.map { if (it.id == id) it.copy(isBought = !it.isBought) else it })
    }

    fun editGroceryItem(id: String, name: String) = mutate { d ->
        d.copy(groceryItems = d.groceryItems.map { if (it.id == id) it.copy(name = name.trim()) else it })
    }

    fun deleteGroceryItem(id: String) = mutate { d -> d.copy(groceryItems = d.groceryItems.filterNot { it.id == id }) }

    // ================= Projects =================

    fun addProject(title: String, start: Long? = null, end: Long? = null) = mutate(refresh = true) {
        it.copy(projects = it.projects + Project(title = title.trim(), startDate = start, endDate = clampEnd(start, end)))
    }

    fun editProject(id: String, title: String, start: Long?, end: Long?) = updateProject(id) {
        it.copy(title = title.trim(), startDate = start, endDate = clampEnd(start, end))
    }

    fun toggleProject(id: String) = mutate(refresh = true) { d ->
        d.copy(projects = d.projects.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it })
    }

    fun deleteProject(id: String) = mutate(refresh = true) { d -> d.copy(projects = d.projects.filterNot { it.id == id }) }

    fun addSubtask(projectId: String, title: String, start: Long? = null, end: Long? = null) = updateProject(projectId) {
        it.copy(subtasks = it.subtasks + Subtask(title = title.trim(), startDate = start, endDate = clampEnd(start, end)))
    }

    fun editSubtask(projectId: String, subtaskId: String, title: String, start: Long?, end: Long?) = updateProject(projectId) { p ->
        p.copy(subtasks = p.subtasks.map {
            if (it.id == subtaskId) it.copy(title = title.trim(), startDate = start, endDate = clampEnd(start, end)) else it
        })
    }

    /** The end can't be before the start (same as iOS). */
    private fun clampEnd(start: Long?, end: Long?): Long? = if (end != null && start != null) maxOf(end, start) else end

    fun toggleSubtask(projectId: String, subtaskId: String) = updateProject(projectId) { p ->
        p.copy(subtasks = p.subtasks.map { if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it })
    }

    fun deleteSubtask(projectId: String, subtaskId: String) = updateProject(projectId) { p ->
        p.copy(subtasks = p.subtasks.filterNot { it.id == subtaskId })
    }

    private fun updateProject(id: String, block: (Project) -> Project) = mutate(refresh = true) { d ->
        d.copy(projects = d.projects.map { if (it.id == id) block(it) else it })
    }

    // ================= Daily Routine =================

    fun addActivities(items: List<Activity>) = mutate(refresh = true) { d ->
        syncConfidence(d.copy(activities = d.activities + items), items.map { dayOf(it.date) }.toSet())
    }

    fun toggleActivity(id: String) = mutate(refresh = true) { d ->
        val item = d.activities.firstOrNull { it.id == id } ?: return@mutate d
        val updated = d.copy(activities = d.activities.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it })
        syncConfidence(updated, setOf(dayOf(item.date)))
    }

    enum class EditScope { THIS_ONE, FOLLOWING, ALL }

    /** How many activities belong to the same repeating series as [id] (1 if it doesn't repeat). */
    fun seriesSize(id: String): Int {
        val a = _data.value.activities.firstOrNull { it.id == id } ?: return 1
        val sid = a.seriesId ?: return 1
        return _data.value.activities.count { it.seriesId == sid }.coerceAtLeast(1)
    }

    /**
     * Edits an activity – only this one, this and the following ones, or the whole series
     * (same as iOS). A new day or time moves every chosen activity by the same number of
     * days, at the new time. Earlier days you already ticked off stay as they were with
     * FOLLOWING. Returns how many activities changed.
     */
    fun editActivity(
        id: String, scope: EditScope, name: String, details: String,
        newStart: Long, durationMinutes: Int, reminderMinutes: Int?,
    ): Int {
        var count = 0
        mutate(refresh = true) { d ->
            val item = d.activities.firstOrNull { it.id == id } ?: return@mutate d
            val series = item.seriesId?.let { sid -> d.activities.filter { it.seriesId == sid } } ?: listOf(item)
            val targets = when (scope) {
                EditScope.THIS_ONE -> listOf(item)
                EditScope.FOLLOWING -> series.filter { it.date >= item.date }
                EditScope.ALL -> series
            }.map { it.id }.toSet()
            count = targets.size

            val shift = java.time.temporal.ChronoUnit.DAYS.between(dayOf(item.date), dayOf(newStart))
            val time = timeOf(newStart)
            val changedDays = mutableSetOf<LocalDate>()
            val updated = d.activities.map { a ->
                if (a.id !in targets) a
                else {
                    changedDays += dayOf(a.date)
                    val moved = dayOf(a.date).plusDays(shift).atTime(time.hour, time.minute)
                        .atZone(zone()).toInstant().toEpochMilli()
                    changedDays += dayOf(moved)
                    a.copy(
                        date = moved, name = name.trim(), details = details,
                        durationMinutes = durationMinutes,
                        reminderMinutes = reminderMinutes?.takeIf { m -> m > 0 },
                    )
                }
            }
            syncConfidence(d.copy(activities = updated), changedDays)
        }
        return count
    }

    fun deleteActivity(id: String) = mutate(refresh = true) { d ->
        val item = d.activities.firstOrNull { it.id == id } ?: return@mutate d
        syncConfidence(d.copy(activities = d.activities.filterNot { it.id == id }), setOf(dayOf(item.date)))
    }

    /**
     * Same as iOS: each day's activity score (done / total × 10) is written as a
     * Confidence entry tagged "activities". Entries you log yourself are never touched.
     */
    private fun syncConfidence(d: AppData, days: Set<LocalDate>): AppData {
        var entries = d.confidenceEntries
        for (day in days) {
            val items = d.activities.filter { dayOf(it.date) == day }
            val existing = entries.firstOrNull { dayOf(it.date) == day && "activities" in it.tags }
            entries = if (items.isEmpty()) {
                if (existing != null) entries - existing else entries
            } else {
                val score = (items.count { it.isCompleted }.toDouble() / items.size * 10).roundToInt()
                if (existing != null) entries.map { if (it.id == existing.id) it.copy(score = score) else it }
                else entries + ConfidenceEntry(
                    date = day.atStartOfDay(zone()).toInstant().toEpochMilli(),
                    score = score,
                    note = "Derived from activities",
                    tags = listOf("activities"),
                )
            }
        }
        return d.copy(confidenceEntries = entries)
    }

    // ================= Confidence =================

    fun addConfidence(score: Int, note: String?) = mutate {
        it.copy(confidenceEntries = it.confidenceEntries + ConfidenceEntry(
            date = System.currentTimeMillis(), score = score, note = note?.takeIf { n -> n.isNotBlank() },
        ))
    }

    fun deleteConfidence(id: String) = mutate { d ->
        d.copy(confidenceEntries = d.confidenceEntries.filterNot { it.id == id })
    }

    fun deleteAllConfidence() = mutate { it.copy(confidenceEntries = emptyList()) }

    // ================= Focus Test =================

    fun addFocusResult(seconds: Double) = mutate {
        it.copy(focusResults = it.focusResults + FocusResult(date = System.currentTimeMillis(), duration = seconds))
    }

    fun deleteFocusResult(id: String) = mutate { d -> d.copy(focusResults = d.focusResults.filterNot { it.id == id }) }

    // ================= Journaling =================

    fun addJournal(mood: Mood, body: RichText) = mutate {
        it.copy(journalEntries = it.journalEntries + JournalEntry(date = System.currentTimeMillis(), mood = mood, body = body))
    }

    fun updateJournal(id: String, mood: Mood, body: RichText) = mutate { d ->
        d.copy(journalEntries = d.journalEntries.map { if (it.id == id) it.copy(mood = mood, body = body) else it })
    }

    fun deleteJournal(id: String) = mutate { d -> d.copy(journalEntries = d.journalEntries.filterNot { it.id == id }) }

    // ================= "Please rate us" box =================

    /** 1 or 2 when the rating box should be shown now (after 1 and 2 months of use), else null. */
    fun reviewDue(now: Long = System.currentTimeMillis()): Int? {
        if (isDemo) return null
        val s = _settings.value
        val days = (now - s.firstLaunchAt) / (24L * 60 * 60 * 1000)
        return when {
            days >= 60 && !s.reviewAsked2 -> 2
            days >= 30 && !s.reviewAsked1 -> 1
            else -> null
        }
    }

    /** The box was shown: don't show it again for this (or an earlier) month. */
    fun markReviewShown(which: Int) = updateSettings {
        if (which >= 2) it.copy(reviewAsked1 = true, reviewAsked2 = true) else it.copy(reviewAsked1 = true)
    }

    // ================= Screenshot mode =================

    /** True while showing sample data for screenshots: nothing is saved and no reminders are set. */
    @Volatile var isDemo: Boolean = false
        private set

    /** Shows the sample data for one screenshot ([DemoData]). Only used by debug builds. */
    fun enableDemo(scene: String) {
        isDemo = true
        _data.value = DemoData.forScene(scene)
        _settings.value = _settings.value.copy(hasSeenIntro = true, userName = "", currency = "EUR")
    }

    // ================= Import =================

    /** Adds items from an export file, skipping ones already in the app (same rules as iOS). Returns how many were added. */
    fun importItems(kind: ExportImport.Kind, imported: ExportImport.Imported): Int {
        var added = 0
        mutate(refresh = true) { d ->
            when (kind) {
                ExportImport.Kind.TASKS -> {
                    val new = imported.tasks.filter { t ->
                        d.tasks.none { it.title == t.title && it.dueAt == t.dueAt && it.priority == t.priority }
                    }.map { it.copy(id = newId()) }
                    added = new.size
                    d.copy(tasks = d.tasks + new)
                }
                ExportImport.Kind.SHOPPING -> {
                    val new = imported.shoppingItems.filter { s -> d.shoppingItems.none { it.id == s.id } }
                    added = new.size
                    d.copy(shoppingItems = d.shoppingItems + new)
                }
                ExportImport.Kind.MONTHLY -> {
                    val new = imported.monthlyTasks.filter { m ->
                        d.monthlyTasks.none { it.title == m.title && it.price == m.price }
                    }
                    added = new.size
                    d.copy(monthlyTasks = d.monthlyTasks + new)
                }
                ExportImport.Kind.PROJECTS -> {
                    val new = imported.projects.filter { p -> d.projects.none { it.id == p.id } }
                    added = new.size
                    d.copy(projects = d.projects + new)
                }
                ExportImport.Kind.ACTIVITIES -> {
                    val new = imported.activities.filter { a ->
                        d.activities.none { it.name == a.name && it.date == a.date && it.durationMinutes == a.durationMinutes }
                    }
                    added = new.size
                    syncConfidence(d.copy(activities = d.activities + new), new.map { dayOf(it.date) }.toSet())
                }
            }
        }
        return added
    }

    // ================= Persistence =================

    private fun mutate(refresh: Boolean = false, block: (AppData) -> AppData) {
        _data.update(block)
        val snapshot = _data.value
        if (isDemo) return   // screenshot mode: sample data stays in memory only
        writer.execute { save(snapshot) }
        if (refresh) ReminderScheduler.refresh(appContext, snapshot, _settings.value)
        TodayWidget.requestUpdate(appContext)
    }

    private fun updateSettings(refresh: Boolean = false, block: (Settings) -> Settings) {
        _settings.update(block)
        val s = _settings.value
        if (isDemo) return
        prefs.edit()
            .putString(K_THEME, s.theme.name)
            .putString(K_CURRENCY, s.currency)
            .putString(K_NAME, s.userName)
            .putBoolean(K_INTRO, s.hasSeenIntro)
            .putBoolean(K_SUMMARY, s.dailySummaryEnabled)
            .putBoolean(K_ACTIVITY, s.activityStartEnabled)
            .putInt(K_HOUR, s.reminderHour)
            .putInt(K_MINUTE, s.reminderMinute)
            .putLong(K_FIRST, s.firstLaunchAt)
            .putBoolean(K_REVIEW1, s.reviewAsked1)
            .putBoolean(K_REVIEW2, s.reviewAsked2)
            .apply()
        if (refresh) ReminderScheduler.refresh(appContext, _data.value, s)
        TodayWidget.requestUpdate(appContext)
    }

    private fun loadSettings(): Settings {
        // Remember the first time the app was opened (for the rating box after 1 and 2 months)
        var first = prefs.getLong(K_FIRST, 0L)
        if (first == 0L) {
            first = System.currentTimeMillis()
            prefs.edit().putLong(K_FIRST, first).apply()
        }
        return readSettings().copy(
            firstLaunchAt = first,
            reviewAsked1 = prefs.getBoolean(K_REVIEW1, false),
            reviewAsked2 = prefs.getBoolean(K_REVIEW2, false),
        )
    }

    private fun readSettings() = Settings(
        theme = ThemeSetting.parse(prefs.getString(K_THEME, null)),
        currency = prefs.getString(K_CURRENCY, null) ?: "USD",
        userName = prefs.getString(K_NAME, null) ?: "",
        hasSeenIntro = prefs.getBoolean(K_INTRO, false),
        dailySummaryEnabled = prefs.getBoolean(K_SUMMARY, true),
        activityStartEnabled = prefs.getBoolean(K_ACTIVITY, true),
        reminderHour = prefs.getInt(K_HOUR, 21),
        reminderMinute = prefs.getInt(K_MINUTE, 0),
    )

    private fun load(): AppData = try {
        if (!file.baseFile.exists()) AppData()
        else Storage.fromJson(JSONObject(String(file.readFully(), Charsets.UTF_8)))
    } catch (e: Exception) {
        Log.e(TAG, "Could not read saved data", e)
        // Keep the unreadable file aside instead of overwriting it
        runCatching { file.baseFile.copyTo(File(appContext.filesDir, "tobedone-unreadable-${System.currentTimeMillis()}.json")) }
        AppData()
    }

    @Synchronized
    private fun save(data: AppData) {
        val out = try { file.startWrite() } catch (e: Exception) {
            Log.e(TAG, "Could not open data file", e); return
        }
        try {
            out.write(Storage.toJson(data).toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            Log.e(TAG, "Could not save data", e)
        }
    }

    companion object {
        private const val TAG = "AppStore"
        private const val K_THEME = "selectedTheme"
        private const val K_CURRENCY = "selectedCurrency"
        private const val K_NAME = "userName"
        private const val K_INTRO = "hasSeenIntroPage"
        private const val K_SUMMARY = "dailySummaryEnabled"
        private const val K_ACTIVITY = "activityStartEnabled"
        private const val K_HOUR = "reminderHour"
        private const val K_MINUTE = "reminderMinute"
        private const val K_FIRST = "firstLaunchAt"
        private const val K_REVIEW1 = "reviewAsked1"
        private const val K_REVIEW2 = "reviewAsked2"

        @Volatile private var instance: AppStore? = null

        fun get(context: Context): AppStore =
            instance ?: synchronized(this) {
                instance ?: AppStore(context.applicationContext).also { instance = it }
            }

        fun zone(): ZoneId = ZoneId.systemDefault()
        fun dayOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate()
        fun timeOf(millis: Long): java.time.LocalTime = Instant.ofEpochMilli(millis).atZone(zone()).toLocalTime()
    }
}
