package dev.aaa1115910.bv.player

import dev.aaa1115910.bv.player.entity.DanmakuSpeedMode
import dev.aaa1115910.bv.player.util.DanmakuSpeedPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class DanmakuSpeedPolicyTest {
    @Test
    fun followVideoKeepsDurationScaleAtOne() {
        listOf(1f, 1.5f, 2f).forEach { playbackSpeed ->
            val timing = DanmakuSpeedPolicy.resolve(
                playbackSpeed = playbackSpeed,
                mode = DanmakuSpeedMode.FollowVideo,
                customPresentationSpeed = 1f
            )

            assertEquals(playbackSpeed, timing.timerSpeed)
            assertEquals(playbackSpeed, timing.presentationSpeed)
            assertEquals(1f, timing.durationScale)
        }
    }

    @Test
    fun readingFirstScalesDurationByPlaybackSpeed() {
        mapOf(
            1f to 1f,
            1.5f to 1.5f,
            2f to 2f
        ).forEach { (playbackSpeed, expectedScale) ->
            val timing = DanmakuSpeedPolicy.resolve(
                playbackSpeed = playbackSpeed,
                mode = DanmakuSpeedMode.ReadingFirst,
                customPresentationSpeed = 1f
            )

            assertEquals(playbackSpeed, timing.timerSpeed)
            assertEquals(1f, timing.presentationSpeed)
            assertEquals(expectedScale, timing.durationScale)
        }
    }

    @Test
    fun customModeUsesPlaybackDividedByPresentationSpeed() {
        val timing = DanmakuSpeedPolicy.resolve(
            playbackSpeed = 2f,
            mode = DanmakuSpeedMode.Custom,
            customPresentationSpeed = 0.5f
        )

        assertEquals(2f, timing.timerSpeed)
        assertEquals(0.5f, timing.presentationSpeed)
        assertEquals(4f, timing.durationScale)
    }

    @Test
    fun customModeClampsPresentationSpeedToSupportedRange() {
        val slowTiming = DanmakuSpeedPolicy.resolve(
            playbackSpeed = 1f,
            mode = DanmakuSpeedMode.Custom,
            customPresentationSpeed = 0.1f
        )
        val fastTiming = DanmakuSpeedPolicy.resolve(
            playbackSpeed = 1f,
            mode = DanmakuSpeedMode.Custom,
            customPresentationSpeed = 3f
        )

        assertEquals(0.5f, slowTiming.presentationSpeed)
        assertEquals(2f, slowTiming.durationScale)
        assertEquals(2f, fastTiming.presentationSpeed)
        assertEquals(0.5f, fastTiming.durationScale)
    }

    @Test
    fun scaleDurationRoundsAndKeepsPositiveDuration() {
        assertEquals(1500L, DanmakuSpeedPolicy.scaleDuration(1000L, 1.5f))
        assertEquals(1L, DanmakuSpeedPolicy.scaleDuration(0L, 0.5f))
    }
}
