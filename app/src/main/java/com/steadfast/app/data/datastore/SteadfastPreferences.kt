package com.steadfast.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.steadfast.app.ui.theme.ThemeSetting
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "steadfast_settings")

@JsonClass(generateAdapter = true)
data class ReadingLocation(
    val bookId: Int,
    val chapter: Int
)

@JsonClass(generateAdapter = true)
data class UserPreferencesSnapshot(
    val themeMode: String = "SYSTEM",
    val recoveryFocusId: String = "pornography",
    val readingGoalMinimum: Int = 2,
    val oldTestamentCurrent: ReadingLocation? = null,
    val newTestamentCurrent: ReadingLocation? = null,
    val appLockEnabled: Boolean = false,
    val shownMilestones: Set<String> = emptySet(),
    val onboardingComplete: Boolean = false,
    val bibleTestamentFilter: Int = 0,
    val journeyTab: Int = 0,
    val journalFilter: String = "ALL"
)

class SteadfastPreferences(context: Context, private val store: DataStore<Preferences> = context.dataStore) {

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
        val BIBLE_FILTER = intPreferencesKey("bible_testament_filter")
        val JOURNEY_TAB = intPreferencesKey("journey_tab")
        val JOURNAL_FILTER = stringPreferencesKey("journal_filter")
        val VERSE_DECK_USED = stringPreferencesKey("verse_deck_used")
        val VERSE_DECK_LAST = stringPreferencesKey("verse_deck_last")
    }

    val bibleTestamentFilter: Flow<Int> = store.data.map { (it[Keys.BIBLE_FILTER] ?: 0).coerceIn(0, 2) }
    val journeyTab: Flow<Int> = store.data.map { (it[Keys.JOURNEY_TAB] ?: 0).coerceIn(0, 2) }
    val journalFilter: Flow<String> = store.data.map { it[Keys.JOURNAL_FILTER]?.takeIf(::validJournalFilter) ?: "ALL" }

    suspend fun setBibleTestamentFilter(filter: Int) { require(filter in 0..2); store.edit { it[Keys.BIBLE_FILTER] = filter } }
    suspend fun setJourneyTab(tab: Int) { require(tab in 0..2); store.edit { it[Keys.JOURNEY_TAB] = tab } }
    suspend fun setJournalFilter(filter: String) { require(validJournalFilter(filter)); store.edit { it[Keys.JOURNAL_FILTER] = filter } }
    private fun validJournalFilter(filter: String) = filter in setOf("ALL", "GENERAL", "URGE", "RESTART")

    suspend fun nextVerseId(ids: List<String>, priority: Set<String>): String {
        var selected: String? = null
        // DataStore serializes draws across repositories and commits before exposing the selected card.
        store.edit { prefs ->
            val state = com.steadfast.app.data.repository.VerseDeckState(
                prefs[Keys.VERSE_DECK_USED].orEmpty().split(',').filter { it.isNotBlank() }.toSet(),
                prefs[Keys.VERSE_DECK_LAST]
            )
            val next = com.steadfast.app.data.repository.VerseDeck.draw(ids, priority, state)
            prefs[Keys.VERSE_DECK_USED] = next.usedIds.joinToString(",")
            prefs[Keys.VERSE_DECK_LAST] = requireNotNull(next.lastId)
            selected = next.lastId
        }
        return requireNotNull(selected)
    }

    val isOnboardingComplete: Flow<Boolean> = store.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val themeSetting: Flow<ThemeSetting> = store.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "DARK" -> ThemeSetting.DARK
            "LIGHT" -> ThemeSetting.LIGHT
            else -> ThemeSetting.SYSTEM
        }
    }

    val isAppLockEnabled: Flow<Boolean> = store.data.map { prefs ->
        prefs[Keys.APP_LOCK_ENABLED] ?: false
    }

    val recoveryFocusId: Flow<String> = store.data.map { prefs ->
        prefs[Keys.RECOVERY_FOCUS_ID] ?: "pornography"
    }

    val readingGoalMinimum: Flow<Int> = store.data.map { prefs ->
        prefs[Keys.READING_GOAL_MIN] ?: 2
    }

    val otCurrentReading: Flow<ReadingLocation?> = store.data.map { prefs ->
        val bookId = prefs[Keys.OT_CURRENT_BOOK_ID]
        val chapter = prefs[Keys.OT_CURRENT_CHAPTER]
        if (bookId != null && chapter != null) ReadingLocation(bookId, chapter) else null
    }

    val ntCurrentReading: Flow<ReadingLocation?> = store.data.map { prefs ->
        val bookId = prefs[Keys.NT_CURRENT_BOOK_ID]
        val chapter = prefs[Keys.NT_CURRENT_CHAPTER]
        if (bookId != null && chapter != null) ReadingLocation(bookId, chapter) else null
    }

    val shownMilestones: Flow<Set<String>> = store.data.map { prefs ->
        val raw = prefs[Keys.SHOWN_MILESTONES].orEmpty()
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    suspend fun snapshotUserSettings(): UserPreferencesSnapshot {
        val prefs = store.data.first()
        return UserPreferencesSnapshot(
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            recoveryFocusId = prefs[Keys.RECOVERY_FOCUS_ID] ?: "pornography",
            readingGoalMinimum = prefs[Keys.READING_GOAL_MIN] ?: 2,
            oldTestamentCurrent = prefs[Keys.OT_CURRENT_BOOK_ID]?.let { book ->
                prefs[Keys.OT_CURRENT_CHAPTER]?.let { ReadingLocation(book, it) }
            },
            newTestamentCurrent = prefs[Keys.NT_CURRENT_BOOK_ID]?.let { book ->
                prefs[Keys.NT_CURRENT_CHAPTER]?.let { ReadingLocation(book, it) }
            },
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            shownMilestones = prefs[Keys.SHOWN_MILESTONES].orEmpty().split(",").filter { it.isNotBlank() }.toSet(),
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            bibleTestamentFilter = (prefs[Keys.BIBLE_FILTER] ?: 0).coerceIn(0, 2),
            journeyTab = (prefs[Keys.JOURNEY_TAB] ?: 0).coerceIn(0, 2),
            journalFilter = prefs[Keys.JOURNAL_FILTER]?.takeIf(::validJournalFilter) ?: "ALL"
        )
    }

    /** One DataStore commit: observers never see partially restored settings. */
    suspend fun replaceUserSettings(snapshot: UserPreferencesSnapshot) {
        store.edit { prefs ->
            prefs[Keys.THEME_MODE] = snapshot.themeMode
            prefs[Keys.RECOVERY_FOCUS_ID] = snapshot.recoveryFocusId
            prefs[Keys.READING_GOAL_MIN] = snapshot.readingGoalMinimum
            prefs[Keys.APP_LOCK_ENABLED] = snapshot.appLockEnabled
            prefs[Keys.ONBOARDING_COMPLETE] = snapshot.onboardingComplete
            prefs[Keys.SHOWN_MILESTONES] = snapshot.shownMilestones.joinToString(",")
            prefs[Keys.BIBLE_FILTER] = snapshot.bibleTestamentFilter
            prefs[Keys.JOURNEY_TAB] = snapshot.journeyTab
            prefs[Keys.JOURNAL_FILTER] = snapshot.journalFilter
            if (snapshot.oldTestamentCurrent == null) {
                prefs.remove(Keys.OT_CURRENT_BOOK_ID)
                prefs.remove(Keys.OT_CURRENT_CHAPTER)
            } else {
                prefs[Keys.OT_CURRENT_BOOK_ID] = snapshot.oldTestamentCurrent.bookId
                prefs[Keys.OT_CURRENT_CHAPTER] = snapshot.oldTestamentCurrent.chapter
            }
            if (snapshot.newTestamentCurrent == null) {
                prefs.remove(Keys.NT_CURRENT_BOOK_ID)
                prefs.remove(Keys.NT_CURRENT_CHAPTER)
            } else {
                prefs[Keys.NT_CURRENT_BOOK_ID] = snapshot.newTestamentCurrent.bookId
                prefs[Keys.NT_CURRENT_CHAPTER] = snapshot.newTestamentCurrent.chapter
            }
        }
    }

    /** Persist validated onboarding choices while leaving recovery incomplete until Room succeeds. */
    suspend fun stageOnboardingSettings(snapshot: UserPreferencesSnapshot) {
        val existing = snapshotUserSettings()
        replaceUserSettings(snapshot.copy(
            oldTestamentCurrent = snapshot.oldTestamentCurrent ?: existing.oldTestamentCurrent,
            newTestamentCurrent = snapshot.newTestamentCurrent ?: existing.newTestamentCurrent,
            shownMilestones = snapshot.shownMilestones.ifEmpty { existing.shownMilestones },
            bibleTestamentFilter = existing.bibleTestamentFilter,
            journeyTab = existing.journeyTab,
            journalFilter = existing.journalFilter,
            onboardingComplete = false
        ))
    }

    /** Keep the duplicated focus and readiness fields consistent in one DataStore commit. */
    suspend fun reconcileSessionSetup(focusId: String, complete: Boolean) {
        store.edit {
            it[Keys.RECOVERY_FOCUS_ID] = focusId
            it[Keys.ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        store.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setThemeSetting(setting: ThemeSetting) {
        store.edit { it[Keys.THEME_MODE] = setting.name }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        store.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setRecoveryFocusId(focusId: String) {
        store.edit { it[Keys.RECOVERY_FOCUS_ID] = focusId }
    }

    suspend fun setReadingGoalMinimum(goal: Int) {
        store.edit { it[Keys.READING_GOAL_MIN] = goal.coerceIn(1, 5) }
    }

    suspend fun setOtCurrentReading(bookId: Int?, chapter: Int?) {
        store.edit {
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
        store.edit {
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
        store.edit { prefs ->
            val current = prefs[Keys.SHOWN_MILESTONES].orEmpty().split(",").filter { it.isNotBlank() }.toMutableSet()
            current.add(milestoneId)
            prefs[Keys.SHOWN_MILESTONES] = current.joinToString(",")
        }
    }

    suspend fun clearShownMilestones() {
        store.edit { it.remove(Keys.SHOWN_MILESTONES) }
    }

    suspend fun setLastBackgroundEpoch(millis: Long) {
        store.edit { it[Keys.LAST_BACKGROUND_EPOCH] = millis }
    }

    suspend fun getLastBackgroundEpoch(): Long {
        return store.data.map { it[Keys.LAST_BACKGROUND_EPOCH] ?: 0L }.firstOrNull() ?: 0L
    }
}
