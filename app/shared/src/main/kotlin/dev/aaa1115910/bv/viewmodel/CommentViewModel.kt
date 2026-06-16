package dev.aaa1115910.bv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.entity.user.DynamicImageDraft
import dev.aaa1115910.biliapi.entity.user.DynamicMentionDraft
import dev.aaa1115910.biliapi.entity.user.DynamicEmotePackageDraft
import dev.aaa1115910.biliapi.repositories.CommentRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class CommentViewModel(
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        val logger = KotlinLogging.logger {}
    }

    var commentId = 0L
    var commentType = 0L

    val comments = mutableStateListOf<Comment>()
    val replies = mutableStateListOf<Comment>()
    var replyRootComment by mutableStateOf<Comment?>(null)

    var rpid by mutableLongStateOf(0L)
    var rpCount by mutableIntStateOf(0)

    var nextCommentPage = CommentPage()
    var nextCommentReplyPage = CommentReplyPage()

    var commentSort by mutableStateOf(CommentSort.Hot)
    var replySort by mutableStateOf(CommentSort.Time)

    var hasMoreComments by mutableStateOf(true)
    var hasMoreReplies by mutableStateOf(true)
    var refreshingComments by mutableStateOf(false)
    var refreshingReplies by mutableStateOf(false)
    var updatingComments by mutableStateOf(false)
    var updatingReplies by mutableStateOf(false)
    var sendingComment by mutableStateOf(false)
    val emotePackages = mutableStateListOf<DynamicEmotePackageDraft>()
    var loadingEmotes by mutableStateOf(false)

    fun setCommentTarget(commentId: Long, commentType: Long) {
        if (commentId <= 0L || commentType <= 0L) return
        if (this.commentId == commentId && this.commentType == commentType) return
        this.commentId = commentId
        this.commentType = commentType
        nextCommentPage = CommentPage()
        hasMoreComments = true
        refreshingComments = false
        updatingComments = false
        comments.clear()
        nextCommentReplyPage = CommentReplyPage()
        hasMoreReplies = true
        refreshingReplies = false
        updatingReplies = false
        replies.clear()
        replyRootComment = null
        rpid = 0L
        rpCount = 0
    }

    suspend fun loadMoreComment() {
        if (commentId <= 0L || commentType <= 0L) {
            logger.warn { "Skip loading comments for invalid target: commentId=$commentId, commentType=$commentType" }
            withContext(Dispatchers.Main) {
                updatingComments = false
                refreshingComments = false
            }
            return
        }
        if (updatingComments) return
        withContext(Dispatchers.Main) {
            updatingComments = true
        }
        if (!hasMoreComments) {
            withContext(Dispatchers.Main) {
                updatingComments = false
            }
            delay(300)
            withContext(Dispatchers.Main) {
                refreshingComments = false
            }
            return
        }
        logger.fInfo { "Load more comments: [commentId=$commentId, commentType=$commentType, page=$nextCommentPage]" }
        runCatching {
            val commentsData = commentRepository.getComments(
                id = commentId,
                type = commentType,
                page = nextCommentPage,
                sort = commentSort,
                preferApiType = Prefs.apiType
            )
            nextCommentPage = commentsData.nextPage
            hasMoreComments = commentsData.hasNext
            comments.addAll(commentsData.comments)
        }.onFailure {
            logger.fException(it) { "Load more comments failed" }
            withContext(Dispatchers.Main) {
                "加载评论失败：${it.localizedMessage}".toast(BVApp.context)
            }
        }
        withContext(Dispatchers.Main) {
            updatingComments = false
        }
        delay(300)
        withContext(Dispatchers.Main) {
            refreshingComments = false
        }
    }

    suspend fun switchCommentSort(newSort: CommentSort) {
        logger.fInfo { "Switch comment sort to ${newSort.name}" }
        commentSort = newSort
        refreshComments()
    }

    suspend fun refreshComments() {
        refreshingComments = true
        logger.fInfo { "refresh comments" }
        nextCommentPage = CommentPage()
        hasMoreComments = true
        comments.clear()
        loadMoreComment()
    }

    suspend fun loadMoreReplies() {
        if (commentId <= 0L || commentType <= 0L || rpid <= 0L) {
            logger.warn { "Skip loading replies for invalid target: commentId=$commentId, commentType=$commentType, rpid=$rpid" }
            withContext(Dispatchers.Main) {
                updatingReplies = false
                refreshingReplies = false
            }
            return
        }
        if (updatingReplies) return
        withContext(Dispatchers.Main) {
            updatingReplies = true
        }
        if (!hasMoreReplies) {
            withContext(Dispatchers.Main) {
                updatingReplies = false
            }
            delay(300)
            withContext(Dispatchers.Main) {
                refreshingReplies = false
            }
            return
        }
        logger.fInfo { "Load more replies, commentId=$commentId, commentType=$commentType, page=$nextCommentReplyPage" }
        runCatching {
            val commentRepliesData = commentRepository.getCommentReplies(
                rpid = rpid,
                type = commentType,
                commentId = commentId,
                page = nextCommentReplyPage,
                sort = replySort,
                preferApiType = Prefs.apiType
            )
            nextCommentReplyPage = commentRepliesData.nextPage
            hasMoreReplies = commentRepliesData.hasNext
            if (replyRootComment == null) replyRootComment = commentRepliesData.rootComment
            replies.addAll(commentRepliesData.replies)
        }.onFailure {
            logger.fException(it) { "Load more replies failed" }
        }
        withContext(Dispatchers.Main) {
            updatingReplies = false
        }
        delay(300)
        withContext(Dispatchers.Main) {
            refreshingReplies = false
        }
    }

    suspend fun switchReplySort(newSort: CommentSort) {
        logger.fInfo { "Switch reply sort to ${newSort.name}" }
        replySort = newSort
        refreshReplies()
    }

    suspend fun refreshReplies() {
        refreshingReplies = true
        logger.fInfo { "refresh replies" }
        nextCommentReplyPage = CommentReplyPage()
        hasMoreReplies = true
        replies.clear()
        loadMoreReplies()
    }

    suspend fun sendComment(
        message: String,
        root: Long? = null,
        parent: Long? = null,
        pictures: List<DynamicImageDraft> = emptyList(),
        atNameToMid: Map<String, Long> = emptyMap(),
        syncToDynamic: Boolean = false
    ): Result<Unit> {
        val content = message.trim()
        if (content.isBlank() && pictures.isEmpty()) {
            return Result.failure(IllegalArgumentException("评论不能为空"))
        }
        if (!Prefs.isLogin) {
            return Result.failure(IllegalStateException("账号未登录"))
        }
        if (commentId <= 0L || commentType <= 0L) {
            return Result.failure(IllegalStateException("评论区不可用"))
        }
        if (sendingComment) {
            return Result.failure(IllegalStateException("正在发送中"))
        }

        return runCatching {
            withContext(Dispatchers.Main) { sendingComment = true }
            commentRepository.addComment(
                type = commentType,
                oid = commentId,
                message = content,
                root = root?.takeIf { it != 0L },
                parent = parent?.takeIf { it != 0L },
                pictures = pictures,
                atNameToMid = atNameToMid,
                syncToDynamic = syncToDynamic
            )
            refreshComments()
        }.onSuccess {
            withContext(Dispatchers.Main) {
                "发送成功".toast(BVApp.context)
            }
        }.onFailure {
            logger.fException(it) { "Send comment failed" }
            withContext(Dispatchers.Main) {
                "发送失败：${it.localizedMessage}".toast(BVApp.context)
            }
        }.also {
            withContext(Dispatchers.Main) { sendingComment = false }
        }
    }

    private fun updateCommentInList(list: MutableList<Comment>, updatedComment: Comment) {
        list.indices.forEach { index ->
            val comment = list[index]
            when {
                comment.rpid == updatedComment.rpid -> {
                    list[index] = updatedComment
                }

                comment.replies.any { it.rpid == updatedComment.rpid } -> {
                    list[index] = comment.copy(
                        replies = comment.replies.map { reply ->
                            if (reply.rpid == updatedComment.rpid) updatedComment else reply
                        }
                    )
                }
            }
        }
    }

    private suspend fun updateCommentState(updatedComment: Comment) {
        withContext(Dispatchers.Main) {
            updateCommentInList(comments, updatedComment)
            updateCommentInList(replies, updatedComment)
            if (replyRootComment?.rpid == updatedComment.rpid) {
                replyRootComment = updatedComment
            }
        }
    }

    suspend fun toggleCommentLike(comment: Comment): Result<Comment> {
        if (!Prefs.isLogin) {
            return Result.failure(IllegalStateException("账号未登录"))
        }
        return runCatching {
            commentRepository.likeComment(comment)
        }.onSuccess { updatedComment ->
            updateCommentState(updatedComment)
        }.onFailure {
            logger.fException(it) { "Toggle comment like failed" }
        }
    }

    suspend fun toggleCommentDislike(comment: Comment): Result<Comment> {
        if (!Prefs.isLogin) {
            return Result.failure(IllegalStateException("账号未登录"))
        }
        return runCatching {
            commentRepository.hateComment(comment)
        }.onSuccess { updatedComment ->
            updateCommentState(updatedComment)
        }.onFailure {
            logger.fException(it) { "Toggle comment dislike failed" }
        }
    }

    suspend fun reportComment(
        comment: Comment,
        reasonType: Int,
        reasonDesc: String? = null,
        addBlacklist: Boolean = false
    ): Result<Unit> {
        if (!Prefs.isLogin) {
            return Result.failure(IllegalStateException("账号未登录"))
        }
        return runCatching {
            commentRepository.reportComment(
                comment = comment,
                reasonType = reasonType,
                reasonDesc = reasonDesc,
                addBlacklist = addBlacklist
            )
        }.onFailure {
            logger.fException(it) { "Report comment failed" }
        }
    }

    suspend fun blacklistCommentUser(comment: Comment): Result<Unit> {
        if (!Prefs.isLogin) {
            return Result.failure(IllegalStateException("账号未登录"))
        }
        return runCatching {
            val success = userRepository.blacklistUser(comment.member.mid)
            if (!success) error("加入黑名单失败")
        }.onFailure {
            logger.fException(it) { "Blacklist comment user failed" }
        }
    }

    suspend fun loadEmotePackages() {
        if (loadingEmotes || emotePackages.isNotEmpty()) return
        runCatching {
            withContext(Dispatchers.Main) { loadingEmotes = true }
            userRepository.getDynamicEmotePackages()
        }.onSuccess { packages ->
            withContext(Dispatchers.Main) {
                emotePackages.clear()
                emotePackages.addAll(packages)
            }
        }.onFailure {
            logger.fException(it) { "Load reply emote packages failed" }
        }.also {
            withContext(Dispatchers.Main) { loadingEmotes = false }
        }
    }

    suspend fun searchMention(keyword: String): Result<List<DynamicMentionDraft>> =
        runCatching {
            userRepository.searchDynamicMention(keyword)
        }

    suspend fun uploadCommentImage(fileName: String, bytes: ByteArray): Result<DynamicImageDraft> =
        runCatching {
            userRepository.uploadDynamicImage(fileName = fileName, bytes = bytes)
        }
}
