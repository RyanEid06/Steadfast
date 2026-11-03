package com.steadfast.app.ui.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun JourneyTimelineChart(sessions: List<com.steadfast.app.data.model.StreakSessionEntity>, modifier: Modifier = Modifier) {
    val now = com.steadfast.app.core.time.rememberForegroundTimeMillis(
        enabled = sessions.any { it.endEpochMillis == null }, intervalMillis = 60_000L
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val archivedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisLabelHeight = with(density) { textMeasurer.measure("May 20\n2026", labelStyle).size.height.toDp() + 10.dp }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val summary = sessions.sortedBy { it.startEpochMillis }.joinToString("; ") { session ->
        val zone = runCatching { ZoneId.of(session.startZoneId) }.getOrElse { ZoneId.systemDefault() }
        val date = Instant.ofEpochMilli(session.startEpochMillis).atZone(zone).format(dateFormatter)
        "Session ${session.id}, $date, ${streakElapsedDurationLabel(streakElapsedMillis(session.startEpochMillis, session.endEpochMillis ?: now))} elapsed${if (session.endEpochMillis == null) ", active" else ", archived"}"
    }
    Column(modifier = modifier.testTag("journey_timeline").semantics {
        contentDescription = "Streak timeline. Horizontal axis: chronological date and time. Vertical axis: elapsed streak duration. $summary"
    }) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val axes = remember(sessions, now / 60_000L) { journeyTimelineGeometry(sessions, now, 1f) }
            val unitName = when (axes.elapsedUnitMillis) {
                86_400_000L -> "days"
                3_600_000L -> "hours"
                60_000L -> "minutes"
                else -> "seconds"
            }
            val elapsedLabels = axes.elapsedTicks.associateWith {
                textMeasurer.measure(DecimalFormat("0.##").format(it.toDouble() / axes.elapsedUnitMillis), labelStyle)
            }
            val axisWidth = with(density) { elapsedLabels.values.maxOf { it.size.width }.toDp() + 12.dp }
            val viewport = (maxWidth - axisWidth).value.coerceAtLeast(1f)
            val maxContentWidth = maxOf(viewport, 60_000f / density.density)
            val geometry = remember(sessions, now / 60_000L, viewport, maxContentWidth) {
                journeyTimelineGeometry(sessions, now, viewport, maxContentWidth)
            }
            val elapsedLabelHeight = elapsedLabels.values.maxOf { it.size.height }.toFloat()
            val plotTop = maxOf(with(density) { 10.dp.toPx() }, elapsedLabelHeight / 2)
            Column(Modifier.fillMaxSize()) {
                Text("Streak ($unitName)", style = labelStyle)
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Canvas(Modifier.width(axisWidth).fillMaxHeight()) {
                        val top = plotTop
                        val bottom = size.height - axisLabelHeight.toPx()
                        val tickSpacing = (bottom - top) / (geometry.elapsedTicks.size - 1).coerceAtLeast(1)
                        val displayedTicks = if (tickSpacing < elapsedLabelHeight + 4.dp.toPx()) {
                            listOf(geometry.elapsedTicks.first(), geometry.elapsedTicks.last())
                        } else geometry.elapsedTicks
                        displayedTicks.forEach { millis ->
                            val text = elapsedLabels.getValue(millis)
                            val y = bottom - (bottom - top) * geometry.yFraction(millis)
                            drawText(text, topLeft = Offset(0f, y - text.size.height / 2))
                        }
                        drawLine(gridColor, Offset(size.width - 1.dp.toPx(), top), Offset(size.width - 1.dp.toPx(), bottom))
                    }
                    val scroll = rememberScrollState()
                    val scrollModifier = if (geometry.scrolls) Modifier.horizontalScroll(scroll) else Modifier
                    Box(Modifier.weight(1f).fillMaxHeight().clipToBounds().then(scrollModifier)) {
                        Canvas(Modifier.width(geometry.contentWidthDp.dp).fillMaxHeight()) {
                            val top = plotTop
                            val bottom = size.height - axisLabelHeight.toPx()
                            fun position(point: JourneyTimelinePoint) = Offset(
                                geometry.xFraction(point.epochMillis) * size.width,
                                bottom - geometry.yFraction(point.elapsedMillis) * (bottom - top)
                            )
                            geometry.elapsedTicks.forEach { millis ->
                                val y = bottom - geometry.yFraction(millis) * (bottom - top)
                                drawLine(gridColor, Offset(0f, y), Offset(size.width, y))
                            }
                            val formatter = DateTimeFormatter.ofPattern(
                                if (geometry.endEpochMillis - geometry.startEpochMillis <= 2 * 86_400_000L) "MMM d\nHH:mm" else "MMM d\nyyyy"
                            )
                            geometry.timeTicks.forEach { timestamp ->
                                val x = geometry.xFraction(timestamp) * size.width
                                drawLine(gridColor, Offset(x, top), Offset(x, bottom + 4.dp.toPx()))
                                val text = textMeasurer.measure(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(formatter), labelStyle)
                                drawText(text, topLeft = Offset((x - text.size.width / 2).coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f)), bottom + 6.dp.toPx()))
                            }
                            geometry.strokes.forEach { stroke ->
                                val path = Path()
                                stroke.points.forEachIndexed { index, point ->
                                    val p = position(point)
                                    if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                                }
                                val color = if (stroke.active) lineColor else archivedColor
                                drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                                val peak = position(stroke.points[1])
                                drawCircle(color, radius = 3.dp.toPx(), center = peak)
                            }
                        }
                    }
                }
                Text(if (geometry.scrolls) "Date / time · Swipe to explore history" else "Date / time", style = labelStyle)
            }
        }
    }
}
