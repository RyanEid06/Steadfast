package com.steadfast.app.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BibleBook(
    val id: Int,
    val name: String,
    val testament: String, // "OT" or "NT"
    val chapters: Int,
    val order: Int
)

@JsonClass(generateAdapter = true)
data class BibleBooksData(
    val canon: String,
    val translation_preference: String?,
    val totalBooks: Int,
    val totalChapters: Int,
    val oldTestamentChapters: Int,
    val newTestamentChapters: Int,
    val books: List<BibleBook>
)

@JsonClass(generateAdapter = true)
data class RecoveryFocus(
    val id: String,
    val label: String
)

@JsonClass(generateAdapter = true)
data class RecoveryFocusesData(
    val label: String,
    val items: List<RecoveryFocus>
)

@JsonClass(generateAdapter = true)
data class MilestoneDefinition(
    val id: String,
    val kind: String, // "elapsed_days", "calendar_months", "calendar_years"
    val value: Int,
    val title: String
)

@JsonClass(generateAdapter = true)
data class MilestonesData(
    val milestones: List<MilestoneDefinition>
)

@JsonClass(generateAdapter = true)
data class VerseReferenceCatalog(
    val note: String?,
    val preferredTranslation: String?,
    val themes: Map<String, List<String>>
)

@JsonClass(generateAdapter = true)
data class ScriptureEntry(
    val id: String,
    val bookId: Int,
    val book: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val reference: String,
    val text: String,
    val testament: String,
    val tags: List<String>
)

@JsonClass(generateAdapter = true)
data class ScaleComparison(
    val id: String,
    val thresholdDays: Int,
    val title: String,
    val description: String,
    val scriptureReference: String,
    val sourceNote: String? = null
)
