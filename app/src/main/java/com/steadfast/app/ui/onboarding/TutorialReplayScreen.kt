package com.steadfast.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.ui.theme.SteadfastGold

private data class TutorialPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val highlights: List<Pair<String, String>>
)

/**
 * Presentation-only tutorial tour.
 * Strictly adheres to STEADFAST_V1_CORRECTION_QA.md:
 * Replaying this tutorial can NEVER mutate recovery start time, create duplicate streaks,
 * alter Bible progress, change theme, or modify lock settings.
 */
@Composable
fun TutorialReplayScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember {
        listOf(
            TutorialPage(
                title = "Sanctuary of Grace",
                subtitle = "Steadfast is your private, shame-free sanctuary for Christian recovery and daily Scripture progress.",
                icon = Icons.Outlined.Shield,
                highlights = listOf(
                    "Zero Accounts & No Cloud" to "Everything lives exclusively on your local device. No telemetry, no accounts, and no internet access.",
                    "Personal Freedom" to "No mandatory daily check-ins or guilt mechanics. You are called to stand firm in grace.",
                    "Rooted in Scripture" to "Daily strength drawn from God's Word rather than willpower alone."
                )
            ),
            TutorialPage(
                title = "The Dashboard & Timer",
                subtitle = "The focal center of your recovery journey.",
                icon = Icons.Outlined.Timer,
                highlights = listOf(
                    "Calendar-Accurate Counter" to "Before 30 days, shows Days, Hours, and Seconds. At 30 days, expands into Months. At 1 year, adds Years.",
                    "Live Precision" to "Updates second-by-second while you are looking at the screen with zero battery drain when backgrounded.",
                    "Scripture of Peace" to "A verified passage paired with its exact reference to anchor each day."
                )
            ),
            TutorialPage(
                title = "Emergency Grace Sanctuary",
                subtitle = "Instant, judgment-free support when urges strike.",
                icon = Icons.Outlined.Shield,
                highlights = listOf(
                    "One-Tap Access" to "The prominent 'I'm Struggling' button on the Dashboard opens immediate intervention.",
                    "Prioritized Scripture" to "Prioritizes verses from chapters you have completed, with verified anti-repeat cycling.",
                    "Guided Breathing" to "4-4-4-4 Box Breathing exercise with real-time haptic cues to restore calm.",
                    "Urgent Journaling" to "Tap 'Write Note' to immediately capture raw emotions with the URGE tag."
                )
            ),
            TutorialPage(
                title = "Canonical Bible Progress",
                subtitle = "Read at your own pace through all 66 canonical Protestant books and 1,189 chapters.",
                icon = Icons.Outlined.AutoStories,
                highlights = listOf(
                    "Chapter Grid" to "Tap any chapter to mark completed or unread. Warm gold indicates completed.",
                    "Independent Bookmarks" to "Long-press any chapter to set your active reading location (one OT bookmark and one NT bookmark).",
                    "No Bible Streak" to "Daily targets remain purely motivational. Completing a chapter never artificially forces the bookmark."
                )
            ),
            TutorialPage(
                title = "Sanctuary Journal & Security",
                subtitle = "Your private thoughts and milestones kept safe.",
                icon = Icons.Outlined.EditNote,
                highlights = listOf(
                    "Contextual Notes" to "Organize your entries by GENERAL, URGE, or RESTART contexts with full search and filtering.",
                    "Encrypted Backups" to "Export your entire history to a PBKDF2 + AES-GCM encrypted .steadfast file.",
                    "Device & Biometric Lock" to "Lock Steadfast behind fingerprint, face, or device PIN for absolute peace of mind."
                )
            )
        )
    }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    val currentPage = pages[currentPageIndex]
    val totalPages = pages.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
                    text = "STEADFAST GUIDE • ${currentPageIndex + 1} OF $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = SteadfastGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("tutorial_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tutorial",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Central Animated Card Content
            AnimatedContent(
                targetState = currentPageIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tutorial_page_anim",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIdx ->
                val page = pages[pageIdx]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SteadfastGold.copy(alpha = 0.12f))
                            .border(1.dp, SteadfastGold.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = SteadfastGold,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = page.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            page.highlights.forEach { (heading, body) ->
                                Column {
                                    Text(
                                        text = heading,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SteadfastGold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPageIndex > 0) {
                    OutlinedButton(
                        onClick = { currentPageIndex-- },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("tutorial_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Previous")
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Page indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until totalPages) {
                        Box(
                            modifier = Modifier
                                .size(if (i == currentPageIndex) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == currentPageIndex) SteadfastGold
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                if (currentPageIndex < totalPages - 1) {
                    Button(
                        onClick = { currentPageIndex++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SteadfastGold,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("tutorial_next_button")
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.size(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SteadfastGold,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("tutorial_finish_button")
                    ) {
                        Text("Finish Tour", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
