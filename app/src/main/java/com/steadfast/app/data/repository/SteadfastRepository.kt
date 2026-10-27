package com.steadfast.app.data.repository

import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.StreakDao
import com.steadfast.app.data.model.JournalEntryEntity
import com.steadfast.app.data.model.StreakSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.ZoneId

class SteadfastRepository(
    private val streakDao: StreakDao,
    private val journalRepository: JournalRepository,
    private val preferences: SteadfastPreferences
) {
    val activeSession: Flow<StreakSessionEntity?> = streakDao.getActiveSession()
    val allSessions: Flow<List<StreakSessionEntity>> = streakDao.getAllSessions()
    val archivedSessions: Flow<List<StreakSessionEntity>> = streakDao.getArchivedSessions()

    suspend fun getActiveSessionSync(): StreakSessionEntity? = streakDao.getActiveSessionSync()

    suspend fun startInitialSession(
        focusId: String,
        startEpochMillis: Long,
        startZoneId: String = ZoneId.systemDefault().id
    ): Long {
        preferences.setRecoveryFocusId(focusId)
        preferences.clearShownMilestones()
        val session = StreakSessionEntity(
            recoveryFocusId = focusId,
            startEpochMillis = startEpochMillis,
            startZoneId = startZoneId,
            endEpochMillis = null
        )
        return streakDao.insertSession(session)
    }

    suspend fun restartStreak(
        endEpochMillis: Long = System.currentTimeMillis(),
        newStartEpochMillis: Long = System.currentTimeMillis(),
        newZoneId: String = ZoneId.systemDefault().id,
        optionalNote: String? = null
    ) {
        val currentActive = streakDao.getActiveSessionSync()
        val focusId = currentActive?.recoveryFocusId ?: preferences.recoveryFocusId.firstOrNull() ?: "pornography"

        if (currentActive != null) {
            streakDao.archiveSession(currentActive.id, endEpochMillis)
            if (!optionalNote.isNullOrBlank()) {
                journalRepository.addEntry(
                    body = optionalNote,
                    context = "RESTART",
                    streakSessionId = currentActive.id
                )
            }
        }

        // Reset shown milestone celebrations for new session
        preferences.clearShownMilestones()

        // Create new active session
        val newSession = StreakSessionEntity(
            recoveryFocusId = focusId,
            startEpochMillis = newStartEpochMillis,
            startZoneId = newZoneId,
            endEpochMillis = null
        )
        streakDao.insertSession(newSession)
    }

    suspend fun updateSessionStart(
        sessionId: Long,
        newStartEpochMillis: Long,
        newZoneId: String = ZoneId.systemDefault().id
    ) {
        val session = streakDao.getAllSessionsSync().find { it.id == sessionId } ?: return
        streakDao.updateSession(
            session.copy(
                startEpochMillis = newStartEpochMillis,
                startZoneId = newZoneId
            )
        )
    }

    suspend fun updateActiveSessionFocus(newFocusId: String) {
        preferences.setRecoveryFocusId(newFocusId)
        val current = streakDao.getActiveSessionSync() ?: return
        streakDao.updateSession(current.copy(recoveryFocusId = newFocusId))
    }

    /**
     * Derives longest streak duration in days across all historical and current sessions.
     */
    fun calculateLongestStreakDays(sessions: List<StreakSessionEntity>, nowMillis: Long = System.currentTimeMillis()): Long {
        var longestDays = 0L
        for (session in sessions) {
            val endMillis = session.endEpochMillis ?: nowMillis
            val duration = StreakDurationCalculator.calculate(
                startEpochMillis = session.startEpochMillis,
                startZoneId = session.startZoneId,
                nowEpochMillis = endMillis
            )
            if (duration.totalDays > longestDays) {
                longestDays = duration.totalDays
            }
        }
        return longestDays
    }
}
