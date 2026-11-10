package ro.vishrama.tobedone.data

import org.json.JSONArray
import org.json.JSONObject

/** Internal save format (dates as epoch milliseconds). */
internal object Storage {

    fun toJson(d: AppData): JSONObject = JSONObject().apply {
        put("version", 2)
        put("tasks", arr(d.tasks) {
            JSONObject().put("id", it.id).put("title", it.title).put("dueAt", it.dueAt)
                .put("priority", it.priority.name).put("isCompleted", it.isCompleted)
                .putOpt("reminderMinutes", it.reminderMinutes)
        })
        put("shoppingItems", arr(d.shoppingItems) {
            JSONObject().put("id", it.id).put("name", it.name).put("price", it.price)
                .put("currency", it.currency).put("priority", it.priority.name)
                .put("isCompleted", it.isCompleted).putOpt("completedAt", it.completedAt)
        })
        put("monthlyTasks", arr(d.monthlyTasks) {
            JSONObject().put("id", it.id).put("title", it.title).put("price", it.price)
                .putOpt("dayOfMonth", it.dayOfMonth).put("hour", it.hour).put("minute", it.minute)
                .putOpt("paidMonth", it.paidMonth)
        })
        put("groceryItems", arr(d.groceryItems) {
            JSONObject().put("id", it.id).put("name", it.name).put("isBought", it.isBought)
        })
        put("projects", arr(d.projects) { p ->
            JSONObject().put("id", p.id).put("title", p.title).put("isCompleted", p.isCompleted)
                .put("subtasks", arr(p.subtasks) {
                    JSONObject().put("id", it.id).put("title", it.title).put("isCompleted", it.isCompleted)
                })
        })
        put("activities", arr(d.activities) {
            JSONObject().put("id", it.id).put("date", it.date).put("name", it.name)
                .put("details", it.details).put("durationMinutes", it.durationMinutes)
                .put("isCompleted", it.isCompleted).putOpt("seriesId", it.seriesId)
                .put("repeatsWeekly", it.repeatsWeekly).put("repeatWeeks", it.repeatWeeks)
                .put("repeatsDaily", it.repeatsDaily).put("repeatDays", it.repeatDays)
                .putOpt("reminderMinutes", it.reminderMinutes)
        })
        put("confidenceEntries", arr(d.confidenceEntries) {
            JSONObject().put("id", it.id).put("date", it.date).put("score", it.score)
                .putOpt("note", it.note).put("tags", JSONArray(it.tags))
        })
        put("focusResults", arr(d.focusResults) {
            JSONObject().put("id", it.id).put("date", it.date).put("duration", it.duration)
        })
        put("journalEntries", arr(d.journalEntries) {
            JSONObject().put("id", it.id).put("date", it.date).put("mood", it.mood.name)
                .put("text", it.body.text)
                .put("bold", RichText.encode(it.body.bold))
                .put("italic", RichText.encode(it.body.italic))
                .put("underline", RichText.encode(it.body.underline))
                .put("red", RichText.encode(it.body.red))
        })
    }

    fun fromJson(o: JSONObject) = AppData(
        tasks = o.objects("tasks").map {
            DueTask(
                id = it.getString("id"),
                title = it.getString("title"),
                dueAt = it.getLong("dueAt"),
                priority = Priority.parse(it.optString("priority")),
                isCompleted = it.optBoolean("isCompleted"),
                reminderMinutes = it.optIntOrNull("reminderMinutes"),
            )
        },
        shoppingItems = o.objects("shoppingItems").map {
            ShoppingItem(
                id = it.getString("id"),
                name = it.getString("name"),
                price = it.optDouble("price", 0.0),
                currency = it.optString("currency", "USD"),
                priority = Priority.parse(it.optString("priority")),
                isCompleted = it.optBoolean("isCompleted"),
                completedAt = it.optLongOrNull("completedAt"),
            )
        },
        monthlyTasks = o.objects("monthlyTasks").map {
            MonthlyTask(
                id = it.getString("id"),
                title = it.getString("title"),
                price = it.optDouble("price", 0.0),
                dayOfMonth = it.optIntOrNull("dayOfMonth"),
                hour = it.optInt("hour", 10),
                minute = it.optInt("minute", 0),
                paidMonth = it.optStringOrNull("paidMonth"),
            )
        },
        groceryItems = o.objects("groceryItems").map {
            GroceryItem(it.getString("id"), it.getString("name"), it.optBoolean("isBought"))
        },
        projects = o.objects("projects").map { p ->
            Project(
                id = p.getString("id"),
                title = p.getString("title"),
                isCompleted = p.optBoolean("isCompleted"),
                subtasks = p.objects("subtasks").map {
                    Subtask(it.getString("id"), it.getString("title"), it.optBoolean("isCompleted"))
                },
            )
        },
        activities = o.objects("activities").map {
            Activity(
                id = it.getString("id"),
                date = it.getLong("date"),
                name = it.getString("name"),
                details = it.optString("details", ""),
                durationMinutes = it.optInt("durationMinutes", 0),
                isCompleted = it.optBoolean("isCompleted"),
                seriesId = it.optStringOrNull("seriesId"),
                repeatsWeekly = it.optBoolean("repeatsWeekly"),
                repeatWeeks = it.optInt("repeatWeeks", 0),
                repeatsDaily = it.optBoolean("repeatsDaily"),
                repeatDays = it.optInt("repeatDays", 0),
                reminderMinutes = it.optIntOrNull("reminderMinutes"),
            )
        },
        confidenceEntries = o.objects("confidenceEntries").map {
            ConfidenceEntry(
                id = it.getString("id"),
                date = it.getLong("date"),
                score = it.getInt("score"),
                note = it.optStringOrNull("note"),
                tags = it.optJSONArray("tags")?.let { a -> (0 until a.length()).map { i -> a.getString(i) } }
                    ?: emptyList(),
            )
        },
        focusResults = o.objects("focusResults").map {
            FocusResult(it.getString("id"), it.getLong("date"), it.getDouble("duration"))
        },
        journalEntries = o.objects("journalEntries").map {
            JournalEntry(
                id = it.getString("id"),
                date = it.getLong("date"),
                mood = Mood.parse(it.optString("mood")),
                body = RichText(
                    text = it.optString("text", ""),
                    bold = RichText.decode(it.optStringOrNull("bold")),
                    italic = RichText.decode(it.optStringOrNull("italic")),
                    underline = RichText.decode(it.optStringOrNull("underline")),
                    red = RichText.decode(it.optStringOrNull("red")),
                ),
            )
        },
    )

    private fun <T> arr(list: List<T>, map: (T) -> JSONObject) = JSONArray(list.map(map))
}

internal fun JSONObject.objects(key: String): List<JSONObject> {
    val arr = optJSONArray(key) ?: return emptyList()
    return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
}

internal fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

internal fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) getInt(key) else null

internal fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) getLong(key) else null
