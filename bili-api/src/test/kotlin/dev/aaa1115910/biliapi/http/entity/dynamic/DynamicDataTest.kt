package dev.aaa1115910.biliapi.http.entity.dynamic

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `dynamic stat counts accept string values`() {
        val data = Json.decodeFromString<DynamicData>(
            """
            {
                "items": [
                    {
                        "modules": {
                            "module_stat": {
                                "like": {
                                    "count": "12",
                                    "status": true
                                },
                                "comment": {
                                    "count": "0"
                                },
                                "forward": {
                                    "count": 3
                                }
                            }
                        }
                    }
                ]
            }
            """.trimIndent()
        )

        val stat = data.items.first().modules.moduleStat
        assertEquals(12, stat?.like?.count)
        assertEquals(0, stat?.comment?.count)
        assertEquals(3, stat?.forward?.count)
    }

    @Test
    fun `opus detail nullable numeric fields accept string values`() {
        val data = Json.decodeFromString<OpusDetailData>(
            """
            {
                "item": {
                    "id_str": "123",
                    "basic": {
                        "comment_type": "17"
                    },
                    "modules": [
                        {
                            "module_author": {
                                "mid": 1,
                                "name": "up",
                                "face": "",
                                "pub_ts": "1710000000"
                            },
                            "module_stat": {
                                "like": {
                                    "count": "7"
                                }
                            }
                        }
                    ]
                }
            }
            """.trimIndent()
        )

        val item = data.item
        val module = item?.modules?.first()
        assertEquals(17, item?.basic?.commentType)
        assertEquals(1_710_000_000L, module?.moduleAuthor?.pubTs)
        assertEquals(7, module?.moduleStat?.like?.count)
    }
}
