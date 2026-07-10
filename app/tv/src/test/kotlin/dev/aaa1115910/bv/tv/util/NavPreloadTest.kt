package dev.aaa1115910.bv.tv.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NavPreloadTest {
    private val pages = listOf("user", "home", "ugc", "pgc", "settings")

    @Test
    fun keepsOnlyCurrentAndOneNeighborForTwoPageBudget() {
        assertEquals(
            listOf("ugc", "pgc"),
            boundedAdjacentNavItems(pages, current = "ugc", step = 1, maxItems = 2),
        )
    }

    @Test
    fun fallsBackToPreviousAtRightBoundary() {
        assertEquals(
            listOf("settings", "pgc"),
            boundedAdjacentNavItems(pages, current = "settings", step = 1, maxItems = 2),
        )
    }

    @Test
    fun standardBudgetKeepsBothNeighbors() {
        assertEquals(
            listOf("ugc", "pgc", "home"),
            boundedAdjacentNavItems(pages, current = "ugc", step = 1, maxItems = 3),
        )
    }
}
