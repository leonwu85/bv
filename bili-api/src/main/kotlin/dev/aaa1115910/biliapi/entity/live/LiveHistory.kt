package dev.aaa1115910.biliapi.entity.live

import dev.aaa1115910.biliapi.http.entity.history.HistoryItem as WebHistoryItem

data class LiveHistoryPage(
    val items: List<LiveHistoryItem>,
    val cursor: LiveHistoryCursor = LiveHistoryCursor(),
    val hasMore: Boolean
)

data class LiveHistoryCursor(
    val max: Long = 0L,
    val viewAt: Long = 0L,
    val business: String = ""
)

data class LiveHistoryItem(
    val roomId: Int,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val cover: String = "",
    val face: String = "",
    val areaName: String = "",
    val liveStatus: Int = 0,
    val viewAt: Long = 0L
)

fun WebHistoryItem.toLiveHistoryItem(): LiveHistoryItem? {
    if (history.business != "live") return null

    val displayTitle = buildList {
        if (title.isNotBlank()) add(title)
        if (longTitle.isNotBlank() && longTitle != title) add(longTitle)
    }.joinToString(separator = "\n")

    return LiveHistoryItem(
        roomId = history.oid.toInt(),
        uid = authorMid,
        title = displayTitle,
        uname = authorName,
        cover = cover.ifBlank { covers?.firstOrNull().orEmpty() },
        face = authorFace,
        areaName = tagName,
        liveStatus = liveStatus,
        viewAt = viewAt
    )
}