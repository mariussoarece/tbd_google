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

    fun addTask(title: String, dueAt: Long, priority: Priority) = mutate(refresh = true) {
        // Snap to the start of the minute, like iOS
        val t = DueTask(title = title.trim(), dueAt = dueAt / 60_000L * 60_000L, priority = priority)
        it.copy(tasks = it.tasks + t)
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

    fun deleteShoppingItem(id: String) = mutate { d ->
        d.copy(shoppingItems = d.shoppingItems.filterNot { it.id == id })
    }

    // ================= Monthly =================

    fun addMonthlyTask(title: String, price: Double) = mutate {
        it.copy(monthlyTasks = it.monthlyTasks + MonthlyTask(title = title.trim(), price = price))
    }

    fun deleteMonthlyTask(id: String) = mutate { d -> d.copy(monthlyTasks = d.monthlyTasks.filterNot { it.id == id }) }

    // ================= Grocery =================

    fun addGroceryItem(name: String) = mutate { it.copy(groceryItems = it.groceryItems + GroceryItem(name = name.trim())) }

    fun toggleGroceryItem(id: String) = mutate { d ->
        d.copy(groceryItems = d.groceryItems.map { if (it.id == id) it.copy(isBought = !it.isBought) else it })
    }

    fun deleteGroceryItem(id: String) = mutate { d -> d.copy(groceryItems = d.groceryItems.filterNot { it.id == id }) }

    // ================= Projects =================

    fun addProject(title: String) = mutate { it.copy(projects = it.projects + Project(title = title.trim())) }

    fun toggleProject(id: String) = mutate { d ->
        d.copy(projects = d.projects.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it })
    }

    fun deleteProject(id: String) = mutate { d -> d.copy(projects = d.projects.filterNot { it.id == id }) }

    fun addSubtask(projectId: String, title: String) = updateProject(projectId) {
        it.copy(subtasks = it.subtasks + Subtask(title = title.trim()))
    }

    fun toggleSubtask(projectId: String, subtaskId: String) = updateProject(projectId) { p ->
        p.copy(subtasks = p.subtasks.map { if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it })
    }

    fun deleteSubtask(projectId: String, subtaskId: String) = updateProject(projectId) { p ->
        p.copy(subtasks = p.subtasks.filterNot { it.id == subtaskId })
    }

    private fun updateProject(id: String, block: (Project) -> Project) = mutate { d ->
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
        writer.execute { save(snapshot) }
        if (refresh) ReminderScheduler.refresh(appContext, snapshot, _settings.value)
    }

    private fun updateSettings(refresh: Boolean = false, block: (Settings) -> Settings) {
        _settings.update(block)
        val s = _settings.value
        prefs.edit()
            .putString(K_THEME, s.theme.name)
            .putString(K_CURRENCY, s.currency)
            .putString(K_NAME, s.userName)
            .putBoolean(K_INTRO, s.hasSeenIntro)
            .putBoolean(K_SUMMARY, s.dailySummaryEnabled)
            .putBoolean(K_ACTIVITY, s.activityStartEnabled)
            .putInt(K_HOUR, s.reminderHour)
            .putInt(K_MINUTE, s.reminderMinute)
            .apply()
        if (refresh) ReminderScheduler.refresh(appContext, _data.value, s)
    }

    private fun loadSettings() = Settings(
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

        @Volatile private var instance: AppStore? = null

        fun get(context: Context): AppStore =
            instance ?: synchronized(this) {
                instance ?: AppStore(context.applicationContext).also { instance = it }
            }

        fun zone(): ZoneId = ZoneId.systemDefault()
        fun dayOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate()
    }
}
