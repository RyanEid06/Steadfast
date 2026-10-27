package com.steadfast.app.ui.struggling

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.SteadfastGoldLight
import com.steadfast.app.ui.theme.SteadfastNavyDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class BreathPhase(val label: String, val seconds: Int, val isExpand: Boolean) {
    INHALE("Breathe In Slowly", 4, true),
    HOLD("Hold Gently", 4, true),
    EXHALE("Release & Surrender", 4, false),
    REST("Rest in Peace", 4, false)
}

@Composable
fun BreathingExerciseScreen(
    onDoneClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phaseIndex by remember { mutableIntStateOf(0) }
    var secondsRemaining by remember { mutableIntStateOf(4) }
    val phases = remember { BreathPhase.values() }
    val currentPhase = phases[phaseIndex]

    var targetScale by remember { mutableStateOf(0.7f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val phase = phases[phaseIndex]
            targetScale = if (phase.isExpand) 1.25f else 0.7f
            for (sec in phase.seconds downTo 1) {
                secondsRemaining = sec
                delay(1000L)
            }
            phaseIndex = (phaseIndex + 1) % phases.size
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 4000, easing = LinearEasing),
        label = "breath_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SteadfastNavyDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CALMING BREATH",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    color = SteadfastGold,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDoneClicked,
                    modifier = Modifier.testTag("breathing_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Central Expanding/Contracting Breathing Ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    SteadfastGold.copy(alpha = 0.45f),
                                    SteadfastNavyDark.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(2.dp, SteadfastGold.copy(alpha = 0.75f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentPhase.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$secondsRemaining",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = SteadfastGoldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "“Be still, and know that I am God.”",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Psalm 46:10",
                    style = MaterialTheme.typography.labelSmall,
                    color = SteadfastGold.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Bottom Finish Button
            Button(
                onClick = onDoneClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("breathing_finish_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SteadfastGold,
                    contentColor = SteadfastNavyDark
                )
            ) {
                Text(
                    text = "I Feel Calmer Now",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
