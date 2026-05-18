package dev.aaa1115910.biliapi.http.entity.dynamic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DynamicEntranceData(
    @SerialName("update_info")
    val updateInfo: DynamicEntranceUpdateInfo? = null,
    val entrance: DynamicEntrance? = null
) {
    fun unreadCount(): Int = updateInfo?.unreadCount()
        ?: entrance?.updateInfo?.unreadCount()
        ?: 0
}

@Serializable
data class DynamicEntrance(
    @SerialName("update_info")
    val updateInfo: DynamicEntranceUpdateInfo? = null
)

@Serializable
data class DynamicEntranceUpdateInfo(
    val item: DynamicEntranceUpdateItem? = null,
    val count: Int? = null
) {
    fun unreadCount(): Int = item?.count ?: count ?: 0
}

@Serializable
data class DynamicEntranceUpdateItem(
    val count: Int = 0
)
