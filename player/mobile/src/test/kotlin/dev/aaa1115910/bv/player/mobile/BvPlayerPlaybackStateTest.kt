package dev.aaa1115910.bv.player.mobile

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BvPlayerPlaybackStateTest {
    @Test
    fun preservesFirstPlayTapWhileInitialCidIsResolved() {
        assertFalse(shouldResetPlaybackStartedState(previousCid = 0L, currentCid = 123L))
    }

    @Test
    fun resetsPlaybackStateWhenSwitchingBetweenResolvedVideos() {
        assertTrue(shouldResetPlaybackStartedState(previousCid = 123L, currentCid = 456L))
        assertFalse(shouldResetPlaybackStartedState(previousCid = 123L, currentCid = 123L))
    }
}
