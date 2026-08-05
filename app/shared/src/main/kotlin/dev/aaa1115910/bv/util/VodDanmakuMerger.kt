package dev.aaa1115910.bv.util

import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import java.util.Locale
import kotlin.math.abs

private const val MAX_TEXT_EDIT_DISTANCE = 3
private const val MAX_PINYIN_EDIT_DISTANCE = 2

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

private data class DanmakuTextIndexKey(
    val type: Int,
    val normalized: String
)

private data class DanmakuTextCharIndexKey(
    val type: Int,
    val length: Int,
    val char: Char
)

private data class DanmakuPinyinIndexKey(
    val type: Int,
    val tokens: List<String>
)

private data class DanmakuPinyinTokenIndexKey(
    val type: Int,
    val tokenCount: Int,
    val token: String
)

internal data class ActiveDanmakuGroupRef(
    val id: Int,
    val group: ActiveDanmakuGroup
)

internal class VodDanmakuMergeState {
    private val lock = Any()
    private val activeGroups = LinkedHashMap<Int, ActiveDanmakuGroup>()
    private val groupsByExactText = mutableMapOf<DanmakuTextIndexKey, LinkedHashSet<Int>>()
    private val groupsByTextChar = mutableMapOf<DanmakuTextCharIndexKey, LinkedHashSet<Int>>()
    private val groupsByExactPinyin = mutableMapOf<DanmakuPinyinIndexKey, LinkedHashSet<Int>>()
    private val groupsByPinyinToken = mutableMapOf<DanmakuPinyinTokenIndexKey, LinkedHashSet<Int>>()
    private var nextGroupId = 0
    internal var lastProcessedSegmentIndex: Int? = null

    internal fun putGroup(group: ActiveDanmakuGroup) {
        synchronized(lock) {
            val groupId = nextGroupId++
            activeGroups[groupId] = group
            indexAnalysis(groupId, group.type, group.analysis)
        }
    }

    internal fun addToGroup(
        groupRef: ActiveDanmakuGroupRef,
        danmaku: DanmakuData,
        analysis: DanmakuTextAnalysis
    ) {
        synchronized(lock) {
            if (activeGroups[groupRef.id] !== groupRef.group) return
            if (groupRef.group.add(danmaku, analysis)) {
                indexAnalysis(groupRef.id, groupRef.group.type, analysis)
            }
        }
    }

    internal fun findExactTextGroup(type: Int, normalized: String): ActiveDanmakuGroupRef? =
        synchronized(lock) {
            if (normalized.isBlank()) return@synchronized null
            findFirstActiveGroup(groupsByExactText[DanmakuTextIndexKey(type, normalized)])
        }

    internal fun findExactPinyinGroup(type: Int, tokens: List<String>): ActiveDanmakuGroupRef? =
        synchronized(lock) {
            if (tokens.size < 2) return@synchronized null
            findFirstActiveGroup(groupsByExactPinyin[DanmakuPinyinIndexKey(type, tokens)])
        }

    internal fun textCandidateGroups(
        type: Int,
        analysis: DanmakuTextAnalysis
    ): List<ActiveDanmakuGroupRef> =
        synchronized(lock) {
            if (analysis.normalized.isBlank()) return@synchronized emptyList()
            val candidateIds = LinkedHashSet<Int>()
            val minLength = maxOf(1, analysis.normalized.length - MAX_TEXT_EDIT_DISTANCE)
            val maxLength = analysis.normalized.length + MAX_TEXT_EDIT_DISTANCE
            analysis.distinctTextChars.forEach { char ->
                for (length in minLength..maxLength) {
                    groupsByTextChar[DanmakuTextCharIndexKey(type, length, char)]
                        ?.let(candidateIds::addAll)
                }
            }
            activeGroupRefs(candidateIds)
        }

    internal fun pinyinCandidateGroups(
        type: Int,
        analysis: DanmakuTextAnalysis
    ): List<ActiveDanmakuGroupRef> =
        synchronized(lock) {
            val tokens = analysis.pinyinTokens
            if (tokens.size < 2) return@synchronized emptyList()
            val candidateIds = LinkedHashSet<Int>()
            val minTokenCount = maxOf(2, tokens.size - MAX_PINYIN_EDIT_DISTANCE)
            val maxTokenCount = tokens.size + MAX_PINYIN_EDIT_DISTANCE
            analysis.distinctPinyinTokens.forEach { token ->
                for (tokenCount in minTokenCount..maxTokenCount) {
                    groupsByPinyinToken[DanmakuPinyinTokenIndexKey(type, tokenCount, token)]
                        ?.let(candidateIds::addAll)
                }
            }
            activeGroupRefs(candidateIds)
        }

    internal fun isEmpty(): Boolean = synchronized(lock) {
        activeGroups.isEmpty()
    }

    internal fun takeGroupsBefore(
        thresholdSeconds: Float
    ): List<ActiveDanmakuGroup> = synchronized(lock) {
        if (activeGroups.isEmpty()) return emptyList()

        val groups = mutableListOf<ActiveDanmakuGroup>()
        val iterator = activeGroups.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.firstTime >= thresholdSeconds) break
            groups += entry.value
            iterator.remove()
            removeGroupFromIndexes(entry.key, entry.value)
        }
        groups
    }

    internal fun takeAllGroups(): List<ActiveDanmakuGroup> = synchronized(lock) {
        if (activeGroups.isEmpty()) return emptyList()
        val groups = activeGroups.values.toList()
        activeGroups.clear()
        clearIndexes()
        groups
    }

    fun clear() {
        synchronized(lock) {
            activeGroups.clear()
            clearIndexes()
            nextGroupId = 0
            lastProcessedSegmentIndex = null
        }
    }

    private fun indexAnalysis(groupId: Int, type: Int, analysis: DanmakuTextAnalysis) {
        if (analysis.normalized.isNotBlank()) {
            groupsByExactText.addIndex(
                DanmakuTextIndexKey(type, analysis.normalized),
                groupId
            )
            analysis.distinctTextChars.forEach { char ->
                groupsByTextChar.addIndex(
                    DanmakuTextCharIndexKey(type, analysis.normalized.length, char),
                    groupId
                )
            }
        }

        val pinyinTokens = analysis.pinyinTokens
        if (pinyinTokens.size >= 2) {
            groupsByExactPinyin.addIndex(DanmakuPinyinIndexKey(type, pinyinTokens), groupId)
            analysis.distinctPinyinTokens.forEach { token ->
                groupsByPinyinToken.addIndex(
                    DanmakuPinyinTokenIndexKey(type, pinyinTokens.size, token),
                    groupId
                )
            }
        }
    }

    private fun removeGroupFromIndexes(groupId: Int, group: ActiveDanmakuGroup) {
        group.analyses().forEach { analysis ->
            if (analysis.normalized.isNotBlank()) {
                groupsByExactText.removeIndex(
                    DanmakuTextIndexKey(group.type, analysis.normalized),
                    groupId
                )
                analysis.distinctTextChars.forEach { char ->
                    groupsByTextChar.removeIndex(
                        DanmakuTextCharIndexKey(
                            group.type,
                            analysis.normalized.length,
                            char
                        ),
                        groupId
                    )
                }
            }

            val pinyinTokens = analysis.pinyinTokens
            if (pinyinTokens.size >= 2) {
                groupsByExactPinyin.removeIndex(
                    DanmakuPinyinIndexKey(group.type, pinyinTokens),
                    groupId
                )
                analysis.distinctPinyinTokens.forEach { token ->
                    groupsByPinyinToken.removeIndex(
                        DanmakuPinyinTokenIndexKey(group.type, pinyinTokens.size, token),
                        groupId
                    )
                }
            }
        }
    }

    private fun findFirstActiveGroup(groupIds: Set<Int>?): ActiveDanmakuGroupRef? {
        if (groupIds.isNullOrEmpty()) return null
        val groupId = groupIds.minOrNull() ?: return null
        val group = activeGroups[groupId] ?: return null
        return ActiveDanmakuGroupRef(groupId, group)
    }

    private fun activeGroupRefs(candidateIds: Set<Int>): List<ActiveDanmakuGroupRef> {
        if (candidateIds.isEmpty()) return emptyList()
        return candidateIds.sorted().mapNotNull { groupId ->
            activeGroups[groupId]?.let { group -> ActiveDanmakuGroupRef(groupId, group) }
        }
    }

    private fun clearIndexes() {
        groupsByExactText.clear()
        groupsByTextChar.clear()
        groupsByExactPinyin.clear()
        groupsByPinyinToken.clear()
    }

    private fun <K> MutableMap<K, LinkedHashSet<Int>>.addIndex(key: K, groupId: Int) {
        getOrPut(key) { LinkedHashSet() }.add(groupId)
    }

    private fun <K> MutableMap<K, LinkedHashSet<Int>>.removeIndex(key: K, groupId: Int) {
        val groupIds = this[key] ?: return
        groupIds.remove(groupId)
        if (groupIds.isEmpty()) remove(key)
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

        DanmakuTextAnalyzer.preparePinyin(
            segmentDanmaku.asSequence()
                .filterNot { it.shouldBypassMerge() }
                .map { it.text }
        )
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
            val activeGroupRef = findMatchingGroup(state, danmaku, analysis)
            if (activeGroupRef != null) {
                state.addToGroup(activeGroupRef, danmaku, analysis)
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

    /**
     * Uses normalized exact-text coalescing for the foreground segment. Edit-distance and pinyin
     * matching remain enabled for background segments, but never block startup rendering.
     */
    fun processSegmentForImmediateDisplay(
        segmentDanmaku: List<DanmakuData>,
        segmentIndex: Int,
        segmentDurationMs: Long,
        state: VodDanmakuMergeState
    ): DanmakuSegmentMergeResult {
        val emittedDanmaku = flushPending(state).toMutableList()
        val groups = mutableListOf<ActiveDanmakuGroup>()
        val latestGroupByExactText = mutableMapOf<DanmakuTextIndexKey, ActiveDanmakuGroup>()
        var mergedDuplicateCount = 0

        segmentDanmaku.sortedBy { it.time }.forEach { danmaku ->
            if (danmaku.shouldBypassMerge()) {
                emittedDanmaku += MergedDanmakuEntry(
                    source = danmaku,
                    totalCount = 1
                )
                return@forEach
            }

            val analysis = DanmakuTextAnalyzer.analyze(danmaku.text)
            val exactTextKey = analysis.normalized
                .takeIf { it.isNotBlank() }
                ?.let { normalized -> DanmakuTextIndexKey(danmaku.type, normalized) }
            val activeGroup = exactTextKey
                ?.let(latestGroupByExactText::get)
                ?.takeIf { group ->
                    danmaku.time - group.firstTime <= MERGE_WINDOW_SECONDS
                }

            if (activeGroup != null) {
                activeGroup.add(danmaku, analysis)
                mergedDuplicateCount++
            } else {
                val group = ActiveDanmakuGroup(
                    source = danmaku,
                    analysis = analysis
                )
                groups += group
                if (exactTextKey != null) latestGroupByExactText[exactTextKey] = group
            }
        }

        emittedDanmaku += groups.map { group -> group.toEntry() }

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
        return state.takeGroupsBefore(currentTimeSeconds - MERGE_WINDOW_SECONDS)
            .map { group -> group.toEntry() }
    }

    private fun flushGroupsBefore(
        state: VodDanmakuMergeState,
        thresholdSeconds: Float
    ): List<MergedDanmakuEntry> {
        return state.takeGroupsBefore(thresholdSeconds)
            .map { group -> group.toEntry() }
    }

    private fun flushAll(state: VodDanmakuMergeState): List<MergedDanmakuEntry> {
        return state.takeAllGroups()
            .map { group -> group.toEntry() }
            .also {
                state.lastProcessedSegmentIndex = null
            }
    }

    private fun findMatchingGroup(
        state: VodDanmakuMergeState,
        danmaku: DanmakuData,
        analysis: DanmakuTextAnalysis
    ): ActiveDanmakuGroupRef? {
        state.findExactTextGroup(danmaku.type, analysis.normalized)?.let { return it }
        if (analysis.isPureNoise) return null

        val textCandidates = state.textCandidateGroups(danmaku.type, analysis)
        textCandidates
            .firstOrNull { groupRef -> groupRef.group.canMergeText(analysis) }
            ?.let { return it }

        val pinyinTokens = analysis.pinyinTokens
        if (pinyinTokens.size < 2) return null
        state.findExactPinyinGroup(danmaku.type, pinyinTokens)?.let { return it }
        val pinyinCandidates = state.pinyinCandidateGroups(danmaku.type, analysis)
        return pinyinCandidates
            .firstOrNull { groupRef -> groupRef.group.canMergePinyin(analysis) }
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
    private val normalizedAnalyses = mutableSetOf(analysis.normalized)
    private val textCounts = linkedMapOf(source.text to 1)
    val type: Int = source.type
    val firstTime: Float = source.time

    fun canMergeText(candidate: DanmakuTextAnalysis): Boolean {
        return analyses.any { existing ->
            DanmakuSimilarity.isTextSimilar(existing, candidate)
        }
    }

    fun canMergePinyin(candidate: DanmakuTextAnalysis): Boolean {
        return analyses.any { existing ->
            DanmakuSimilarity.isPinyinSimilar(existing, candidate)
        }
    }

    /** Returns true when this analysis adds a new lookup shape to the group. */
    fun add(danmaku: DanmakuData, analysis: DanmakuTextAnalysis): Boolean {
        peers += danmaku
        textCounts[danmaku.text] = (textCounts[danmaku.text] ?: 0) + 1
        if (!normalizedAnalyses.add(analysis.normalized)) return false

        analyses += analysis
        return true
    }

    internal fun analyses(): List<DanmakuTextAnalysis> = analyses

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

internal class DanmakuTextAnalysis(
    val normalized: String,
    val isPureNoise: Boolean,
    pinyinTokensProvider: () -> List<String>
) {
    val distinctTextChars: Set<Char> by lazy(LazyThreadSafetyMode.NONE) {
        normalized.toSet()
    }
    val pinyinTokens: List<String> by lazy(LazyThreadSafetyMode.NONE, pinyinTokensProvider)
    val distinctPinyinTokens: Set<String> by lazy(LazyThreadSafetyMode.NONE) {
        pinyinTokens.toSet()
    }
}

private object DanmakuTextAnalyzer {
    private const val MAX_PINYIN_CACHE_SIZE = 4096
    // Supplied by the jpinyin dependency. Reading the single-character table directly avoids its
    // expensive Android cold-start initialization of the multi-pronunciation trie.
    private const val PINYIN_RESOURCE_PATH = "data/pinyin.dict"
    private const val MARKED_VOWELS = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ"
    private const val UNMARKED_VOWELS = "aeiouv"
    private val pinyinTableLock = Any()
    private val force233Regex = Regex("^23{2,}$")
    private val force666Regex = Regex("^6{3,}$")
    private val pinyinCache = object : LinkedHashMap<String, List<String>>(MAX_PINYIN_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean {
            return size > MAX_PINYIN_CACHE_SIZE
        }
    }
    private val pinyinByChar = arrayOfNulls<String>(Char.MAX_VALUE.code + 1)
    private val loadedPinyinChars = BooleanArray(Char.MAX_VALUE.code + 1)
    private val pinyinResourceBytes: ByteArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DanmakuTextAnalyzer::class.java.classLoader
            ?.getResourceAsStream(PINYIN_RESOURCE_PATH)
            ?.use { stream -> stream.readBytes() }
            ?: ByteArray(0)
    }

    fun preparePinyin(texts: Sequence<String>) {
        val requiredChars = buildSet {
            texts.forEach { text ->
                text.forEach { char ->
                    if (char.isChinese()) add(char)
                }
            }
        }
        if (requiredChars.isEmpty()) return

        synchronized(pinyinTableLock) {
            loadMissingPinyin(requiredChars)
        }
    }

    fun analyze(text: String): DanmakuTextAnalysis {
        val normalized = normalize(text)
        return DanmakuTextAnalysis(
            normalized = normalized.text,
            isPureNoise = normalized.isPureNoise,
            pinyinTokensProvider = {
                if (normalized.isPureNoise) emptyList() else pinyinTokens(normalized.text)
            }
        )
    }

    private fun normalize(text: String): NormalizedText {
        val halfWidth = buildString(text.length) {
            text.forEach { char -> append(char.toHalfWidth()) }
        }
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
                pinyinToken(char)
            }
            .filter { it.isNotBlank() }
        val result = if (tokens.size >= 2) tokens else emptyList()

        synchronized(pinyinCache) {
            pinyinCache[normalized] = result
        }
        return result
    }

    private fun pinyinToken(char: Char): String? {
        return synchronized(pinyinTableLock) {
            if (!loadedPinyinChars[char.code]) loadMissingPinyin(setOf(char))
            pinyinByChar[char.code]
        }
    }

    private fun loadMissingPinyin(requiredChars: Set<Char>) {
        val missingChars = requiredChars
            .filterTo(mutableSetOf()) { char -> !loadedPinyinChars[char.code] }
        if (missingChars.isEmpty()) return

        val bytes = pinyinResourceBytes
        var lineStart = 0
        while (lineStart < bytes.size && missingChars.isNotEmpty()) {
            var lineEnd = lineStart
            while (lineEnd < bytes.size && bytes[lineEnd] != '\n'.code.toByte()) {
                lineEnd++
            }

            var separatorIndex = lineStart
            while (
                separatorIndex < lineEnd &&
                bytes[separatorIndex] != '='.code.toByte()
            ) {
                separatorIndex++
            }
            val char = decodeUtf8Char(bytes, lineStart, separatorIndex)
            if (char != null && char in missingChars && separatorIndex < lineEnd) {
                val firstPinyinStart = separatorIndex + 1
                var firstPinyinEnd = firstPinyinStart
                while (
                    firstPinyinEnd < lineEnd &&
                    bytes[firstPinyinEnd] != ','.code.toByte() &&
                    bytes[firstPinyinEnd] != '\r'.code.toByte()
                ) {
                    firstPinyinEnd++
                }
                pinyinByChar[char.code] = bytes
                    .decodeToString(firstPinyinStart, firstPinyinEnd)
                    .removePinyinTone()
                missingChars.remove(char)
            }

            lineStart = lineEnd + 1
        }
        requiredChars.forEach { char -> loadedPinyinChars[char.code] = true }
    }

    private fun decodeUtf8Char(bytes: ByteArray, startIndex: Int, endIndex: Int): Char? {
        if (startIndex >= endIndex) return null
        val first = bytes[startIndex].toInt() and 0xFF
        val codePoint = when {
            first and 0x80 == 0 -> first
            first and 0xE0 == 0xC0 && startIndex + 1 < endIndex -> {
                ((first and 0x1F) shl 6) or (bytes[startIndex + 1].toInt() and 0x3F)
            }
            first and 0xF0 == 0xE0 && startIndex + 2 < endIndex -> {
                ((first and 0x0F) shl 12) or
                    ((bytes[startIndex + 1].toInt() and 0x3F) shl 6) or
                    (bytes[startIndex + 2].toInt() and 0x3F)
            }
            else -> return null
        }
        return codePoint.takeIf { it <= Char.MAX_VALUE.code }?.toChar()
    }

    private fun String.removePinyinTone(): String = buildString(length) {
        this@removePinyinTone.forEach { char ->
            val markedIndex = MARKED_VOWELS.indexOf(char)
            append(
                when {
                    markedIndex >= 0 -> UNMARKED_VOWELS[markedIndex / 4]
                    char == 'ü' -> 'v'
                    else -> char
                }
            )
        }
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
        return this == '\u3007' ||
            this in '\u3400'..'\u4DBF' ||
            this in '\u4E00'..'\u9FFF' ||
            this in '\uF900'..'\uFAFF'
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
    fun isTextSimilar(
        existing: DanmakuTextAnalysis,
        candidate: DanmakuTextAnalysis
    ): Boolean {
        if (existing.normalized.isBlank() || candidate.normalized.isBlank()) return false
        if (existing.normalized == candidate.normalized) return true
        if (existing.isPureNoise || candidate.isPureNoise) return false
        return isTextEditDistanceSimilar(existing.normalized, candidate.normalized)
    }

    fun isPinyinSimilar(
        existing: DanmakuTextAnalysis,
        candidate: DanmakuTextAnalysis
    ): Boolean {
        if (existing.isPureNoise || candidate.isPureNoise) return false
        return arePinyinTokensSimilar(existing.pinyinTokens, candidate.pinyinTokens)
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

    private fun arePinyinTokensSimilar(left: List<String>, right: List<String>): Boolean {
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
