package dev.aaa1115910.biliapi.entity.reply

import bilibili.main.community.reply.v1.Content as GrpcContent
import bilibili.main.community.reply.v1.TranslationSwitch
import dev.aaa1115910.biliapi.http.entity.reply.CommentData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import dev.aaa1115910.biliapi.entity.Picture

data class CommentsData(
    val comments: List<Comment> = emptyList(),
    val nextPage: CommentPage = CommentPage(),
    val hasNext: Boolean,
    val vote: CommentVote? = null
) {
    companion object {
        private val pinnedReplyJson = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        fun fromCommentData(commentData: CommentData): CommentsData {
            val nextOffset = commentData.cursor.paginationReply?.nextOffset
            val pins = listOfNotNull(commentData.top.upper, commentData.top.admin, commentData.top.vote) +
                (commentData.topReplies as? JsonArray).orEmpty()
            val pinnedReplies = pins.mapNotNull { item ->
                try {
                    pinnedReplyJson.decodeFromJsonElement<CommentData.Reply>(item)
                } catch (_: SerializationException) {
                    null
                }
            }
            return CommentsData(
                comments = (pinnedReplies + commentData.replies).distinctBy { it.rpid }.map(Comment::fromReply),
                nextPage = CommentPage(
                    nextWebPage = commentData.cursor.paginationReply?.nextOffset ?: ""
                ),
                hasNext = commentData.cursor.isEnd.not() && !nextOffset.isNullOrBlank()
            )
        }

        fun fromMainListReply(mainListReply: bilibili.main.community.reply.v1.MainListReply): CommentsData {
            val pins = buildList {
                if (mainListReply.hasUpTop()) add(mainListReply.upTop)
                if (mainListReply.hasAdminTop()) add(mainListReply.adminTop)
                if (mainListReply.hasVoteTop()) add(mainListReply.voteTop)
                addAll(mainListReply.topRepliesList)
            }
            val normal = mainListReply.repliesList
            val regularIds = (pins + normal).mapTo(hashSetOf()) { it.id }
            val hotspots = mainListReply.mixedCardsList.sortedBy { it.displayRank }.mapNotNull { mixed ->
                if (!mixed.hasHotspot()) return@mapNotNull null
                val card = mixed.hotspot
                if (card.rpid in regularIds || card.rpid <= 0 || card.oid <= 0 || card.type <= 0) return@mapNotNull null
                Comment.fromHotspot(card)
            }
            return CommentsData(
                comments = (pins.map(Comment::fromReplyInfo) + hotspots + normal.map(Comment::fromReplyInfo)).distinctBy { it.rpid },
                nextPage = CommentPage(
                    nextAppPage = mainListReply.paginationReply.nextOffset
                ),
                hasNext = !mainListReply.cursor.isEnd && mainListReply.paginationReply.nextOffset.isNotBlank(),
                vote = mainListReply
                    .takeIf { it.hasVoteCard() }
                    ?.voteCard
                    ?.takeIf { it.voteId > 0L }
                    ?.let { voteCard ->
                        CommentVote(
                            id = voteCard.voteId,
                            title = voteCard.title,
                            count = voteCard.count
                        )
                    }
            )
        }
    }
}

data class CommentVote(
    val id: Long,
    val title: String,
    val count: Long
)

data class Comment(
    val rpid: Long,
    val mid: Long,
    val oid: Long,
    val type: Long,
    val parent: Long,
    val content: List<String>,
    val member: Member,
    val timeDesc: String,
    val emotes: List<Emote>,
    val pictures: List<Picture>,
    val replies: List<Comment>,
    val repliesCount: Int,
    val mentions: Map<String, Long> = emptyMap(),
    val action: Int = 0,
    val like: Long = 0,
    val translatedContent: List<String> = emptyList(),
    val translatedEmotes: List<Emote> = emptyList(),
    val showTranslation: Boolean = false,
    val translationSwitch: TranslationSwitch = TranslationSwitch.TRANSLATION_SWITCH_UNSUPPORTED,
    val parentReplyName: String = "",
    val sourceText: String = "",
    val hotspotTitle: String? = null,
) {
    val attribution: String
        get() = listOfNotNull(parentReplyName.takeIf(String::isNotBlank)?.let { "回复 @$it" },
            sourceText.takeIf(String::isNotBlank), timeDesc.takeIf(String::isNotBlank)).joinToString(" · ")

    val displayContent: List<String>
        get() = if (showTranslation && hasTranslatedContent) translatedContent else content

    val displayEmotes: List<Emote>
        get() = if (showTranslation && hasTranslatedContent) translatedEmotes else emotes

    val hasTranslatedContent: Boolean
        get() = translatedContent.isNotEmpty() || translatedEmotes.isNotEmpty()

    val canTranslate: Boolean
        get() = translationSwitch == TranslationSwitch.TRANSLATION_SWITCH_SHOW_TRANSLATION

    val isLiked: Boolean
        get() = action == 1

    val isDisliked: Boolean
        get() = action == 2

    companion object {
        fun fromReply(reply: dev.aaa1115910.biliapi.http.entity.reply.CommentData.Reply): Comment {
            return Comment(
                rpid = reply.rpid,
                mentions = reply.content.atNameToMid,
                mid = reply.mid,
                oid = reply.oid.toLong(),
                type = reply.type,
                parent = reply.parent,
                content = reply.content.message.splitWithEmotes(*reply.content.emote.keys.toTypedArray()),
                member = Member(
                    mid = reply.mid,
                    avatar = reply.member.avatar,
                    name = reply.member.uname
                ),
                timeDesc = reply.replyControl.timeDesc,
                emotes = reply.content.emote.values.map { Emote.fromEmote(it) },
                pictures = reply.content.pictures.map { Picture.fromPicture(it) },
                replies = reply.replies.map { fromReply(it) },
                repliesCount = reply.count,
                action = reply.action,
                like = reply.like.toLong(),
                translationSwitch = TranslationSwitch.TRANSLATION_SWITCH_UNSUPPORTED
            )
        }

        fun fromReplyInfo(reply: bilibili.main.community.reply.v1.ReplyInfo): Comment {
            val originalContent = mapGrpcContent(reply.content)
            val translatedContent = mapTranslatedGrpcContent(reply)
            return Comment(
                rpid = reply.id,
                parentReplyName = reply.parentReplyMember.name,
                sourceText = reply.hotInfo.fromText.ifBlank { reply.hotInfo.arcTitle },
                mentions = reply.content.atNameToMidMap,
                mid = reply.mid,
                oid = reply.oid,
                type = reply.type,
                parent = reply.parent,
                content = originalContent.content,
                member = Member(
                    mid = reply.mid,
                    avatar = reply.member.face,
                    name = reply.member.name
                ),
                timeDesc = reply.replyControl.timeDesc,
                emotes = originalContent.emotes,
                pictures = reply.content.picturesList.map { Picture.fromPicture(it) },
                replies = reply.repliesList.map { fromReplyInfo(it) },
                repliesCount = reply.count.toInt(),
                action = reply.replyControl.action.toInt(),
                like = reply.like,
                translatedContent = translatedContent?.content.orEmpty(),
                translatedEmotes = translatedContent?.emotes.orEmpty(),
                showTranslation = translatedContent != null && reply.replyControl.showTranslation,
                translationSwitch = reply.replyControl.translationSwitch
            )
        }

        internal fun fromHotspot(card: bilibili.main.community.reply.v1.HotspotCard): Comment {
            val content = mapGrpcContent(card.content)
            val author = card.member.basic
            return Comment(
                rpid = card.rpid, oid = card.oid, type = card.type.toLong(), mid = author.mid, parent = 0,
                member = Member(author.mid, author.face, author.name), content = content.content,
                emotes = content.emotes, pictures = card.content.picturesList.map(Picture::fromPicture),
                mentions = card.content.atNameToMidMap, replies = emptyList(), repliesCount = 0,
                timeDesc = card.replyControl.timeDesc, like = card.like,
                hotspotTitle = card.titleText.ifBlank { card.title }.ifBlank { "热点评论" },
                sourceText = card.fromText.ifBlank { card.arcTitle }
            )
        }

        fun withTranslatedReply(comment: Comment, reply: bilibili.main.community.reply.v1.ReplyInfo): Comment {
            val translatedContent = mapTranslatedGrpcContent(reply) ?: return comment
            val translationSwitch = reply.replyControl.translationSwitch.takeUnless {
                it == TranslationSwitch.TRANSLATION_SWITCH_UNSPECIFIED
            } ?: comment.translationSwitch
            return comment.copy(
                translatedContent = translatedContent.content,
                translatedEmotes = translatedContent.emotes,
                showTranslation = true,
                translationSwitch = translationSwitch
            )
        }

        private fun mapTranslatedGrpcContent(reply: bilibili.main.community.reply.v1.ReplyInfo): RichContent? {
            if (!reply.hasTranslatedContent()) return null
            return mapGrpcContent(reply.translatedContent)
        }

        private fun mapGrpcContent(content: GrpcContent): RichContent {
            return RichContent(
                content = content.message.splitWithEmotes(*content.emoteMap.keys.toTypedArray()),
                emotes = content.emoteMap.values.map { Emote.fromEmote(it) }
            )
        }
    }

    data class Member(
        val mid: Long,
        val avatar: String,
        val name: String
    )

    data class Emote(
        val text: String,
        val url: String,
        val size: EmoteSize
    ) {
        companion object {
            fun fromEmote(emote: dev.aaa1115910.biliapi.http.entity.reply.CommentData.Reply.Content.Emote): Emote {
                return Emote(
                    text = emote.text,
                    url = emote.url,
                    size = if (emote.meta.size == 1) EmoteSize.Small else EmoteSize.Large
                )
            }

            fun fromEmote(emote: bilibili.main.community.reply.v1.Emote): Emote {
                return Emote(
                    text = emote.text,
                    url = emote.url,
                    size = if (emote.size == 1L) EmoteSize.Small else EmoteSize.Large
                )
            }
        }
    }

    private data class RichContent(
        val content: List<String>,
        val emotes: List<Emote>
    )
}

enum class EmoteSize(val fontSize: Int) {
    Small(20), Large(20)
}

private fun String.splitWithEmotes(vararg emotes: String): List<String> {
    val delimiter = emotes.joinToString("|").replace("[", "\\[").replace("]", "\\]")
    val regex = Regex("(?=$delimiter)|(?<=$delimiter)")
    return this.split(regex)
}
