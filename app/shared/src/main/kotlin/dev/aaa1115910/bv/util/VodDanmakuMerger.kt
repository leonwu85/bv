package dev.aaa1115910.bv.util

import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData

internal data class MergedDanmakuEntry(
    val source: DanmakuData,
    val totalCount: Int
) {
    val content: String
        get() = if (totalCount > 1) "${source.text}(x$totalCount)" else source.text
}

internal data class DanmakuSegmentMergeResult(
    val emittedDanmaku: List<MergedDanmakuEntry>,
    val mergedDuplicateCount: Int
)

internal class VodDanmakuMergeState {
    private val activeGroups = LinkedHashMap<VodDanmakuMergeKey, ActiveDanmakuGroup>()
    internal var lastProcessedSegmentIndex: Int? = null

    internal fun getGroup(key: VodDanmakuMergeKey): ActiveDanmakuGroup? = activeGroups[key]

    internal fun putGroup(key: VodDanmakuMergeKey, group: ActiveDanmakuGroup) {
        activeGroups[key] = group
    }

    internal fun removeGroup(key: VodDanmakuMergeKey) {
        activeGroups.remove(key)
    }

    internal fun isEmpty(): Boolean = activeGroups.isEmpty()

    internal fun matchingGroups(
        predicate: (ActiveDanmakuGroup) -> Boolean
    ): List<Pair<VodDanmakuMergeKey, ActiveDanmakuGroup>> {
        return activeGroups.filterValues(predicate).toList()
    }

    fun clear() {
        activeGroups.clear()
        lastProcessedSegmentIndex = null
    }
}

internal object VodDanmakuMerger {
    private const val MERGE_WINDOW_SECONDS = 20f

    fun processSegment(
        segmentDanmaku: List<DanmakuData>,
        segmentIndex: Int,
        segmentDurationMs: Long,
        state: VodDanmakuMergeState
    ): DanmakuSegmentMergeResult {
        if (segmentDanmaku.isEmpty()) {
            state.lastProcessedSegmentIndex = segmentIndex
            return DanmakuSegmentMergeResult(emptyList(), 0)
        }

        val emittedDanmaku = mutableListOf<MergedDanmakuEntry>()
        var mergedDuplicateCount = 0

        val lastProcessedSegmentIndex = state.lastProcessedSegmentIndex
        if (lastProcessedSegmentIndex != null && segmentIndex != lastProcessedSegmentIndex + 1) {
            emittedDanmaku += flushAll(state)
        }

        segmentDanmaku.sortedBy { it.time }.forEach { danmaku ->
            emittedDanmaku += flushExpiredGroups(state, danmaku.time)

            val key = VodDanmakuMergeKey.from(danmaku)
            val activeGroup = state.getGroup(key)
            if (activeGroup != null && danmaku.time - activeGroup.source.time <= MERGE_WINDOW_SECONDS) {
                activeGroup.totalCount++
                mergedDuplicateCount++
            } else {
                state.putGroup(
                    key,
                    ActiveDanmakuGroup(
                    source = danmaku,
                    totalCount = 1
                    )
                )
            }
        }

        val segmentBoundarySeconds = segmentIndex * (segmentDurationMs / 1000f)
        emittedDanmaku += flushGroupsBefore(
            state = state,
            thresholdSeconds = segmentBoundarySeconds - MERGE_WINDOW_SECONDS
        )
        state.lastProcessedSegmentIndex = segmentIndex

        return DanmakuSegmentMergeResult(
            emittedDanmaku = emittedDanmaku.sortedBy { it.source.time },
            mergedDuplicateCount = mergedDuplicateCount
        )
    }

    fun flushPending(state: VodDanmakuMergeState): List<MergedDanmakuEntry> {
        return flushAll(state)
    }

    private fun flushExpiredGroups(
        state: VodDanmakuMergeState,
        currentTimeSeconds: Float
    ): List<MergedDanmakuEntry> {
        return flushMatchingGroups(state) { currentTimeSeconds - it.source.time > MERGE_WINDOW_SECONDS }
    }

    private fun flushGroupsBefore(
        state: VodDanmakuMergeState,
        thresholdSeconds: Float
    ): List<MergedDanmakuEntry> {
        return flushMatchingGroups(state) { it.source.time < thresholdSeconds }
    }

    private fun flushAll(state: VodDanmakuMergeState): List<MergedDanmakuEntry> {
        return flushMatchingGroups(state) { true }.also {
            state.lastProcessedSegmentIndex = null
        }
    }

    private fun flushMatchingGroups(
        state: VodDanmakuMergeState,
        predicate: (ActiveDanmakuGroup) -> Boolean
    ): List<MergedDanmakuEntry> {
        if (state.isEmpty()) return emptyList()

        val matchedGroups = state.matchingGroups(predicate)
            .sortedBy { (_, group) -> group.source.time }

        if (matchedGroups.isEmpty()) return emptyList()

        matchedGroups.forEach { (key, _) ->
            state.removeGroup(key)
        }

        return matchedGroups.map { (_, group) ->
            MergedDanmakuEntry(
                source = group.source,
                totalCount = group.totalCount
            )
        }
    }
}

internal data class VodDanmakuMergeKey(
    val text: String,
    val color: Int,
    val size: Int,
    val type: Int
) {
    companion object {
        fun from(danmaku: DanmakuData): VodDanmakuMergeKey {
            return VodDanmakuMergeKey(
                text = danmaku.text,
                color = danmaku.color,
                size = danmaku.size,
                type = danmaku.type
            )
        }
    }
}

internal data class ActiveDanmakuGroup(
    val source: DanmakuData,
    var totalCount: Int
)