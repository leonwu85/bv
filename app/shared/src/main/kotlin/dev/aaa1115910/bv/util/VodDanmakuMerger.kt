package dev.aaa1115910.bv.util

import com.github.stuxuhai.jpinyin.PinyinFormat
import com.github.stuxuhai.jpinyin.PinyinHelper
import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import java.util.Locale
import kotlin.math.abs

internal data class MergedDanmakuEntry(
    val source: DanmakuData,
    val totalCount: Int,
    val representativeText: String = source.text
) {
    val content: String
        get() = if (totalCount > 1) "$representativeText(x$totalCount)" else representativeText
}

internal data class DanmakuSegmentMergeResult(
    val emittedDanmaku: List<MergedDanmakuEntry>,
    val mergedDuplicateCount: Int
)

internal class VodDanmakuMergeState {
    private val lock = Any()
    private val activeGroups = LinkedHashMap<Int, ActiveDanmakuGroup>()
    private var nextGroupId = 0
    internal var lastProcessedSegmentIndex: Int? = null

    internal fun activeGroups(): List<ActiveDanmakuGroup> = synchronized(lock) {
        activeGroups.values.toList()
    }

    internal fun putGroup(group: ActiveDanmakuGroup) {
        synchronized(lock) {
            activeGroups[nextGroupId++] = group
        }
    }

    internal fun isEmpty(): Boolean = synchronized(lock) {
        activeGroups.isEmpty()
    }

    internal fun takeMatchingGroups(
        predicate: (ActiveDanmakuGroup) -> Boolean
    ): List<ActiveDanmakuGroup> = synchronized(lock) {
        if (activeGroups.isEmpty()) return emptyList()

        val matchedGroups = mutableListOf<ActiveDanmakuGroup>()
        val iterator = activeGroups.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!predicate(entry.value)) continue
            matchedGroups += entry.value
            iterator.remove()
        }
        matchedGroups
    }

    fun clear() {
        synchronized(lock) {
            activeGroups.clear()
            nextGroupId = 0
            lastProcessedSegmentIndex = null
        }
    }
}

internal object VodDanmakuMerger {
    private const val MERGE_WINDOW_SECONDS = 30f
    private val mergeableTypes = setOf(1, 4, 5)

    fun processSegment(
        segmentDanmaku: List<DanmakuData>,
        segmentIndex: Int,
        segmentDurationMs: Long,
        state: VodDanmakuMergeState
    ): DanmakuSegmentMergeResult {
        val emittedDanmaku = mutableListOf<MergedDanmakuEntry>()
        var mergedDuplicateCount = 0

        val lastProcessedSegmentIndex = state.lastProcessedSegmentIndex
        if (lastProcessedSegmentIndex != null && segmentIndex != lastProcessedSegmentIndex + 1) {
            emittedDanmaku += flushAll(state)
        }

        if (segmentDanmaku.isEmpty()) {
            state.lastProcessedSegmentIndex = segmentIndex
            return DanmakuSegmentMergeResult(
                emittedDanmaku = emittedDanmaku.sortedBy { it.source.time },
                mergedDuplicateCount = mergedDuplicateCount
            )
        }

        segmentDanmaku.sortedBy { it.time }.forEach { danmaku ->
            emittedDanmaku += flushExpiredGroups(state, danmaku.time)

            if (danmaku.shouldBypassMerge()) {
                emittedDanmaku += MergedDanmakuEntry(
                    source = danmaku,
                    totalCount = 1
                )
                return@forEach
            }

            val analysis = DanmakuTextAnalyzer.analyze(danmaku.text)
            val activeGroup = findMatchingGroup(state, danmaku, analysis)
            if (activeGroup != null) {
                activeGroup.add(danmaku, analysis)
                mergedDuplicateCount++
            } else {
                state.putGroup(
                    ActiveDanmakuGroup(
                        source = danmaku,
                        analysis = analysis
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

        val matchedGroups = state.takeMatchingGroups(predicate)
            .sortedBy { group -> group.firstTime }

        if (matchedGroups.isEmpty()) return emptyList()

        return matchedGroups.map { group -> group.toEntry() }
    }

    private fun findMatchingGroup(
        state: VodDanmakuMergeState,
        danmaku: DanmakuData,
        analysis: DanmakuTextAnalysis
    ): ActiveDanmakuGroup? {
        return state.activeGroups().firstOrNull { group ->
            group.type == danmaku.type &&
                danmaku.time - group.firstTime <= MERGE_WINDOW_SECONDS &&
                group.canMerge(analysis)
        }
    }

    private fun DanmakuData.shouldBypassMerge(): Boolean {
        return pool == 1 || type !in mergeableTypes
    }
}

internal data class ActiveDanmakuGroup(
    val source: DanmakuData,
    val analysis: DanmakuTextAnalysis
) {
    private val peers = mutableListOf(source)
    private val analyses = mutableListOf(analysis)
    private val textCounts = linkedMapOf(source.text to 1)
    val type: Int = source.type
    val firstTime: Float = source.time

    fun canMerge(candidate: DanmakuTextAnalysis): Boolean {
        return analyses.any { existing ->
            DanmakuSimilarity.isSimilar(existing, candidate)
        }
    }

    fun add(danmaku: DanmakuData, analysis: DanmakuTextAnalysis) {
        peers += danmaku
        analyses += analysis
        textCounts[danmaku.text] = (textCounts[danmaku.text] ?: 0) + 1
    }

    fun toEntry(): MergedDanmakuEntry {
        val representativeText = chooseRepresentativeText()
        val representativeSource = chooseRepresentativeSource()
        return MergedDanmakuEntry(
            source = representativeSource,
            totalCount = peers.size,
            representativeText = representativeText
        )
    }

    private fun chooseRepresentativeText(): String {
        val maxCount = textCounts.values.maxOrNull() ?: return source.text
        val candidates = textCounts
            .filterValues { it == maxCount }
            .keys
            .sortedBy { it.length }
        return candidates[candidates.size / 2]
    }

    private fun chooseRepresentativeSource(): DanmakuData {
        val representative = peers.first()
        val representativeSize = peers
            .map { it.size }
            .filter { it <= 30 }
            .maxOrNull()
            ?: representative.size
        return representative.copy(
            size = representativeSize,
            level = peers.maxOf { it.level }
        )
    }
}

internal data class DanmakuTextAnalysis(
    val normalized: String,
    val pinyinTokens: List<String>,
    val isPureNoise: Boolean
)

private object DanmakuTextAnalyzer {
    private const val MAX_PINYIN_CACHE_SIZE = 2048
    private val force233Regex = Regex("^23{2,}$")
    private val force666Regex = Regex("^6{3,}$")
    private val pinyinCache = object : LinkedHashMap<String, List<String>>(MAX_PINYIN_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean {
            return size > MAX_PINYIN_CACHE_SIZE
        }
    }

    fun analyze(text: String): DanmakuTextAnalysis {
        val normalized = normalize(text)
        return DanmakuTextAnalysis(
            normalized = normalized.text,
            pinyinTokens = if (normalized.isPureNoise) emptyList() else pinyinTokens(normalized.text),
            isPureNoise = normalized.isPureNoise
        )
    }

    private fun normalize(text: String): NormalizedText {
        val halfWidth = text
            .map { it.toHalfWidth() }
            .joinToString(separator = "")
            .lowercase(Locale.ROOT)
        normalizePureNoise(halfWidth)?.let {
            return NormalizedText(text = it, isPureNoise = true)
        }
        val withoutTrailingNoise = halfWidth.trimTrailingNoise()
        val compressed = withoutTrailingNoise.compressSpaces()
        val compacted = compressed.removeSpacesBetweenHan()
        return NormalizedText(
            text = when {
                force233Regex.matches(compacted) -> "23333"
                force666Regex.matches(compacted) -> "66666"
                else -> compacted
            },
            isPureNoise = false
        )
    }

    private fun normalizePureNoise(text: String): String? {
        val compacted = text.filterNot { it.isWhitespace() }
        if (compacted.isEmpty() || compacted.any { !it.isTrailingNoise() }) return null

        val first = compacted.first()
        return if (compacted.all { it == first }) first.toString() else compacted
    }

    private fun pinyinTokens(normalized: String): List<String> {
        if (normalized.count { it.isChinese() } < 2) return emptyList()

        synchronized(pinyinCache) {
            pinyinCache[normalized]?.let { return it }
        }

        val tokens = normalized
            .mapNotNull { char ->
                if (!char.isChinese()) return@mapNotNull null
                runCatching {
                    PinyinHelper
                        .convertToPinyinArray(char, PinyinFormat.WITHOUT_TONE)
                        ?.firstOrNull()
                        ?.lowercase(Locale.ROOT)
                }.getOrNull()
            }
            .filter { it.isNotBlank() }
        val result = if (tokens.size >= 2) tokens else emptyList()

        synchronized(pinyinCache) {
            pinyinCache[normalized] = result
        }
        return result
    }

    private fun Char.toHalfWidth(): Char {
        return when (this) {
            '\u3000' -> ' '
            in '\uFF01'..'\uFF5E' -> (code - 0xFEE0).toChar()
            else -> this
        }
    }

    private fun String.trimTrailingNoise(): String {
        var end = length
        while (end > 0 && this[end - 1].isTrailingNoise()) {
            end--
        }
        return substring(0, end).trim()
    }

    private fun String.compressSpaces(): String {
        val builder = StringBuilder(length)
        var lastWasSpace = false
        forEach { char ->
            if (char.isWhitespace()) {
                if (!lastWasSpace) builder.append(' ')
                lastWasSpace = true
            } else {
                builder.append(char)
                lastWasSpace = false
            }
        }
        return builder.toString().trim()
    }

    private fun String.removeSpacesBetweenHan(): String {
        if (' ' !in this) return this

        val builder = StringBuilder(length)
        forEachIndexed { index, char ->
            if (
                char == ' ' &&
                getOrNull(index - 1)?.isHan() == true &&
                getOrNull(index + 1)?.isHan() == true
            ) {
                return@forEachIndexed
            }
            builder.append(char)
        }
        return builder.toString()
    }

    private fun Char.isTrailingNoise(): Boolean {
        return when (Character.getType(this)) {
            Character.CONNECTOR_PUNCTUATION.toInt(),
            Character.DASH_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt(),
            Character.END_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
            Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt(),
            Character.MATH_SYMBOL.toInt(),
            Character.CURRENCY_SYMBOL.toInt(),
            Character.MODIFIER_SYMBOL.toInt(),
            Character.OTHER_SYMBOL.toInt() -> true
            else -> false
        }
    }

    private fun Char.isChinese(): Boolean {
        return isHan()
    }

    private fun Char.isHan(): Boolean {
        return Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN
    }
}

private data class NormalizedText(
    val text: String,
    val isPureNoise: Boolean
)

private object DanmakuSimilarity {
    fun isSimilar(
        existing: DanmakuTextAnalysis,
        candidate: DanmakuTextAnalysis
    ): Boolean {
        if (existing.normalized.isBlank() || candidate.normalized.isBlank()) return false
        if (existing.normalized == candidate.normalized) return true
        if (existing.isPureNoise || candidate.isPureNoise) return false
        if (isTextEditDistanceSimilar(existing.normalized, candidate.normalized)) return true
        return isPinyinSimilar(existing.pinyinTokens, candidate.pinyinTokens)
    }

    private fun isTextEditDistanceSimilar(left: String, right: String): Boolean {
        val maxLength = maxOf(left.length, right.length)
        val threshold = when (maxLength) {
            in 0..1 -> return false
            in 2..4 -> 1
            in 5..12 -> 2
            else -> minOf(3, maxLength / 6)
        }
        return boundedEditDistance(left, right, threshold) <= threshold
    }

    private fun isPinyinSimilar(left: List<String>, right: List<String>): Boolean {
        if (left.size < 2 || right.size < 2) return false
        if (left == right) return true

        val maxLength = maxOf(left.size, right.size)
        val threshold = if (maxLength <= 4) 1 else 2
        return boundedEditDistance(left, right, threshold) <= threshold
    }

    private fun boundedEditDistance(left: String, right: String, maxDistance: Int): Int {
        if (abs(left.length - right.length) > maxDistance) return maxDistance + 1
        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)

        for (i in 1..left.length) {
            current[0] = i
            var rowMin = current[0]
            for (j in 1..right.length) {
                val cost = if (left[i - 1] == right[j - 1]) 0 else 1
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + cost
                )
                rowMin = minOf(rowMin, current[j])
            }
            if (rowMin > maxDistance) return maxDistance + 1
            val temp = previous
            previous = current
            current = temp
        }

        return previous[right.length]
    }

    private fun boundedEditDistance(left: List<String>, right: List<String>, maxDistance: Int): Int {
        if (abs(left.size - right.size) > maxDistance) return maxDistance + 1
        var previous = IntArray(right.size + 1) { it }
        var current = IntArray(right.size + 1)

        for (i in 1..left.size) {
            current[0] = i
            var rowMin = current[0]
            for (j in 1..right.size) {
                val cost = if (left[i - 1] == right[j - 1]) 0 else 1
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + cost
                )
                rowMin = minOf(rowMin, current[j])
            }
            if (rowMin > maxDistance) return maxDistance + 1
            val temp = previous
            previous = current
            current = temp
        }

        return previous[right.size]
    }
}
