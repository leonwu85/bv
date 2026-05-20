package dev.aaa1115910.biliapi.repositories

import bilibili.app.view.v1.ViewGrpcKt
import bilibili.app.view.v1.viewReq
import bilibili.main.community.reply.v1.Mode
import bilibili.main.community.reply.v1.ReplyGrpcKt
import bilibili.main.community.reply.v1.detailListReq
import bilibili.main.community.reply.v1.mainListReq
import bilibili.pagination.feedPagination
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentRepliesData
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.entity.reply.CommentsData
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.InteractiveNode
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.biliapi.entity.video.upowerBadgeText
import dev.aaa1115910.biliapi.entity.video.upowerPlayState
import dev.aaa1115910.biliapi.http.entity.video.isInteractiveVideo
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.user.garb.EquipPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class VideoDetailRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: FavoriteRepository,
    private val likeRepository: LikeRepository,
    private val coinRepository: CoinRepository
) {
    private val viewStub
        get() = runCatching {
            ViewGrpcKt.ViewCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()
    private val replyStub
        get() = runCatching {
            ReplyGrpcKt.ReplyCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private suspend fun getWebVideoHistory(videoDetail: VideoDetail): VideoDetail.History {
        videoDetail.history
            .takeIf { it.progress > 0 || it.lastPlayedCid != 0L }
            ?.let { return it }

        val sessData = authRepository.sessionData.orEmpty()
        if (sessData.isBlank()) {
            return VideoDetail.History(0, 0)
        }

        val historyItem = runCatching {
            BiliHttpApi.searchHistory(
                keyword = videoDetail.title,
                sessData = sessData
            ).getResponseData().list.firstOrNull { item ->
                item.history.business == "archive" && (
                    item.history.oid == videoDetail.aid ||
                        (videoDetail.bvid.isNotBlank() && item.history.bvid == videoDetail.bvid)
                    )
            }
        }.onFailure {
            println("Search video history failed: $it")
        }.getOrNull()

        if (historyItem != null) {
            return VideoDetail.History(
                progress = historyItem.progress.coerceAtLeast(0),
                lastPlayedCid = historyItem.history.cid
            )
        }

        return VideoDetail.History(0, 0)
    }

    private suspend fun fillInteractiveInfo(
        videoDetail: VideoDetail,
        knownInteractive: Boolean? = null
    ) {
        val sessData = authRepository.sessionData.orEmpty()
        val isInteractive = knownInteractive ?: runCatching {
            BiliHttpApi.getVideoInfo(
                bv = videoDetail.bvid,
                sessData = sessData.ifBlank { null }
            ).getResponseData().isInteractiveVideo
        }.onFailure {
            println("Get interactive flag failed: $it")
        }.getOrDefault(false)

        videoDetail.isInteractive = isInteractive
        if (!isInteractive) {
            videoDetail.interactiveGraphVersion = null
            videoDetail.interactiveNodes = emptyList()
            return
        }

        val graphVersion = runCatching {
            BiliHttpApi.getVideoPlayerInfo(
                av = videoDetail.aid,
                bv = videoDetail.bvid,
                cid = videoDetail.cid,
                sessData = sessData.ifBlank { null }
            ).getResponseData().interaction?.graphVersion
        }.onFailure {
            println("Get interactive graph version failed: $it")
        }.getOrNull()

        videoDetail.interactiveGraphVersion = graphVersion
        if (graphVersion == null) {
            videoDetail.interactiveNodes = emptyList()
            return
        }

        val interactiveNodes = runCatching {
            BiliHttpApi.getInteractiveEdgeInfo(
                bvid = videoDetail.bvid,
                graphVersion = graphVersion,
                sessData = sessData.ifBlank { null }
            ).getResponseData().storyList.map(InteractiveNode::fromStoryNode)
        }.onFailure {
            println("Get interactive edge info failed: $it")
        }.getOrDefault(emptyList())

        videoDetail.interactiveNodes = interactiveNodes
    }

    suspend fun getVideoDetail(
        aid: Long,
        preferApiType: ApiType = ApiType.Web,
        withUserActions: Boolean = true
    ): VideoDetail {
        return when (preferApiType) {
            ApiType.Web -> {
                withContext(Dispatchers.IO) {
                    // 串行执行：获取视频详情
                    val videoDetailWithoutUserActions = run {
                        val httpVideoDetail = BiliHttpApi.getVideoDetail(
                            av = aid,
                            sessData = authRepository.sessionData ?: ""
                        ).getResponseData()
                        VideoDetail.fromVideoDetail(httpVideoDetail)
                    }

                    // 声明变量
                    var isLiked = false
                    var isFavoured = false
                    var isCoined = false

//                    if (withUserActions) {
//                        // 检查点赞、收藏、投币状态
//                        runCatching {
//                            val archiveRelation = BiliHttpApi.getArchiveRelation(
//                                avid = aid,
//                                sessData = authRepository.sessionData ?: ""
//                            ).getResponseData()
//                            isLiked = archiveRelation.like
//                            isCoined = archiveRelation.coin > 0
//                            isFavoured = archiveRelation.favorite
//                        }.onFailure {
//                            println("Check video relation failed: $it")
//                        }
//                    }
                    if (withUserActions) {
                        // 串行执行：检查点赞状态
                        isLiked = runCatching {
                            likeRepository.checkVideoLike(
                                aid = aid,
                                preferApiType = ApiType.Web
                            )
                        }.onFailure {
                            println("Check video liked failed: $it")
                        }.getOrDefault(false)

                        // 串行执行：检查投币状态
                        isCoined =  runCatching {
                            coinRepository.checkVideoCoin(
                                aid = aid,
                                preferApiType = ApiType.Web
                            )
                        }.onFailure {
                            println("Check video liked failed: $it")
                        }.getOrDefault(false)

                        // 串行执行：检查收藏状态
                        isFavoured = runCatching {
                            favoriteRepository.checkVideoFavoured(
                                aid = aid,
                                preferApiType = ApiType.Web
                            )
                        }.onFailure {
                            println("Check video favoured failed: $it")
                        }.getOrDefault(false)
                    }

                    val history = getWebVideoHistory(videoDetailWithoutUserActions)

                    // 串行执行：获取播放器图标
                    val playerIcon = runCatching {
                        val videoModeInfo = BiliHttpApi.getVideoMoreInfo(
                            avid = aid,
                            cid = videoDetailWithoutUserActions.cid,
                            sessData = authRepository.sessionData ?: "",
                            buvid3 = authRepository.buvid3 ?: ""
                        ).getResponseData()
                        VideoDetail.PlayerIcon.fromPlayerIcon(videoModeInfo.playerIcon)
                    }.onFailure {
                        println("Get video player icon failed: $it")
                    }.getOrDefault(null)

                    // 更新并返回结果
                    videoDetailWithoutUserActions.apply {
                        userActions.like = isLiked
                        userActions.coin = isCoined
                        userActions.favorite = isFavoured
                        this.history = history
                        this.playerIcon = playerIcon
                        fillInteractiveInfo(this, knownInteractive = this.isInteractive)
                    }
                }
            }

            ApiType.App -> {
                val viewReply = runCatching {
                    viewStub?.view(viewReq {
                        this.aid = aid
                    }) ?: throw IllegalStateException("Player stub is not initialized")
                }.onFailure { handleGrpcException(it) }.getOrThrow()
                VideoDetail.fromViewReply(viewReply).apply {
                    runCatching {
                        val webVideoInfo = BiliHttpApi.getVideoInfo(
                            bv = this.bvid.takeIf { it.isNotBlank() },
                            sessData = authRepository.sessionData ?: ""
                        ).getResponseData()
                        applyChargingArcInfo(
                            isChargingArc = webVideoInfo.isUpowerExclusive,
                            chargingArcBadge = upowerBadgeText(webVideoInfo.isUpowerExclusive),
                            isUpowerPlay = upowerPlayState(
                                isUpowerExclusive = webVideoInfo.isUpowerExclusive,
                                isUpowerPlay = webVideoInfo.isUpowerPlay
                            )
                        )
                    }.onFailure {
                        println("Get video charging arc info failed: $it")
                    }
                    fillInteractiveInfo(this)
                    if (playerIcon?.idle?.isBlank() != false && authRepository.sessionData != null) {
                        println("player icon not found in view reply, try to get it from garb api")
                        runCatching {
                            val playerIconGarb = BiliHttpApi.getUserEquippedGarb(
                                part = EquipPart.PlayerIcon,
                                sessData = authRepository.sessionData!!
                            ).getResponseData()
                            val playerIconItem = playerIconGarb.item
                                ?: throw IllegalStateException("player icon not equipped")
                            this.playerIcon = VideoDetail.PlayerIcon(
                                idle = playerIconItem.properties.icon ?: "",
                                moving = playerIconItem.properties.dragIcon ?: ""
                            )
                        }.onFailure {
                            println("Get player icon failed: $it")
                        }
                    }
                }
            }
        }
    }

    suspend fun getPgcVideoDetail(
        epid: Int? = null,
        seasonId: Int? = null,
        preferApiType: ApiType = ApiType.Web
    ): SeasonDetail {
        val normalizedEpid = epid?.takeIf { it > 0 }
        val normalizedSeasonId = seasonId?.takeIf { it > 0 }

        when (preferApiType) {
            ApiType.Web -> {
                val webSeasonData = BiliHttpApi.getWebSeasonInfo(
                    epId = normalizedEpid,
                    seasonId = normalizedSeasonId,
                    sessData = authRepository.sessionData ?: ""
                ).getResponseData()
                val userStatusSeasonId = normalizedSeasonId ?: webSeasonData.seasonId.takeIf { it > 0 }
                if (userStatusSeasonId != null) {
                    webSeasonData.userStatus = BiliHttpApi.getSeasonUserStatus(
                        seasonId = userStatusSeasonId,
                        sessData = authRepository.sessionData ?: ""
                    ).getResponseData()
                }
                val seasonDetail = SeasonDetail.fromSeasonData(webSeasonData)
                val firstEp = webSeasonData.episodes.firstOrNull() ?: return seasonDetail

                val playerIcon = runCatching {
                    val videoModeInfo = BiliHttpApi.getVideoMoreInfo(
                        avid = firstEp.aid,
                        cid = firstEp.cid,
                        sessData = authRepository.sessionData ?: "",
                        buvid3 = authRepository.buvid3 ?: ""
                    ).getResponseData()
                    val playerIcon = VideoDetail.PlayerIcon.fromPlayerIcon(videoModeInfo.playerIcon)
                    playerIcon
                }.onFailure {
                    println("Get video player icon failed: $it")
                }.getOrDefault(null)
                seasonDetail.playerIcon = playerIcon
                return seasonDetail
            }

            ApiType.App -> {
                val appSeasonData = BiliHttpApi.getAppSeasonInfo(
                    epId = normalizedEpid,
                    seasonId = normalizedSeasonId,
                    mobiApp = "android_hd",
                    accessKey = authRepository.accessToken ?: ""
                ).getResponseData()
                return SeasonDetail.fromSeasonData(appSeasonData)
            }
        }
    }

    suspend fun getComments(
        aid: Long,
        sort: CommentSort = CommentSort.Hot,
        page: CommentPage = CommentPage(),
        preferApiType: ApiType = ApiType.Web
    ): CommentsData {
        when (preferApiType) {
            ApiType.Web -> {
                val webComments = BiliHttpApi.getComments(
                    oid = aid,
                    type = 1,
                    mode = sort.param,
                    paginationStr = Json.encodeToString(mapOf("offset" to page.nextWebPage)),
                    sessData = authRepository.sessionData ?: "",
                    buvid3 = authRepository.buvid3 ?: ""
                ).getResponseData()
                return CommentsData.fromCommentData(webComments)
            }

            ApiType.App -> {
                val appComments = replyStub?.mainList(
                    mainListReq {
                        oid = aid
                        type = 1
                        /*cursor = cursorReq {
                            next = page.nextAppPage.toLong()
                            mode = when (sort) {
                                CommentSort.Hot -> Mode.MAIN_LIST_HOT
                                CommentSort.HotAndTime -> Mode.DEFAULT
                                CommentSort.Time -> Mode.MAIN_LIST_TIME
                            }
                        }*/
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
            }
        }
    }

    suspend fun getCommentReplies(
        aid: Long,
        commentId: Long,
        page: CommentReplyPage = CommentReplyPage(),
        sort: CommentSort = CommentSort.Hot,
        preferApiType: ApiType = ApiType.Web
    ): CommentRepliesData {
        when (preferApiType) {
            ApiType.Web -> {
                val webReplies = BiliHttpApi.getCommentReplies(
                    oid = aid,
                    type = 1,
                    root = commentId,
                    pageSize = 20,
                    pageNumber = page.nextWebPage,
                ).getResponseData()
                return CommentRepliesData.fromCommentReplyData(webReplies)
            }

            ApiType.App -> {
                val appReplies = replyStub?.detailList(
                    detailListReq {
                        oid = aid
                        type = 1
                        root = commentId
                        /*cursor = cursorReq {
                            next = page.nextAppPage.toLong()
                        }*/
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
}
