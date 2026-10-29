package com.steadfast.app.data.repository

import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.data.datastore.SteadfastPreferences
import androidx.room.withTransaction
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.StreakSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.ZoneId

class SteadfastRepository(
    private val database: SteadfastDatabase,
    private val journalRepository: JournalRepository,
    private val preferences: SteadfastPreferences
) {
    private val streakDao = database.streakDao()
    val activeSession: Flow<StreakSessionEntity?> = streakDao.getActiveSession()
    val allSessions: Flow<List<StreakSessionEntity>> = streakDao.getAllSessions()
    val archivedSessions: Flow<List<StreakSessionEntity>> = streakDao.getArchivedSessions()

    suspend fun getActiveSessionSync(): StreakSessionEntity? = streakDao.getActiveSessionSync()

    suspend fun startInitialSession(
        focusId: String,
        startEpochMillis: Long,
        startZoneId: String = ZoneId.systemDefault().id
    ): Long = database.withTransaction {
        validateStart(startEpochMillis, startZoneId)
        require(streakDao.getAllSessionsSync().isEmpty()) { "Recovery history already exists. Use Start Again to retain it." }
        preferences.setRecoveryFocusId(focusId)
        preferences.clearShownMilestones()
        val session = StreakSessionEntity(
            recoveryFocusId = focusId,
            startEpochMillis = startEpochMillis,
            startZoneId = startZoneId,
            endEpochMillis = null
        )
        streakDao.insertSession(session)
    }

    suspend fun restartStreak(
        newStartEpochMillis: Long = System.currentTimeMillis(),
        newZoneId: String = ZoneId.systemDefault().id,
        optionalNote: String? = null
    ) = database.withTransaction {
        validateStart(newStartEpochMillis, newZoneId)
        val history = streakDao.getAllSessionsSync()
        require(history.count { it.endEpochMillis == null } <= 1) { "Recovery history has multiple active sessions." }
        val currentActive = history.singleOrNull { it.endEpochMillis == null }
        require(currentActive == null || newStartEpochMillis > currentActive.startEpochMillis) {
            "Choose a restart time after your current streak began."
        }
        require(history.filter { it.endEpochMillis != null }.all { it.endEpochMillis!! <= newStartEpochMillis }) {
            "The restart time cannot overlap your recovery history."
        }
        val focusId = currentActive?.recoveryFocusId ?: preferences.recoveryFocusId.firstOrNull() ?: "pornography"

        if (currentActive != null) {
            streakDao.archiveSession(currentActive.id, newStartEpochMillis)
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
        Unit
    }

    suspend fun updateSessionStart(
        sessionId: Long,
        newStartEpochMillis: Long,
        newZoneId: String = ZoneId.systemDefault().id
    ) = database.withTransaction {
        validateStart(newStartEpochMillis, newZoneId)
        val history = streakDao.getAllSessionsSync()
        val session = history.find { it.id == sessionId } ?: return@withTransaction
        require(session.endEpochMillis == null || newStartEpochMillis <= session.endEpochMillis) { "A session cannot start after it ended." }
        val candidate = session.copy(startEpochMillis = newStartEpochMillis, startZoneId = newZoneId)
        val ordered = history.map { if (it.id == sessionId) candidate else it }
            .sortedWith(compareBy<StreakSessionEntity> { it.startEpochMillis }.thenBy { it.endEpochMillis ?: Long.MAX_VALUE })
        require(ordered.zipWithNext().all { (previous, next) -> previous.endEpochMillis != null && previous.endEpochMillis <= next.startEpochMillis }) {
            "The start time cannot overlap another recovery session."
        }
        streakDao.updateSession(
            candidate
        )
    }

    private fun validateStart(start: Long, zone: String) {
        require(start > 0 && start <= System.currentTimeMillis()) { "Choose a valid start time that is not in the future." }
        require(runCatching { ZoneId.of(zone) }.isSuccess) { "Choose a valid start timezone." }
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
