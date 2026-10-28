package com.example.data.model

data class BibleBook(
    val id: Int,
    val name: String,
    val testament: String, // "OT" or "NT"
    val chapters: Int,
    val order: Int
)

data class BibleBooksData(
    val canon: String,
    val translation_preference: String?,
    val totalBooks: Int,
    val totalChapters: Int,
    val oldTestamentChapters: Int,
    val newTestamentChapters: Int,
    val books: List<BibleBook>
)

data class RecoveryFocus(
    val id: String,
    val label: String
)

data class RecoveryFocusesData(
    val label: String,
    val items: List<RecoveryFocus>
)

data class MilestoneDefinition(
    val id: String,
    val kind: String, // "elapsed_days", "calendar_months", "calendar_years"
    val value: Int,
    val title: String
)

data class MilestonesData(
    val milestones: List<MilestoneDefinition>
)

data class VerseReferenceCatalog(
    val note: String?,
    val preferredTranslation: String?,
    val themes: Map<String, List<String>>
)

data class ScaleComparison(
    val id: String,
    val thresholdDays: Int,
    val title: String,
    val description: String,
    val scriptureReference: String
)
