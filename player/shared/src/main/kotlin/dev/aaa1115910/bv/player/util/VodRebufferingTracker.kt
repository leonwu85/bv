package dev.aaa1115910.bv.player.util

/** Coalesce transient buffer/playing callbacks by how much video actually played between stalls. */
class VodRebufferingTracker {
    private var playingFromMs: Long? = null
    private var playedSinceLastReportMs = 0L

    fun onPlay(positionMs: Long) {
        if (playingFromMs == null) playingFromMs = positionMs
    }

    fun onPause() {
        playingFromMs = null
        playedSinceLastReportMs = 0L
    }

    fun onBuffering(positionMs: Long): Boolean {
        val start = playingFromMs ?: return false
        playingFromMs = null
        playedSinceLastReportMs += (positionMs - start).coerceAtLeast(0L)
        if (playedSinceLastReportMs < MIN_PLAYED_BETWEEN_STALLS_MS) return false
        playedSinceLastReportMs = 0L
        return true
    }

    private companion object {
        const val MIN_PLAYED_BETWEEN_STALLS_MS = 5_000L
    }
}
