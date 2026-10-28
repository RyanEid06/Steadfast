package com.example.data.repository

import com.example.data.db.BibleDao
import com.example.data.seed.AssetSeedLoader

data class CuratedVerse(
    val reference: String,
    val bookName: String,
    val chapter: Int,
    val verseNumber: String,
    val theme: String,
    val isFromCompletedChapter: Boolean = false
)

class VerseRepository(
    private val seedLoader: AssetSeedLoader,
    private val bibleDao: BibleDao
) {
    private val catalog by lazy { seedLoader.loadVerseCatalog() }
    private val books by lazy { seedLoader.loadBibleBooks() }

    // Map common book names to BibleBook
    private val bookNameToIdMap: Map<String, Int> by lazy {
        val map = mutableMapOf<String, Int>()
        for (b in books) {
            map[b.name.lowercase()] = b.id
            if (b.name == "Psalms") map["psalm"] = b.id
            if (b.name == "Song of Solomon") map["song of songs"] = b.id
        }
        map
    }

    private val allVerses: List<CuratedVerse> by lazy {
        val list = mutableListOf<CuratedVerse>()
        catalog.themes.forEach { (theme, refs) ->
            refs.forEach { ref ->
                val parsed = parseReference(ref, theme)
                if (parsed != null) {
                    list.add(parsed)
                }
            }
        }
        list
    }

    private val recentShownReferences = mutableListOf<String>()
    private val maxRecentHistory = 15

    private fun parseReference(reference: String, theme: String): CuratedVerse? {
        val colonIdx = reference.lastIndexOf(':')
        if (colonIdx == -1) return null
        val versePart = reference.substring(colonIdx + 1).trim()
        val beforeColon = reference.substring(0, colonIdx).trim()
        val spaceIdx = beforeColon.lastIndexOf(' ')
        if (spaceIdx == -1) return null

        val bookPart = beforeColon.substring(0, spaceIdx).trim()
        val chapterPart = beforeColon.substring(spaceIdx + 1).trim().toIntOrNull() ?: 1

        return CuratedVerse(
            reference = reference,
            bookName = bookPart,
            chapter = chapterPart,
            verseNumber = versePart,
            theme = theme
        )
    }

    private fun findBookId(bookName: String): Int? {
        return bookNameToIdMap[bookName.lowercase()]
    }

    suspend fun getNextVerse(): CuratedVerse {
        val completedList = bibleDao.getAllCompletedSync()
        val completedSet = completedList.map { it.bookId to it.chapter }.toSet()

        // 1. Check for verses belonging to user's completed chapters
        val completedChapterVerses = allVerses.filter { verse ->
            val bId = findBookId(verse.bookName)
            bId != null && completedSet.contains(bId to verse.chapter)
        }.map { it.copy(isFromCompletedChapter = true) }

        // Filter out recently shown
        val priorityPool = completedChapterVerses.filter { !recentShownReferences.contains(it.reference) }
        val eligiblePriority = if (priorityPool.isNotEmpty()) priorityPool else completedChapterVerses

        val selected = if (eligiblePriority.isNotEmpty()) {
            eligiblePriority.shuffled().first()
        } else {
            // Fall back to global catalog with anti-repeat
            val globalPool = allVerses.filter { !recentShownReferences.contains(it.reference) }
            val fallback = if (globalPool.isNotEmpty()) globalPool else allVerses
            fallback.shuffled().first()
        }

        // Record anti-repeat
        recentShownReferences.add(selected.reference)
        if (recentShownReferences.size > maxRecentHistory) {
            recentShownReferences.removeAt(0)
        }

        return selected
    }

    fun getRandomDailyEncouragement(): CuratedVerse {
        return allVerses.shuffled().firstOrNull() ?: CuratedVerse(
            reference = "Philippians 4:13",
            bookName = "Philippians",
            chapter = 4,
            verseNumber = "13",
            theme = "strength"
        )
    }
}
