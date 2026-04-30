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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.tv.component.live.LiveHistoryCard
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.TopNav
import dev.aaa1115910.bv.tv.component.TopNavItem
import dev.aaa1115910.bv.tv.component.live.LiveRoomCard
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import dev.aaa1115910.bv.viewmodel.live.LiveTabType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

private class LiveNavCommitState {
    var parentJob: Job? = null
    var subJob: Job? = null
    var pendingParentNav: LiveParentNavItem? = null
    var pendingSubNav: SubAreaNavItem? = null

    fun cancelParent() {
        parentJob?.cancel()
        parentJob = null
        pendingParentNav = null
    }

    fun cancelSub() {
        subJob?.cancel()
        subJob = null
        pendingSubNav = null
    }

    fun cancelAll() {
        cancelParent()
        cancelSub()
    }
}

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
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)
    val navSelectionCommitDelay = if (enableMainUiAnimation) 250L else 150L

    val gridState = rememberLazyGridState()
    val subNavFocusRequester = remember { FocusRequester() }
    val firstContentFocusRequester = remember { FocusRequester() }
    var focusLayer by remember { mutableStateOf<LiveFocusLayer?>(null) }
    val navCommitState = remember { LiveNavCommitState() }

    val currentRoomList = liveViewModel.getCurrentRoomList()
    val currentHistoryList = liveViewModel.historyList
    val currentContentKey = liveViewModel.currentContentKey()
    var suppressLoadMore by remember { mutableStateOf(false) }
    val currentListSize = when (liveViewModel.currentTabType) {
        LiveTabType.History -> currentHistoryList.size
        else -> currentRoomList.size
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

        liveViewModel.lastFocusedRoomIndex = 0
        when (nav) {
            LiveParentNavItem.Recommend -> liveViewModel.switchTab(LiveTabType.Recommend)
            LiveParentNavItem.Following -> liveViewModel.switchTab(LiveTabType.Following)
            LiveParentNavItem.History -> liveViewModel.switchTab(LiveTabType.History)
            is LiveParentNavItem.Area -> liveViewModel.switchToAreaGroup(nav.group)
        }
    }

    fun scheduleParentNavCommit(nav: LiveParentNavItem) {
        navCommitState.cancelParent()
        navCommitState.cancelSub()
        navCommitState.pendingParentNav = nav

        if (isCommittedParentNav(nav)) {
            navCommitState.pendingParentNav = null
            return
        }

        navCommitState.parentJob = scope.launch {
            delay(navSelectionCommitDelay)
            if (focusLayer != LiveFocusLayer.ParentNav) return@launch
            if (navCommitState.pendingParentNav != nav) return@launch
            commitParentNav(nav)
            navCommitState.cancelParent()
        }
    }

    fun isCommittedSubNav(nav: SubAreaNavItem): Boolean {
        return liveViewModel.currentTabType == LiveTabType.Area &&
            liveViewModel.currentSubArea?.id == nav.area.id
    }

    fun commitSubNav(nav: SubAreaNavItem) {
        if (isCommittedSubNav(nav)) return

        liveViewModel.lastFocusedRoomIndex = 0
        liveViewModel.switchSubArea(nav.area)
    }

    fun scheduleSubNavCommit(nav: SubAreaNavItem) {
        navCommitState.cancelSub()
        navCommitState.pendingSubNav = nav

        if (isCommittedSubNav(nav)) {
            navCommitState.pendingSubNav = null
            return
        }

        navCommitState.subJob = scope.launch {
            delay(navSelectionCommitDelay)
            if (focusLayer != LiveFocusLayer.SubNav) return@launch
            if (navCommitState.pendingSubNav != nav) return@launch
            commitSubNav(nav)
            navCommitState.cancelSub()
        }
    }

    fun requestFirstContentFocusAfterFrame() {
        scope.launch {
            delay(16)
            firstContentFocusRequester.requestFocus(scope)
        }
    }

    fun requestFocusBelowParentNavAfterFrame() {
        scope.launch {
            delay(16)
            if (liveViewModel.currentTabType == LiveTabType.Area && liveViewModel.subAreaList.isNotEmpty()) {
                subNavFocusRequester.requestFocus(scope)
            } else {
                firstContentFocusRequester.requestFocus(scope)
            }
        }
    }

    fun commitPendingParentNavForDown(): Boolean {
        val pendingNav = navCommitState.pendingParentNav ?: return false
        navCommitState.cancelParent()
        commitParentNav(pendingNav)
        requestFocusBelowParentNavAfterFrame()
        return true
    }

    fun commitPendingSubNavForDown(): Boolean {
        val pendingNav = navCommitState.pendingSubNav ?: return false
        navCommitState.cancelSub()
        commitSubNav(pendingNav)
        requestFirstContentFocusAfterFrame()
        return true
    }

    DisposableEffect(Unit) {
        onDispose {
            navCommitState.cancelAll()
        }
    }

    BackHandler(focusLayer != null) {
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

                    TopNav(
                        modifier = Modifier
                            .focusRequester(navFocusRequester)
                            .padding(end = 80.dp)
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    focusLayer = LiveFocusLayer.ParentNav
                                } else {
                                    navCommitState.cancelParent()
                                    if (focusLayer == LiveFocusLayer.ParentNav) {
                                        focusLayer = null
                                    }
                                }
                            },
                        items = parentNavItems,
                        isLargePadding = false,
                        initialSelectedItem = initialSelectedItem,
                        onFocusedChanged = { nav ->
                            (nav as? LiveParentNavItem)?.let(::scheduleParentNavCommit)
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
                            onRequestDrawerFocus()
                        },
                        onPendingDownKeyEvent = {
                            commitPendingParentNavForDown()
                        },
                        onDownKeyEvent = {
                            commitPendingParentNavForDown()
                        }
                    )
                }

                // 第二行：子分区
                if (liveViewModel.currentTabType == LiveTabType.Area &&
                    liveViewModel.subAreaList.isNotEmpty()) {
                    val subAreaSnapshot = liveViewModel.subAreaList.toList()
                    val subNavItems = remember(subAreaSnapshot) {
                        subAreaSnapshot.map { SubAreaNavItem(it) }
                    }
                    TopNav(
                        modifier = Modifier
                            .focusRequester(subNavFocusRequester)
                            .padding(end = 80.dp)
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    focusLayer = LiveFocusLayer.SubNav
                                } else {
                                    navCommitState.cancelSub()
                                    if (focusLayer == LiveFocusLayer.SubNav) {
                                        focusLayer = null
                                    }
                                }
                            },
                        items = subNavItems,
                        isLargePadding = focusLayer != LiveFocusLayer.Content && currentListOnTop,
                        initialSelectedItem = subNavItems.firstOrNull {
                            it.area.id == liveViewModel.currentSubArea?.id
                        },
                        onFocusedChanged = { nav ->
                            (nav as? SubAreaNavItem)?.let(::scheduleSubNavCommit)
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
                        },
                        onPendingDownKeyEvent = {
                            commitPendingSubNavForDown()
                        },
                        onDownKeyEvent = {
                            commitPendingSubNavForDown()
                        }
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
                LoadingTip()
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
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstContentFocusRequester)
                                } else {
                                    Modifier
                                },
                                data = room,
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
                            key = { _, room -> room.roomId }
                        ) { index, room ->
                            LiveRoomCard(
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstContentFocusRequester)
                                } else {
                                    Modifier
                                },
                                data = room,
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
                        item {
                            LoadingTip()
                        }
                    }
                }
            }
        }
    }
}
