package dev.aaa1115910.bv.util

import android.content.Intent
import androidx.compose.runtime.*
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.reply.*
import dev.aaa1115910.biliapi.repositories.CommentRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import kotlinx.coroutines.CancellationException
import org.koin.compose.getKoin

fun Intent.putCommentAnchor(anchor: CommentAnchor?) = apply {
    anchor?.let {
        putExtra("commentRoot", it.rootId)
        it.replyId?.let { id -> putExtra("commentReply", id) }
        it.oid?.let { id -> putExtra("commentOid", id) }
        it.type?.let { type -> putExtra("commentType", type) }
    }
}

fun Intent.commentAnchor(): CommentAnchor? = getLongExtra("commentRoot", 0).takeIf { it > 0 }?.let {
    CommentAnchor(it, getLongExtra("commentReply", 0).takeIf { id -> id > 0 },
        getLongExtra("commentOid", 0).takeIf { id -> id > 0 },
        getLongExtra("commentType", 0).takeIf { type -> type > 0 })
}

/** Each source/anchor owns its requests; disposing its composition cancels outstanding work. */
class CommentLinkState(
    val anchor: CommentAnchor,
    private val repository: CommentRepository,
    private val resolveSource: suspend () -> Pair<Long, Long>
) {
    var firstPage by mutableStateOf<CommentRepliesData?>(null)
    var root by mutableStateOf<Comment?>(null)
    val replies = mutableStateListOf<Comment>()
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var targetUnavailable by mutableStateOf(false)
    var hasNext by mutableStateOf(true)
    private var page = CommentReplyPage()
    var oid = anchor.oid ?: 0L
    var type = anchor.type ?: 1L

    suspend fun load() {
        if (loading || !hasNext) return
        loading = true
        error = null
        try {
            if (oid <= 0) {
                val source = resolveSource()
                oid = source.first
                type = source.second
            }
            require(oid > 0 && type > 0) { "该内容暂不支持评论" }
            val first = root == null
            val data = repository.getCommentReplies(anchor.rootId, type, oid, page,
                preferApiType = ApiType.App, targetReplyId = anchor.replyId.takeIf { first })
            require(data.rootComment.rpid == anchor.rootId) { "评论已删除或暂不可见" }
            if (first) firstPage = data
            root = data.rootComment
            replies.addAll(data.replies.filter { candidate -> replies.none { it.rpid == candidate.rpid } })
            if (first) targetUnavailable = anchor.replyId != null && replies.none { it.rpid == anchor.replyId }
            page = data.nextPage
            hasNext = data.hasNext && page.nextAppPage.isNotBlank()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "评论加载失败"
        } finally {
            loading = false
        }
    }
}

@Composable
fun rememberCommentLinkState(anchor: CommentAnchor, videoId: Long = 0, dynamicId: String? = null): CommentLinkState {
    val repository = getKoin().get<CommentRepository>()
    val users = getKoin().get<UserRepository>()
    val state = remember(anchor, videoId, dynamicId) {
        CommentLinkState(anchor, repository) {
            if (dynamicId != null) {
                val item = users.getDynamicDetail(dynamicId)
                item.commentId to item.commentType
            } else {
                videoId to (anchor.type ?: 1L)
            }
        }
    }
    LaunchedEffect(state) { state.load() }
    return state
}
