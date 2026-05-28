package dev.aaa1115910.bv.viewmodel

internal const val DANMAKU_SEGMENT_EMPTY_RETRY_DELAY_MS = 10_000L
internal const val DANMAKU_SEGMENT_FAILURE_RETRY_DELAY_MS = 5_000L
internal const val DANMAKU_SEGMENT_CONFIRMED_EMPTY_RECHECK_DELAY_MS = 10 * 60_000L
internal const val DANMAKU_SEGMENT_MAX_EMPTY_ATTEMPTS = 3
internal const val DANMAKU_SEGMENT_MAX_FAILURE_ATTEMPTS = 3

internal sealed class DanmakuSegmentCacheEntry<out T> {
    abstract val updatedAtMs: Long
    abstract val source: String
    abstract val rawCount: Int
    abstract val cacheState: String

    data class Loaded<T>(
        val value: T,
        override val updatedAtMs: Long,
        override val source: String,
        override val rawCount: Int
    ) : DanmakuSegmentCacheEntry<T>() {
        override val cacheState: String = "Loaded"
    }

    data class ConfirmedEmpty(
        override val updatedAtMs: Long,
        override val source: String,
        override val rawCount: Int,
        val attempts: Int,
        val retryAfterMs: Long,
        val reason: String
    ) : DanmakuSegmentCacheEntry<Nothing>() {
        override val cacheState: String = "ConfirmedEmpty"
    }

    data class TransientFailed(
        override val updatedAtMs: Long,
        override val source: String,
        override val rawCount: Int,
        val attempts: Int,
        val retryAfterMs: Long,
        val reason: String
    ) : DanmakuSegmentCacheEntry<Nothing>() {
        override val cacheState: String = "TransientFailed"
    }
}

internal fun DanmakuSegmentCacheEntry<*>?.shouldFetchDanmakuSegment(nowMs: Long): Boolean {
    return when (this) {
        null -> true
        is DanmakuSegmentCacheEntry.Loaded -> false
        is DanmakuSegmentCacheEntry.ConfirmedEmpty -> nowMs >= retryAfterMs
        is DanmakuSegmentCacheEntry.TransientFailed -> nowMs >= retryAfterMs
    }
}

internal fun nextEmptyDanmakuSegmentCacheEntry(
    previous: DanmakuSegmentCacheEntry<*>?,
    nowMs: Long,
    source: String,
    rawCount: Int,
    reason: String = "empty_reply"
): DanmakuSegmentCacheEntry<Nothing> {
    val attempts = previous.retryAttemptsFor(reason) + 1
    return if (attempts >= DANMAKU_SEGMENT_MAX_EMPTY_ATTEMPTS) {
        DanmakuSegmentCacheEntry.ConfirmedEmpty(
            updatedAtMs = nowMs,
            source = source,
            rawCount = rawCount,
            attempts = attempts,
            retryAfterMs = nowMs + DANMAKU_SEGMENT_CONFIRMED_EMPTY_RECHECK_DELAY_MS,
            reason = reason
        )
    } else {
        DanmakuSegmentCacheEntry.TransientFailed(
            updatedAtMs = nowMs,
            source = source,
            rawCount = rawCount,
            attempts = attempts,
            retryAfterMs = nowMs + DANMAKU_SEGMENT_EMPTY_RETRY_DELAY_MS * attempts,
            reason = reason
        )
    }
}

internal fun nextFailedDanmakuSegmentCacheEntry(
    previous: DanmakuSegmentCacheEntry<*>?,
    nowMs: Long,
    source: String,
    reason: String
): DanmakuSegmentCacheEntry<Nothing> {
    val attempts = previous.retryAttemptsFor(reason) + 1
    return if (attempts >= DANMAKU_SEGMENT_MAX_FAILURE_ATTEMPTS) {
        DanmakuSegmentCacheEntry.TransientFailed(
            updatedAtMs = nowMs,
            source = source,
            rawCount = 0,
            attempts = attempts,
            retryAfterMs = nowMs + DANMAKU_SEGMENT_EMPTY_RETRY_DELAY_MS,
            reason = reason
        )
    } else {
        DanmakuSegmentCacheEntry.TransientFailed(
            updatedAtMs = nowMs,
            source = source,
            rawCount = 0,
            attempts = attempts,
            retryAfterMs = nowMs + DANMAKU_SEGMENT_FAILURE_RETRY_DELAY_MS * attempts,
            reason = reason
        )
    }
}

private fun DanmakuSegmentCacheEntry<*>?.retryAttemptsFor(reason: String): Int {
    return when (this) {
        is DanmakuSegmentCacheEntry.ConfirmedEmpty -> attempts.takeIf { this.reason == reason } ?: 0
        is DanmakuSegmentCacheEntry.TransientFailed -> attempts.takeIf { this.reason == reason } ?: 0
        else -> 0
    }
}
