package com.steadfast.app.data.seed

import android.content.Context
import com.steadfast.app.data.model.BibleBook
import com.steadfast.app.data.model.BibleBooksData
import com.steadfast.app.data.model.MilestoneDefinition
import com.steadfast.app.data.model.MilestonesData
import com.steadfast.app.data.model.RecoveryFocus
import com.steadfast.app.data.model.RecoveryFocusesData
import com.steadfast.app.data.model.ScaleComparison
import com.steadfast.app.data.model.VerseReferenceCatalog
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class AssetSeedLoader(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun loadBibleBooks(): List<BibleBook> {
        return try {
            val json = context.assets.open("bible_books.json").bufferedReader().use { it.readText() }
            val adapter = moshi.adapter(BibleBooksData::class.java)
            adapter.fromJson(json)?.books ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadRecoveryFocuses(): List<RecoveryFocus> {
        return try {
            val json = context.assets.open("recovery_focuses.json").bufferedReader().use { it.readText() }
            val adapter = moshi.adapter(RecoveryFocusesData::class.java)
            adapter.fromJson(json)?.items ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadMilestones(): List<MilestoneDefinition> {
        return try {
            val json = context.assets.open("milestones.json").bufferedReader().use { it.readText() }
            val adapter = moshi.adapter(MilestonesData::class.java)
            adapter.fromJson(json)?.milestones ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadVerseCatalog(): VerseReferenceCatalog {
        return try {
            val json = context.assets.open("verse_reference_catalog_starter.json").bufferedReader().use { it.readText() }
            val adapter = moshi.adapter(VerseReferenceCatalog::class.java)
            adapter.fromJson(json) ?: VerseReferenceCatalog(null, "NLT", emptyMap())
        } catch (_: Exception) {
            VerseReferenceCatalog(null, "NLT", emptyMap())
        }
    }

    fun getScaleComparisons(): List<ScaleComparison> {
        return try {
            val json = context.assets.open("scale_comparisons.json").bufferedReader().use { it.readText() }
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ScaleComparison::class.java)
            val adapter = moshi.adapter<List<ScaleComparison>>(listType)
            adapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
