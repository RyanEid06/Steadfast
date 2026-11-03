package com.steadfast.app.data.repository

import com.steadfast.app.data.db.BibleDao
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.model.ScriptureEntry
import com.steadfast.app.data.seed.AssetSeedLoader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CuratedVerse(
    val reference: String,
    val bookName: String,
    val chapter: Int,
    val verseNumber: String,
    val theme: String,
    val isFromCompletedChapter: Boolean = false,
    val text: String? = null,
    val translation: String? = null
)

class VerseRepository(
    private val seedLoader: AssetSeedLoader,
    private val bibleDao: BibleDao,
    private val preferences: SteadfastPreferences = SteadfastPreferences(seedLoader.applicationContext)
) {
    private val entries by lazy { seedLoader.loadScriptureCatalog() }
    private val mutex = Mutex()
    private val recent = mutableListOf<String>()

    private fun ScriptureEntry.toVerse(completed: Boolean = false) = CuratedVerse(
        reference, book, chapter,
        if (verseStart == verseEnd) "$verseStart" else "$verseStart-$verseEnd",
        tags.joinToString(", "), completed, text, "WEB"
    )

    fun getAllCatalogVerses(): List<CuratedVerse> = entries.map { it.toVerse() }
    fun getRecentHistory(): List<String> = recent.toList()

    suspend fun getNextVerse(): CuratedVerse = mutex.withLock {
        val completed = bibleDao.getAllCompletedSync().map { it.bookId to it.chapter }.toSet()
        val priority = entries.filter { (it.bookId to it.chapter) in completed }.map { it.id }.toSet()
        val selectedId = preferences.nextVerseId(entries.map { it.id }, priority)
        val entry = entries.first { it.id == selectedId }
        recent.add(entry.reference)
        if (recent.size > entries.size) recent.removeAt(0)
        entry.toVerse(entry.id in priority)
    }

    // The fixed Dashboard reserves space for a short encouragement; the sanctuary uses all 500.
    fun getRandomDailyEncouragement(): CuratedVerse = entries.filter { it.text.length <= 220 }.random().toVerse()
}
