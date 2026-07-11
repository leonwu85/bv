package dev.aaa1115910.biliapi.entity.user

data class DynamicTopicFeed(
    val items: List<DynamicTopicFeedItem> = emptyList(),
    val hasMore: Boolean = false,
    val offset: String = ""
)

sealed interface DynamicTopicFeedItem {
    data class DynamicCard(
        val dynamic: DynamicItem
    ) : DynamicTopicFeedItem

    data class FoldCard(
        val count: Int,
        val description: String
    ) : DynamicTopicFeedItem
}
