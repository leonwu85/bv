package dev.aaa1115910.bv.tv.screens.main.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.bv.component.QrImage
import dev.aaa1115910.bv.entity.DynamicTabType
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.R as tvR
import dev.aaa1115910.bv.tv.activities.user.FollowActivity
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.activities.dynamic.DynamicDetailActivity
import dev.aaa1115910.bv.tv.component.ContentStatusCard
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.TvDynamicImageUseCase
import dev.aaa1115910.bv.tv.component.TvSafeDynamicImage
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.tv.util.LocalTvImageLoadingAllowed
import dev.aaa1115910.bv.tv.util.TOP_NAV_PRELOAD_STEP
import dev.aaa1115910.bv.tv.util.LocalTvPreloadCoordinator
import dev.aaa1115910.bv.tv.util.LocalTvUiPerformanceProfile
import dev.aaa1115910.bv.tv.util.boundedAdjacentNavItems
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.scrollToItemIfAvailable
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

private enum class DynamicFocusLayer {
    SubTab,
    Content,
}

private val DynamicAccent = Color(0xFFFB7299)
private val DynamicCardShape = RoundedCornerShape(8.dp)
private const val DynamicChargingUrlPrefix = "https://www.bilibili.com/h5/upower/index?mid="
private const val DYNAMIC_ADJACENT_PRELOAD_IDLE_MS = 500L

@Composable
private fun rememberDynamicListImageModel(url: String): ImageRequest? {
    val context = LocalContext.current
    val imageLoadingAllowed = LocalTvImageLoadingAllowed.current
    return remember(context, url, imageLoadingAllowed) {
        if (!imageLoadingAllowed || url.isBlank()) {
            null
        } else {
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(false)
                .build()
        }
    }
}

@Composable
fun NewDynamicsScreen(
    modifier: Modifier = Modifier,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    tabRowFocusRequester: FocusRequester,
    initialSelectedTabIndex: Int = 0,
    onSelectedTabChanged: (Int) -> Unit = {},
    onSubTabRowReady: (Int) -> Unit = {},
    onSubTabRowUnavailable: () -> Unit = {},
    onBackToParentTabRow: () -> Unit = {},
    onLeftKeyEvent: () -> Unit = {},
    onRightKeyEvent: () -> Unit = {},
    dynamicViewModel: DynamicViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val performanceProfile = LocalTvUiPerformanceProfile.current
    val preloadCoordinator = LocalTvPreloadCoordinator.current
    val dynamicTabs = remember {
        listOf(
            DynamicTabType.All,
            DynamicTabType.Video,
            DynamicTabType.Pgc,
            DynamicTabType.Article
        )
    }

    var selectedTabIndex by remember {
        mutableIntStateOf(initialSelectedTabIndex.coerceIn(0, dynamicTabs.lastIndex))
    }
    val focusedIndexState = remember { mutableIntStateOf(-1) }
    var showTip by remember { mutableStateOf(false) }
    var focusLayer by remember { mutableStateOf<DynamicFocusLayer?>(null) }
    var chargingQrContent by remember { mutableStateOf("") }
    val allStaggeredGridState = rememberLazyStaggeredGridState()
    val pgcGridState = rememberLazyGridState()
    val articleStaggeredGridState = rememberLazyStaggeredGridState()

    // 当子 Tab 切换时通知父组件记住选择
    LaunchedEffect(selectedTabIndex) {
        onSelectedTabChanged(selectedTabIndex)
    }

    val selectedTabType = dynamicTabs[selectedTabIndex]

    fun scrollToTabTop(tabType: DynamicTabType) {
        scope.launch {
            when (tabType) {
                DynamicTabType.Video -> lazyGridState.scrollToItemIfAvailable(0)
                DynamicTabType.All, DynamicTabType.Up -> allStaggeredGridState.scrollToItemIfAvailable(0)
                DynamicTabType.Pgc -> pgcGridState.scrollToItemIfAvailable(0)
                DynamicTabType.Article -> articleStaggeredGridState.scrollToItemIfAvailable(0)
            }
        }
    }

    fun updateFocusedIndex(index: Int) {
        focusedIndexState.intValue = index
        showTip = index >= 0
    }

    LaunchedEffect(initialSelectedTabIndex) {
        val targetIndex = initialSelectedTabIndex.coerceIn(0, dynamicTabs.lastIndex)
        if (selectedTabIndex != targetIndex) {
            selectedTabIndex = targetIndex
            updateFocusedIndex(-1)
        }
    }

    fun isListScrolling(tabType: DynamicTabType): Boolean = when (tabType) {
        DynamicTabType.Video -> lazyGridState.isScrollInProgress
        DynamicTabType.All, DynamicTabType.Up -> allStaggeredGridState.isScrollInProgress
        DynamicTabType.Pgc -> pgcGridState.isScrollInProgress
        DynamicTabType.Article -> articleStaggeredGridState.isScrollInProgress
    }

    suspend fun awaitListIdle(tabType: DynamicTabType) {
        while (true) {
            snapshotFlow { isListScrolling(tabType) }.first { !it }
            delay(DYNAMIC_ADJACENT_PRELOAD_IDLE_MS)
            if (!isListScrolling(tabType)) {
                return
            }
        }
    }

    // 根据选中的 Tab 获取对应的数据列表
    val currentList: List<DynamicItem> = when (selectedTabType) {
        DynamicTabType.All -> dynamicViewModel.dynamicAllList
        DynamicTabType.Video -> emptyList() // 视频标签页单独处理
        DynamicTabType.Pgc -> dynamicViewModel.dynamicPgcList
        DynamicTabType.Article -> dynamicViewModel.dynamicArticleList
        DynamicTabType.Up -> dynamicViewModel.dynamicUpList
    }

    // 获取当前标签的加载状态
    val (isLoading, hasMore) = when (selectedTabType) {
        DynamicTabType.All -> dynamicViewModel.loadingAll to dynamicViewModel.allHasMore
        DynamicTabType.Video -> dynamicViewModel.loadingVideo to dynamicViewModel.videoHasMore
        DynamicTabType.Pgc -> dynamicViewModel.loadingPgc to dynamicViewModel.pgcHasMore
        DynamicTabType.Article -> dynamicViewModel.loadingArticle to dynamicViewModel.articleHasMore
        DynamicTabType.Up -> dynamicViewModel.loadingUp to dynamicViewModel.upHasMore
    }


    // 视频点击处理器
    val onClickVideo: (DynamicVideo) -> Unit = { video ->
        VideoInfoActivity.actionStart(
            context = context,
            aid = video.aid,
            proxyArea = ProxyArea.checkProxyArea(video.title)
        )
    }

    val onLongClickVideo: (DynamicVideo) -> Unit = { video ->
        UpInfoActivity.actionStart(
            context,
            mid = video.authorId,
            name = video.author,
            face = video.authorFace
        )
    }

    val onClickDynamicItem: (DynamicItem) -> Unit = { dynamic ->
        val chargingUrl = dynamic.chargingQrContent()
        if (dynamic.isChargingBlocked() && chargingUrl.isNotBlank()) {
            chargingQrContent = chargingUrl
        } else {
            when (dynamic.type) {
                DynamicType.Av -> {
                    dynamic.video?.let { video ->
                        VideoInfoActivity.actionStart(
                            context = context,
                            aid = video.aid,
                            proxyArea = ProxyArea.checkProxyArea(dynamic.video?.title ?: "")
                        )
                    }
                }
                DynamicType.UgcSeason -> {
                    dynamic.ugcSeason?.let { ugcSeason ->
                        VideoInfoActivity.actionStart(
                            context = context,
                            aid = ugcSeason.aid,
                            proxyArea = ProxyArea.checkProxyArea(ugcSeason.title)
                        )
                    }
                }
                DynamicType.Pgc -> {
                    dynamic.pgc?.let { pgc ->
                        SeasonInfoActivity.actionStart(
                            context = context,
                            epId = pgc.epid,
                            seasonId = pgc.seasonId
                        )
                    }
                }
                else -> {
                    // 其他类型跳转到动态详情页
                    dynamic.id?.let { dynamicId ->
                        DynamicDetailActivity.actionStart(context, dynamicId)
                    }
                }
            }
        }
    }

    val onLongClickDynamicItem: (DynamicItem) -> Unit = { dynamic ->
        UpInfoActivity.actionStart(
            context,
            mid = dynamic.author.mid,
            name = dynamic.author.author,
            face = dynamic.author.avatar
        )
    }

    // Observe focus without making the whole screen read the focused index.
    LaunchedEffect(selectedTabType) {
        snapshotFlow {
            val index = focusedIndexState.intValue
            val itemCount = dynamicViewModel.itemCount(selectedTabType)
            index >= 0 &&
                    itemCount > 0 &&
                    index + 12 > itemCount &&
                    dynamicViewModel.hasMore(selectedTabType) &&
                    !dynamicViewModel.isLoading(selectedTabType)
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) {
                    withContext(Dispatchers.IO) {
                        dynamicViewModel.loadMoreByType(selectedTabType)
                    }
                }
            }
    }

    // 子 Tab 预加载：当前页优先，再按设备预算串行预取相邻页。
    // 任务直接挂在 LaunchedEffect 下，切换 Tab 时可以取消旧的预加载队列。
    LaunchedEffect(selectedTabType, dynamicViewModel.isLogin) {
        if (!dynamicViewModel.isLogin) return@LaunchedEffect
        val targets = boundedAdjacentNavItems(
            items = dynamicTabs,
            current = selectedTabType,
            step = TOP_NAV_PRELOAD_STEP,
            maxItems = performanceProfile.maxKeepPages,
        )
        withContext(Dispatchers.IO) {
            dynamicViewModel.ensureFirstPage(selectedTabType)
        }
        awaitListIdle(selectedTabType)
        withContext(Dispatchers.IO) {
            preloadCoordinator.runExclusive {
                targets.filter { it != selectedTabType }.forEach { type ->
                    dynamicViewModel.ensureFirstPage(type)
                }
            }
        }
    }

    // 在内容区域按返回键时，先将焦点给到子 TabRow
    BackHandler(focusLayer != null) {
        when (focusLayer) {
            DynamicFocusLayer.Content -> tabRowFocusRequester.requestFocus()
            DynamicFocusLayer.SubTab -> onBackToParentTabRow()
            null -> Unit
        }
    }

    if (dynamicViewModel.isLogin) {
        DisposableEffect(Unit) {
            onDispose {
                onSubTabRowUnavailable()
            }
        }

        Column(modifier = modifier.fillMaxSize()) {
            // Tab Row
            DynamicTabRow(
                tabs = dynamicTabs,
                tabRowFocusRequester = tabRowFocusRequester,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index ->
                    selectedTabIndex = index
                    updateFocusedIndex(-1)
                },
                onTabClick = { index ->
                    if (index == selectedTabIndex) {
                        // 再次点击当前 Tab，触发刷新
                        updateFocusedIndex(-1)
                        scrollToTabTop(selectedTabType)
                        scope.launch(Dispatchers.IO) {
                            dynamicViewModel.refreshByType(selectedTabType)
                            dynamicViewModel.loadMoreByType(selectedTabType)
                        }
                    } else {
                        // 切换 Tab
                        selectedTabIndex = index
                        updateFocusedIndex(-1)
                    }
                },
                onFocusChanged = {
                    if (it) {
                        focusLayer = DynamicFocusLayer.SubTab
                    } else if (focusLayer == DynamicFocusLayer.SubTab) {
                        focusLayer = null
                    }
                },
                onSelectedTabReady = onSubTabRowReady,
                onLeftKeyEvent = onLeftKeyEvent,
                onRightKeyEvent = onRightKeyEvent
            )

            // 根据选中的 Tab 显示不同的布局
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged {
                        if (it.hasFocus) {
                            focusLayer = DynamicFocusLayer.Content
                        } else if (focusLayer == DynamicFocusLayer.Content) {
                            focusLayer = null
                        }
                    }
            ) {
                // Tip 叠加层
                if (showTip) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, end = 20.dp)
                            .offset(x = (-20).dp, y = (-24).dp)
                            .align(Alignment.TopEnd),
                        text = stringResource(tvR.string.entry_follow_screen),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.End
                    )
                }
                when (selectedTabType) {
                    DynamicTabType.Video -> {
                        // 视频页使用旧的 grid 布局和 SmallVideoCard
                        VideoDynamicContent(
                            videoList = dynamicViewModel.dynamicVideoList,
                            lazyGridState = lazyGridState,
                            onClickVideo = onClickVideo,
                            onLongClickVideo = onLongClickVideo,
                            onFocus = ::updateFocusedIndex,
                            isLoading = isLoading,
                            hasMore = hasMore
                        )
                    }

                    DynamicTabType.Pgc -> {
                        PgcDynamicContent(
                            filteredList = currentList,
                            gridState = pgcGridState,
                            onClickDynamicItem = onClickDynamicItem,
                            onLongClickDynamicItem = onLongClickDynamicItem,
                            onFocus = ::updateFocusedIndex,
                            isLoading = isLoading,
                            hasMore = hasMore
                        )
                    }

                    DynamicTabType.Article -> {
                        ArticleDynamicContent(
                            filteredList = currentList,
                            staggeredGridState = articleStaggeredGridState,
                            onClickDynamicItem = onClickDynamicItem,
                            onLongClickDynamicItem = onLongClickDynamicItem,
                            onFocus = ::updateFocusedIndex,
                            isLoading = isLoading,
                            hasMore = hasMore
                        )
                    }

                    DynamicTabType.All,
                    DynamicTabType.Up -> {
                        AllDynamicContent(
                            filteredList = currentList,
                            staggeredGridState = allStaggeredGridState,
                            onClickDynamicItem = onClickDynamicItem,
                            onLongClickDynamicItem = onLongClickDynamicItem,
                            onFocus = ::updateFocusedIndex,
                            isLoading = isLoading,
                            hasMore = hasMore
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "请先登录")
        }
    }

    if (chargingQrContent.isNotBlank()) {
        DynamicChargingQrDialog(
            qrContent = chargingQrContent,
            onDismissRequest = { chargingQrContent = "" }
        )
    }
}

@Composable
private fun DynamicTabRow(
    modifier: Modifier = Modifier,
    tabs: List<DynamicTabType>,
    tabRowFocusRequester: FocusRequester,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClick: (Int) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    onSelectedTabReady: (Int) -> Unit = {},
    onLeftKeyEvent: () -> Unit = {},
    onRightKeyEvent: () -> Unit = {}
) {
    val context = LocalContext.current
    val lastReportedReadyIndex = remember { intArrayOf(-1) }

    TabRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp)
            .focusRestorer(tabRowFocusRequester)
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    // 在第一个子 tab 按左键，跳转到父级上一个 tab
                    if (it.key == Key.DirectionLeft && selectedTabIndex == 0) {
                        onLeftKeyEvent()
                        return@onPreviewKeyEvent true
                    }
                    // 在最后一个子 tab 按右键，跳转到父级下一个 tab
                    if (it.key == Key.DirectionRight && selectedTabIndex == tabs.lastIndex) {
                        onRightKeyEvent()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
        selectedTabIndex = selectedTabIndex
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                modifier = if (index == selectedTabIndex) {
                    Modifier
                        .focusRequester(tabRowFocusRequester)
                        .onGloballyPositioned {
                            if (lastReportedReadyIndex[0] != index) {
                                lastReportedReadyIndex[0] = index
                                onSelectedTabReady(index)
                            }
                        }
                } else {
                    Modifier
                },
                selected = index == selectedTabIndex,
                onFocus = { onTabSelected(index) },
                onClick = { onTabClick(index) }
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    text = tab.getDisplayName(context),
                    color = LocalContentColor.current,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// 视频页内容 - 使用旧的 Grid 布局和 SmallVideoCard
@Composable
private fun VideoDynamicContent(
    videoList: List<DynamicVideo>,
    lazyGridState: LazyGridState,
    onClickVideo: (DynamicVideo) -> Unit,
    onLongClickVideo: (DynamicVideo) -> Unit,
    onFocus: (Int) -> Unit,
    isLoading: Boolean,
    hasMore: Boolean
) {
    val context = LocalContext.current
    val padding = dimensionResource(tvR.dimen.grid_padding) / 2
    val spacedBy = dimensionResource(tvR.dimen.grid_spacedBy) / 2
    val gridColumns = Prefs.gridColumns

    if (videoList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                LoadingTip()
            } else {
                ContentStatusCard(text = stringResource(R.string.no_data))
            }
        }
        return
    }

    ProvideListBringIntoViewSpec {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    if (!it.hasFocus) {
                        onFocus(-1)
                    }
                }
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp && it.key == Key.Menu) {
                        context.startActivity(Intent(context, FollowActivity::class.java))
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            state = lazyGridState,
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(padding),
            verticalArrangement = Arrangement.spacedBy(spacedBy),
            horizontalArrangement = Arrangement.spacedBy(spacedBy)
        ) {
            itemsIndexed(
                items = videoList,
                key = { index, video -> "${video.aid}#$index" },
                contentType = { _, _ -> "dynamic_video_card" }
            ) { index, video ->
                SmallVideoCard(
                    data = remember(video.aid) {
                        VideoCardData(
                            avid = video.aid,
                            title = video.title,
                            cover = video.cover,
                            play = video.play,
                            danmaku = video.danmaku,
                            upName = video.author,
                            time = video.duration * 1000L,
                            pubTime = video.pubTime,
                            isChargingArc = video.isChargingArc,
                            badgeText = video.chargingArcBadge
                        )
                    },
                    onClick = { onClickVideo(video) },
                    onLongClick = { onLongClickVideo(video) },
                    onFocus = { onFocus(index) }
                )
            }

            if (isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingTip()
                    }
                }
            }

            if (!hasMore && videoList.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        text = "没有更多了捏",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// 全部页内容 - 保留瀑布流，但放大卡片信息密度和焦点态
@Composable
private fun AllDynamicContent(
    filteredList: List<DynamicItem>,
    staggeredGridState: LazyStaggeredGridState,
    onClickDynamicItem: (DynamicItem) -> Unit,
    onLongClickDynamicItem: (DynamicItem) -> Unit,
    onFocus: (Int) -> Unit,
    isLoading: Boolean,
    hasMore: Boolean
) {
    val context = LocalContext.current

    if (filteredList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                LoadingTip()
            } else {
                ContentStatusCard(text = stringResource(R.string.no_data))
            }
        }
        return
    }

    ProvideListBringIntoViewSpec {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    if (!it.hasFocus) {
                        onFocus(-1)
                    }
                }
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp && it.key == Key.Menu) {
                        context.startActivity(Intent(context, FollowActivity::class.java))
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            columns = StaggeredGridCells.Fixed(2),
            state = staggeredGridState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = filteredList,
                key = { index, item -> item.id ?: "dyn-all-$index-${item.commentId}" },
                contentType = { _, _ -> "dynamic_all_card" }
            ) { index, item ->
                AllDynamicCard(
                    dynamicItem = item,
                    onClick = { onClickDynamicItem(item) },
                    onLongClick = { onLongClickDynamicItem(item) },
                    onFocus = { onFocus(index) }
                )
            }

            if (isLoading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingTip()
                    }
                }
            }

            if (!hasMore && filteredList.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        text = "没有更多了捏",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PgcDynamicContent(
    filteredList: List<DynamicItem>,
    gridState: LazyGridState,
    onClickDynamicItem: (DynamicItem) -> Unit,
    onLongClickDynamicItem: (DynamicItem) -> Unit,
    onFocus: (Int) -> Unit,
    isLoading: Boolean,
    hasMore: Boolean
) {
    val context = LocalContext.current

    if (filteredList.isEmpty()) {
        DynamicEmptyContent(isLoading = isLoading)
        return
    }

    ProvideListBringIntoViewSpec {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    if (!it.hasFocus) {
                        onFocus(-1)
                    }
                }
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp && it.key == Key.Menu) {
                        context.startActivity(Intent(context, FollowActivity::class.java))
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = filteredList,
                key = { index, item -> item.id ?: "dyn-pgc-$index-${item.commentId}" },
                contentType = { _, _ -> "dynamic_pgc_card" }
            ) { index, item ->
                PgcDynamicCard(
                    dynamicItem = item,
                    onClick = { onClickDynamicItem(item) },
                    onLongClick = { onLongClickDynamicItem(item) },
                    onFocus = { onFocus(index) }
                )
            }

            if (isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DynamicLoadingLine()
                }
            }

            if (!hasMore && filteredList.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DynamicEndLine()
                }
            }
        }
    }
}

@Composable
private fun ArticleDynamicContent(
    filteredList: List<DynamicItem>,
    staggeredGridState: LazyStaggeredGridState,
    onClickDynamicItem: (DynamicItem) -> Unit,
    onLongClickDynamicItem: (DynamicItem) -> Unit,
    onFocus: (Int) -> Unit,
    isLoading: Boolean,
    hasMore: Boolean
) {
    val context = LocalContext.current

    if (filteredList.isEmpty()) {
        DynamicEmptyContent(isLoading = isLoading)
        return
    }

    ProvideListBringIntoViewSpec {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    if (!it.hasFocus) {
                        onFocus(-1)
                    }
                }
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp && it.key == Key.Menu) {
                        context.startActivity(Intent(context, FollowActivity::class.java))
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            columns = StaggeredGridCells.Fixed(2),
            state = staggeredGridState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = filteredList,
                key = { index, item -> item.id ?: "dyn-article-$index-${item.commentId}" },
                contentType = { _, _ -> "dynamic_article_card" }
            ) { index, item ->
                ArticleListCard(
                    dynamicItem = item,
                    onClick = { onClickDynamicItem(item) },
                    onLongClick = { onLongClickDynamicItem(item) },
                    onFocus = { onFocus(index) }
                )
            }

            if (isLoading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    DynamicLoadingLine()
                }
            }

            if (!hasMore && filteredList.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    DynamicEndLine()
                }
            }
        }
    }
}

@Composable
private fun AllDynamicCard(
    modifier: Modifier = Modifier,
    dynamicItem: DynamicItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocus: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus() },
        onClick = onClick,
        onLongClick = onLongClick,
        shape = CardDefaults.shape(DynamicCardShape),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        scale = CardDefaults.scale(focusedScale = 1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 作者信息头部
            DynamicCardHeader(author = dynamicItem.author)

            // 根据类型显示不同内容
            if (dynamicItem.isChargingBlocked()) {
                DynamicChargingBlockedContent(blocked = dynamicItem.blocked)
            } else when (dynamicItem.type) {
                DynamicType.Av -> DynamicVideoContent(video = dynamicItem.video)
                DynamicType.UgcSeason -> DynamicUgcSeasonContent(ugcSeason = dynamicItem.ugcSeason)
                DynamicType.Pgc -> DynamicPgcContent(
                    pgc = dynamicItem.pgc,
                    author = dynamicItem.author
                )
                DynamicType.Draw -> DynamicDrawContent(draw = dynamicItem.draw)
                DynamicType.Word -> DynamicWordContent(word = dynamicItem.word)
                DynamicType.Article -> DynamicArticleContent(article = dynamicItem.article)
                DynamicType.Forward -> DynamicForwardContent(
                    word = dynamicItem.word,
                    orig = dynamicItem.orig
                )
                else -> DynamicUnknownContent(type = dynamicItem.type)
            }

            DynamicFooterInfo(footer = dynamicItem.footer)
        }
    }
}

@Composable
private fun PgcDynamicCard(
    modifier: Modifier = Modifier,
    dynamicItem: DynamicItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocus: () -> Unit
) {
    val pgc = dynamicItem.pgc
    if (pgc == null) {
        AllDynamicCard(
            modifier = modifier,
            dynamicItem = dynamicItem,
            onClick = onClick,
            onLongClick = onLongClick,
            onFocus = onFocus
        )
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 156.dp)
            .onFocusChanged { if (it.isFocused) onFocus() },
        onClick = onClick,
        onLongClick = onLongClick,
        shape = CardDefaults.shape(DynamicCardShape),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        scale = CardDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 156.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(277.dp)
                    .aspectRatio(16f / 9f)
            ) {
                DynamicMediaImage(
                    url = pgc.cover,
                    modifier = Modifier.fillMaxSize()
                )
                DynamicBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    text = "番剧"
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = pgc.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(17.dp)
                    )
                    Text(
                        text = dynamicItem.author.pubTime.ifBlank { dynamicItem.author.pubAction.ifBlank { "更新" } },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleListCard(
    modifier: Modifier = Modifier,
    dynamicItem: DynamicItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocus: () -> Unit
) {
    val article = dynamicItem.article
    val isChargingBlocked = dynamicItem.isChargingBlocked()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus() },
        onClick = onClick,
        onLongClick = onLongClick,
        shape = CardDefaults.shape(DynamicCardShape),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        scale = CardDefaults.scale(focusedScale = 1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isChargingBlocked) {
                DynamicChargingBlockedContent(
                    blocked = dynamicItem.blocked,
                    compact = true
                )
            } else {
                article?.covers?.firstOrNull()?.let {
                    DynamicMediaImage(
                        url = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2.08f)
                    )
                }
                Text(
                    text = article?.title?.takeIf(String::isNotBlank) ?: "专栏动态",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!article?.text.isNullOrBlank()) {
                    Text(
                        text = article.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DynamicBadge(
                    text = if (isChargingBlocked) {
                        "充电专属"
                    } else {
                        article?.label?.ifBlank { "专栏" } ?: "专栏"
                    }
                )
                Text(
                    text = dynamicItem.author.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dynamicItem.author.pubTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DynamicCardHeader(
    author: DynamicItem.DynamicAuthorModule
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AvatarImage(url = author.avatar)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = author.author,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(author.pubTime, author.pubAction)
                    .filter(String::isNotBlank)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DynamicVideoContent(video: DynamicItem.DynamicVideoModule?) {
    video ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        DynamicVideoCover(
            video = video,
            modifier = Modifier
                .width(204.dp)
                .aspectRatio(16f / 9f)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 114.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (video.text.isNotBlank()) {
                Text(
                    text = video.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DynamicVideoCover(
    video: DynamicItem.DynamicVideoModule,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(DynamicCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        AsyncImage(
            model = rememberDynamicListImageModel(video.cover),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )
        if (video.isChargingArc) {
            DynamicBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                text = if (video.chargingArcBadge.isNotBlank()) video.chargingArcBadge else "充电专属"
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = video.duration,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (video.play.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_count),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = video.play,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (video.danmaku.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_danmaku_count),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = video.danmaku,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicUgcSeasonContent(ugcSeason: DynamicItem.DynamicUgcSeasonModule?) {
    ugcSeason ?: return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = ugcSeason.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        DynamicMediaImage(
            url = ugcSeason.cover,
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
        )
        if (ugcSeason.desc.isNotBlank()) {
            Text(
                text = ugcSeason.desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DynamicPgcContent(
    pgc: DynamicItem.DynamicPgcModule?,
    author: DynamicItem.DynamicAuthorModule
) {
    pgc ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(204.dp)
                .aspectRatio(16f / 9f)
        ) {
            DynamicMediaImage(
                url = pgc.cover,
                modifier = Modifier.fillMaxSize(),
            )
            DynamicBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                text = "番剧"
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 114.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = pgc.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            DynamicMetaRow(text = author.pubTime.ifBlank { author.pubAction.ifBlank { "更新" } })
        }
    }
}

@Composable
private fun DynamicDrawContent(draw: DynamicItem.DynamicDrawModule?) {
    draw ?: return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 标题
        val title = draw.title
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (draw.images.isNotEmpty()) {
            DynamicImageStrip(images = draw.images)
        }
        // 文字
        if (draw.text.isNotBlank()) {
            Text(
                text = draw.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DynamicWordContent(word: DynamicItem.DynamicWordModule?) {
    word ?: return
    if (word.text.isNotBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.52f),
                    shape = DynamicCardShape
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DynamicChargingBlockedContent(
    blocked: DynamicItem.DynamicBlockedModule?,
    compact: Boolean = false
) {
    val title = blocked?.title
        ?.takeIf(String::isNotBlank)
        ?: "充电专属内容"
    val hint = blocked?.hintMessage
        ?.takeIf(String::isNotBlank)
        ?: "点击后使用哔哩哔哩手机客户端扫码充电"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 112.dp else 132.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                shape = DynamicCardShape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DynamicBadge(text = "充电专属")
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DynamicArticleContent(article: DynamicItem.DynamicArticleModule?) {
    article ?: return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (article.covers.isNotEmpty()) {
            DynamicMediaImage(
                url = article.covers.first(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            )
        }
        if (article.text.isNotBlank()) {
            Text(
                text = article.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        DynamicBadge(text = article.label.ifBlank { "专栏" })
    }
}

@Composable
private fun DynamicForwardContent(
    word: DynamicItem.DynamicWordModule?,
    orig: DynamicItem?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 转发文字
        if (word != null && word.text.isNotBlank()) {
            Text(
                text = "转发：${word.text}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        // 原动态
        if (orig != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 原作者
                    if (orig.author.mid > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AsyncImage(
                                model = rememberDynamicListImageModel(orig.author.avatar),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = orig.author.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                    // 原内容
                    when (orig.type) {
                        DynamicType.Av -> orig.video?.let {
                            DynamicVideoContent(video = it)
                        }
                        DynamicType.UgcSeason -> orig.ugcSeason?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DynamicType.Word -> orig.word?.let {
                            Text(
                                text = it.text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DynamicType.Draw -> orig.draw?.let {
                            val summaryText = it.text.ifBlank {
                                it.title?.takeIf(String::isNotBlank) ?: "原动态"
                            }
                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DynamicType.Article -> orig.article?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> Text(
                            text = "原动态",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicUnknownContent(type: DynamicType) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂不支持的动态类型: ${type.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun DynamicEmptyContent(isLoading: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            LoadingTip()
        } else {
            ContentStatusCard(text = stringResource(R.string.no_data))
        }
    }
}

@Composable
private fun DynamicLoadingLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingTip()
    }
}

@Composable
private fun DynamicEndLine() {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        text = "没有更多了捏",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AvatarImage(
    url: String,
    modifier: Modifier = Modifier.size(36.dp)
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
    ) {
        AsyncImage(
            model = rememberDynamicListImageModel(url),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun DynamicMediaImage(
    url: String,
    modifier: Modifier = Modifier,
    imageSize: ImageSize = ImageSize.DynamicPreview,
    alignment: Alignment = Alignment.Center,
    sourceWidth: Int = 0,
    sourceHeight: Int = 0
) {
    TvSafeDynamicImage(
        url = url,
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        modifier = modifier,
        useCase = TvDynamicImageUseCase.ListPreview,
        imageSize = imageSize,
        alignment = alignment,
        contentScale = ContentScale.Crop,
        shape = DynamicCardShape,
        emptyText = "暂无封面"
    )
}

@Composable
private fun DynamicImageStrip(images: List<Picture>) {
    val visibleImages = images.take(2)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        visibleImages.forEach { image ->
            val isLongImage = image.width > 0 && image.height > image.width * 3
            DynamicMediaImage(
                url = image.url,
                sourceWidth = image.width,
                sourceHeight = image.height,
                modifier = Modifier
                    .weight(1f)
                    .height(126.dp),
                alignment = if (isLongImage) Alignment.TopCenter else Alignment.Center
            )
        }
    }
    if (images.size > visibleImages.size) {
        Text(
            text = "+${images.size - visibleImages.size} 张图片",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DynamicBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = DynamicAccent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(5.dp)
            )
            .border(
                width = 1.dp,
                color = DynamicAccent.copy(alpha = 0.85f),
                shape = RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = DynamicAccent,
            maxLines = 1
        )
    }
}

@Composable
private fun DynamicMetaRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DynamicFooterInfo(
    footer: DynamicItem.DynamicFooterModule?,
    prominent: Boolean = false
) {
    footer ?: return

    val stats = listOf(
        Icons.Rounded.ChatBubbleOutline to footer.comment,
        Icons.Rounded.ThumbUp to footer.like,
        Icons.Rounded.FavoriteBorder to footer.share
    ).filter { it.second > 0 }

    if (stats.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (prominent) 20.dp else 14.dp)
    ) {
        stats.forEach { (icon, count) ->
            DynamicStatItem(
                icon = icon,
                text = formatDynamicCount(count),
                prominent = prominent
            )
        }
    }
}

@Composable
private fun DynamicStatItem(
    icon: ImageVector,
    text: String,
    prominent: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (prominent) 0.76f else 0.52f),
            modifier = Modifier.size(if (prominent) 21.dp else 17.dp)
        )
        Text(
            text = text,
            style = if (prominent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (prominent) 0.76f else 0.56f),
            maxLines = 1
        )
    }
}

@Composable
private fun DynamicChargingQrDialog(
    qrContent: String,
    onDismissRequest: () -> Unit
) {
    TvAlertDialog(
        modifier = Modifier.widthIn(min = 760.dp, max = 860.dp),
        text = {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QrImage(
                    modifier = Modifier.size(220.dp),
                    content = qrContent,
                    borderWidth = 20.dp,
                    showLoadingWhenContentChanged = false
                )
                Column(
                    modifier = Modifier.widthIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "手机扫码前往充电页面",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "请使用哔哩哔哩手机客户端扫码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = "关闭")
            }
        },
        containerColor = Color(0xFF111111).copy(alpha = 0.96f),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

private fun DynamicItem.isChargingBlocked(): Boolean {
    val blocked = blocked ?: return false
    return listOf(blocked.title, blocked.hintMessage, blocked.button?.text.orEmpty())
        .any { it.contains("充电") }
}

private fun DynamicItem.chargingQrContent(): String =
    author.mid
        .takeIf { it > 0L }
        ?.let { "$DynamicChargingUrlPrefix$it" }
        .orEmpty()

private fun formatDynamicCount(count: Int): String = when {
    count >= 10000 -> {
        val value = count / 10000f
        if (count >= 100000) "${count / 10000}万" else String.format("%.1f万", value)
    }
    else -> count.toString()
}
