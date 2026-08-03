package dev.aaa1115910.biliapi.http.entity.web

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class NavResponseDataTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decode logged in identity and wbi keys`() {
        val data = json.decodeFromString<NavResponseData>(
            """
            {
              "isLogin": true,
              "mid": 123456,
              "uname": "Pili user",
              "face": "https://i0.hdslb.com/avatar.jpg",
              "wbi_img": {
                "img_url": "https://i0.hdslb.com/bfs/wbi/image-key.png",
                "sub_url": "https://i0.hdslb.com/bfs/wbi/sub-key.png"
              }
            }
            """.trimIndent()
        )

        assertEquals(true, data.isLogin)
        assertEquals(123456L, data.mid)
        assertEquals("Pili user", data.uname)
        assertEquals("https://i0.hdslb.com/avatar.jpg", data.face)
        assertEquals("image-key", data.wbiImg.getImgKey())
        assertEquals("sub-key", data.wbiImg.getSubKey())
    }

    @Test
    fun `identity fields remain compatible when absent`() {
        val data = json.decodeFromString<NavResponseData>(
            """
            {
              "isLogin": false,
              "wbi_img": {
                "img_url": "https://i0.hdslb.com/bfs/wbi/image-key.png",
                "sub_url": "https://i0.hdslb.com/bfs/wbi/sub-key.png"
              }
            }
            """.trimIndent()
        )

        assertEquals(0L, data.mid)
        assertEquals("", data.uname)
        assertEquals("", data.face)
    }
}
