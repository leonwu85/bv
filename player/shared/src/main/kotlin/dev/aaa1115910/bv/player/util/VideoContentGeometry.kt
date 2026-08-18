package dev.aaa1115910.bv.player.util

data class VideoContentBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

/**
 * Resolves the visible video geometry shared by the video surface and danmaku mask.
 *
 * Some play-url responses do not contain stream dimensions, and a few portrait streams report
 * their coded (landscape) dimensions before rotation is applied. In those cases the page-level
 * portrait flag is the only reliable orientation signal.
 */
object VideoContentGeometry {
    const val DEFAULT_LANDSCAPE_ASPECT_RATIO = 16f / 9f
    const val DEFAULT_PORTRAIT_ASPECT_RATIO = 9f / 16f

    fun resolveAspectRatio(
        width: Int,
        height: Int,
        isPortraitVideo: Boolean,
        fallbackAspectRatio: Float = if (isPortraitVideo) {
            DEFAULT_PORTRAIT_ASPECT_RATIO
        } else {
            DEFAULT_LANDSCAPE_ASPECT_RATIO
        },
    ): Float {
        val measuredAspectRatio = if (width > 0 && height > 0) {
            width.toFloat() / height.toFloat()
        } else {
            Float.NaN
        }
        val validFallback = fallbackAspectRatio.takeIf { it.isFinite() && it > 0f }
            ?: if (isPortraitVideo) {
                DEFAULT_PORTRAIT_ASPECT_RATIO
            } else {
                DEFAULT_LANDSCAPE_ASPECT_RATIO
            }
        val candidate = measuredAspectRatio.takeIf { it.isFinite() && it > 0f }
            ?: validFallback

        return if (isPortraitVideo && candidate > 1f) {
            1f / candidate
        } else {
            candidate
        }
    }

    fun fitCenter(
        containerWidth: Float,
        containerHeight: Float,
        videoAspectRatio: Float,
    ): VideoContentBounds {
        if (
            !containerWidth.isFinite() ||
            !containerHeight.isFinite() ||
            containerWidth <= 0f ||
            containerHeight <= 0f
        ) {
            return VideoContentBounds(0f, 0f, 0f, 0f)
        }
        if (!videoAspectRatio.isFinite() || videoAspectRatio <= 0f) {
            return VideoContentBounds(0f, 0f, containerWidth, containerHeight)
        }

        val containerAspectRatio = containerWidth / containerHeight
        return if (videoAspectRatio > containerAspectRatio) {
            val videoHeight = containerWidth / videoAspectRatio
            VideoContentBounds(
                left = 0f,
                top = (containerHeight - videoHeight) / 2f,
                width = containerWidth,
                height = videoHeight,
            )
        } else {
            val videoWidth = containerHeight * videoAspectRatio
            VideoContentBounds(
                left = (containerWidth - videoWidth) / 2f,
                top = 0f,
                width = videoWidth,
                height = containerHeight,
            )
        }
    }
}
