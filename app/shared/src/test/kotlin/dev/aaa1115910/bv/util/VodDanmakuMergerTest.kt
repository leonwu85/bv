package dev.aaa1115910.bv.util

import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VodDanmakuMergerTest {
    @Test
    fun mergesDuplicatesWithinThirtySeconds() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "来了"),
                danmaku(time = 30f, dmid = 2L, text = "来了")
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
    fun normalizesTextBeforeMerging() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "23333"),
                danmaku(time = 2f, dmid = 2L, text = "233333333"),
                danmaku(time = 3f, dmid = 3L, text = "23333"),
                danmaku(time = 4f, dmid = 4L, text = "６６６６"),
                danmaku(time = 5f, dmid = 5L, text = "66666"),
                danmaku(time = 6f, dmid = 6L, text = "66666"),
                danmaku(time = 7f, dmid = 7L, text = "来 了!!!"),
                danmaku(time = 8f, dmid = 8L, text = "来了"),
                danmaku(time = 9f, dmid = 9L, text = "来了")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(
            listOf("23333(x3)", "66666(x3)", "来了(x3)"),
            result.emittedDanmaku.map { it.content }
        )
        assertEquals(6, result.mergedDuplicateCount)
    }

    @Test
    fun mergesRepeatedPureQuestionMarks() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "？"),
                danmaku(time = 2f, dmid = 2L, text = "？？？"),
                danmaku(time = 3f, dmid = 3L, text = "？？")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(1, result.emittedDanmaku.size)
        assertEquals(2, result.mergedDuplicateCount)
        assertEquals("？？(x3)", result.emittedDanmaku.single().content)
    }

    @Test
    fun doesNotMergeDifferentPurePunctuation() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "？"),
                danmaku(time = 2f, dmid = 2L, text = "！！！"),
                danmaku(time = 3f, dmid = 3L, text = "？！")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(3, result.emittedDanmaku.size)
        assertEquals(0, result.mergedDuplicateCount)
    }

    @Test
    fun mergesWhenVisualAttributesDifferButKeepsTypesSeparate() {
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

        assertEquals(2, result.emittedDanmaku.size)
        assertEquals(2, result.mergedDuplicateCount)
        assertEquals("来了(x3)", result.emittedDanmaku.first().content)
        assertEquals(30, result.emittedDanmaku.first().source.size)
        assertEquals("来了", result.emittedDanmaku.last().content)
        assertEquals(5, result.emittedDanmaku.last().source.type)
    }

    @Test
    fun mergesPinyinSimilarDanmaku() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "好看"),
                danmaku(time = 5f, dmid = 2L, text = "郝侃")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(1, result.emittedDanmaku.size)
        assertEquals(1, result.mergedDuplicateCount)
        assertEquals(2, result.emittedDanmaku.single().totalCount)
    }

    @Test
    fun doesNotUsePinyinSimilarityForSingleChineseCharacter() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "好"),
                danmaku(time = 5f, dmid = 2L, text = "号")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(2, result.emittedDanmaku.size)
        assertEquals(0, result.mergedDuplicateCount)
        assertTrue(result.emittedDanmaku.all { it.totalCount == 1 })
    }

    @Test
    fun doesNotMergeObviouslyDifferentText() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "前方高能"),
                danmaku(time = 5f, dmid = 2L, text = "天气不错")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(2, result.emittedDanmaku.size)
        assertEquals(0, result.mergedDuplicateCount)
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
                danmaku(time = 375f, dmid = 2L, text = "来了")
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
    fun immediateDisplayDoesNotWaitForNextSegmentToFlushTail() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegmentForImmediateDisplay(
            segmentDanmaku = listOf(
                danmaku(time = 350f, dmid = 1L, text = "片尾弹幕")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(listOf("片尾弹幕"), result.emittedDanmaku.map { it.content })
        assertTrue(state.isEmpty())
    }

    @Test
    fun immediateDisplayUsesNormalizedExactFastPath() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegmentForImmediateDisplay(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "来 了!!!"),
                danmaku(time = 2f, dmid = 2L, text = "来了"),
                danmaku(time = 3f, dmid = 3L, text = "好看"),
                danmaku(time = 4f, dmid = 4L, text = "郝侃")
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(1, result.mergedDuplicateCount)
        assertEquals(listOf(2, 1, 1), result.emittedDanmaku.map { it.totalCount })
        assertTrue(state.isEmpty())
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

    @Test
    fun bypassesPoolAndSpecialDanmaku() {
        val state = VodDanmakuMergeState()

        val result = VodDanmakuMerger.processSegment(
            segmentDanmaku = listOf(
                danmaku(time = 1f, dmid = 1L, text = "字幕", pool = 1),
                danmaku(time = 2f, dmid = 2L, text = "字幕", pool = 1),
                danmaku(time = 3f, dmid = 3L, text = "特殊", type = 7),
                danmaku(time = 4f, dmid = 4L, text = "特殊", type = 7),
                danmaku(time = 5f, dmid = 5L, text = "代码", type = 8),
                danmaku(time = 6f, dmid = 6L, text = "代码", type = 8),
                danmaku(time = 7f, dmid = 7L, text = "bas", type = 9),
                danmaku(time = 8f, dmid = 8L, text = "bas", type = 9)
            ),
            segmentIndex = 1,
            segmentDurationMs = 360_000L,
            state = state
        )

        assertEquals(8, result.emittedDanmaku.size)
        assertEquals(0, result.mergedDuplicateCount)
        assertTrue(result.emittedDanmaku.all { it.totalCount == 1 })
    }

    @Test
    fun processesPopularVideoSizedSegmentWithoutQuadraticDelay() {
        val state = VodDanmakuMergeState()
        val hanChars = "阿波次德饿发个喝一机开了么你哦跑去日思特无西有中"
        val danmaku = buildList {
            repeat(1_542) { index ->
                val text = buildString {
                    append(hanChars[index % hanChars.length])
                    append(hanChars[index / hanChars.length % hanChars.length])
                    append(hanChars[index / (hanChars.length * hanChars.length) % hanChars.length])
                    append(index.toString(36))
                }
                val time = index * 0.2f
                add(danmaku(time = time, dmid = index * 2L, text = text))
                add(danmaku(time = time + 0.1f, dmid = index * 2L + 1L, text = text))
            }
        }

        lateinit var result: DanmakuSegmentMergeResult
        val elapsedMs = measureTimeMillis {
            result = VodDanmakuMerger.processSegmentForImmediateDisplay(
                segmentDanmaku = danmaku,
                segmentIndex = 1,
                segmentDurationMs = 360_000L,
                state = state
            )
        }

        assertTrue(result.mergedDuplicateCount >= 1_542)
        assertTrue(elapsedMs < 1_500L, "3084 条弹幕合并耗时 ${elapsedMs}ms")
    }

    private fun danmaku(
        time: Float,
        dmid: Long,
        text: String,
        type: Int = 1,
        size: Int = 25,
        color: Int = 0xFFFFFF,
        pool: Int = 0
    ): DanmakuData {
        return DanmakuData(
            time = time,
            type = type,
            size = size,
            color = color,
            timestamp = 0,
            pool = pool,
            midHash = "mid-$dmid",
            dmid = dmid,
            level = 1,
            text = text
        )
    }
}
