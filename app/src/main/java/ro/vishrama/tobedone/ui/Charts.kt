package ro.vishrama.tobedone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ro.vishrama.tobedone.ui.theme.IOS
import ro.vishrama.tobedone.ui.theme.LocalGrouped

/**
 * Small line chart with points (replaces Swift Charts LineMark + PointMark).
 * [points] are (x, y); x is usually a time in milliseconds.
 */
@Composable
fun LineChart(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    yMin: Double? = null,
    yMax: Double? = null,
    target: Double? = null,
    targetLabel: String? = null,
    color: Color = IOS.Blue,
) {
    val g = LocalGrouped.current
    val labelColor = g.secondaryText
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (points.isEmpty()) return@Canvas
        val left = 34.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()

        val xs = points.map { it.first }
        val minX = xs.min()
        val maxX = xs.max()
        val lo = yMin ?: points.minOf { it.second }
        val hi = (yMax ?: points.maxOf { it.second }).let { if (it == lo) lo + 1 else it }

        fun px(x: Double) = if (maxX == minX) (left + right) / 2 else (left + (x - minX) / (maxX - minX) * (right - left)).toFloat()
        fun py(y: Double) = (bottom - (y - lo) / (hi - lo) * (bottom - top)).toFloat()

        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.argb(
                (labelColor.alpha * 255).toInt(), (labelColor.red * 255).toInt(),
                (labelColor.green * 255).toInt(), (labelColor.blue * 255).toInt(),
            )
            textSize = 10.dp.toPx()
            isAntiAlias = true
        }

        // Grid lines + y labels (min, middle, max)
        listOf(lo, (lo + hi) / 2, hi).forEach { y ->
            drawLine(g.separator, Offset(left, py(y)), Offset(right, py(y)), strokeWidth = 0.5.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                if (y % 1.0 == 0.0) y.toInt().toString() else String.format("%.1f", y),
                0f, py(y) + 4.dp.toPx(), paint,
            )
        }

        // Target line (e.g. "Target 7+")
        if (target != null) {
            drawLine(
                labelColor, Offset(left, py(target)), Offset(right, py(target)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
            if (targetLabel != null) {
                drawContext.canvas.nativeCanvas.drawText(targetLabel, left + 4.dp.toPx(), py(target) - 4.dp.toPx(), paint)
            }
        }

        val sorted = points.sortedBy { it.first }
        val path = Path()
        sorted.forEachIndexed { i, (x, y) ->
            if (i == 0) path.moveTo(px(x), py(y)) else path.lineTo(px(x), py(y))
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
        sorted.forEach { (x, y) -> drawCircle(color, radius = 3.5.dp.toPx(), center = Offset(px(x), py(y))) }
    }
}
