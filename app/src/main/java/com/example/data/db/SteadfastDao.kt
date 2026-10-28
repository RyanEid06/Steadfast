package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.BibleChapterCompletionEntity
import com.example.data.model.JournalEntryEntity
import com.example.data.model.StreakSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_sessions WHERE endEpochMillis IS NULL ORDER BY id DESC LIMIT 1")
    fun getActiveSession(): Flow<StreakSessionEntity?>

    @Query("SELECT * FROM streak_sessions WHERE endEpochMillis IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun getActiveSessionSync(): StreakSessionEntity?

    @Query("SELECT * FROM streak_sessions ORDER BY startEpochMillis DESC")
    fun getAllSessions(): Flow<List<StreakSessionEntity>>

    @Query("SELECT * FROM streak_sessions ORDER BY startEpochMillis DESC")
    suspend fun getAllSessionsSync(): List<StreakSessionEntity>

    @Query("SELECT * FROM streak_sessions WHERE endEpochMillis IS NOT NULL ORDER BY startEpochMillis DESC")
    fun getArchivedSessions(): Flow<List<StreakSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StreakSessionEntity): Long

    @Update
    suspend fun updateSession(session: StreakSessionEntity)

    @Query("UPDATE streak_sessions SET endEpochMillis = :endEpochMillis WHERE id = :sessionId")
    suspend fun archiveSession(sessionId: Long, endEpochMillis: Long)

    @Query("DELETE FROM streak_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM streak_sessions")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(sessions: List<StreakSessionEntity>) {
        clearAll()
        for (session in sessions) {
            insertSession(session)
        }
    }
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY createdAtEpochMillis DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY createdAtEpochMillis DESC")
    suspend fun getAllEntriesSync(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE context = :context ORDER BY createdAtEpochMillis DESC")
    fun getEntriesByContext(context: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM journal_entries")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entries: List<JournalEntryEntity>) {
        clearAll()
        for (entry in entries) {
            insertEntry(entry)
        }
    }
}

@Dao
interface BibleDao {
    @Query("SELECT * FROM bible_chapter_completions")
    fun getAllCompleted(): Flow<List<BibleChapterCompletionEntity>>

    @Query("SELECT * FROM bible_chapter_completions")
    suspend fun getAllCompletedSync(): List<BibleChapterCompletionEntity>

    @Query("SELECT COUNT(*) FROM bible_chapter_completions")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT chapter FROM bible_chapter_completions WHERE bookId = :bookId")
    fun getCompletedChaptersForBook(bookId: Int): Flow<List<Int>>

    @Query("SELECT chapter FROM bible_chapter_completions WHERE bookId = :bookId")
    suspend fun getCompletedChaptersForBookSync(bookId: Int): List<Int>

    @Query("SELECT COUNT(*) > 0 FROM bible_chapter_completions WHERE bookId = :bookId AND chapter = :chapter")
    fun isChapterCompleted(bookId: Int, chapter: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) > 0 FROM bible_chapter_completions WHERE bookId = :bookId AND chapter = :chapter")
    suspend fun isChapterCompletedSync(bookId: Int, chapter: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(entity: BibleChapterCompletionEntity)

    @Query("DELETE FROM bible_chapter_completions WHERE bookId = :bookId AND chapter = :chapter")
    suspend fun deleteCompletion(bookId: Int, chapter: Int)

    @Query("DELETE FROM bible_chapter_completions")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(completions: List<BibleChapterCompletionEntity>) {
        clearAll()
        for (completion in completions) {
            insertCompletion(completion)
        }
    }
}
