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

        assertEquals(
            VideoCodec.AVC,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.AV1, VideoCodec.AVC, VideoCodec.HEVC)
            )
        )

        assertEquals(
            VideoCodec.AV1,
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = null,
                currentCodec = VideoCodec.HEVC,
                defaultCodec = VideoCodec.HEVC,
                selectedQuality = Resolution.R1080P,
                availableCodecs = listOf(VideoCodec.DVH1, VideoCodec.HEVC, VideoCodec.AV1)
            )
        )

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
    }

    @Test
    fun tvCodecPriorityPrefersDvh1WhenQualityIsDolbyVision() {
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
