package com.steadfast.app.ui.struggling

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
fun StrugglingInterventionScreen(
    verseRepository: VerseRepository,
    onTakeBreathingExercise: () -> Unit,
    onWriteNote: () -> Unit,
    onViewProgress: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var currentVerse by remember { mutableStateOf<CuratedVerse?>(null) }

    LaunchedEffect(Unit) {
        currentVerse = verseRepository.getNextVerse()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SteadfastNavyDark)
    ) {
        // Atmospheric background hero
        Image(
            painter = painterResource(id = R.drawable.hero_struggling),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.25f
        )

        // Gradient dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SteadfastNavyDark.copy(alpha = 0.7f),
                            SteadfastNavyDark.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SANCTUARY OF GRACE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    color = SteadfastGold.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("struggling_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Central Verse Card Focus
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SteadfastGold.copy(alpha = 0.15f))
                        .border(1.dp, SteadfastGold.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_steadfast_launcher_fg),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedContent(
                    targetState = currentVerse,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "verse_animation"
                ) { verse ->
                    if (verse != null) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141B2B).copy(alpha = 0.85f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SteadfastGold.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (verse.isFromCompletedChapter) {
                                    Text(
                                        text = "FROM YOUR COMPLETED SCRIPTURE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.2.sp,
                                        color = SteadfastGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                Text(
                                    text = verse.reference,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = SteadfastGold,
                                    textAlign = TextAlign.Center
                                )

                                if (verse.text != null) {
                                    Text(
                                        text = "“${verse.text}”",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        color = Color(0xFFF6F4ED),
                                        textAlign = TextAlign.Center
                                    )
                                    verse.translation?.let { tr ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "— $tr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SteadfastGold.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Open your Bible to this passage to reflect and meditate on God’s Word.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp,
                                            color = Color(0xFFE0DCD3),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            currentVerse = verseRepository.getNextVerse()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SteadfastGold.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = SteadfastGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Give Me Another Verse", color = SteadfastGold)
                }
            }

            // Stack of Support Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Take a Deep Breath
                Button(
                    onClick = onTakeBreathingExercise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("struggling_breathing_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SteadfastGold,
                        contentColor = SteadfastNavyDark
                    )
                ) {
                    Icon(Icons.Default.SelfImprovement, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take a Deep Breath", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 2. Write a Note
                    OutlinedButton(
                        onClick = onWriteNote,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("struggling_write_note_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Write Note", color = Color.White)
                    }

                    // 3. View Progress
                    OutlinedButton(
                        onClick = onViewProgress,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("struggling_view_progress_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("My Progress", color = Color.White)
                    }
                }

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I'm Grounded & Standing Firm Now", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}
