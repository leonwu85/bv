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
        )
            .distinct()
            .flatMap { it.compatibleCodecPreferences() }
            .distinct()

        return preferredCodecs.firstOrNull { it in availableCodecs }
            ?: availableCodecs.minByOrNull { it.ordinal }
            ?: VideoCodec.AVC
    }

    fun selectTvVideoCodec(
        requestedCodec: VideoCodec?,
        currentCodec: VideoCodec,
        defaultCodec: VideoCodec,
        selectedQuality: Resolution,
        availableCodecs: List<VideoCodec>,
    ): VideoCodec {
        val preferredCodecs = (
            listOfNotNull(requestedCodec) +
                tvAutoCodecPriority(defaultCodec, selectedQuality) +
                listOf(currentCodec, defaultCodec)
            ).distinct()

        return preferredCodecs.firstOrNull { it in availableCodecs }
            ?: availableCodecs.firstOrNull()
            ?: VideoCodec.AVC
    }

    private fun tvAutoCodecPriority(
        defaultCodec: VideoCodec,
        selectedQuality: Resolution,
    ): List<VideoCodec> = when {
        selectedQuality == Resolution.RDolby -> listOf(
            VideoCodec.DVH1,
            VideoCodec.HVC1,
            VideoCodec.AVC,
            VideoCodec.AV1,
            VideoCodec.HEVC
        )

        defaultCodec == VideoCodec.AVC -> listOf(
            VideoCodec.AVC,
            VideoCodec.HVC1,
            VideoCodec.AV1,
            VideoCodec.HEVC,
            VideoCodec.DVH1
        )

        defaultCodec == VideoCodec.AV1 -> listOf(
            VideoCodec.AV1,
            VideoCodec.HVC1,
            VideoCodec.AVC,
            VideoCodec.HEVC,
            VideoCodec.DVH1
        )

        defaultCodec == VideoCodec.DVH1 -> listOf(
            VideoCodec.DVH1,
            VideoCodec.HVC1,
            VideoCodec.AVC,
            VideoCodec.AV1,
            VideoCodec.HEVC
        )

        else -> listOf(
            VideoCodec.HVC1,
            VideoCodec.AVC,
            VideoCodec.AV1,
            VideoCodec.HEVC,
            VideoCodec.DVH1
        )
    }

    private fun VideoCodec.compatibleCodecPreferences(): List<VideoCodec> = when (this) {
        VideoCodec.HEVC -> listOf(VideoCodec.HEVC, VideoCodec.HVC1, VideoCodec.DVH1)
        VideoCodec.HVC1 -> listOf(VideoCodec.HVC1, VideoCodec.HEVC)
        VideoCodec.DVH1 -> listOf(VideoCodec.DVH1, VideoCodec.HVC1, VideoCodec.HEVC)
        else -> listOf(this)
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
