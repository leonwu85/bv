package dev.aaa1115910.bilisubtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubtitleParserTest {
    @Test
    fun `read bcc subtitle`() {
        val fileContent = this::class.java.getResource("/example.bcc")?.readText()!!
        val result = SubtitleParser.fromBccString(fileContent)
        println(result)
    }

    @Test
    fun `read srt subtitle`() {
        val fileContent = this::class.java.getResource("/example.srt")?.readText()!!
        val result = SubtitleParser.fromSrtString(fileContent)
        println(result)
    }

        @Test
        fun `read ai bcc subtitle without location`() {
                val fileContent = """
                        {
                            "font_size": 0.4,
                            "font_color": "#FFFFFF",
                            "background_alpha": 0.5,
                            "background_color": "#9C27B0",
                            "Stroke": "none",
                            "type": "AIsubtitle",
                            "lang": "pt",
                            "version": "1.0",
                            "body": [
                                {
                                    "from": 0.5,
                                    "to": 2.0,
                                    "content": "Ola mundo",
                                    "sid": 1,
                                    "version": "1.0"
                                }
                            ]
                        }
                """.trimIndent()

                val result = SubtitleParser.fromBccString(fileContent, isAI = true)

                assertEquals(1, result.size)
                assertEquals("Ola mundo", result.first().content)
                assertTrue(result.first().isAI)
        }
}