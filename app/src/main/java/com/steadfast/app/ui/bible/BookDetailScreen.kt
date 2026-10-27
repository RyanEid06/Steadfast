package com.steadfast.app.ui.bible

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.repository.BookWithProgress
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.SteadfastNavyDark

@Composable
fun BookDetailScreen(
    bookWithProgress: BookWithProgress?,
    currentReading: ReadingLocation?,
    onToggleChapter: (chapter: Int) -> Unit,
    onSetCurrentReading: (chapter: Int) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (bookWithProgress == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Book not found")
        }
        return
    }

    val book = bookWithProgress.book
    val completedSet = bookWithProgress.completedChapters
    val isCurrentBook = currentReading?.bookId == book.id
    val currentChapter = if (isCurrentBook) currentReading?.chapter else null

    var chapterActionDialogChapter by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier.testTag("book_detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = book.name,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Summary Card following Style 3
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (book.testament == "OT") "OLD TESTAMENT" else "NEW TESTAMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = SteadfastGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(bookWithProgress.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SteadfastGold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { bookWithProgress.percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SteadfastGold,
                    trackColor = MaterialTheme.colorScheme.surface,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${bookWithProgress.completedCount} of ${book.chapters} Chapters Read",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentChapter != null) {
                        Text(
                            text = "Reading: Ch. $currentChapter",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SteadfastGold
                        )
                    }
                }
            }
        }

        // Chapter grid instruction
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CHAPTERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap to toggle • Long press to bookmark",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Clean Numbered Chapter Grid following Style 3
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 54.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            items(book.chapters) { index ->
                val chapterNumber = index + 1
                val isCompleted = completedSet.contains(chapterNumber)
                val isCurrentReadingLocation = (currentChapter == chapterNumber)

                ChapterCell(
                    chapter = chapterNumber,
                    isCompleted = isCompleted,
                    isCurrentReading = isCurrentReadingLocation,
                    onClick = { onToggleChapter(chapterNumber) },
                    onLongClick = { chapterActionDialogChapter = chapterNumber }
                )
            }
        }
    }

    // Long press action dialog to set as current reading
    chapterActionDialogChapter?.let { ch ->
        AlertDialog(
            onDismissRequest = { chapterActionDialogChapter = null },
            title = { Text("${book.name} Chapter $ch") },
            text = { Text("Set this chapter as your active ${if (book.testament == "OT") "Old Testament" else "New Testament"} Current Reading bookmark?") },
            confirmButton = {
                Button(
                    onClick = {
                        chapterActionDialogChapter = null
                        onSetCurrentReading(ch)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SteadfastGold, contentColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Set as Current Reading")
                }
            },
            dismissButton = {
                TextButton(onClick = { chapterActionDialogChapter = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ChapterCell(
    chapter: Int,
    isCompleted: Boolean,
    isCurrentReading: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Style 3 Chapter cell:
    // Completed = Warm Gold filled with dark text
    // Unread = Neutral surface with cream text
    // Current Reading = Bookmark badge + distinctive highlight border (independent from color)
    @OptIn(ExperimentalFoundationApi::class)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isCompleted) SteadfastGold
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isCurrentReading) 2.dp else 1.dp,
                color = when {
                    isCurrentReading -> Color.White
                    isCompleted -> SteadfastGold
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("chapter_cell_$chapter"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = chapter.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) SteadfastNavyDark else MaterialTheme.colorScheme.onSurface
            )
        }

        // Current Reading bookmark badge in top-right corner
        if (isCurrentReading) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
