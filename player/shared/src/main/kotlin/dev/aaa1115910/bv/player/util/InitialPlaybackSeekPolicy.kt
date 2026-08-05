package dev.aaa1115910.bv.player.util

import kotlin.math.abs

fun isInitialPlaybackSeek(
    pendingInitialSeek: Boolean,
    hasExplicitUserSeek: Boolean,
    expectedPositionMs: Long,
    actualPositionMs: Long,
    toleranceMs: Long = 1_500L
): Boolean {
    if (!pendingInitialSeek || hasExplicitUserSeek || expectedPositionMs <= 0L) return false

    return abs(actualPositionMs - expectedPositionMs) <= toleranceMs.coerceAtLeast(0L)
}
