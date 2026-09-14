package dev.aaa1115910.biliapi.entity.search

import dev.aaa1115910.biliapi.entity.BiliContentLink
import dev.aaa1115910.biliapi.repositories.SearchTypePage
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import kotlinx.serialization.json.*

/** Parse each optional module independently: a malformed promotion must not hide search results. */
fun parseSearchAggregate(data: JsonObject): SearchTypeResult {
    val videos = mutableListOf<SearchTypeResult.Video>()
    val users = mutableListOf<SearchTypeResult.User>()
    val pgcs = mutableListOf<SearchTypeResult.Pgc>()
    val activities = mutableListOf<SearchTypeResult.Activity>()
    data.array("result").forEach { module ->
        val block = module as? JsonObject ?: return@forEach
        block.array("data").forEach itemLoop@{ element ->
            val item = element as? JsonObject ?: return@itemLoop
            when (block.text("result_type")) {
                "video" -> parseAggregateVideo(item)?.let(videos::add)
                "bili_user" -> item.number("mid").takeIf { it > 0 }?.let { mid ->
                    users.add(SearchTypeResult.User(
                        mid = mid, name = item.text("uname"), avatar = imageUrl(item.text("upic")),
                        sign = item.text("usign"), fans = item.number("fans"),
                        recentVideos = item.array("res").mapNotNull { (it as? JsonObject)?.let(::parseAggregateVideo) }
                            .map { it.copy(author = item.text("uname"), upId = mid, upFace = imageUrl(item.text("upic"))) }
                    ))
                }
                "media_bangumi", "media_ft" -> item.number("season_id").takeIf { it in 1..Int.MAX_VALUE }?.let { id ->
                    pgcs.add(SearchTypeResult.Pgc(item.text("title"), imageUrl(item.text("cover")),
                        (item["media_score"] as? JsonObject)?.text("score")?.toFloatOrNull() ?: 0f, id.toInt()))
                }
                "activity" -> {
                    val url = item.text("url")
                    val target = BiliContentLink.parse(url)
                    if (target is BiliContentLink.Live || target is BiliContentLink.LiveActivity) {
                        activities.add(SearchTypeResult.Activity(item.text("title"), imageUrl(item.text("cover")), item.text("desc"), url))
                    }
                }
            }
        }
    }
    val page = data.number("page").toInt().coerceAtLeast(1)
    val videoPages = (((data["pageinfo"] as? JsonObject)?.get("video") as? JsonObject)?.get("pages") as? JsonPrimitive)?.intOrNull?.takeIf { it >= 0 }
    return SearchTypeResult(
        videos = videos.distinctBy { it.aid }, pgcs = pgcs.distinctBy { it.seasonId },
        users = users.distinctBy { it.mid }, activities = activities.distinctBy { it.url },
        page = SearchTypePage(nextPageForWeb = page + 1),
        pageSize = data.number("pagesize").toInt().takeIf { it > 0 } ?: 20,
        // A short aggregate first page does not imply a short video-only continuation.
        hasMore = videoPages?.let { it > page } ?: videos.isNotEmpty()
    )
}

private fun parseAggregateVideo(item: JsonObject): SearchTypeResult.Video? {
    val aid = item.number("aid").takeIf { it > 0 } ?: return null
    if (item.text("type") in setOf("live_room", "ketang")) return null
    val duration = item.text("duration").split(':').fold(0L) { sum, part -> sum * 60 + (part.toLongOrNull() ?: 0) }
    return SearchTypeResult.Video(
        aid = aid, bvid = item.text("bvid"), title = item.text("title"), cover = imageUrl(item.text("pic")),
        author = item.text("author"), upId = item.number("mid"), upFace = imageUrl(item.text("upic")),
        duration = duration.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(), play = item.number("play"),
        danmaku = item.number("danmaku").toInt(), pubTime = item.number("pubdate").toInt(),
        pubDate = item.number("pubdate").toInt()
    )
}

private fun JsonObject.text(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
private fun JsonObject.number(key: String): Long = text(key).toLongOrNull() ?: text(key).toDoubleOrNull()?.toLong() ?: 0
private fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())
private fun imageUrl(url: String): String = if (url.startsWith("//")) "https:$url" else url

fun SearchTypeResult.SearchTypeResultItem.searchKey(): String = when (this) {
    is SearchTypeResult.Video -> "video:$aid"
    is SearchTypeResult.User -> "user:$mid"
    is SearchTypeResult.Pgc -> "pgc:$seasonId"
    is SearchTypeResult.LiveRoom -> "live:$roomId"
    is SearchTypeResult.Article -> "article:$id"
    is SearchTypeResult.Activity -> "activity:$url"
    else -> error("Unknown search result")
}
