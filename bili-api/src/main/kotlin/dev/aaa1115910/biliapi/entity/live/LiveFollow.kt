package dev.aaa1115910.biliapi.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播关注列表响应
 */
@Serializable
data class LiveFollowResponse(
    val code: Int,
    val message: String,
    val data: LiveFollowData = LiveFollowData()
)

@Serializable
data class LiveFollowData(
    val title: String = "",
    @SerialName("list") val rooms: List<LiveFollowRoom> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("total_page") val totalPage: Int = 0,
    @SerialName("live_count") val liveCount: Int = 0
)

@Serializable
data class LiveFollowRoom(
    @SerialName("roomid") val roomId: Int = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val face: String = "",
    @SerialName("user_cover") val userCover: String = "",
    @SerialName("system_cover") val systemCover: String = "",
    val cover: String = "",
    @SerialName("online") val online: Int = 0,
    @SerialName("live_status") val liveStatus: Int = 0,
    @SerialName("parent_id") val parentId: Int = 0,
    @SerialName("parent_name") val parentName: String = "",
    @SerialName("area_id") val areaId: Int = 0,
    @SerialName("area_name") val areaName: String = "",
    @SerialName("watched_show") val watchedShow: WatchedShow? = null
) {
    fun toLiveRoomItem(): LiveRoomItem {
        return LiveRoomItem(
            roomId = roomId,
            uid = uid,
            title = title,
            uname = uname,
            online = online,
            cover = cover,
            userCover = userCover,
            systemCover = systemCover,
            face = face,
            parentId = parentId,
            parentName = parentName,
            areaId = areaId,
            areaName = areaName,
            watchedShow = watchedShow
        )
    }
}
