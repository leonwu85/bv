package com.origeek.imageViewer.previewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class PreviewerTransformStateTest {
    @Test
    fun `stale pager index does not query an empty data source`() {
        var queried = false

        val key = transformKeyAtOrNull(
            index = 2,
            pageCount = 0,
            getKey = {
                queried = true
                it
            },
        )

        assertNull(key)
        assertFalse(queried)
    }

    @Test
    fun `data source shrinking between snapshots falls back to no transform`() {
        val pictures = emptyList<String>()

        val key = transformKeyAtOrNull(
            index = 2,
            pageCount = 3,
            getKey = pictures::get,
        )

        assertNull(key)
    }

    @Test
    fun `valid pager index resolves transform key`() {
        val pictures = listOf("first", "second")

        val key = transformKeyAtOrNull(
            index = 1,
            pageCount = pictures.size,
            getKey = pictures::get,
        )

        assertEquals("second", key)
    }
}
