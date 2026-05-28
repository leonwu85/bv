package dev.aaa1115910.bv.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DanmakuSegmentCacheStateTest {
    @Test
    fun webEmptySegmentCanBeRetriedAndReplacedByLoadedData() {
        val now = 1_000L
        val firstEmpty = nextEmptyDanmakuSegmentCacheEntry(
            previous = null,
            nowMs = now,
            source = "web_app_xml_empty",
            rawCount = 0
        )

        assertIs<DanmakuSegmentCacheEntry.TransientFailed>(firstEmpty)
        assertFalse(firstEmpty.shouldFetchDanmakuSegment(now + DANMAKU_SEGMENT_EMPTY_RETRY_DELAY_MS - 1))
        assertTrue(firstEmpty.shouldFetchDanmakuSegment(now + DANMAKU_SEGMENT_EMPTY_RETRY_DELAY_MS))

        val loaded = DanmakuSegmentCacheEntry.Loaded(
            value = listOf("danmaku"),
            updatedAtMs = now + DANMAKU_SEGMENT_EMPTY_RETRY_DELAY_MS,
            source = "web",
            rawCount = 1
        )

        assertFalse(loaded.shouldFetchDanmakuSegment(now + DANMAKU_SEGMENT_EMPTY_RETRY_DELAY_MS))
        assertEquals(listOf("danmaku"), loaded.value)
    }

    @Test
    fun failedSegmentDoesNotBecomeLoadedEmptyCache() {
        val now = 2_000L
        val failed = nextFailedDanmakuSegmentCacheEntry(
            previous = null,
            nowMs = now,
            source = "web",
            reason = "SocketTimeoutException"
        )

        assertIs<DanmakuSegmentCacheEntry.TransientFailed>(failed)
        assertFalse(failed.shouldFetchDanmakuSegment(now + DANMAKU_SEGMENT_FAILURE_RETRY_DELAY_MS - 1))
        assertTrue(failed.shouldFetchDanmakuSegment(now + DANMAKU_SEGMENT_FAILURE_RETRY_DELAY_MS))
    }

    @Test
    fun repeatedEmptySegmentBecomesConfirmedEmptyWithLongRecheck() {
        var cacheEntry: DanmakuSegmentCacheEntry<*>? = null
        val now = 3_000L

        repeat(DANMAKU_SEGMENT_MAX_EMPTY_ATTEMPTS) { index ->
            cacheEntry = nextEmptyDanmakuSegmentCacheEntry(
                previous = cacheEntry,
                nowMs = now + index * DANMAKU_SEGMENT_EMPTY_RETRY_DELAY_MS,
                source = "web_app_xml_empty",
                rawCount = 0
            )
        }

        val confirmedEmpty = assertIs<DanmakuSegmentCacheEntry.ConfirmedEmpty>(cacheEntry)
        assertFalse(confirmedEmpty.shouldFetchDanmakuSegment(confirmedEmpty.retryAfterMs - 1))
        assertTrue(confirmedEmpty.shouldFetchDanmakuSegment(confirmedEmpty.retryAfterMs))
    }

    @Test
    fun preloadedTransientEmptyFutureSegmentCanBeRevalidatedLater() {
        val preloadTime = 4_000L
        val cacheEntry = nextEmptyDanmakuSegmentCacheEntry(
            previous = null,
            nowMs = preloadTime,
            source = "web_app_xml_empty",
            rawCount = 0
        )

        assertIs<DanmakuSegmentCacheEntry.TransientFailed>(cacheEntry)
        assertTrue(cacheEntry.shouldFetchDanmakuSegment(cacheEntry.retryAfterMs))
    }
}
