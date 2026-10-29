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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.core.time.StreakDuration
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.SteadfastGoldDark
import com.steadfast.app.ui.theme.SteadfastGoldLight

@Composable
fun CircularStreakTimer(
    duration: StreakDuration,
    size: Dp = 230.dp,
    modifier: Modifier = Modifier
) {
    val displayUnits = StreakDurationCalculator.getDisplayUnits(duration)
    val isReducedMotion = com.steadfast.app.core.design.rememberReducedMotion()

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

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background ring canvas
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 5.dp.toPx()
            // Track circle
            drawCircle(
                color = Color(0xFF222C42).copy(alpha = 0.5f),
                style = Stroke(width = strokeWidth)
            )

            // Gold progress arc (sweep 60 to 360 based on seconds or days cycle)
            val sweep = ((duration.seconds / 60f) * 360f).coerceIn(10f, 360f)
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(SteadfastGoldDark, SteadfastGold, SteadfastGoldLight, SteadfastGoldDark)
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                alpha = pulseAlpha
            )
        }

        // Inner Content Box
        Box(
            modifier = Modifier
                .size(size - 32.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(1.dp, SteadfastGold.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "STEADFAST STREAK",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    color = SteadfastGold.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time Units Display Row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    displayUnits.forEachIndexed { index, unit ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = if (displayUnits.size >= 5) 2.dp else 4.dp)
                        ) {
                            Text(
                                text = unit.value.toString().padStart(if (unit.label == "days" && unit.value < 10) 1 else 2, '0'),
                                style = MaterialTheme.typography.headlineLarge,
                                fontSize = if (displayUnits.size >= 5) 19.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = unit.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (index < displayUnits.size - 1) {
                            Text(
                                text = ":",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = if (displayUnits.size >= 5) 16.sp else 20.sp,
                                color = SteadfastGold.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
