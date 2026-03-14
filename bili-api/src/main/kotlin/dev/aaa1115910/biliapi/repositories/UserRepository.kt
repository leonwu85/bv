package dev.aaa1115910.biliapi.repositories

import bilibili.app.dynamic.v2.DynamicGrpcKt
import bilibili.app.dynamic.v2.Refresh
import bilibili.app.dynamic.v2.UserInfo
import bilibili.app.dynamic.v2.dynAllReq
import bilibili.app.dynamic.v2.dynDetailReq
import bilibili.app.dynamic.v2.dynVideoReq
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.DynamicData
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicVideoData
import dev.aaa1115910.biliapi.entity.user.FollowedUser
import dev.aaa1115910.biliapi.entity.user.ArticleParagraph
import dev.aaa1115910.biliapi.entity.user.ArticlePicture
import dev.aaa1115910.biliapi.entity.user.ArticleTextNode
import dev.aaa1115910.biliapi.entity.user.TextNodeType
import dev.aaa1115910.biliapi.entity.user.SpaceVideoData
import dev.aaa1115910.biliapi.entity.user.SpaceVideoOrder
import dev.aaa1115910.biliapi.entity.user.SpaceVideoPage
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.dynamic.OpusParagraph
import dev.aaa1115910.biliapi.http.entity.dynamic.OpusTextNode
import dev.aaa1115910.biliapi.http.entity.dynamic.ArticleViewData
import dev.aaa1115910.biliapi.http.entity.user.FollowAction
import dev.aaa1115910.biliapi.http.entity.user.FollowActionSource
import dev.aaa1115910.biliapi.http.entity.user.RelationType
import dev.aaa1115910.biliapi.http.entity.user.UserCardData
import dev.aaa1115910.biliapi.http.entity.user.UserInfoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.math.ceil

@Single
class UserRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val dynamicStub
        get() = runCatching {
            DynamicGrpcKt.DynamicCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private suspend fun modifyFollow(
        mid: Long,
        action: FollowAction,
        preferApiType: ApiType = ApiType.Web
    ): Boolean {
        val response = when (preferApiType) {
            ApiType.Web -> {
                BiliHttpApi.modifyFollow(
                    mid = mid,
                    action = action,
                    actionSource = FollowActionSource.Space,
                    csrf = authRepository.biliJct,
                    sessData = authRepository.sessionData,
                    buvid3 = authRepository.buvid3,
                    dedeUserId = authRepository.mid!!
                )
            }

            ApiType.App -> {
                BiliHttpApi.modifyFollow(
                    mid = mid,
                    action = action,
                    actionSource = FollowActionSource.Space,
                    accessKey = authRepository.accessToken,
                    buvid3 = authRepository.buvid3,
                    dedeUserId = authRepository.mid!!
                )
            }
        }
        return response.code == 0
    }

    suspend fun followUser(
        mid: Long,
        preferApiType: ApiType = ApiType.Web
    ): Boolean = modifyFollow(mid, FollowAction.AddFollow, preferApiType)

    suspend fun unfollowUser(
        mid: Long,
        preferApiType: ApiType = ApiType.Web
    ): Boolean = modifyFollow(mid, FollowAction.DelFollow, preferApiType)

    suspend fun checkIsFollowing(
        mid: Long,
        preferApiType: ApiType = ApiType.Web
    ): Boolean? {
        if (authRepository.sessionData == null && authRepository.accessToken == null) return null
        return runCatching {
            val response = when (preferApiType) {
                ApiType.Web -> {
                    BiliHttpApi.getRelations(
                        mid = mid,
                        sessData = authRepository.sessionData
                    )
                }

                ApiType.App -> {
                    BiliHttpApi.getRelations(
                        mid = mid,
                        //移动端貌似并没有使用这个接口，目前该接口返回-663鉴权失败，直接改用sessdata获取
                        sessData = authRepository.sessionData
                        //accessKey = authRepository.accessToken
                    )
                }
            }.getResponseData()
            listOf(
                RelationType.Followed,
                RelationType.FollowedQuietly,
                RelationType.BothFollowed
            ).contains(response.relation.attribute)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    //TODO 改成返回 关注数，粉丝数，黑名单数
    suspend fun getFollowingUpCount(
        mid: Long,
        preferApiType: ApiType
    ): Int {
        if (authRepository.sessionData == null && authRepository.accessToken == null) return 0
        return runCatching {
            val response = when (preferApiType) {
                ApiType.Web -> {
                    BiliHttpApi.getRelationStat(
                        mid = mid,
                        sessData = authRepository.sessionData
                    )
                }

                ApiType.App -> {
                    BiliHttpApi.getRelationStat(
                        mid = mid,
                        accessKey = authRepository.accessToken
                    )
                }
            }.getResponseData()
            response.following
        }.onFailure {
            it.printStackTrace()
        }.getOrNull() ?: 0
    }

    suspend fun addSeasonFollow(
        seasonId: Int,
        preferApiType: ApiType = ApiType.Web
    ): String {
        return when (preferApiType) {
            ApiType.Web -> BiliHttpApi.addSeasonFollow(
                seasonId = seasonId,
                csrf = authRepository.biliJct!!,
                sessData = authRepository.sessionData!!
            )

            ApiType.App -> BiliHttpApi.addSeasonFollow(
                seasonId = seasonId,
                accessKey = authRepository.accessToken!!
            )
        }.getResponseData().toast
    }

    suspend fun delSeasonFollow(
        seasonId: Int,
        preferApiType: ApiType = ApiType.Web
    ): String {
        return when (preferApiType) {
            ApiType.Web -> BiliHttpApi.delSeasonFollow(
                seasonId = seasonId,
                csrf = authRepository.biliJct!!,
                sessData = authRepository.sessionData!!
            )

            ApiType.App -> BiliHttpApi.delSeasonFollow(
                seasonId = seasonId,
                accessKey = authRepository.accessToken!!
            )
        }.getResponseData().toast
    }

    suspend fun getSpaceVideos(
        mid: Long,
        order: SpaceVideoOrder = SpaceVideoOrder.PubDate,
        page: SpaceVideoPage = SpaceVideoPage(),
        preferApiType: ApiType = ApiType.Web
    ): SpaceVideoData {
        return when (preferApiType) {
            ApiType.Web -> {
                val webSpaceVideoData = BiliHttpApi.getWebUserSpaceVideos(
                    mid = mid,
                    order = order.value,
                    pageNumber = page.nextWebPageNumber,
                    pageSize = page.nextWebPageSize,
                    sessData = authRepository.sessionData ?: ""
                ).getResponseData()
                SpaceVideoData.fromWebSpaceVideoData(webSpaceVideoData)
            }

            ApiType.App -> {
                val appSpaceVideoData = BiliHttpApi.getAppUserSpaceVideos(
                    mid = mid,
                    lastAvid = page.lastAvid,
                    order = order.value,
                    ts = System.currentTimeMillis(),
                    accessKey = authRepository.accessToken ?: ""
                ).getResponseData()
                SpaceVideoData.fromAppSpaceVideoData(appSpaceVideoData)
            }
        }
    }

    suspend fun getDynamicVideos(
        page: Int,
        offset: String,
        updateBaseline: String,
        preferApiType: ApiType = ApiType.Web
    ): DynamicVideoData {
        return when (preferApiType) {
            ApiType.Web -> {
                val responseData = BiliHttpApi.getDynamicList(
                    type = "video",
                    page = page,
                    offset = offset,
                    sessData = authRepository.sessionData ?: ""
                ).getResponseData()
                DynamicVideoData.fromDynamicData(responseData)
            }

            ApiType.App -> {
                var result: DynamicVideoData? = null
                runCatching {
                    val dynVideoReply = dynamicStub?.dynVideo(dynVideoReq {
                        this.page = page
                        this.offset = offset
                        this.updateBaseline = updateBaseline
                        localTime = 8
                        refreshType =
                            if (offset == "") Refresh.refresh_new else Refresh.refresh_history
                    })
                    result = DynamicVideoData.fromDynamicData(dynVideoReply!!)
                }.onFailure {
                    handleGrpcException(it)
                }
                result!!
            }
        }
    }

    suspend fun getDynamics(
        page: Int,
        offset: String,
        updateBaseline: String,
        preferApiType: ApiType = ApiType.Web
    ): DynamicData {
        return getDynamicsByType("all", page, offset, updateBaseline, preferApiType)
    }

    suspend fun getDynamicsByType(
        type: String,
        page: Int,
        offset: String,
        updateBaseline: String,
        preferApiType: ApiType = ApiType.Web
    ): DynamicData {
        return when (preferApiType) {
            ApiType.Web -> {
                val responseData = BiliHttpApi.getDynamicList(
                    type = type,
                    page = page,
                    offset = offset,
                    sessData = authRepository.sessionData ?: ""
                ).getResponseData()
                DynamicData.fromDynamicData(responseData)
            }

            ApiType.App -> {
                var result: DynamicData? = null
                runCatching {
                    val dynAllReply = dynamicStub?.dynAll(dynAllReq {
                        this.page = page
                        this.offset = offset
                        this.updateBaseline = updateBaseline
                        localTime = 8
                        refreshType =
                            if (offset == "") Refresh.refresh_new else Refresh.refresh_history
                    })
                    result = DynamicData.fromDynamicData(dynAllReply!!)
                }.onFailure {
                    handleGrpcException(it)
                }
                result!!
            }
        }
    }

    suspend fun getDynamicDetail(
        dynamicId: String,
        preferApiType: ApiType = ApiType.Web
    ): DynamicItem {
        return when (preferApiType) {
            ApiType.Web -> {
                val responseData = BiliHttpApi.getDynamicDetail(
                    id = dynamicId,
                    features = "itemOpusStyle",
                    sessData = authRepository.sessionData ?: ""
                ).getResponseData()
                DynamicItem.fromDynamicItem(responseData.item)
            }

            ApiType.App -> {
                var result: DynamicItem? = null
                runCatching {
                    val dynDetailReply = dynamicStub?.dynDetail(dynDetailReq {
                        this.dynamicId = dynamicId
                        localTime = 8
                    })
                    result = DynamicItem.fromDynamicItem(dynDetailReply!!.item)
                }.onFailure {
                    handleGrpcException(it)
                }
                result!!
            }
        }
    }

    /**
     * 获取 Opus (专栏/图文) 详情
     * 用于获取专栏的完整段落内容
     *
     * @return 段落列表
     */
    suspend fun getOpusDetail(
        opusId: String,
        preferApiType: ApiType = ApiType.Web
    ): List<ArticleParagraph> {
        return when (preferApiType) {
            ApiType.Web -> {
                runCatching {
                    val responseData = BiliHttpApi.getOpusDetail(
                        opusId = opusId,
                        sessData = authRepository.sessionData ?: ""
                    ).getResponseData()

                    // 检查是否需要 fallback 到传统专栏 API
                    val fallbackId = responseData.fallback?.id
                    if (fallbackId != null) {
                        // 使用传统专栏 API 获取内容
                        val articleData = BiliHttpApi.getArticleView(
                            cvId = fallbackId,
                            sessData = authRepository.sessionData ?: ""
                        ).getResponseData()
                        // 从 articleView 的 opus.content.paragraphs 获取段落
                        parseOpusParagraphs(articleData.opus?.content?.paragraphs ?: emptyList())
                    } else {
                        // 从 modules 列表中找到 MODULE_TYPE_CONTENT 类型的模块
                        val contentModule = responseData.item?.modules?.find { it.moduleType == "MODULE_TYPE_CONTENT" }
                        parseOpusParagraphs(contentModule?.moduleContent?.paragraphs ?: emptyList())
                    }
                }.getOrElse { emptyList() }
            }

            ApiType.App -> {
                // App 端暂时使用 Web API
                runCatching {
                    val responseData = BiliHttpApi.getOpusDetail(
                        opusId = opusId,
                        sessData = authRepository.sessionData ?: ""
                    ).getResponseData()

                    // 检查是否需要 fallback 到传统专栏 API
                    val fallbackId = responseData.fallback?.id
                    if (fallbackId != null) {
                        // 使用传统专栏 API 获取内容
                        val articleData = BiliHttpApi.getArticleView(
                            cvId = fallbackId,
                            sessData = authRepository.sessionData ?: ""
                        ).getResponseData()
                        // 从 articleView 的 opus.content.paragraphs 获取段落
                        parseOpusParagraphs(articleData.opus?.content?.paragraphs ?: emptyList())
                    } else {
                        // 从 modules 列表中找到 MODULE_TYPE_CONTENT 类型的模块
                        val contentModule = responseData.item?.modules?.find { it.moduleType == "MODULE_TYPE_CONTENT" }
                        parseOpusParagraphs(contentModule?.moduleContent?.paragraphs ?: emptyList())
                    }
                }.getOrElse { emptyList() }
            }
        }
    }

    /**
     * 解析 Opus 段落数据
     */
    private fun parseOpusParagraphs(paragraphs: List<OpusParagraph>): List<ArticleParagraph> {
        return paragraphs.mapNotNull { para ->
            when (para.paraType) {
                1 -> { // TEXT
                    val nodes = para.text?.nodes?.mapNotNull { node ->
                        parseOpusTextNode(node)
                    } ?: emptyList()
                    if (nodes.isNotEmpty()) {
                        ArticleParagraph.TextParagraph(nodes = nodes)
                    } else null
                }
                2 -> { // PICTURES
                    val pictures = para.pic?.pics?.map { pic ->
                        ArticlePicture(
                            url = pic.url ?: "",
                            width = pic.width ?: 0,
                            height = pic.height ?: 0
                        )
                    } ?: emptyList()
                    if (pictures.isNotEmpty()) {
                        ArticleParagraph.PicturesParagraph(pictures = pictures)
                    } else null
                }
                3 -> { // LINE
                    val picture = para.line?.pic?.let { pic ->
                        ArticlePicture(
                            url = pic.url ?: "",
                            width = pic.width ?: 0,
                            height = pic.height ?: 0
                        )
                    }
                    ArticleParagraph.LineParagraph(picture = picture)
                }
                else -> null
            }
        }
    }

    /**
     * 解析 Opus 文本节点
     */
    private fun parseOpusTextNode(node: OpusTextNode): ArticleTextNode? {
        return when {
            node.emote != null -> ArticleTextNode(
                text = node.emote.text ?: node.rawText,
                type = TextNodeType.Emoji,
                emojiUrl = node.emote.iconUrl
            )
            node.rich != null -> ArticleTextNode(
                text = node.rich.text ?: node.rawText,
                type = TextNodeType.Link,
                linkUrl = node.rich.jumpUrl
            )
            node.word != null -> ArticleTextNode(
                text = node.word.words ?: node.rawText,
                type = TextNodeType.Plain,
                isBold = node.word.style?.bold ?: false,
                isItalic = node.word.style?.italic ?: false
            )
            else -> if (node.rawText.isNotBlank()) {
                ArticleTextNode(text = node.rawText, type = TextNodeType.Plain)
            } else null
        }
    }

    suspend fun getFollowedUsers(
        mid: Long,
        preferApiType: ApiType = ApiType.Web
    ): List<FollowedUser> {
        return when (preferApiType) {
            ApiType.Web -> {
                val result = mutableListOf<FollowedUser>()
                val firstResponse = BiliHttpApi.getUserFollow(
                    mid = mid,
                    sessData = authRepository.sessionData!!
                ).getResponseData()
                val userCount = firstResponse.total
                val pageCount = ceil((userCount.toFloat() / 50)).toInt()
                result.addAll(firstResponse.list.map { FollowedUser.fromHttpFollowedUser(it) })
                withContext(Dispatchers.IO) {
                    (2..pageCount).map { pageNumber ->
                        async {
                            BiliHttpApi.getUserFollow(
                                mid = mid,
                                pageNumber = pageNumber,
                                sessData = authRepository.sessionData!!
                            ).getResponseData()
                        }
                    }.awaitAll().forEach { userFollowData ->
                        result.addAll(userFollowData.list.map { FollowedUser.fromHttpFollowedUser(it) })
                    }
                }
                result
            }

            ApiType.App -> {
                val result = mutableListOf<FollowedUser>()
                val firstResponse = BiliHttpApi.getUserFollow(
                    mid = mid,
                    accessKey = authRepository.accessToken!!
                ).getResponseData()
                val userCount = firstResponse.total
                val pageCount = ceil((userCount.toFloat() / 50)).toInt()
                result.addAll(firstResponse.list.map { FollowedUser.fromHttpFollowedUser(it) })
                withContext(Dispatchers.IO) {
                    (2..pageCount).map { pageNumber ->
                        async {
                            BiliHttpApi.getUserFollow(
                                mid = mid,
                                pageNumber = pageNumber,
                                accessKey = authRepository.accessToken!!
                            ).getResponseData()
                        }
                    }.awaitAll().forEach { userFollowData ->
                        result.addAll(userFollowData.list.map { FollowedUser.fromHttpFollowedUser(it) })
                    }
                }
                result
            }
        }
    }
    
    suspend fun getUserInfo(mid: Long): UserInfoData {
        val response = BiliHttpApi.getUserInfo(
            uid = mid,
            sessData = authRepository.sessionData ?: ""
        ).getResponseData()
        return response
    }

    suspend fun getUserCardInfo(mid: Long): UserCardData {
        val response = BiliHttpApi.getUserCardInfo(
            mid = mid,
            sessData = authRepository.sessionData ?: ""
        ).getResponseData()
        return response
    }
}