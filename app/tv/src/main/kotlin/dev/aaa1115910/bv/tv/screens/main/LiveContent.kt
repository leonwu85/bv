package dev.aaa1115910.bv.tv.screens.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.tv.component.live.LiveHistoryCard
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.TopNav
import dev.aaa1115910.bv.tv.component.TopNavItem
import dev.aaa1115910.bv.tv.component.live.LiveRoomCard
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import dev.aaa1115910.bv.viewmodel.live.LiveTabType
import io.github.oshai.kotlinlogging.KotlinLogging
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

@Composable
fun LiveContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    liveViewModel: LiveViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("LiveContent")
    val context = LocalContext.current

    val gridState = rememberLazyGridState()
    val subNavFocusRequester = remember { FocusRequester() }
    var focusOnContent by remember { mutableStateOf(false) }
    var parentNavHasFocus by remember { mutableStateOf(false) }
    var subNavHasFocus by remember { mutableStateOf(false) }

    val currentRoomList = liveViewModel.getCurrentRoomList()
    val currentHistoryList = liveViewModel.historyList
    val currentContentKey = liveViewModel.currentContentKey()
    var suppressLoadMore by remember { mutableStateOf(false) }
    val currentListSize by remember {
        derivedStateOf {
            when (liveViewModel.currentTabType) {
                LiveTabType.History -> currentHistoryList.size
                else -> currentRoomList.size
            }
        }
    }

    val currentListOnTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(currentContentKey) {
        suppressLoadMore = true
        gridState.scrollToItem(0)
        suppressLoadMore = false
    }

    // 监听滚动位置，触发分页加载
    LaunchedEffect(
        currentContentKey,
        gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index,
        currentListSize,
        liveViewModel.loading
    ) {
        if (suppressLoadMore || currentListSize == 0 || liveViewModel.loading || !liveViewModel.currentHasMore()) {
            return@LaunchedEffect
        }
        val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val totalItems = currentListSize
        if (lastVisibleIndex >= totalItems - 5) {
            logger.info { "Trigger load more, lastVisibleIndex: $lastVisibleIndex, totalItems: $totalItems" }
            liveViewModel.loadMore()
        }
    }

    // 恢复焦点到上次点击的直播间
    LaunchedEffect(currentContentKey, currentListSize) {
        if (liveViewModel.lastFocusedRoomIndex > 0 &&
            liveViewModel.lastFocusedRoomIndex < currentListSize) {
            gridState.scrollToItem(liveViewModel.lastFocusedRoomIndex)
        }
    }

    // 判断当前是否有子分区导航
    val hasSubNav = liveViewModel.currentTabType == LiveTabType.Area && liveViewModel.subAreaList.isNotEmpty()

    BackHandler(focusOnContent || subNavHasFocus || parentNavHasFocus) {
        logger.info { "onFocusBackToNav" }
        if (subNavHasFocus) {
            navFocusRequester.requestFocus(scope)
            return@BackHandler
        }
        if (parentNavHasFocus) {
            drawerItemFocusRequesters[DrawerItem.Live]?.requestFocus()
            return@BackHandler
        }
        // 如果没有子分区导航，直接返回到主分区导航
        if (hasSubNav) {
            subNavFocusRequester.requestFocus(scope)
        } else {
            navFocusRequester.requestFocus(scope)
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

                    TopNav(
                        modifier = Modifier
                            .focusRequester(navFocusRequester)
                            .padding(end = 80.dp)
                            .onFocusChanged { parentNavHasFocus = it.hasFocus },
                        items = parentNavItems,
                        isLargePadding = false,
                        initialSelectedItem = initialSelectedItem,
                        onSelectedChanged = { nav ->
                            liveViewModel.lastFocusedRoomIndex = 0
                            when (nav) {
                                is LiveParentNavItem.Recommend -> {
                                    liveViewModel.switchTab(LiveTabType.Recommend)
                                }
                                is LiveParentNavItem.Following -> {
                                    liveViewModel.switchTab(LiveTabType.Following)
                                }
                                is LiveParentNavItem.History -> {
                                    liveViewModel.switchTab(LiveTabType.History)
                                    liveViewModel.refresh()
                                }
                                is LiveParentNavItem.Area -> {
                                    liveViewModel.switchTab(LiveTabType.Area)
                                    liveViewModel.switchParentArea(nav.group)
                                }
                            }
                            scope.launch { gridState.scrollToItem(0) }
                        },
                        onClick = { nav ->
                            when (nav) {
                                is LiveParentNavItem.Recommend -> {
                                    if (liveViewModel.currentTabType == LiveTabType.Recommend) {
                                        liveViewModel.lastFocusedRoomIndex = 0
                                        liveViewModel.refresh()
                                        scope.launch { gridState.scrollToItem(0) }
                                    }
                                }
                                is LiveParentNavItem.Following -> {
                                    if (liveViewModel.currentTabType == LiveTabType.Following) {
                                        liveViewModel.lastFocusedRoomIndex = 0
                                        liveViewModel.refresh()
                                        scope.launch { gridState.scrollToItem(0) }
                                    }
                                }
                                is LiveParentNavItem.History -> {
                                    if (liveViewModel.currentTabType == LiveTabType.History) {
                                        liveViewModel.lastFocusedRoomIndex = 0
                                        liveViewModel.refresh()
                                        scope.launch { gridState.scrollToItem(0) }
                                    }
                                }
                                is LiveParentNavItem.Area -> {
                                    if (nav.group.id == liveViewModel.currentParentGroup?.id) {
                                        liveViewModel.lastFocusedRoomIndex = 0
                                        liveViewModel.refresh()
                                        scope.launch { gridState.scrollToItem(0) }
                                    }
                                }
                            }
                        },
                        onLeftKeyEvent = {
                            drawerItemFocusRequesters[DrawerItem.Live]?.requestFocus()
                        }
                    )
                }

                // 第二行：子分区
                if (liveViewModel.currentTabType == LiveTabType.Area &&
                    liveViewModel.subAreaList.isNotEmpty()) {
                    val subNavItems = liveViewModel.subAreaList.map { SubAreaNavItem(it) }
                    key(liveViewModel.currentParentGroup?.id) {
                        TopNav(
                            modifier = Modifier
                                .focusRequester(subNavFocusRequester)
                                .padding(end = 80.dp)
                                .onFocusChanged { subNavHasFocus = it.hasFocus },
                            items = subNavItems,
                            isLargePadding = !focusOnContent && currentListOnTop,
                            initialSelectedItem = subNavItems.firstOrNull {
                                it.area.id == liveViewModel.currentSubArea?.id
                            },
                            onSelectedChanged = { nav ->
                                (nav as? SubAreaNavItem)?.let {
                                    liveViewModel.lastFocusedRoomIndex = 0
                                    liveViewModel.switchSubArea(it.area)
                                }
                            },
                            onClick = { nav ->
                                (nav as? SubAreaNavItem)?.let { item ->
                                    if (item.area.id == liveViewModel.currentSubArea?.id) {
                                        liveViewModel.lastFocusedRoomIndex = 0
                                        liveViewModel.refresh()
                                        scope.launch { gridState.scrollToItem(0) }
                                    }
                                }
                            },
                            onLeftKeyEvent = {
                                navFocusRequester.requestFocus(scope)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
        ) {
            if (currentListSize == 0 && liveViewModel.loading) {
                LoadingTip()
            } else if (currentListSize == 0) {
                // 空状态提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyText = when (liveViewModel.currentTabType) {
                        LiveTabType.Recommend -> "暂无推荐直播"
                        LiveTabType.Following -> "关注的主播暂未开播"
                        LiveTabType.History -> "暂无直播历史"
                        LiveTabType.Area -> "该分区暂无直播"
                    }
                    Text(text = emptyText)
                }
            } else {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (liveViewModel.currentTabType == LiveTabType.History) {
                        itemsIndexed(
                            items = currentHistoryList,
                            key = { _, room -> "${room.roomId}:${room.viewAt}" }
                        ) { index, room ->
                            LiveHistoryCard(
                                data = room,
                                onClick = {
                                    liveViewModel.lastFocusedRoomIndex = index
                                    VideoPlayerV3Activity.actionStartLive(
                                        context = context,
                                        roomId = room.roomId,
                                        title = room.title,
                                        upName = room.uname,
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
                            key = { _, room -> room.roomId }
                        ) { index, room ->
                            LiveRoomCard(
                                data = room,
                                onClick = {
                                    liveViewModel.lastFocusedRoomIndex = index
                                    VideoPlayerV3Activity.actionStartLive(
                                        context = context,
                                        roomId = room.roomId,
                                        title = room.title,
                                        upName = room.uname,
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
                        item {
                            LoadingTip()
                        }
                    }
                }
            }
        }
    }
}
