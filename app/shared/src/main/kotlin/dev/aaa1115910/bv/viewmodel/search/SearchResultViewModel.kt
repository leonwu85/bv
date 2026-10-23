package dev.aaa1115910.bv.viewmodel.search

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.search.searchKey
import dev.aaa1115910.biliapi.repositories.*
import dev.aaa1115910.bv.repository.LocalUserBlockRepository
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.Partition
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SearchResultViewModel(
    private val searchRepository: SearchRepository,
    private val localUserBlocks: LocalUserBlockRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    var keyword by mutableStateOf("")
        private set
    var searchType by mutableStateOf(SearchType.All)
    private val results = mutableStateMapOf<SearchType, SearchResult>()
    private val updating = mutableStateMapOf<SearchType, Boolean>()
    private val inited = mutableStateMapOf<SearchType, Boolean>()
    private val errors = mutableStateMapOf<SearchType, String?>()
    private val pages = mutableMapOf<SearchType, SearchTypePage>()
    private val hasMore = mutableMapOf<SearchType, Boolean>()
    private val versions = mutableMapOf<SearchType, Int>()
    private val jobs = mutableMapOf<SearchType, Job>()
    data class Verification(
        val type: SearchType, val query: String, val version: Int, val session: String?,
        val token: String, val gt: String, val challenge: String
    )
    var verification by mutableStateOf<Verification?>(null)
        private set
    private var validating: Verification? = null
    private var refreshingVerification: Verification? = null
    private var validationJob: Job? = null
    private val usedVouchers = mutableSetOf<String>()
    private var registeringType: SearchType? = null

    private fun Verification.isCurrent() = keyword == query && (versions[type] ?: 0) == version && authRepository.sessionData == session

    fun cancelVerification() {
        val pending = verification ?: validating
        if (pending?.isCurrent() == true) {
            versions[pending.type] = pending.version + 1
            jobs.remove(pending.type)?.cancel()
            updating[pending.type] = false
            errors[pending.type] = "已取消验证，点击重试可重新验证"
        }
        verification = null
        validating = null
        refreshingVerification = null
        validationJob?.cancel()
    }

    private suspend fun registerVerification(voucher: String, type: SearchType, query: String, version: Int) {
        if (verification != null && verification !== refreshingVerification || registeringType != null) {
            errors[type] = "请先完成当前人机验证，再重试搜索"
            return
        }
        val session = authRepository.sessionData
        registeringType = type
        try {
            check(usedVouchers.add(voucher)) { "验证凭据已使用，请重新搜索获取新验证" }
            val response = withContext(Dispatchers.IO) {
                dev.aaa1115910.biliapi.http.BiliHttpApi.gaiaVgateRegister(voucher, session, authRepository.biliJct).getResponseData()
            }
            check(response.token.isNotBlank() && response.geetest.gt.isNotBlank() && response.geetest.challenge.isNotBlank()) { "验证申请失败" }
            val pending = Verification(type, query, version, session, response.token, response.geetest.gt, response.geetest.challenge)
            if (pending.isCurrent()) {
                verification = pending
                errors[type] = "请完成人机验证"
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { if (keyword == query && (versions[type] ?: 0) == version) errors[type] = e.localizedMessage ?: "验证申请失败，请重试" }
        finally { if (registeringType == type) registeringType = null }
    }

    fun verify(challenge: String, validate: String, seccode: String, sourceChallenge: String) {
        val pending = verification ?: return
        if (!pending.isCurrent() || validating != null || refreshingVerification != null || sourceChallenge != pending.challenge ||
            challenge.isBlank() || validate.isBlank() || seccode.isBlank()) return
        validating = pending
        verification = null
        validationJob = viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    dev.aaa1115910.biliapi.http.BiliHttpApi.gaiaVgateValidate(pending.token, challenge, validate, seccode,
                        pending.session, authRepository.biliJct).getResponseData()
                }
                if (validating !== pending || !pending.isCurrent()) return@launch
                check(response.isValid == 1 && response.griskId.isNotBlank()) { "验证未通过，请重试" }
                authRepository.gaiaVtoken = response.griskId
                validating = null
                loadMore(pending.type, retry = true, allowVerification = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (pending.isCurrent()) errors[pending.type] = e.localizedMessage ?: "验证失败，请重试" }
            finally { if (validating === pending) validating = null }
        }
    }

    suspend fun refreshVerification(): Boolean {
        val pending = verification ?: return false
        if (!pending.isCurrent() || validating != null) return false
        if (refreshingVerification != null) return false
        refreshingVerification = pending
        return try {
            loadMore(pending.type, retry = true)
            jobs[pending.type]?.join()
            verification?.let { it.isCurrent() && it.challenge != pending.challenge } == true
        } finally { if (refreshingVerification === pending) refreshingVerification = null }
    }


    init {
        viewModelScope.launch {
            localUserBlocks.blocked.collect {
                results.keys.toList().forEach { type -> results[type] = result(type).filtered() }
            }
        }
    }

    private fun SearchResult.filtered() = copy(
        videos = videos.filterNot { localUserBlocks.isBlocked(it.upId) },
        biliUsers = biliUsers.filterNot { localUserBlocks.isBlocked(it.mid) },
        liveRooms = liveRooms.filterNot { localUserBlocks.isBlocked(it.uid) },
        articles = articles.filterNot { localUserBlocks.isBlocked(it.authorId) },
        aggregateItems = aggregateItems.filterNot { item ->
            when (item) {
                is SearchTypeResult.Video -> localUserBlocks.isBlocked(item.upId)
                is SearchTypeResult.User -> localUserBlocks.isBlocked(item.mid)
                is SearchTypeResult.LiveRoom -> localUserBlocks.isBlocked(item.uid)
                is SearchTypeResult.Article -> localUserBlocks.isBlocked(item.authorId)
                else -> false
            }
        }
    )

    val allSearchResult get() = result(SearchType.All)
    val videoSearchResult get() = result(SearchType.Video)
    val mediaBangumiSearchResult get() = result(SearchType.MediaBangumi)
    val mediaFtSearchResult get() = result(SearchType.MediaFt)
    val biliUserSearchResult get() = result(SearchType.BiliUser)
    val liveRoomSearchResult get() = result(SearchType.LiveRoom)
    val articleSearchResult get() = result(SearchType.Article)
    fun result(type: SearchType) = results[type] ?: SearchResult(type)

    var selectedOrder by mutableStateOf(SearchFilterOrderType.ComprehensiveSort)
    var selectedDuration by mutableStateOf(SearchFilterDuration.All)
    var selectedPartition: Partition? by mutableStateOf(null)
    var selectedChildPartition: Partition? by mutableStateOf(null)
    var enableProxySearchResult = false

    fun submit(value: String) {
        val next = value.trim()
        if (keyword != next) {
            SearchType.entries.forEach(::resetState)
            keyword = next
        }
        update()
    }

    fun applyVideoFilters(order: SearchFilterOrderType, duration: SearchFilterDuration, partition: Partition?, childPartition: Partition?) {
        selectedOrder = order
        selectedDuration = duration
        selectedPartition = partition
        selectedChildPartition = childPartition?.takeIf { it in (partition?.children ?: emptyList()) }
        resetState(SearchType.All)
        resetState(SearchType.Video)
        if (searchType in listOf(SearchType.All, SearchType.Video)) update()
    }

    fun isLoading(type: SearchType): Boolean = updating[type] == true
    fun isInitialized(type: SearchType): Boolean = inited[type] == true
    fun error(type: SearchType): String? = errors[type]

    fun update() {
        resetState(searchType)
        loadMore(searchType)
    }

    private fun resetState(type: SearchType) {
        if (verification?.type == type || validating?.type == type) cancelVerification()
        jobs.remove(type)?.cancel()
        versions[type] = (versions[type] ?: 0) + 1
        results[type] = SearchResult(type)
        pages[type] = SearchTypePage()
        hasMore[type] = true
        updating[type] = false
        inited[type] = false
        errors[type] = null
    }

    fun init(type: SearchType) {
        if (!isInitialized(type)) loadMore(type)
    }

    fun loadMore(type: SearchType, retry: Boolean = false, allowVerification: Boolean = true) {
        if (keyword.isBlank() || isLoading(type) || hasMore[type] == false || (!retry && errors[type] != null)) return
        val version = versions[type] ?: 0
        val query = keyword
        val page = pages[type] ?: SearchTypePage()
        val tid = selectedChildPartition?.tid ?: selectedPartition?.tid
        val order = selectedOrder
        val duration = selectedDuration
        val api = Prefs.apiType
        val proxy = enableProxySearchResult
        updating[type] = true
        inited[type] = true
        errors[type] = null
        jobs[type] = viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    searchRepository.searchType(query, type, tid, order, duration, page, api, proxy)
                }
                if ((versions[type] ?: 0) != version || keyword != query) return@launch
                if (refreshingVerification?.let { it.type == type && it.version == version && it.isCurrent() } == true) verification = null
                results[type] = result(type).append(response).filtered()
                pages[type] = response.page
                val count = when (type) {
                    SearchType.All, SearchType.Video -> response.videos.size
                    SearchType.MediaBangumi, SearchType.MediaFt -> response.pgcs.size
                    SearchType.BiliUser -> response.users.size
                    SearchType.LiveRoom -> response.liveRooms.size
                    SearchType.Article -> response.articles.size
                }
                hasMore[type] = if (response.page.nextPageForApp.isNotBlank() && response.page.nextPageForApp == page.nextPageForApp) false else response.hasMore ?: if (response.page.nextPageForApp.isNotBlank()) {
                    response.page.nextPageForApp != page.nextPageForApp
                } else {
                    count >= (response.pageSize?.takeIf { it > 0 } ?: 20)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if ((versions[type] ?: 0) == version && keyword == query) {
                    if (e is dev.aaa1115910.biliapi.http.entity.VVoucherException && allowVerification) {
                        registerVerification(e.vVoucher, type, query, version)
                    } else errors[type] = if (e is dev.aaa1115910.biliapi.http.entity.VVoucherException)
                        "验证后搜索仍受限，请稍后重试" else e.localizedMessage ?: "搜索失败，请重试"
                }
            } finally {
                if ((versions[type] ?: 0) == version) updating[type] = false
            }
        }
    }

    data class SearchResult(
        val type: SearchType,
        val videos: List<SearchTypeResult.Video> = emptyList(),
        val mediaBangumis: List<SearchTypeResult.Pgc> = emptyList(),
        val mediaFts: List<SearchTypeResult.Pgc> = emptyList(),
        val biliUsers: List<SearchTypeResult.User> = emptyList(),
        val liveRooms: List<SearchTypeResult.LiveRoom> = emptyList(),
        val articles: List<SearchTypeResult.Article> = emptyList(),
        val aggregateItems: List<SearchTypeResult.SearchTypeResultItem> = emptyList()
    ) {
        val count get() = if (type == SearchType.All) aggregateItems.size else
            videos.size + mediaBangumis.size + mediaFts.size + biliUsers.size + liveRooms.size + articles.size

        fun append(data: SearchTypeResult): SearchResult = when (type) {
            SearchType.All -> copy(aggregateItems = (aggregateItems + data.activities +
                data.users.flatMap { listOf(it) + it.recentVideos } + data.pgcs + data.videos).distinctBy { it.searchKey() })
            SearchType.Video -> copy(videos = (videos + data.videos).distinctBy { it.aid })
            SearchType.MediaBangumi -> copy(mediaBangumis = (mediaBangumis + data.pgcs).distinctBy { it.seasonId })
            SearchType.MediaFt -> copy(mediaFts = (mediaFts + data.pgcs).distinctBy { it.seasonId })
            SearchType.BiliUser -> copy(biliUsers = (biliUsers + data.users).distinctBy { it.mid })
            SearchType.LiveRoom -> copy(liveRooms = (liveRooms + data.liveRooms).distinctBy { it.roomId })
            SearchType.Article -> copy(articles = (articles + data.articles).distinctBy { it.id })
        }
    }
}

enum class SearchResultType(
    val type: String,
    private val strRes: Int
) {
    All(type = "all", strRes = R.string.search_result_type_name_all),
    Video(type = "video", strRes = R.string.search_result_type_name_video),
    MediaBangumi(type = "media_bangumi", R.string.search_result_type_name_media_bangumi),
    MediaFt(type = "media_ft", strRes = R.string.search_result_type_name_media_ft),
    BiliUser(type = "bili_user", strRes = R.string.search_result_type_name_bili_user),
    LiveRoom(type = "live_room", strRes = R.string.search_result_type_name_live_room),
    Article(type = "article", strRes = R.string.search_result_type_name_article);

    fun getDisplayName(context: Context) = context.getString(strRes)
}
