package dev.aaa1115910.bv.tv.util

/**
 * Main-thread request generation guard for UI-owned suspend work.
 *
 * Cancellation remains the primary ownership mechanism. The generation check is the final guard
 * against repositories or platform calls that return a result after cancellation was requested.
 */
internal class LatestRequestOwner {
    private var generation: Long = 0L

    fun claim(): Long = ++generation

    fun invalidate() {
        generation++
    }

    fun owns(candidate: Long): Boolean = candidate == generation
}
