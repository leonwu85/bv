package dev.aaa1115910.bv.viewmodel.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import dev.aaa1115910.biliapi.repositories.LikeRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.DynamicTabType
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository

@KoinViewModel
class DynamicViewModel(
    private val bvUserRepository: BvUserRepository,
    private val userRepository: UserRepository,
    private val likeRepository: LikeRepository
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
    private val likingDynamicIds = mutableSetOf<String>()

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
        viewModelScope.launch(Dispatchers.Main.immediate) {
            if (!tempBlockedMids.contains(mid)) tempBlockedMids.add(mid)
            dynamicAllList.removeAll { it.author.mid == mid }
            dynamicVideoList.removeAll { it.authorId == mid }
            dynamicPgcList.removeAll { it.author.mid == mid }
            dynamicArticleList.removeAll { it.author.mid == mid }
            dynamicUpList.removeAll { it.author.mid == mid }
        }
    }

    fun toggleDynamicLike(dynamicItem: DynamicItem) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            if (!bvUserRepository.isLogin) {
                "请先登录后点赞".toast(BVApp.context)
                return@launch
            }
            val dynamicId = dynamicItem.id?.takeIf(String::isNotBlank) ?: return@launch
            val footer = dynamicItem.footer ?: return@launch
            if (!likingDynamicIds.add(dynamicId)) return@launch

            val targetLiked = !footer.isLiked
            val snapshots = applyOptimisticDynamicLike(
                dynamicId = dynamicId,
                targetLiked = targetLiked
            )

            try {
                withContext(Dispatchers.IO) {
                    if (targetLiked) {
                        likeRepository.addDynamicLike(
                            dynamicId = dynamicId,
                            preferApiType = Prefs.apiType
                        )
                    } else {
                        likeRepository.delDynamicLike(
                            dynamicId = dynamicId,
                            preferApiType = Prefs.apiType
                        )
                    }
                }
            } catch (exception: CancellationException) {
                withContext(NonCancellable + Dispatchers.Main.immediate) {
                    rollbackDynamicLike(snapshots)
                }
                throw exception
            } catch (throwable: Throwable) {
                logger.fWarn {
                    "Toggle dynamic like failed [dynamicId=$dynamicId, targetLiked=$targetLiked]: " +
                        throwable.stackTraceToString()
                }
                withContext(Dispatchers.Main.immediate) {
                    rollbackDynamicLike(snapshots)
                    "操作失败: ${throwable.localizedMessage}".toast(BVApp.context)
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main.immediate) {
                    likingDynamicIds.remove(dynamicId)
                }
            }
        }
    }

    private fun applyOptimisticDynamicLike(
        dynamicId: String,
        targetLiked: Boolean
    ): List<DynamicLikeSnapshot> {
        val snapshots = mutableListOf<DynamicLikeSnapshot>()
        val likeDelta = if (targetLiked) 1 else -1
        dynamicItemLists.forEach { list ->
            for (index in list.indices) {
                val item = list[index]
                val previousFooter = item.footer
                if (item.id != dynamicId || previousFooter == null) continue
                val optimisticFooter = previousFooter.copy(
                    like = maxOf(0, previousFooter.like + likeDelta),
                    isLiked = targetLiked
                )
                val optimisticItem = item.copy(footer = optimisticFooter)
                snapshots += DynamicLikeSnapshot(
                    list = list,
                    previousFooter = previousFooter,
                    optimisticItem = optimisticItem
                )
                list[index] = optimisticItem
            }
        }
        return snapshots
    }

    private fun rollbackDynamicLike(snapshots: List<DynamicLikeSnapshot>) {
        snapshots.forEach { snapshot ->
            val index = snapshot.list.indexOfFirst { it === snapshot.optimisticItem }
            if (index != -1) {
                val current = snapshot.list[index]
                snapshot.list[index] = current.copy(footer = snapshot.previousFooter)
            }
        }
    }

    private val dynamicItemLists: List<MutableList<DynamicItem>>
        get() = listOf(
            dynamicAllList,
            dynamicPgcList,
            dynamicArticleList,
            dynamicUpList
        )

    private data class DynamicLikeSnapshot(
        val list: MutableList<DynamicItem>,
        val previousFooter: DynamicItem.DynamicFooterModule,
        val optimisticItem: DynamicItem
    )

    private data class DynamicLoadRequest(
        val generation: Int,
        val page: Int,
        val historyOffset: String,
        val updateBaseline: String
    )

    private data class DynamicUpLoadRequest(
        val generation: Int,
        val page: Int,
        val historyOffset: String,
        val updateBaseline: String,
        val up: DynamicUpUser
    )

    private data class FollowUpLoadRequest(
        val offset: String?
    )

    suspend fun loadMorePgc() {
        pgcLoadMutex.withLock { loadPgcDataLocked() }
    }

    suspend fun loadMoreArticle() {
        articleLoadMutex.withLock { loadArticleDataLocked() }
    }

    suspend fun loadMoreUp() {
        upLoadMutex.withLock { loadUpDataLocked() }
    }

    suspend fun selectUp(up: DynamicUpUser) {
        withContext(Dispatchers.Main.immediate) {
            Log.i("DynamicViewModel", "select up: mid=${up.mid}, name=${up.uname}")
            selectedUp = up.copy(hasUpdate = false)
            clearFollowUpUpdate(up.mid)
            clearUpDataOnMain()
        }
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
                if (withContext(Dispatchers.Main.immediate) { dynamicAllList.isNotEmpty() }) {
                    return@withLock
                }
                loadAllDataLocked()
            }
            DynamicTabType.Video -> videoLoadMutex.withLock {
                if (withContext(Dispatchers.Main.immediate) { dynamicVideoList.isNotEmpty() }) {
                    return@withLock
                }
                loadVideoDataLocked()
            }
            DynamicTabType.Pgc -> pgcLoadMutex.withLock {
                if (withContext(Dispatchers.Main.immediate) { dynamicPgcList.isNotEmpty() }) {
                    return@withLock
                }
                loadPgcDataLocked()
            }
            DynamicTabType.Article -> articleLoadMutex.withLock {
                if (withContext(Dispatchers.Main.immediate) { dynamicArticleList.isNotEmpty() }) {
                    return@withLock
                }
                loadArticleDataLocked()
            }
            DynamicTabType.Up -> upLoadMutex.withLock {
                if (withContext(Dispatchers.Main.immediate) { dynamicUpList.isNotEmpty() }) {
                    return@withLock
                }
                loadUpDataLocked()
            }
        }
    }

    private suspend fun loadVideoDataLocked() {
        val request = withContext(Dispatchers.Main.immediate) {
            if (loadingVideo || !videoHasMore || !bvUserRepository.isLogin) {
                null
            } else {
                loadingVideo = true
                DynamicLoadRequest(
                    generation = videoLoadGeneration,
                    page = currentVideoPage + 1,
                    historyOffset = videoHistoryOffset.orEmpty(),
                    updateBaseline = videoUpdateBaseline.orEmpty()
                )
            }
        } ?: return
        logger.fInfo {
            "Load more dynamic videos [apiType=${Prefs.apiType}, " +
                "offset=${request.historyOffset}, page=${request.page}]"
        }
        try {
            val dynamicVideoData = userRepository.getDynamicVideos(
                page = request.page,
                offset = request.historyOffset,
                updateBaseline = request.updateBaseline,
                preferApiType = Prefs.apiType
            )
            val applied = withContext(Dispatchers.Main.immediate) {
                if (request.generation != videoLoadGeneration) {
                    false
                } else {
                    currentVideoPage = request.page
                    dynamicVideoList.addAll(
                        dynamicVideoData.videos.filter { it.authorId !in tempBlockedMids }
                    )
                    videoHistoryOffset = dynamicVideoData.historyOffset
                    videoUpdateBaseline = dynamicVideoData.updateBaseline
                    videoHasMore = dynamicVideoData.hasMore
                    true
                }
            }
            if (applied) {
                logger.fInfo {
                    "Load dynamic video list page: ${request.page}," +
                        "size: ${dynamicVideoData.videos.size}"
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (it: Throwable) {
            logger.fWarn { "Load dynamic video list failed: ${it.stackTraceToString()}" }
            when (it) {
                is AuthFailureException -> logger.fInfo { "User auth failure" }
                else -> withContext(Dispatchers.Main.immediate) {
                    "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                if (request.generation == videoLoadGeneration) loadingVideo = false
            }
        }
    }

    private suspend fun loadAllDataLocked() {
        val request = withContext(Dispatchers.Main.immediate) {
            if (loadingAll || !allHasMore || !bvUserRepository.isLogin) {
                null
            } else {
                loadingAll = true
                DynamicLoadRequest(
                    generation = allLoadGeneration,
                    page = currentAllPage + 1,
                    historyOffset = allHistoryOffset.orEmpty(),
                    updateBaseline = allUpdateBaseline.orEmpty()
                )
            }
        } ?: return
        logger.fInfo {
            "Load more dynamic all [apiType=${Prefs.apiType}, " +
                "offset=${request.historyOffset}, page=${request.page}]"
        }
        try {
            val dynamicData = userRepository.getDynamics(
                page = request.page,
                offset = request.historyOffset,
                updateBaseline = request.updateBaseline,
                preferApiType = Prefs.apiType
            )
            val applied = withContext(Dispatchers.Main.immediate) {
                if (request.generation != allLoadGeneration) {
                    false
                } else {
                    currentAllPage = request.page
                    dynamicAllList.addAll(
                        dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids }
                    )
                    allHistoryOffset = dynamicData.historyOffset
                    allUpdateBaseline = dynamicData.updateBaseline
                    allHasMore = dynamicData.hasMore
                    true
                }
            }
            if (applied) {
                logger.fInfo {
                    "Load dynamic all list page: ${request.page}," +
                        "size: ${dynamicData.dynamics.size}"
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (it: Throwable) {
            logger.fWarn { "Load dynamic all list failed: ${it.stackTraceToString()}" }
            when (it) {
                is AuthFailureException -> logger.fInfo { "User auth failure" }
                else -> withContext(Dispatchers.Main.immediate) {
                    "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                if (request.generation == allLoadGeneration) loadingAll = false
            }
        }
    }

    private fun clearVideoDataOnMain() {
        videoLoadGeneration++
        dynamicVideoList.clear()
        currentVideoPage = 0
        loadingVideo = false
        videoHasMore = true
        videoHistoryOffset = null
        videoUpdateBaseline = null
    }

    private fun clearAllDataOnMain() {
        allLoadGeneration++
        dynamicAllList.clear()
        currentAllPage = 0
        loadingAll = false
        allHasMore = true
        allHistoryOffset = null
        allUpdateBaseline = null
    }

    private fun clearPgcDataOnMain() {
        pgcLoadGeneration++
        dynamicPgcList.clear()
        currentPgcPage = 0
        loadingPgc = false
        pgcHasMore = true
        pgcHistoryOffset = null
        pgcUpdateBaseline = null
    }

    private fun clearArticleDataOnMain() {
        articleLoadGeneration++
        dynamicArticleList.clear()
        currentArticlePage = 0
        loadingArticle = false
        articleHasMore = true
        articleHistoryOffset = null
        articleUpdateBaseline = null
    }

    private fun clearUpDataOnMain() {
        upLoadGeneration += 1
        dynamicUpList.clear()
        currentUpPage = 0
        loadingUp = false
        upHasMore = true
        upHistoryOffset = null
        upUpdateBaseline = null
    }

    suspend fun refreshByType(type: DynamicTabType) {
        withContext(Dispatchers.Main.immediate) {
            when (type) {
                DynamicTabType.All -> clearAllDataOnMain()
                DynamicTabType.Video -> clearVideoDataOnMain()
                DynamicTabType.Pgc -> clearPgcDataOnMain()
                DynamicTabType.Article -> clearArticleDataOnMain()
                DynamicTabType.Up -> clearUpDataOnMain()
            }
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
        withContext(Dispatchers.Main.immediate) {
            creatingDynamic = true
        }
        return runCatching {
            val dynamicId = userRepository.createDynamic(
                draft = draft.copy(text = trimmedText)
            )
            refreshByType(refreshType)
            loadMoreByType(refreshType)
            dynamicId
        }.also {
            withContext(Dispatchers.Main.immediate) {
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
        val request = withContext(Dispatchers.Main.immediate) {
            if (loadingPgc || !pgcHasMore || !bvUserRepository.isLogin) {
                null
            } else {
                loadingPgc = true
                DynamicLoadRequest(
                    generation = pgcLoadGeneration,
                    page = currentPgcPage + 1,
                    historyOffset = pgcHistoryOffset.orEmpty(),
                    updateBaseline = pgcUpdateBaseline.orEmpty()
                )
            }
        } ?: return
        logger.fInfo {
            "Load more dynamic pgc [apiType=${Prefs.apiType}, " +
                "offset=${request.historyOffset}, page=${request.page}]"
        }
        try {
            val dynamicData = userRepository.getDynamicsByType(
                type = "pgc",
                page = request.page,
                offset = request.historyOffset,
                updateBaseline = request.updateBaseline,
                preferApiType = Prefs.apiType
            )
            val applied = withContext(Dispatchers.Main.immediate) {
                if (request.generation != pgcLoadGeneration) {
                    false
                } else {
                    currentPgcPage = request.page
                    dynamicPgcList.addAll(
                        dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids }
                    )
                    pgcHistoryOffset = dynamicData.historyOffset
                    pgcUpdateBaseline = dynamicData.updateBaseline
                    pgcHasMore = dynamicData.hasMore
                    true
                }
            }
            if (applied) {
                logger.fInfo {
                    "Load dynamic pgc list page: ${request.page}," +
                        "size: ${dynamicData.dynamics.size}"
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (it: Throwable) {
            if (it.message?.contains("4101132") == true || it.message == "请求数据发生错误，请刷新或稍后重试") {
                withContext(Dispatchers.Main.immediate) {
                    if (request.generation == pgcLoadGeneration) pgcHasMore = false
                }
                logger.fInfo { "No more pgc data available" }
            } else {
                logger.fWarn { "Load dynamic pgc list failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> logger.fInfo { "User auth failure" }
                    else -> withContext(Dispatchers.Main.immediate) {
                        "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                    }
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                if (request.generation == pgcLoadGeneration) loadingPgc = false
            }
        }
    }

    private suspend fun loadArticleDataLocked() {
        val request = withContext(Dispatchers.Main.immediate) {
            if (loadingArticle || !articleHasMore || !bvUserRepository.isLogin) {
                null
            } else {
                loadingArticle = true
                DynamicLoadRequest(
                    generation = articleLoadGeneration,
                    page = currentArticlePage + 1,
                    historyOffset = articleHistoryOffset.orEmpty(),
                    updateBaseline = articleUpdateBaseline.orEmpty()
                )
            }
        } ?: return
        logger.fInfo {
            "Load more dynamic article [apiType=${Prefs.apiType}, " +
                "offset=${request.historyOffset}, page=${request.page}]"
        }
        try {
            val dynamicData = userRepository.getDynamicsByType(
                type = "article",
                page = request.page,
                offset = request.historyOffset,
                updateBaseline = request.updateBaseline,
                preferApiType = Prefs.apiType
            )
            val applied = withContext(Dispatchers.Main.immediate) {
                if (request.generation != articleLoadGeneration) {
                    false
                } else {
                    dynamicArticleList.addAll(
                        dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids }
                    )
                    currentArticlePage = request.page
                    articleHistoryOffset = dynamicData.historyOffset
                    articleUpdateBaseline = dynamicData.updateBaseline
                    articleHasMore = dynamicData.hasMore && dynamicData.dynamics.isNotEmpty()
                    true
                }
            }
            if (applied) {
                logger.fInfo {
                    "Load dynamic article list page: ${request.page}," +
                        "size: ${dynamicData.dynamics.size}"
                }
                if (dynamicData.dynamics.isEmpty()) {
                    logger.fInfo { "No article data returned, stop auto loading" }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (it: Throwable) {
            withContext(Dispatchers.Main.immediate) {
                if (request.generation == articleLoadGeneration) articleHasMore = false
            }
            if (it.message?.contains("4101132") == true || it.message == "请求数据发生错误，请刷新或稍后重试") {
                logger.fInfo { "No more article data available" }
            } else {
                logger.fWarn { "Load dynamic article list failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> logger.fInfo { "User auth failure" }
                    else -> withContext(Dispatchers.Main.immediate) {
                        "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                    }
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                if (request.generation == articleLoadGeneration) loadingArticle = false
            }
        }
    }

    suspend fun refreshFollowUpPanel() {
        withContext(Dispatchers.Main.immediate) {
            followUpList.clear()
            liveUpList.clear()
            liveUpCount = 0
            followUpOffset = null
            followUpHasMore = true
        }
        loadFollowUpPanel()
    }

    suspend fun loadFollowUpPanel() {
        val request = withContext(Dispatchers.Main.immediate) {
            if (
                !bvUserRepository.isLogin ||
                loadingFollowUp ||
                !followUpHasMore && followUpList.isNotEmpty()
            ) {
                null
            } else {
                loadingFollowUp = true
                FollowUpLoadRequest(offset = followUpOffset)
            }
        } ?: return
        runCatching {
            val data = if (request.offset == null) {
                userRepository.getDynamicFollowUp(preferApiType = Prefs.apiType)
            } else {
                userRepository.getDynamicUpList(
                    offset = request.offset,
                    preferApiType = Prefs.apiType
                )
            }
            withContext(Dispatchers.Main.immediate) {
                if (request.offset == null) {
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
        withContext(NonCancellable + Dispatchers.Main.immediate) {
            loadingFollowUp = false
        }
    }

    private suspend fun loadUpDataLocked() {
        val request = withContext(Dispatchers.Main.immediate) {
            val up = selectedUp
            if (up == null || loadingUp || !upHasMore || !bvUserRepository.isLogin) {
                null
            } else {
                loadingUp = true
                DynamicUpLoadRequest(
                    generation = upLoadGeneration,
                    page = currentUpPage + 1,
                    historyOffset = upHistoryOffset.orEmpty(),
                    updateBaseline = upUpdateBaseline.orEmpty(),
                    up = up
                )
            }
        } ?: return
        Log.i(
            "DynamicViewModel",
            "load up dynamics start: mid=${request.up.mid}, page=${request.page}, " +
                "generation=${request.generation}"
        )
        logger.fInfo {
            "Load more dynamic by up [mid=${request.up.mid}, " +
                "offset=${request.historyOffset}, page=${request.page}]"
        }
        try {
            val dynamicData = userRepository.getDynamicsByUp(
                mid = request.up.mid,
                page = request.page,
                offset = request.historyOffset,
                updateBaseline = request.updateBaseline,
                preferApiType = Prefs.apiType
            )
            val applied = withContext(Dispatchers.Main.immediate) {
                if (
                    selectedUp?.mid != request.up.mid ||
                    upLoadGeneration != request.generation
                ) {
                    false
                } else {
                    currentUpPage = request.page
                    dynamicUpList.addAll(
                        dynamicData.dynamics.filter { it.author.mid !in tempBlockedMids }
                    )
                    upHistoryOffset = dynamicData.historyOffset
                    upUpdateBaseline = dynamicData.updateBaseline
                    upHasMore = dynamicData.hasMore
                    true
                }
            }
            if (applied) {
                Log.i(
                    "DynamicViewModel",
                    "load up dynamics success: mid=${request.up.mid}, " +
                        "size=${dynamicData.dynamics.size}, hasMore=${dynamicData.hasMore}"
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (it: Throwable) {
            if (it.message?.contains("4101132") == true || it.message == "请求数据发生错误，请刷新或稍后重试") {
                val applied = withContext(Dispatchers.Main.immediate) {
                    if (
                        selectedUp?.mid == request.up.mid &&
                        upLoadGeneration == request.generation
                    ) {
                        upHasMore = false
                        true
                    } else {
                        false
                    }
                }
                if (applied) {
                    Log.i("DynamicViewModel", "load up dynamics empty: mid=${request.up.mid}")
                    logger.fInfo { "No more up dynamic data available" }
                }
            } else {
                Log.w("DynamicViewModel", "load up dynamics failed: mid=${request.up.mid}", it)
                logger.fWarn { "Load dynamic by up failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> logger.fInfo { "User auth failure" }
                    else -> withContext(Dispatchers.Main.immediate) {
                        "加载动态失败: ${it.localizedMessage}".toast(BVApp.context)
                    }
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                if (
                    selectedUp?.mid == request.up.mid &&
                    upLoadGeneration == request.generation
                ) {
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
