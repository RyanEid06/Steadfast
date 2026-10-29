package com.steadfast.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.repository.BibleRepository
import com.steadfast.app.data.seed.AssetSeedLoader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BibleStateValidationTest {
    private lateinit var db: SteadfastDatabase
    private lateinit var repo: BibleRepository
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        repo = BibleRepository(db.bibleDao(), AssetSeedLoader(context), SteadfastPreferences(context))
    }
    @After fun close() = db.close()
    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Invalid Bible location must be rejected") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun invalidChapterCannotBeCompleted() = runBlocking {
        rejected { repo.toggleChapter(1, 51) }
        rejected { repo.setChapterCompleted(40, 0, true) }
        rejected { repo.setChapterCompleted(99, 1, true) }
        assertTrue(db.bibleDao().getAllCompletedSync().isEmpty())
    }
    @Test fun readingPointerRequiresValidChapterAndTestament() = runBlocking {
        rejected { repo.setOtCurrentReading(40, 1) }
        rejected { repo.setNtCurrentReading(1, 1) }
        rejected { repo.setOtCurrentReading(1, 51) }
        rejected { repo.setNtCurrentReading(40, null) }
    }
    @Test fun bookmarksAndCompletionRemainIndependent() = runBlocking {
        repo.setOtCurrentReading(1, 12)
        repo.setNtCurrentReading(40, 3)
        repo.toggleChapter(1, 12)
        assertEquals(ReadingLocation(1, 12), repo.otCurrentReading.first())
        assertEquals(ReadingLocation(40, 3), repo.ntCurrentReading.first())
        repo.setOtCurrentReading(1, 15)
        assertTrue(db.bibleDao().isChapterCompletedSync(1, 12))
        repo.setOtCurrentReading(null, null)
        assertNull(repo.otCurrentReading.first())
        assertTrue(db.bibleDao().isChapterCompletedSync(1, 12))
    }
}
