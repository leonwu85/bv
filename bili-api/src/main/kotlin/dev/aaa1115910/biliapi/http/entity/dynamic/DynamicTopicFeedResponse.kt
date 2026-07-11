package dev.aaa1115910.biliapi.http.entity.dynamic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DynamicTopicFeedResponse(
    @SerialName("topic_card_list")
    val topicCardList: DynamicTopicCardList? = null
)

@Serializable
data class DynamicTopicCardList(
    @SerialName("has_more")
    val hasMore: Boolean = false,
    val offset: String = "",
    val items: List<DynamicTopicCardItem> = emptyList()
)

@Serializable
data class DynamicTopicCardItem(
    @SerialName("dynamic_card_item")
    val dynamicCardItem: DynamicItem? = null,
    @SerialName("fold_card_item")
    val foldCardItem: DynamicTopicFoldCard? = null,
    @SerialName("topic_type")
    val topicType: String? = null
)

@Serializable
data class DynamicTopicFoldCard(
    @SerialName("fold_count")
    val foldCount: Int = 0,
    @SerialName("fold_desc")
    val foldDesc: String = ""
)
