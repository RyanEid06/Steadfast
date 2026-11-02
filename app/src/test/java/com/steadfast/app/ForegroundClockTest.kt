package com.steadfast.app

import android.os.Looper
import android.content.Context
import androidx.room.Room
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.core.time.nextStreakChangeEpochMillis
import com.steadfast.app.core.time.rememberForegroundBoundaryTimeMillis
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.data.repository.JournalRepository
import com.steadfast.app.data.repository.MilestoneRepository
import com.steadfast.app.data.repository.SteadfastRepository
import com.steadfast.app.data.seed.AssetSeedLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows
import java.time.Instant
import java.time.ZonedDateTime
import java.time.Clock
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong
import java.time.Duration
import java.io.File

class ForegroundClockTest {
    @Test fun nextChangeTracksExactElapsedDayBoundary() {
        val start = Instant.parse("2024-01-01T12:00:00Z").toEpochMilli()
        val now = Instant.parse("2024-01-02T11:59:59.999Z").toEpochMilli()

        assertEquals(
            Instant.parse("2024-01-02T12:00:00Z").toEpochMilli(),
            nextStreakChangeEpochMillis(start, "UTC", now)
        )
    }

    @Test fun nextChangeUsesSessionCalendarAcrossDaylightSavingTransition() {
        val zone = "America/New_York"
        val start = Instant.parse("2024-03-09T17:00:00Z").toEpochMilli() // noon before DST
        val now = Instant.parse("2024-03-10T15:00:00Z").toEpochMilli() // 11 AM after DST
        val nextCalendarDay = ZonedDateTime.of(2024, 3, 10, 12, 0, 0, 0, java.time.ZoneId.of(zone))
            .toInstant().toEpochMilli()

        assertEquals(
            nextCalendarDay,
            nextStreakChangeEpochMillis(start, zone, now)
        )
        assertTrue(nextCalendarDay < start + 86_400_000L)
    }

    @Test fun resumeAfterDayBoundarySchedulesTheFollowingBoundary() {
        val start = Instant.parse("2024-01-01T12:00:00Z").toEpochMilli()
        val resumedAt = Instant.parse("2024-01-03T12:00:01Z").toEpochMilli()

        assertEquals(
            Instant.parse("2024-01-04T12:00:00Z").toEpochMilli(),
            nextStreakChangeEpochMillis(start, "UTC", resumedAt)
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ForegroundBoundaryClockLifecycleTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var storeJob: kotlinx.coroutines.CompletableJob
    private lateinit var storeFile: File
    private lateinit var preferences: SteadfastPreferences
    private lateinit var database: SteadfastDatabase

    @Before fun createIsolatedPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        storeFile = File(context.filesDir, "foreground-boundary-test.preferences_pb")
        storeFile.delete()
        storeJob = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(storeJob + Dispatchers.IO),
            produceFile = { storeFile }
        )
        preferences = SteadfastPreferences(context, store)
    }

    @After fun closeIsolatedPreferences() = runBlocking {
        storeJob.cancelAndJoin()
        storeFile.delete()
        if (::database.isInitialized) database.close()
    }

    @Test fun boundaryClockDoesNotTickEverySecondAndRefreshesOnResume() {
        lateinit var owner: TestOwner
        compose.runOnIdle { owner = TestOwner() }
        val observed = AtomicLong(0L)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val now = rememberForegroundBoundaryTimeMillis(
                    startEpochMillis = System.currentTimeMillis() - 3_600_000L,
                    startZoneId = "UTC"
                )
                SideEffect { observed.set(now) }
            }
        }
        compose.waitForIdle()
        val initial = observed.get()
        assertTrue("clock emitted its initial resumed time", initial > 0L)

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2_200L))
        compose.runOnIdle { assertEquals("no per-second updates between thresholds", initial, observed.get()) }

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_100L))
        compose.runOnIdle { assertEquals("background clock remains stopped", initial, observed.get()) }

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1L))
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertNotEquals("resume refreshes from current wall time", initial, observed.get())
    }

    @Test fun dayBoundaryUpdatesMilestonesAndLongestStreakWithoutRoomEmission() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java)
            .allowMainThreadQueries().build()
        val steadfastRepository = SteadfastRepository(database, JournalRepository(database.journalDao()), preferences)
        val day = 86_400_000L
        val start = Instant.parse("2024-01-01T12:00:00Z").toEpochMilli()
        val clock = MutableClock(start + day - 1L)
        val observed = AtomicLong(0L)
        val activeStart = mutableLongStateOf(start)
        lateinit var owner: TestOwner
        compose.runOnIdle { owner = TestOwner() }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val now = rememberForegroundBoundaryTimeMillis(
                    startEpochMillis = activeStart.longValue,
                    startZoneId = "UTC",
                    clock = clock
                )
                SideEffect { observed.set(now) }
            }
        }
        compose.waitForIdle()

        val milestoneRepository = MilestoneRepository(AssetSeedLoader(context), preferences)
        val archivedStart = start - 10L * day
        val sessions = listOf(
            StreakSessionEntity(recoveryFocusId = "pornography", startEpochMillis = archivedStart,
                startZoneId = "UTC", endEpochMillis = archivedStart + 5L * day),
            StreakSessionEntity(recoveryFocusId = "pornography", startEpochMillis = start, startZoneId = "UTC")
        )
        fun activeDuration() = StreakDurationCalculator.calculate(start, "UTC", observed.get())

        assertEquals(start + day - 1L, observed.get())
        assertEquals(5L, steadfastRepository.calculateLongestStreakDays(sessions, observed.get()))
        assertNull(milestoneRepository.getPendingCelebration(activeDuration()))

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        clock.set(start + day)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals(start + day, observed.get())
        assertEquals(1L, activeDuration().totalDays)
        assertEquals("d1", milestoneRepository.getPendingCelebration(activeDuration())?.id)
        assertEquals("the longer archived row remains the maximum", 5L,
            steadfastRepository.calculateLongestStreakDays(sessions, observed.get()))

        milestoneRepository.markCelebrated("d1")
        assertNull(milestoneRepository.getPendingCelebration(activeDuration()))

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        clock.set(start + 3L * day)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals(start + 3L * day, observed.get())
        assertEquals("highest achieved unshown milestone", "d3",
            milestoneRepository.getPendingCelebration(activeDuration())?.id)
        assertEquals(5L, steadfastRepository.calculateLongestStreakDays(sessions, observed.get()))

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        clock.set(start + 6L * day)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals("resume refreshes the active session past the archived maximum", start + 6L * day, observed.get())
        assertEquals("longest streak advances from the active row without a Room emission", 6L,
            steadfastRepository.calculateLongestStreakDays(sessions, observed.get()))
        assertEquals("already eligible milestones remain highest-unshown", "d3",
            milestoneRepository.getPendingCelebration(activeDuration())?.id)

        // A restart clears the prior session's shown set and starts a fresh boundary clock.
        preferences.clearShownMilestones()
        val restartedAt = observed.get()
        compose.runOnIdle { activeStart.longValue = restartedAt }
        compose.waitForIdle()
        assertEquals(0L, StreakDurationCalculator.calculate(restartedAt, "UTC", observed.get()).totalDays)
        assertNull(milestoneRepository.getPendingCelebration(
            StreakDurationCalculator.calculate(restartedAt, "UTC", observed.get())
        ))

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        clock.set(restartedAt + day)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals(restartedAt + day, observed.get())
        assertEquals("the new session can celebrate its own first milestone", "d1",
            milestoneRepository.getPendingCelebration(
                StreakDurationCalculator.calculate(restartedAt, "UTC", observed.get())
            )?.id)
    }

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }

    private class MutableClock(initialMillis: Long) : Clock() {
        private val currentMillis = AtomicLong(initialMillis)
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(currentMillis.get())
        override fun millis(): Long = currentMillis.get()
        fun set(epochMillis: Long) { currentMillis.set(epochMillis) }
    }
}
