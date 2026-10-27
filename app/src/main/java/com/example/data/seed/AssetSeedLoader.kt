package com.example.data.seed

import android.content.Context
import com.example.data.model.BibleBook
import com.example.data.model.BibleBooksData
import com.example.data.model.MilestoneDefinition
import com.example.data.model.MilestonesData
import com.example.data.model.RecoveryFocus
import com.example.data.model.RecoveryFocusesData
import com.example.data.model.ScaleComparison
import com.example.data.model.VerseReferenceCatalog
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
        return listOf(
            ScaleComparison(
                id = "sc_1",
                thresholdDays = 1,
                title = "The First Step of Faith",
                description = "You have completed 24 hours of intentional surrender. The journey of a thousand leagues begins beneath your feet.",
                scriptureReference = "Zechariah 4:10 — 'Do not despise these small beginnings, for the Lord rejoices to see the work begin.'"
            ),
            ScaleComparison(
                id = "sc_3",
                thresholdDays = 3,
                title = "The Resurrected Morning",
                description = "Three days: the span Christ rested in the tomb before rising in victory. You are stepping into new life.",
                scriptureReference = "Hosea 6:2 — 'After two days he will revive us; on the third day he will restore us, that we may live in his presence.'"
            ),
            ScaleComparison(
                id = "sc_7",
                thresholdDays = 7,
                title = "A Sabbath of Freedom",
                description = "168 unbroken hours. God created the heavens and the earth and rested on the seventh day. A full week consecrated to freedom.",
                scriptureReference = "Genesis 2:3 — 'And God blessed the seventh day and declared it holy, because on it he rested from all his work.'"
            ),
            ScaleComparison(
                id = "sc_14",
                thresholdDays = 14,
                title = "Fortified Resolve",
                description = "Two complete cycles of weeks. Your mind and neural pathways are clearing as grace replaces compulsive impulses.",
                scriptureReference = "Psalm 31:24 — 'Be strong, and let your heart take courage, all you who wait for the Lord!'"
            ),
            ScaleComparison(
                id = "sc_21",
                thresholdDays = 21,
                title = "The Daniel Fast",
                description = "Three weeks of steady spiritual devotion, mirroring Daniel's faithful fasting before the angel brought God's message.",
                scriptureReference = "Daniel 10:2-3 — 'At that time I, Daniel, mourned for three weeks... until the three full weeks were over.'"
            ),
            ScaleComparison(
                id = "sc_30",
                thresholdDays = 30,
                title = "A Full Moon Cycle",
                description = "One complete month. Your timer now elevates into Months, Days, Hours, and Seconds.",
                scriptureReference = "Lamentations 3:22-23 — 'The faithful love of the Lord never ends! His mercies never cease. Great is his faithfulness; his mercies begin afresh each morning.'"
            ),
            ScaleComparison(
                id = "sc_60",
                thresholdDays = 60,
                title = "Deep Rooted Strength",
                description = "Two months of perseverance. Your roots are sinking deep beside streams of living water.",
                scriptureReference = "Jeremiah 17:8 — 'They are like trees planted along a riverbank, with roots that reach deep into the water.'"
            ),
            ScaleComparison(
                id = "sc_90",
                thresholdDays = 90,
                title = "A Season Transformed",
                description = "A quarter of a calendar year. Old seasons pass away; new clarity and spiritual discipline take firm hold.",
                scriptureReference = "2 Corinthians 5:17 — 'This means that anyone who belongs to Christ has become a new person. The old life is gone; a new life has begun!'"
            ),
            ScaleComparison(
                id = "sc_180",
                thresholdDays = 180,
                title = "Half a Solar Year",
                description = "Half a year walking in steadfast grace. You have walked through changing weather and remained unmovable.",
                scriptureReference = "1 Corinthians 15:58 — 'Therefore, my beloved brethren, be steadfast, immovable, always abounding in the work of the Lord.'"
            ),
            ScaleComparison(
                id = "sc_365",
                thresholdDays = 365,
                title = "A Jubilee Year",
                description = "A full calendar year in the light. 365 days of answered prayers, overcome urges, and deepened devotion.",
                scriptureReference = "Isaiah 40:31 — 'Those who trust in the Lord will find new strength. They will soar high on wings like eagles; they will run and not grow weary; they will walk and not faint.'"
            )
        )
    }
}
