package ro.vishrama.tobedone.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import ro.vishrama.tobedone.notifications.ReminderScheduler
import java.io.File
import java.util.concurrent.Executors

/**
 * Single source of truth for all lists. Everything is kept in memory as a
 * StateFlow and saved to one JSON file on the device (the Android equivalent
 * of SwiftData's local store). No data ever leaves the phone.
 */
class AppStore private constructor(private val appContext: Context) {

    private val file = AtomicFile(File(appContext.filesDir, "tobedone.json"))
    private val writer = Executors.newSingleThreadExecutor()
    private val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _theme = MutableStateFlow(ThemeSetting.fromName(prefs.getString(KEY_THEME, null)))
    val theme: StateFlow<ThemeSetting> = _theme.asStateFlow()

    private val _currency = MutableStateFlow(prefs.getString(KEY_CURRENCY, null) ?: DEFAULT_CURRENCY)
    val currency: StateFlow<String> = _currency.asStateFlow()

    // ---------- Settings ----------

    fun setTheme(value: ThemeSetting) {
        prefs.edit().putString(KEY_THEME, value.name).apply()
        _theme.value = value
    }

    /** Currency used for new shopping items (iOS app always used RON). */
    fun setCurrency(value: String) {
        val clean = value.trim().uppercase().ifEmpty { DEFAULT_CURRENCY }
        prefs.edit().putString(KEY_CURRENCY, clean).apply()
        _currency.value = clean
    }

    // ---------- Due tasks ----------

    fun addDueTask(title: String, dueEpochDay: Long, priority: Priority) {
        val task = DueTask(title = title.trim(), dueEpochDay = dueEpochDay, priority = priority)
        mutate { it.copy(dueTasks = it.dueTasks + task) }
        ReminderScheduler.schedule(appContext, task)
    }

    fun toggleDueTask(id: String) {
        var changed: DueTask? = null
        mutate { d ->
            d.copy(dueTasks = d.dueTasks.map {
                if (it.id == id) it.copy(isCompleted = !it.isCompleted).also { t -> changed = t } else it
            })
        }
        changed?.let {
            if (it.isCompleted) ReminderScheduler.cancel(appContext, it.id)
            else ReminderScheduler.schedule(appContext, it)
        }
    }

    fun deleteDueTask(id: String) {
        mutate { d -> d.copy(dueTasks = d.dueTasks.filterNot { it.id == id }) }
        ReminderScheduler.cancel(appContext, id)
    }

    // ---------- Shopping ----------

    fun addShoppingItem(name: String, price: Double, currency: String, priority: Priority) {
        val item = ShoppingItem(name = name.trim(), price = price, currency = currency, priority = priority)
        mutate { it.copy(shoppingItems = it.shoppingItems + item) }
    }

    fun completeShoppingItem(id: String) = mutate { d ->
        d.copy(shoppingItems = d.shoppingItems.map { if (it.id == id) it.copy(isCompleted = true) else it })
    }

    fun deleteShoppingItem(id: String) = mutate { d ->
        d.copy(shoppingItems = d.shoppingItems.filterNot { it.id == id })
    }

    // ---------- Monthly ----------

    fun addMonthlyTask(title: String, price: Double) = mutate {
        it.copy(monthlyTasks = it.monthlyTasks + MonthlyTask(title = title.trim(), price = price))
    }

    fun deleteMonthlyTask(id: String) = mutate { d ->
        d.copy(monthlyTasks = d.monthlyTasks.filterNot { it.id == id })
    }

    // ---------- Grocery ----------

    fun addGroceryItem(name: String) = mutate {
        it.copy(groceryItems = it.groceryItems + GroceryItem(name = name.trim()))
    }

    fun toggleGroceryItem(id: String) = mutate { d ->
        d.copy(groceryItems = d.groceryItems.map { if (it.id == id) it.copy(isBought = !it.isBought) else it })
    }

    fun deleteGroceryItem(id: String) = mutate { d ->
        d.copy(groceryItems = d.groceryItems.filterNot { it.id == id })
    }

    // ---------- Other tasks ----------

    fun addOtherTask(title: String) = mutate {
        it.copy(otherTasks = it.otherTasks + OtherTask(title = title.trim()))
    }

    fun toggleOtherTask(id: String) = mutate { d ->
        d.copy(otherTasks = d.otherTasks.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it })
    }

    fun addSubtask(taskId: String, title: String) = mutate { d ->
        d.copy(otherTasks = d.otherTasks.map {
            if (it.id == taskId) it.copy(subtasks = it.subtasks + Subtask(title = title.trim())) else it
        })
    }

    fun deleteOtherTask(id: String) = mutate { d ->
        d.copy(otherTasks = d.otherTasks.filterNot { it.id == id })
    }

    // ---------- Persistence ----------

    private fun mutate(block: (AppData) -> AppData) {
        _data.update(block)
        val snapshot = _data.value
        writer.execute { save(snapshot) }
    }

    private fun load(): AppData = try {
        if (!file.baseFile.exists()) AppData()
        else fromJson(JSONObject(String(file.readFully(), Charsets.UTF_8)))
    } catch (e: Exception) {
        Log.e(TAG, "Could not read saved data", e)
        AppData()
    }

    @Synchronized
    private fun save(data: AppData) {
        val out = try { file.startWrite() } catch (e: Exception) {
            Log.e(TAG, "Could not open data file", e); return
        }
        try {
            out.write(toJson(data).toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            Log.e(TAG, "Could not save data", e)
        }
    }

    companion object {
        private const val TAG = "AppStore"
        private const val KEY_THEME = "selectedTheme"
        private const val KEY_CURRENCY = "currency"
        const val DEFAULT_CURRENCY = "RON"

        @Volatile private var instance: AppStore? = null

        fun get(context: Context): AppStore =
            instance ?: synchronized(this) {
                instance ?: AppStore(context.applicationContext).also { instance = it }
            }

        // ---- JSON mapping ----

        private fun toJson(d: AppData) = JSONObject().apply {
            put("version", 1)
            put("dueTasks", JSONArray(d.dueTasks.map {
                JSONObject().put("id", it.id).put("title", it.title).put("dueEpochDay", it.dueEpochDay)
                    .put("priority", it.priority.name).put("isCompleted", it.isCompleted)
            }))
            put("shoppingItems", JSONArray(d.shoppingItems.map {
                JSONObject().put("id", it.id).put("name", it.name).put("price", it.price)
                    .put("currency", it.currency).put("priority", it.priority.name)
                    .put("isCompleted", it.isCompleted)
            }))
            put("monthlyTasks", JSONArray(d.monthlyTasks.map {
                JSONObject().put("id", it.id).put("title", it.title).put("price", it.price)
            }))
            put("groceryItems", JSONArray(d.groceryItems.map {
                JSONObject().put("id", it.id).put("name", it.name).put("isBought", it.isBought)
            }))
            put("otherTasks", JSONArray(d.otherTasks.map { t ->
                JSONObject().put("id", t.id).put("title", t.title).put("isCompleted", t.isCompleted)
                    .put("subtasks", JSONArray(t.subtasks.map {
                        JSONObject().put("id", it.id).put("title", it.title)
                    }))
            }))
        }

        private fun fromJson(o: JSONObject) = AppData(
            dueTasks = o.objects("dueTasks").map {
                DueTask(
                    id = it.getString("id"),
                    title = it.getString("title"),
                    dueEpochDay = it.getLong("dueEpochDay"),
                    priority = Priority.fromName(it.optString("priority")),
                    isCompleted = it.optBoolean("isCompleted"),
                )
            },
            shoppingItems = o.objects("shoppingItems").map {
                ShoppingItem(
                    id = it.getString("id"),
                    name = it.getString("name"),
                    price = it.optDouble("price", 0.0),
                    currency = it.optString("currency", "RON"),
                    priority = Priority.fromName(it.optString("priority")),
                    isCompleted = it.optBoolean("isCompleted"),
                )
            },
            monthlyTasks = o.objects("monthlyTasks").map {
                MonthlyTask(id = it.getString("id"), title = it.getString("title"), price = it.optDouble("price", 0.0))
            },
            groceryItems = o.objects("groceryItems").map {
                GroceryItem(id = it.getString("id"), name = it.getString("name"), isBought = it.optBoolean("isBought"))
            },
            otherTasks = o.objects("otherTasks").map { t ->
                OtherTask(
                    id = t.getString("id"),
                    title = t.getString("title"),
                    isCompleted = t.optBoolean("isCompleted"),
                    subtasks = t.objects("subtasks").map { Subtask(id = it.getString("id"), title = it.getString("title")) },
                )
            },
        )

        private fun JSONObject.objects(key: String): List<JSONObject> {
            val arr = optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        }
    }
}
