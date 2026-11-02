package com.steadfast.app.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.core.time.StreakDuration
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.SteadfastGoldDark
import com.steadfast.app.ui.theme.SteadfastGoldLight

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun CircularStreakTimer(
    duration: StreakDuration,
    size: Dp = 230.dp,
    modifier: Modifier = Modifier,
    unitsBesideRing: Boolean = false
) {
    val displayUnits = StreakDurationCalculator.getDisplayUnits(duration)
    val isReducedMotion = com.steadfast.app.core.design.rememberReducedMotion()
    val compactText = size < 230.dp || LocalDensity.current.fontScale >= 1.5f

    val pulseAlpha = if (isReducedMotion) 1f else {
        val transition = rememberInfiniteTransition(label = "ring_glow")
        transition.animateFloat(
            initialValue = 0.7f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ), label = "pulse_alpha"
        ).value
    }

    if (unitsBesideRing) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TimerRingSurface(duration, size, pulseAlpha, modifier) { }
            Spacer(Modifier.width(if (size <= 80.dp) 4.dp else 12.dp))
            TimerUnitGrid(
                displayUnits,
                columns = 3,
                modifier = Modifier.weight(1f),
                noColumnPadding = true
            )
        }
    } else {
        TimerRingSurface(duration, size, pulseAlpha, modifier) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "STEADFAST STREAK",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    lineHeight = if (compactText) 12.sp else MaterialTheme.typography.labelSmall.lineHeight,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (compactText) TimerUnitGrid(displayUnits, columns = if (displayUnits.size >= 4) 2 else displayUnits.size)
                else Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                    displayUnits.forEachIndexed { index, unit ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = if (displayUnits.size >= 5) 0.dp else 4.dp)
                        ) {
                            Text(
                                text = unit.value.toString().padStart(if (unit.label == "days" && unit.value < 10) 1 else 2, '0'),
                                style = MaterialTheme.typography.headlineLarge,
                                fontSize = if (displayUnits.size >= 5) 19.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Serif
                            )
                            Text(unit.label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (index < displayUnits.size - 1) Text(
                            text = ":", style = MaterialTheme.typography.titleMedium,
                            fontSize = if (displayUnits.size >= 5) 16.sp else 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun TimerUnitGrid(
    displayUnits: List<com.steadfast.app.core.time.TimeUnitDisplay>,
    columns: Int,
    modifier: Modifier = Modifier,
    noColumnPadding: Boolean = false
) {
    FlowRow(
        modifier = modifier.testTag("dashboard_streak_units"),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        maxItemsInEachRow = columns
    ) {
        displayUnits.forEach { unit ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = if (noColumnPadding) 0.dp else 2.dp)
            ) {
                Text(
                    text = unit.value.toString().padStart(if (unit.label == "days" && unit.value < 10) 1 else 2, '0'),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = unit.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TimerRingSurface(
    duration: StreakDuration,
    size: Dp,
    pulseAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 5.dp.toPx()
            drawCircle(color = Color(0xFF222C42).copy(alpha = 0.5f), style = Stroke(width = strokeWidth))
            val sweep = ((duration.seconds / 60f) * 360f).coerceIn(10f, 360f)
            drawArc(
                brush = Brush.sweepGradient(listOf(SteadfastGoldDark, SteadfastGold, SteadfastGoldLight, SteadfastGoldDark)),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                alpha = pulseAlpha
            )
        }
        Box(
            modifier = Modifier.size(size - 32.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))))
                .border(1.dp, SteadfastGold.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
