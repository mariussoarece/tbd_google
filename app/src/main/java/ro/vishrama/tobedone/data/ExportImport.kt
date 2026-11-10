package ro.vishrama.tobedone.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Export / import files in the same JSON format as the iPhone app, so a file
 * exported on iOS can be imported on Android and the other way round.
 *
 * iOS writes dates as seconds since 1 January 2001 (Apple's reference date).
 */
object ExportImport {

    private const val APPLE_EPOCH_OFFSET_SECONDS = 978_307_200L

    private fun toApple(millis: Long): Double = millis / 1000.0 - APPLE_EPOCH_OFFSET_SECONDS
    private fun fromApple(seconds: Double): Long = ((seconds + APPLE_EPOCH_OFFSET_SECONDS) * 1000).toLong()

    enum class Kind(val fileName: String) {
        TASKS("todo_tasks_export.json"),
        SHOPPING("shopping_tasks_export.json"),
        MONTHLY("monthly_tasks_export.json"),
        PROJECTS("other_tasks_export.json"),
        ACTIVITIES("daily_activities_export.json"),
    }

    /** Builds the file content. Like iOS, only unfinished items are exported (Monthly: all). */
    fun export(kind: Kind, data: AppData): String {
        if (kind == Kind.ACTIVITIES) {
            return JSONObject().put("activities", activities(data.activities.filter { !it.isCompleted })).toString(2)
        }
        val o = JSONObject()
            .put("tasks", JSONArray())
            .put("shoppingItems", JSONArray())
            .put("monthlyTasks", JSONArray())
            .put("tasksWithSubtasks", JSONArray())
            .put("activityTasks", JSONArray())
        when (kind) {
            Kind.TASKS -> o.put("tasks", JSONArray(data.tasks.filter { !it.isCompleted }.map {
                JSONObject().put("id", it.id).put("title", it.title).put("dueDate", toApple(it.dueAt))
                    .put("priority", it.priority.label).put("isCompleted", it.isCompleted)
                    .putOpt("reminderMinutes", it.reminderMinutes)
            }))
            Kind.SHOPPING -> o.put("shoppingItems", JSONArray(data.shoppingItems.filter { !it.isCompleted }.map {
                JSONObject().put("id", it.id).put("name", it.name).put("price", it.price)
                    .put("currency", it.currency).put("priority", it.priority.label)
                    .put("isCompleted", it.isCompleted)
            }))
            Kind.MONTHLY -> o.put("monthlyTasks", JSONArray(data.monthlyTasks.map {
                JSONObject().put("id", it.id).put("title", it.title).put("price", it.price)
                    .putOpt("dayOfMonth", it.dayOfMonth).put("hour", it.hour).put("minute", it.minute)
            }))
            Kind.PROJECTS -> o.put("tasksWithSubtasks", JSONArray(data.projects.filter { !it.isCompleted }.map { p ->
                JSONObject().put("id", p.id).put("title", p.title).put("isCompleted", false)
                    .put("subtasks", JSONArray(p.subtasks.map {
                        JSONObject().put("id", it.id).put("title", it.title).put("isCompleted", it.isCompleted)
                    }))
            }))
            Kind.ACTIVITIES -> Unit
        }
        return o.toString(2)
    }

    private fun activities(list: List<Activity>) = JSONArray(list.map {
        JSONObject().put("id", it.id).put("date", toApple(it.date)).put("name", it.name)
            .put("details", it.details).put("durationMinutes", it.durationMinutes)
            .put("isCompleted", it.isCompleted).putOpt("seriesId", it.seriesId)
            .put("repeatsWeekly", it.repeatsWeekly).put("repeatWeeks", it.repeatWeeks)
            .put("repeatsDaily", it.repeatsDaily).put("repeatDays", it.repeatDays)
            .putOpt("reminderMinutes", it.reminderMinutes)
    })

    /** All Journaling entries as one HTML page (opens in any browser, keeps bold/italic/underline/red). */
    fun exportJournalHtml(entries: List<JournalEntry>): String {
        val fmt = java.time.format.DateTimeFormatter.ofLocalizedDateTime(
            java.time.format.FormatStyle.FULL, java.time.format.FormatStyle.SHORT,
        )
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        sb.append("<title>ToBeDone – Journal</title>")
        sb.append("<style>body{font-family:sans-serif;max-width:720px;margin:24px auto;padding:0 16px;line-height:1.5}")
        sb.append(".e{border-bottom:1px solid #ddd;padding:16px 0}.d{color:#777;font-size:14px}.m{font-weight:600}</style>")
        sb.append("</head><body><h1>Journal</h1>")
        for (e in entries.sortedByDescending { it.date }) {
            val date = java.time.Instant.ofEpochMilli(e.date).atZone(AppStore.zone()).format(fmt)
            sb.append("<div class=\"e\"><div class=\"d\">").append(date).append("</div>")
            sb.append("<div class=\"m\">").append(e.mood.emoji).append(" ").append(e.mood.label).append("</div>")
            sb.append("<p>").append(e.body.toHtml()).append("</p></div>")
        }
        sb.append("</body></html>")
        return sb.toString()
    }

    /** Everything that could be read from an import file. Older iOS key names are accepted too. */
    data class Imported(
        val tasks: List<DueTask> = emptyList(),
        val shoppingItems: List<ShoppingItem> = emptyList(),
        val monthlyTasks: List<MonthlyTask> = emptyList(),
        val projects: List<Project> = emptyList(),
        val activities: List<Activity> = emptyList(),
    )

    fun parse(text: String): Imported {
        val o = JSONObject(text)
        return Imported(
            tasks = o.objects("tasks").map {
                DueTask(
                    id = it.optString("id", newId()),
                    title = it.getString("title"),
                    dueAt = fromApple(it.getDouble("dueDate")),
                    priority = Priority.parse(it.optString("priority")),
                    isCompleted = it.optBoolean("isCompleted"),
                    reminderMinutes = it.optIntOrNull("reminderMinutes"),
                )
            },
            shoppingItems = (o.objects("shoppingItems") + o.objects("shoppingtasks")).map {
                ShoppingItem(
                    id = it.optString("id", newId()),
                    name = it.getString("name"),
                    price = it.optDouble("price", 0.0),
                    currency = it.optString("currency", "USD"),
                    priority = Priority.parse(it.optString("priority")),
                    isCompleted = it.optBoolean("isCompleted"),
                )
            },
            monthlyTasks = o.objects("monthlyTasks").map {
                MonthlyTask(
                    title = it.getString("title"),
                    price = it.optDouble("price", 0.0),
                    dayOfMonth = it.optIntOrNull("dayOfMonth")?.coerceIn(1, 28),
                    hour = it.optInt("hour", 10),
                    minute = it.optInt("minute", 0),
                )
            },
            projects = (o.objects("tasksWithSubtasks") + o.objects("othertasks")).map { p ->
                Project(
                    id = p.optString("id", newId()),
                    title = p.getString("title"),
                    isCompleted = p.optBoolean("isCompleted"),
                    subtasks = p.objects("subtasks").map {
                        Subtask(it.optString("id", newId()), it.getString("title"), it.optBoolean("isCompleted"))
                    },
                )
            },
            activities = (o.objects("activities") + o.objects("activityTasks")).map {
                Activity(
                    date = fromApple(it.getDouble("date")),
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
        )
    }
}
