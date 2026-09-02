package dev.aaa1115910.bv.player.impl.vlc

import kotlin.test.Test
import kotlin.test.assertEquals

class VlcVideoSizeNormalizerTest {
    @Test
    fun trimsMacroblockPaddingOnHeight() {
        assertEquals(1920 to 1080, VlcVideoSizeNormalizer.normalize(1920, 1088))
        assertEquals(960 to 540, VlcVideoSizeNormalizer.normalize(960, 544))
        assertEquals(640 to 360, VlcVideoSizeNormalizer.normalize(640, 368))
    }

    @Test
    fun trimsMacroblockPaddingOnWidthForPortraitVideo() {
        assertEquals(1080 to 1920, VlcVideoSizeNormalizer.normalize(1088, 1920))
    }

    @Test
    fun keepsStandardSizesUntouched() {
        assertEquals(1920 to 1080, VlcVideoSizeNormalizer.normalize(1920, 1080))
        assertEquals(1280 to 720, VlcVideoSizeNormalizer.normalize(1280, 720))
        assertEquals(3840 to 2160, VlcVideoSizeNormalizer.normalize(3840, 2160))
        assertEquals(854 to 480, VlcVideoSizeNormalizer.normalize(854, 480))
    }

    @Test
    fun keepsUnusualButLegitimateSizes() {
        // Ultra-wide content must not be "corrected" into a nearby standard ratio.
        assertEquals(2560 to 1080, VlcVideoSizeNormalizer.normalize(2560, 1080))
        // Odd sizes that are not multiples of 16 cannot be padding artefacts.
        assertEquals(1280 to 718, VlcVideoSizeNormalizer.normalize(1280, 718))
    }

    @Test
    fun ignoresInvalidSizes() {
        assertEquals(0 to 1080, VlcVideoSizeNormalizer.normalize(0, 1080))
        assertEquals(1920 to -1, VlcVideoSizeNormalizer.normalize(1920, -1))
    }
}
