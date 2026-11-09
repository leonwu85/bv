package dev.aaa1115910.bv.viewmodel

import dev.aaa1115910.bv.player.entity.VideoPlayerStateData
import dev.aaa1115910.bv.player.util.VodRebufferingTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VodRebufferingTest {
    @Test
    fun resumeBurstDoesNotTriggerCdnPromptButDistinctStallsDo() {
        val tracker = VodRebufferingTracker()
        val policy = VodBufferRecoveryPolicy()
        var nowMs = 0L
        var positionMs = 0L
        fun bufferAfter(playedMs: Long): VodBufferRecoveryDecision {
            tracker.onPlay(positionMs)
            positionMs += playedMs
            nowMs += playedMs
            return if (tracker.onBuffering(positionMs)) policy.onRebuffering(nowMs)
            else VodBufferRecoveryDecision.None
        }

        tracker.onPlay(positionMs)
        positionMs += 60_000
        tracker.onPause()
        nowMs += 360_000
        assertFalse(tracker.onBuffering(positionMs))
        repeat(10) { assertEquals(VodBufferRecoveryDecision.None, bufferAfter(50)) }
        repeat(2) { assertEquals(VodBufferRecoveryDecision.None, bufferAfter(5_000)) }
        assertEquals(VodBufferRecoveryDecision.SuggestCdnSwitch, bufferAfter(5_000))
    }

    @Test
    fun duplicateCallbacksCountOnlyOnceAndPauseClearsPartialProgress() {
        val tracker = VodRebufferingTracker()
        tracker.onPlay(0)
        tracker.onPlay(3_000)
        assertTrue(tracker.onBuffering(5_000))
        assertFalse(tracker.onBuffering(5_000))
        tracker.onPlay(5_000)
        assertFalse(tracker.onBuffering(8_000))
        tracker.onPause()
        tracker.onPlay(8_000)
        assertFalse(tracker.onBuffering(10_000))
        tracker.onPlay(10_000)
        assertTrue(tracker.onBuffering(13_000))
    }

    @Test
    fun remoteCanPauseWhileVideoClockIsStoppedForBuffering() {
        assertTrue(VideoPlayerStateData(isPlaying = true).canPause)
        assertTrue(VideoPlayerStateData(isBuffering = true).canPause)
        assertFalse(VideoPlayerStateData().canPause)
    }
}
