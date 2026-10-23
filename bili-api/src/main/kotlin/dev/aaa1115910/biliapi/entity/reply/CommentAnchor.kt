package dev.aaa1115910.biliapi.entity.reply

/** The subject IDs are optional: ordinary content links resolve them from their source. */
data class CommentAnchor(
    val rootId: Long,
    val replyId: Long? = null,
    val oid: Long? = null,
    val type: Long? = null,
)
