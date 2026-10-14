package dev.aaa1115910.bv.player.tv

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.player.seekbar.ViewPointSegment
import dev.aaa1115910.bv.player.tv.theme.PlayerColors

internal fun DrawScope.drawSegmentedTrack(
    duration: Long,
    progressFraction: Float,
    bufferedFraction: Float,
    segments: List<ViewPointSegment>,
    playedTrackBrush: Brush,
    trackColor: Color = PlayerColors.seekBarTrack,
    bufferedColor: Color = PlayerColors.seekBarBuffered,
) {
    if (size.width <= 0f || size.height <= 0f) return
    val progressX = size.width * progressFraction.coerceIn(0f, 1f)
    val bufferedX = size.width * bufferedFraction.coerceIn(0f, 1f)
    if (segments.isEmpty()) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(trackColor, size = size, cornerRadius = radius)
        drawRoundRect(bufferedColor, size = Size(bufferedX, size.height), cornerRadius = radius)
        drawRoundRect(playedTrackBrush, size = Size(progressX, size.height), cornerRadius = radius)
        return
    }
    val total = duration.coerceAtLeast(1).toDouble()
    segments.forEachIndexed { index, segment ->
        val start = (size.width * (segment.startMs / total)).toFloat()
        val end = (size.width * (segment.endMs / total)).toFloat()
        // Short chapters keep their true position and remain visible on dense timelines.
        val inset = minOf(2.dp.toPx(), (end - start) / 4f)
        val left = start + if (index == 0) 0f else inset
        val right = end - if (index == segments.lastIndex) 0f else inset
        if (right <= left) return@forEachIndexed
        val trackSize = Size(right - left, size.height)
        val radius = CornerRadius(minOf(size.height / 2f, trackSize.width / 2f))
        drawRoundRect(trackColor, Offset(left, 0f), trackSize, radius)
        if (bufferedX > left) clipRect(left = left, right = minOf(right, bufferedX)) {
            drawRoundRect(bufferedColor, Offset(left, 0f), trackSize, radius)
        }
        if (progressX > left) clipRect(left = left, right = minOf(right, progressX)) {
            drawRoundRect(playedTrackBrush, Offset(left, 0f), trackSize, radius)
        }
    }
}
