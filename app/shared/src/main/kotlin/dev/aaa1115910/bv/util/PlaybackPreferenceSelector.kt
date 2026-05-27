package dev.aaa1115910.bv.util

import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec

object PlaybackPreferenceSelector {
    fun selectQuality(
        isCellular: Boolean,
        defaultQuality: Resolution,
        cellularQuality: Resolution,
    ) = if (isCellular) cellularQuality else defaultQuality

    fun selectAudio(
        isCellular: Boolean,
        defaultAudio: Audio,
        cellularAudio: Audio,
    ) = if (isCellular) cellularAudio else defaultAudio

    fun selectLiveQuality(
        isCellular: Boolean,
        defaultLiveQn: Int,
        cellularLiveQn: Int,
    ) = if (isCellular) cellularLiveQn else defaultLiveQn

    fun selectVideoCodec(
        requestedCodec: VideoCodec?,
        currentCodec: VideoCodec,
        defaultCodec: VideoCodec,
        secondCodec: VideoCodec,
        availableCodecs: List<VideoCodec>,
    ): VideoCodec {
        val preferredCodecs = listOfNotNull(
            requestedCodec,
            currentCodec,
            defaultCodec,
            secondCodec,
        ).distinct()

        return preferredCodecs.firstOrNull { it in availableCodecs }
            ?: availableCodecs.minByOrNull { it.ordinal }
            ?: VideoCodec.AVC
    }

    fun normalizeLiveCdnHost(host: String): String {
        val trimmed = host.trim()
        if (trimmed.isEmpty()) return ""

        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return withScheme.trimEnd('/')
    }
}
