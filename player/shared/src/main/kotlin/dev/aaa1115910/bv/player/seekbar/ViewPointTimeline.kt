package dev.aaa1115910.bv.player.seekbar

import dev.aaa1115910.bv.player.entity.VideoPlayerViewPoint
import java.util.concurrent.TimeUnit

/** A continuous part of the timeline; a null title represents an unlabelled interval. */
data class ViewPointSegment(val startMs: Long, val endMs: Long, val title: String? = null)

fun buildViewPointTimeline(
    viewPoints: List<VideoPlayerViewPoint>,
    durationMs: Long,
): List<ViewPointSegment> {
    if (durationMs <= 0L) return emptyList()
    val chapters = viewPoints.mapNotNull { point ->
        val start = TimeUnit.SECONDS.toMillis(point.from).coerceIn(0L, durationMs)
        val end = TimeUnit.SECONDS.toMillis(point.to).coerceIn(0L, durationMs)
        if (end <= start || point.content.isBlank()) null
        else ViewPointSegment(start, end, point.content.trim())
    }.sortedWith(compareBy<ViewPointSegment> { it.startMs }.thenByDescending { it.endMs })
        .distinctBy { it.startMs }
    if (chapters.isEmpty()) return emptyList()

    return buildList {
        var end = 0L
        chapters.forEach { chapter ->
            if (chapter.startMs > end) add(ViewPointSegment(end, chapter.startMs))
            // Overlapping API entries must never duplicate time or produce negative widths.
            val start = maxOf(end, chapter.startMs)
            if (chapter.endMs > start) {
                add(chapter.copy(startMs = start))
                end = chapter.endMs
            }
        }
        if (end < durationMs) add(ViewPointSegment(end, durationMs))
    }
}

fun List<ViewPointSegment>.viewPointAt(positionMs: Long): ViewPointSegment? {
    val end = lastOrNull()?.endMs ?: return null
    val position = positionMs.coerceIn(0L, end)
    return firstOrNull {
        position >= it.startMs && (position < it.endMs || position == end && it.endMs == end)
    }?.takeIf { it.title != null }
}
