package dev.aaa1115910.bv.player.util

data class TvDanmakuCapabilities(
    val useSurfaceViewForVod: Boolean,
    val danmakuMaskSupported: Boolean,
)

enum class DanmakuMaskSelectionDecision {
    Apply,
    Unsupported,
}

object TvDanmakuCompatibilityPolicy {
    const val MODERN_DANMAKU_MIN_SDK = 26

    fun resolve(
        sdkInt: Int,
        isTvDevice: Boolean,
    ): TvDanmakuCapabilities {
        val legacyTvDevice = isTvDevice && sdkInt < MODERN_DANMAKU_MIN_SDK
        return TvDanmakuCapabilities(
            useSurfaceViewForVod = !legacyTvDevice,
            danmakuMaskSupported = !legacyTvDevice,
        )
    }

    fun resolveMaskSelection(
        danmakuMaskSupported: Boolean,
        requestedEnabled: Boolean,
    ): DanmakuMaskSelectionDecision {
        return if (!danmakuMaskSupported && requestedEnabled) {
            DanmakuMaskSelectionDecision.Unsupported
        } else {
            DanmakuMaskSelectionDecision.Apply
        }
    }
}
