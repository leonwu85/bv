package dev.aaa1115910.bv.viewmodel.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.biliapi.entity.live.LiveAreaGroup
import dev.aaa1115910.biliapi.entity.live.LiveHistoryCursor
import dev.aaa1115910.biliapi.entity.live.LiveHistoryItem
import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.repositories.LiveHistoryRepository
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

/**
 * 直播 Tab 类型枚举
 */
enum class LiveTabType {
    Recommend,   // 推荐
    Following,   // 关注
    History,     // 历史
    Area         // 分区
}

@KoinViewModel
class LiveViewModel(
    private val liveRepository: LiveRepository,
    private val liveHistoryRepository: LiveHistoryRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger("LiveViewModel")

    private data class AreaRoomCache(
        val rooms: List<LiveRoomItem>,
        val nextPage: Int,
        val hasMore: Boolean
    )

    // ==================== Tab 状态 ====================

    /**
     * 当前 Tab 类型
     */
    var currentTabType by mutableStateOf(LiveTabType.Recommend)
        private set

    // ==================== 推荐相关 ====================

    /**
     * 推荐直播间列表
     */
    val recommendList = mutableStateListOf<LiveRoomItem>()

    /**
     * 推荐列表当前页码
     */
    private var recommendPage = 1

    /**
     * 推荐列表是否还有更多
     */
    var recommendHasMore by mutableStateOf(true)
        private set

    // ==================== 关注相关 ====================

    /**
     * 关注主播直播列表
     */
    val followingList = mutableStateListOf<LiveRoomItem>()

    /**
     * 关注列表当前页码
     */
    private var followingPage = 1

    /**
     * 关注列表是否还有更多
     */
    var followingHasMore by mutableStateOf(true)
        private set

    /**
     * 正在直播的关注主播数量
     */
    var followingLiveCount by mutableStateOf(0)
        private set

    // ==================== 历史相关 ====================

    /**
     * 直播观看历史列表
     */
    val historyList = mutableStateListOf<LiveHistoryItem>()

    /**
     * 历史列表分页游标
     */
    private var historyCursor by mutableStateOf(LiveHistoryCursor())

    /**
     * 历史列表是否还有更多
     */
    var historyHasMore by mutableStateOf(true)
        private set

    // ==================== 分区相关（保持原有逻辑） ====================

    /**
     * 主分区列表（父分区组）
     */
    val parentAreaGroups = mutableStateListOf<LiveAreaGroup>()

    /**
     * 当前选中的主分区组
     */
    var currentParentGroup by mutableStateOf<LiveAreaGroup?>(null)

    /**
     * 当前主分区下的子分区列表
     */
    val subAreaList = mutableStateListOf<LiveAreaItem>()

    /**
     * 当前选中的子分区
     */
    var currentSubArea by mutableStateOf<LiveAreaItem?>(null)

    /**
     * 当前分区的直播间列表
     */
    val roomList = mutableStateListOf<LiveRoomItem>()

    // ==================== 通用状态 ====================

    /**
     * 是否正在加载
     */
    var recommendLoading by mutableStateOf(false)
        private set

    var followingLoading by mutableStateOf(false)
        private set

    var historyLoading by mutableStateOf(false)
        private set

    var areaLoading by mutableStateOf(false)
        private set

    val loading: Boolean
        get() = when (currentTabType) {
            LiveTabType.Recommend -> recommendLoading
            LiveTabType.Following -> followingLoading
            LiveTabType.History -> historyLoading
            LiveTabType.Area -> areaLoading
        }

    /**
     * 是否有下一页（分区模式）
     */
    var hasMore by mutableStateOf(true)

    /**
     * 当前页码（分区模式）
     */
    private var currentPage = 1

    /**
     * 上次聚焦的直播间索引（用于从播放器返回时恢复焦点）
     */
    var lastFocusedRoomIndex by mutableStateOf(0)

    private var recommendJob: Job? = null
    private var followingJob: Job? = null
    private var historyJob: Job? = null
    private var areaJob: Job? = null

    private var recommendRequestVersion = 0
    private var followingRequestVersion = 0
    private var historyRequestVersion = 0
    private var areaRequestVersion = 0

    private val areaRoomCache = mutableMapOf<String, AreaRoomCache>()

    init {
        loadAreas()
        loadRecommend(refresh = true)
    }

    // ==================== Tab 切换 ====================

    /**
     * 切换 Tab 类型
     */
    fun switchTab(tabType: LiveTabType) {
        if (currentTabType == tabType) return
        currentTabType = tabType

        when (tabType) {
            LiveTabType.Recommend -> {
                if (recommendList.isEmpty()) {
                    loadRecommend(refresh = true)
                }
            }
            LiveTabType.Following -> {
                if (followingList.isEmpty()) {
                    loadFollowing(refresh = true)
                }
            }
            LiveTabType.History -> {
                if (historyList.isEmpty()) {
                    loadHistory(refresh = true)
                }
            }
            LiveTabType.Area -> {
                if (roomList.isEmpty() && currentSubArea != null) {
                    loadRooms(refresh = true)
                }
            }
        }
    }

    // ==================== 推荐加载 ====================

    /**
     * 加载推荐直播间
     */
    fun loadRecommend(refresh: Boolean = false) {
        if (recommendLoading && !refresh) return
        if (refresh) {
            recommendJob?.cancel()
        }

        val targetPage = if (refresh) 1 else recommendPage
        val requestVersion = ++recommendRequestVersion
        recommendLoading = true

        recommendJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = liveRepository.getLiveFeed(page = targetPage)
                if (response.code == 0) {
                    val incomingRooms = response.data.cardList.mapNotNull { card ->
                        if (card.cardType == "small_card_v1") {
                            card.cardData?.smallCardV1?.toLiveRoomItem()
                        } else {
                            null
                        }
                    }.distinctBy { it.roomId }

                    withContext(Dispatchers.Main) {
                        if (requestVersion != recommendRequestVersion) return@withContext

                        if (refresh) {
                            recommendList.clear()
                            recommendList.addAll(incomingRooms)
                        } else {
                            val existingIds = recommendList.map { it.roomId }.toHashSet()
                            val uniqueNewRooms = incomingRooms.filter { it.roomId !in existingIds }
                            recommendList.addAll(uniqueNewRooms)
                        }

                        recommendHasMore = response.data.hasMore == 1
                        recommendPage = if (recommendHasMore) targetPage + 1 else targetPage
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (requestVersion != recommendRequestVersion) return@withContext
                        "加载推荐失败: ${response.message}".toast(BVApp.context)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Failed to load live feed" }
                withContext(Dispatchers.Main) {
                    if (requestVersion != recommendRequestVersion) return@withContext
                    "加载推荐失败: ${e.message}".toast(BVApp.context)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (requestVersion == recommendRequestVersion) {
                        recommendLoading = false
                    }
                }
            }
        }
    }

    // ==================== 关注加载 ====================

    /**
     * 加载关注主播直播
     */
    fun loadFollowing(refresh: Boolean = false) {
        if (followingLoading && !refresh) return
        if (refresh) {
            followingJob?.cancel()
        }

        val requestVersion = ++followingRequestVersion
        followingLoading = true

        followingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = liveRepository.getLiveFeed(page = 1)

                if (response.code == 0) {
                    val incomingRooms = response.data.cardList.flatMap { card ->
                        if (card.cardType == "my_idol_v1") {
                            card.cardData?.myIdolV1?.list?.map { it.toLiveRoomItem() }.orEmpty()
                        } else {
                            emptyList()
                        }
                    }.distinctBy { it.roomId }

                    withContext(Dispatchers.Main) {
                        if (requestVersion != followingRequestVersion) return@withContext

                        followingLiveCount = incomingRooms.size
                        if (refresh) {
                            followingList.clear()
                            followingList.addAll(incomingRooms)
                        } else {
                            val existingIds = followingList.map { it.roomId }.toHashSet()
                            val uniqueNewRooms = incomingRooms.filter { it.roomId !in existingIds }
                            followingList.addAll(uniqueNewRooms)
                        }
                        followingPage = 1
                        followingHasMore = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (requestVersion != followingRequestVersion) return@withContext
                        "加载关注直播失败: ${response.message}".toast(BVApp.context)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Failed to load live following" }
                withContext(Dispatchers.Main) {
                    if (requestVersion != followingRequestVersion) return@withContext
                    "加载关注直播失败: ${e.message}".toast(BVApp.context)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (requestVersion == followingRequestVersion) {
                        followingLoading = false
                    }
                }
            }
        }
    }

    // ==================== 历史加载 ====================

    /**
     * 加载直播观看历史
     */
    fun loadHistory(refresh: Boolean = false) {
        if (historyLoading && !refresh) return
        if (refresh) {
            historyJob?.cancel()
        }

        val targetCursor = if (refresh) LiveHistoryCursor() else historyCursor
        val requestVersion = ++historyRequestVersion
        historyLoading = true

        historyJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = liveHistoryRepository.getLiveHistories(
                    max = targetCursor.max,
                    viewAt = targetCursor.viewAt,
                    business = targetCursor.business
                )
                withContext(Dispatchers.Main) {
                    if (requestVersion != historyRequestVersion) return@withContext

                    if (refresh) {
                        historyList.clear()
                        historyList.addAll(response.items)
                    } else {
                        val existingKeys = historyList
                            .map { "${it.roomId}:${it.viewAt}" }
                            .toHashSet()
                        val uniqueNewItems = response.items.filter {
                            "${it.roomId}:${it.viewAt}" !in existingKeys
                        }
                        historyList.addAll(uniqueNewItems)
                    }

                    historyCursor = response.cursor
                    historyHasMore = response.hasMore
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Failed to load live histories" }
                withContext(Dispatchers.Main) {
                    if (requestVersion != historyRequestVersion) return@withContext
                    "加载直播历史失败: ${e.message}".toast(BVApp.context)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (requestVersion == historyRequestVersion) {
                        historyLoading = false
                    }
                }
            }
        }
    }

    // ==================== 分区加载（保持原有逻辑） ====================

    /**
     * 加载所有分区
     */
    fun loadAreas() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = liveRepository.getLiveAreaList()
                if (response.code == 0) {
                    withContext(Dispatchers.Main) {
                        parentAreaGroups.clear()
                        parentAreaGroups.addAll(response.data)
                    }
                    logger.info { "Loaded ${response.data.size} parent area groups" }
                } else {
                    withContext(Dispatchers.Main) {
                        "加载直播分区失败: ${response.message}".toast(BVApp.context)
                    }
                }
            }.onFailure { e ->
                logger.error(e) { "Failed to load live areas" }
                withContext(Dispatchers.Main) {
                    "加载直播分区失败: ${e.message}".toast(BVApp.context)
                }
            }
        }
    }

    /**
     * 切换主分区
     */
    fun switchParentArea(group: LiveAreaGroup) {
        if (currentParentGroup?.id == group.id) return
        currentParentGroup = group
        subAreaList.clear()
        subAreaList.addAll(group.list)
        // 默认选中第一个子分区
        if (group.list.isNotEmpty()) {
            currentSubArea = group.list[0]
            restoreAreaCacheOrLoad(group.list[0])
        } else {
            areaJob?.cancel()
            areaRequestVersion++
            areaLoading = false
            roomList.clear()
            hasMore = false
            currentPage = 1
        }
    }

    /**
     * 切换子分区
     */
    fun switchSubArea(area: LiveAreaItem) {
        if (currentSubArea?.id == area.id) return
        currentSubArea = area
        restoreAreaCacheOrLoad(area)
    }

    /**
     * 加载直播间列表
     * @param refresh 是否刷新（清空现有数据）
     */
    fun loadRooms(refresh: Boolean = false) {
        val area = currentSubArea ?: return
        if (areaLoading && !refresh) return
        if (refresh) {
            areaJob?.cancel()
        }

        val areaContextKey = buildAreaContextKey(area.parentId, area.id)
        val targetPage = if (refresh) 1 else currentPage
        val requestVersion = ++areaRequestVersion
        areaLoading = true

        areaJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = liveRepository.getLiveRoomList(
                    parentAreaId = area.parentId,
                    areaId = area.id,
                    page = targetPage,
                    pageSize = 30
                )
                if (response.code == 0) {
                    withContext(Dispatchers.Main) {
                        if (requestVersion != areaRequestVersion) return@withContext
                        if (getCurrentAreaContextKey() != areaContextKey) return@withContext

                        val incomingRooms = response.data.list.distinctBy { it.roomId }
                        if (refresh) {
                            roomList.clear()
                            roomList.addAll(incomingRooms)
                        } else {
                            val existingIds = roomList.map { it.roomId }.toHashSet()
                            val newRooms = incomingRooms.filter { it.roomId !in existingIds }
                            roomList.addAll(newRooms)
                        }
                        hasMore = response.data.list.size >= 30
                        currentPage = if (hasMore) targetPage + 1 else targetPage
                        areaRoomCache[areaContextKey] = AreaRoomCache(
                            rooms = roomList.toList(),
                            nextPage = currentPage,
                            hasMore = hasMore
                        )
                    }
                    logger.info { "Loaded ${response.data.list.size} rooms for area ${area.name}, page $targetPage" }
                } else {
                    withContext(Dispatchers.Main) {
                        if (requestVersion != areaRequestVersion) return@withContext
                        if (getCurrentAreaContextKey() != areaContextKey) return@withContext
                        "加载直播间列表失败: ${response.message}".toast(BVApp.context)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Failed to load live rooms" }
                withContext(Dispatchers.Main) {
                    if (requestVersion != areaRequestVersion) return@withContext
                    if (getCurrentAreaContextKey() != areaContextKey) return@withContext
                    "加载直播间列表失败: ${e.message}".toast(BVApp.context)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (requestVersion == areaRequestVersion && getCurrentAreaContextKey() == areaContextKey) {
                        areaLoading = false
                    }
                }
            }
        }
    }

    // ==================== 统一接口 ====================

    /**
     * 获取当前 Tab 的直播间列表
     */
    fun getCurrentRoomList(): List<LiveRoomItem> {
        return when (currentTabType) {
            LiveTabType.Recommend -> recommendList
            LiveTabType.Following -> followingList
            LiveTabType.History -> emptyList()
            LiveTabType.Area -> roomList
        }
    }

    /**
     * 刷新当前 Tab
     */
    fun refresh() {
        when (currentTabType) {
            LiveTabType.Recommend -> loadRecommend(refresh = true)
            LiveTabType.Following -> loadFollowing(refresh = true)
            LiveTabType.History -> loadHistory(refresh = true)
            LiveTabType.Area -> loadRooms(refresh = true)
        }
    }

    /**
     * 加载更多
     */
    fun loadMore() {
        when (currentTabType) {
            LiveTabType.Recommend -> {
                if (recommendHasMore && !recommendLoading) {
                    loadRecommend(refresh = false)
                }
            }
            LiveTabType.Following -> {
                if (followingHasMore && !followingLoading) {
                    loadFollowing(refresh = false)
                }
            }
            LiveTabType.History -> {
                if (historyHasMore && !historyLoading) {
                    loadHistory(refresh = false)
                }
            }
            LiveTabType.Area -> {
                if (hasMore && !areaLoading) {
                    loadRooms(refresh = false)
                }
            }
        }
    }

    fun currentContentKey(): String {
        return when (currentTabType) {
            LiveTabType.Recommend -> "recommend"
            LiveTabType.Following -> "following"
            LiveTabType.History -> "history"
            LiveTabType.Area -> "area:${getCurrentAreaContextKey()}"
        }
    }

    /**
     * 当前 Tab 是否还有更多数据
     */
    fun currentHasMore(): Boolean {
        return when (currentTabType) {
            LiveTabType.Recommend -> recommendHasMore
            LiveTabType.Following -> followingHasMore
            LiveTabType.History -> historyHasMore
            LiveTabType.Area -> hasMore
        }
    }

    private fun getCurrentAreaContextKey(): String {
        val area = currentSubArea ?: return ""
        return buildAreaContextKey(area.parentId, area.id)
    }

    private fun restoreAreaCacheOrLoad(area: LiveAreaItem) {
        val areaContextKey = buildAreaContextKey(area.parentId, area.id)
        val cache = areaRoomCache[areaContextKey]
        if (cache != null) {
            areaJob?.cancel()
            areaRequestVersion++
            areaLoading = false
            roomList.clear()
            roomList.addAll(cache.rooms)
            currentPage = cache.nextPage
            hasMore = cache.hasMore
            logger.info { "Restore cached rooms for area ${area.name}, size=${cache.rooms.size}, nextPage=${cache.nextPage}" }
            return
        }
        loadRooms(refresh = true)
    }

    private fun buildAreaContextKey(parentAreaId: String, areaId: String): String {
        return "$parentAreaId:$areaId"
    }
}
