package dev.aaa1115910.biliapi.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播推荐响应
 */
@Serializable
data class LiveFeedResponse(
    val code: Int,
    val message: String,
    val data: LiveFeedData = LiveFeedData()
)

@Serializable
data class LiveFeedData(
    @SerialName("card_list") val cardList: List<LiveFeedCard> = emptyList(),
    @SerialName("has_more") val hasMore: Int = 0
)

@Serializable
data class LiveFeedCard(
    @SerialName("card_type") val cardType: String? = null,
    @SerialName("card_data") val cardData: LiveFeedCardData? = null
)

@Serializable
data class LiveFeedCardData(
    @SerialName("small_card_v1") val smallCardV1: LiveFeedSmallCard? = null,
    @SerialName("second_card_v1") val secondCardV1: LiveFeedSecondCard? = null,
    @SerialName("area_entrance_v1") val areaEntranceV1: LiveFeedAreaEntrance? = null,
    @SerialName("my_idol_v1") val myIdolV1: LiveFeedMyIdol? = null
)

@Serializable
data class LiveFeedSmallCard(
    @SerialName("id") val id: Int = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val face: String = "",
    val cover: String = "",
    val online: Int = 0,
    @SerialName("area_name") val areaName: String = "",
    @SerialName("watched_show") val watchedShow: WatchedShow? = null
) {
    fun toLiveRoomItem(): LiveRoomItem {
        return LiveRoomItem(
            roomId = id,
            uid = uid,
            title = title,
            uname = uname,
            online = online,
            cover = cover,
            userCover = cover,
            systemCover = "",
            face = face,
            parentId = 0,
            parentName = "",
            areaId = 0,
            areaName = areaName,
            watchedShow = watchedShow
        )
    }
}

@Serializable
data class LiveFeedSecondCard(
    @SerialName("roomid") val roomId: Int = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val face: String = "",
    val cover: String = "",
    val online: Int = 0,
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
            userCover = cover,
            systemCover = "",
            face = face,
            parentId = 0,
            parentName = "",
            areaId = 0,
            areaName = areaName,
            watchedShow = watchedShow
        )
    }
}

@Serializable
data class LiveFeedAreaEntrance(
    val list: List<LiveFeedAreaItem> = emptyList()
)

@Serializable
data class LiveFeedAreaItem(
    val id: Int = 0,
    val title: String = "",
    val pic: String = ""
)

@Serializable
data class LiveFeedMyIdol(
    val list: List<LiveFeedMyIdolItem> = emptyList()
)

@Serializable
data class LiveFeedMyIdolItem(
    @SerialName("roomid") val roomId: Int = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val face: String = "",
    val cover: String = "",
    val online: Int = 0,
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
            userCover = cover,
            systemCover = "",
            face = face,
            parentId = 0,
            parentName = "",
            areaId = 0,
            areaName = areaName,
            watchedShow = watchedShow
        )
    }
}
