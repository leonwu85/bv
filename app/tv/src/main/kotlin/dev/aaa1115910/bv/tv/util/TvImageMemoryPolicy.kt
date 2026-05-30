package dev.aaa1115910.bv.tv.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.bv.util.ImageSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class TvImageMemoryPolicy(
    val detailImageSize: ImageSize,
    val detailLongImageSize: ImageSize,
    val previewViewportScale: Float,
    val previewMaxDecodePixels: Int,
    val previewPrefetchCount: Int
) {
    fun previewRequestSize(
        picture: Picture,
        viewportWidth: Int,
        viewportHeight: Int
    ): Pair<Int, Int> {
        val targetBoxWidth = max(1, (max(1, viewportWidth) * previewViewportScale).roundToInt())
        val targetBoxHeight = max(1, (max(1, viewportHeight) * previewViewportScale).roundToInt())
        val sourceWidth = picture.width
        val sourceHeight = picture.height

        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return capToMaxPixels(targetBoxWidth, targetBoxHeight)
        }

        val scale = min(
            targetBoxWidth.toFloat() / sourceWidth,
            targetBoxHeight.toFloat() / sourceHeight
        ).coerceAtMost(1f)
        val targetWidth = max(1, (sourceWidth * scale).roundToInt())
        val targetHeight = max(1, (sourceHeight * scale).roundToInt())
        return capToMaxPixels(targetWidth, targetHeight)
    }

    fun containerRequestSize(
        viewportWidth: Int,
        viewportHeight: Int
    ): Pair<Int, Int> {
        val targetWidth = max(1, (max(1, viewportWidth) * previewViewportScale).roundToInt())
        val targetHeight = max(1, (max(1, viewportHeight) * previewViewportScale).roundToInt())
        return capToMaxPixels(targetWidth, targetHeight)
    }

    internal fun capToMaxPixels(width: Int, height: Int): Pair<Int, Int> {
        val pixels = width.toLong() * height.toLong()
        if (pixels <= previewMaxDecodePixels) return width to height

        val scale = sqrt(previewMaxDecodePixels.toDouble() / pixels.toDouble())
        return max(1, (width * scale).toInt()) to max(1, (height * scale).toInt())
    }
}

fun Context.tvImageMemoryPolicy(): TvImageMemoryPolicy {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memoryInfo)
    val memoryClassMb = activityManager?.memoryClass ?: 128
    val largeMemoryClassMb = activityManager?.largeMemoryClass ?: memoryClassMb
    val totalMemoryMb = (memoryInfo.totalMem / 1024L / 1024L).toInt()
    val availableMemoryMb = (memoryInfo.availMem / 1024L / 1024L).toInt()
    val isLowRamDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
        activityManager?.isLowRamDevice == true

    return when {
        memoryInfo.lowMemory ||
            isLowRamDevice ||
            memoryClassMb <= 192 ||
            totalMemoryMb in 1..2048 ||
            availableMemoryMb in 1..512 -> TvImageMemoryPolicy(
            detailImageSize = ImageSize.DynamicDetailSmall,
            detailLongImageSize = ImageSize.DynamicLongDetailSmall,
            previewViewportScale = 1.25f,
            previewMaxDecodePixels = 2_000_000,
            previewPrefetchCount = 0
        )

        memoryClassMb >= 384 || largeMemoryClassMb >= 512 -> TvImageMemoryPolicy(
            detailImageSize = ImageSize.DynamicDetailLarge,
            detailLongImageSize = ImageSize.DynamicLongDetailLarge,
            previewViewportScale = 3f,
            previewMaxDecodePixels = 8_000_000,
            previewPrefetchCount = 2
        )

        else -> TvImageMemoryPolicy(
            detailImageSize = ImageSize.DynamicDetailMedium,
            detailLongImageSize = ImageSize.DynamicLongDetailMedium,
            previewViewportScale = 2f,
            previewMaxDecodePixels = 4_000_000,
            previewPrefetchCount = 1
        )
    }
}
