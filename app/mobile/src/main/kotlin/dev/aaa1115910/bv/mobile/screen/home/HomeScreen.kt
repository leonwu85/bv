package dev.aaa1115910.bv.mobile.screen.home

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.MobileTabRow
import dev.aaa1115910.bv.mobile.screen.home.home.PopularPage
import dev.aaa1115910.bv.mobile.screen.home.home.RcmdPage
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import kotlin.Int

private const val HOME_FOREGROUND_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    rcmdGridState: LazyGridState,
    popularGridState: LazyGridState,
    popularViewModel: PopularViewModel = koinViewModel(),
    recommendViewModel: RecommendViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel(),
    windowSize: WindowWidthSizeClass,
    messageUnreadCount: Int = 0,
    onOpenSearch: () -> Unit,
    onOpenMine: () -> Unit,
    onOpenInbox: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pageState = rememberPagerState(
        initialPage = MobileHomeTab.Recommend.ordinal,
        pageCount = { MobileHomeTab.entries.size }
    )

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            if (windowSize == WindowWidthSizeClass.Compact) {
                HomeTopAppBar(
                    avatar = userViewModel.face,
                    showInbox = userViewModel.isLogin,
                    messageUnreadCount = messageUnreadCount,
                    onOpenSearch = onOpenSearch,
                    onOpenMine = onOpenMine,
                    onOpenInbox = onOpenInbox
                )
            }
        }
    ) { innerPadding ->
        HomeScreenContent(
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            pageState = pageState,
            selectedTabIndex = pageState.currentPage,
            windowSize = windowSize,
            rcmdGridState = rcmdGridState,
            popularGridState = popularGridState,
            onChangeTabIndex = { scope.launch { pageState.animateScrollToPage(it) } },
            popularViewModel = popularViewModel,
            recommendViewModel = recommendViewModel,
        )
    }
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    pageState: PagerState,
    selectedTabIndex: Int,
    windowSize: WindowWidthSizeClass,
    rcmdGridState: LazyGridState,
    popularGridState: LazyGridState,
    onChangeTabIndex: (Int) -> Unit,
    popularViewModel: PopularViewModel = koinViewModel(),
    recommendViewModel: RecommendViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastForegroundRefreshAt by rememberSaveable {
        mutableStateOf(SystemClock.elapsedRealtime())
    }

    fun refreshRecommend() {
        scope.launch(Dispatchers.IO) {
            recommendViewModel.resetPage()
            delay(300)
            recommendViewModel.loadMore {
                recommendViewModel.clearData()
            }
            recommendViewModel.refreshing = false
        }
    }

    fun refreshPopular() {
        scope.launch(Dispatchers.IO) {
            popularViewModel.resetPage()
            delay(300)
            popularViewModel.loadMore {
                popularViewModel.clearData()
            }
            popularViewModel.refreshing = false
        }
    }

    fun refreshCurrentTabIfReady() {
        when (MobileHomeTab.entries[selectedTabIndex]) {
            MobileHomeTab.Recommend -> {
                if (recommendViewModel.recommendVideoList.isNotEmpty() && !recommendViewModel.loading) {
                    refreshRecommend()
                }
            }

            MobileHomeTab.Popular -> {
                if (popularViewModel.popularVideoList.isNotEmpty() && !popularViewModel.loading) {
                    refreshPopular()
                }
            }

            else -> Unit
        }
    }

    LaunchedEffect(selectedTabIndex) {
        when (MobileHomeTab.entries[selectedTabIndex]) {
            MobileHomeTab.Recommend -> {
                if (recommendViewModel.recommendVideoList.isEmpty() && !recommendViewModel.loading) {
                    withContext(Dispatchers.IO) {
                        recommendViewModel.loadMore()
                        recommendViewModel.refreshing = false
                    }
                }
            }

            MobileHomeTab.Popular -> {
                if (popularViewModel.popularVideoList.isEmpty() && !popularViewModel.loading) {
                    withContext(Dispatchers.IO) {
                        popularViewModel.loadMore()
                        popularViewModel.refreshing = false
                    }
                }
            }

            else -> Unit
        }
    }

    DisposableEffect(lifecycleOwner, selectedTabIndex) {
        var leaveFromThisPage = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> leaveFromThisPage = true
                Lifecycle.Event.ON_RESUME -> {
                    if (leaveFromThisPage) {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastForegroundRefreshAt >= HOME_FOREGROUND_REFRESH_INTERVAL_MS) {
                            lastForegroundRefreshAt = now
                            refreshCurrentTabIfReady()
                        }
                    }
                    leaveFromThisPage = false
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .background(Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(top = 4.dp)
                .height(42.dp)
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            MobileTabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = selectedTabIndex,
                tabs = MobileHomeTab.entries.map { it.title },
                onTabSelected = onChangeTabIndex,
                containerColor = Color.Transparent,
                horizontalArrangement = Arrangement.Center,
                tabHeight = 42.dp
            )
        }

        Surface(
            color = Color.Transparent,
            shape = if (windowSize == WindowWidthSizeClass.Compact) RoundedCornerShape(0.dp) else MaterialTheme.shapes.medium,
        ) {
            HorizontalPager(
                modifier = Modifier,
                state = pageState,
            ) { page ->
                when (MobileHomeTab.entries[page]) {
                    MobileHomeTab.Live -> {
                        if (selectedTabIndex == page) {
                            ContentCenterScreen(
                                windowSize = windowSize,
                                showTopBar = false,
                                lockedSection = ContentCenterSection.Live
                            )
                        } else {
                            Box(Modifier.fillMaxSize())
                        }
                    }

                    MobileHomeTab.Recommend -> {
                        RcmdPage(
                            state = rcmdGridState,
                            windowSize = windowSize,
                            videos = recommendViewModel.recommendVideoList,
                            onClickVideo = { video ->
                                VideoPlayerActivity.actionStart(context = context, video = video)
                            },
                            loading = recommendViewModel.loading,
                            refreshing = recommendViewModel.refreshing,
                            enabled = selectedTabIndex == MobileHomeTab.Recommend.ordinal &&
                                recommendViewModel.recommendVideoList.isNotEmpty(),
                            onRefresh = {
                                lastForegroundRefreshAt = SystemClock.elapsedRealtime()
                                refreshRecommend()
                            },
                            loadMore = {
                                scope.launch(Dispatchers.IO) {
                                    recommendViewModel.loadMore()
                                    recommendViewModel.refreshing = false
                                }
                            }
                        )
                    }

                    MobileHomeTab.Popular -> {
                        PopularPage(
                            state = popularGridState,
                            windowSize = windowSize,
                            videos = popularViewModel.popularVideoList,
                            onClickVideo = { video ->
                                VideoPlayerActivity.actionStart(context = context, video = video)
                            },
                            loading = popularViewModel.loading,
                            refreshing = popularViewModel.refreshing,
                            enabled = selectedTabIndex == MobileHomeTab.Popular.ordinal &&
                                popularViewModel.popularVideoList.isNotEmpty(),
                            onRefresh = {
                                lastForegroundRefreshAt = SystemClock.elapsedRealtime()
                                refreshPopular()
                            },
                            loadMore = {
                                scope.launch(Dispatchers.IO) {
                                    popularViewModel.loadMore()
                                    popularViewModel.refreshing = false
                                }
                            }
                        )
                    }

                    MobileHomeTab.Zone -> {
                        if (selectedTabIndex == page) {
                            ContentCenterScreen(
                                windowSize = windowSize,
                                showTopBar = false,
                                lockedSection = ContentCenterSection.Ugc
                            )
                        } else {
                            Box(Modifier.fillMaxSize())
                        }
                    }

                    MobileHomeTab.Bangumi -> {
                        if (selectedTabIndex == page) {
                            ContentCenterScreen(
                                windowSize = windowSize,
                                showTopBar = false,
                                lockedSection = ContentCenterSection.Pgc,
                                initialPgcType = PgcType.Anime
                            )
                        } else {
                            Box(Modifier.fillMaxSize())
                        }
                    }

                    MobileHomeTab.Cinema -> {
                        if (selectedTabIndex == page) {
                            ContentCenterScreen(
                                windowSize = windowSize,
                                showTopBar = false,
                                lockedSection = ContentCenterSection.Pgc,
                                initialPgcType = PgcType.Movie
                            )
                        } else {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopAppBar(
    modifier: Modifier = Modifier,
    avatar: String,
    showInbox: Boolean,
    messageUnreadCount: Int,
    onOpenSearch: () -> Unit,
    onOpenMine: () -> Unit,
    onOpenInbox: () -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenSearch),
                color = MaterialTheme.colorScheme.surfaceBright,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                    Text(
                        text = "搜索视频、番剧、UP主",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
        },
        navigationIcon = {},
        actions = {
            if (showInbox) {
                IconButton(onClick = onOpenInbox) {
                    if (messageUnreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(text = if (messageUnreadCount > 99) "99+" else messageUnreadCount.toString())
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "消息")
                        }
                    } else {
                        Icon(Icons.Outlined.Notifications, contentDescription = "消息")
                    }
                }
            }
            IconButton(onClick = onOpenMine) {
                if (avatar.isBlank()) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Gray)
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .size(36.dp),
                            model = avatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        )
    )
}

private enum class MobileHomeTab(val title: String) {
    Live("直播"),
    Recommend("推荐"),
    Popular("热门"),
    Zone("分区"),
    Bangumi("番剧"),
    Cinema("影视")
}
