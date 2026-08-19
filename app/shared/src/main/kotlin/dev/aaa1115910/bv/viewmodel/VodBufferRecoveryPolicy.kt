package dev.aaa1115910.bv.viewmodel

import dev.aaa1115910.bv.entity.CdnService
import dev.aaa1115910.bv.player.entity.Resolution
import java.util.ArrayDeque

sealed interface VodBufferRecoveryPrompt {
    val resumePositionMs: Long

    data class SwitchCdn(
        override val resumePositionMs: Long,
        val fromService: CdnService,
        val toService: CdnService,
    ) : VodBufferRecoveryPrompt

    data class LowerResolution(
        override val resumePositionMs: Long,
        val fromResolution: Resolution,
        val toResolution: Resolution,
    ) : VodBufferRecoveryPrompt
}

internal enum class VodBufferRecoveryStage {
    MonitoringPrimary,
    CdnPrompt,
    SwitchingCdn,
    MonitoringAfterCdnSwitch,
    ResolutionPrompt,
    SwitchingResolution,
    Suppressed,
}

internal enum class VodBufferRecoveryDecision {
    None,
    SuggestCdnSwitch,
    SuggestResolutionDowngrade,
}

internal class VodBufferRecoveryPolicy(
    private val bufferingThreshold: Int = DEFAULT_BUFFERING_THRESHOLD,
    private val bufferingWindowMs: Long = DEFAULT_BUFFERING_WINDOW_MS,
) {
    private val recentBufferingEvents = ArrayDeque<Long>()

    var stage: VodBufferRecoveryStage = VodBufferRecoveryStage.MonitoringPrimary
        private set

    fun onRebuffering(nowMs: Long): VodBufferRecoveryDecision = when (stage) {
        VodBufferRecoveryStage.MonitoringPrimary -> {
            val cutoff = nowMs - bufferingWindowMs
            while (recentBufferingEvents.isNotEmpty() && recentBufferingEvents.first() < cutoff) {
                recentBufferingEvents.removeFirst()
            }
            recentBufferingEvents.addLast(nowMs)
            if (recentBufferingEvents.size >= bufferingThreshold) {
                recentBufferingEvents.clear()
                stage = VodBufferRecoveryStage.CdnPrompt
                VodBufferRecoveryDecision.SuggestCdnSwitch
            } else {
                VodBufferRecoveryDecision.None
            }
        }

        VodBufferRecoveryStage.MonitoringAfterCdnSwitch -> {
            stage = VodBufferRecoveryStage.ResolutionPrompt
            VodBufferRecoveryDecision.SuggestResolutionDowngrade
        }

        else -> VodBufferRecoveryDecision.None
    }

    fun startCdnSwitch(): Boolean {
        if (stage != VodBufferRecoveryStage.CdnPrompt) return false
        stage = VodBufferRecoveryStage.SwitchingCdn
        return true
    }

    fun finishCdnSwitch(success: Boolean) {
        if (stage != VodBufferRecoveryStage.SwitchingCdn) return
        stage = if (success) {
            VodBufferRecoveryStage.MonitoringAfterCdnSwitch
        } else {
            VodBufferRecoveryStage.Suppressed
        }
    }

    fun startResolutionDowngrade(): Boolean {
        if (stage != VodBufferRecoveryStage.ResolutionPrompt) return false
        stage = VodBufferRecoveryStage.SwitchingResolution
        return true
    }

    fun finishResolutionDowngrade() {
        if (stage == VodBufferRecoveryStage.SwitchingResolution) {
            stage = VodBufferRecoveryStage.Suppressed
        }
    }

    fun suppress() {
        recentBufferingEvents.clear()
        stage = VodBufferRecoveryStage.Suppressed
    }

    fun reset() {
        recentBufferingEvents.clear()
        stage = VodBufferRecoveryStage.MonitoringPrimary
    }

    companion object {
        const val DEFAULT_BUFFERING_THRESHOLD = 3
        const val DEFAULT_BUFFERING_WINDOW_MS = 5 * 60_000L
    }
}

internal fun nextVodRecoveryCdnService(current: CdnService): CdnService = when (current) {
    CdnService.BaseUrl -> CdnService.BackupUrl
    CdnService.BackupUrl -> CdnService.BaseUrl
    else -> CdnService.BackupUrl
}

internal fun nextLowerVodResolution(
    current: Resolution,
    available: Iterable<Resolution>,
): Resolution? = available
    .distinct()
    .filter { it.code < current.code }
    .maxByOrNull { it.code }
