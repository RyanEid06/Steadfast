package com.steadfast.app

import android.content.Context
import android.provider.Settings
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.core.design.ReducedMotionPolicy
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.repository.VerseRepository
import com.steadfast.app.data.seed.AssetSeedLoader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReconciliationUiDataTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun scriptureTextIsBoundToTheExactReference() {
        val db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        try {
            val verses = VerseRepository(AssetSeedLoader(context), db.bibleDao()).getAllCatalogVerses()
            val james = verses.first { it.reference == "James 1:12" }
            assertEquals("WEB", james.translation)
            assertEquals("Blessed is the man who endures temptation, for when he has been approved, he will receive the crown of life, which the Lord promised to those who love him.", james.text)
            val unknown = verses.first { it.reference == "Matthew 26:41" }
            assertNull(unknown.text)
            assertNull(unknown.translation)
        } finally { db.close() }
    }

    @Test fun dailyEncouragementAlwaysHasVerifiedText() {
        val db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repository = VerseRepository(AssetSeedLoader(context), db.bibleDao())
            repeat(100) {
                val verse = repository.getRandomDailyEncouragement()
                assertEquals("WEB", verse.translation)
                assertFalse(verse.text.isNullOrBlank())
            }
        } finally { db.close() }
    }

    @Test fun scaleComparisonsComeFromTheAsset() {
        val comparisons = AssetSeedLoader(context).getScaleComparisons()
        assertEquals("24 Hours of Freedom", comparisons.first().title)
        assertTrue(comparisons.any { it.thresholdDays == 40 })
        assertFalse(comparisons.any { it.description.contains("neural pathways") })
    }

    @Test fun systemAnimationSettingsEnableReducedMotion() {
        ReducedMotionPolicy.testReducedMotionOverride = null
        try {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            assertTrue(ReducedMotionPolicy.isReducedMotionEnabled(context))
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
            assertTrue(ReducedMotionPolicy.isReducedMotionEnabled(context))
        } finally {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        }
    }
    @Test fun widgetUsesActualNextMilestoneAndElapsedDays() = kotlinx.coroutines.runBlocking {
        val db = SteadfastDatabase.getInstance(context)
        db.streakDao().clearAll()
        try {
            val empty = com.steadfast.app.widget.loadWidgetContent(context)
            assertEquals("Begin Walk", empty.streak)
            db.streakDao().insertSession(com.steadfast.app.data.model.StreakSessionEntity(
                recoveryFocusId = "discipline", startEpochMillis = System.currentTimeMillis() - 60_000, startZoneId = "UTC"
            ))
            val fresh = com.steadfast.app.widget.loadWidgetContent(context)
            assertEquals("0 days", fresh.streak)
            assertEquals("Next: 1 Day", fresh.milestone)
            db.streakDao().clearAll()
            db.streakDao().insertSession(com.steadfast.app.data.model.StreakSessionEntity(
                recoveryFocusId = "discipline", startEpochMillis = System.currentTimeMillis() - 8 * 86_400_000L, startZoneId = "UTC"
            ))
            val week = com.steadfast.app.widget.loadWidgetContent(context)
            assertEquals("8 days", week.streak)
            assertEquals("Next: 14 Days", week.milestone)
        } finally { db.streakDao().clearAll() }
    }    @Test fun widgetDoesNotReadPrivateProgressWhileRestoreIsPending() = kotlinx.coroutines.runBlocking {
        val journal = java.io.File(context.noBackupFilesDir, "restore-recovery.enc")
        journal.parentFile!!.mkdirs()
        assertFalse(journal.exists())
        try {
            journal.writeBytes(byteArrayOf(1))
            val content = com.steadfast.app.widget.loadWidgetContent(context)
            assertEquals("Open Steadfast", content.streak)
            assertEquals("Restore in progress", content.focus)
            var readPrivateData = false
            val guarded = com.steadfast.app.core.security.BackupManager.withStableUserData(context) {
                readPrivateData = true
                "private"
            }
            assertNull(guarded)
            assertFalse(readPrivateData)
        } finally { journal.delete() }
    }}
