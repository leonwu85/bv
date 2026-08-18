package dev.aaa1115910.bv.tv.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import kotlin.math.min

// KeepAlivePages already holds visible-page images until its tier-specific image delay has
// elapsed. Avoid stacking another full quiet window on top: on Standard devices that duplicate
// wait was pushed across a long AVD frame and delayed the first decode by ~260 ms.
private const val STANDARD_FIRST_BATCH_QUIET_MS = 0L
private const val BALANCED_FIRST_BATCH_QUIET_MS = 32L
private const val CONSERVATIVE_FIRST_BATCH_QUIET_MS = 64L
private const val STANDARD_BATCH_INTERVAL_MS = 64L
private const val BALANCED_BATCH_INTERVAL_MS = 88L
private const val CONSERVATIVE_BATCH_INTERVAL_MS = 112L
private const val INITIAL_VISIBLE_ROWS = 3

/**
 * Releases the visible TV grid viewport first, then expands the off-screen cache window one row
 * at a time. Decode concurrency is limited separately in Coil; splitting the visible rows across
 * multiple compositions only delays content and does not provide additional CPU peak control.
 */
@Composable
fun rememberProgressiveImageLoadLimit(
    enabled: Boolean,
    progressive: Boolean,
    itemCount: Int,
    columns: Int,
    contentKey: Any?,
): Int {
    val coordinator = LocalTvPreloadCoordinator.current
    val performanceTier = LocalTvUiPerformanceProfile.current.tier
    val safeColumns = columns.coerceAtLeast(1)
    var loadLimit by remember(contentKey) { mutableIntStateOf(0) }

    LaunchedEffect(
        enabled,
        progressive,
        itemCount,
        safeColumns,
        contentKey,
        performanceTier,
        coordinator,
    ) {
        if (!enabled || itemCount <= 0) {
            loadLimit = 0
            return@LaunchedEffect
        }
        if (!progressive) {
            // The startup page can use the launch interval to fill Coil's queue. Leaving it in
            // staged mode creates stale work that competes with the user's first Tab switch.
            loadLimit = itemCount
            return@LaunchedEffect
        }

        val firstBatchQuietMillis = when (performanceTier) {
            TvUiPerformanceTier.Conservative -> CONSERVATIVE_FIRST_BATCH_QUIET_MS
            TvUiPerformanceTier.Balanced -> BALANCED_FIRST_BATCH_QUIET_MS
            TvUiPerformanceTier.Standard -> STANDARD_FIRST_BATCH_QUIET_MS
        }
        val batchIntervalMillis = when (performanceTier) {
            TvUiPerformanceTier.Conservative -> CONSERVATIVE_BATCH_INTERVAL_MS
            TvUiPerformanceTier.Balanced -> BALANCED_BATCH_INTERVAL_MS
            TvUiPerformanceTier.Standard -> STANDARD_BATCH_INTERVAL_MS
        }

        if (loadLimit <= 0) {
            coordinator.awaitInteractionIdle(firstBatchQuietMillis)
            loadLimit = min(safeColumns * INITIAL_VISIBLE_ROWS, itemCount)
        }

        while (loadLimit < itemCount) {
            delay(batchIntervalMillis)
            withFrameNanos { }
            loadLimit = min(loadLimit + safeColumns, itemCount)
        }
    }

    return when {
        !enabled -> 0
        !progressive -> itemCount
        else -> loadLimit
    }
}
