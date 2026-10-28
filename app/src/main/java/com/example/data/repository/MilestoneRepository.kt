package com.example.data.repository

import com.example.core.time.StreakDuration
import com.example.data.datastore.SteadfastPreferences
import com.example.data.model.MilestoneDefinition
import com.example.data.seed.AssetSeedLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

data class MilestoneStatus(
    val definition: MilestoneDefinition,
    val isAchieved: Boolean,
    val isNextTarget: Boolean
)

class MilestoneRepository(
    private val seedLoader: AssetSeedLoader,
    private val preferences: SteadfastPreferences
) {
    val definitions: List<MilestoneDefinition> by lazy {
        seedLoader.loadMilestones()
    }

    val shownCelebrations: Flow<Set<String>> = preferences.shownMilestones

    fun evaluateMilestones(duration: StreakDuration): List<MilestoneStatus> {
        val statuses = mutableListOf<MilestoneStatus>()
        var foundNext = false

        for (def in definitions) {
            val achieved = isMilestoneAchieved(def, duration)
            val isNext = !achieved && !foundNext
            if (isNext) foundNext = true

            statuses.add(
                MilestoneStatus(
                    definition = def,
                    isAchieved = achieved,
                    isNextTarget = isNext
                )
            )
        }
        return statuses
    }

    fun isMilestoneAchieved(def: MilestoneDefinition, duration: StreakDuration): Boolean {
        return when (def.kind) {
            "elapsed_days" -> duration.totalDays >= def.value
            "calendar_months" -> (duration.years * 12 + duration.months) >= def.value
            "calendar_years" -> duration.years >= def.value
            else -> duration.totalDays >= def.value
        }
    }

    fun getNextMilestone(duration: StreakDuration): MilestoneDefinition? {
        return definitions.firstOrNull { !isMilestoneAchieved(it, duration) }
    }

    /**
     * Checks if there is an achieved milestone that hasn't been celebrated yet in the active session.
     */
    suspend fun getPendingCelebration(duration: StreakDuration): MilestoneDefinition? {
        val shown = preferences.shownMilestones.firstOrNull() ?: emptySet()
        // Check highest achieved milestone that hasn't been shown
        val achieved = definitions.filter { isMilestoneAchieved(it, duration) }
        return achieved.lastOrNull { !shown.contains(it.id) }
    }

    suspend fun markCelebrated(milestoneId: String) {
        preferences.markMilestoneShown(milestoneId)
    }
}
