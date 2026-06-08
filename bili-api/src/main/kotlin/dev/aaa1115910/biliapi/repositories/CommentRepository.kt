package dev.aaa1115910.biliapi.repositories

import bilibili.main.community.reply.v1.Mode
import bilibili.main.community.reply.v1.ReplyGrpcKt
import bilibili.main.community.reply.v1.detailListReq
import bilibili.main.community.reply.v1.mainListReq
import bilibili.pagination.feedPagination
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentRepliesData
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.entity.reply.CommentsData
import dev.aaa1115910.biliapi.entity.user.DynamicImageDraft
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class CommentRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val replyStub
        get() = runCatching {
            ReplyGrpcKt.ReplyCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getComments(
        id: Long,
        type: Long,
        sort: CommentSort = CommentSort.Hot,
        page: CommentPage = CommentPage(),
        preferApiType: ApiType = ApiType.Web
    ): CommentsData {
        when (preferApiType) {
            ApiType.Web -> {
                val webComments = BiliHttpApi.getComments(
                    oid = id,
                    type = type,
                    mode = sort.param,
                    paginationStr = Json.encodeToString(mapOf("offset" to page.nextWebPage)),
                    sessData = authRepository.sessionData,
                    dedeUserID = authRepository.mid,
                    buvid3 = authRepository.buvid3
                ).getResponseData()
                return CommentsData.fromCommentData(webComments)
            }

            ApiType.App -> {
                runCatching {
                    val appComments = replyStub?.mainList(
                        mainListReq {
                            this.oid = id.toLong()
                            this.type = type.toLong()
                            mode = when (sort) {
                                CommentSort.Hot -> Mode.MAIN_LIST_HOT
                                CommentSort.HotAndTime -> Mode.DEFAULT
                                CommentSort.Time -> Mode.MAIN_LIST_TIME
                            }
                            pagination = feedPagination {
                                offset = page.nextAppPage
                            }
                        }
                    ) ?: throw IllegalStateException("Reply stub is not initialized")
                    return CommentsData.fromMainListReply(appComments)
                }.onFailure {
                    handleGrpcException(it)
                }.getOrThrow()
            }
        }
    }

    suspend fun getCommentReplies(
        rpid: Long,
        type: Long,
        commentId: Long,
        page: CommentReplyPage = CommentReplyPage(),
        sort: CommentSort = CommentSort.Hot,
        preferApiType: ApiType = ApiType.Web
    ): CommentRepliesData {
        when (preferApiType) {
            ApiType.Web -> {
                val webReplies = BiliHttpApi.getCommentReplies(
                    oid = commentId,
                    type = type,
                    root = rpid,
                    pageSize = 20,
                    pageNumber = page.nextWebPage,
                    sessData = authRepository.sessionData,
                    dedeUserID = authRepository.mid,
                    buvid3 = authRepository.buvid3,
                ).getResponseData()
                return CommentRepliesData.fromCommentReplyData(webReplies)
            }

            ApiType.App -> {
                val appReplies = replyStub?.detailList(
                    detailListReq {
                        this.oid = commentId
                        this.type = type
                        root = rpid
                        mode = when (sort) {
                            CommentSort.Hot -> Mode.MAIN_LIST_HOT
                            CommentSort.HotAndTime -> Mode.DEFAULT
                            CommentSort.Time -> Mode.MAIN_LIST_TIME
                        }
                        pagination = feedPagination {
                            offset = page.nextAppPage
                        }
                    }
                ) ?: throw IllegalStateException("Reply stub is not initialized")
                return CommentRepliesData.fromCommentReplyList(appReplies)
            }
        }
    }

    suspend fun addComment(
        type: Long,
        oid: Long,
        message: String,
        root: Long? = null,
        parent: Long? = null,
        pictures: List<DynamicImageDraft> = emptyList(),
        atNameToMid: Map<String, Long> = emptyMap(),
        syncToDynamic: Boolean = false
    ) {
        val (success, responseMessage) = BiliHttpApi.addReply(
            oid = oid,
            type = type,
            message = message,
            root = root,
            parent = parent,
            pictures = pictures,
            atNameToMid = atNameToMid,
            csrf = authRepository.biliJct,
            sessData = authRepository.sessionData,
            syncToDynamic = syncToDynamic
        )
        if (!success) {
            throw Exception("发送评论失败: $responseMessage")
        }
    }

    suspend fun likeComment(comment: Comment): Comment {
        val action = if (comment.isLiked) 0 else 1
        val response = BiliHttpApi.likeReply(
            oid = comment.oid,
            type = comment.type,
            rpid = comment.rpid,
            action = action,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录")
        )
        if (response.code != 0) {
            throw Exception(response.message)
        }
        val nextLike = if (action == 1) {
            comment.like + 1
        } else {
            (comment.like - 1).coerceAtLeast(0)
        }
        return comment.copy(
            action = if (action == 1) 1 else 0,
            like = nextLike
        )
    }

    suspend fun hateComment(comment: Comment): Comment {
        val action = if (comment.isDisliked) 0 else 1
        val response = BiliHttpApi.hateReply(
            oid = comment.oid,
            type = comment.type,
            rpid = comment.rpid,
            action = action,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录")
        )
        if (response.code != 0) {
            throw Exception(response.message)
        }
        return comment.copy(
            action = if (action == 1) 2 else 0,
            like = if (action == 1 && comment.isLiked) {
                (comment.like - 1).coerceAtLeast(0)
            } else {
                comment.like
            }
        )
    }

    suspend fun reportComment(
        comment: Comment,
        reasonType: Int,
        reasonDesc: String? = null,
        addBlacklist: Boolean = false
    ) {
        val response = BiliHttpApi.reportReply(
            oid = comment.oid,
            type = comment.type,
            rpid = comment.rpid,
            reasonType = reasonType,
            reasonDesc = reasonDesc,
            addBlacklist = addBlacklist,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录")
        )
        if (response.code != 0) {
            throw Exception(response.message)
        }
    }

    suspend fun translateReply(comment: Comment): Comment? {
        if (!comment.canTranslate) return null
        return runCatching {
            val response = replyStub?.translateReply(
                bilibili.main.community.reply.v1.TranslateReplyReq.newBuilder()
                    .setType(comment.type)
                    .setOid(comment.oid)
                    .addRpids(comment.rpid)
                    .build()
            ) ?: throw IllegalStateException("Reply stub is not initialized")
            val translatedReply = response.translatedRepliesMap[comment.rpid] ?: return@runCatching null
            Comment.withTranslatedReply(comment, translatedReply)
                .takeIf { it.hasTranslatedContent }
        }.onFailure {
            handleGrpcException(it)
        }.getOrThrow()
    }
}
