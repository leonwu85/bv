package dev.aaa1115910.bv.player.autoplay

import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.repository.InteractivePlaybackContext
import dev.aaa1115910.bv.util.formatPubTimeString

internal fun VideoDetail.toRelatedVideoCardDataList(): List<VideoCardData> {
    return relatedVideos.map {
        VideoCardData(
            avid = it.aid,
            title = it.title,
            cover = it.cover,
            upName = it.author?.name ?: "",
            time = it.duration * 1000L,
            play = it.view,
            danmaku = it.danmaku,
            jumpToSeason = it.jumpToSeason,
            epId = it.epid,
            pubTime = it.pubTime,
            upId = it.author?.mid ?: 0L,
            upFace = it.author?.face ?: "",
            isChargingArc = it.isChargingArchive,
            badgeText = it.chargingArchiveBadge
        )
    }
}

internal fun VideoDetail.toInteractivePlaybackContextOrNull(): InteractivePlaybackContext? {
    val graphVersion = interactiveGraphVersion
    if (!isInteractive || bvid.isBlank() || graphVersion == null) return null
    return InteractivePlaybackContext(
        bvid = bvid,
        graphVersion = graphVersion,
    )
}

internal fun VideoDetail.toVideoListForTargetCid(targetCid: Long = cid): List<VideoListItem> {
    if (interactiveNodes.isNotEmpty()) {
        return interactiveNodes.mapIndexed { index, node ->
            VideoListInteractiveNode(
                aid = aid,
                cid = node.cid,
                title = title,
                partTitle = node.title,
                index = index,
                nodeId = node.nodeId,
                edgeId = node.edgeId,
                startPos = node.startPos,
                isCurrent = node.isCurrent,
            )
        }
    }

    val currentSeason = ugcSeason
    if (currentSeason != null) {
        val sectionIndex = currentSeason.sections.indexOfFirst { section ->
            section.episodes.any { episode ->
                episode.cid == targetCid || episode.pages.any { page -> page.cid == targetCid }
            }
        }.takeIf { it >= 0 } ?: 0

        return buildUgcSeasonVideoList(sectionIndex)
    }

    return pages.mapIndexed { index, videoPage ->
        VideoListPart(
            aid = aid,
            cid = videoPage.cid,
            title = title,
            partTitle = videoPage.title,
            index = index,
            duration = videoPage.duration,
        )
    }
}

internal fun VideoDetail.toPreparedAutoPlayTransitionContext(
    targetCid: Long,
    preferredPartTitle: String? = null,
): PreparedAutoPlayTransitionContext? {
    val targetPage = pages.find { it.cid == targetCid } ?: pages.firstOrNull() ?: return null

    return PreparedAutoPlayTransitionContext(
        aid = aid,
        cid = targetPage.cid,
        epid = null,
        seasonId = null,
        title = title,
        partTitle = preferredPartTitle ?: targetPage.title,
        cover = cover,
        isVerticalVideo = targetPage.dimension.isVertical,
        playerIconIdle = playerIcon?.idle ?: "",
        playerIconMoving = playerIcon?.moving ?: "",
        play = stat.view,
        danmaku = stat.danmaku,
        like = stat.like,
        coin = stat.coin,
        favorite = stat.favorite,
        upName = author.name,
        upId = author.mid,
        upFace = author.face,
        pubTime = publishDate.formatPubTimeString(),
        availableVideoList = toVideoListForTargetCid(targetPage.cid),
        relatedVideos = toRelatedVideoCardDataList(),
        interactivePlaybackContext = toInteractivePlaybackContextOrNull(),
    )
}

private fun VideoDetail.buildUgcSeasonVideoList(sectionIndex: Int): List<VideoListItem> {
    val currentSeason = ugcSeason ?: return emptyList()
    val result = mutableListOf<VideoListItem>()

    currentSeason.sections[sectionIndex].episodes.forEachIndexed { episodeIndex, episode ->
        if (episode.pages.size == 1) {
            episode.pages.forEach { page ->
                result.add(
                    VideoListUgcEpisode(
                        aid = episode.aid,
                        cid = page.cid,
                        title = episode.title,
                        partTitle = "",
                        index = episodeIndex,
                        cover = episode.cover,
                        duration = page.duration,
                        viewCount = episode.viewCount,
                        danmakuCount = episode.danmakuCount,
                    )
                )
            }
        } else {
            result.add(
                VideoListUgcEpisodeTitle(
                    title = episode.title,
                    index = episodeIndex,
                    cover = episode.cover,
                    duration = episode.duration,
                    viewCount = episode.viewCount,
                    danmakuCount = episode.danmakuCount,
                )
            )
            episode.pages.forEachIndexed { pageIndex, page ->
                result.add(
                    VideoListPart(
                        aid = episode.aid,
                        cid = page.cid,
                        title = episode.title,
                        partTitle = page.title,
                        index = pageIndex,
                        duration = page.duration,
                    )
                )
            }
        }
    }

    return result
}
