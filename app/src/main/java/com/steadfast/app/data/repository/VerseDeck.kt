package com.steadfast.app.data.repository

import kotlin.random.Random

data class VerseDeckState(val usedIds: Set<String> = emptySet(), val lastId: String? = null)

object VerseDeck {
    /** Sampling unused cards without replacement is a shuffle, with priority confined to this cycle. */
    fun draw(ids: List<String>, priority: Set<String>, state: VerseDeckState, random: Random = Random.Default): VerseDeckState {
        require(ids.isNotEmpty() && ids.distinct().size == ids.size)
        val catalog = ids.toSet()
        val used = state.usedIds.intersect(catalog).toMutableSet()
        if (used.size == catalog.size) used.clear()
        val unused = ids.filter { it !in used && (used.isNotEmpty() || ids.size == 1 || it != state.lastId) }
        val prioritized = unused.filter { it in priority }
        val selected = (prioritized.ifEmpty { unused }).random(random)
        used.add(selected)
        return VerseDeckState(used, selected)
    }
}
