package ro.vishrama.tobedone.data

/**
 * Human-readable length of time, e.g. "45 min", "2 h and 15 min",
 * "1 day and 30 min", "3 days, 2 h and 5 min".
 */
fun describeMinutes(totalMinutes: Long): String {
    val total = totalMinutes.coerceAtLeast(0)
    val days = total / (24 * 60)
    val hours = (total % (24 * 60)) / 60
    val minutes = total % 60
    val parts = buildList {
        if (days > 0) add("$days day" + if (days == 1L) "" else "s")
        if (hours > 0) add("$hours h")
        if (minutes > 0 || isEmpty()) add("$minutes min")
    }
    return when (parts.size) {
        1 -> parts[0]
        2 -> "${parts[0]} and ${parts[1]}"
        else -> "${parts[0]}, ${parts[1]} and ${parts[2]}"
    }
}
