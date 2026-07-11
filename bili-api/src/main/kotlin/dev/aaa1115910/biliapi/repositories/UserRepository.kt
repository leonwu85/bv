package dev.aaa1115910.biliapi.repositories

import bilibili.app.dynamic.v2.DynamicGrpcKt
import bilibili.app.dynamic.v2.Refresh
import bilibili.app.dynamic.v2.UserInfo
import bilibili.app.dynamic.v2.dynAllReq
import bilibili.app.dynamic.v2.dynDetailReq
import bilibili.app.dynamic.v2.dynVideoReq
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.DynamicData
import dev.aaa1115910.biliapi.entity.user.DynamicEmoteDraft
import dev.aaa1115910.biliapi.entity.user.DynamicEmotePackageDraft
import dev.aaa1115910.biliapi.entity.user.DynamicImageDraft
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicMentionDraft
import dev.aaa1115910.biliapi.entity.user.DynamicPublishDraft
import dev.aaa1115910.biliapi.entity.user.DynamicReplyOption
import dev.aaa1115910.biliapi.entity.user.DynamicReserveDraft
import dev.aaa1115910.biliapi.entity.user.DynamicRichContent
import dev.aaa1115910.biliapi.entity.user.DynamicTopicDraft
import dev.aaa1115910.biliapi.entity.user.DynamicTopicFeed
import dev.aaa1115910.biliapi.entity.user.DynamicTopicFeedItem
import dev.aaa1115910.biliapi.entity.user.DynamicUpData
import dev.aaa1115910.biliapi.entity.user.DynamicVideoData
import dev.aaa1115910.biliapi.entity.user.FollowedUser
import dev.aaa1115910.biliapi.entity.user.ArticleParagraph
import dev.aaa1115910.biliapi.entity.user.ArticlePicture
import dev.aaa1115910.biliapi.entity.user.DynamicVoteDraft
import dev.aaa1115910.biliapi.entity.user.ArticleTextNode
import dev.aaa1115910.biliapi.entity.user.OpusDetailResult
import dev.aaa1115910.biliapi.entity.user.TextNodeType
import dev.aaa1115910.biliapi.entity.user.SpaceVideoData
import dev.aaa1115910.biliapi.entity.user.SpaceVideoOrder
import dev.aaa1115910.biliapi.entity.user.SpaceVideoPage
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.dynamic.OpusParagraph
import dev.aaa1115910.biliapi.http.entity.dynamic.OpusTextNode
import dev.aaa1115910.biliapi.http.entity.dynamic.ArticleViewData
import dev.aaa1115910.biliapi.http.entity.dynamic.DynamicTopicFeedResponse
import dev.aaa1115910.biliapi.http.entity.user.FollowAction
import dev.aaa1115910.biliapi.http.entity.user.FollowActionSource
import dev.aaa1115910.biliapi.http.entity.user.RelationType
import dev.aaa1115910.biliapi.http.entity.user.AppUserSpaceData
import dev.aaa1115910.biliapi.http.entity.user.UserCardData
import dev.aaa1115910.biliapi.http.entity.user.UserInfoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.koin.core.annotation.Single
import kotlin.math.ceil
import kotlin.random.Random

@Single
class UserRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val dynamicStub
        get() = runCatching {
            DynamicGrpcKt.DynamicCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private fun dynamicTextContent(text: String): JsonObject = buildJsonObject {
        put("raw_text", text)
        put("type", 1)
        put("biz_id", "")
    }

    private fun dynamicVoteContent(title: String, voteId: Long): JsonObject = buildJsonObject {
        put("raw_text", title)
        put("type", 4)
        put("biz_id", voteId.toString())
    }

    private fun dynamicRichContent(content: DynamicRichContent): JsonObject = buildJsonObject {
        put("raw_text", content.rawText)
        put("type", content.type)
        put("biz_id", content.bizId)
    }

    private fun dynamicImage(image: DynamicImageDraft): JsonObject = buildJsonObject {
        put("img_width", image.imgWidth)
        put("img_height", image.imgHeight)
        put("img_size", image.imgSize)
        put("img_src", image.imgSrc)
    }

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

    suspend fun blacklistUser(
        mid: Long,
        preferApiType: ApiType = ApiType.Web
    ): Boolean = modifyFollow(mid, FollowAction.AddBlackList, preferApiType)

    suspend fun unblacklistUser(
        mid: Long,
        preferApiType: ApiType = ApiType.Web
    ): Boolean = modifyFollow(mid, FollowAction.DelBlackList, preferApiType)

    suspend fun removeFan(
        mid: Long,
        preferApiType: ApiType = ApiType.Web
    ): Boolean = modifyFollow(mid, FollowAction.DelFan, preferApiType)

    suspend fun reportDynamic(
        accusedUid: Long,
        dynamicId: String,
        reasonType: Int,
        reasonDesc: String? = null
    ) {
        val response = BiliHttpApi.reportDynamic(
            accusedUid = accusedUid,
            dynamicId = dynamicId,
            reasonType = reasonType,
            reasonDesc = reasonDesc,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录")
        )
        if (response.code != 0) {
            throw Exception("举报失败: ${response.message}")
        }
    }

    suspend fun createTextDynamic(
        text: String,
        voteDraft: DynamicVoteDraft? = null
    ): String = createDynamic(
        DynamicPublishDraft(
            text = text,
            voteDraft = voteDraft
        )
    )

    suspend fun createDynamic(draft: DynamicPublishDraft): String {
        val sessData = authRepository.sessionData ?: error("账号未登录")
        val csrf = authRepository.biliJct ?: error("账号未登录")
        val mid = authRepository.mid ?: error("账号未登录")
        val currentVoteDraft = draft.voteDraft
        val voteId = currentVoteDraft?.let { createDynamicVote(it) }
        val contents = buildList {
            val richContents = draft.richContents.filter { it.rawText.isNotBlank() }
            if (richContents.isNotEmpty()) {
                richContents.forEach { add(dynamicRichContent(it)) }
            } else if (draft.text.isNotBlank()) {
                add(dynamicTextContent(draft.text.trim()))
            }
            if (voteId != null) {
                if (isEmpty()) {
                    add(dynamicTextContent("我发起了一个投票"))
                }
                add(dynamicTextContent(" "))
                add(dynamicVoteContent(checkNotNull(currentVoteDraft).title, voteId))
                add(dynamicTextContent(" "))
            }
        }
        require(contents.isNotEmpty() || draft.pictures.isNotEmpty() || draft.reserve != null) {
            "请输入动态内容"
        }
        val attachCard = draft.reserve?.let { reserve ->
            buildJsonObject {
                putJsonObject("common_card") {
                    put("type", 14)
                    put("biz_id", reserve.id)
                    put("reserve_source", 0)
                    put("reserve_lottery", 0)
                }
            }
        } ?: JsonNull
        val hasOption =
            draft.privatePub || draft.publishTime != null || draft.replyOption != DynamicReplyOption.Allow
        val dynReq = buildJsonObject {
            putJsonObject("content") {
                put("contents", JsonArray(contents))
                if (draft.title.isNotBlank()) put("title", draft.title.trim())
            }
            if (hasOption) {
                putJsonObject("option") {
                    if (draft.privatePub) put("private_pub", 1)
                    draft.publishTime?.let { put("timer_pub_time", it) }
                    when (draft.replyOption) {
                        DynamicReplyOption.Allow -> Unit
                        DynamicReplyOption.Close -> put("close_comment", 1)
                        DynamicReplyOption.Choose -> put("up_choose_comment", 1)
                    }
                }
            }
            put("scene", if (draft.pictures.isNotEmpty()) 2 else 1)
            if (draft.pictures.isNotEmpty()) {
                put("pics", JsonArray(draft.pictures.map(::dynamicImage)))
            }
            put("attach_card", attachCard)
            put("upload_id", "${mid}_${System.currentTimeMillis() / 1000}_${Random.nextInt(1000, 10000)}")
            putJsonObject("meta") {
                putJsonObject("app_meta") {
                    put("from", "create.dynamic.web")
                    put("mobi_app", "web")
                }
            }
            draft.topic?.let { topic ->
                putJsonObject("topic") {
                    put("id", topic.id)
                    put("name", topic.name)
                    put("from_source", "dyn.web.list")
                    put("from_topic_id", 0)
                }
            }
        }
        val response = BiliHttpApi.createDynamic(
            dynReq = dynReq,
            csrf = csrf,
            sessData = sessData,
            dedeUserID = mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        val data = response.getResponseData()
        return data.dynIdStr.ifBlank { data.dynId.takeIf { it > 0L }?.toString().orEmpty() }
    }

    suspend fun uploadDynamicImage(fileName: String, bytes: ByteArray): DynamicImageDraft {
        val response = BiliHttpApi.uploadDynamicImage(
            fileName = fileName,
            bytes = bytes,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录"),
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        val data = response.getResponseData()
        return DynamicImageDraft(
            imgSrc = data.imageUrl,
            imgWidth = data.imageWidth,
            imgHeight = data.imageHeight,
            imgSize = data.imgSize
        )
    }

    suspend fun getDynamicTopicRcmd(): List<DynamicTopicDraft> {
        val response = BiliHttpApi.getDynamicTopicRcmd(
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        return response.getResponseData().topicItems
            .filter { it.id > 0L && it.name.isNotBlank() }
            .map { DynamicTopicDraft(id = it.id, name = it.name) }
    }

    suspend fun getDynamicTopicFeed(
        topicId: Long,
        sortBy: Int = 0,
        offset: String? = null
    ): DynamicTopicFeed {
        val response = BiliHttpApi.getDynamicTopicFeed(
            topicId = topicId,
            sortBy = sortBy,
            offset = offset,
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        return response.getResponseData().toDynamicTopicFeed()
    }

    suspend fun expandDynamicTopicFold(
        topicId: Long,
        sortBy: Int = 0
    ): DynamicTopicFeed {
        val response = BiliHttpApi.getDynamicTopicFold(
            topicId = topicId,
            sortBy = sortBy,
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        return response.getResponseData().toDynamicTopicFeed()
    }

    private fun DynamicTopicFeedResponse.toDynamicTopicFeed(): DynamicTopicFeed {
        val cards = topicCardList ?: return DynamicTopicFeed()
        return DynamicTopicFeed(
            hasMore = cards.hasMore,
            offset = cards.offset,
            items = cards.items.mapNotNull { card ->
                card.dynamicCardItem?.let { dynamic ->
                    DynamicTopicFeedItem.DynamicCard(DynamicItem.fromDynamicItem(dynamic))
                } ?: card.foldCardItem?.let { fold ->
                    DynamicTopicFeedItem.FoldCard(
                        count = fold.foldCount,
                        description = fold.foldDesc
                    )
                }
            }
        )
    }

    suspend fun searchDynamicTopic(keyword: String, content: String = ""): List<DynamicTopicDraft> {
        val response = BiliHttpApi.searchDynamicTopic(
            keywords = keyword,
            content = content,
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        return response.getResponseData().topicItems
            .filter { it.id > 0L && it.name.isNotBlank() }
            .map { DynamicTopicDraft(id = it.id, name = it.name) }
    }

    suspend fun searchDynamicMention(keyword: String? = null): List<DynamicMentionDraft> {
        val response = BiliHttpApi.searchDynamicMention(
            keyword = keyword,
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        return response.getResponseData().groups
            .flatMap { it.items }
            .filter { it.uid.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { it.uid }
            .map {
                DynamicMentionDraft(
                    uid = it.uid,
                    name = it.name,
                    face = it.face,
                    fans = it.fans
                )
            }
    }

    suspend fun getDynamicEmotePackages(): List<DynamicEmotePackageDraft> {
        val response = BiliHttpApi.getDynamicEmotes(
            business = "reply",
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        return response.getResponseData().packages
            .map {
                DynamicEmotePackageDraft(
                    url = it.url,
                    type = it.type,
                    emotes = it.emote
                        .filter { emote -> emote.text.isNotBlank() }
                        .distinctBy { emote -> emote.text }
                        .map { emote ->
                            DynamicEmoteDraft(
                                text = emote.text,
                                url = emote.url,
                                size = emote.meta.size,
                                alias = emote.meta.alias
                            )
                        }
                )
            }
            .filter { it.emotes.isNotEmpty() }
    }

    suspend fun createLiveReserve(
        title: String,
        livePlanStartTime: Long,
        subType: Int = 0
    ): DynamicReserveDraft {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotBlank()) { "请填写直播预约标题" }
        val response = BiliHttpApi.createReserve(
            title = normalizedTitle,
            livePlanStartTime = livePlanStartTime,
            subType = subType,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录"),
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        val sid = response.getResponseData().sid
        check(sid > 0L) { "直播预约创建失败" }
        return DynamicReserveDraft(
            id = sid,
            title = normalizedTitle,
            livePlanStartTime = livePlanStartTime,
            subType = subType
        )
    }

    suspend fun updateLiveReserve(
        reserve: DynamicReserveDraft
    ): DynamicReserveDraft {
        val response = BiliHttpApi.updateReserve(
            sid = reserve.id,
            title = reserve.title.trim(),
            livePlanStartTime = reserve.livePlanStartTime,
            subType = reserve.subType,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录"),
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        return reserve.copy(id = response.getResponseData().sid.takeIf { it > 0L } ?: reserve.id)
    }

    suspend fun getLiveReserveInfo(sid: Long): DynamicReserveDraft {
        val response = BiliHttpApi.getReserveInfo(
            sid = sid,
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        val data = response.getResponseData()
        return DynamicReserveDraft(
            id = data.id,
            title = data.title,
            livePlanStartTime = data.livePlanStartTime
        )
    }

    private suspend fun createDynamicVote(voteDraft: DynamicVoteDraft): Long {
        val sessData = authRepository.sessionData ?: error("账号未登录")
        val csrf = authRepository.biliJct ?: error("账号未登录")
        val mid = authRepository.mid ?: error("账号未登录")
        val normalizedOptions = voteDraft.options
            .map(String::trim)
            .filter(String::isNotEmpty)
        require(voteDraft.title.isNotBlank()) { "请填写投票标题" }
        require(normalizedOptions.size >= 2) { "投票至少需要两个选项" }
        val voteInfo = buildJsonObject {
            put("title", voteDraft.title.trim())
            put("desc", voteDraft.desc.trim())
            put("type", 0)
            put("choice_cnt", voteDraft.choiceCnt.coerceIn(1, normalizedOptions.size))
            put("duration", voteDraft.durationSeconds.coerceAtLeast(60))
            putJsonArray("options") {
                normalizedOptions.forEach { option ->
                    add(
                        buildJsonObject {
                            put("opt_desc", option)
                            put("img_url", "")
                        }
                    )
                }
            }
            put("only_fans_level", 0)
            put("vote_publisher", mid)
        }
        val response = BiliHttpApi.createVote(
            voteInfo = voteInfo,
            csrf = csrf,
            sessData = sessData,
            dedeUserID = mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        return response.getResponseData().voteId.also {
            check(it > 0L) { "投票创建失败" }
        }
    }

    suspend fun getDynamicVoteInfo(voteId: Long) =
        BiliHttpApi.getDynamicVoteInfo(
            voteId = voteId,
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        ).getResponseData()

    suspend fun doDynamicVote(
        voteId: Long,
        votes: List<Int>,
        dynamicId: String? = null,
        anonymous: Boolean = false
    ) = BiliHttpApi.doDynamicVote(
        voteId = voteId,
        votes = votes,
        dynamicId = dynamicId,
        anonymous = anonymous,
        csrf = authRepository.biliJct ?: error("账号未登录"),
        sessData = authRepository.sessionData ?: error("账号未登录"),
        dedeUserID = authRepository.mid,
        buvid3 = authRepository.buvid3
    ).getResponseData()

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
                    sessData = authRepository.sessionData ?: "",
                    dedeUserID = authRepository.mid
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
                    sessData = authRepository.sessionData,
                    dedeUserID = authRepository.mid,
                    buvid3 = authRepository.buvid3
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
            ApiType.Web, ApiType.App -> {
                val responseData = BiliHttpApi.getDynamicList(
                    type = type,
                    page = page,
                    offset = offset,
                    sessData = authRepository.sessionData,
                    dedeUserID = authRepository.mid,
                    buvid3 = authRepository.buvid3
                ).getResponseData()
                DynamicData.fromDynamicData(responseData)
            }

//            ApiType.App -> {
//                var result: DynamicData? = null
//                runCatching {
//                    val dynAllReply = dynamicStub?.dynAll(dynAllReq {
//                        this.page = page
//                        this.offset = offset
//                        this.updateBaseline = updateBaseline
//                        localTime = 8
//                        refreshType =
//                            if (offset == "") Refresh.refresh_new else Refresh.refresh_history
//                    })
//                    result = DynamicData.fromDynamicData(dynAllReply!!)
//                }.onFailure {
//                    handleGrpcException(it)
//                }
//                result!!
//            }
        }
    }

    suspend fun getDynamicsByUp(
        mid: Long,
        page: Int,
        offset: String,
        updateBaseline: String,
        preferApiType: ApiType = ApiType.Web
    ): DynamicData {
        return when (preferApiType) {
            ApiType.Web, ApiType.App -> {
                val responseData = BiliHttpApi.getDynamicList(
                    page = page,
                    offset = offset,
                    hostMid = mid,
                    sessData = authRepository.sessionData,
                    dedeUserID = authRepository.mid,
                    buvid3 = authRepository.buvid3
                ).getResponseData()
                DynamicData.fromDynamicData(responseData)
            }
        }
    }

    suspend fun getDynamicFollowUp(
        preferApiType: ApiType = ApiType.Web
    ): DynamicUpData {
        return when (preferApiType) {
            ApiType.Web, ApiType.App -> {
                val responseData = BiliHttpApi.getDynamicFollowUp(
                    sessData = authRepository.sessionData,
                    dedeUserID = authRepository.mid,
                    buvid3 = authRepository.buvid3
                ).getResponseData()
                DynamicUpData.fromFollowUpData(responseData)
            }
        }
    }

    suspend fun getDynamicUnreadCount(): Int {
        if (authRepository.sessionData.isNullOrBlank()) return 0
        return BiliHttpApi.getDynamicEntrance(
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        ).getResponseData().unreadCount()
    }

    suspend fun getDynamicUpList(
        offset: String,
        preferApiType: ApiType = ApiType.Web
    ): DynamicUpData {
        return when (preferApiType) {
            ApiType.Web, ApiType.App -> {
                val responseData = BiliHttpApi.getDynamicUpList(
                    offset = offset,
                    sessData = authRepository.sessionData,
                    dedeUserID = authRepository.mid,
                    buvid3 = authRepository.buvid3
                ).getResponseData()
                DynamicUpData.fromUpListData(responseData)
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
                    sessData = authRepository.sessionData ?: "",
                    csrf = authRepository.biliJct
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
    ): List<ArticleParagraph> = getOpusDetailResult(opusId, preferApiType).paragraphs

    /**
     * 获取 Opus (专栏/图文) 详情及评论区元数据
     */
    suspend fun getOpusDetailResult(
        opusId: String,
        preferApiType: ApiType = ApiType.Web
    ): OpusDetailResult {
        return when (preferApiType) {
            ApiType.Web -> getOpusDetailResultFromWeb(opusId)
            ApiType.App -> {
                // App 端暂时使用 Web API
                getOpusDetailResultFromWeb(opusId)
            }
        }
    }

    private suspend fun getOpusDetailResultFromWeb(opusId: String): OpusDetailResult {
        return runCatching {
            val responseData = BiliHttpApi.getOpusDetail(
                opusId = opusId,
                sessData = authRepository.sessionData ?: ""
            ).getResponseData()

            val basic = responseData.item?.basic
            val commentId = basic?.commentIdStr?.toLongOrNull() ?: 0L
            val commentType = basic?.commentType?.toLong() ?: 0L

            // 检查是否需要 fallback 到传统专栏 API
            val fallbackId = responseData.fallback?.id
            val paragraphs = if (fallbackId != null) {
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

            OpusDetailResult(
                paragraphs = paragraphs,
                commentId = commentId,
                commentType = commentType
            )
        }.getOrElse {
            OpusDetailResult(paragraphs = emptyList())
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

    suspend fun getFanUsers(
        mid: Long,
        preferApiType: ApiType = ApiType.Web
    ): List<FollowedUser> {
        return when (preferApiType) {
            ApiType.Web -> {
                val result = mutableListOf<FollowedUser>()
                val firstResponse = BiliHttpApi.getUserFans(
                    mid = mid,
                    sessData = authRepository.sessionData!!
                ).getResponseData()
                val userCount = firstResponse.total
                val pageCount = ceil((userCount.toFloat() / 50)).toInt()
                result.addAll(firstResponse.list.map { FollowedUser.fromHttpFollowedUser(it) })
                withContext(Dispatchers.IO) {
                    (2..pageCount).map { pageNumber ->
                        async {
                            BiliHttpApi.getUserFans(
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
                val firstResponse = BiliHttpApi.getUserFans(
                    mid = mid,
                    accessKey = authRepository.accessToken!!
                ).getResponseData()
                val userCount = firstResponse.total
                val pageCount = ceil((userCount.toFloat() / 50)).toInt()
                result.addAll(firstResponse.list.map { FollowedUser.fromHttpFollowedUser(it) })
                withContext(Dispatchers.IO) {
                    (2..pageCount).map { pageNumber ->
                        async {
                            BiliHttpApi.getUserFans(
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

    suspend fun getUserCardInfo(
        mid: Long,
        photo: Boolean = false
    ): UserCardData {
        val response = BiliHttpApi.getUserCardInfo(
            mid = mid,
            photo = photo,
            sessData = authRepository.sessionData ?: ""
        ).getResponseData()
        return response
    }

    suspend fun getAppUserSpace(mid: Long): AppUserSpaceData {
        return BiliHttpApi.getAppUserSpace(mid).getResponseData()
    }
}
