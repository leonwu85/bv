package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.search.parseSearchAggregate
import bilibili.app.interfaces.v1.suggestionResult3Req
import bilibili.pagination.pagination
import bilibili.polymer.app.search.v1.SearchByTypeRequest
import bilibili.polymer.app.search.v1.searchByTypeRequest
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.search.SearchKeyword
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.BiliHttpProxyApi
import org.koin.core.annotation.Single

@Single
class SearchRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val searchSuggestStub
        get() = runCatching {
            bilibili.app.interfaces.v1.SearchGrpcKt.SearchCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private val searchResultStub
        get() = runCatching {
            bilibili.polymer.app.search.v1.SearchGrpcKt.SearchCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private val proxySearchResultStub
        get() = runCatching {
            bilibili.polymer.app.search.v1.SearchGrpcKt.SearchCoroutineStub(channelRepository.proxyChannel!!)
        }.getOrNull()

    /*private val searchStub
        get() = runCatching {
            SearchGrpcKt.SearchCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun search(
        keyword: String,
        page: Int = 1,
        pageSize: Int = 20,
        preferApiType: ApiType = ApiType.Web
    ): SearchData {
        return when (preferApiType) {
            ApiType.Web -> {
                val data = BiliHttpApi.search(
                    keyword = keyword,
                    page = page,
                    pageSize = pageSize,
                    sessData = authRepository.sessionData!!,
                ).getResponseData()
                SearchData.fromSearchResponse(data)
            }

            ApiType.App -> {
                val reply = searchStub?.searchV2(searchV2Req {
                    this.keyword = keyword
                    this.page = page
                    this.pageSize = pageSize
                })
                SearchData.fromSearchResponse(reply!!)
            }
        }
    }*/

    suspend fun getSearchHotwords(
        limit: Int = 30,
        preferApiType: ApiType = ApiType.Web
    ): List<SearchKeyword> {
        return when (preferApiType) {
            ApiType.Web -> BiliHttpApi.getWebSearchSquare(limit = limit)
                .getResponseData().trending.list
                .map { SearchKeyword.fromHttpWebHotword(it) }

            /*ApiType.App -> BiliHttpApi.getAppSearchSquare(limit = limit)
                .getResponseData()
                .firstOrNull { it.type == "trending" }
                ?.data?.list
                ?.map { SearchKeyword.fromHttpAppSquareDataItem(it) }
                ?: emptyList()*/

            ApiType.App -> BiliHttpApi.getSearchTrendRank(limit = limit)
                .getResponseData().list
                .map { SearchKeyword.fromHttpAppSearchTrendingHotword(it) }
        }.filter { it.keyword.isNotBlank() }
    }

    suspend fun getSearchRecommendKeywords(): List<SearchKeyword> {
        return BiliHttpApi.getSearchRecommend()
            .getResponseData()
            .list
            .map { SearchKeyword.fromHttpSearchRecommendItem(it) }
            .filter { it.keyword.isNotBlank() }
    }

    suspend fun getSearchTrendingRanking(
        limit: Int = 50
    ): List<SearchKeyword> {
        return BiliHttpApi.getSearchTrendRank(limit = limit)
            .getResponseData()
            .list
            .map { SearchKeyword.fromHttpAppSearchTrendingHotword(it) }
            .filter { it.keyword.isNotBlank() }
    }

    suspend fun getSearchSuggest(
        keyword: String,
        preferApiType: ApiType = ApiType.App
    ): List<String> {
        return when (preferApiType) {
            ApiType.Web -> BiliHttpApi.getKeywordSuggest(
                term = keyword,
                buvid = authRepository.buvid ?: "",
            ).suggests.map { it.value }

            //TODO 返回的关键词提示中可能包含通过avid/bvid/专栏id等的直达跳转结果项，需要过滤掉或进行单独处理
            ApiType.App -> searchSuggestStub?.suggest3(suggestionResult3Req {
                this.keyword = keyword
            })?.listList?.map { it.keyword } ?: emptyList()
        }
    }

    /**
     * 按分类进行搜索
     *
     * app 端的接口无法对视频投稿结果进行筛选搜索
     */
    suspend fun searchType(
        keyword: String,
        type: SearchType,
        tid: Int?,
        order: SearchFilterOrderType,
        duration: SearchFilterDuration,
        page: SearchTypePage,
        preferApiType: ApiType = ApiType.App,
        enableProxy: Boolean = false
    ): SearchTypeResult {
        if (type == SearchType.All) {
            val filtered = tid != null || order != SearchFilterOrderType.ComprehensiveSort || duration != SearchFilterDuration.All
            if (page.nextPageForWeb == 1 && !filtered) {
                val data = if (enableProxy) {
                    BiliHttpProxyApi.searchAll(keyword, authRepository.sessionData, authRepository.buvid3)
                } else {
                    BiliHttpApi.searchAll(keyword = keyword, sessData = authRepository.sessionData, buvid3 = authRepository.buvid3)
                }.getResponseData()
                return parseSearchAggregate(data)
            }
            return searchType(keyword, SearchType.Video, tid, order, duration, page, ApiType.Web, enableProxy)
        }
        // 专栏的 gRPC 卡片尚未在本项目协议模型中实现，固定使用稳定的 Web 结果。
        val effectiveApiType = if (type == SearchType.Article) ApiType.Web else preferApiType
        val effectiveTid = tid.takeIf { type == SearchType.Video }
        val effectiveOrder = order.takeIf { type == SearchType.Video }
            ?: SearchFilterOrderType.ComprehensiveSort
        val effectiveDuration = duration.takeIf { type == SearchType.Video }
            ?: SearchFilterDuration.All
        return when (effectiveApiType) {
            ApiType.Web -> {
                val response = if (enableProxy) {
                    BiliHttpProxyApi.searchType(
                        keyword = keyword,
                        type = type.httpTypeParam,
                        page = page.nextPageForWeb,
                        tid = effectiveTid,
                        order = effectiveOrder.httpOrderParam,
                        duration = effectiveDuration.httpDurationParam,
                        sessData = authRepository.sessionData,
                        dedeUserID = authRepository.mid,
                        buvid3 = authRepository.buvid3,
                    )
                } else {
                    BiliHttpApi.searchType(
                        keyword = keyword,
                        type = type.httpTypeParam,
                        page = page.nextPageForWeb,
                        tid = effectiveTid,
                        order = effectiveOrder.httpOrderParam,
                        duration = effectiveDuration.httpDurationParam,
                        sessData = authRepository.sessionData,
                        dedeUserID = authRepository.mid,
                        buvid3 = authRepository.buvid3,
                    )
                }.getResponseData()
                SearchTypeResult.fromSearchTypeResult(response)
            }

            ApiType.App -> {
                val searchTypeReply = runCatching {
                    val searchTypeRequest = searchByTypeRequest {
                        this.keyword = keyword
                        this.type = type.grpcTypeParam
                        categorySort = effectiveOrder.grpcOrderParam
                        userType = SearchByTypeRequest.UserType.ALL
                        userSort = SearchByTypeRequest.UserSort.USER_SORT_DEFAULT
                        pagination = pagination {
                            next = page.nextPageForApp
                        }
                    }
                    if (enableProxy) {
                        proxySearchResultStub?.searchByType(searchTypeRequest)
                            ?: throw IllegalStateException("Proxy search result stub is not initialized")
                    } else {
                        searchResultStub?.searchByType(searchTypeRequest)
                            ?: throw IllegalStateException("Search result stub is not initialized")
                    }
                }.onFailure { handleGrpcException(it) }.getOrThrow()
                SearchTypeResult.fromSearchTypeResult(searchTypeReply)
            }
        }
    }
}

data class SearchTypePage(
    val nextPageForWeb: Int = 1,
    val nextPageForApp: String = ""
)

enum class SearchType(
    val httpTypeParam: String,
    val grpcTypeParam: Int
) {
    All(httpTypeParam = "all", grpcTypeParam = -1),
    Video(httpTypeParam = "video", grpcTypeParam = 10),
    MediaBangumi(httpTypeParam = "media_bangumi", grpcTypeParam = 7),
    MediaFt(httpTypeParam = "media_ft", grpcTypeParam = 8),
    BiliUser(httpTypeParam = "bili_user", grpcTypeParam = 2),
    LiveRoom(httpTypeParam = "live_room", grpcTypeParam = 4),
    Article(httpTypeParam = "article", grpcTypeParam = 6)
}

enum class SearchFilterOrderType(
    val httpOrderParam: String?,
    val grpcOrderParam: SearchByTypeRequest.CategorySort
) {
    ComprehensiveSort(
        httpOrderParam = null,
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_DEFAULT
    ),
    MostClicks(
        httpOrderParam = "click",
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_CLICK_COUNT
    ),
    LatestPublish(
        httpOrderParam = "pubdate",
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_PUBLISH_TIME
    ),
    MostDanmaku(
        httpOrderParam = "dm",
        grpcOrderParam = SearchByTypeRequest.CategorySort.UNRECOGNIZED
    ),
    MostFavorites(
        httpOrderParam = "stow",
        grpcOrderParam = SearchByTypeRequest.CategorySort.UNRECOGNIZED
    ),
    MostComment(
        httpOrderParam = null,
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_COMMENT_COUNT
    ),
    MostLikes(
        httpOrderParam = null,
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_LIKE_COUNT
    );

    companion object {
        val webFilters =
            listOf(ComprehensiveSort, MostClicks, LatestPublish, MostDanmaku, MostFavorites)
        val allFilters =
            listOf(ComprehensiveSort, MostClicks, LatestPublish, MostComment, MostLikes)
    }
}

enum class SearchFilterDuration(
    val httpDurationParam: Int?,
    //val grpcOrderParam: SearchByTypeRequest.
) {
    All(null),
    LessThan10Minutes(1),
    Between10And30Minutes(2),
    Between30And60Minutes(3),
    MoreThan60Minutes(4);
}

data class SearchTypeResult(
    val videos: List<Video> = emptyList(),
    val pgcs: List<Pgc> = emptyList(),
    val users: List<User> = emptyList(),
    val liveRooms: List<LiveRoom> = emptyList(),
    val articles: List<Article> = emptyList(),
    val page: SearchTypePage,
    val pageSize: Int? = 20,
    val activities: List<Activity> = emptyList(),
    val hasMore: Boolean? = null
) {
    companion object {
        fun fromSearchTypeResult(result: dev.aaa1115910.biliapi.http.entity.search.SearchResultData): SearchTypeResult {
            val items = result.searchTypeResults
            return SearchTypeResult(
                videos = items.filterIsInstance<dev.aaa1115910.biliapi.http.entity.search.SearchVideoResult>().mapNotNull { runCatching { Video.fromSearchVideoResult(it) }.getOrNull() },
                pgcs = items.filterIsInstance<dev.aaa1115910.biliapi.http.entity.search.SearchMediaResult>().mapNotNull { runCatching { Pgc.fromSearchPgcResult(it) }.getOrNull() },
                users = items.filterIsInstance<dev.aaa1115910.biliapi.http.entity.search.SearchBiliUserResult>().mapNotNull { runCatching { User.fromSearchUserResult(it) }.getOrNull() },
                liveRooms = items.filterIsInstance<dev.aaa1115910.biliapi.http.entity.search.SearchLiveRoomResult>().mapNotNull { runCatching { LiveRoom.fromSearchLiveRoomResult(it) }.getOrNull() },
                articles = items.filterIsInstance<dev.aaa1115910.biliapi.http.entity.search.SearchArticleResult>().mapNotNull { runCatching { Article.fromSearchArticleResult(it) }.getOrNull() },
                page = SearchTypePage(nextPageForWeb = result.page + 1), pageSize = result.pageSize,
                hasMore = result.page < result.numPages
            )
        }

        fun fromSearchTypeResult(result: bilibili.polymer.app.search.v1.SearchByTypeResponse): SearchTypeResult {
            return SearchTypeResult(
                videos = result.itemsList.filter { it.cardItemCase == bilibili.polymer.app.search.v1.Item.CardItemCase.AV }
                    .mapNotNull { runCatching { Video.fromSearchVideoCard(it) }.getOrNull() },
                pgcs = result.itemsList.filter { it.cardItemCase == bilibili.polymer.app.search.v1.Item.CardItemCase.BANGUMI }
                    .mapNotNull { runCatching { Pgc.fromSearchPgcCard(it) }.getOrNull() },
                users = result.itemsList.filter { it.cardItemCase == bilibili.polymer.app.search.v1.Item.CardItemCase.AUTHOR }
                    .mapNotNull { runCatching { User.fromSearchUserCard(it) }.getOrNull() },
                page = SearchTypePage(nextPageForApp = result.pagination.next),
                hasMore = result.pagination.next.isNotBlank()
            )
        }
    }

    interface SearchTypeResultItem

    data class Activity(val title: String, val cover: String, val description: String, val url: String) : SearchTypeResultItem

    data class Video(
        val aid: Long,
        val bvid: String,
        val title: String,
        val cover: String,
        val author: String,
        val upId: Long = 0,
        val upFace: String = "",
        val duration: Int,
        val play: Long,
        val danmaku: Int,
        val pubTime: Int,
        val pubDate: Int
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchVideoResult(video: dev.aaa1115910.biliapi.http.entity.search.SearchVideoResult) =
                Video(
                    aid = video.aid,
                    bvid = video.bvid,
                    title = video.title,
                    cover = if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic,
                    author = video.author,
                    upId = video.mid,
                    upFace = video.upic,
                    duration = convertStringTimeToSeconds(video.duration),
                    play = video.play,
                    danmaku = video.danmaku,
                    pubTime = video.pubDate,
                    pubDate = video.pubDate
                )

            fun fromSearchVideoCard(video: bilibili.polymer.app.search.v1.Item) =
                Video(
                    aid = video.param.toLong(),
                    bvid = video.av.share.video.bvid,
                    title = video.av.title,
                    cover = video.av.cover,
                    author = video.av.author,
                    upId = video.av.mid,
                    upFace = video.av.face,
                    duration = convertStringTimeToSeconds(video.av.duration),
                    play = video.av.play,
                    danmaku = video.av.danmaku,
                    pubTime = video.av.ptime,
                    pubDate = video.av.ptime
                )
        }
    }

    data class Pgc(
        val title: String,
        val cover: String,
        val star: Float,
        val seasonId: Int
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchPgcResult(pgc: dev.aaa1115910.biliapi.http.entity.search.SearchMediaResult) =
                Pgc(
                    title = pgc.title,
                    cover = pgc.cover,
                    star = pgc.mediaScore.score,
                    seasonId = pgc.seasonId
                )

            fun fromSearchPgcCard(pgc: bilibili.polymer.app.search.v1.Item) =
                Pgc(
                    title = pgc.bangumi.title,
                    cover = pgc.bangumi.cover,
                    star = pgc.bangumi.rating.toFloat(),
                    seasonId = pgc.bangumi.seasonId.toInt()
                )
        }
    }

    data class User(
        val mid: Long,
        val name: String,
        val avatar: String,
        val sign: String,
        val fans: Long? = null,
        val recentVideos: List<Video> = emptyList()
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchUserResult(user: dev.aaa1115910.biliapi.http.entity.search.SearchBiliUserResult) =
                User(
                    mid = user.mid,
                    name = user.uname,
                    avatar = if (user.upic.startsWith("//")) "https:${user.upic}" else user.upic,
                    sign = user.usign
                )

            fun fromSearchUserCard(user: bilibili.polymer.app.search.v1.Item) =
                User(
                    mid = user.param.toLong(),
                    name = user.author.title,
                    avatar = user.author.cover,
                    sign = user.author.sign
                )
        }
    }

    data class LiveRoom(
        val roomId: Int,
        val title: String,
        val uname: String,
        val cover: String,
        val online: Int,
        val cateName: String,
        val uid: Long,
        val uface: String
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchLiveRoomResult(liveRoom: dev.aaa1115910.biliapi.http.entity.search.SearchLiveRoomResult) =
                LiveRoom(
                    roomId = liveRoom.roomId,
                    title = liveRoom.title,
                    uname = liveRoom.uname,
                    cover = if (liveRoom.cover.startsWith("//")) "https:${liveRoom.cover}" else liveRoom.cover,
                    online = liveRoom.online,
                    cateName = liveRoom.cateName,
                    uid = liveRoom.uid,
                    uface = if (liveRoom.uFace.startsWith("//")) "https:${liveRoom.uFace}" else liveRoom.uFace
                )
        }
    }

    data class Article(
        val id: Int,
        val title: String,
        val description: String,
        val cover: String?,
        val authorId: Long,
        val categoryName: String,
        val view: Long,
        val like: Int,
        val reply: Int,
        val pubTime: Int
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchArticleResult(
                article: dev.aaa1115910.biliapi.http.entity.search.SearchArticleResult
            ) = Article(
                id = article.id,
                title = article.title,
                description = article.desc,
                cover = article.imageUrls.firstOrNull()?.let {
                    if (it.startsWith("//")) "https:$it" else it
                },
                authorId = article.mid,
                categoryName = article.categoryName,
                view = article.view,
                like = article.like,
                reply = article.reply,
                pubTime = article.pubTime
            )
        }
    }
}

private fun convertStringTimeToSeconds(time: String): Int {
    val parts = time.split(":")
    val hours = if (parts.size == 3) parts[0].toInt() else 0
    val minutes = parts[parts.size - 2].toInt()
    val seconds = parts[parts.size - 1].toInt()
    return (hours * 3600) + (minutes * 60) + seconds
}
