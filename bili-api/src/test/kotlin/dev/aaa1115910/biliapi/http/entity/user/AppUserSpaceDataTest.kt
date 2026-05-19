package dev.aaa1115910.biliapi.http.entity.user

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals

class AppUserSpaceDataTest {
    @Test
    fun `top image falls back to cover when nested image is null`() {
        val topImage = AppUserSpaceTopImage(
            item = Json.parseToJsonElement(
                """
                {
                    "image": null,
                    "animation": {
                        "location": "0-100-200",
                        "height": 200
                    }
                }
                """.trimIndent()
            ),
            cover = "https://fallback.example/header.png"
        )

        assertEquals("https://fallback.example/header.png", topImage.header)
        assertEquals(0.5f, topImage.alignmentY)
    }

    @Test
    fun `top image ignores null item`() {
        val topImage = AppUserSpaceTopImage(
            item = JsonNull,
            cover = "https://fallback.example/header.png"
        )

        assertEquals("https://fallback.example/header.png", topImage.header)
        assertEquals(0f, topImage.alignmentY)
    }
}
