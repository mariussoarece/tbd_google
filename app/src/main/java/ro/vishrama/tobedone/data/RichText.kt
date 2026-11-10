package ro.vishrama.tobedone.data

/** A styled part of a text: characters [start, end). */
data class StyleRange(val start: Int, val end: Int)

enum class TextAttr { BOLD, ITALIC, UNDERLINE, RED }

/**
 * Text with bold / italic / underline / red parts, as used by Journaling.
 * Each style is a list of non-overlapping, sorted ranges.
 */
data class RichText(
    val text: String = "",
    val bold: List<StyleRange> = emptyList(),
    val italic: List<StyleRange> = emptyList(),
    val underline: List<StyleRange> = emptyList(),
    val red: List<StyleRange> = emptyList(),
) {
    fun ranges(a: TextAttr): List<StyleRange> = when (a) {
        TextAttr.BOLD -> bold
        TextAttr.ITALIC -> italic
        TextAttr.UNDERLINE -> underline
        TextAttr.RED -> red
    }

    fun with(a: TextAttr, list: List<StyleRange>): RichText = when (a) {
        TextAttr.BOLD -> copy(bold = list)
        TextAttr.ITALIC -> copy(italic = list)
        TextAttr.UNDERLINE -> copy(underline = list)
        TextAttr.RED -> copy(red = list)
    }

    /** Styles of the character just before [pos] (what new typing continues with). */
    fun attrsAt(pos: Int): Set<TextAttr> {
        val i = if (pos > 0) pos - 1 else pos
        return TextAttr.entries.filter { a -> ranges(a).any { i >= it.start && i < it.end } }.toSet()
    }

    /** True when every character in [start, end) has style [a]. */
    fun covers(a: TextAttr, start: Int, end: Int): Boolean {
        if (start >= end) return false
        var pos = start
        for (r in ranges(a)) {
            if (r.end <= pos) continue
            if (r.start > pos) return false
            pos = r.end
            if (pos >= end) return true
        }
        return false
    }

    /** Turns style [a] on for [start, end), or off if it is already on everywhere there. */
    fun toggle(a: TextAttr, start: Int, end: Int): RichText {
        if (start >= end) return this
        val list = ranges(a)
        return with(a, if (covers(a, start, end)) subtract(list, start, end) else add(list, start, end))
    }

    /** Sets style [a] on or off for [start, end). */
    fun set(a: TextAttr, start: Int, end: Int, on: Boolean): RichText {
        if (start >= end) return this
        val list = ranges(a)
        return with(a, if (on) add(list, start, end) else subtract(list, start, end))
    }

    /**
     * The text was edited to [newText]. Moves the styled parts with the text and
     * gives newly typed characters the styles in [typing].
     */
    fun edited(newText: String, typing: Set<TextAttr>): RichText {
        val old = text
        if (old == newText) return this
        var p = 0
        val max = minOf(old.length, newText.length)
        while (p < max && old[p] == newText[p]) p++
        var q = 0
        while (q < max - p && old[old.length - 1 - q] == newText[newText.length - 1 - q]) q++
        val oldEnd = old.length - q
        val newEnd = newText.length - q
        val delta = newText.length - old.length

        fun map(x: Int): Int = when {
            x <= p -> x
            x >= oldEnd -> x + delta
            else -> p
        }

        var result = RichText(text = newText)
        for (a in TextAttr.entries) {
            val moved = normalize(ranges(a).map { StyleRange(map(it.start), map(it.end)) })
            result = result.with(a, moved)
        }
        if (newEnd > p) {
            for (a in TextAttr.entries) result = result.set(a, p, newEnd, a in typing)
        }
        return result
    }

    companion object {
        fun normalize(list: List<StyleRange>): List<StyleRange> {
            val sorted = list.filter { it.end > it.start }.sortedBy { it.start }
            val out = mutableListOf<StyleRange>()
            for (r in sorted) {
                val last = out.lastOrNull()
                if (last != null && r.start <= last.end) {
                    out[out.size - 1] = StyleRange(last.start, maxOf(last.end, r.end))
                } else out += r
            }
            return out
        }

        fun add(list: List<StyleRange>, start: Int, end: Int) = normalize(list + StyleRange(start, end))

        fun subtract(list: List<StyleRange>, start: Int, end: Int): List<StyleRange> {
            val out = mutableListOf<StyleRange>()
            for (r in list) {
                if (r.end <= start || r.start >= end) { out += r; continue }
                if (r.start < start) out += StyleRange(r.start, start)
                if (r.end > end) out += StyleRange(end, r.end)
            }
            return normalize(out)
        }

        /** "0-5,9-12" ⇄ ranges, for saving. */
        fun encode(list: List<StyleRange>): String = list.joinToString(",") { "${it.start}-${it.end}" }

        fun decode(s: String?): List<StyleRange> =
            s.orEmpty().split(",").mapNotNull { part ->
                val bits = part.split("-")
                if (bits.size != 2) null
                else {
                    val a = bits[0].toIntOrNull()
                    val b = bits[1].toIntOrNull()
                    if (a == null || b == null) null else StyleRange(a, b)
                }
            }
    }
}

/** The text as HTML with <b>, <i>, <u> and red colour, for the export file. */
fun RichText.toHtml(): String {
    val cuts = sortedSetOf(0, text.length)
    TextAttr.entries.forEach { a -> ranges(a).forEach { cuts += it.start.coerceIn(0, text.length); cuts += it.end.coerceIn(0, text.length) } }
    val points = cuts.toList()
    val sb = StringBuilder()
    for (i in 0 until points.size - 1) {
        val s = points[i]
        val e = points[i + 1]
        if (s >= e) continue
        val on = TextAttr.entries.filter { a -> covers(a, s, e) }
        var chunk = text.substring(s, e)
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
        if (TextAttr.RED in on) chunk = "<span style=\"color:#d32f2f\">$chunk</span>"
        if (TextAttr.UNDERLINE in on) chunk = "<u>$chunk</u>"
        if (TextAttr.ITALIC in on) chunk = "<i>$chunk</i>"
        if (TextAttr.BOLD in on) chunk = "<b>$chunk</b>"
        sb.append(chunk)
    }
    return sb.toString()
}
