package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.R
import com.example.core.time.StreakDurationCalculator
import com.example.data.model.StreakSessionEntity
import com.example.data.repository.CuratedVerse
import com.example.ui.theme.SteadfastGold
import com.example.ui.theme.SteadfastGoldDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun DashboardScreen(
    activeSession: StreakSessionEntity?,
    dailyEncouragement: CuratedVerse,
    onStrugglingClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    // In-memory ticker: updates once per second only while actively in the foreground
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isScreenVisible by remember { mutableStateOf(true) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isScreenVisible = true
        currentTimeMillis = System.currentTimeMillis()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        isScreenVisible = false
    }

    LaunchedEffect(isScreenVisible, activeSession) {
        while (isActive && isScreenVisible && activeSession != null) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val duration = remember(activeSession, currentTimeMillis) {
        if (activeSession != null) {
            StreakDurationCalculator.calculate(
                startEpochMillis = activeSession.startEpochMillis,
                startZoneId = activeSession.startZoneId,
                nowEpochMillis = currentTimeMillis
            )
        } else {
            StreakDurationCalculator.calculate(
                startEpochMillis = System.currentTimeMillis(),
                startZoneId = "UTC",
                nowEpochMillis = System.currentTimeMillis()
            )
        }
    }

    val focusLabel = remember(activeSession) {
        activeSession?.recoveryFocusId?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Self-Discipline"
    }

    // Fixed, no-scroll composition strictly following DESIGN_FREEZE.md & UI_SPEC.md
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Atmospheric top hero backdrop fading smoothly into the surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hero_dashboard_dark),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.35f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Compact Top Bar: Steadfast Cross Emblem + Title + Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF161E2E))
                            .border(1.dp, SteadfastGold.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // In-app brand mark: clean geometric gold cross
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                            val gold = Color(0xFFD4AF37)
                            val stroke = 2.2.dp.toPx()
                            // Vertical beam
                            drawLine(
                                color = gold,
                                start = androidx.compose.ui.geometry.Offset(size.width / 2f, 1.dp.toPx()),
                                end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height - 1.dp.toPx()),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                            // Horizontal beam
                            val horizY = size.height * 0.38f
                            drawLine(
                                color = gold,
                                start = androidx.compose.ui.geometry.Offset(2.dp.toPx(), horizY),
                                end = androidx.compose.ui.geometry.Offset(size.width - 2.dp.toPx(), horizY),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "STEADFAST",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = SteadfastGold
                        )
                        Text(
                            text = focusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dashboard_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Inspiring Biblical horizon reminder
            Text(
                text = "“Stand firm, and you will win life.”",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            // 3. Circular Streak Counter — Emotional Center of Dashboard
            CircularStreakTimer(
                duration = duration,
                size = 230.dp,
                modifier = Modifier.testTag("dashboard_streak_circle")
            )

            // 4. One Real Scripture Encouragement Card (Style 1 structure, Style 3 palette)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                border = BorderStroke(1.dp, SteadfastGold.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_scripture_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FormatQuote,
                                contentDescription = null,
                                tint = SteadfastGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SCRIPTURE OF PEACE",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                color = SteadfastGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dailyEncouragement.reference,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SteadfastGold.copy(alpha = 0.18f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "NLT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SteadfastGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "“I can do all things through Christ who strengthens me.”",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }

            // 5. One Prominent "I'm Struggling" Action — Refined Gold/Dark Navy Pill Button
            Button(
                onClick = onStrugglingClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dashboard_struggling_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B2333),
                    contentColor = SteadfastGold
                ),
                border = BorderStroke(1.2.dp, SteadfastGold.copy(alpha = 0.85f))
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SteadfastGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "I'm Struggling",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = SteadfastGold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
