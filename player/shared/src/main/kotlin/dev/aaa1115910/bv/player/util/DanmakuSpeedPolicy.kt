package dev.aaa1115910.bv.player.util

import com.kuaishou.akdanmaku.DanmakuConfig
import dev.aaa1115910.bv.player.entity.DanmakuSpeedMode
import kotlin.math.roundToLong

data class DanmakuSpeedTiming(
    val timerSpeed: Float,
    val presentationSpeed: Float,
    val durationScale: Float
)

object DanmakuSpeedPolicy {
    const val MIN_PRESENTATION_SPEED = 0.5f
    const val MAX_PRESENTATION_SPEED = 2f

    fun resolve(
        playbackSpeed: Float,
        mode: DanmakuSpeedMode,
        customPresentationSpeed: Float
    ): DanmakuSpeedTiming {
        val timerSpeed = sanitizePlaybackSpeed(playbackSpeed)
        val presentationSpeed = when (mode) {
            DanmakuSpeedMode.FollowVideo -> timerSpeed
            DanmakuSpeedMode.ReadingFirst -> 1f
            DanmakuSpeedMode.Custom -> sanitizePresentationSpeed(customPresentationSpeed)
        }

        return DanmakuSpeedTiming(
            timerSpeed = timerSpeed,
            presentationSpeed = presentationSpeed,
            durationScale = timerSpeed / presentationSpeed
        )
    }

    fun sanitizePresentationSpeed(speed: Float): Float {
        return speed.takeIf { it.isFinite() && it > 0f }
            ?.coerceIn(MIN_PRESENTATION_SPEED, MAX_PRESENTATION_SPEED)
            ?: 1f
    }

    fun scaleDuration(durationMs: Long, durationScale: Float): Long {
        val safeScale = durationScale.takeIf { it.isFinite() && it > 0f } ?: 1f
        return (durationMs.coerceAtLeast(1L).toDouble() * safeScale.toDouble())
            .roundToLong()
            .coerceAtLeast(1L)
    }

    fun applyDurationScale(
        config: DanmakuConfig,
        timing: DanmakuSpeedTiming,
        baseDurationMs: Long,
        baseRollingDurationMs: Long
    ): DanmakuConfig {
        val nextConfig = config.copy(
            durationMs = scaleDuration(baseDurationMs, timing.durationScale),
            rollingDurationMs = scaleDuration(baseRollingDurationMs, timing.durationScale)
        )
        nextConfig.updateLayout()
        nextConfig.updateRetainer()
        nextConfig.updateVisibility()
        return nextConfig
    }

    private fun sanitizePlaybackSpeed(speed: Float): Float {
        return speed.takeIf { it.isFinite() && it > 0f } ?: 1f
    }
}
