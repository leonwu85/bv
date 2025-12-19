package dev.aaa1115910.bv.viewmodel.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LiveViewModel(
    private val liveRepository: LiveRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger("LiveViewModel")

    /**
     * 扁平化的分区列表（所有子分区）
     */
    val areaList = mutableStateListOf<LiveAreaItem>()

    /**
     * 当前选中的分区
     */
    var currentArea by mutableStateOf<LiveAreaItem?>(null)

    /**
     * 当前分区的直播间列表
     */
    val roomList = mutableStateListOf<LiveRoomItem>()

    /**
     * 是否正在加载
     */
    var loading by mutableStateOf(false)

    /**
     * 是否有下一页
     */
    var hasMore by mutableStateOf(true)

    /**
     * 当前页码
     */
    private var currentPage = 1

    /**
     * 上次聚焦的直播间索引（用于从播放器返回时恢复焦点）
     */
    var lastFocusedRoomIndex by mutableStateOf(0)

    init {
        loadAreas()
    }

    /**
     * 加载所有分区
     */
    fun loadAreas() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = liveRepository.getLiveAreaList()
                if (response.code == 0) {
                    // 扁平化所有子分区
                    val allAreas = response.data.flatMap { it.list }
                    withContext(Dispatchers.Main) {
                        areaList.clear()
                        areaList.addAll(allAreas)
                        // 默认选中第一个分区
                        if (allAreas.isNotEmpty() && currentArea == null) {
                            currentArea = allAreas[0]
                            loadRooms(refresh = true)
                        }
                    }
                    logger.info { "Loaded ${allAreas.size} live areas" }
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
     * 切换分区
     */
    fun switchArea(area: LiveAreaItem) {
        if (currentArea?.id == area.id) return
        currentArea = area
        loadRooms(refresh = true)
    }

    /**
     * 加载直播间列表
     * @param refresh 是否刷新（清空现有数据）
     */
    fun loadRooms(refresh: Boolean = false) {
        val area = currentArea ?: return
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
                        roomList.addAll(response.data)
                        hasMore = response.data.size >= 30
                        if (hasMore) {
                            currentPage++
                        }
                    }
                    logger.info { "Loaded ${response.data.size} rooms for area ${area.name}, page $currentPage" }
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

    /**
     * 刷新当前分区的直播间列表
     */
    fun refresh() {
        loadRooms(refresh = true)
    }

    /**
     * 加载更多直播间
     */
    fun loadMore() {
        if (hasMore && !loading) {
            loadRooms(refresh = false)
        }
    }
}
