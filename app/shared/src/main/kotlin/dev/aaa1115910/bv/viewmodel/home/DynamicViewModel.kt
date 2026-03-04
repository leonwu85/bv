package dev.aaa1115910.bv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.user.DynamicItem
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
import org.koin.android.annotation.KoinViewModel
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
    var loadingVideo = false
    var videoHasMore = true
    private var videoHistoryOffset: String? = null
    private var videoUpdateBaseline: String? = null

    // 全部动态
    val dynamicAllList = mutableStateListOf<DynamicItem>()
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

    val isLogin get() = bvUserRepository.isLogin

    init {
        println("=====init DynamicViewModel")
    }

    suspend fun loadMoreVideo() {
        if (!loadingVideo) loadVideoData()
    }

    suspend fun loadMoreAll() {
        if (!loadingAll) loadAllData()
    }

    suspend fun loadMorePgc() {
        if (!loadingPgc) loadPgcData()
    }

    suspend fun loadMoreArticle() {
        if (!loadingArticle) loadArticleData()
    }

    suspend fun loadMoreByType(type: DynamicTabType) {
        when (type) {
            DynamicTabType.All -> loadMoreAll()
            DynamicTabType.Video -> loadMoreVideo()
            DynamicTabType.Pgc -> loadMorePgc()
            DynamicTabType.Article -> loadMoreArticle()
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
            dynamicVideoList.addAllWithMainContext(dynamicVideoData.videos)
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
            dynamicAllList.addAll(dynamicData.dynamics)
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
            dynamicPgcList.addAll(dynamicData.dynamics)
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
            dynamicArticleList.addAll(dynamicData.dynamics)
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
}