package dev.aaa1115910.bv.tv.util

import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.bv.util.ImageSize
import kotlin.test.Test
import kotlin.test.assertTrue

class TvImageMemoryPolicyTest {
    private val lowEndPolicy = TvImageMemoryPolicy(
        detailImageSize = ImageSize.DynamicDetailSmall,
        detailLongImageSize = ImageSize.DynamicDetailSmall,
        previewViewportScale = 1.25f,
        previewMaxDecodePixels = 2_000_000,
        previewPrefetchCount = 0
    )

    @Test
    fun containerRequestSizeCapsToMaxPixels() {
        val (width, height) = lowEndPolicy.containerRequestSize(
            viewportWidth = 1920,
            viewportHeight = 1080
        )

        assertTrue(width > 0)
        assertTrue(height > 0)
        assertTrue(width.toLong() * height.toLong() <= 2_000_000L)
    }

    @Test
    fun previewRequestSizeCapsLongImage() {
        val longImage = Picture(
            url = "https://i0.hdslb.com/bfs/new_dyn/example.jpg",
            width = 750,
            height = 8761,
            key = "long-image"
        )

        val (width, height) = lowEndPolicy.previewRequestSize(
            picture = longImage,
            viewportWidth = 1920,
            viewportHeight = 1080
        )

        assertTrue(width > 0)
        assertTrue(height > 0)
        assertTrue(width.toLong() * height.toLong() <= 2_000_000L)
        assertTrue(width < longImage.width || height < longImage.height)
    }

    @Test
    fun previewRequestSizeUsesViewportWhenSourceSizeUnknown() {
        val unknownSizeImage = Picture(
            url = "https://i0.hdslb.com/bfs/new_dyn/unknown.jpg",
            width = 0,
            height = 0,
            key = "unknown-size"
        )

        val (width, height) = lowEndPolicy.previewRequestSize(
            picture = unknownSizeImage,
            viewportWidth = 1920,
            viewportHeight = 1080
        )

        assertTrue(width > 0)
        assertTrue(height > 0)
        assertTrue(width.toLong() * height.toLong() <= 2_000_000L)
    }
}
