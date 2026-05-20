package dev.aaa1115910.biliapi.entity.video

import bilibili.app.view.v1.authorOrNull
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.biliapi.entity.user.Author

data class RelatedVideo(
    val aid: Long,
    val cover: String,
    val title: String,
    val duration: Int,
    val author: Author?,
    val jumpToSeason: Boolean,
    val epid: Int?,
    val view: Long,
    val danmaku: Int,
    val pubTime: String? = null,
    val isChargingArchive: Boolean = false,
    val chargingArchiveBadge: String = ""
) {
    companion object {
        fun fromRelate(relate: bilibili.app.view.v1.Relate): RelatedVideo {
            val chargingBadge = listOf(
                relate.badge,
                relate.badgeStyle.text,
                relate.rcmdReasonStyle.text
            ).firstOrNull { it.isChargingBadgeText() }.orEmpty()
            return RelatedVideo(
                aid = relate.aid,
                cover = relate.pic,
                title = relate.title,
                duration = relate.duration.toInt(),
                author = relate.authorOrNull?.let { Author.fromAuthor(it) }
                    ?: relate.desc?.let { Author(0, it, "") },
                jumpToSeason = relate.goto.needJumpToSeason(),
                epid = if (relate.goto.needJumpToSeason()) relate.uri.substringBeforeLast("?")
                    .substringAfterLast("/ep").toInt() else null,
                view = relate.stat.view,
                danmaku = relate.stat.danmaku,
                isChargingArchive = chargingBadge.isNotBlank(),
                chargingArchiveBadge = chargingBadge
            )
        }

        fun fromRelate(relate: dev.aaa1115910.biliapi.http.entity.video.RelatedVideoInfo) =
            RelatedVideo(
                aid = relate.aid,
                cover = relate.pic,
                title = relate.title,
                duration = relate.duration,
                author = relate.owner.let { Author.fromVideoOwner(it) },
                jumpToSeason = false,
                epid = null,
                view = relate.stat.view,
                danmaku = relate.stat.danmaku,
                pubTime = relate.pubdate.toLong().toSmartDate(),
                isChargingArchive = relate.isChargingArchive || relate.chargingPay?.level != null,
                chargingArchiveBadge = if (relate.isChargingArchive || relate.chargingPay?.level != null) {
                    "充电专属"
                } else {
                    ""
                }
            )
    }
}

private fun String.needJumpToSeason() = this.contains("bangumi_ep") || this.contains("special")
private fun String.isChargingBadgeText() = contains("充电") || contains("限时免费")
