package dev.aaa1115910.bv.tv.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LatestRequestOwnerTest {
    @Test
    fun newerClaimPreventsOlderRequestFromCommitting() {
        val owner = LatestRequestOwner()
        val first = owner.claim()
        val second = owner.claim()

        assertFalse(owner.owns(first))
        assertTrue(owner.owns(second))
    }

    @Test
    fun invalidationPreventsInFlightRequestFromCommitting() {
        val owner = LatestRequestOwner()
        val inFlight = owner.claim()

        owner.invalidate()

        assertFalse(owner.owns(inFlight))
    }
}
