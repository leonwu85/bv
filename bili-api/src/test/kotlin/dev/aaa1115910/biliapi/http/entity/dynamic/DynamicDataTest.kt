package dev.aaa1115910.biliapi.http.entity.dynamic

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class DynamicDataTest {
    @Test
    fun `author following accepts numeric flag`() {
        val data = Json.decodeFromString<DynamicData>(
            """
            {
                "items": [
                    {
                        "modules": {
                            "module_author": {
                                "following": 1
                            }
                        }
                    }
                ]
            }
            """.trimIndent()
        )

        assertTrue(data.items.first().modules.moduleAuthor?.following == true)
    }
}
