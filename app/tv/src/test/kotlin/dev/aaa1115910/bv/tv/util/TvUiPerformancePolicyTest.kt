package dev.aaa1115910.bv.tv.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvUiPerformancePolicyTest {
    @Test
    fun lowMemoryMediaTekUsesConservativeProfile() {
        val profile = TvUiPerformancePolicy.resolve(
            hardwareIdentity = "MediaTek MT5895",
            totalMemoryMb = 2_048,
            processorCount = 4,
        )

        assertEquals(TvUiPerformanceTier.Conservative, profile.tier)
        assertEquals(2, profile.maxKeepPages)
        assertFalse(profile.allowFullPageAnimation)
        assertTrue(profile.isMediaTek)
    }

    @Test
    fun higherMemoryMediaTekStillAvoidsFullPageAnimation() {
        val profile = TvUiPerformancePolicy.resolve(
            hardwareIdentity = "mediatek pentonic",
            totalMemoryMb = 4_096,
            processorCount = 8,
        )

        assertEquals(TvUiPerformanceTier.Balanced, profile.tier)
        assertEquals(2, profile.maxKeepPages)
        assertFalse(profile.allowFullPageAnimation)
    }

    @Test
    fun capableNonMediaTekUsesStandardProfile() {
        val profile = TvUiPerformancePolicy.resolve(
            hardwareIdentity = "vendor reference board",
            totalMemoryMb = 4_096,
            processorCount = 8,
        )

        assertEquals(TvUiPerformanceTier.Standard, profile.tier)
        assertEquals(3, profile.maxKeepPages)
        assertTrue(profile.allowFullPageAnimation)
        assertFalse(profile.isMediaTek)
    }
}
