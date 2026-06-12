package dev.aaa1115910.biliapi.entity.message

data class DirectMessagePage(
    val sessions: List<DirectMessageSession>,
    val hasMore: Boolean,
    val nextCursor: Long? = null,
    val nextOffsets: Map<Int, DirectMessageOffset> = emptyMap(),
    val actions: List<DirectMessageAction> = emptyList(),
    val outsideActions: List<DirectMessageAction> = emptyList()
)

data class DirectMessageOffset(
    val normalOffset: Long,
    val topOffset: Long
)

data class DirectMessageAction(
    val title: String,
    val url: String,
    val type: Int,
    val hasRedDot: Boolean
)

data class DirectMessageFeedUnread(
    val reply: Int = 0,
    val at: Int = 0,
    val like: Int = 0,
    val sysMsg: Int = 0
)

data class DirectMessageSession(
    val talkerId: Long,
    val name: String,
    val face: String,
    val summary: String,
    val timestampMicros: Long,
    val unreadCount: Int,
    val maxSeqno: Long,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val isFollowed: Boolean,
    val isLive: Boolean
)

data class DirectMessageHistoryPage(
    val messages: List<DirectMessage>,
    val hasMore: Boolean,
    val minSeqno: Long?,
    val maxSeqno: Long?,
    val emotes: List<DirectMessageEmote> = emptyList()
)

data class DirectMessage(
    val senderUid: Long,
    val receiverId: Long,
    val msgType: Int,
    val content: DirectMessageContent,
    val rawContent: String,
    val msgSeqno: Long,
    val msgKey: Long,
    val timestampSeconds: Long,
    val status: Int,
    val source: Int
)

data class DirectMessageImageDraft(
    val url: String,
    val width: Int,
    val height: Int,
    val imageType: String,
    val size: Double
)

data class DirectMessageEmote(
    val text: String,
    val url: String,
    val size: Int
)

sealed interface DirectMessageContent {
    data class Text(val text: String) : DirectMessageContent

    data class Image(
        val url: String,
        val width: Int,
        val height: Int
    ) : DirectMessageContent

    data class Card(
        val title: String,
        val subtitle: String,
        val cover: String,
        val badge: String,
        val jumpUrl: String
    ) : DirectMessageContent

    data class Notice(val text: String) : DirectMessageContent

    data class Unsupported(val text: String) : DirectMessageContent
}
