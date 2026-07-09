package dev.aaa1115910.bv.util

import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec

object PlaybackPreferenceSelector {
    /**
     * 用户可配置的 H.265 变体优先级（不含 DVH1）。
     * DVH1 仅在画质为杜比视界时启用，且固定为最优选。
     */
    val h265Codecs: List<VideoCodec> = listOf(
        VideoCodec.HVC1,
        VideoCodec.HEVC,
    )

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
        h265CodecPriority: List<VideoCodec> = emptyList(),
    ): VideoCodec {
        val preferredCodecs = (
            listOfNotNull(requestedCodec) +
                resolveTvVideoCodecPriority(defaultCodec, selectedQuality, h265CodecPriority) +
                listOf(currentCodec, defaultCodec)
            ).distinct()

        // 非杜比视界不自动选用 DVH1；仅手动指定时保留
        val selectableCodecs = if (selectedQuality == Resolution.RDolby) {
            availableCodecs
        } else {
            availableCodecs.filter { it != VideoCodec.DVH1 || it == requestedCodec }
        }

        return preferredCodecs.firstOrNull { it in selectableCodecs }
            ?: selectableCodecs.firstOrNull()
            ?: availableCodecs.firstOrNull()
            ?: VideoCodec.AVC
    }

    /**
     * 解析 TV 端完整编码优先级。
     * 仅当默认编码为 H.265 时，[h265CodecPriority] 才会参与排序。
     *
     * - 默认 H.265：先按配置完整尝试 hvc1/hev1，全部不可用后再降 AVC/AV1；不含 DVH1
     * - 杜比视界：DVH1 固定最优选且唯一启用时机，再试 hvc1/hev1，最后才降 AVC/AV1
     */
    fun resolveTvVideoCodecPriority(
        defaultCodec: VideoCodec,
        selectedQuality: Resolution,
        h265CodecPriority: List<VideoCodec> = emptyList(),
    ): List<VideoCodec> {
        val h265 = normalizeH265CodecPriority(h265CodecPriority)
        val nonH265Fallback = listOf(VideoCodec.AVC, VideoCodec.AV1)

        return when {
            // 杜比视界：仅此时启用 DVH1，且固定最优选
            selectedQuality == Resolution.RDolby -> {
                listOf(VideoCodec.DVH1) + h265 + nonH265Fallback
            }

            defaultCodec == VideoCodec.AVC -> listOf(
                VideoCodec.AVC,
                VideoCodec.HVC1,
                VideoCodec.AV1,
                VideoCodec.HEVC,
            )

            defaultCodec == VideoCodec.AV1 -> listOf(
                VideoCodec.AV1,
                VideoCodec.HVC1,
                VideoCodec.AVC,
                VideoCodec.HEVC,
            )

            // 默认 H.265：先完整遍历可配置的 H.265 变体，再降 AVC/AV1（不含 DVH1）
            else -> h265 + nonH265Fallback
        }
    }

    fun defaultH265CodecPriority(): List<VideoCodec> = h265Codecs

    fun normalizeH265CodecPriority(priority: List<VideoCodec>): List<VideoCodec> {
        val ordered = LinkedHashSet<VideoCodec>()
        priority.filter { it in h265Codecs }.forEach { ordered.add(it) }
        h265Codecs.forEach { ordered.add(it) }
        return ordered.toList()
    }

    fun encodeH265CodecPriority(priority: List<VideoCodec>): String =
        normalizeH265CodecPriority(priority).joinToString(",") { it.ordinal.toString() }

    fun parseH265CodecPriority(raw: String?): List<VideoCodec> {
        if (raw.isNullOrBlank()) return defaultH265CodecPriority()

        val parsed = raw.split(",")
            .mapNotNull { part -> part.trim().toIntOrNull() }
            .mapNotNull { ordinal -> VideoCodec.entries.getOrNull(ordinal) }

        return normalizeH265CodecPriority(parsed)
    }

    /**
     * 按 TV 编码优先级对可用编码列表排序。
     * 非杜比视界时 DVH1 沉底，不参与优先展示。
     */
    fun orderAvailableVideoCodecs(
        availableCodecs: List<VideoCodec>,
        defaultCodec: VideoCodec,
        selectedQuality: Resolution,
        h265CodecPriority: List<VideoCodec> = emptyList(),
    ): List<VideoCodec> {
        val priority = resolveTvVideoCodecPriority(defaultCodec, selectedQuality, h265CodecPriority)
        val remaining = availableCodecs.toMutableList()
        val ordered = mutableListOf<VideoCodec>()
        priority.forEach { codec ->
            if (remaining.remove(codec)) ordered += codec
        }
        // 非杜比：剩余项中先放非 DVH1，DVH1 最后
        if (selectedQuality != Resolution.RDolby) {
            val dvh1 = remaining.filter { it == VideoCodec.DVH1 }
            remaining.removeAll(dvh1.toSet())
            ordered += remaining
            ordered += dvh1
        } else {
            ordered += remaining
        }
        return ordered
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
