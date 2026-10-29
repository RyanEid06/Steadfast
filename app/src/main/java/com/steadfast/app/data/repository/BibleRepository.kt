package com.steadfast.app.data.repository

import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.BibleDao
import com.steadfast.app.data.model.BibleBook
import com.steadfast.app.data.model.BibleChapterCompletionEntity
import com.steadfast.app.data.seed.AssetSeedLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class BibleProgress(
    val wholeCompletedChapters: Int,
    val wholeTotalChapters: Int = 1189,
    val wholePercentage: Float,
    val otCompletedChapters: Int,
    val otTotalChapters: Int = 929,
    val otPercentage: Float,
    val ntCompletedChapters: Int,
    val ntTotalChapters: Int = 260,
    val ntPercentage: Float,
    val completedBooksCount: Int,
    val totalBooksCount: Int = 66
)

data class BookWithProgress(
    val book: BibleBook,
    val completedChapters: Set<Int>,
    val completedCount: Int,
    val percentage: Float,
    val isFullyCompleted: Boolean
)

class BibleRepository(
    private val bibleDao: BibleDao,
    private val seedLoader: AssetSeedLoader,
    private val preferences: SteadfastPreferences
) {
    val books: List<BibleBook> by lazy {
        seedLoader.loadBibleBooks().sortedBy { it.order }
    }

    private val booksById: Map<Int, BibleBook> by lazy {
        books.associateBy { it.id }
    }

    val completedChapters: Flow<List<BibleChapterCompletionEntity>> = bibleDao.getAllCompleted()

    val progress: Flow<BibleProgress> = completedChapters.map { list ->
        val completedSet = list.map { it.bookId to it.chapter }.toSet()
        var otCount = 0
        var ntCount = 0
        var fullyCompletedBooks = 0

        for (book in books) {
            var bookCompleted = 0
            for (ch in 1..book.chapters) {
                if (completedSet.contains(book.id to ch)) {
                    bookCompleted++
                    if (book.testament == "OT") otCount++ else ntCount++
                }
            }
            if (bookCompleted == book.chapters) {
                fullyCompletedBooks++
            }
        }

        val total = otCount + ntCount
        BibleProgress(
            wholeCompletedChapters = total,
            wholeTotalChapters = 1189,
            wholePercentage = if (1189 > 0) (total.toFloat() / 1189f).coerceIn(0f, 1f) else 0f,
            otCompletedChapters = otCount,
            otTotalChapters = 929,
            otPercentage = if (929 > 0) (otCount.toFloat() / 929f).coerceIn(0f, 1f) else 0f,
            ntCompletedChapters = ntCount,
            ntTotalChapters = 260,
            ntPercentage = if (260 > 0) (ntCount.toFloat() / 260f).coerceIn(0f, 1f) else 0f,
            completedBooksCount = fullyCompletedBooks,
            totalBooksCount = 66
        )
    }

    val booksWithProgress: Flow<List<BookWithProgress>> = completedChapters.map { list ->
        val group = list.groupBy({ it.bookId }, { it.chapter })
        books.map { book ->
            val set = group[book.id]?.toSet() ?: emptySet()
            val completed = set.size
            val pct = if (book.chapters > 0) (completed.toFloat() / book.chapters.toFloat()).coerceIn(0f, 1f) else 0f
            BookWithProgress(
                book = book,
                completedChapters = set,
                completedCount = completed,
                percentage = pct,
                isFullyCompleted = completed == book.chapters
            )
        }
    }

    fun getBookWithProgress(bookId: Int): Flow<BookWithProgress?> {
        return bibleDao.getCompletedChaptersForBook(bookId).map { chaptersList ->
            val book = booksById[bookId] ?: return@map null
            val set = chaptersList.toSet()
            val completed = set.size
            val pct = if (book.chapters > 0) (completed.toFloat() / book.chapters.toFloat()).coerceIn(0f, 1f) else 0f
            BookWithProgress(
                book = book,
                completedChapters = set,
                completedCount = completed,
                percentage = pct,
                isFullyCompleted = completed == book.chapters
            )
        }
    }

    fun getBook(bookId: Int): BibleBook? = booksById[bookId]

    val otCurrentReading: Flow<ReadingLocation?> = preferences.otCurrentReading
    val ntCurrentReading: Flow<ReadingLocation?> = preferences.ntCurrentReading
    val readingGoalMinimum: Flow<Int> = preferences.readingGoalMinimum

    suspend fun toggleChapter(bookId: Int, chapter: Int) {
        validateChapter(bookId, chapter)
        val isCompleted = bibleDao.isChapterCompletedSync(bookId, chapter)
        if (isCompleted) {
            bibleDao.deleteCompletion(bookId, chapter)
        } else {
            bibleDao.insertCompletion(BibleChapterCompletionEntity(bookId, chapter))
        }
    }

    suspend fun setChapterCompleted(bookId: Int, chapter: Int, completed: Boolean) {
        validateChapter(bookId, chapter)
        if (completed) {
            bibleDao.insertCompletion(BibleChapterCompletionEntity(bookId, chapter))
        } else {
            bibleDao.deleteCompletion(bookId, chapter)
        }
    }

    suspend fun setOtCurrentReading(bookId: Int?, chapter: Int?) {
        validateReadingLocation(bookId, chapter, "OT")
        preferences.setOtCurrentReading(bookId, chapter)
    }

    suspend fun setNtCurrentReading(bookId: Int?, chapter: Int?) {
        validateReadingLocation(bookId, chapter, "NT")
        preferences.setNtCurrentReading(bookId, chapter)
    }

    suspend fun setReadingGoal(goal: Int) {
        require(goal in 1..5) { "Reading goal must be between 1 and 5 chapters." }
        preferences.setReadingGoalMinimum(goal)
    }

    private fun validateChapter(bookId: Int, chapter: Int) {
        val book = booksById[bookId]
        require(book != null && chapter in 1..book.chapters) { "Choose a valid Bible book and chapter." }
    }

    private fun validateReadingLocation(bookId: Int?, chapter: Int?, testament: String) {
        if (bookId == null && chapter == null) return
        require(bookId != null && chapter != null) { "A reading bookmark needs both book and chapter." }
        validateChapter(bookId, chapter)
        require(booksById[bookId]?.testament == testament) { "Choose a book from the correct Testament." }
    }
}
