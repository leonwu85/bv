package dev.aaa1115910.bv.viewmodel.user

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.biliapi.entity.user.HistoryItemType
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.HistoryRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HistoryViewModel(
    private val userRepository: UserRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var histories = mutableStateListOf<VideoCardData>()
    var searchResults = mutableStateListOf<VideoCardData>()
    var noMore by mutableStateOf(false)

    var searchQuery by mutableStateOf("")
        private set
    var searchUpdating by mutableStateOf(false)
        private set
    private var searchPage = 1
    private var searchRemoteNoMore = false
    private var searchFallbackCursor = 0L
    private var searchFallbackNoMore = false
    private var searchGeneration = 0
    private var searchDebounceJob: Job? = null
    private var searchLoadJob: Job? = null

    val visibleHistories: List<VideoCardData>
        get() = if (searchQuery.isBlank()) {
            histories
        } else {
            mergeHistorySearchResults(
                query = searchQuery,
                loadedHistories = histories,
                remoteResults = searchResults
            )
        }

    val isLoading: Boolean
        get() = if (searchQuery.isBlank()) updating else searchUpdating

    private var cursor = 0L
    var updating by mutableStateOf(false)
        private set

    private val loadMutex = Mutex()
    /** clearData 时递增，丢弃进行中的旧请求结果 */
    private var loadGeneration = 0

    fun update() {
        if (searchQuery.isNotBlank()) {
            loadMoreSearchResults()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            updateHistories()
        }
    }

    fun updateSearchQuery(value: String) {
        if (searchQuery == value) return
        searchQuery = value
        searchDebounceJob?.cancel()
        searchLoadJob?.cancel()
        searchGeneration++
        searchPage = 1
        searchRemoteNoMore = false
        searchFallbackCursor = if (Prefs.apiType == ApiType.Web) cursor else 0L
        searchFallbackNoMore = Prefs.apiType == ApiType.Web && noMore
        searchUpdating = false
        searchResults.clear()
        if (value.isBlank()) return
        searchResults.addAll(histories.filter { it.matchesHistorySearch(value) })
        searchDebounceJob = viewModelScope.launch {
            delay(350)
            loadMoreSearchResults()
        }
    }

    private fun loadMoreSearchResults() {
        val query = searchQuery.trim()
        if (
            query.isEmpty() ||
            searchUpdating ||
            (searchRemoteNoMore && searchFallbackNoMore)
        ) return
        val generation = searchGeneration
        val page = searchPage
        logger.fInfo {
            "Starting history search: loaded=${histories.size}, " +
                "localMatches=${histories.count { it.matchesHistorySearch(query) }}, " +
                "fallbackCursor=$searchFallbackCursor"
        }
        searchUpdating = true
        searchLoadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!searchRemoteNoMore) {
                    try {
                        val data = historyRepository.searchHistories(query, page)
                        val cards = data.data.map { it.toVideoCard(BVApp.context) }
                        val currentSearch = withContext(Dispatchers.Main) {
                            if (!isCurrentSearch(query, generation)) {
                                false
                            } else {
                                addSearchResults(cards)
                                searchRemoteNoMore = data.data.size < 20
                                searchPage++
                                true
                            }
                        }
                        if (!currentSearch) return@launch
                        logger.fInfo {
                            "Search histories response: page=$page, count=${cards.size}, " +
                                "noMore=$searchRemoteNoMore"
                        }
                    } catch (it: CancellationException) {
                        throw it
                    } catch (it: Throwable) {
                        logger.fWarn { "Remote history search failed: ${it.stackTraceToString()}" }
                        withContext(Dispatchers.Main) {
                            if (isCurrentSearch(query, generation)) searchRemoteNoMore = true
                        }
                    }
                }

                scanOlderHistoriesUntilMatch(query, generation)
            } catch (it: CancellationException) {
                throw it
            } catch (it: Throwable) {
                logger.fWarn { "Search histories failed: ${it.stackTraceToString()}" }
            } finally {
                withContext(Dispatchers.Main) {
                    if (generation == searchGeneration) searchUpdating = false
                }
            }
        }
    }

    /**
     * The server-side history search occasionally misses records that are still
     * returned by the cursor feed. Scan older cursor pages until one page has a
     * local match; the next bottom-reached event continues from that cursor.
     */
    private suspend fun scanOlderHistoriesUntilMatch(
        query: String,
        generation: Int
    ) {
        while (true) {
            val pageCursor = withContext(Dispatchers.Main) {
                if (!isCurrentSearch(query, generation) || searchFallbackNoMore) null
                else searchFallbackCursor
            } ?: return

            val data = historyRepository.getHistories(
                cursor = pageCursor,
                preferApiType = ApiType.Web
            )
            val cards = data.data.map { it.toVideoCard(BVApp.context) }
            val matches = cards.filter { it.matchesHistorySearch(query) }
            val nextCursor = data.cursor
            val noMoreFallback = nextCursor == 0L || nextCursor == pageCursor

            val currentSearch = withContext(Dispatchers.Main) {
                if (!isCurrentSearch(query, generation)) {
                    false
                } else {
                    addSearchResults(matches)
                    searchFallbackCursor = nextCursor
                    searchFallbackNoMore = noMoreFallback
                    true
                }
            }
            if (!currentSearch) return

            logger.fInfo {
                "Fallback history search: cursor=$pageCursor, nextCursor=$nextCursor, " +
                    "items=${cards.size}, matches=${matches.size}, noMore=$noMoreFallback"
            }
            if (matches.isNotEmpty() || noMoreFallback) return
        }
    }

    private fun addSearchResults(cards: List<VideoCardData>) {
        val existing = searchResults.mapTo(mutableSetOf()) { it.historySearchIdentity() }
        searchResults.addAll(cards.filter { existing.add(it.historySearchIdentity()) })
    }

    private fun isCurrentSearch(query: String, generation: Int): Boolean {
        return generation == searchGeneration && query == searchQuery.trim()
    }

    private suspend fun updateHistories(context: Context = BVApp.context) {
        val generation = loadGeneration
        loadMutex.withLock {
            if (generation != loadGeneration || noMore) return@withLock
            logger.fInfo { "Updating histories with params [cursor=$cursor, apiType=${Prefs.apiType}]" }
            withContext(Dispatchers.Main) {
                if (generation == loadGeneration) updating = true
            }
            try {
                if (generation != loadGeneration) return@withLock
                val existingKeys = withContext(Dispatchers.Main) {
                    histories.mapTo(mutableSetOf()) { historyCardKey(it) }
                }
                val data = historyRepository.getHistories(
                    cursor = cursor,
                    preferApiType = Prefs.apiType
                )
                if (generation != loadGeneration) return@withLock

                data.data.forEach { historyItem ->
                    if (generation != loadGeneration) return@forEach
                    val itemKey = historyItemKey(historyItem)
                    if (!existingKeys.add(itemKey)) {
                        logger.fInfo { "Skip duplicated history item: $itemKey" }
                        return@forEach
                    }
                    histories.addWithMainContext(historyItem.toVideoCard(context))
                }
                if (generation != loadGeneration) return@withLock
                cursor = data.cursor
                logger.fInfo { "Update history cursor: [cursor=$cursor]" }
                logger.fInfo { "Update histories success" }
                if (cursor == 0L) {
                    withContext(Dispatchers.Main) {
                        if (generation == loadGeneration) noMore = true
                    }
                    logger.fInfo { "No more history" }
                }
            } catch (it: Throwable) {
                logger.fWarn { "Update histories failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> logger.fInfo { "User auth failure" }
                    else -> {}
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (generation == loadGeneration) updating = false
                }
            }
        }
    }

    var deleting by mutableStateOf(false)
        private set

    var deletePhase by mutableStateOf(0)
        private set

    var pendingFocusIndex by mutableStateOf(-1)
        private set

    fun deleteHistory(history: VideoCardData, targetIndex: Int) {
        val business = history.historyBusiness ?: return
        val kid = history.historyKid ?: return
        val viewAt = history.historyViewAt

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                deleting = true
            }
            runCatching {
                val success = historyRepository.deleteHistory(
                    business = business,
                    kid = kid
                )
                if (success) {
                    withContext(Dispatchers.Main) {
                        pendingFocusIndex = targetIndex
                        deletePhase = 1
                    }

                    withContext(Dispatchers.Main) {
                        val removeIndex = histories.indexOfFirst {
                            it.historyBusiness == business &&
                                it.historyKid == kid &&
                                (viewAt == null || it.historyViewAt == viewAt)
                        }
                        if (removeIndex >= 0) histories.removeAt(removeIndex)
                        searchResults.removeAll {
                            it.historyBusiness == business &&
                                it.historyKid == kid &&
                                (viewAt == null || it.historyViewAt == viewAt)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        deletePhase = 2
                    }

                    logger.fInfo { "Delete history success: business=$business, kid=$kid" }
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.history_delete_success)
                            .toast(BVApp.context)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.history_delete_failed)
                            .toast(BVApp.context)
                    }
                }
            }.onFailure {
                logger.fWarn { "Delete history failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    BVApp.context.getString(R.string.history_delete_failed)
                        .toast(BVApp.context)
                }
            }
            withContext(Dispatchers.Main) {
                deleting = false
            }
        }
    }

    fun resetDeletePhase() {
        deletePhase = 0
        pendingFocusIndex = -1
    }

    private fun historyItemKey(item: dev.aaa1115910.biliapi.entity.user.HistoryItem): String {
        return "${historyBusiness(item.type)}_${item.kid}_${item.viewAt}_${item.oid}"
    }

    private fun historyCardKey(item: VideoCardData): String {
        return "${item.historyBusiness}_${item.historyKid ?: item.avid}_${item.historyViewAt ?: 0L}_${item.avid}"
    }

    private fun historyBusiness(type: HistoryItemType): String? {
        return when (type) {
            HistoryItemType.Archive -> "archive"
            HistoryItemType.Pgc -> "pgc"
            HistoryItemType.Unknown -> null
        }
    }

    var historyPaused: Boolean? by mutableStateOf(null)
        private set
    var managingHistory by mutableStateOf(false)
        private set

    fun refreshHistoryPaused() {
        if (managingHistory) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { historyRepository.getHistoryPaused() }
                .onSuccess { paused ->
                    withContext(Dispatchers.Main) { historyPaused = paused }
                }
                .onFailure {
                    logger.fWarn { "Get history paused status failed: ${it.stackTraceToString()}" }
                }
        }
    }

    fun setHistoryPaused(paused: Boolean) {
        runHistoryOperation(if (paused) "已暂停记录观看历史" else "已恢复记录观看历史") {
            historyRepository.setHistoryPaused(paused)
            withContext(Dispatchers.Main) { historyPaused = paused }
        }
    }

    fun clearAllHistory() {
        runHistoryOperation("观看历史已清空") {
            historyRepository.clearHistory()
            withContext(Dispatchers.Main) {
                histories.clear()
                searchResults.clear()
                cursor = 0L
                noMore = true
                searchRemoteNoMore = true
                searchFallbackCursor = 0L
                searchFallbackNoMore = true
            }
        }
    }

    fun deleteViewedHistory() {
        val viewed = histories.filter {
            it.historyFinished && it.historyBusiness != null && it.historyKid != null
        }
        if (viewed.isEmpty()) {
            "没有已观看完的历史记录".toast(BVApp.context)
            return
        }
        runHistoryOperation("已观看历史已删除") {
            val success = historyRepository.deleteHistories(
                viewed.map { it.historyBusiness!! to it.historyKid!! }
            )
            if (!success) error("删除失败")
            val keys = viewed.mapTo(mutableSetOf()) { historyCardKey(it) }
            withContext(Dispatchers.Main) {
                histories.removeAll { historyCardKey(it) in keys }
                searchResults.removeAll { historyCardKey(it) in keys }
            }
        }
    }

    private fun runHistoryOperation(
        successMessage: String,
        action: suspend () -> Unit
    ) {
        if (managingHistory) return
        managingHistory = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { action() }
                .onSuccess {
                    withContext(Dispatchers.Main) { successMessage.toast(BVApp.context) }
                }
                .onFailure {
                    logger.fWarn { "History operation failed: ${it.stackTraceToString()}" }
                    withContext(Dispatchers.Main) {
                        (it.localizedMessage ?: "操作失败").toast(BVApp.context)
                    }
                }
            withContext(Dispatchers.Main) { managingHistory = false }
        }
    }

    private fun dev.aaa1115910.biliapi.entity.user.HistoryItem.toVideoCard(
        context: Context
    ): VideoCardData {
        val isPgc = type == HistoryItemType.Pgc
        return VideoCardData(
            avid = oid,
            bvid = bvid,
            title = title,
            cover = cover,
            upName = author,
            upId = authorId,
            upFace = authorFace,
            timeString = if (progress == -1) context.getString(R.string.play_time_finish)
            else context.getString(
                R.string.play_time_history,
                (progress * 1000L).formatHourMinSec(),
                (duration * 1000L).formatHourMinSec()
            ),
            jumpToSeason = isPgc,
            epId = epid,
            seasonId = seasonId ?: if (isPgc) kid.toInt() else null,
            pubTime = viewAt.toSmartDate() + context.getString(R.string.view_at),
            historyViewAt = viewAt,
            historyBusiness = historyBusiness(type),
            historyKid = kid,
            historyFinished = progress == -1
        )
    }

    fun clearData() {
        loadGeneration++
        searchGeneration++
        searchDebounceJob?.cancel()
        searchLoadJob?.cancel()
        histories.clear()
        searchResults.clear()
        searchQuery = ""
        searchPage = 1
        searchRemoteNoMore = false
        searchFallbackCursor = 0L
        searchFallbackNoMore = false
        searchUpdating = false
        cursor = 0L
        noMore = false
        updating = false
        logger.fInfo { "History data cleared" }
    }
}
