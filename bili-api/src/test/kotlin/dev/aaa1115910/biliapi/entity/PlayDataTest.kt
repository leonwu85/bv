package dev.aaa1115910.biliapi.entity

import dev.aaa1115910.biliapi.http.entity.video.Durl
import dev.aaa1115910.biliapi.http.entity.video.PlayUrlData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayDataTest {
    @Test
    fun `legacy durl is exposed as a playable muxed stream`() {
        val playData = PlayData.fromPlayUrlData(
            PlayUrlData(
                quality = 16,
                videoCodecId = 7,
                timeLength = 12_345,
                durl = listOf(
                    Durl(
                        order = 1,
                        length = 12_345,
                        size = 1_024,
                        ahead = "",
                        vhead = "",
                        url = "https://example.com/video.mp4",
                        backupUrl = listOf("https://backup.example.com/video.mp4")
                    )
                )
            )
        )

        assertTrue(playData.hasPlayableVodStreams())
        assertTrue(playData.hasMuxedVideo())
        assertEquals(0, playData.playableAudioCount())
        assertEquals(1, playData.dashVideos.size)
        assertEquals(16, playData.dashVideos.single().quality)
        assertEquals("avc1", playData.dashVideos.single().codecs)
        assertEquals("https://example.com/video.mp4", playData.dashVideos.single().baseUrl)
        assertEquals(
            listOf("https://backup.example.com/video.mp4"),
            playData.dashVideos.single().backUrl
        )
    }

    @Test
    fun `empty play url response remains unplayable`() {
        val playData = PlayData.fromPlayUrlData(PlayUrlData())

        assertFalse(playData.hasPlayableVodStreams())
        assertFalse(playData.hasMuxedVideo())
        assertTrue(playData.dashVideos.isEmpty())
        assertTrue(playData.dashAudios.isEmpty())
    }
}
