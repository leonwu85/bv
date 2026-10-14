package dev.aaa1115910.bv.mobile.activities

import kotlinx.serialization.Serializable

/** Only identifiers and playback position are retained, never pages, players or API responses. */
@Serializable
internal data class VideoDetailHistoryEntry(
    val aid: Long,
    val cid: Long = 0L,
    val epid: Int? = null,
    val seasonId: Int? = null,
    val fromSeason: Boolean = false,
    val fromToView: Boolean = false,
    val playOfflineCache: Boolean = false,
    val resumeHistory: Boolean = true,
    val positionMs: Long? = null,
) {
    val isValid: Boolean get() = aid > 0L || (epid ?: 0) > 0 || (seasonId ?: 0) > 0

    fun isSameVideo(other: VideoDetailHistoryEntry): Boolean = when {
        aid > 0L && other.aid > 0L -> aid == other.aid
        epid != null && other.epid != null -> epid == other.epid
        else -> epid == null && other.epid == null && seasonId != null && seasonId == other.seasonId
    }
}

internal class VideoDetailHistory {
    companion object {
        const val MAX_ENTRIES = 20
    }

    private val entries = ArrayDeque<VideoDetailHistoryEntry>()

    fun recordNavigation(
        current: VideoDetailHistoryEntry?,
        next: VideoDetailHistoryEntry?,
        enabled: Boolean,
        fromRecommendation: Boolean,
    ) {
        if (!enabled || !fromRecommendation || current == null || next == null) {
            clear()
            return
        }
        if (!current.isValid || !next.isValid || current.isSameVideo(next)) return
        if (entries.size == MAX_ENTRIES) entries.removeFirst()
        entries.addLast(current)
    }

    fun pop(enabled: Boolean): VideoDetailHistoryEntry? {
        if (!enabled) clear()
        return entries.removeLastOrNull()
    }

    fun clear() = entries.clear()

    fun snapshot(): List<VideoDetailHistoryEntry> = entries.toList()

    fun restore(savedEntries: List<VideoDetailHistoryEntry>) {
        clear()
        entries.addAll(savedEntries.filter { it.isValid }.takeLast(MAX_ENTRIES))
    }
}
