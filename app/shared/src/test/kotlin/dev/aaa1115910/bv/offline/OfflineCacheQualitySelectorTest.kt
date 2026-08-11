package dev.aaa1115910.bv.offline

import dev.aaa1115910.bv.player.entity.Resolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfflineCacheQualitySelectorTest {
    @Test
    fun `selects exact preferred quality when available`() {
        val result = OfflineCacheQualitySelector.select(
            availableQualities = listOf(Resolution.R4K, Resolution.R1080P, Resolution.R720P),
            preferredQuality = Resolution.R1080P,
        )

        assertEquals(Resolution.R1080P, result)
    }

    @Test
    fun `falls back to highest quality below preference`() {
        val result = OfflineCacheQualitySelector.select(
            availableQualities = listOf(Resolution.R1080P, Resolution.R720P, Resolution.R480P),
            preferredQuality = Resolution.R4K,
        )

        assertEquals(Resolution.R1080P, result)
    }

    @Test
    fun `uses lowest available quality when every option is above preference`() {
        val result = OfflineCacheQualitySelector.select(
            availableQualities = listOf(Resolution.R4K, Resolution.R1080P),
            preferredQuality = Resolution.R720P,
        )

        assertEquals(Resolution.R1080P, result)
    }

    @Test
    fun `returns null when no quality is available`() {
        assertNull(
            OfflineCacheQualitySelector.select(
                availableQualities = emptyList(),
                preferredQuality = Resolution.R1080P,
            )
        )
    }
}
