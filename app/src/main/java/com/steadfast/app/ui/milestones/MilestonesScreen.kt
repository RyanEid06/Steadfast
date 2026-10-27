package com.steadfast.app.ui.milestones

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.data.model.MilestoneDefinition
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.data.repository.MilestoneRepository
import com.steadfast.app.data.repository.MilestoneStatus
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.SteadfastGoldDark

@Composable
fun MilestonesScreen(
    activeSession: StreakSessionEntity?,
    milestoneRepository: MilestoneRepository,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = remember(activeSession) {
        if (activeSession != null) {
            StreakDurationCalculator.calculate(
                startEpochMillis = activeSession.startEpochMillis,
                startZoneId = activeSession.startZoneId
            )
        } else {
            StreakDurationCalculator.calculate(System.currentTimeMillis(), "UTC")
        }
    }

    val milestoneStatuses = remember(duration) {
        milestoneRepository.evaluateMilestones(duration)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier.testTag("milestones_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Milestones Timeline",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "A PATH OF STEADFAST FAITH",
                    style = MaterialTheme.typography.labelSmall,
                    color = SteadfastGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            itemsIndexed(milestoneStatuses) { index, status ->
                TimelineMilestoneRow(
                    status = status,
                    isLast = index == milestoneStatuses.size - 1
                )
            }
        }
    }
}

@Composable
private fun TimelineMilestoneRow(
    status: MilestoneStatus,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Left Column: Indicator Node + Vertical Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            status.isAchieved -> SteadfastGold
                            status.isNextTarget -> SteadfastGoldDark.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (status.isAchieved) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Achieved",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (status.isNextTarget) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Next Target",
                        tint = SteadfastGold,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(
                            if (status.isAchieved) SteadfastGold.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    status.isNextTarget -> MaterialTheme.colorScheme.surfaceVariant
                    status.isAchieved -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            ),
            border = if (status.isNextTarget) BorderStroke(1.dp, SteadfastGold) else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = status.definition.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (status.isNextTarget || status.isAchieved) FontWeight.Bold else FontWeight.Normal,
                        color = if (status.isAchieved) SteadfastGold else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (status.isAchieved) "Completed" else if (status.isNextTarget) "Next Goal Ahead" else "Upcoming",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (status.isNextTarget) {
                    Text(
                        text = "IN SIGHT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SteadfastGold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SteadfastGold.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
