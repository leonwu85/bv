package dev.aaa1115910.bv.viewmodel.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.biliapi.entity.live.LiveAreaGroup
import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

/**
 * 直播 Tab 类型枚举
 */
enum class LiveTabType {
    Recommend,   // 推荐
    Following,   // 关注
    Area         // 分区
}

@KoinViewModel
class LiveViewModel(
    private val liveRepository: LiveRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger("LiveViewModel")

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
    var loading by mutableStateOf(false)

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
        if (loading) return

        viewModelScope.launch(Dispatchers.IO) {
            loading = true
            if (refresh) {
                recommendPage = 1
                withContext(Dispatchers.Main) {
                    recommendList.clear()
                    recommendHasMore = true
                }
            }

            runCatching {
                val response = liveRepository.getLiveFeed(page = recommendPage)
                if (response.code == 0) {
                    val newRooms = mutableListOf<LiveRoomItem>()
                    // 从 card_list 中提取直播间
                    response.data.cardList.forEach { card ->
                        if (card.cardType == "small_card_v1") {
                            card.cardData?.smallCardV1?.toLiveRoomItem()?.let {
                                newRooms.add(it)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val existingIds = recommendList.map { it.roomId }.toHashSet()
                        val uniqueNewRooms = newRooms.filter { it.roomId !in existingIds }
                        recommendList.addAll(uniqueNewRooms)
                        recommendHasMore = response.data.hasMore == 1
                        if (recommendHasMore) {
                            recommendPage++
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        "加载推荐失败: ${response.message}".toast(BVApp.context)
                    }
                }
            }.onFailure { e ->
                logger.error(e) { "Failed to load live feed" }
                withContext(Dispatchers.Main) {
                    "加载推荐失败: ${e.message}".toast(BVApp.context)
                }
            }
            loading = false
        }
    }

    // ==================== 关注加载 ====================

    /**
     * 加载关注主播直播
     */
    fun loadFollowing(refresh: Boolean = false) {
        if (loading) return

        viewModelScope.launch(Dispatchers.IO) {
            loading = true
            if (refresh) {
                followingPage = 1
                withContext(Dispatchers.Main) {
                    followingList.clear()
                    followingHasMore = false  // 关注列表不支持分页
                }
            }

            runCatching {
                val response = liveRepository.getLiveFeed(page = 1)

                if (response.code == 0) {
                    val newRooms = mutableListOf<LiveRoomItem>()
                    response.data.cardList.forEach { card ->
                        if (card.cardType == "my_idol_v1") {
                            card.cardData?.myIdolV1?.list?.forEach { item ->
                                newRooms.add(item.toLiveRoomItem())
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        followingLiveCount = newRooms.size
                        val existingIds = followingList.map { it.roomId }.toHashSet()
                        val uniqueNewRooms = newRooms.filter { it.roomId !in existingIds }
                        followingList.addAll(uniqueNewRooms)
                        followingHasMore = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        "加载关注直播失败: ${response.message}".toast(BVApp.context)
                    }
                }
            }.onFailure { e ->
                logger.error(e) { "Failed to load live following" }
                withContext(Dispatchers.Main) {
                    "加载关注直播失败: ${e.message}".toast(BVApp.context)
                }
            }
            loading = false
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
            loadRooms(refresh = true)
        }
    }

    /**
     * 切换子分区
     */
    fun switchSubArea(area: LiveAreaItem) {
        if (currentSubArea?.id == area.id) return
        currentSubArea = area
        loadRooms(refresh = true)
    }

    /**
     * 加载直播间列表
     * @param refresh 是否刷新（清空现有数据）
     */
    fun loadRooms(refresh: Boolean = false) {
        val area = currentSubArea ?: return
        if (loading) return

        viewModelScope.launch(Dispatchers.IO) {
            loading = true
            if (refresh) {
                currentPage = 1
                withContext(Dispatchers.Main) {
                    roomList.clear()
                    hasMore = true
                }
            }

            runCatching {
                val response = liveRepository.getLiveRoomList(
                    parentAreaId = area.parentId,
                    areaId = area.id,
                    page = currentPage,
                    pageSize = 30
                )
                if (response.code == 0) {
                    withContext(Dispatchers.Main) {
                        val existingIds = roomList.map { it.roomId }.toHashSet()
                        val newRooms = response.data.list.filter { it.roomId !in existingIds }
                        roomList.addAll(newRooms)
                        hasMore = response.data.list.size >= 30
                        if (hasMore) {
                            currentPage++
                        }
                    }
                    logger.info { "Loaded ${response.data.list.size} rooms for area ${area.name}, page $currentPage" }
                } else {
                    withContext(Dispatchers.Main) {
                        "加载直播间列表失败: ${response.message}".toast(BVApp.context)
                    }
                }
            }.onFailure { e ->
                logger.error(e) { "Failed to load live rooms" }
                withContext(Dispatchers.Main) {
                    "加载直播间列表失败: ${e.message}".toast(BVApp.context)
                }
            }
            loading = false
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
            LiveTabType.Area -> loadRooms(refresh = true)
        }
    }

    /**
     * 加载更多
     */
    fun loadMore() {
        when (currentTabType) {
            LiveTabType.Recommend -> {
                if (recommendHasMore && !loading) {
                    loadRecommend(refresh = false)
                }
            }
            LiveTabType.Following -> {
                if (followingHasMore && !loading) {
                    loadFollowing(refresh = false)
                }
            }
            LiveTabType.Area -> {
                if (hasMore && !loading) {
                    loadRooms(refresh = false)
                }
            }
        }
    }

    /**
     * 当前 Tab 是否还有更多数据
     */
    fun currentHasMore(): Boolean {
        return when (currentTabType) {
            LiveTabType.Recommend -> recommendHasMore
            LiveTabType.Following -> followingHasMore
            LiveTabType.Area -> hasMore
        }
    }
}
