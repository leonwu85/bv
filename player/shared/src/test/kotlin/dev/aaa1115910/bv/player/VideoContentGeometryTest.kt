package dev.aaa1115910.bv.player

import dev.aaa1115910.bv.player.util.VideoContentGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoContentGeometryTest {
    @Test
    fun `portrait metadata maps mask to centered image bounds`() {
        val aspectRatio = VideoContentGeometry.resolveAspectRatio(
            width = 1080,
            height = 1920,
            isPortraitVideo = true,
        )
        val bounds = VideoContentGeometry.fitCenter(
            containerWidth = 1920f,
            containerHeight = 1080f,
            videoAspectRatio = aspectRatio,
        )

        assertEquals(9f / 16f, aspectRatio)
        assertEquals(656.25f, bounds.left)
        assertEquals(0f, bounds.top)
        assertEquals(607.5f, bounds.width)
        assertEquals(1080f, bounds.height)
    }

    @Test
    fun `portrait flag supplies portrait fallback when dimensions are unavailable`() {
        val aspectRatio = VideoContentGeometry.resolveAspectRatio(
            width = 0,
            height = 0,
            isPortraitVideo = true,
        )

        assertEquals(9f / 16f, aspectRatio)
        assertTrue(aspectRatio < 1f)
    }

    @Test
    fun `portrait flag normalizes coded landscape dimensions`() {
        val aspectRatio = VideoContentGeometry.resolveAspectRatio(
            width = 1920,
            height = 1080,
            isPortraitVideo = true,
        )

        assertEquals(9f / 16f, aspectRatio)
    }

    @Test
    fun `landscape and four by three content retain expected bounds`() {
        val landscapeBounds = VideoContentGeometry.fitCenter(
            containerWidth = 1920f,
            containerHeight = 1080f,
            videoAspectRatio = 16f / 9f,
        )
        val fourByThreeBounds = VideoContentGeometry.fitCenter(
            containerWidth = 1920f,
            containerHeight = 1080f,
            videoAspectRatio = 4f / 3f,
        )

        assertEquals(0f, landscapeBounds.left)
        assertEquals(1920f, landscapeBounds.width)
        assertEquals(240f, fourByThreeBounds.left)
        assertEquals(1440f, fourByThreeBounds.width)
    }

    @Test
    fun `invalid mask aspect safely falls back to full container`() {
        val bounds = VideoContentGeometry.fitCenter(
            containerWidth = 1920f,
            containerHeight = 1080f,
            videoAspectRatio = Float.NaN,
        )

        assertEquals(0f, bounds.left)
        assertEquals(0f, bounds.top)
        assertEquals(1920f, bounds.width)
        assertEquals(1080f, bounds.height)
    }
}
