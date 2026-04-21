package dev.aaa1115910.bv.player.autoplay

import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.repository.InteractivePlaybackContext

sealed interface AutoPlayCandidate {
    val aid: Long

    data class CrossVideoPart(
        val item: VideoListItemData,
    ) : AutoPlayCandidate {
        override val aid: Long
            get() = item.aid
    }

    data class RelatedVideo(
        val video: VideoCardData,
    ) : AutoPlayCandidate {
        override val aid: Long
            get() = video.avid
    }
}

internal data class PreparedAutoPlayTransitionContext(
    val aid: Long,
    val cid: Long,
    val epid: Int? = null,
    val seasonId: Int? = null,
    val title: String,
    val partTitle: String,
    val isVerticalVideo: Boolean,
    val playerIconIdle: String = "",
    val playerIconMoving: String = "",
    val play: Long = 0L,
    val danmaku: Int = 0,
    val like: Int = 0,
    val coin: Int = 0,
    val favorite: Int = 0,
    val upName: String = "",
    val upId: Long = 0L,
    val upFace: String = "",
    val pubTime: String = "",
    val availableVideoList: List<VideoListItem>,
    val relatedVideos: List<VideoCardData>,
    val interactivePlaybackContext: InteractivePlaybackContext? = null,
)

internal data class PreparedAutoPlayTarget(
    val candidate: AutoPlayCandidate,
    val transitionContext: PreparedAutoPlayTransitionContext? = null,
    val playData: PlayData? = null,
    val playDataFetchedAtMs: Long = 0L,
    val isSupported: Boolean = true,
)