package com.steadfast.app

import com.steadfast.app.data.repository.VerseDeck
import com.steadfast.app.data.repository.VerseDeckState
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class VerseDeckTest {
    private val ids = (1..500).map { "verse-$it" }

    @Test fun everyCycleExhaustsAll500EvenWithCompletedChapterPriority() {
        var state = VerseDeckState()
        val random = Random(71)
        var last: String? = null
        repeat(4) {
            val shown = mutableSetOf<String>()
            repeat(500) { index ->
                val draw = VerseDeck.draw(ids, ids.take(3).toSet(), state, random)
                assertNotEquals(last, draw.lastId)
                assertTrue("Premature repetition", shown.add(draw.lastId!!))
                if (index < 3) assertTrue(draw.lastId in ids.take(3))
                state = draw
                last = draw.lastId
            }
            assertEquals(ids.toSet(), shown)
        }
    }

    @Test fun reconstructedStateContinuesWithoutRepeating() {
        var state = VerseDeckState()
        val shown = mutableSetOf<String>()
        repeat(500) {
            state = VerseDeck.draw(ids, emptySet(), VerseDeckState(state.usedIds.toSet(), state.lastId), Random(it))
            assertTrue(shown.add(state.lastId!!))
        }
    }

    @Test fun catalogChangesRetainUsedReferencesAndDiscardRemovedOnes() {
        val state = VerseDeckState(setOf("a", "removed"), "a")
        val next = VerseDeck.draw(listOf("a", "b", "new"), setOf("a"), state, Random(4))
        assertTrue(next.lastId in setOf("b", "new"))
        assertTrue("a" in next.usedIds)
        assertFalse("removed" in next.usedIds)
    }

    @Test fun singleEntryRemainsUsableAndEmptyCatalogIsRejected() {
        val first = VerseDeck.draw(listOf("only"), emptySet(), VerseDeckState())
        assertEquals("only", VerseDeck.draw(listOf("only"), emptySet(), first).lastId)
        assertThrows(IllegalArgumentException::class.java) { VerseDeck.draw(emptyList(), emptySet(), first) }
    }
}
