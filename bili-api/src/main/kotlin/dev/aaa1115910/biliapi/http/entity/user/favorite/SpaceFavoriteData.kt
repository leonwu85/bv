package dev.aaa1115910.biliapi.http.entity.user.favorite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpaceFavoriteData(
    val id: Int? = null,
    val name: String? = null,
    val mediaListResponse: SpaceFavoriteMediaListResponse? = null
)

@Serializable
data class SpaceFavoriteMediaListResponse(
    val count: Int = 0,
    val list: List<SpaceFavoriteItem> = emptyList()
)

@Serializable
data class SpaceFavoriteFolderListData(
    val count: Int = 0,
    val list: List<SpaceFavoriteItem> = emptyList(),
    @SerialName("has_more")
    val hasMore: Boolean = false
)

@Serializable
data class SpaceFavoriteItem(
    val id: Long? = null,
    @SerialName("media_id")
    val mediaId: Long? = null,
    val count: Int? = null,
    @SerialName("is_public")
    val isPublic: Int? = null,
    val fid: Long? = null,
    val mid: Long? = null,
    val attr: Int? = null,
    val title: String? = null,
    val cover: String? = null,
    val upper: SpaceFavoriteUpper? = null,
    @SerialName("cover_type")
    val coverType: Int? = null,
    val intro: String? = null,
    val ctime: Long? = null,
    val mtime: Long? = null,
    val state: Int? = null,
    @SerialName("fav_state")
    val favState: Int? = null,
    @SerialName("media_count")
    val mediaCount: Int? = null,
    @SerialName("view_count")
    val viewCount: Int? = null,
    val type: Int? = null
)

@Serializable
data class SpaceFavoriteUpper(
    val mid: Long? = null,
    val name: String? = null,
    val face: String? = null
)
