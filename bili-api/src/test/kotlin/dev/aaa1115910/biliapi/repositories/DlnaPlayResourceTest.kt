package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.http.entity.video.Durl
import dev.aaa1115910.biliapi.http.entity.video.PlayUrlData
import kotlin.test.Test
import kotlin.test.assertEquals

class DlnaPlayResourceTest {
    @Test
    fun `preserves mp4 mime type`() {
        val resource = playUrlData(
            format = "mp4",
            urls = listOf("https://example.com/video?id=1"),
        ).toDlnaPlayResource()

        assertEquals("https://example.com/video?id=1", resource.url)
        assertEquals("video/mp4", resource.mimeType)
    }

    @Test
    fun `preserves flv mime type`() {
        val resource = playUrlData(
            format = "flv720",
            urls = listOf("https://example.com/video.flv"),
        ).toDlnaPlayResource()

        assertEquals("video/x-flv", resource.mimeType)
    }

    @Test
    fun `uses first tv cast entry like PiliPlus`() {
        val resource = playUrlData(
            format = "mp4",
            urls = listOf(
                "https://example.com/cast-source.mp4",
                "https://example.com/unused-entry.mp4",
            ),
        ).toDlnaPlayResource()

        assertEquals("https://example.com/cast-source.mp4", resource.url)
    }

    @Test
    fun `falls back to a valid backup url on first tv cast entry`() {
        val resource = PlayUrlData(
            format = "mp4",
            durl = listOf(
                Durl(
                    order = 1,
                    length = 1_000,
                    size = 1,
                    ahead = "",
                    vhead = "",
                    url = "",
                    backupUrl = listOf("https://example.com/backup.mp4"),
                ),
            ),
        ).toDlnaPlayResource()

        assertEquals("https://example.com/backup.mp4", resource.url)
    }

    private fun playUrlData(format: String, urls: List<String>): PlayUrlData =
        PlayUrlData(
            format = format,
            durl = urls.mapIndexed { index, url ->
                Durl(
                    order = index + 1,
                    length = 1_000,
                    size = 1,
                    ahead = "",
                    vhead = "",
                    url = url,
                )
            },
        )
}
