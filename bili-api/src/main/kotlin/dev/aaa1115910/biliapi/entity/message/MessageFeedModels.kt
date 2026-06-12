package dev.aaa1115910.biliapi.entity.message

enum class MessageFeedType(val title: String) {
    Reply("回复我的"),
    At("@我"),
    Like("收到的赞"),
    System("系统通知")
}

data class MessageFeedPage(
    val items: List<MessageFeedItem>,
    val hasMore: Boolean,
    val cursorId: Long? = null,
    val cursorTime: Long? = null
)

data class MessageFeedItem(
    val id: String,
    val type: MessageFeedType,
    val section: String = "",
    val userMid: Long? = null,
    val username: String = "",
    val avatar: String = "",
    val title: String = "",
    val body: String = "",
    val quote: String = "",
    val image: String = "",
    val timeText: String = "",
    val timestampSeconds: Long? = null,
    val jumpUri: String = "",
    val deleteType: Int? = null
)
