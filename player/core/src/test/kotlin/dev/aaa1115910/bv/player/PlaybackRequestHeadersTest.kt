package dev.aaa1115910.bv.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackRequestHeadersTest {
    private val options = VideoPlayerOptions(referer = "https://www.bilibili.com")

    @Test
    fun omitsRefererForAndroidHdAppUrl() {
        val url = "https://upos.example.com/video.m4s?platform=android_hd&deadline=1"

        assertNull(options.playbackRefererFor(url))
    }

    @Test
    fun omitsRefererForEncodedAndroidAppPlatform() {
        val url = "https://upos.example.com/video.m4s?platform=android%5Ftv&deadline=1"

        assertNull(options.playbackRefererFor(url))
    }

    @Test
    fun omitsRefererForProxiedAndroidAppUrl() {
        val url = "https://proxy.example.com?url=https%3A%2F%2Fupos.example.com%2Fvideo.m4s%3Fplatform%3Dandroid_hd%26deadline%3D1"

        assertNull(options.playbackRefererFor(url))
    }

    @Test
    fun keepsRefererForWebUrl() {
        val url = "https://upos.example.com/video.m4s?platform=pc&deadline=1"

        assertEquals("https://www.bilibili.com", options.playbackRefererFor(url))
    }

    @Test
    fun keepsRefererWhenPlatformIsAbsent() {
        val url = "https://upos.example.com/video.m4s?deadline=1"

        assertEquals("https://www.bilibili.com", options.playbackRefererFor(url))
    }

    @Test
    fun omitsRefererWhenEitherMergedStreamUsesAppUrl() {
        val webVideo = "https://upos.example.com/video.m4s?platform=pc"
        val appAudio = "https://upos.example.com/audio.m4s?platform=android"

        assertNull(options.playbackRefererFor(webVideo, appAudio))
    }
}
