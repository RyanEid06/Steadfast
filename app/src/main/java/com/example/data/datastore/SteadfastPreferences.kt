package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "steadfast_settings")

data class ReadingLocation(
    val bookId: Int,
    val chapter: Int
)

class SteadfastPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val RECOVERY_FOCUS_ID = stringPreferencesKey("recovery_focus_id")
        val READING_GOAL_MIN = intPreferencesKey("reading_goal_min")
        val OT_CURRENT_BOOK_ID = intPreferencesKey("ot_current_book_id")
        val OT_CURRENT_CHAPTER = intPreferencesKey("ot_current_chapter")
        val NT_CURRENT_BOOK_ID = intPreferencesKey("nt_current_book_id")
        val NT_CURRENT_CHAPTER = intPreferencesKey("nt_current_chapter")
        val SHOWN_MILESTONES = stringPreferencesKey("shown_milestones")
        val LAST_BACKGROUND_EPOCH = longPreferencesKey("last_background_epoch")
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val themeSetting: Flow<ThemeSetting> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "DARK" -> ThemeSetting.DARK
            "LIGHT" -> ThemeSetting.LIGHT
            else -> ThemeSetting.SYSTEM
        }
    }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_LOCK_ENABLED] ?: false
    }

    val recoveryFocusId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECOVERY_FOCUS_ID] ?: "pornography"
    }

    val readingGoalMinimum: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.READING_GOAL_MIN] ?: 2
    }

    val otCurrentReading: Flow<ReadingLocation?> = context.dataStore.data.map { prefs ->
        val bookId = prefs[Keys.OT_CURRENT_BOOK_ID]
        val chapter = prefs[Keys.OT_CURRENT_CHAPTER]
        if (bookId != null && chapter != null) ReadingLocation(bookId, chapter) else null
    }

    val ntCurrentReading: Flow<ReadingLocation?> = context.dataStore.data.map { prefs ->
        val bookId = prefs[Keys.NT_CURRENT_BOOK_ID]
        val chapter = prefs[Keys.NT_CURRENT_CHAPTER]
        if (bookId != null && chapter != null) ReadingLocation(bookId, chapter) else null
    }

    val shownMilestones: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SHOWN_MILESTONES].orEmpty()
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setThemeSetting(setting: ThemeSetting) {
        context.dataStore.edit { it[Keys.THEME_MODE] = setting.name }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setRecoveryFocusId(focusId: String) {
        context.dataStore.edit { it[Keys.RECOVERY_FOCUS_ID] = focusId }
    }

    suspend fun setReadingGoalMinimum(goal: Int) {
        context.dataStore.edit { it[Keys.READING_GOAL_MIN] = goal.coerceIn(1, 5) }
    }

    suspend fun setOtCurrentReading(bookId: Int?, chapter: Int?) {
        context.dataStore.edit {
            if (bookId != null && chapter != null) {
                it[Keys.OT_CURRENT_BOOK_ID] = bookId
                it[Keys.OT_CURRENT_CHAPTER] = chapter
            } else {
                it.remove(Keys.OT_CURRENT_BOOK_ID)
                it.remove(Keys.OT_CURRENT_CHAPTER)
            }
        }
    }

    suspend fun setNtCurrentReading(bookId: Int?, chapter: Int?) {
        context.dataStore.edit {
            if (bookId != null && chapter != null) {
                it[Keys.NT_CURRENT_BOOK_ID] = bookId
                it[Keys.NT_CURRENT_CHAPTER] = chapter
            } else {
                it.remove(Keys.NT_CURRENT_BOOK_ID)
                it.remove(Keys.NT_CURRENT_CHAPTER)
            }
        }
    }

    suspend fun markMilestoneShown(milestoneId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SHOWN_MILESTONES].orEmpty().split(",").filter { it.isNotBlank() }.toMutableSet()
            current.add(milestoneId)
            prefs[Keys.SHOWN_MILESTONES] = current.joinToString(",")
        }
    }

    suspend fun clearShownMilestones() {
        context.dataStore.edit { it.remove(Keys.SHOWN_MILESTONES) }
    }

    suspend fun setLastBackgroundEpoch(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKGROUND_EPOCH] = millis }
    }

    suspend fun getLastBackgroundEpoch(): Long {
        var last = 0L
        context.dataStore.edit {
            last = it[Keys.LAST_BACKGROUND_EPOCH] ?: 0L
        }
        return last
    }
}
