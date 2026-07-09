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
import dev.aaa1115910.bv.entity.DynamicTabType
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    // 各子 Tab 加载互斥：预加载 + 页面 LaunchedEffect 可能并发进入
    private val videoLoadMutex = Mutex()
    private val allLoadMutex = Mutex()
    private val pgcLoadMutex = Mutex()
    private val articleLoadMutex = Mutex()
    private val upLoadMutex = Mutex()

    private var videoLoadGeneration = 0
    private var allLoadGeneration = 0
    private var pgcLoadGeneration = 0
    private var articleLoadGeneration = 0

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
        videoLoadMutex.withLock { loadVideoDataLocked() }
    }

    suspend fun loadMoreAll() {
        allLoadMutex.withLock { loadAllDataLocked() }
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
        pgcLoadMutex.withLock { loadPgcDataLocked() }
    }

    suspend fun loadMoreArticle() {
        articleLoadMutex.withLock { loadArticleDataLocked() }
    }

    suspend fun loadMoreUp() {
        upLoadMutex.withLock { loadUpDataLocked() }
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

    /**
     * 仅在列表为空时加载首页。用于预加载：HomeContent 与 NewDynamicsScreen 可同时调用，
     * 互斥内再次判断空列表，避免并发双拉导致页码乱序/跳页。
     */
    suspend fun ensureFirstPage(type: DynamicTabType) {
        when (type) {
            DynamicTabType.All -> allLoadMutex.withLock {
                if (dynamicAllList.isNotEmpty()) return@withLock
                loadAllDataLocked()
            }
            DynamicTabType.Video -> videoLoadMutex.withLock {
                if (dynamicVideoList.isNotEmpty()) return@withLock
                loadVideoDataLocked()
            }
            DynamicTabType.Pgc -> pgcLoadMutex.withLock {
                if (dynamicPgcList.isNotEmpty()) return@withLock
                loadPgcDataLocked()
            }
            DynamicTabType.Article -> articleLoadMutex.withLock {
                if (dynamicArticleList.isNotEmpty()) return@withLock
                loadArticleDataLocked()
            }
            DynamicTabType.Up -> upLoadMutex.withLock {
                if (dynamicUpList.isNotEmpty()) return@withLock
                loadUpDataLocked()
            }
        }
    }

    private suspend fun loadVideoDataLocked() {
        if (loadingVideo || !videoHasMore || !bvUserRepository.isLogin) return
        val generation = videoLoadGeneration
        loadingVideo = true
        val nextPage = currentVideoPage + 1
        logger.fInfo { "Load more dynamic videos [apiType=${Prefs.apiType}, offset=$videoHistoryOffset, page=$nextPage]" }
        try {
            val dynamicVideoData = userRepository.getDynamicVideos(
                page = nextPage,
                offset = videoHistoryOffset ?: "",
                updateBaseline = videoUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            if (generation != videoLoadGeneration) return
            currentVideoPage = nextPage
            dynamicVideoList.addAllWithMainContext(
                dynamicVideoData.videos.filter { it.authorId !in tempBlockedMids }
            )
            videoHistoryOffset = dynamicVideoData.historyOffset
            videoUpdateBaseline = dynamicVideoData.updateBaseline
            videoHasMore = dynamicVideoData.hasMore
            logger.fInfo { "Load dynamic video list page: $currentVideoPage,size: ${dynamicVideoData.videos.size}" }
        } catch (it: Throwable) {
            logger.fWarn { "Load dynamic video list failed: ${it.stackTraceToString()}" }
            when (it) {
                is AuthFailureException -> logger.fInfo { "User auth failure" }
                else -> withContext(Dispatchers.Main) {
                    "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                }
            }
        } finally {
            if (generation == videoLoadGeneration) {
                withContext(Dispatchers.Main) { loadingVideo = false }
            }
        }
    }

    private suspend fun loadAllDataLocked() {
        if (loadingAll || !allHasMore || !bvUserRepository.isLogin) return
        val generation = allLoadGeneration
        loadingAll = true
        val nextPage = currentAllPage + 1
        logger.fInfo { "Load more dynamic all [apiType=${Prefs.apiType}, offset=$allHistoryOffset, page=$nextPage]" }
        try {
            val dynamicData = userRepository.getDynamics(
                page = nextPage,
                offset = allHistoryOffset ?: "",
                updateBaseline = allUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            if (generation != allLoadGeneration) return
            currentAllPage = nextPage
            dynamicAllList.addAll(dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids })
            allHistoryOffset = dynamicData.historyOffset
            allUpdateBaseline = dynamicData.updateBaseline
            allHasMore = dynamicData.hasMore
            logger.fInfo { "Load dynamic all list page: $currentAllPage,size: ${dynamicData.dynamics.size}" }
        } catch (it: Throwable) {
            logger.fWarn { "Load dynamic all list failed: ${it.stackTraceToString()}" }
            when (it) {
                is AuthFailureException -> logger.fInfo { "User auth failure" }
                else -> withContext(Dispatchers.Main) {
                    "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                }
            }
        } finally {
            if (generation == allLoadGeneration) {
                withContext(Dispatchers.Main) { loadingAll = false }
            }
        }
    }

    fun clearVideoData() {
        videoLoadGeneration++
        dynamicVideoList.clear()
        currentVideoPage = 0
        loadingVideo = false
        videoHasMore = true
        videoHistoryOffset = null
        videoUpdateBaseline = null
    }

    fun clearAllData() {
        allLoadGeneration++
        dynamicAllList.clear()
        currentAllPage = 0
        loadingAll = false
        allHasMore = true
        allHistoryOffset = null
        allUpdateBaseline = null
    }

    fun clearPgcData() {
        pgcLoadGeneration++
        dynamicPgcList.clear()
        currentPgcPage = 0
        loadingPgc = false
        pgcHasMore = true
        pgcHistoryOffset = null
        pgcUpdateBaseline = null
    }

    fun clearArticleData() {
        articleLoadGeneration++
        dynamicArticleList.clear()
        currentArticlePage = 0
        loadingArticle = false
        articleHasMore = true
        articleHistoryOffset = null
        articleUpdateBaseline = null
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

    private suspend fun loadPgcDataLocked() {
        if (loadingPgc || !pgcHasMore || !bvUserRepository.isLogin) return
        val generation = pgcLoadGeneration
        loadingPgc = true
        val nextPage = currentPgcPage + 1
        logger.fInfo { "Load more dynamic pgc [apiType=${Prefs.apiType}, offset=$pgcHistoryOffset, page=$nextPage]" }
        try {
            val dynamicData = userRepository.getDynamicsByType(
                type = "pgc",
                page = nextPage,
                offset = pgcHistoryOffset ?: "",
                updateBaseline = pgcUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            if (generation != pgcLoadGeneration) return
            currentPgcPage = nextPage
            dynamicPgcList.addAll(dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids })
            pgcHistoryOffset = dynamicData.historyOffset
            pgcUpdateBaseline = dynamicData.updateBaseline
            pgcHasMore = dynamicData.hasMore
            logger.fInfo { "Load dynamic pgc list page: $currentPgcPage,size: ${dynamicData.dynamics.size}" }
        } catch (it: Throwable) {
            if (it.message?.contains("4101132") == true || it.message == "请求数据发生错误，请刷新或稍后重试") {
                if (generation == pgcLoadGeneration) pgcHasMore = false
                logger.fInfo { "No more pgc data available" }
            } else {
                logger.fWarn { "Load dynamic pgc list failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> logger.fInfo { "User auth failure" }
                    else -> withContext(Dispatchers.Main) {
                        "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                    }
                }
            }
        } finally {
            if (generation == pgcLoadGeneration) {
                withContext(Dispatchers.Main) { loadingPgc = false }
            }
        }
    }

    private suspend fun loadArticleDataLocked() {
        if (loadingArticle || !articleHasMore || !bvUserRepository.isLogin) return
        val generation = articleLoadGeneration
        loadingArticle = true
        val nextPage = currentArticlePage + 1
        logger.fInfo { "Load more dynamic article [apiType=${Prefs.apiType}, offset=$articleHistoryOffset, page=$nextPage]" }
        try {
            val dynamicData = userRepository.getDynamicsByType(
                type = "article",
                page = nextPage,
                offset = articleHistoryOffset ?: "",
                updateBaseline = articleUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            if (generation != articleLoadGeneration) return
            val articleItems = dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids }
            dynamicArticleList.addAll(articleItems)
            currentArticlePage = nextPage
            articleHistoryOffset = dynamicData.historyOffset
            articleUpdateBaseline = dynamicData.updateBaseline
            articleHasMore = dynamicData.hasMore && dynamicData.dynamics.isNotEmpty()
            logger.fInfo { "Load dynamic article list page: $currentArticlePage,size: ${dynamicData.dynamics.size}" }
            if (dynamicData.dynamics.isEmpty()) {
                logger.fInfo { "No article data returned, stop auto loading" }
            }
        } catch (it: Throwable) {
            if (generation == articleLoadGeneration) articleHasMore = false
            if (it.message?.contains("4101132") == true || it.message == "请求数据发生错误，请刷新或稍后重试") {
                logger.fInfo { "No more article data available" }
            } else {
                logger.fWarn { "Load dynamic article list failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> logger.fInfo { "User auth failure" }
                    else -> withContext(Dispatchers.Main) {
                        "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                    }
                }
            }
        } finally {
            if (generation == articleLoadGeneration) {
                withContext(Dispatchers.Main) { loadingArticle = false }
            }
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

    private suspend fun loadUpDataLocked() {
        val up = selectedUp ?: return
        if (loadingUp || !upHasMore || !bvUserRepository.isLogin) return
        val loadGeneration = upLoadGeneration
        loadingUp = true
        val nextPage = currentUpPage + 1
        Log.i(
            "DynamicViewModel",
            "load up dynamics start: mid=${up.mid}, page=$nextPage, generation=$loadGeneration"
        )
        logger.fInfo { "Load more dynamic by up [mid=${up.mid}, offset=$upHistoryOffset, page=$nextPage]" }
        try {
            val dynamicData = userRepository.getDynamicsByUp(
                mid = up.mid,
                page = nextPage,
                offset = upHistoryOffset ?: "",
                updateBaseline = upUpdateBaseline ?: "",
                preferApiType = Prefs.apiType
            )
            if (selectedUp?.mid == up.mid && upLoadGeneration == loadGeneration) {
                currentUpPage = nextPage
                dynamicUpList.addAll(dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids })
                upHistoryOffset = dynamicData.historyOffset
                upUpdateBaseline = dynamicData.updateBaseline
                upHasMore = dynamicData.hasMore
                Log.i(
                    "DynamicViewModel",
                    "load up dynamics success: mid=${up.mid}, size=${dynamicData.dynamics.size}, hasMore=${dynamicData.hasMore}"
                )
            }
        } catch (it: Throwable) {
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
                    is AuthFailureException -> logger.fInfo { "User auth failure" }
                    else -> withContext(Dispatchers.Main) {
                        "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                    }
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                if (selectedUp?.mid == up.mid && upLoadGeneration == loadGeneration) {
                    loadingUp = false
                }
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

    fun hasMore(type: DynamicTabType): Boolean {
        return when (type) {
            DynamicTabType.All -> allHasMore
            DynamicTabType.Video -> videoHasMore
            DynamicTabType.Pgc -> pgcHasMore
            DynamicTabType.Article -> articleHasMore
            DynamicTabType.Up -> upHasMore
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
