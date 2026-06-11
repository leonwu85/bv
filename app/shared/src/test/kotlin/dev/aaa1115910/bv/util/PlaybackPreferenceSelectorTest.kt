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
    fun mapsHevcCodecStringAliasesToH265Codec() {
        assertEquals(VideoCodec.HEVC, VideoCodec.fromCodecString("hev1.1.6.L153.90"))
        assertEquals(VideoCodec.HEVC, VideoCodec.fromCodecString("hvc1.1.6.L153.90"))
        assertEquals(VideoCodec.HEVC, VideoCodec.fromCodecString("dvh1.05.06"))
        assertEquals(VideoCodec.HEVC, VideoCodec.fromCodecString("dvhe.05.06"))
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
