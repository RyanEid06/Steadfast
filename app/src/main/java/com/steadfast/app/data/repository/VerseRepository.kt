package com.steadfast.app.data.repository

import com.steadfast.app.data.db.BibleDao
import com.steadfast.app.data.seed.AssetSeedLoader

data class CuratedVerse(
    val reference: String,
    val bookName: String,
    val chapter: Int,
    val verseNumber: String,
    val theme: String,
    val text: String? = null,
    val translation: String? = null,
    val isFromCompletedChapter: Boolean = false
)

private data class VerifiedText(
    val text: String,
    val translation: String
)

class VerseRepository(
    private val seedLoader: AssetSeedLoader,
    private val bibleDao: BibleDao
) {
    private val catalog by lazy { seedLoader.loadVerseCatalog() }
    private val books by lazy { seedLoader.loadBibleBooks() }

    // Verified public-domain (WEB) Scripture texts bound strictly to their exact matching reference.
    // Strictly adheres to STEADFAST_V1_CORRECTION_QA.md: never merge, invent, or mismatch verses.
    private val verifiedTextMap: Map<String, VerifiedText> = mapOf(
        "Philippians 4:13" to VerifiedText(
            text = "I can do all things through Christ, who strengthens me.",
            translation = "WEB"
        ),
        "1 Corinthians 10:13" to VerifiedText(
            text = "No temptation has taken you except what is common to man. God is faithful, who will not allow you to be tempted above what you are able, but will with the temptation also make the way of escape, that you may be able to endure it.",
            translation = "WEB"
        ),
        "James 1:12" to VerifiedText(
            text = "Blessed is the man who endures temptation, for when he has been approved, he will receive the crown of life, which the Lord promised to those who love him.",
            translation = "WEB"
        ),
        "James 4:7" to VerifiedText(
            text = "Be subject therefore to God. Resist the devil, and he will flee from you.",
            translation = "WEB"
        ),
        "Psalm 28:7" to VerifiedText(
            text = "Yahweh is my strength and my shield. My heart has trusted in him, and I am helped. Therefore my heart greatly rejoices. With my song I will thank him.",
            translation = "WEB"
        ),
        "John 14:27" to VerifiedText(
            text = "Peace I leave with you. My peace I give to you; not as the world gives, give I to you. Don't let your heart be troubled, neither let it be fearful.",
            translation = "WEB"
        ),
        "2 Timothy 1:7" to VerifiedText(
            text = "For God didn't give us a spirit of fear, but of power, love, and self-control.",
            translation = "WEB"
        ),
        "Romans 12:2" to VerifiedText(
            text = "Don't be conformed to this world, but be transformed by the renewing of your mind, so that you may prove what is the good, well-pleasing, and perfect will of God.",
            translation = "WEB"
        ),
        "1 Peter 5:7" to VerifiedText(
            text = "Casting all your worries on him, because he cares for you.",
            translation = "WEB"
        ),
        "Psalm 46:1" to VerifiedText(
            text = "God is our refuge and strength, a very present help in trouble.",
            translation = "WEB"
        ),
        "Psalm 119:9" to VerifiedText(
            text = "How can a young man keep his way pure? By living according to your word.",
            translation = "WEB"
        ),
        "Psalm 119:11" to VerifiedText(
            text = "I have hidden your word in my heart, that I might not sin against you.",
            translation = "WEB"
        ),
        "1 John 1:9" to VerifiedText(
            text = "If we confess our sins, he is faithful and righteous to forgive us the sins, and to cleanse us from all unrighteousness.",
            translation = "WEB"
        ),
        "Matthew 11:28" to VerifiedText(
            text = "Come to me, all you who labor and are heavily burdened, and I will give you rest.",
            translation = "WEB"
        ),
        "Ephesians 6:10" to VerifiedText(
            text = "Finally, be strong in the Lord, and in the strength of his might.",
            translation = "WEB"
        ),
        "Galatians 6:9" to VerifiedText(
            text = "Let’s not become weary in doing good; for we will reap in due season, if we don’t give up.",
            translation = "WEB"
        ),
        "John 3:16" to VerifiedText(
            text = "For God so loved the world, that he gave his one and only Son, that whoever believes in him should not perish, but have eternal life.",
            translation = "WEB"
        )
    )

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

        val verified = verifiedTextMap[reference]

        return CuratedVerse(
            reference = reference,
            bookName = bookPart,
            chapter = chapterPart,
            verseNumber = versePart,
            theme = theme,
            text = verified?.text,
            translation = verified?.translation
        )
    }

    fun findBookId(bookName: String): Int? {
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
        val verifiedOnly = allVerses.filter { it.text != null }
        return verifiedOnly.shuffled().firstOrNull() ?: CuratedVerse(
            reference = "Philippians 4:13",
            bookName = "Philippians",
            chapter = 4,
            verseNumber = "13",
            theme = "strength",
            text = "I can do all things through Christ, who strengthens me.",
            translation = "WEB"
        )
    }

    // Exposed for targeted unit testing
    fun getAllCatalogVerses(): List<CuratedVerse> = allVerses
    fun getRecentHistory(): List<String> = recentShownReferences.toList()
    fun clearRecentHistory() = recentShownReferences.clear()
}
