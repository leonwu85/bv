package dev.aaa1115910.bv.util

import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackPreferenceSelectorTest {
    @Test
    fun selectsCellularDefaultsOnlyOnCellularNetwork() {
        assertEquals(
            Resolution.R4K,
            PlaybackPreferenceSelector.selectQuality(
                isCellular = false,
                defaultQuality = Resolution.R4K,
                cellularQuality = Resolution.R720P
            )
        )
        assertEquals(
            Resolution.R720P,
            PlaybackPreferenceSelector.selectQuality(
                isCellular = true,
                defaultQuality = Resolution.R4K,
                cellularQuality = Resolution.R720P
            )
        )

        assertEquals(
            Audio.A192K,
            PlaybackPreferenceSelector.selectAudio(
                isCellular = false,
                defaultAudio = Audio.A192K,
                cellularAudio = Audio.A64K
            )
        )
        assertEquals(
            Audio.A64K,
            PlaybackPreferenceSelector.selectAudio(
                isCellular = true,
                defaultAudio = Audio.A192K,
                cellularAudio = Audio.A64K
            )
        )

        assertEquals(
            20000,
            PlaybackPreferenceSelector.selectLiveQuality(
                isCellular = false,
                defaultLiveQn = 20000,
                cellularLiveQn = 10000
            )
        )
        assertEquals(
            10000,
            PlaybackPreferenceSelector.selectLiveQuality(
                isCellular = true,
                defaultLiveQn = 20000,
                cellularLiveQn = 10000
            )
        )
    }

    @Test
    fun selectsSecondCodecBeforeExistingFallback() {
        assertEquals(
            VideoCodec.AV1,
            PlaybackPreferenceSelector.selectVideoCodec(
                requestedCodec = VideoCodec.HEVC,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                secondCodec = VideoCodec.AV1,
                availableCodecs = listOf(VideoCodec.AV1, VideoCodec.AVC)
            )
        )

        assertEquals(
            VideoCodec.AVC,
            PlaybackPreferenceSelector.selectVideoCodec(
                requestedCodec = VideoCodec.HEVC,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                secondCodec = VideoCodec.AV1,
                availableCodecs = listOf(VideoCodec.AVC)
            )
        )
    }

    @Test
    fun selectsSpecificH265CodecForGenericH265Preference() {
        assertEquals(
            VideoCodec.HVC1,
            PlaybackPreferenceSelector.selectVideoCodec(
                requestedCodec = VideoCodec.HEVC,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                secondCodec = VideoCodec.AVC,
                availableCodecs = listOf(VideoCodec.HVC1, VideoCodec.AVC)
            )
        )
    }

    @Test
    fun mapsCodecStringsToSpecificVideoCodec() {
        assertEquals(VideoCodec.AVC, VideoCodec.fromCodecString("avc1.640028"))
        assertEquals(VideoCodec.HEVC, VideoCodec.fromCodecString("hev1.1.6.L153.90"))
        assertEquals(VideoCodec.HVC1, VideoCodec.fromCodecString("hvc1.1.6.L153.90"))
        assertEquals(VideoCodec.DVH1, VideoCodec.fromCodecString("dvh1.05.06"))
        assertEquals(VideoCodec.DVH1, VideoCodec.fromCodecString("dvhe.05.06"))
        assertEquals(VideoCodec.AV1, VideoCodec.fromCodecString("av01.0.08M.08"))
    }

    @Test
    fun tvCodecPriorityPrefersHvc1WhenDefaultIsH265() {
        assertEquals(
            VideoCodec.HVC1,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(
                    VideoCodec.HEVC,
                    VideoCodec.HVC1,
                    VideoCodec.AVC,
                    VideoCodec.AV1,
                    VideoCodec.DVH1
                )
            )
        )

        // HVC1 不可用时，应继续在 H.265 中选择 HEVC，而不是先降到 AVC
        assertEquals(
            VideoCodec.HEVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.AV1, VideoCodec.AVC, VideoCodec.HEVC)
            )
        )

        // 有 HEVC 时，不应因 AV1 可用就跳过 H.265；非杜比不选 DVH1
        assertEquals(
            VideoCodec.HEVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.DVH1, VideoCodec.HEVC, VideoCodec.AV1)
            )
        )

        // 非杜比：即使只有 DVH1 + HEVC，也选 HEVC；若仅 DVH1+AVC 则降 AVC 而非 DVH1
        assertEquals(
            VideoCodec.HEVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.DVH1, VideoCodec.HEVC)
            )
        )
        assertEquals(
            VideoCodec.AVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.DVH1, VideoCodec.AVC)
            )
        )

        // 全部 H.265（hvc1/hev1）不可用后，才降到 AVC
        assertEquals(
            VideoCodec.AVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.AV1, VideoCodec.AVC)
            )
        )
    }

    @Test
    fun tvCodecPriorityEnablesDvh1OnlyForDolbyVision() {
        // 杜比视界：DVH1 最优选
        assertEquals(
            VideoCodec.DVH1,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.RDolby,
                availableCodecs = listOf(VideoCodec.HVC1, VideoCodec.DVH1, VideoCodec.AVC)
            )
        )

        // 杜比无 DVH1 时，仍应优先其余 H.265，而不是先降到 AVC/AV1
        assertEquals(
            VideoCodec.HVC1,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.AVC,
                selectedQuality = Resolution.RDolby,
                availableCodecs = listOf(VideoCodec.AV1, VideoCodec.HVC1, VideoCodec.HEVC)
            )
        )

        assertEquals(
            VideoCodec.HEVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.RDolby,
                availableCodecs = listOf(VideoCodec.AVC, VideoCodec.AV1, VideoCodec.HEVC)
            )
        )

        // 非杜比：即使 available 含 DVH1 且排在最前，也不自动选用
        assertEquals(
            VideoCodec.HVC1,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.DVH1, VideoCodec.HVC1, VideoCodec.AVC)
            )
        )
    }

    @Test
    fun tvCodecPriorityPrefersAvcWhenDefaultIsH264() {
        assertEquals(
            VideoCodec.AVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.AVC,
                defaultCodec = VideoCodec.AVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.HVC1, VideoCodec.AVC)
            )
        )

        assertEquals(
            VideoCodec.HVC1,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.AVC,
                defaultCodec = VideoCodec.AVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.AV1, VideoCodec.HVC1, VideoCodec.HEVC)
            )
        )
    }

    @Test
    fun tvCodecPriorityHonorsRequestedCodecFirst() {
        assertEquals(
            VideoCodec.AV1,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = VideoCodec.AV1,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.RDolby,
                availableCodecs = listOf(VideoCodec.DVH1, VideoCodec.AV1)
            )
        )
    }

    @Test
    fun tvH265PriorityOnlyAppliesWhenDefaultIsH265() {
        // 默认 H.265 且配置 HEVC 优先时，应选 HEVC 而不是 HVC1
        assertEquals(
            VideoCodec.HEVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.HVC1, VideoCodec.HEVC, VideoCodec.AVC),
                h265CodecPriority = listOf(VideoCodec.HEVC, VideoCodec.HVC1, VideoCodec.DVH1)
            )
        )

        // 默认不是 H.265 时，H.265 优先级配置不改变 AVC 优先行为
        assertEquals(
            VideoCodec.AVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.AVC,
                defaultCodec = VideoCodec.AVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.HVC1, VideoCodec.AVC, VideoCodec.HEVC),
                h265CodecPriority = listOf(VideoCodec.HEVC, VideoCodec.HVC1, VideoCodec.DVH1)
            )
        )
    }

    @Test
    fun ordersAvailableCodecsByResolvedTvPriority() {
        // H.265 全部排在 AVC 前：HEVC > HVC1 > AVC
        assertEquals(
            listOf(VideoCodec.HEVC, VideoCodec.HVC1, VideoCodec.AVC),
            PlaybackPreferenceSelector.orderAvailableVideoCodecs(
                availableCodecs = listOf(VideoCodec.HVC1, VideoCodec.AVC, VideoCodec.HEVC),
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                h265CodecPriority = listOf(VideoCodec.HEVC, VideoCodec.HVC1, VideoCodec.DVH1)
            )
        )
    }

    @Test
    fun resolveTvH265PriorityPlacesAllH265BeforeAvcAndDvh1OnlyForDolby() {
        // 非杜比：仅 hvc1/hev1，再 AVC/AV1，不含 DVH1
        assertEquals(
            listOf(
                VideoCodec.HVC1,
                VideoCodec.HEVC,
                VideoCodec.AVC,
                VideoCodec.AV1
            ),
            PlaybackPreferenceSelector.resolveTvVideoCodecPriority(
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                h265CodecPriority = emptyList()
            )
        )

        assertEquals(
            listOf(
                VideoCodec.HEVC,
                VideoCodec.HVC1,
                VideoCodec.AVC,
                VideoCodec.AV1
            ),
            PlaybackPreferenceSelector.resolveTvVideoCodecPriority(
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                h265CodecPriority = listOf(VideoCodec.HEVC, VideoCodec.DVH1, VideoCodec.HVC1)
            )
        )

        // 杜比：DVH1 固定最优选，再 hvc1/hev1，最后 AVC/AV1
        assertEquals(
            listOf(
                VideoCodec.DVH1,
                VideoCodec.HVC1,
                VideoCodec.HEVC,
                VideoCodec.AVC,
                VideoCodec.AV1
            ),
            PlaybackPreferenceSelector.resolveTvVideoCodecPriority(
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.RDolby,
                h265CodecPriority = listOf(VideoCodec.HVC1, VideoCodec.HEVC, VideoCodec.DVH1)
            )
        )
    }

    @Test
    fun parsesAndEncodesH265CodecPriority() {
        // DVH1 不参与用户可配置优先级，会被过滤
        val encoded = PlaybackPreferenceSelector.encodeH265CodecPriority(
            listOf(VideoCodec.HEVC, VideoCodec.AVC, VideoCodec.DVH1)
        )
        assertEquals(
            listOf(VideoCodec.HEVC, VideoCodec.HVC1),
            PlaybackPreferenceSelector.parseH265CodecPriority(encoded)
        )
        assertEquals(
            PlaybackPreferenceSelector.defaultH265CodecPriority(),
            PlaybackPreferenceSelector.parseH265CodecPriority("")
        )
    }

    @Test
    fun normalizesLiveCdnHostAndBuildsOverrideUrl() {
        assertEquals("", PlaybackPreferenceSelector.normalizeLiveCdnHost("   "))
        assertEquals(
            "https://live.example.com",
            PlaybackPreferenceSelector.normalizeLiveCdnHost("live.example.com/")
        )

        assertEquals(
            "https://default.example.com/live/stream.m3u8?expires=1",
            LiveStreamUrlFetcher.buildLiveUrl(
                host = "https://default.example.com",
                baseUrl = "/live/stream.m3u8",
                extra = "?expires=1",
                liveCdnHost = ""
            )
        )
        assertEquals(
            "https://live.example.com/live/stream.m3u8?expires=1",
            LiveStreamUrlFetcher.buildLiveUrl(
                host = "https://default.example.com",
                baseUrl = "live/stream.m3u8",
                extra = "?expires=1",
                liveCdnHost = "https://live.example.com/"
            )
        )
    }
}
