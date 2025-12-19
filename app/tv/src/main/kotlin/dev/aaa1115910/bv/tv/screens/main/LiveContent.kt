package dev.aaa1115910.bv.tv.screens.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.TopNav
import dev.aaa1115910.bv.tv.component.TopNavItem
import dev.aaa1115910.bv.tv.component.live.LiveRoomCard
import dev.aaa1115910.bv.util.LiveStreamUrlFetcher
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

// 为直播分区创建稳定的 TopNavItem，确保选中项与焦点切换一致
private data class LiveTopNavItem(val area: LiveAreaItem) : TopNavItem {
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
    var focusOnContent by remember { mutableStateOf(false) }
    var topNavHasFocus by remember { mutableStateOf(false) }

    val currentListOnTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        }
    }

    // 监听滚动位置，触发分页加载
    LaunchedEffect(gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val totalItems = liveViewModel.roomList.size
        // 滚动到倒数第5个item时加载更多
        if (lastVisibleIndex >= totalItems - 5 && liveViewModel.hasMore && !liveViewModel.loading) {
            logger.info { "Trigger load more, lastVisibleIndex: $lastVisibleIndex, totalItems: $totalItems" }
            liveViewModel.loadMore()
        }
    }

    // 恢复焦点到上次点击的直播间
    LaunchedEffect(liveViewModel.roomList.size) {
        if (liveViewModel.lastFocusedRoomIndex > 0 && 
            liveViewModel.lastFocusedRoomIndex < liveViewModel.roomList.size) {
            gridState.scrollToItem(liveViewModel.lastFocusedRoomIndex)
        }
    }

    // 当 areaList 首次加载完成后，请求 TopNav 焦点（必须在 Composable 内部）
    LaunchedEffect(liveViewModel.areaList.isNotEmpty()) {
        if (liveViewModel.areaList.isNotEmpty()) {
            kotlinx.coroutines.delay(100)  // 等待 TopNav 完全渲染
            navFocusRequester.requestFocus(scope)
        }
    }

    BackHandler(focusOnContent || topNavHasFocus) {
        logger.info { "onFocusBackToNav" }
        if (topNavHasFocus) {
            drawerItemFocusRequesters[DrawerItem.Live]?.requestFocus()
            return@BackHandler
        }
        navFocusRequester.requestFocus(scope)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            // 只在区域列表加载完成后显示TopNav
            if (liveViewModel.areaList.isNotEmpty()) {
                // 构建稳定的 nav items 列表，避免匿名对象导致 initialSelectedItem 不匹配
                val navItems = remember(liveViewModel.areaList) {
                    liveViewModel.areaList.map { LiveTopNavItem(it) }
                }
                TopNav(
                    modifier = Modifier
                        .focusRequester(navFocusRequester)
                        .padding(end = 80.dp)
                        .onFocusChanged { topNavHasFocus = it.hasFocus },
                    items = navItems,
                    isLargePadding = !focusOnContent && currentListOnTop,
                    initialSelectedItem = navItems.firstOrNull { it.area.id == liveViewModel.currentArea?.id },
                    onSelectedChanged = { nav ->
                        (nav as? LiveTopNavItem)?.let { liveViewModel.switchArea(it.area) }
                    },
                    onClick = { nav ->
                        // 点击当前Tab刷新数据
                        (nav as? LiveTopNavItem)?.let { item ->
                            if (item.area.id == liveViewModel.currentArea?.id) {
                                liveViewModel.refresh()
                            }
                        }
                    },
                    onLeftKeyEvent = {
                        drawerItemFocusRequesters[DrawerItem.Live]?.requestFocus()
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
        ) {
            if (liveViewModel.roomList.isEmpty() && liveViewModel.loading) {
                LoadingTip()
            } else {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(
                        items = liveViewModel.roomList,
                        key = { _, room -> room.roomId }
                    ) { index, room ->
                    LiveRoomCard(
                        data = room,
                        onClick = {
                            // 保存焦点位置
                            liveViewModel.lastFocusedRoomIndex = index
                            // 启动播放器
                            scope.launch(Dispatchers.IO) {
                                val playInfo = LiveStreamUrlFetcher.fetchLiveStreamUrl(room.roomId)
                                if (playInfo != null) {
                                    VideoPlayerV3Activity.actionStartLive(
                                        context = context,
                                        roomId = playInfo.roomId,
                                        streamUrl = playInfo.streamUrl,
                                        title = room.title,
                                        upName = room.uname
                                    )
                                }
                            }
                        },
                        onFocus = {
                            logger.debug { "Focus on room ${room.roomId}" }
                        }
                    )
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
