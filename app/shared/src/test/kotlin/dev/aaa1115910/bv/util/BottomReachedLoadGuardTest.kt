package dev.aaa1115910.bv.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomReachedLoadGuardTest {
    @Test
    fun loadingTransitionsDoNotRetriggerTheSameItems() {
        val guard = BottomReachedLoadGuard()

        assertTrue(
            guard.shouldTrigger(shouldLoadMore = true, loading = false, totalItemsCount = 20)
        )
        assertFalse(
            guard.shouldTrigger(shouldLoadMore = true, loading = true, totalItemsCount = 20)
        )
        assertFalse(
            guard.shouldTrigger(shouldLoadMore = true, loading = false, totalItemsCount = 20)
        )
    }

    @Test
    fun newlyAppendedItemsCanTriggerAnotherLoadWhenStillAtBottom() {
        val guard = BottomReachedLoadGuard()

        assertTrue(
            guard.shouldTrigger(shouldLoadMore = true, loading = false, totalItemsCount = 20)
        )
        assertFalse(
            guard.shouldTrigger(shouldLoadMore = true, loading = true, totalItemsCount = 40)
        )
        assertTrue(
            guard.shouldTrigger(shouldLoadMore = true, loading = false, totalItemsCount = 40)
        )
    }

    @Test
    fun leavingBottomRearmsTheSameItemCount() {
        val guard = BottomReachedLoadGuard()

        assertTrue(
            guard.shouldTrigger(shouldLoadMore = true, loading = false, totalItemsCount = 20)
        )
        assertFalse(
            guard.shouldTrigger(shouldLoadMore = false, loading = false, totalItemsCount = 20)
        )
        assertTrue(
            guard.shouldTrigger(shouldLoadMore = true, loading = false, totalItemsCount = 20)
        )
    }

    @Test
    fun emptyLayoutTriggersOnlyOnceUntilItsContentChanges() {
        val guard = BottomReachedLoadGuard()

        assertTrue(
            guard.shouldTrigger(shouldLoadMore = true, loading = false, totalItemsCount = 0)
        )
        assertFalse(
            guard.shouldTrigger(shouldLoadMore = true, loading = false, totalItemsCount = 0)
        )
        assertTrue(
            guard.shouldTrigger(shouldLoadMore = true, loading = false, totalItemsCount = 20)
        )
    }
}
