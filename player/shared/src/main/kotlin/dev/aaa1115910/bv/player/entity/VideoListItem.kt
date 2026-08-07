package dev.aaa1115910.bv.player.entity

interface VideoListItem

open class VideoListItemData(
    open val aid: Long,
    open val cid: Long? = null,
    open val epid: Int? = null,
    open val seasonId: Int? = null,
    open val title: String,
    open val partTitle: String = "",
    open val index: Int,
    open val cover: String? = null,
    open val duration: Int = 0,
) : VideoListItem

data class VideoListPart(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    override val duration: Int = 0,
) : VideoListItemData(aid, cid, epid, seasonId, title, partTitle, index, duration = duration)

data class VideoListUgcEpisode(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    override val cover: String? = null,
    override val duration: Int = 0,
    val viewCount: Long = 0,
    val danmakuCount: Int = 0,
) : VideoListItemData(aid, cid, epid, seasonId, title, partTitle, index, cover, duration)

data class VideoListUgcEpisodeTitle(
    val index: Int,
    val title: String,
    val cover: String? = null,
    val duration: Int = 0,
    val viewCount: Long = 0,
    val danmakuCount: Int = 0,
) : VideoListItem

data class VideoListInteractiveNode(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    val nodeId: Long,
    val edgeId: Long? = null,
    val startPos: Int? = null,
    val isCurrent: Boolean = false,
) : VideoListItemData(aid, cid, epid, seasonId, title, partTitle, index)

data class VideoListPgcEpisode(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    override val cover: String? = null,
    override val duration: Int = 0,
    val viewCount: Long = 0,
    val danmakuCount: Int = 0,
) : VideoListItemData(aid, cid, epid, seasonId, title, partTitle, index, cover, duration)
