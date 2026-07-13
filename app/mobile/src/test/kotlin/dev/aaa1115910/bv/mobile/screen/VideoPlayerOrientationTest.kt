package dev.aaa1115910.bv.mobile.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoPlayerOrientationTest {
    @Test
    fun mapsPortraitAndLandscapeOrientationsToTiltAngle() {
        assertEquals(0, landscapeTiltDegrees(0))
        assertEquals(90, landscapeTiltDegrees(90))
        assertEquals(0, landscapeTiltDegrees(180))
        assertEquals(90, landscapeTiltDegrees(270))
    }

    @Test
    fun mapsBothDiagonalDirectionsConsistently() {
        assertEquals(45, landscapeTiltDegrees(45))
        assertEquals(45, landscapeTiltDegrees(135))
        assertEquals(45, landscapeTiltDegrees(225))
        assertEquals(45, landscapeTiltDegrees(315))
    }

    @Test
    fun ignoresUnknownOrientationValues() {
        assertNull(landscapeTiltDegrees(-1))
        assertNull(landscapeTiltDegrees(360))
    }
}
