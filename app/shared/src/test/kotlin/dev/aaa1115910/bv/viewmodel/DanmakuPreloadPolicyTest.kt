package dev.aaa1115910.bv.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuPreloadPolicyTest {
    @Test
    fun lowMemoryDeviceUsesWindowOnlyDanmakuPreload() {
        val policy = resolveDanmakuPreloadPolicy(
            totalMemoryBytes = 2L.gib,
            availableMemoryBytes = 768L.mib,
            isLowRamDevice = true,
            maxHeapBytes = 256L.mib
        )

        assertEquals(DanmakuPreloadMode.WINDOW_ONLY, policy.mode)
        assertFalse(policy.fullVideoPreloadEnabled)
        assertEquals(3, policy.maxCachedSegments)
    }

    @Test
    fun midMemoryDeviceAvoidsFullVideoDanmakuPreload() {
        val policy = resolveDanmakuPreloadPolicy(
            totalMemoryBytes = 4L.gib,
            availableMemoryBytes = 1024L.mib,
            isLowRamDevice = false,
            maxHeapBytes = 320L.mib
        )

        assertEquals(DanmakuPreloadMode.WINDOW_ONLY, policy.mode)
        assertFalse(policy.fullVideoPreloadEnabled)
    }

    @Test
    fun highMemoryDeviceAllowsFullVideoDanmakuPreload() {
        val policy = resolveDanmakuPreloadPolicy(
            totalMemoryBytes = 8L.gib,
            availableMemoryBytes = 2L.gib,
            isLowRamDevice = false,
            maxHeapBytes = 512L.mib
        )

        assertEquals(DanmakuPreloadMode.FULL_VIDEO, policy.mode)
        assertTrue(policy.fullVideoPreloadEnabled)
    }

    private val Long.mib: Long
        get() = this * 1024L * 1024L

    private val Long.gib: Long
        get() = this * 1024L * 1024L * 1024L
}
