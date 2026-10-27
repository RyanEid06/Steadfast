package com.steadfast.app.ui.dashboard

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
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.luminance
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
import com.steadfast.app.R
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.data.repository.CuratedVerse
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.SteadfastStrugglingDark
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Compact Top Bar: Steadfast Identity + Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, SteadfastGold.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_steadfast_launcher_fg),
                            contentDescription = "Steadfast Emblem",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "STEADFAST",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
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
                    modifier = Modifier.testTag("dashboard_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Restrained Cross / Horizon Hero Atmosphere with Theme-Specific Artwork
            val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val heroDrawableRes = if (isDarkTheme) R.drawable.hero_dashboard_dark else R.drawable.hero_dashboard_light
            val heroOverlayGradient = if (isDarkTheme) {
                listOf(Color(0xFF1B2334).copy(alpha = 0.4f), Color.Transparent)
            } else {
                listOf(Color(0xFFE8DFCA).copy(alpha = 0.5f), Color.Transparent)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(heroOverlayGradient)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = heroDrawableRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    alpha = if (isDarkTheme) 0.35f else 0.65f
                )
                Text(
                    text = "“Stand firm, and you will win life.”",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 3. Circular Streak Counter — Emotional Center of Dashboard
            CircularStreakTimer(
                duration = duration,
                size = 230.dp,
                modifier = Modifier.testTag("dashboard_streak_circle")
            )

            // 4. One Real Scripture Encouragement Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    SteadfastGold.copy(alpha = 0.2f)
                ),
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
                        Text(
                            text = "SCRIPTURE OF PEACE",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp,
                            color = SteadfastGold,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = dailyEncouragement.reference,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (dailyEncouragement.text != null) {
                        Text(
                            text = "“${dailyEncouragement.text}”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    } else {
                        Text(
                            text = "Meditation Passage • Read in your personal Bible",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 5. One Prominent "I'm Struggling" Action
            Button(
                onClick = onStrugglingClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dashboard_struggling_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SteadfastStrugglingDark.copy(alpha = 0.9f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "I'm Struggling",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
