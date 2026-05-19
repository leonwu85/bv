package dev.aaa1115910.bv.viewmodel.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.user.DynamicEmotePackageDraft
import dev.aaa1115910.biliapi.entity.user.DynamicImageDraft
import dev.aaa1115910.biliapi.entity.user.DynamicMentionDraft
import dev.aaa1115910.biliapi.entity.user.DynamicPublishDraft
import dev.aaa1115910.biliapi.entity.user.DynamicReserveDraft
import dev.aaa1115910.biliapi.entity.user.DynamicTopicDraft
import dev.aaa1115910.biliapi.entity.user.DynamicVoteDraft
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicUpUser
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.DynamicTabType
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository

@KoinViewModel
class DynamicViewModel(
    private val bvUserRepository: BvUserRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    // 视频动态
    val dynamicVideoList = mutableStateListOf<DynamicVideo>()
    private var currentVideoPage = 0
    var loadingVideo by mutableStateOf(false)
    var videoHasMore by mutableStateOf(true)
    private var videoHistoryOffset: String? = null
    private var videoUpdateBaseline: String? = null

    // 全部动态
    val dynamicAllList = mutableStateListOf<DynamicItem>()
    private val tempBlockedMids = mutableStateListOf<Long>()
    private var currentAllPage = 0
    var loadingAll by mutableStateOf(false)
    var allHasMore by mutableStateOf(true)
    private var allHistoryOffset: String? = null
    private var allUpdateBaseline: String? = null

    // PGC动态（追番追剧）
    val dynamicPgcList = mutableStateListOf<DynamicItem>()
    private var currentPgcPage = 0
    var loadingPgc by mutableStateOf(false)
    var pgcHasMore by mutableStateOf(true)
    private var pgcHistoryOffset: String? = null
    private var pgcUpdateBaseline: String? = null

    // 专栏动态
    val dynamicArticleList = mutableStateListOf<DynamicItem>()
    private var currentArticlePage = 0
    var loadingArticle by mutableStateOf(false)
    var articleHasMore by mutableStateOf(true)
    private var articleHistoryOffset: String? = null
    private var articleUpdateBaseline: String? = null

    // UP 主筛选动态
    val dynamicUpList = mutableStateListOf<DynamicItem>()
    val followUpList = mutableStateListOf<DynamicUpUser>()
    val liveUpList = mutableStateListOf<DynamicUpUser>()
    var selectedUp by mutableStateOf<DynamicUpUser?>(null)
    var liveUpCount by mutableStateOf(0)
    private var currentUpPage = 0
    var loadingUp by mutableStateOf(false)
    var upHasMore by mutableStateOf(true)
    private var upHistoryOffset: String? = null
    private var upUpdateBaseline: String? = null
    private var upLoadGeneration = 0
    var loadingFollowUp by mutableStateOf(false)
    var followUpHasMore by mutableStateOf(true)
    private var followUpOffset: String? = null

    val isLogin get() = bvUserRepository.isLogin
    var creatingDynamic by mutableStateOf(false)
        private set
    val selfUp
        get() = DynamicUpUser(
            face = bvUserRepository.avatar,
            hasUpdate = false,
            mid = bvUserRepository.uid,
            uname = bvUserRepository.username.ifBlank { "我" }
        )

    init {
        println("=====init DynamicViewModel")
    }

    suspend fun loadMoreVideo() {
        if (!loadingVideo) loadVideoData()
    }

    suspend fun loadMoreAll() {
        if (!loadingAll) loadAllData()
    }

    fun tempBlockAuthor(mid: Long) {
        if (mid <= 0L) return
        if (!tempBlockedMids.contains(mid)) tempBlockedMids.add(mid)
        dynamicAllList.removeAll { it.author.mid == mid }
        dynamicVideoList.removeAll { it.authorId == mid }
        dynamicPgcList.removeAll { it.author.mid == mid }
        dynamicArticleList.removeAll { it.author.mid == mid }
        dynamicUpList.removeAll { it.author.mid == mid }
    }

    suspend fun loadMorePgc() {
        if (!loadingPgc) loadPgcData()
    }

    suspend fun loadMoreArticle() {
        if (!loadingArticle) loadArticleData()
    }

    suspend fun loadMoreUp() {
        if (!loadingUp) loadUpData()
    }

    fun selectUp(up: DynamicUpUser) {
        Log.i("DynamicViewModel", "select up: mid=${up.mid}, name=${up.uname}")
        selectedUp = up.copy(hasUpdate = false)
        clearFollowUpUpdate(up.mid)
        clearUpData()
    }

    private fun clearFollowUpUpdate(mid: Long) {
        val index = followUpList.indexOfFirst { it.mid == mid }
        if (index != -1 && followUpList[index].hasUpdate) {
            followUpList[index] = followUpList[index].copy(hasUpdate = false)
        }
    }

    suspend fun loadMoreByType(type: DynamicTabType) {
        when (type) {
            DynamicTabType.All -> loadMoreAll()
            DynamicTabType.Video -> loadMoreVideo()
            DynamicTabType.Pgc -> loadMorePgc()
            DynamicTabType.Article -> loadMoreArticle()
            DynamicTabType.Up -> loadMoreUp()
        }
    }

    private suspend fun loadVideoData() {
        if (!videoHasMore || !bvUserRepository.isLogin) return
        loadingVideo = true
        logger.fInfo { "Load more dynamic videos [apiType=${Prefs.apiType}, offset=$videoHistoryOffset, page=${currentVideoPage + 1}]" }
        runCatching {
            val dynamicVideoData = userRepository.getDynamicVideos(
                page = ++currentVideoPage,
                offset = videoHistoryOffset ?: "",
                updateBaseline = videoUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            dynamicVideoList.addAllWithMainContext(
                dynamicVideoData.videos.filter { it.authorId !in tempBlockedMids }
            )
            videoHistoryOffset = dynamicVideoData.historyOffset
            videoUpdateBaseline = dynamicVideoData.updateBaseline
            videoHasMore = dynamicVideoData.hasMore

            logger.fInfo { "Load dynamic video list page: ${currentVideoPage},size: ${dynamicVideoData.videos.size}" }
            val avList = dynamicVideoData.videos.map {
                it.aid
            }
            logger.fInfo { "Load dynamic video size: ${avList.size}" }
            logger.info { "Load dynamic video list ${avList}}" }
        }.onFailure {
            logger.fWarn { "Load dynamic video list failed: ${it.stackTraceToString()}" }
            when (it) {
                is AuthFailureException -> {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.exception_auth_failure)
                            .toast(BVApp.context)
                    }
                    logger.fInfo { "User auth failure" }
                    if (!BuildConfig.DEBUG) bvUserRepository.logout()
                }

                else -> {
                    withContext(Dispatchers.Main) {
                        "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            loadingVideo = false
        }
    }

    private suspend fun loadAllData() {
        if (!allHasMore || !bvUserRepository.isLogin) return
        loadingAll = true
        logger.fInfo { "Load more dynamic all [apiType=${Prefs.apiType}, offset=$allHistoryOffset, page=${currentAllPage + 1}]" }
        runCatching {
            val dynamicData = userRepository.getDynamics(
                page = ++currentAllPage,
                offset = allHistoryOffset ?: "",
                updateBaseline = allUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            dynamicAllList.addAll(dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids })
            allHistoryOffset = dynamicData.historyOffset
            allUpdateBaseline = dynamicData.updateBaseline
            allHasMore = dynamicData.hasMore

            logger.fInfo { "Load dynamic all list page: ${currentAllPage},size: ${dynamicData.dynamics.size}" }
        }.onFailure {
            logger.fWarn { "Load dynamic all list failed: ${it.stackTraceToString()}" }
            when (it) {
                is AuthFailureException -> {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.exception_auth_failure)
                            .toast(BVApp.context)
                    }
                    logger.fInfo { "User auth failure" }
                }

                else -> {
                    withContext(Dispatchers.Main) {
                        "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            loadingAll = false
        }
    }

    fun clearVideoData() {
        dynamicVideoList.clear()
        currentVideoPage = 0
        loadingVideo = false
        videoHasMore = true
        videoHistoryOffset = null
    }

    fun clearAllData() {
        dynamicAllList.clear()
        currentAllPage = 0
        loadingAll = false
        allHasMore = true
        allHistoryOffset = null
    }

    fun clearPgcData() {
        dynamicPgcList.clear()
        currentPgcPage = 0
        loadingPgc = false
        pgcHasMore = true
        pgcHistoryOffset = null
    }

    fun clearArticleData() {
        dynamicArticleList.clear()
        currentArticlePage = 0
        loadingArticle = false
        articleHasMore = true
        articleHistoryOffset = null
    }

    fun clearUpData() {
        upLoadGeneration += 1
        dynamicUpList.clear()
        currentUpPage = 0
        loadingUp = false
        upHasMore = true
        upHistoryOffset = null
        upUpdateBaseline = null
    }

    fun refreshByType(type: DynamicTabType) {
        when (type) {
            DynamicTabType.All -> clearAllData()
            DynamicTabType.Video -> clearVideoData()
            DynamicTabType.Pgc -> clearPgcData()
            DynamicTabType.Article -> clearArticleData()
            DynamicTabType.Up -> clearUpData()
        }
    }

    suspend fun publishTextDynamic(
        text: String,
        voteDraft: DynamicVoteDraft? = null,
        refreshType: DynamicTabType = DynamicTabType.All
    ): Result<String> = publishDynamic(
        draft = DynamicPublishDraft(
            text = text,
            voteDraft = voteDraft
        ),
        refreshType = refreshType
    )

    suspend fun publishDynamic(
        draft: DynamicPublishDraft,
        refreshType: DynamicTabType = DynamicTabType.All
    ): Result<String> {
        if (creatingDynamic) return Result.failure(IllegalStateException("正在发布"))
        if (!bvUserRepository.isLogin) return Result.failure(IllegalStateException("账号未登录"))
        val trimmedText = draft.text.trim()
        if (
            trimmedText.isBlank() &&
            draft.richContents.isEmpty() &&
            draft.pictures.isEmpty() &&
            draft.reserve == null &&
            draft.voteDraft == null
        ) {
            return Result.failure(IllegalArgumentException("请输入动态内容"))
        }
        withContext(Dispatchers.Main) {
            creatingDynamic = true
        }
        return runCatching {
            val dynamicId = userRepository.createDynamic(
                draft = draft.copy(text = trimmedText)
            )
            withContext(Dispatchers.Main) {
                refreshByType(refreshType)
            }
            loadMoreByType(refreshType)
            dynamicId
        }.also {
            withContext(Dispatchers.Main) {
                creatingDynamic = false
            }
        }
    }

    suspend fun uploadDynamicImage(fileName: String, bytes: ByteArray): Result<DynamicImageDraft> =
        runCatching {
            userRepository.uploadDynamicImage(fileName = fileName, bytes = bytes)
        }

    suspend fun loadDynamicTopics(keyword: String, content: String = ""): Result<List<DynamicTopicDraft>> =
        runCatching {
            if (keyword.isBlank()) {
                userRepository.getDynamicTopicRcmd()
            } else {
                userRepository.searchDynamicTopic(keyword = keyword, content = content)
            }
        }

    suspend fun searchDynamicMention(keyword: String): Result<List<DynamicMentionDraft>> =
        runCatching {
            userRepository.searchDynamicMention(keyword = keyword.takeIf { it.isNotBlank() })
        }

    suspend fun loadDynamicEmotePackages(): Result<List<DynamicEmotePackageDraft>> =
        runCatching {
            userRepository.getDynamicEmotePackages()
        }

    suspend fun createLiveReserve(
        title: String,
        livePlanStartTime: Long,
        subType: Int = 0
    ): Result<DynamicReserveDraft> = runCatching {
        userRepository.createLiveReserve(
            title = title,
            livePlanStartTime = livePlanStartTime,
            subType = subType
        )
    }

    suspend fun updateLiveReserve(reserve: DynamicReserveDraft): Result<DynamicReserveDraft> =
        runCatching {
            userRepository.updateLiveReserve(reserve)
        }

    private suspend fun loadPgcData() {
        if (!pgcHasMore || !bvUserRepository.isLogin) return
        loadingPgc = true
        logger.fInfo { "Load more dynamic pgc [apiType=${Prefs.apiType}, offset=$pgcHistoryOffset, page=${currentPgcPage + 1}]" }
        runCatching {
            val dynamicData = userRepository.getDynamicsByType(
                type = "pgc",
                page = ++currentPgcPage,
                offset = pgcHistoryOffset ?: "",
                updateBaseline = pgcUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            dynamicPgcList.addAll(dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids })
            pgcHistoryOffset = dynamicData.historyOffset
            pgcUpdateBaseline = dynamicData.updateBaseline
            pgcHasMore = dynamicData.hasMore

            logger.fInfo { "Load dynamic pgc list page: ${currentPgcPage},size: ${dynamicData.dynamics.size}" }
        }.onFailure {
            // 错误码 4101132 表示没有数据，视为正常情况
            if (it.message?.contains("4101132") == true || it.message == "请求数据发生错误，请刷新或稍后重试") {
                pgcHasMore = false
                logger.fInfo { "No more pgc data available" }
            } else {
                logger.fWarn { "Load dynamic pgc list failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> {
                        withContext(Dispatchers.Main) {
                            BVApp.context.getString(R.string.exception_auth_failure)
                                .toast(BVApp.context)
                        }
                        logger.fInfo { "User auth failure" }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                        }
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            loadingPgc = false
        }
    }

    private suspend fun loadArticleData() {
        if (!articleHasMore || !bvUserRepository.isLogin) return
        loadingArticle = true
        logger.fInfo { "Load more dynamic article [apiType=${Prefs.apiType}, offset=$articleHistoryOffset, page=${currentArticlePage + 1}]" }
        runCatching {
            val dynamicData = userRepository.getDynamicsByType(
                type = "article",
                page = ++currentArticlePage,
                offset = articleHistoryOffset ?: "",
                updateBaseline = articleUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            dynamicArticleList.addAll(dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids })
            articleHistoryOffset = dynamicData.historyOffset
            articleUpdateBaseline = dynamicData.updateBaseline
            articleHasMore = dynamicData.hasMore

            logger.fInfo { "Load dynamic article list page: ${currentArticlePage},size: ${dynamicData.dynamics.size}" }
        }.onFailure {
            // 错误码 4101132 表示没有数据，视为正常情况
            if (it.message?.contains("4101132") == true || it.message == "请求数据发生错误，请刷新或稍后重试") {
                articleHasMore = false
                logger.fInfo { "No more article data available" }
            } else {
                logger.fWarn { "Load dynamic article list failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> {
                        withContext(Dispatchers.Main) {
                            BVApp.context.getString(R.string.exception_auth_failure)
                                .toast(BVApp.context)
                        }
                        logger.fInfo { "User auth failure" }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                        }
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            loadingArticle = false
        }
    }

    suspend fun refreshFollowUpPanel() {
        followUpList.clear()
        liveUpList.clear()
        liveUpCount = 0
        followUpOffset = null
        followUpHasMore = true
        loadFollowUpPanel()
    }

    suspend fun loadFollowUpPanel() {
        if (!bvUserRepository.isLogin || loadingFollowUp || !followUpHasMore && followUpList.isNotEmpty()) return
        loadingFollowUp = true
        runCatching {
            val data = if (followUpOffset == null) {
                userRepository.getDynamicFollowUp(preferApiType = Prefs.apiType)
            } else {
                userRepository.getDynamicUpList(
                    offset = followUpOffset.orEmpty(),
                    preferApiType = Prefs.apiType
                )
            }
            withContext(Dispatchers.Main) {
                if (followUpOffset == null) {
                    liveUpList.clear()
                    liveUpList.addAll(data.liveUsers)
                    liveUpCount = data.liveCount
                }
                val existingMids = followUpList.map { it.mid }.toHashSet()
                followUpList.addAll(data.upList.filter { it.mid !in existingMids })
                followUpOffset = data.offset
                followUpHasMore = data.hasMore && !data.offset.isNullOrBlank()
            }
        }.onFailure {
            logger.fWarn { "Load dynamic follow up failed: ${it.stackTraceToString()}" }
        }
        withContext(Dispatchers.Main) {
            loadingFollowUp = false
        }
    }

    private suspend fun loadUpData() {
        val up = selectedUp ?: return
        if (!upHasMore || !bvUserRepository.isLogin) return
        val loadGeneration = upLoadGeneration
        loadingUp = true
        Log.i(
            "DynamicViewModel",
            "load up dynamics start: mid=${up.mid}, page=${currentUpPage + 1}, generation=$loadGeneration"
        )
        logger.fInfo { "Load more dynamic by up [mid=${up.mid}, offset=$upHistoryOffset, page=${currentUpPage + 1}]" }
        runCatching {
            val dynamicData = userRepository.getDynamicsByUp(
                mid = up.mid,
                page = ++currentUpPage,
                offset = upHistoryOffset ?: "",
                updateBaseline = upUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            if (selectedUp?.mid == up.mid && upLoadGeneration == loadGeneration) {
                dynamicUpList.addAll(dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids })
                upHistoryOffset = dynamicData.historyOffset
                upUpdateBaseline = dynamicData.updateBaseline
                upHasMore = dynamicData.hasMore
                Log.i(
                    "DynamicViewModel",
                    "load up dynamics success: mid=${up.mid}, size=${dynamicData.dynamics.size}, hasMore=${dynamicData.hasMore}"
                )
            }
        }.onFailure {
            if (it.message?.contains("4101132") == true || it.message == "请求数据发生错误，请刷新或稍后重试") {
                if (selectedUp?.mid == up.mid && upLoadGeneration == loadGeneration) {
                    upHasMore = false
                    Log.i("DynamicViewModel", "load up dynamics empty: mid=${up.mid}")
                    logger.fInfo { "No more up dynamic data available" }
                }
            } else {
                Log.w("DynamicViewModel", "load up dynamics failed: mid=${up.mid}", it)
                logger.fWarn { "Load dynamic by up failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> {
                        withContext(Dispatchers.Main) {
                            BVApp.context.getString(R.string.exception_auth_failure)
                                .toast(BVApp.context)
                        }
                        logger.fInfo { "User auth failure" }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                        }
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            if (selectedUp?.mid == up.mid && upLoadGeneration == loadGeneration) {
                loadingUp = false
            }
        }
    }

    fun isLoading(type: DynamicTabType): Boolean {
        return when (type) {
            DynamicTabType.All -> loadingAll
            DynamicTabType.Video -> loadingVideo
            DynamicTabType.Pgc -> loadingPgc
            DynamicTabType.Article -> loadingArticle
            DynamicTabType.Up -> loadingUp
        }
    }

    fun itemCount(type: DynamicTabType): Int {
        return when (type) {
            DynamicTabType.All -> dynamicAllList.size
            DynamicTabType.Video -> dynamicVideoList.size
            DynamicTabType.Pgc -> dynamicPgcList.size
            DynamicTabType.Article -> dynamicArticleList.size
            DynamicTabType.Up -> dynamicUpList.size
        }
    }
}
