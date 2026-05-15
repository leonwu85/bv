package dev.aaa1115910.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveRoomInfoH5Data(
    @SerialName("room_info")
    val roomInfo: LiveRoomInfoH5RoomInfo? = null,
    @SerialName("anchor_info")
    val anchorInfo: LiveRoomInfoH5AnchorInfo? = null,
    @SerialName("watched_show")
    val watchedShow: LiveRoomInfoH5WatchedShow? = null
)

@Serializable
data class LiveRoomInfoH5RoomInfo(
    val uid: Long = 0,
    val title: String = "",
    val cover: String = "",
    @SerialName("app_background")
    val appBackground: String = ""
)

@Serializable
data class LiveRoomInfoH5AnchorInfo(
    @SerialName("base_info")
    val baseInfo: LiveRoomInfoH5BaseInfo? = null
)

@Serializable
data class LiveRoomInfoH5BaseInfo(
    val uname: String = "",
    val face: String = ""
)

@Serializable
data class LiveRoomInfoH5WatchedShow(
    @SerialName("text_large")
    val textLarge: String = ""
)
