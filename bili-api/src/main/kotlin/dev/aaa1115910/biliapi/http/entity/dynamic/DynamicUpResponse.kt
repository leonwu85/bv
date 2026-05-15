package dev.aaa1115910.biliapi.http.entity.dynamic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DynamicFollowUpData(
    @SerialName("live_users")
    val liveUsers: LiveUsers? = null,
    @SerialName("up_list")
    val upList: UpList? = null
) {
    @Serializable
    data class LiveUsers(
        val count: Int = 0,
        val group: String? = null,
        val items: List<LiveUserItem> = emptyList()
    )

    @Serializable
    data class UpList(
        @SerialName("has_more")
        val hasMore: Boolean = false,
        val offset: String? = null,
        val items: List<UpItem> = emptyList()
    )
}

@Serializable
data class DynamicUpListData(
    @SerialName("has_more")
    val hasMore: Boolean = false,
    val offset: String? = null,
    val items: List<UpItem> = emptyList()
)

@Serializable
data class UpItem(
    val face: String? = null,
    @SerialName("has_update")
    val hasUpdate: Boolean = false,
    val mid: Long = 0,
    val uname: String? = null
)

@Serializable
data class LiveUserItem(
    val face: String? = null,
    @SerialName("has_update")
    val hasUpdate: Boolean = false,
    @SerialName("is_reserve_recall")
    val isReserveRecall: Boolean = false,
    @SerialName("jump_url")
    val jumpUrl: String? = null,
    val mid: Long = 0,
    @SerialName("room_id")
    val roomId: Long = 0,
    val title: String? = null,
    val uname: String? = null
)
