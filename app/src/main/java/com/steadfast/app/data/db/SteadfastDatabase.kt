package com.steadfast.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.steadfast.app.data.model.BibleChapterCompletionEntity
import com.steadfast.app.data.model.JournalEntryEntity
import com.steadfast.app.data.model.StreakSessionEntity

@Database(
    entities = [
        StreakSessionEntity::class,
        JournalEntryEntity::class,
        BibleChapterCompletionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SteadfastDatabase : RoomDatabase() {
    abstract fun streakDao(): StreakDao
    abstract fun journalDao(): JournalDao
    abstract fun bibleDao(): BibleDao

    companion object {
        @Volatile
        private var INSTANCE: SteadfastDatabase? = null

        fun getInstance(context: Context): SteadfastDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SteadfastDatabase::class.java,
                    "steadfast.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
