package com.steadfast.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.data.seed.AssetSeedLoader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeedCatalogTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun bundledCatalogsHaveCanonicalCountsAndSentinels() {
        val loader = AssetSeedLoader(context)
        assertEquals(18, loader.loadRecoveryFocuses().size)
        assertTrue(loader.loadRecoveryFocuses().any { it.id == "pornography" })
        val books = loader.loadBibleBooks()
        assertEquals(66, books.size)
        assertEquals(1189, books.sumOf { it.chapters })
        assertEquals("Genesis", books.first().name)
        assertEquals(150, books.single { it.id == 19 }.chapters)
        assertEquals("Revelation", books.last().name)
        assertEquals(12, loader.loadMilestones().size)
        assertEquals("y1", loader.loadMilestones().last().id)
        assertEquals(8, loader.loadVerseCatalog().themes.size)
        assertTrue(loader.loadVerseCatalog().themes.values.flatten().contains("James 1:12"))
        assertEquals(21, loader.getScaleComparisons().size)
        assertEquals("sc_1", loader.getScaleComparisons().first().id)
    }

    @Test fun unavailableAssetsAreAnIdentifiableFailureNeverEmptySuccess() {
        val broken = object : ContextWrapper(context) {
            override fun getAssets(): AssetManager = throw IllegalStateException("Synthetic unavailable assets")
        }
        val loader = AssetSeedLoader(broken)
        assertThrows(IllegalStateException::class.java) { loader.loadRecoveryFocuses() }
        assertThrows(IllegalStateException::class.java) { loader.loadBibleBooks() }
        assertThrows(IllegalStateException::class.java) { loader.loadMilestones() }
        assertThrows(IllegalStateException::class.java) { loader.loadVerseCatalog() }
        assertThrows(IllegalStateException::class.java) { loader.getScaleComparisons() }
    }

    @Test fun malformedAndEmptyCatalogsAreFailuresAndRetryCanRecover() {
        var broken = true
        val loader = AssetSeedLoader(context) { name ->
            if (broken && name == "recovery_focuses.json") "{broken"
            else context.assets.open(name).bufferedReader().use { it.readText() }
        }
        assertThrows(IllegalStateException::class.java) { loader.loadRecoveryFocuses() }
        broken = false
        assertEquals(18, loader.loadRecoveryFocuses().size)
        val empty = AssetSeedLoader(context) { "{\"label\":\"Synthetic\",\"items\":[]}" }
        assertThrows(IllegalStateException::class.java) { empty.loadRecoveryFocuses() }
    }

    @Test fun truncatedButWellFormedFocusCatalogIsNotReady() {
        val loader = AssetSeedLoader(context) { name ->
            if (name == "recovery_focuses.json") """{"label":"Synthetic","items":[{"id":"discipline","label":"Discipline"}]}"""
            else context.assets.open(name).bufferedReader().use { it.readText() }
        }
        assertThrows(IllegalStateException::class.java) { loader.loadRecoveryFocuses() }
    }
}
