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

    @Test
    fun twoPageBudgetKeepsThePreviousDrawerDestinationForRoundTrips() {
        assertEquals(
            listOf("ugc", "home"),
            boundedAdjacentNavItems(pages, "ugc", maxItems = 2, preferredNeighbor = "home"),
        )
        assertEquals(
            listOf("home", "ugc"),
            boundedAdjacentNavItems(pages, "home", maxItems = 2, preferredNeighbor = "ugc"),
        )
    }

    @Test
    fun preferredNeighborDoesNotDuplicatePagesOrDisplaceTheOtherNeighborWithThreeSlots() {
        assertEquals(
            listOf("ugc", "home", "pgc"),
            boundedAdjacentNavItems(pages, "ugc", maxItems = 3, preferredNeighbor = "home"),
        )
        assertEquals(
            listOf("ugc", "pgc", "home"),
            boundedAdjacentNavItems(pages, "ugc", maxItems = 3, preferredNeighbor = "pgc"),
        )
    }

    @Test
    fun ignoresPreferencesOutsideTheWindowOrRemovedFromNavigation() {
        for (preferred in listOf("user", "removed", "ugc")) {
            assertEquals(
                listOf("ugc", "pgc"),
                boundedAdjacentNavItems(pages, "ugc", maxItems = 2, preferredNeighbor = preferred),
            )
        }
    }

    @Test
    fun preferenceCannotOverrideOnePageBudgetOrDisabledPreloading() {
        assertEquals(
            listOf("ugc"),
            boundedAdjacentNavItems(pages, "ugc", maxItems = 1, preferredNeighbor = "home"),
        )
        assertEquals(
            listOf("ugc"),
            boundedAdjacentNavItems(pages, "ugc", step = 0, maxItems = 3, preferredNeighbor = "home"),
        )
    }
}
