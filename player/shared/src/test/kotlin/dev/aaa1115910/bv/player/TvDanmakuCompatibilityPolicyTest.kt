package dev.aaa1115910.bv.player

import dev.aaa1115910.bv.player.util.DanmakuMaskSelectionDecision
import dev.aaa1115910.bv.player.util.TvDanmakuCompatibilityPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvDanmakuCompatibilityPolicyTest {
    @Test
    fun `tv before android 8 uses view rendering and disables mask`() {
        listOf(23, 24, 25).forEach { sdkInt ->
            val capabilities = TvDanmakuCompatibilityPolicy.resolve(
                sdkInt = sdkInt,
                isTvDevice = true,
            )

            assertFalse(capabilities.useSurfaceViewForVod, "sdk=$sdkInt")
            assertFalse(capabilities.danmakuMaskSupported, "sdk=$sdkInt")
        }
    }

    @Test
    fun `tv from android 8 keeps surface rendering and mask`() {
        listOf(26, 27, 35).forEach { sdkInt ->
            val capabilities = TvDanmakuCompatibilityPolicy.resolve(
                sdkInt = sdkInt,
                isTvDevice = true,
            )

            assertTrue(capabilities.useSurfaceViewForVod, "sdk=$sdkInt")
            assertTrue(capabilities.danmakuMaskSupported, "sdk=$sdkInt")
        }
    }

    @Test
    fun `non tv devices keep existing behavior before android 8`() {
        val capabilities = TvDanmakuCompatibilityPolicy.resolve(
            sdkInt = 25,
            isTvDevice = false,
        )

        assertTrue(capabilities.useSurfaceViewForVod)
        assertTrue(capabilities.danmakuMaskSupported)
    }

    @Test
    fun `unsupported mask enable request is rejected`() {
        assertEquals(
            DanmakuMaskSelectionDecision.Unsupported,
            TvDanmakuCompatibilityPolicy.resolveMaskSelection(
                danmakuMaskSupported = false,
                requestedEnabled = true,
            ),
        )
        assertEquals(
            DanmakuMaskSelectionDecision.Apply,
            TvDanmakuCompatibilityPolicy.resolveMaskSelection(
                danmakuMaskSupported = false,
                requestedEnabled = false,
            ),
        )
        assertEquals(
            DanmakuMaskSelectionDecision.Apply,
            TvDanmakuCompatibilityPolicy.resolveMaskSelection(
                danmakuMaskSupported = true,
                requestedEnabled = true,
            ),
        )
    }
}
