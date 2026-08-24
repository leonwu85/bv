package dev.aaa1115910.bv.player.util

/**
 * Keeps buffering callbacks caused by an explicit seek out of rebuffering metrics.
 *
 * A seek is not considered recovered when the player merely reports [onSeekCompleted], because
 * some engines enter buffering immediately after that callback. Recovery is confirmed only after
 * playback advances far enough from the completed seek position.
 */
class SeekRebufferingGuard(
    private val stablePlaybackProgressMs: Long = DEFAULT_STABLE_PLAYBACK_PROGRESS_MS,
) {
    private var pendingSeekTargetPositionMs: Long? = null
    private var completedSeekPositionMs: Long? = null

    val shouldSuppressRebuffering: Boolean
        get() = pendingSeekTargetPositionMs != null || completedSeekPositionMs != null

    init {
        require(stablePlaybackProgressMs >= 0L) {
            "stablePlaybackProgressMs must not be negative"
        }
    }

    fun onSeekStarted(targetPositionMs: Long) {
        pendingSeekTargetPositionMs = targetPositionMs.coerceAtLeast(0L)
        completedSeekPositionMs = null
    }

    fun onSeekCompleted(positionMs: Long) {
        if (!shouldSuppressRebuffering) return

        pendingSeekTargetPositionMs = null
        completedSeekPositionMs = positionMs.coerceAtLeast(0L)
    }

    fun onStablePlaybackProgress(positionMs: Long) {
        val currentPositionMs = positionMs.coerceAtLeast(0L)
        val seekPositionMs = completedSeekPositionMs ?: run {
            val targetPositionMs = pendingSeekTargetPositionMs ?: return
            if (absoluteDifference(currentPositionMs, targetPositionMs) > SEEK_ARRIVAL_TOLERANCE_MS) {
                return
            }

            // Some engines omit onSeekCompleted when seeking to the current position. Treat the
            // first progress sample at the target as completion so suppression cannot stick.
            pendingSeekTargetPositionMs = null
            completedSeekPositionMs = currentPositionMs
            currentPositionMs
        }
        val playbackProgressMs = currentPositionMs - seekPositionMs
        if (playbackProgressMs >= stablePlaybackProgressMs) {
            completedSeekPositionMs = null
        }
    }

    fun reset() {
        pendingSeekTargetPositionMs = null
        completedSeekPositionMs = null
    }

    private fun absoluteDifference(first: Long, second: Long): Long =
        if (first >= second) first - second else second - first

    private companion object {
        const val DEFAULT_STABLE_PLAYBACK_PROGRESS_MS = 1_000L
        const val SEEK_ARRIVAL_TOLERANCE_MS = 250L
    }
}
