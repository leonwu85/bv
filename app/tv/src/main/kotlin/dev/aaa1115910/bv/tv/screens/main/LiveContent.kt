package dev.aaa1115910.bv.tv.screens.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.bv.tv.R
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.tv.component.live.LiveHistoryCard
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.TopNavItem
import dev.aaa1115910.bv.tv.component.live.LiveRoomCard
import dev.aaa1115910.bv.tv.component.live.LiveTabRow
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.tv.util.LocalTvPageActive
import dev.aaa1115910.bv.tv.util.LocalTvImageLoadingAllowed
import dev.aaa1115910.bv.tv.util.LocalTvPreloadCoordinator
import dev.aaa1115910.bv.tv.util.rememberProgressiveImageLoadLimit
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.requestFocusWithRetry
import dev.aaa1115910.bv.util.scrollToItemIfAvailable
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import dev.aaa1115910.bv.viewmodel.live.LiveTabType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import dev.aaa1115910.biliapi.entity.live.LiveAreaGroup

// 主分区 TopNavItem - 支持推荐/关注/分区
private sealed class LiveParentNavItem : TopNavItem {
    data object Recommend : LiveParentNavItem() {
        override fun getDisplayName(context: Context): String = "推荐"
    }
    data object Following : LiveParentNavItem() {
        override fun getDisplayName(context: Context): String = "关注"
    }
    data object History : LiveParentNavItem() {
        override fun getDisplayName(context: Context): String = "历史"
    }
    data class Area(val group: LiveAreaGroup) : LiveParentNavItem() {
        override fun getDisplayName(context: Context): String = group.name
    }
}

// 子分区 TopNavItem
private data class SubAreaNavItem(val area: LiveAreaItem) : TopNavItem {
    override fun getDisplayName(context: Context): String = area.name
}

private enum class LiveFocusLayer {
    ParentNav,
    SubNav,
    Content,
}

private data class LiveFocusRequest(
    val contentKey: String,
    val destination: LiveFocusLayer,
)

private const val LIVE_PAGINATION_IDLE_MS = 120L

@Composable
fun LiveContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    onRequestDrawerFocus: () -> Unit = {},
    liveViewModel: LiveViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("LiveContent")
    val context = LocalContext.current
    val gridColumns by Prefs.gridColumnsFlow.collectAsState(Prefs.gridColumns)
    val gridPadding = dimensionResource(R.dimen.grid_padding) / 2
    val gridSpacing = dimensionResource(R.dimen.grid_spacedBy) / 2
    val pageActive = LocalTvPageActive.current
    val preloadCoordinator = LocalTvPreloadCoordinator.current
    val subNavFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    var focusLayer by remember { mutableStateOf<LiveFocusLayer?>(null) }
    var pendingFocusRequest by remember { mutableStateOf<LiveFocusRequest?>(null) }

    val currentRoomList = liveViewModel.getCurrentRoomList()
    val currentHistoryList = liveViewModel.historyList
    val currentContentKey = liveViewModel.currentContentKey()
    // Start the destination with its own viewport. Reusing the old grid and then scrolling
    // to zero caused two layouts and could paginate the new tab using the old tab's index.
    val gridState = remember(currentContentKey) {
        LazyGridState(firstVisibleItemIndex = liveViewModel.lastFocusedRoomIndex.coerceAtLeast(0))
    }
    val currentListSize = when (liveViewModel.currentTabType) {
        LiveTabType.History -> currentHistoryList.size
        else -> currentRoomList.size
    }

    // Observe layout in a flow, not in composition: scrolling and measuring must not
    // recompose both navigation rows. Hidden pages and tab selection do not paginate.
    LaunchedEffect(currentContentKey, gridState, pageActive) {
        if (!pageActive) return@LaunchedEffect
        snapshotFlow {
            val count = if (liveViewModel.currentTabType == LiveTabType.History) {
                liveViewModel.historyList.size
            } else {
                liveViewModel.getCurrentRoomList().size
            }
            liveViewModel.currentContentKey() == currentContentKey &&
                focusLayer == LiveFocusLayer.Content &&
                !gridState.isScrollInProgress &&
                count > 0 && !liveViewModel.loading && liveViewModel.currentHasMore() &&
                (gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) >= count - 5
        }.distinctUntilChanged().collectLatest { shouldLoadMore ->
            if (shouldLoadMore) {
                preloadCoordinator.awaitInteractionIdle(LIVE_PAGINATION_IDLE_MS)
                if (liveViewModel.currentContentKey() == currentContentKey &&
                    focusLayer == LiveFocusLayer.Content && !gridState.isScrollInProgress
                ) {
                    liveViewModel.loadMore()
                }
            }
        }
    }

    val imageLoadLimit = rememberProgressiveImageLoadLimit(
        enabled = LocalTvImageLoadingAllowed.current && pageActive,
        progressive = true,
        itemCount = currentListSize,
        columns = gridColumns,
        contentKey = currentContentKey,
    )

    // 判断当前是否有子分区导航
    val hasSubNav = liveViewModel.currentTabType == LiveTabType.Area && liveViewModel.subAreaList.isNotEmpty()

    fun isCommittedParentNav(nav: LiveParentNavItem): Boolean {
        return when (nav) {
            LiveParentNavItem.Recommend -> liveViewModel.currentTabType == LiveTabType.Recommend
            LiveParentNavItem.Following -> liveViewModel.currentTabType == LiveTabType.Following
            LiveParentNavItem.History -> liveViewModel.currentTabType == LiveTabType.History
            is LiveParentNavItem.Area -> {
                liveViewModel.currentTabType == LiveTabType.Area &&
                    liveViewModel.currentParentGroup?.id == nav.group.id
            }
        }
    }

    fun commitParentNav(nav: LiveParentNavItem) {
        if (isCommittedParentNav(nav)) return

        pendingFocusRequest = null
        liveViewModel.lastFocusedRoomIndex = 0
        when (nav) {
            LiveParentNavItem.Recommend -> liveViewModel.switchTab(LiveTabType.Recommend)
            LiveParentNavItem.Following -> liveViewModel.switchTab(LiveTabType.Following)
            LiveParentNavItem.History -> liveViewModel.switchTab(LiveTabType.History)
            is LiveParentNavItem.Area -> liveViewModel.switchToAreaGroup(nav.group)
        }
    }

    fun isCommittedSubNav(nav: SubAreaNavItem): Boolean {
        return liveViewModel.currentTabType == LiveTabType.Area &&
            liveViewModel.currentSubArea?.id == nav.area.id
    }

    fun commitSubNav(nav: SubAreaNavItem) {
        if (isCommittedSubNav(nav)) return

        pendingFocusRequest = null
        liveViewModel.lastFocusedRoomIndex = 0
        liveViewModel.switchSubArea(nav.area)
    }

    fun requestFocusBelowNav(fromParent: Boolean): Boolean {
        val destination = if (fromParent && liveViewModel.currentTabType == LiveTabType.Area &&
            liveViewModel.subAreaList.isNotEmpty()
        ) {
            LiveFocusLayer.SubNav
        } else {
            LiveFocusLayer.Content
        }
        pendingFocusRequest = LiveFocusRequest(liveViewModel.currentContentKey(), destination)
        return true
    }

    // Bind down-key focus to the selected content and its layout. If a cold tab is still
    // loading, keep the request until its first item exists; switching tabs cancels it.
    LaunchedEffect(
        pendingFocusRequest,
        currentContentKey,
        currentListSize,
        hasSubNav,
        liveViewModel.loading,
        pageActive,
    ) {
        val request = pendingFocusRequest ?: return@LaunchedEffect
        if (!pageActive || request.contentKey != currentContentKey) {
            pendingFocusRequest = null
            return@LaunchedEffect
        }
        if (request.destination == LiveFocusLayer.Content && currentListSize == 0) {
            if (!liveViewModel.loading) pendingFocusRequest = null
            return@LaunchedEffect
        }
        if (request.destination == LiveFocusLayer.SubNav && !hasSubNav) return@LaunchedEffect
        withFrameNanos { }
        val requester = if (request.destination == LiveFocusLayer.SubNav) {
            subNavFocusRequester
        } else {
            contentFocusRequester
        }
        requester.requestFocusWithRetry()
        if (pendingFocusRequest == request) pendingFocusRequest = null
    }

    BackHandler(focusLayer != null) {
        pendingFocusRequest = null
        logger.info { "onFocusBackToNav" }
        when (focusLayer) {
            LiveFocusLayer.Content -> {
                if (hasSubNav) {
                    subNavFocusRequester.requestFocus(scope)
                } else {
                    navFocusRequester.requestFocus(scope)
                }
            }
            LiveFocusLayer.SubNav -> {
                navFocusRequester.requestFocus(scope)
            }
            LiveFocusLayer.ParentNav -> {
                onRequestDrawerFocus()
            }
            null -> Unit
        }
    }

    // 构建主分区导航项列表：推荐 + 关注 + 分区列表
    val parentNavItems by remember {
        derivedStateOf {
            buildList {
                add(LiveParentNavItem.Recommend)
                add(LiveParentNavItem.Following)
                add(LiveParentNavItem.History)
                addAll(liveViewModel.parentAreaGroups.map { LiveParentNavItem.Area(it) })
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                // 第一行：主分区（推荐 + 关注 + 分区）
                if (parentNavItems.isNotEmpty()) {
                    val initialSelectedItem = when (liveViewModel.currentTabType) {
                        LiveTabType.Recommend -> LiveParentNavItem.Recommend
                        LiveTabType.Following -> LiveParentNavItem.Following
                        LiveTabType.History -> LiveParentNavItem.History
                        LiveTabType.Area -> liveViewModel.currentParentGroup?.let { LiveParentNavItem.Area(it) }
                    } ?: LiveParentNavItem.Recommend

                    LiveTabRow(
                        modifier = Modifier
                            .focusRequester(navFocusRequester)
                            .padding(end = 80.dp)
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    focusLayer = LiveFocusLayer.ParentNav
                                } else if (focusLayer == LiveFocusLayer.ParentNav) {
                                    focusLayer = null
                                }
                            },
                        items = parentNavItems,
                        selectedItem = initialSelectedItem,
                        itemKey = { nav ->
                            when (nav) {
                                LiveParentNavItem.Recommend -> "recommend"
                                LiveParentNavItem.Following -> "following"
                                LiveParentNavItem.History -> "history"
                                is LiveParentNavItem.Area -> "area:${nav.group.id}"
                            }
                        },
                        onSelectedChanged = ::commitParentNav,
                        onClick = { nav ->
                            when (nav) {
                                is LiveParentNavItem.Recommend -> {
                                    if (liveViewModel.currentTabType == LiveTabType.Recommend) {
                                        liveViewModel.lastFocusedRoomIndex = 0
                                        liveViewModel.refresh()
                                        scope.launch { gridState.scrollToItemIfAvailable(0) }
                                    }
                                }
                                is LiveParentNavItem.Following -> {
                                    if (liveViewModel.currentTabType == LiveTabType.Following) {
                                        liveViewModel.lastFocusedRoomIndex = 0
                                        liveViewModel.refresh()
                                        scope.launch { gridState.scrollToItemIfAvailable(0) }
                                    }
                                }
                                is LiveParentNavItem.History -> {
                                    if (liveViewModel.currentTabType == LiveTabType.History) {
                                        liveViewModel.lastFocusedRoomIndex = 0
                                        liveViewModel.refresh()
                                        scope.launch { gridState.scrollToItemIfAvailable(0) }
                                    }
                                }
                                is LiveParentNavItem.Area -> {
                                    if (nav.group.id == liveViewModel.currentParentGroup?.id) {
                                        liveViewModel.lastFocusedRoomIndex = 0
                                        liveViewModel.refresh()
                                        scope.launch { gridState.scrollToItemIfAvailable(0) }
                                    }
                                }
                            }
                        },
                        onLeftKeyEvent = {
                            pendingFocusRequest = null
                            onRequestDrawerFocus()
                        },
                        onDownKeyEvent = { requestFocusBelowNav(fromParent = true) }
                    )
                }

                // 第二行：子分区
                if (liveViewModel.currentTabType == LiveTabType.Area &&
                    liveViewModel.subAreaList.isNotEmpty()) {
                    val subAreaSnapshot = liveViewModel.subAreaList.toList()
                    val subNavItems = remember(subAreaSnapshot) {
                        subAreaSnapshot.map { SubAreaNavItem(it) }
                    }
                    LiveTabRow(
                        modifier = Modifier
                            .focusRequester(subNavFocusRequester)
                            .padding(end = 80.dp)
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    focusLayer = LiveFocusLayer.SubNav
                                } else if (focusLayer == LiveFocusLayer.SubNav) {
                                    focusLayer = null
                                }
                            },
                        items = subNavItems,
                        selectedItem = subNavItems.firstOrNull {
                            it.area.id == liveViewModel.currentSubArea?.id
                        },
                        itemKey = { it.area.id },
                        onSelectedChanged = ::commitSubNav,
                        onClick = { nav ->
                            (nav as? SubAreaNavItem)?.let { item ->
                                if (item.area.id == liveViewModel.currentSubArea?.id) {
                                    liveViewModel.lastFocusedRoomIndex = 0
                                    liveViewModel.refresh()
                                    scope.launch { gridState.scrollToItemIfAvailable(0) }
                                }
                            }
                        },
                        onLeftKeyEvent = {
                            pendingFocusRequest = null
                            navFocusRequester.requestFocus(scope)
                        },
                        onUpKeyEvent = {
                            pendingFocusRequest = null
                            navFocusRequester.requestFocus(scope)
                        },
                        onDownKeyEvent = { requestFocusBelowNav(fromParent = false) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onFocusChanged {
                    if (it.hasFocus) {
                        focusLayer = LiveFocusLayer.Content
                    } else if (focusLayer == LiveFocusLayer.Content) {
                        focusLayer = null
                    }
                }
        ) {
            if (currentListSize == 0 && liveViewModel.loading) {
                LoadingTip(deferIndicatorUntilInteractionIdle = true)
            } else if (currentListSize == 0) {
                // 空状态提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyText = when (liveViewModel.currentTabType) {
                        LiveTabType.Recommend -> "暂无推荐直播"
                        LiveTabType.Following -> "暂无关注的UP主直播中"
                        LiveTabType.History -> "暂无直播历史"
                        LiveTabType.Area -> {
                            val areaName = liveViewModel.currentSubArea?.name
                                ?: liveViewModel.currentParentGroup?.name
                            if (areaName.isNullOrBlank()) {
                                "暂无分区直播中"
                            } else {
                                "暂无${areaName}分区直播中"
                            }
                        }
                    }
                    Text(
                        text = emptyText,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(contentFocusRequester)
                        .focusGroup(),
                    state = gridState,
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(gridPadding),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                ) {
                    if (liveViewModel.currentTabType == LiveTabType.History) {
                        itemsIndexed(
                            items = currentHistoryList,
                            key = { index, room -> "$index:${room.roomId}:${room.viewAt}" },
                            contentType = { _, _ -> "live_history" }
                        ) { index, room ->
                            LiveHistoryCard(
                                data = room,
                                loadImages = index < imageLoadLimit,
                                onClick = {
                                    liveViewModel.lastFocusedRoomIndex = index
                                    VideoPlayerV3Activity.actionStartLive(
                                        context = context,
                                        roomId = room.roomId,
                                        title = room.title,
                                        upName = room.uname,
                                        upFace = room.face,
                                        upMid = room.uid,
                                        watchedNum = 0
                                    )
                                },
                                onFocus = {
                                    logger.debug { "Focus on history room ${room.roomId}" }
                                }
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = currentRoomList,
                            key = { index, room -> "$index:${room.roomId}" },
                            contentType = { _, _ -> "live_room" }
                        ) { index, room ->
                            LiveRoomCard(
                                data = room,
                                loadImages = index < imageLoadLimit,
                                onClick = {
                                    liveViewModel.lastFocusedRoomIndex = index
                                    VideoPlayerV3Activity.actionStartLive(
                                        context = context,
                                        roomId = room.roomId,
                                        title = room.title,
                                        upName = room.uname,
                                        upFace = room.face,
                                        upMid = room.uid,
                                        watchedNum = room.watchedShow?.num ?: (room.online / 10)
                                    )
                                },
                                onFocus = {
                                    logger.debug { "Focus on room ${room.roomId}" }
                                }
                            )
                        }
                    }

                    // 加载中提示
                    if (liveViewModel.loading) {
                        item(key = "live_loading", contentType = "loading") {
                            LoadingTip(deferIndicatorUntilInteractionIdle = true)
                        }
                    }
                }
            }
        }
    }
}
