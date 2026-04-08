package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoPlayerInfo(
    val interaction: Interaction? = null,
    @SerialName("last_play_time")
    val lastPlayTime: Int = 0,
    @SerialName("last_play_cid")
    val lastPlayCid: Long = 0L,
) {
    @Serializable
    data class Interaction(
        @SerialName("graph_version")
        val graphVersion: Int? = null,
    )
}