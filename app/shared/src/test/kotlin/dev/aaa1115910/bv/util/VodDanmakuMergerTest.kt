package dev.aaa1115910.bv.util

import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VodDanmakuMergerTest {
    @Test
    fun mergesDuplicatesWithinTwentySeconds() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "来了"),
                danmaku(time = 20f, dmid = 2L, text = "来了")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(1, result.emittedDanmaku.size)
        assertEquals(1, result.mergedDuplicateCount)
        assertEquals("来了(x2)", result.emittedDanmaku.single().content)
    }

    @Test
    fun doesNotMergeWhenVisualAttributesDiffer() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "来了", color = 0xFFFFFF),
                danmaku(time = 5f, dmid = 2L, text = "来了", color = 0xFF0000),
                danmaku(time = 8f, dmid = 3L, text = "来了", size = 30),
                danmaku(time = 10f, dmid = 4L, text = "来了", type = 5)
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(4, result.emittedDanmaku.size)
        assertEquals(0, result.mergedDuplicateCount)
        assertTrue(result.emittedDanmaku.all { it.totalCount == 1 })
    }

    @Test
    fun mergesAcrossAdjacentSegmentsWithinWindow() {
        val state = VodDanmakuMergeState()

        val firstSegmentResult = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 350f, dmid = 1L, text = "来了")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )
        val secondSegmentResult = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 365f, dmid = 2L, text = "来了")
            ),
            segmentIndex = 2,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertTrue(firstSegmentResult.emittedDanmaku.isEmpty())
        assertEquals(1, secondSegmentResult.emittedDanmaku.size)
        assertEquals("来了(x2)", secondSegmentResult.emittedDanmaku.single().content)
        assertEquals(1, secondSegmentResult.mergedDuplicateCount)
    }

    @Test
    fun flushesPendingGroupsOnSegmentDiscontinuity() {
        val state = VodDanmakuMergeState()

        VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 350f, dmid = 1L, text = "来了")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 721f, dmid = 2L, text = "别急")
            ),
            segmentIndex = 3,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(2, result.emittedDanmaku.size)
        assertEquals("来了", result.emittedDanmaku.first().content)
        assertEquals("别急", result.emittedDanmaku.last().content)
    }

    private fun danmaku(
        time: Float,
        dmid: Long,
        text: String,
        type: Int = 1,
        size: Int = 25,
        color: Int = 0xFFFFFF
    ): DanmakuData {
        return DanmakuData(
            time = time,
            type = type,
            size = size,
            color = color,
            timestamp = 0,
            pool = 0,
            midHash = "mid-$dmid",
            dmid = dmid,
            level = 1,
            text = text
        )
    }
}