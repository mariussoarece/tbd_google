package ro.vishrama.tobedone.data

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Sample content for the Play Store screenshots, the same as the iPhone app's
 * DemoData.swift. Only used by debug builds started with the "demoScene" extra
 * (scripts/take-screenshots.sh does this). It lives in memory only, so your real
 * tasks are never read, changed or backed up.
 *
 * Scenes: home, todo, routine, monthly, projects, shopping, grocery,
 * confidence, focus, journal.
 */
object DemoData {

    fun forScene(scene: String): AppData = when (scene) {
        "todo" -> AppData(tasks = todo())
        "routine" -> routine()
        "monthly" -> AppData(monthlyTasks = listOf(
            MonthlyTask(title = "Rent", price = 650.0, dayOfMonth = 1, hour = 10),
            MonthlyTask(title = "Internet", price = 25.0, dayOfMonth = 15, hour = 10),
        ))
        "projects" -> AppData(projects = projects())
        "shopping" -> AppData(shoppingItems = shopping() + listOf(
            ShoppingItem(name = "Headphones", price = 59.0, currency = "EUR", isCompleted = true,
                completedAt = at(-2, 18)),
        ))
        "grocery" -> AppData(groceryItems = grocery())
        "confidence" -> AppData(confidenceEntries = confidence())
        "focus" -> AppData(focusResults = focus())
        "journal" -> AppData(journalEntries = journal())
        else -> home()
    }

    // ---------- helpers ----------

    private fun zone() = AppStore.zone()

    /** A day relative to today at a given time, in epoch milliseconds. */
    private fun at(dayOffset: Int, hour: Int, minute: Int = 0): Long =
        LocalDate.now(zone()).plusDays(dayOffset.toLong()).atTime(hour, minute).atZone(zone()).toInstant().toEpochMilli()

    private fun todayDay(offset: Int = 0) = LocalDate.now(zone()).plusDays(offset.toLong()).dayOfMonth.coerceAtMost(28)

    private fun task(title: String, due: Long, p: Priority, done: Boolean = false, remind: Int? = null) =
        DueTask(title = title, dueAt = due, priority = p, isCompleted = done, reminderMinutes = remind)

    // ---------- Home: today's plan ----------

    private fun home() = AppData(
        activities = listOf(
            Activity(date = at(0, 9), name = "Deep work for the new project", durationMinutes = 120),
            Activity(date = at(0, 21), name = "Read a book", durationMinutes = 30),
        ),
        tasks = listOf(
            task("Send the project report", at(0, 16), Priority.HIGH),
            task("Book train tickets", at(1, 9), Priority.MEDIUM),
        ),
        monthlyTasks = listOf(
            MonthlyTask(title = "Pay the electricity bill", price = 75.0, dayOfMonth = todayDay(), hour = 18),
            MonthlyTask(title = "Rent", price = 650.0, dayOfMonth = 1, hour = 10),
        ),
        projects = projects().take(1),
        shoppingItems = shopping(),
        groceryItems = grocery(),
        confidenceEntries = confidence(),
        focusResults = focus(),
        journalEntries = journal().take(1),
    )

    // ---------- To Do: two due, the rest done on earlier days ----------

    private fun todo() = listOf(
        task("Book train tickets", at(1, 9), Priority.MEDIUM, remind = 60),
        task("Renew gym membership", at(3, 10), Priority.LOW),
        task("Send the project report", at(-1, 16), Priority.HIGH, done = true),
        task("Call the dentist", at(-1, 11), Priority.MEDIUM, done = true),
        task("Water the plants", at(-2, 8), Priority.LOW, done = true),
        task("Pay the parking ticket", at(-3, 12), Priority.HIGH, done = true),
    )

    // ---------- Daily Routine: this week, today selected ----------

    private fun routine(): AppData {
        fun weekly(name: String, dayOffset: Int, hour: Int, minutes: Int, doneFirst: Boolean): List<Activity> {
            val series = newId()
            return (0 until 4).map { w ->
                val start = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(at(dayOffset, hour)), zone())
                    .plusWeeks(w.toLong()).toInstant().toEpochMilli()
                Activity(
                    date = start, name = name, durationMinutes = minutes,
                    isCompleted = doneFirst && w == 0,
                    seriesId = series, repeatsWeekly = true, repeatWeeks = 4,
                )
            }
        }
        val activities = weekly("Team meeting", -1, 12, 60, true) +
            weekly("Morning run", 0, 9, 30, true) +
            weekly("Lunch walk", 0, 13, 30, false) +
            listOf(
                Activity(date = at(0, 17), name = "Language class", durationMinutes = 60),
                Activity(date = at(0, 21), name = "Read a book", durationMinutes = 30),
            )
        return AppData(
            activities = activities,
            monthlyTasks = listOf(
                MonthlyTask(title = "Rent", price = 650.0, dayOfMonth = todayDay(-1), hour = 10),
                MonthlyTask(title = "Internet", price = 25.0, dayOfMonth = todayDay(), hour = 11),
                MonthlyTask(title = "Phone", price = 15.0, dayOfMonth = todayDay(1), hour = 10),
            ),
            tasks = listOf(
                task("Call the dentist", at(-1, 16), Priority.MEDIUM, done = true),
                task("Send the project report", at(1, 15), Priority.HIGH),
                task("Book train tickets", at(2, 11), Priority.MEDIUM),
                task("Pick up the dry cleaning", at(3, 17), Priority.LOW),
            ),
            // Daily Routine scores also appear in Confidence, like in the app
            confidenceEntries = emptyList(),
        )
    }

    // ---------- Projects: one open with three subtasks, one completed ----------

    private fun projects() = listOf(
        Project(title = "Plan summer holiday", subtasks = listOf(
            Subtask(title = "Choose destination", isCompleted = true),
            Subtask(title = "Book flights"),
            Subtask(title = "Find a hotel"),
        )),
        Project(title = "Paint the balcony", isCompleted = true, subtasks = listOf(
            Subtask(title = "Buy paint", isCompleted = true),
            Subtask(title = "Paint the walls", isCompleted = true),
        )),
    )

    private fun shopping() = listOf(
        ShoppingItem(name = "Running shoes", price = 89.99, currency = "EUR", priority = Priority.HIGH),
        ShoppingItem(name = "Desk lamp", price = 34.50, currency = "EUR"),
        ShoppingItem(name = "Birthday gift for Anna", price = 45.00, currency = "EUR", priority = Priority.HIGH),
    )

    // ---------- Grocery: five to buy, three bought ----------

    private fun grocery() =
        listOf("Milk", "Bread", "Eggs", "Tomatoes", "Avocados").map { GroceryItem(name = it) } +
            listOf("Coffee", "Bananas", "Rice").map { GroceryItem(name = it, isBought = true) }

    // ---------- Confidence: the last 30 days, slowly improving ----------

    private fun confidence(): List<ConfidenceEntry> {
        val scores = listOf(5, 6, 5, 6, 7, 6, 7, 7, 6, 7, 8, 7, 7, 8, 7, 8, 8, 7, 8, 8, 7, 8, 9, 8, 8, 9, 8, 8, 9, 8)
        return scores.mapIndexed { i, score ->
            val day = scores.size - 1 - i
            ConfidenceEntry(
                date = at(-day, 20), score = score,
                note = if (i == scores.size - 1) "Finished my report early" else null,
            )
        }
    }

    // ---------- Focus Test: three tests, one week apart ----------

    private fun focus() = listOf(2 to 52.7, 1 to 61.3, 0 to 74.8).map { (weeksAgo, seconds) ->
        FocusResult(date = at(-7 * weeksAgo, 20, 30), duration = seconds)
    }

    // ---------- Journaling: a few days, with some formatting ----------

    private fun journal(): List<JournalEntry> {
        fun rich(text: String, bold: String? = null, italic: String? = null, red: String? = null, underline: String? = null): RichText {
            fun r(word: String?) = word?.let { w ->
                val i = text.indexOf(w)
                if (i < 0) emptyList() else listOf(StyleRange(i, i + w.length))
            } ?: emptyList()
            return RichText(text, bold = r(bold), italic = r(italic), underline = r(underline), red = r(red))
        }
        return listOf(
            JournalEntry(date = at(0, 21, 15), mood = Mood.EXCELLENT, body = rich(
                "Finished the project report two hours early. A long walk at lunch really helped me focus.\nTomorrow: book the train tickets first thing.",
                bold = "two hours early", red = "book the train tickets",
            )),
            JournalEntry(date = at(-1, 22), mood = Mood.GOOD, body = rich(
                "Team meeting went well. I was nervous before speaking, but it was fine. Small steps, big changes.",
                italic = "Small steps, big changes.",
            )),
            JournalEntry(date = at(-3, 20, 30), mood = Mood.COULD_BE_BETTER, body = rich(
                "Slept badly and skipped the morning run. Going to bed earlier tonight.",
                underline = "Going to bed earlier tonight.",
            )),
            JournalEntry(date = at(-6, 19), mood = Mood.AWESOME, body = rich(
                "Best Focus Test result so far: 74.8 seconds! The practice is working.",
                bold = "74.8 seconds!",
            )),
        )
    }
}
