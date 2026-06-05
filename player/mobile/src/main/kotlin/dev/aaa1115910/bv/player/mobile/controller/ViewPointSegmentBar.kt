package dev.aaa1115910.bv.player.mobile.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.player.entity.VideoPlayerViewPoint

@Composable
fun ViewPointSegmentBar(
    modifier: Modifier = Modifier,
    viewPoints: List<VideoPlayerViewPoint>,
    durationMs: Long,
    currentPositionMs: Long,
    onSeekToPosition: (Long) -> Unit,
) {
    if (viewPoints.isEmpty() || durationMs <= 0L) return

    val segments = remember(viewPoints, durationMs) {
        viewPoints
            .filter { point -> point.endMs > 0L && point.content.isNotBlank() }
            .sortedBy { point -> point.endMs }
    }
    if (segments.isEmpty()) return

    val progressFraction = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(13.dp)
            .background(Color.Gray.copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progressFraction)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var previousEnd = 0f
            segments.forEach { point ->
                val segmentEnd = (point.endMs.toFloat() / durationMs.toFloat())
                    .coerceIn(previousEnd, 1f)
                val weight = (segmentEnd - previousEnd).coerceAtLeast(0.001f)

                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clickable { onSeekToPosition(point.startMs) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = point.content,
                        color = Color.White,
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                previousEnd = segmentEnd
            }

            if (previousEnd < 1f) {
                Spacer(modifier = Modifier.weight((1f - previousEnd).coerceAtLeast(0.001f)))
            }
        }
    }
}
