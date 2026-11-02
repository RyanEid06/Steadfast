package com.steadfast.app.data.seed

import android.content.Context
import com.steadfast.app.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class CatalogLoadException(val assetName: String, cause: Exception) :
    IllegalStateException("Bundled catalog unavailable: $assetName", cause)

class AssetSeedLoader(context: Context, private val assets: (String) -> String = { name ->
    context.assets.open(name).bufferedReader().use { it.readText() }
}) {
    private val moshi = Moshi.Builder().build()

    private fun <T> read(name: String, parse: (String) -> T): T = try {
        parse(assets(name))
    } catch (error: Exception) {
        throw CatalogLoadException(name, error)
    }

    private val books by lazy {
        read("bible_books.json") { json ->
            requireNotNull(moshi.adapter(BibleBooksData::class.java).fromJson(json)).books.also { items ->
                require(items.size == 66 && items.map { it.id }.toSet() == (1..66).toSet())
                require(items.map { it.order }.toSet() == (1..66).toSet())
                require(items.all { it.name.isNotBlank() && it.chapters > 0 && it.testament == (if (it.id <= 39) "OT" else "NT") })
                require(items.filter { it.testament == "OT" }.sumOf { it.chapters } == 929)
                require(items.filter { it.testament == "NT" }.sumOf { it.chapters } == 260)
            }
        }
    }
    private val focuses by lazy {
        read("recovery_focuses.json") { json ->
            requireNotNull(moshi.adapter(RecoveryFocusesData::class.java).fromJson(json)).items.also { items ->
                require(items.size == 18 && items.map { it.id }.distinct().size == items.size)
                require(items.all { it.id.isNotBlank() && it.label.isNotBlank() })
            }
        }
    }
    private val milestones by lazy {
        read("milestones.json") { json ->
            requireNotNull(moshi.adapter(MilestonesData::class.java).fromJson(json)).milestones.also { items ->
                require(items.size == 12 && items.map { it.id }.distinct().size == items.size)
                require(items.all { it.id.isNotBlank() && it.title.isNotBlank() && it.value > 0 &&
                    it.kind in setOf("elapsed_days", "calendar_months", "calendar_years") })
            }
        }
    }
    private val verses by lazy {
        read("verse_reference_catalog_starter.json") { json ->
            requireNotNull(moshi.adapter(VerseReferenceCatalog::class.java).fromJson(json)).also { catalog ->
                require(catalog.themes.size == 8)
                require(catalog.themes.all { (theme, refs) -> theme.isNotBlank() && refs.isNotEmpty() && refs.all(::validReference) })
            }
        }
    }
    private val comparisons by lazy {
        read("scale_comparisons.json") { json ->
            val type = Types.newParameterizedType(List::class.java, ScaleComparison::class.java)
            requireNotNull(moshi.adapter<List<ScaleComparison>>(type).fromJson(json)).also { items ->
                require(items.size == 21 && items.map { it.id }.distinct().size == items.size)
                require(items.all { it.id.isNotBlank() && it.title.isNotBlank() && it.description.isNotBlank() &&
                    it.thresholdDays > 0 && it.scriptureReference.split(';').all { ref -> validReference(ref.trim()) } })
            }
        }
    }

    private fun validReference(reference: String): Boolean {
        val match = Regex("(.+) (\\d+):(\\d+)(?:-\\d+)?").matchEntire(reference) ?: return false
        val name = match.groupValues[1].let { if (it == "Psalm") "Psalms" else it }
        val book = books.find { it.name == name } ?: return false
        return (match.groupValues[2].toIntOrNull() ?: 0) in 1..book.chapters &&
            (match.groupValues[3].toIntOrNull() ?: 0) > 0
    }

    // A failed lazy initializer is retried; only validated successful catalogs are shared.
    fun preload() { loadBibleBooks(); loadRecoveryFocuses(); loadMilestones(); loadVerseCatalog(); getScaleComparisons() }
    fun loadBibleBooks(): List<BibleBook> = books
    fun loadRecoveryFocuses(): List<RecoveryFocus> = focuses
    fun loadMilestones(): List<MilestoneDefinition> = milestones
    fun loadVerseCatalog(): VerseReferenceCatalog = verses
    fun getScaleComparisons(): List<ScaleComparison> = comparisons
}
