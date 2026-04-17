package dev.aaa1115910.bv.viewmodel.search

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.biliapi.repositories.SearchRepository
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.SearchTypePage
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.Partition
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SearchResultViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var keyword by mutableStateOf("")
    var searchType by mutableStateOf(SearchType.Video)

    var videoSearchResult by mutableStateOf(SearchResult(SearchType.Video))
    var mediaBangumiSearchResult by mutableStateOf(SearchResult(SearchType.MediaBangumi))
    var mediaFtSearchResult by mutableStateOf(SearchResult(SearchType.MediaFt))
    var biliUserSearchResult by mutableStateOf(SearchResult(SearchType.BiliUser))
    var liveRoomSearchResult by mutableStateOf(SearchResult(SearchType.LiveRoom))

    var selectedOrder by mutableStateOf(SearchFilterOrderType.ComprehensiveSort)
    var selectedDuration by mutableStateOf(SearchFilterDuration.All)
    var selectedPartition: Partition? by mutableStateOf(null)
    var selectedChildPartition: Partition? by mutableStateOf(null)

    private val updating = mutableStateMapOf<SearchType, Boolean>().apply {
        SearchType.entries.forEach { put(it, false) }
    }

    private val hasMore = mutableMapOf<SearchType, Boolean>().apply {
        SearchType.entries.forEach { put(it, true) }
    }

    private val pages = mutableMapOf<SearchType, SearchTypePage>().apply {
        SearchType.entries.forEach { put(it, SearchTypePage()) }
    }

    private val requestVersions = mutableMapOf<SearchType, Int>().apply {
        SearchType.entries.forEach { put(it, 0) }
    }

    private val initeds = mutableStateMapOf<SearchType, Boolean>().apply {
        SearchType.entries.forEach { put(it, false) }
    }

    /**
     * 判断指定类型是否正在加载中
     */
    fun isLoading(type: SearchType): Boolean = updating[type] == true

    /**
     * 判断指定类型是否已完成首次加载
     */
    fun isInitialized(type: SearchType): Boolean = initeds[type] == true

    var enableProxySearchResult = false

    fun update() {
        val currentType = searchType
        resetState(currentType)
        viewModelScope.launch {
            loadMore(currentType, true)
        }
    }

    private fun resetState(type: SearchType) {
        requestVersions[type] = (requestVersions[type] ?: 0) + 1
        pages[type] = SearchTypePage()
        hasMore[type] = true
        updating[type] = false
        when (type) {
            SearchType.Video -> videoSearchResult = SearchResult(SearchType.Video)
            SearchType.MediaBangumi -> mediaBangumiSearchResult = SearchResult(SearchType.MediaBangumi)
            SearchType.MediaFt -> mediaFtSearchResult = SearchResult(SearchType.MediaFt)
            SearchType.BiliUser -> biliUserSearchResult = SearchResult(SearchType.BiliUser)
            SearchType.LiveRoom -> liveRoomSearchResult = SearchResult(SearchType.LiveRoom)
        }
    }

    fun init(searchType: SearchType) {
        if (initeds[searchType] == false) {
            loadMore(searchType)
            initeds[searchType] = true
        }
    }

    fun loadMore(
        searchType: SearchType,
        ignoreUpdating: Boolean = false
    ) {
        if (hasMore[searchType] != true) return
        if (updating[searchType] == true && !ignoreUpdating) return

        val requestVersion = requestVersions[searchType] ?: 0
        updating[searchType] = true
        viewModelScope.launch(Dispatchers.IO) {
            logger.fInfo { "Load search result: [keyword=$keyword, type=$searchType, page=${pages[searchType]}]" }
            runCatching {
                val searchResultResponse = searchRepository.searchType(
                    keyword = keyword,
                    type = searchType,
                    page = pages[searchType] ?: SearchTypePage(),
                    tid = selectedChildPartition?.tid ?: selectedPartition?.tid,
                    order = selectedOrder,
                    duration = selectedDuration,
                    preferApiType = Prefs.apiType,
                    enableProxy = enableProxySearchResult
                )
                withContext(Dispatchers.Main) {
                    if (requestVersions[searchType] != requestVersion) return@withContext

                    when (searchType) {
                        SearchType.Video -> videoSearchResult =
                            videoSearchResult.appendSearchResultData(searchResultResponse)

                         SearchType.MediaBangumi -> mediaBangumiSearchResult =
                             mediaBangumiSearchResult.appendSearchResultData(searchResultResponse)

                         SearchType.MediaFt -> mediaFtSearchResult =
                             mediaFtSearchResult.appendSearchResultData(searchResultResponse)

                         SearchType.BiliUser -> biliUserSearchResult =
                             biliUserSearchResult.appendSearchResultData(searchResultResponse)

                         SearchType.LiveRoom -> liveRoomSearchResult =
                             liveRoomSearchResult.appendSearchResultData(searchResultResponse)
                    }

                    // 检查返回的数据数量，如果少于请求的分页数量则设置 hasMore 为 false
                    val returnedCount = when (searchType) {
                        SearchType.Video -> searchResultResponse.videos.size
                        SearchType.MediaBangumi -> searchResultResponse.pgcs.size
                        SearchType.MediaFt -> searchResultResponse.pgcs.size
                        SearchType.BiliUser -> searchResultResponse.users.size
                        SearchType.LiveRoom -> searchResultResponse.liveRooms.size
                    }
                    val requestedPageSize = 20
                    if (returnedCount < requestedPageSize) {
                        hasMore[searchType] = false
                    }

                    pages[searchType] = searchResultResponse.page
                }
            }
            withContext(Dispatchers.Main) {
                if (requestVersions[searchType] == requestVersion) {
                    updating[searchType] = false
                }
            }
        }
    }

    data class SearchResult(
        var type: SearchType,
        var videos: List<SearchTypeResult.Video> = emptyList(),
        var mediaBangumis: List<SearchTypeResult.Pgc> = emptyList(),
        var mediaFts: List<SearchTypeResult.Pgc> = emptyList(),
        var biliUsers: List<SearchTypeResult.User> = emptyList(),
        var liveRooms: List<SearchTypeResult.LiveRoom> = emptyList(),
        var page: SearchTypePage = SearchTypePage()
    ) {
        val count get() = videos.size + mediaBangumis.size + mediaFts.size + biliUsers.size + liveRooms.size

        fun appendSearchResultData(searchTypeResult: SearchTypeResult): SearchResult {
            return when (type) {
                SearchType.Video -> {
                    SearchResult(type).apply {
                        this.videos = this@SearchResult.videos + searchTypeResult.videos
                    }
                }

                SearchType.MediaBangumi -> {
                    SearchResult(type).apply {
                        this.mediaBangumis = this@SearchResult.mediaBangumis + searchTypeResult.pgcs
                    }
                }

                SearchType.MediaFt -> {
                    SearchResult(type).apply {
                        this.mediaFts = this@SearchResult.mediaFts + searchTypeResult.pgcs
                    }
                }

                SearchType.BiliUser -> {
                    SearchResult(type).apply {
                        this.biliUsers = this@SearchResult.biliUsers + searchTypeResult.users
                    }
                }

                SearchType.LiveRoom -> {
                    SearchResult(type).apply {
                        this.liveRooms = this@SearchResult.liveRooms + searchTypeResult.liveRooms
                    }
                }
            }
        }
    }
}

enum class SearchResultType(
    val type: String,
    private val strRes: Int
) {
    Video(type = "video", strRes = R.string.search_result_type_name_video),
    MediaBangumi(type = "media_bangumi", R.string.search_result_type_name_media_bangumi),
    MediaFt(type = "media_ft", strRes = R.string.search_result_type_name_media_ft),
    BiliUser(type = "bili_user", strRes = R.string.search_result_type_name_bili_user),
    LiveRoom(type = "live_room", strRes = R.string.search_result_type_name_live_room);

    fun getDisplayName(context: Context) = context.getString(strRes)
}
