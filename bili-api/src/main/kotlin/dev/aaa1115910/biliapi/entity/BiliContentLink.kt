package dev.aaa1115910.biliapi.entity

import java.net.URI
import java.net.URLDecoder
import dev.aaa1115910.biliapi.util.AvBvConverter

sealed interface BiliContentLink {
    data class Video(val aid: Long, val page: Int = 1, val cid: Long? = null, val positionMs: Long? = null) : BiliContentLink
    data class Season(val episodeId: Int? = null, val seasonId: Int? = null) : BiliContentLink
    data class Live(val roomId: Int) : BiliContentLink
    data class LiveActivity(val url: String) : BiliContentLink
    data class User(val mid: Long) : BiliContentLink
    data class Dynamic(val id: String) : BiliContentLink

    companion object {
        private val av = Regex("av([1-9][0-9]*)", RegexOption.IGNORE_CASE)
        private val bv = Regex("BV1[1-9A-HJ-NP-Za-km-z]{9}")

        fun parse(input: String): BiliContentLink? = runCatching {
            val text = extractLink(input)
            videoId(text)?.let { return Video(it) }
            val uri = uri(text) ?: return null
            val host = uri.host?.lowercase() ?: return null
            val parts = uri.path.orEmpty().split('/').filter(String::isNotEmpty)
            val query = uri.rawQuery.orEmpty().split('&').mapNotNull { item ->
                val pair = item.split('=', limit = 2)
                if (pair.size != 2) null else pair[0] to URLDecoder.decode(pair[1], "UTF-8")
            }.toMap()
            fun video(id: Long) = Video(
                aid = id,
                page = query["p"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                cid = query["cid"]?.toLongOrNull()?.takeIf { it > 0 },
                positionMs = (query["start_progress"] ?: query["dm_progress"])
                    ?.toLongOrNull()?.takeIf { it >= 0 }
                    ?: query["t"]?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
                        ?.times(1000)?.toLong()
            )
            fun season(value: String?): Season? = when {
                value?.startsWith("ep") == true -> value.drop(2).toIntOrNull()?.takeIf { it > 0 }?.let { Season(episodeId = it) }
                value?.startsWith("ss") == true -> value.drop(2).toIntOrNull()?.takeIf { it > 0 }?.let { Season(seasonId = it) }
                else -> null
            }
            if (uri.scheme == "bilibili") {
                return when (host) {
                    "video" -> (parts.firstOrNull()?.let(::videoId)
                        ?: parts.firstOrNull()?.toLongOrNull()?.takeIf { it > 0 })?.let(::video)
                    "space" -> parts.firstOrNull()?.toLongOrNull()?.takeIf { it > 0 }?.let(::User)
                    "live" -> parts.lastOrNull()?.toIntOrNull()?.takeIf { it > 0 }?.let(::Live)
                    "following", "opus" -> parts.lastOrNull()?.takeIf { it.toLongOrNull()?.let { id -> id > 0 } == true }?.let(::Dynamic)
                    "pgc", "bangumi" -> season(parts.lastOrNull()) ?: when (parts.getOrNull(parts.lastIndex - 1)) {
                        "ep" -> parts.lastOrNull()?.toIntOrNull()?.takeIf { it > 0 }?.let { Season(episodeId = it) }
                        "season", "ss" -> parts.lastOrNull()?.toIntOrNull()?.takeIf { it > 0 }?.let { Season(seasonId = it) }
                        else -> null
                    }
                    else -> null
                }
            }
            if (!isBilibiliHost(host)) return null
            when (host) {
                "live.bilibili.com" -> parts.lastOrNull()?.toIntOrNull()?.takeIf { it > 0 }?.let(::Live)
                    ?: uri.takeIf { parts.firstOrNull() == "blackboard" }?.let { LiveActivity(it.toString()) }
                "space.bilibili.com" -> parts.firstOrNull()?.toLongOrNull()?.takeIf { it > 0 }?.let(::User)
                "t.bilibili.com" -> parts.firstOrNull()?.takeIf { it.toLongOrNull()?.let { id -> id > 0 } == true }?.let(::Dynamic)
                else -> when {
                    parts.firstOrNull() == "video" -> parts.getOrNull(1)?.let(::videoId)?.let(::video)
                    parts.take(2) == listOf("bangumi", "play") -> season(parts.getOrNull(2))
                    parts.firstOrNull() == "opus" -> parts.getOrNull(1)?.takeIf { it.toLongOrNull()?.let { id -> id > 0 } == true }?.let(::Dynamic)
                    else -> null
                }
            }
        }.getOrNull()

        private fun videoId(value: String): Long? = av.matchEntire(value)?.groupValues?.get(1)?.toLongOrNull()
            ?: value.takeIf { bv.matches(it) }?.let { AvBvConverter.bv2av(it).takeIf { id -> id > 0 } }

        /** Official share text may wrap a URL in a title; bare av/BV identifiers still require an exact match. */
        fun extractLink(input: String): String {
            val text = input.trim()
            return Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).find(text)?.value
                ?.trimEnd('。', '，', '！', '）', ')', ']', '】') ?: text
        }

        fun uri(input: String): URI? = runCatching {
            val text = extractLink(input)
            val uri = URI(when {
                text.startsWith("//") -> "https:$text"
                "://" !in text -> "https://$text"
                else -> text
            })
            uri.takeIf {
                it.scheme in setOf("https", "http", "bilibili") &&
                    it.rawUserInfo == null && it.port in setOf(-1, 80, 443)
            }
        }.getOrNull()

        fun isBilibiliHost(host: String): Boolean = host == "bilibili.com" || host.endsWith(".bilibili.com")
        fun isShortLink(input: String): Boolean = uri(input)?.host?.lowercase() in setOf("b23.tv", "www.b23.tv")
    }
}
