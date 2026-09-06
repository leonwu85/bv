package dev.aaa1115910.bv.tv.component

import dev.aaa1115910.bv.viewmodel.LiveDanmakuMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class LiveDanmakuSplitPanelTest {
    @Test
    fun shortMessagesStillGetMinimumReadingTime() {
        assertEquals(330L, liveDanmakuSplitInsertIntervalMs("好"))
    }

    @Test
    fun longerMessagesStayLongerAndIntervalIsCapped() {
        val medium = liveDanmakuSplitInsertIntervalMs("这是一条需要稍微多一点时间阅读的直播弹幕")
        val veryLong = liveDanmakuSplitInsertIntervalMs("很".repeat(100))

        assertTrue(medium > 330L)
        assertEquals(670L, veryLong)
    }

    @Test
    fun splitHistoryKeepsOnlyTheNewestFiveHundredMessages() {
        val messages = (0L..500L)
            .map { id -> message(id = id, userLevel = 0, timestampMs = id) }
            .toMutableList()

        trimLiveDanmakuSplitHistory(messages)

        assertEquals(LIVE_DANMAKU_SPLIT_MAX_HISTORY_MESSAGES, messages.size)
        assertEquals(1L, messages.first().id)
        assertEquals(500L, messages.last().id)
    }

    @Test
    fun splitTextUsesOriginalSizeAtGlobalScaleMinusTwentyPercent() {
        val density = 2f
        val fontScale = 1f
        val resolvedSp = resolveLiveDanmakuSplitTextSizeSp(
            sourceFontSize = 25,
            danmakuScale = 1.25f,
            density = density,
            fontScale = fontScale,
        )

        val composePixels = resolvedSp * density * fontScale
        val expectedPixels = 25f * (density - 0.6f) * (1.25f - 0.2f)
        assertEquals(expectedPixels, composePixels, absoluteTolerance = 0.001f)
    }

    @Test
    fun splitTextPreservesRelativeOriginalFontSizes() {
        val normal = resolveLiveDanmakuSplitTextSizeSp(25, 1.25f, 2f, 1f)
        val large = resolveLiveDanmakuSplitTextSizeSp(36, 1.25f, 2f, 1f)

        assertEquals(36f / 25f, large / normal, absoluteTolerance = 0.001f)
    }

    @Test
    fun splitTextScaleCannotBecomeNegative() {
        val tinyGlobalScale = resolveLiveDanmakuSplitTextSizeSp(25, 0.1f, 2f, 1f)
        val minimumEffectiveScale = resolveLiveDanmakuSplitTextSizeSp(25, 0.3f, 2f, 1f)

        assertEquals(minimumEffectiveScale, tinyGlobalScale, absoluteTolerance = 0.001f)
    }

    @Test
    fun emojiCandidatesFollowTheExistingLiveEmojiSwitch() {
        val emojiMap = mapOf("[doge]" to "https://example.com/doge.png")

        assertTrue(selectLiveDanmakuEmoji("你好[doge]", emojiMap, showEmoji = false).isEmpty())
        assertEquals(
            listOf("[doge]"),
            selectLiveDanmakuEmoji("你好[doge]", emojiMap, showEmoji = true).map { it.key },
        )
    }

    @Test
    fun splitMessagesFollowTheCurrentLiveUserLevelFilter() {
        assertFalse(message(id = 1, userLevel = 9, timestampMs = 1)
            .passesLiveDanmakuSplitFilter(minimumUserLevel = 10))
        assertTrue(message(id = 2, userLevel = 10, timestampMs = 2)
            .passesLiveDanmakuSplitFilter(minimumUserLevel = 10))
        assertTrue(message(id = 3, userLevel = 11, timestampMs = 3)
            .passesLiveDanmakuSplitFilter(minimumUserLevel = 10))
    }

    @Test
    fun raisingLiveFilterRemovesLowerLevelPendingMessages() {
        val buffer = LiveDanmakuPriorityBuffer(capacity = 3)
        buffer.offer(message(id = 1, userLevel = 9, timestampMs = 1))
        buffer.offer(message(id = 2, userLevel = 10, timestampMs = 2))
        buffer.offer(message(id = 3, userLevel = 20, timestampMs = 3))

        buffer.removeBelowUserLevel(minimumUserLevel = 10)

        assertEquals(listOf(2L, 3L), buffer.snapshot().map { it.id })
        buffer.close()
    }

    @Test
    fun fullBufferDropsLowestLevelBeforeOlderHigherLevelMessages() {
        val buffer = LiveDanmakuPriorityBuffer(capacity = 3)
        buffer.offer(message(id = 1, userLevel = 10, timestampMs = 1))
        buffer.offer(message(id = 2, userLevel = 1, timestampMs = 2))
        buffer.offer(message(id = 3, userLevel = 5, timestampMs = 3))

        assertTrue(buffer.offer(message(id = 4, userLevel = 8, timestampMs = 4)))
        assertEquals(listOf(1L, 3L, 4L), buffer.snapshot().map { it.id })
        buffer.close()
    }

    @Test
    fun fullBufferDropsOldestWhenLevelsMatch() {
        val buffer = LiveDanmakuPriorityBuffer(capacity = 2)
        buffer.offer(message(id = 1, userLevel = 10, timestampMs = 1))
        buffer.offer(message(id = 2, userLevel = 10, timestampMs = 2))

        assertTrue(buffer.offer(message(id = 3, userLevel = 10, timestampMs = 3)))
        assertEquals(listOf(2L, 3L), buffer.snapshot().map { it.id })
        buffer.close()
    }

    @Test
    fun fullBufferRejectsIncomingMessageWhenItHasLowestPriority() {
        val buffer = LiveDanmakuPriorityBuffer(capacity = 2)
        buffer.offer(message(id = 1, userLevel = 10, timestampMs = 1))
        buffer.offer(message(id = 2, userLevel = 20, timestampMs = 2))

        assertFalse(buffer.offer(message(id = 3, userLevel = 1, timestampMs = 3)))
        assertEquals(listOf(1L, 2L), buffer.snapshot().map { it.id })
        buffer.close()
    }

    @Test
    fun closingWhileWaitingEndsTheConsumerNormally() = runBlocking {
        val buffer = LiveDanmakuPriorityBuffer()
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { buffer.take() }
        assertFalse(waiting.isCompleted)

        buffer.close()

        withTimeout(1_000) { assertNull(waiting.await()) }
    }

    @Test
    fun closingDropsPendingMessagesAndRejectsFurtherOffers() = runBlocking {
        val buffer = LiveDanmakuPriorityBuffer()
        buffer.offer(message(id = 1, userLevel = 10, timestampMs = 1))

        buffer.close()
        buffer.close()

        assertTrue(buffer.snapshot().isEmpty())
        assertFalse(buffer.offer(message(id = 2, userLevel = 10, timestampMs = 2)))
        withTimeout(1_000) { assertNull(buffer.take()) }
    }

    @Test
    fun consumerWaitsAgainAfterDrainingTheQueueAndCanBeClosed() = runBlocking {
        val buffer = LiveDanmakuPriorityBuffer()
        val first = message(id = 1, userLevel = 10, timestampMs = 1)
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { buffer.take() }
        buffer.offer(first)
        withTimeout(1_000) { assertEquals(first, waiting.await()) }

        val next = async(start = CoroutineStart.UNDISPATCHED) { buffer.take() }
        assertFalse(next.isCompleted)
        buffer.close()
        withTimeout(1_000) { assertNull(next.await()) }
    }

    private fun message(
        id: Long,
        userLevel: Int,
        timestampMs: Long,
    ) = LiveDanmakuMessage(
        id = id,
        username = "user$id",
        content = "message$id",
        medalName = null,
        medalLevel = null,
        userLevel = userLevel,
        color = 0xFFFFFFFF.toInt(),
        fontSize = 25,
        timestampMs = timestampMs,
        emojiMap = emptyMap(),
    )
}
