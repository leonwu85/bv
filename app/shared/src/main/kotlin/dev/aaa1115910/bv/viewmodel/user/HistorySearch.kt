package dev.aaa1115910.bv.viewmodel.user

import dev.aaa1115910.bv.entity.carddata.VideoCardData
import java.text.Normalizer
import java.util.Locale

private val historySearchHtmlTagRegex = Regex("<[^>]*>")

/**
 * Combines matches from the history pages already loaded on the device with the
 * server search response. The server endpoint can omit records which are still
 * present in the regular history feed, so local matches must not be discarded.
 */
internal fun mergeHistorySearchResults(
    query: String,
    loadedHistories: List<VideoCardData>,
    remoteResults: List<VideoCardData>
): List<VideoCardData> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return loadedHistories

    val localMatches = loadedHistories.filter { it.matchesHistorySearch(keyword) }
    if (remoteResults.isEmpty()) return localMatches

    val identities = mutableSetOf<String>()
    return buildList(localMatches.size + remoteResults.size) {
        localMatches.forEach { history ->
            if (identities.add(history.historySearchIdentity())) add(history)
        }
        remoteResults.forEach { history ->
            if (identities.add(history.historySearchIdentity())) add(history)
        }
    }
}

internal fun VideoCardData.matchesHistorySearch(keyword: String): Boolean {
    val normalizedKeyword = keyword.normalizeHistorySearchText()
    if (normalizedKeyword.isEmpty()) return true
    return title.normalizeHistorySearchText().contains(normalizedKeyword) ||
        upName.normalizeHistorySearchText().contains(normalizedKeyword) ||
        bvid.normalizeHistorySearchText().contains(normalizedKeyword)
}

/**
 * Text shown by Compose can contain zero-width characters, variation selectors,
 * or highlighting tags while looking identical to the user's query. Normalize
 * those differences before matching the visible title.
 */
internal fun String.normalizeHistorySearchText(): String {
    val normalized = Normalizer.normalize(
        replace(historySearchHtmlTagRegex, ""),
        Normalizer.Form.NFKC
    )
    return buildString(normalized.length) {
        var index = 0
        while (index < normalized.length) {
            val codePoint = Character.codePointAt(normalized, index)
            index += Character.charCount(codePoint)
            val type = Character.getType(codePoint)
            val ignored = Character.isWhitespace(codePoint) ||
                Character.isSpaceChar(codePoint) ||
                type == Character.CONTROL.toInt() ||
                type == Character.FORMAT.toInt() ||
                type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt() ||
                type == Character.ENCLOSING_MARK.toInt()
            if (!ignored) appendCodePoint(codePoint)
        }
    }.lowercase(Locale.ROOT)
}

internal fun VideoCardData.historySearchIdentity(): String {
    return when {
        historyBusiness != null && historyKid != null ->
            "history:$historyBusiness:$historyKid"
        avid > 0L -> "avid:$avid"
        bvid.isNotBlank() -> "bvid:${bvid.lowercase()}"
        else -> "metadata:$title\u0000$upName\u0000${historyViewAt ?: 0L}"
    }
}
