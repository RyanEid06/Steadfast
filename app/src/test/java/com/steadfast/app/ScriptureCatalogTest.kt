package com.steadfast.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.steadfast.app.data.model.ScriptureEntry
import com.steadfast.app.data.seed.AssetSeedLoader
import com.steadfast.app.data.seed.CatalogLoadException
import java.io.File
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptureCatalogTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val catalog get() = AssetSeedLoader(context).loadScriptureCatalog()
    private val root: File get() = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "data/scripture_source/provenance.json").isFile }
    private val codes = "GEN EXO LEV NUM DEU JOS JDG RUT 1SA 2SA 1KI 2KI 1CH 2CH EZR NEH EST JOB PSA PRO ECC SNG ISA JER LAM EZK DAN HOS JOL AMO OBA JON MIC NAM HAB ZEP HAG ZEC MAL MAT MRK LUK JHN ACT ROM 1CO 2CO GAL EPH PHP COL 1TH 2TH 1TI 2TI TIT PHM HEB JAS 1PE 2PE 1JN 2JN 3JN JUD REV".split(' ')

    @Test fun exact500PassagesCoverBothTestamentsAndAll66BooksWithoutOverlap() {
        val entries = catalog
        assertEquals(500, entries.size)
        assertEquals(500, entries.map { it.id }.toSet().size)
        assertEquals(500, entries.map { it.reference }.toSet().size)
        assertEquals(500, entries.map { it.text }.toSet().size)
        assertEquals((1..66).toSet(), entries.map { it.bookId }.toSet())
        assertEquals(249, entries.count { it.testament == "OT" })
        assertEquals(251, entries.count { it.testament == "NT" })
        val requiredThemes = "temptation sexual-temptation self-control strength perseverance anxiety fear loneliness sadness hopelessness guilt forgiveness repentance love identity peace renewal discipline wisdom courage endurance prayer trust hope gratitude anger patience bondage freedom".split(' ')
        requiredThemes.forEach { theme ->
            assertTrue("Insufficient coverage of $theme", entries.count { theme in it.tags } >= 3)
        }
        assertEquals(listOf("temptation", "endurance"), entries.single { it.reference == "1 Corinthians 10:12-13" }.tags)
        assertEquals(listOf("anxiety", "peace"), entries.single { it.reference == "Philippians 4:4-7" }.tags)
        assertEquals(listOf("guilt", "forgiveness"), entries.single { it.reference == "1 John 1:8-9" }.tags)
        val used = mutableSetOf<Triple<Int, Int, Int>>()
        entries.forEach { entry ->
            assertTrue(entry.text.isNotBlank())
            assertTrue(entry.tags.isNotEmpty())
            (entry.verseStart..entry.verseEnd).forEach { verse ->
                assertTrue("Overlapping ${entry.reference}", used.add(Triple(entry.bookId, entry.chapter, verse)))
            }
        }
        assertEquals(1263, used.size)
        assertEquals("Watch and pray, that you don’t enter into temptation. The spirit indeed is willing, but the flesh is weak.”",
            entries.single { it.reference == "Matthew 26:41" }.text)
        assertTrue(entries.single { it.reference == "Philippians 4:11-13" }.text
            .endsWith("I can do all things through Christ who strengthens me."))
    }

    @Test fun everyWordAndReferenceMatchesIndependentPublisherXmlAndCurationManifest() {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val document = factory.newDocumentBuilder().parse(File(root, "data/scripture_source/eng-web-selected.xml"))
        val verses = document.getElementsByTagName("v")
        val source = mutableMapOf<Triple<String, Int, Int>, String>()
        for (index in 0 until verses.length) {
            val node = verses.item(index)
            val attributes = node.attributes
            val key = Triple(attributes.getNamedItem("b").nodeValue,
                attributes.getNamedItem("c").nodeValue.toInt(), attributes.getNamedItem("v").nodeValue.toInt())
            assertNull("Duplicate publisher verse $key", source.put(key, node.textContent.trim()))
        }
        val bounds = JSONObject(File(root, "data/scripture_source/chapter_verse_counts.json").readText())
        val books = AssetSeedLoader(context).loadBibleBooks().associateBy { it.id }
        var position = 0
        val entries = catalog
        File(root, "data/scripture_curation.tsv").readLines().filter { it.isNotBlank() && !it.startsWith('#') }.forEach { line ->
            val (bookId, tags, references) = line.split('\t')
            val book = books.getValue(bookId.toInt())
            val code = codes[book.id - 1]
            references.split(' ').forEach { reference ->
                val entry = entries[position++]
                val (chapter, range) = reference.split(':')
                val numbers = range.split('-').map(String::toInt)
                val start = numbers.first()
                val end = numbers.last()
                assertEquals(book.id, entry.bookId)
                assertEquals(book.name, entry.book)
                assertEquals(book.testament, entry.testament)
                assertEquals(chapter.toInt(), entry.chapter)
                assertEquals(start, entry.verseStart)
                assertEquals(end, entry.verseEnd)
                assertEquals("${book.name} $reference", entry.reference)
                assertEquals(tags.split(','), entry.tags)
                assertTrue(entry.chapter in 1..book.chapters)
                assertTrue(end <= bounds.getJSONArray(code).getInt(entry.chapter - 1))
                val expected = (start..end).joinToString(" ") { source.getValue(Triple(code, entry.chapter, it)) }
                assertEquals("Incorrect text for ${entry.reference}", expected, entry.text)
            }
        }
        assertEquals(500, position)
        assertEquals(1263, source.size)
        assertEquals(1189, codes.sumOf { bounds.getJSONArray(it).length() })
    }

    @Test fun referenceAssetsAndPinnedSourceChecksumsAreReproducible() {
        val provenance = JSONObject(File(root, "data/scripture_source/provenance.json").readText())
        assertEquals("fd2ddfed425dabe379b03d1bf9d7244e676a3ffb1fb74eed9dd1e9e1e900a836", provenance.getString("archiveSha256"))
        assertEquals("https://ebible.org/Scriptures/eng-web_vpl.zip", provenance.getString("sourceUrl"))
        val files = provenance.getJSONObject("files")
        files.keys().forEach { path ->
            val digest = MessageDigest.getInstance("SHA-256").digest(File(root, path).readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            assertEquals("Checksum for $path", files.getString(path), digest)
        }
        assertArrayEquals(File(root, "data/scripture_catalog.json").readBytes(),
            context.assets.open("scripture_catalog.json").use { it.readBytes() })
        assertEquals(500, provenance.getInt("passageCount"))
        assertEquals(1263, provenance.getInt("verseCount"))
    }

    @Test fun malformedIncompleteAndInvalidScriptureCannotBecomeReady() {
        val entries = catalog
        val corruptions = listOf(
            entries.dropLast(1),
            entries.toMutableList().apply { this[1] = this[0] },
            entries.toMutableList().apply { this[0] = this[0].copy(book = "Wrong book") },
            entries.toMutableList().apply { this[0] = this[0].copy(testament = "NT") },
            entries.toMutableList().apply { this[0] = this[0].copy(chapter = 51) },
            entries.toMutableList().apply { this[0] = this[0].copy(verseStart = 0) },
            entries.toMutableList().apply { this[0] = this[0].copy(verseEnd = 32) },
            entries.toMutableList().apply { this[0] = this[0].copy(verseStart = 4) },
            entries.toMutableList().apply { this[0] = this[0].copy(id = "unrelated") },
            entries.toMutableList().apply { this[0] = this[0].copy(reference = "Genesis 2:1") },
            entries.toMutableList().apply { this[0] = this[0].copy(text = " ") },
            entries.toMutableList().apply { this[0] = this[0].copy(tags = emptyList()) },
            entries.toMutableList().apply {
                this[1] = this[1].copy(id = "web-GEN-001-003-003", chapter = 1, verseStart = 3,
                    verseEnd = 3, reference = "Genesis 1:3")
            }
        )
        val type = Types.newParameterizedType(List::class.java, ScriptureEntry::class.java)
        val adapter = Moshi.Builder().build().adapter<List<ScriptureEntry>>(type)
        (corruptions.map(adapter::toJson) + listOf("null", "[]", "{broken")).forEach { json ->
            val loader = AssetSeedLoader(context) { name -> if (name == "scripture_catalog.json") json else asset(name) }
            val failure = assertThrows(CatalogLoadException::class.java) { loader.loadScriptureCatalog() }
            assertEquals("scripture_catalog.json", failure.assetName)
        }
    }

    @Test fun scriptureFailureRetriesAndBadVerseBoundsCannotLoad() {
        var broken = true
        val loader = AssetSeedLoader(context) { name ->
            if (broken && name == "scripture_catalog.json") "[]" else asset(name)
        }
        assertThrows(CatalogLoadException::class.java) { loader.loadScriptureCatalog() }
        broken = false
        assertEquals(500, loader.loadScriptureCatalog().size)
        val badBounds = AssetSeedLoader(context) { name -> if (name == "scripture_verse_counts.json") "{}" else asset(name) }
        assertThrows(CatalogLoadException::class.java) { badBounds.loadScriptureCatalog() }
    }

    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }

    @Test fun shippedSourcesContainNoReferenceOnlyPromptOrInternetPermission() {
        val main = File(root, "app/src/main")
        main.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml", "json") }.forEach { file ->
            assertFalse("Reference-only Scripture fallback in ${file.name}", file.readText().contains("Open your Bible to"))
        }
        assertFalse(File(main, "AndroidManifest.xml").readText().contains("android.permission.INTERNET"))
    }
}
