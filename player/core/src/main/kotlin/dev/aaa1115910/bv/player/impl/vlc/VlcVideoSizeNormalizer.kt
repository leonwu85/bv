package dev.aaa1115910.bv.player.impl.vlc

import kotlin.math.abs

/**
 * VLC reports the *coded* picture size in `IMedia.VideoTrack` (e.g. 1920x1088 for a 1080p H.264
 * stream), not the visible size. Coded dimensions are only ever padded up to the next multiple of
 * 16, so when a dimension is a multiple of 16 and shaving at most 15 pixels yields (almost exactly)
 * a standard aspect ratio while the raw size does not, the padding is removed. Anything else is
 * returned unchanged so unusual but legitimate sizes are never "corrected".
 */
internal object VlcVideoSizeNormalizer {
    private const val MACROBLOCK = 16
    private const val MAX_PADDING = MACROBLOCK - 1
    /** Padding removal only ever yields an *exact* standard ratio, so the tolerance is very tight. */
    private const val RATIO_TOLERANCE = 0.0002f

    private val standardAspectRatios = listOf(
        16f / 9f, 4f / 3f, 3f / 2f, 16f / 10f, 5f / 4f, 21f / 9f, 2.35f, 2.39f, 1f,
        9f / 16f, 3f / 4f, 2f / 3f, 10f / 16f, 4f / 5f, 9f / 21f,
    )

    fun normalize(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return width to height
        if (ratioError(width, height) <= RATIO_TOLERANCE) return width to height

        var best: Pair<Int, Int>? = null
        var bestError = Float.MAX_VALUE
        for (w in candidates(width)) {
            for (h in candidates(height)) {
                if (w == width && h == height) continue
                val error = ratioError(w, h)
                if (error <= RATIO_TOLERANCE && error < bestError) {
                    best = w to h
                    bestError = error
                }
            }
        }
        return best ?: (width to height)
    }

    /** The raw value first, then increasingly trimmed values that could have been padded to it. */
    private fun candidates(value: Int): List<Int> {
        if (value % MACROBLOCK != 0) return listOf(value)
        return listOf(value) + (1..MAX_PADDING).map { value - it }.filter { it > 0 }
    }

    private fun ratioError(width: Int, height: Int): Float {
        val ratio = width.toFloat() / height.toFloat()
        return standardAspectRatios.minOf { abs(it - ratio) / it }
    }
}
