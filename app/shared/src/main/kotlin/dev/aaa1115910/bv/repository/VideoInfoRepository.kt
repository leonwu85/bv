package dev.aaa1115910.bv.repository

import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.player.entity.VideoListItem
import org.koin.core.annotation.Single

data class InteractivePlaybackContext(
    val bvid: String,
    val graphVersion: Int,
)

@Single
class VideoInfoRepository {
    val videoList = mutableListOf<VideoListItem>()
    val relatedVideos = mutableListOf<VideoCardData>()
    var interactivePlaybackContext: InteractivePlaybackContext? = null

    fun updateInteractivePlaybackContext(bvid: String, graphVersion: Int?) {
        interactivePlaybackContext = if (bvid.isNotBlank() && graphVersion != null) {
            InteractivePlaybackContext(
                bvid = bvid,
                graphVersion = graphVersion,
            )
        } else {
            null
        }
    }

    fun clearInteractivePlaybackContext() {
        interactivePlaybackContext = null
    }
}
