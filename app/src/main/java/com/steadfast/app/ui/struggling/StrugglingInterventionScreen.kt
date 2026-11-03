package com.steadfast.app.ui.struggling

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.R
import com.steadfast.app.data.repository.CuratedVerse
import com.steadfast.app.data.repository.VerseRepository
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.SteadfastNavyDark
import kotlinx.coroutines.launch

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun StrugglingInterventionScreen(
    verseRepository: VerseRepository,
    onTakeBreathingExercise: () -> Unit,
    onWriteNote: () -> Unit,
    onViewProgress: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val reducedMotion = com.steadfast.app.core.design.rememberReducedMotion()
    var currentVerse by remember { mutableStateOf<CuratedVerse?>(null) }
    var verseLoading by remember { mutableStateOf(true) }
    var verseError by remember { mutableStateOf<String?>(null) }

    suspend fun loadVerse() {
        verseLoading = true
        verseError = null
        try {
            currentVerse = verseRepository.getNextVerse()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            verseError = "Could not load Scripture. Please try another verse."
        } finally {
            verseLoading = false
        }
    }

    LaunchedEffect(verseRepository) { loadVerse() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D121D))
    ) {
        val compactLayout = maxHeight <= 600.dp || LocalDensity.current.fontScale >= 1.5f
        // Atmospheric background hero
        Image(
            painter = painterResource(id = R.drawable.hero_struggling),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.28f
        )

        // Gradient dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D121D).copy(alpha = 0.65f),
                            Color(0xFF0D121D).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = if (compactLayout) 0.dp else maxHeight)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (compactLayout) Arrangement.Top else Arrangement.SpaceBetween
        ) {
            // Upper-left Back Action & Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("struggling_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Dashboard",
                        tint = Color.White
                    )
                }

                Text(
                    text = "SANCTUARY OF PEACE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    color = SteadfastGold,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("struggling_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Headline & Calming Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "Take a moment.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You are not alone. God's grace is sufficient for you right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }

            // Central Prominent Light/Cream Scripture Card with Dark Text
            AnimatedContent(
                targetState = currentVerse,
                transitionSpec = {
                    if (reducedMotion) fadeIn(androidx.compose.animation.core.tween(0)) togetherWith fadeOut(androidx.compose.animation.core.tween(0))
                    else fadeIn() togetherWith fadeOut()
                },
                label = "verse_animation",
                modifier = Modifier
                    .fillMaxWidth()
            ) { verse ->
                if (verse == null) {
                    Text(verseError ?: "Loading Scripture…", color = Color.White, modifier = Modifier.padding(24.dp))
                } else {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F5EE)),
                        border = BorderStroke(1.5.dp, SteadfastGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.FormatQuote,
                                        contentDescription = null,
                                        tint = Color(0xFF8A6D1C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (verse.isFromCompletedChapter) "FROM YOUR READING" else "SCRIPTURE ANCHOR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.2.sp,
                                        color = Color(0xFF8A6D1C),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = listOfNotNull(verse.reference, verse.translation).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8A6D1C)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val verseText = "“${requireNotNull(verse.text)}”"

                            Text(
                                text = verseText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Serif,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = Color(0xFF1B2333),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (verseError != null && currentVerse != null) {
                Text(verseError!!, color = Color.White, style = MaterialTheme.typography.bodySmall)
            }

            // 4 Full-Width Action Rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action 1: Take a Deep Breath
                Button(
                    onClick = onTakeBreathingExercise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("struggling_breathing_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SteadfastGold,
                        contentColor = SteadfastNavyDark
                    )
                ) {
                    Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Take a Deep Breath (4-4-4-4)", fontWeight = FontWeight.Bold)
                }

                // Action 2: Another Verse
                OutlinedButton(
                    enabled = !verseLoading,
                    onClick = {
                        scope.launch {
                            loadVerse()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("struggling_another_verse_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SteadfastGold.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = SteadfastGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Give Me Another Verse", color = SteadfastGold, fontWeight = FontWeight.SemiBold)
                }

                // Action 3: Write a Note
                OutlinedButton(
                    onClick = onWriteNote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("struggling_write_note_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Write a Private Note", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                // Action 4: View My Progress
                OutlinedButton(
                    onClick = onViewProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("struggling_view_progress_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View My Progress & Milestones", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                // Dismiss Affirmation
                TextButton(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "I'm Grounded & Standing Firm Now",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
