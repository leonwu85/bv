package dev.aaa1115910.bv.player.impl.mpv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MpvCachePolicyTest {
    private val gib = 1024L * MpvCacheConfig.MIB

    @Test
    fun tiersFollowTotalMemory() {
        assertEquals(16L, MpvCachePolicy.baseBytes(totalMemBytes = 1 * gib, isLowRamDevice = false) / MpvCacheConfig.MIB)
        assertEquals(32L, MpvCachePolicy.baseBytes(totalMemBytes = 2 * gib, isLowRamDevice = false) / MpvCacheConfig.MIB)
        assertEquals(64L, MpvCachePolicy.baseBytes(totalMemBytes = 4 * gib, isLowRamDevice = false) / MpvCacheConfig.MIB)
    }

    @Test
    fun lowRamFlagForcesSmallestTierRegardlessOfReportedMemory() {
        assertEquals(16L, MpvCachePolicy.baseBytes(totalMemBytes = 4 * gib, isLowRamDevice = true) / MpvCacheConfig.MIB)
    }

    @Test
    fun unknownMemoryFallsBackToLargestTier() {
        // ActivityManager can report 0 on odd builds; do not punish the device for a missing number.
        assertEquals(64L, MpvCachePolicy.baseBytes(totalMemBytes = 0, isLowRamDevice = false) / MpvCacheConfig.MIB)
    }

    @Test
    fun vodUsesBaseAndHalfBackBufferWithTimeCap() {
        val config = MpvCachePolicy.resolve(totalMemBytes = 2 * gib, isLowRamDevice = false, isLive = false, expandBuffer = false)
        assertEquals(32L, config.maxBytesMiB)
        assertEquals(16L, config.maxBackBytesMiB)
        assertEquals(120, config.cacheSecs)
        assertEquals(3.0, config.cachePauseWaitSecs)
    }

    @Test
    fun liveHalvesForwardCacheAndKeepsTinyBackBuffer() {
        val config = MpvCachePolicy.resolve(totalMemBytes = 4 * gib, isLowRamDevice = false, isLive = true, expandBuffer = false)
        assertEquals(32L, config.maxBytesMiB)
        assertEquals(4L, config.maxBackBytesMiB)
        assertEquals(30, config.cacheSecs)
        assertEquals(1.5, config.cachePauseWaitSecs)
    }

    @Test
    fun liveNeverDropsBelowMinimum() {
        val config = MpvCachePolicy.resolve(totalMemBytes = 1 * gib, isLowRamDevice = true, isLive = true, expandBuffer = false)
        assertEquals(8L, config.maxBytesMiB)
    }

    @Test
    fun expandedBufferScalesWithTierAndIsCapped() {
        val low = MpvCachePolicy.resolve(totalMemBytes = 1 * gib, isLowRamDevice = false, isLive = false, expandBuffer = true)
        assertEquals(64L, low.maxBytesMiB)
        assertEquals(16L, low.maxBackBytesMiB)

        val high = MpvCachePolicy.resolve(totalMemBytes = 8 * gib, isLowRamDevice = false, isLive = false, expandBuffer = true)
        assertEquals(256L, high.maxBytesMiB)
        assertTrue(high.cacheSecs > low.cacheSecs || high.cacheSecs == low.cacheSecs)
    }

    @Test
    fun liveTakesPrecedenceOverExpandedBuffer() {
        val config = MpvCachePolicy.resolve(totalMemBytes = 4 * gib, isLowRamDevice = false, isLive = true, expandBuffer = true)
        assertEquals(32L, config.maxBytesMiB)
        assertEquals(30, config.cacheSecs)
    }
}
