package com.steadfast.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "streak_sessions")
data class StreakSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recoveryFocusId: String,
    val startEpochMillis: Long,
    val startZoneId: String,
    val endEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["createdAtEpochMillis"])]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val body: String,
    val context: String? = null, // GENERAL, RESTART, URGE
    val streakSessionId: Long? = null
)

@Entity(
    tableName = "bible_chapter_completions",
    primaryKeys = ["bookId", "chapter"]
)
data class BibleChapterCompletionEntity(
    val bookId: Int,
    val chapter: Int
)
