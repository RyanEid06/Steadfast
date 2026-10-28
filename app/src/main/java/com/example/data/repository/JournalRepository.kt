package com.example.data.repository

import com.example.data.db.JournalDao
import com.example.data.model.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {

    val allEntries: Flow<List<JournalEntryEntity>> = journalDao.getAllEntries()

    fun getEntriesByContext(context: String): Flow<List<JournalEntryEntity>> {
        return journalDao.getEntriesByContext(context)
    }

    suspend fun getEntryById(id: Long): JournalEntryEntity? {
        return journalDao.getEntryById(id)
    }

    suspend fun addEntry(
        body: String,
        context: String? = null,
        streakSessionId: Long? = null
    ): Long {
        val now = System.currentTimeMillis()
        val entry = JournalEntryEntity(
            body = body.trim(),
            context = context?.trim()?.uppercase(),
            streakSessionId = streakSessionId,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        return journalDao.insertEntry(entry)
    }

    suspend fun updateEntry(id: Long, body: String, context: String? = null) {
        val existing = journalDao.getEntryById(id) ?: return
        val updated = existing.copy(
            body = body.trim(),
            context = context ?: existing.context,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        journalDao.updateEntry(updated)
    }

    suspend fun deleteEntry(id: Long) {
        journalDao.deleteEntryById(id)
    }
}
