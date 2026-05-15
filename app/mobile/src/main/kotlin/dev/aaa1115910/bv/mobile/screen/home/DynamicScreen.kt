package dev.aaa1115910.bv.mobile.screen.home

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.origeek.imageViewer.previewer.ImagePreviewerState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.biliapi.entity.user.DynamicUpUser
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.bv.entity.DynamicTabType
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.activities.DynamicDetailActivity
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.home.dynamic.DynamicItem
import dev.aaa1115910.bv.mobile.component.user.UserAvatar
import dev.aaa1115910.bv.mobile.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.getLane
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun DynamicScreen(
    modifier: Modifier = Modifier,
    dynamicViewModel: DynamicViewModel = koinViewModel(),
    dynamicGridState: LazyStaggeredGridState,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("DynamicScreen")
    val windowSize = calculateWindowSizeClass(context as Activity).widthSizeClass
    val tabs = DynamicTabType.entries
    var selectedTab by rememberSaveable {
        androidx.compose.runtime.mutableStateOf(Prefs.dynamicDefaultTab)
    }
    val selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex,
        pageCount = { tabs.size }
    )
    val videoGridState = rememberLazyStaggeredGridState()
    val pgcGridState = rememberLazyStaggeredGridState()
    val articleGridState = rememberLazyStaggeredGridState()
    val upGridState = rememberLazyStaggeredGridState()

    val onClickDynamicItem: (DynamicItem) -> Unit = { dynamicItem ->
        logger.fInfo { "click dynamic type: ${dynamicItem.type}" }
        when (dynamicItem.type) {
            DynamicType.Av -> {
                VideoPlayerActivity.actionStart(
                    context = context,
                    aid = dynamicItem.video!!.aid,
                    fromSeason = dynamicItem.video!!.seasonId != null &&
                            dynamicItem.video!!.seasonId != 0,
                )
            }

            DynamicType.Pgc -> {
                VideoPlayerActivity.actionStart(
                    context = context,
                    aid = 0,
                    fromSeason = true,
                    epid = dynamicItem.pgc!!.epid,
                    seasonId = dynamicItem.pgc!!.seasonId,
                )
            }

            else -> {
                if (dynamicItem.id != null) {
                    DynamicDetailActivity.actionStart(context, dynamicItem.id!!)
                } else {
                    "原动态不存在".toast(context)
                }
            }
        }
    }

    LaunchedEffect(dynamicViewModel.isLogin) {
        if (dynamicViewModel.isLogin) {
            dynamicViewModel.loadFollowUpPanel()
            if (selectedTab == DynamicTabType.Up && dynamicViewModel.selectedUp == null) {
                dynamicViewModel.selectUp(dynamicViewModel.selfUp)
            }
            if (dynamicViewModel.itemCount(selectedTab) == 0) {
                dynamicViewModel.loadMoreByType(selectedTab)
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (!dynamicViewModel.isLogin) return@LaunchedEffect
        if (selectedTab == DynamicTabType.Up && dynamicViewModel.selectedUp == null) {
            dynamicViewModel.selectUp(dynamicViewModel.selfUp)
        }
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
        if (dynamicViewModel.itemCount(selectedTab) == 0) {
            dynamicViewModel.loadMoreByType(selectedTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val target = tabs[pagerState.currentPage]
        if (target != selectedTab) selectedTab = target
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        divider = {},
                        containerColor = Color.Transparent,
                        edgePadding = 0.dp,
                        minTabWidth = 0.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = index == selectedTabIndex,
                                onClick = {
                                    selectedTab = tab
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                            ) {
                                Box(
                                    modifier = Modifier.height(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        text = tab.getDisplayName(context),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = { "发布动态暂未实现".toast(context) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "发布动态"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (!dynamicViewModel.isLogin) {
                DynamicCenteredMessage(text = "请先登录后查看动态")
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    DynamicUpPanel(
                        dynamicViewModel = dynamicViewModel,
                        selectedTab = selectedTab,
                        onSelectAll = {
                            selectedTab = DynamicTabType.All
                        },
                        onSelectUp = { up ->
                            dynamicViewModel.selectUp(up)
                            selectedTab = DynamicTabType.Up
                        },
                        onLoadMore = {
                            scope.launch(Dispatchers.IO) {
                                dynamicViewModel.loadFollowUpPanel()
                            }
                        }
                    )
                    HorizontalPager(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        state = pagerState
                    ) { page ->
                        val type = tabs[page]
                        val state = when (type) {
                            DynamicTabType.All -> dynamicGridState
                            DynamicTabType.Video -> videoGridState
                            DynamicTabType.Pgc -> pgcGridState
                            DynamicTabType.Article -> articleGridState
                            DynamicTabType.Up -> upGridState
                        }
                        DynamicTabContent(
                            modifier = Modifier.fillMaxSize(),
                            type = type,
                            state = state,
                            windowSize = windowSize,
                            dynamicViewModel = dynamicViewModel,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onClickDynamicItem = onClickDynamicItem,
                            onClickVideo = { video ->
                                VideoPlayerActivity.actionStart(
                                    context = context,
                                    aid = video.aid,
                                    fromSeason = video.seasonId != null && video.seasonId != 0,
                                    epid = video.epid,
                                    seasonId = video.seasonId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicTabContent(
    modifier: Modifier = Modifier,
    type: DynamicTabType,
    state: LazyStaggeredGridState,
    windowSize: WindowWidthSizeClass,
    dynamicViewModel: DynamicViewModel,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    onClickDynamicItem: (DynamicItem) -> Unit,
    onClickVideo: (DynamicVideo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lane by remember { derivedStateOf { state.getLane() } }
    val isLoading = dynamicViewModel.isLoading(type)
    val pullRefreshState = rememberPullToRefreshState()

    state.OnBottomReached(loading = isLoading) {
        scope.launch(Dispatchers.IO) {
            dynamicViewModel.loadMoreByType(type)
        }
    }

    PullToRefreshBox(
        modifier = modifier,
        state = pullRefreshState,
        isRefreshing = isLoading && dynamicViewModel.itemCount(type) == 0,
        onRefresh = {
            scope.launch(Dispatchers.IO) {
                if (type == DynamicTabType.All) dynamicViewModel.refreshFollowUpPanel()
                dynamicViewModel.refreshByType(type)
                dynamicViewModel.loadMoreByType(type)
            }
        }
    ) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .ifElse(
                    { windowSize != WindowWidthSizeClass.Compact },
                    Modifier.clip(MaterialTheme.shapes.medium)
                )
                .background(MaterialTheme.colorScheme.surface),
            columns = StaggeredGridCells.Adaptive(300.dp),
            state = state,
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                start = if (lane == 1) 0.dp else 8.dp,
                top = if (lane == 1) 0.dp else 8.dp,
                end = if (lane == 1) 0.dp else 8.dp,
                bottom = 100.dp
            )
        ) {
            when (type) {
                DynamicTabType.All -> {
                    items(dynamicViewModel.dynamicAllList) { dynamicItem ->
                        DynamicCard(
                            lane = lane,
                            dynamicItem = dynamicItem,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onTempBlockAuthor = {
                                dynamicViewModel.tempBlockAuthor(it.mid)
                                "已临时屏蔽${it.author}(${it.mid})，重启后恢复".toast(context)
                            },
                            onClick = onClickDynamicItem
                        )
                    }
                }

                DynamicTabType.Video -> {
                    items(dynamicViewModel.dynamicVideoList) { video ->
                        SmallVideoCard(
                            modifier = Modifier.ifElse(
                                lane != 1,
                                Modifier.clip(MaterialTheme.shapes.medium)
                            ),
                            data = video.toVideoCardData(),
                            onClick = { onClickVideo(video) }
                        )
                    }
                }

                DynamicTabType.Pgc -> {
                    items(dynamicViewModel.dynamicPgcList) { dynamicItem ->
                        DynamicCard(
                            lane = lane,
                            dynamicItem = dynamicItem,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onTempBlockAuthor = {
                                dynamicViewModel.tempBlockAuthor(it.mid)
                                "已临时屏蔽${it.author}(${it.mid})，重启后恢复".toast(context)
                            },
                            onClick = onClickDynamicItem
                        )
                    }
                }

                DynamicTabType.Article -> {
                    items(dynamicViewModel.dynamicArticleList) { dynamicItem ->
                        DynamicCard(
                            lane = lane,
                            dynamicItem = dynamicItem,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onTempBlockAuthor = {
                                dynamicViewModel.tempBlockAuthor(it.mid)
                                "已临时屏蔽${it.author}(${it.mid})，重启后恢复".toast(context)
                            },
                            onClick = onClickDynamicItem
                        )
                    }
                }

                DynamicTabType.Up -> {
                    items(dynamicViewModel.dynamicUpList) { dynamicItem ->
                        DynamicCard(
                            lane = lane,
                            dynamicItem = dynamicItem,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onTempBlockAuthor = {
                                dynamicViewModel.tempBlockAuthor(it.mid)
                                "已临时屏蔽${it.author}(${it.mid})，重启后恢复".toast(context)
                            },
                            onClick = onClickDynamicItem
                        )
                    }
                }
            }

            if (dynamicViewModel.itemCount(type) == 0 && !isLoading) {
                item {
                    DynamicCenteredMessage(
                        modifier = Modifier
                            .height(300.dp)
                            .fillMaxSize(),
                        text = if (type == DynamicTabType.Up && dynamicViewModel.selectedUp == null) {
                            "选择一个 UP 主查看动态"
                        } else {
                            "啥都没有"
                        }
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .height(96.dp)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicCard(
    lane: Int?,
    dynamicItem: DynamicItem,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    onTempBlockAuthor: (DynamicItem.DynamicAuthorModule) -> Unit,
    onClick: (DynamicItem) -> Unit
) {
    DynamicItem(
        modifier = Modifier.ifElse(lane != 1, Modifier.clip(MaterialTheme.shapes.medium)),
        dynamicItem = dynamicItem,
        previewerState = previewerState,
        onShowPreviewer = onShowPreviewer,
        onTempBlockAuthor = onTempBlockAuthor,
        onClick = onClick
    )
}

@Composable
private fun DynamicUpPanel(
    dynamicViewModel: DynamicViewModel,
    selectedTab: DynamicTabType,
    onSelectAll: () -> Unit,
    onSelectUp: (DynamicUpUser) -> Unit,
    onLoadMore: () -> Unit
) {
    if (!dynamicViewModel.isLogin) return
    val context = LocalContext.current
    var showLiveUp by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .width(64.dp)
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 200.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            DynamicLiveUpItem(
                liveCount = dynamicViewModel.liveUpCount,
                expanded = showLiveUp,
                onClick = {
                    showLiveUp = !showLiveUp
                }
            )
        }
        if (showLiveUp && dynamicViewModel.liveUpList.isNotEmpty()) {
            items(dynamicViewModel.liveUpList, key = { "live-${it.roomId}-${it.mid}" }) { up ->
                DynamicUpChip(
                    face = up.face,
                    name = up.uname.ifBlank { up.title.ifBlank { "Live" } },
                    selected = false,
                    live = true,
                    hasUpdate = false,
                    onClick = {
                        if (up.roomId > 0) {
                            VideoPlayerActivity.actionStartLive(
                                context = context,
                                roomId = up.roomId.toInt(),
                                title = up.title.ifBlank { up.uname.ifBlank { "直播间" } },
                                upName = up.uname,
                                upFace = up.face,
                                upMid = up.mid
                            )
                        } else {
                            "直播间不存在".toast(context)
                        }
                    }
                )
            }
        }
        item {
            DynamicUpChip(
                face = "",
                name = "全部动态",
                selected = selectedTab == DynamicTabType.All,
                isAll = true,
                hasUpdate = false,
                onClick = onSelectAll
            )
        }
        item {
            DynamicUpChip(
                face = dynamicViewModel.selfUp.face,
                name = "我",
                selected = selectedTab == DynamicTabType.Up &&
                        dynamicViewModel.selectedUp?.mid == dynamicViewModel.selfUp.mid,
                hasUpdate = false,
                onClick = { onSelectUp(dynamicViewModel.selfUp) }
            )
        }
        items(dynamicViewModel.followUpList, key = { it.mid }) { up ->
            if (up == dynamicViewModel.followUpList.lastOrNull() && dynamicViewModel.followUpHasMore) {
                LaunchedEffect(up.mid) { onLoadMore() }
            }
            DynamicUpChip(
                face = up.face,
                name = up.uname,
                selected = selectedTab == DynamicTabType.Up && dynamicViewModel.selectedUp?.mid == up.mid,
                hasUpdate = up.hasUpdate,
                onClick = { onSelectUp(up) }
            )
        }
        if (dynamicViewModel.loadingFollowUp) {
            item {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        } else if (dynamicViewModel.followUpList.isEmpty()) {
            item {
                IconButton(onClick = onLoadMore) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新关注 UP"
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicLiveUpItem(
    liveCount: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .height(60.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Live($liveCount)",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DynamicUpChip(
    face: String,
    name: String,
    selected: Boolean,
    hasUpdate: Boolean,
    onClick: () -> Unit,
    isAll: Boolean = false,
    live: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .height(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            if (isAll) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(0xFF5CB67B)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "BV",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                UserAvatar(
                    avatar = face,
                    size = 42.dp
                )
            }
            if (hasUpdate) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
            if (live) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(text = "Live")
                }
            }
        }
        Text(
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            text = name.ifBlank { "-" },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun DynamicCenteredMessage(
    modifier: Modifier = Modifier.fillMaxSize(),
    text: String
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DynamicVideo.toVideoCardData(): VideoCardData {
    return VideoCardData(
        avid = aid,
        bvid = bvid.orEmpty(),
        title = title,
        cover = cover,
        upName = author,
        upId = authorId,
        upFace = authorFace,
        play = play,
        danmaku = danmaku,
        time = duration.toLong() * 1000L,
        jumpToSeason = seasonId != null && seasonId != 0,
        epId = epid,
        seasonId = seasonId,
        pubTime = pubTime,
        isChargingArc = isChargingArc,
        badgeText = chargingArcBadge
    )
}
