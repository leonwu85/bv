package dev.aaa1115910.bv.tv.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.aaa1115910.bv.entity.DynamicPageStyle
import dev.aaa1115910.bv.entity.DynamicTabType
import dev.aaa1115910.bv.tv.component.HomeTopNavItem
import dev.aaa1115910.bv.tv.component.TopNav
import dev.aaa1115910.bv.tv.screens.main.home.DynamicsScreen
import dev.aaa1115910.bv.tv.screens.main.home.NewDynamicsScreen
import dev.aaa1115910.bv.tv.screens.main.home.PopularScreen
import dev.aaa1115910.bv.tv.screens.main.home.RecommendScreen
import dev.aaa1115910.bv.tv.screens.user.FavoriteScreen
import dev.aaa1115910.bv.tv.screens.user.FollowingSeasonScreen
import dev.aaa1115910.bv.tv.screens.user.HistoryScreen
import dev.aaa1115910.bv.tv.screens.user.ToViewScreen
import dev.aaa1115910.bv.tv.util.KeepAlivePages
import dev.aaa1115910.bv.tv.util.LocalTvUiPerformanceProfile
import dev.aaa1115910.bv.tv.util.LocalTvPreloadCoordinator
import dev.aaa1115910.bv.tv.util.TOP_NAV_PRELOAD_STEP
import dev.aaa1115910.bv.tv.util.TvUiPerformanceTier
import dev.aaa1115910.bv.tv.util.boundedAdjacentNavItems
import dev.aaa1115910.bv.tv.util.homeNavItemsFlow
import dev.aaa1115910.bv.tv.util.parseHomeNavItemsOrder
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.scrollToItemIfAvailable
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import dev.aaa1115910.bv.viewmodel.user.FollowingSeasonViewModel
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private enum class HomeFocusLayer {
    TopNav,
    Content,
}

private data class DynamicAutoDrillRequest(
    val generation: Int,
    val targetSubTabIndex: Int,
)

private const val HOME_ADJACENT_PRELOAD_IDLE_MS = 500L

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HomeContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    selectedTabOrdinal: Int = Prefs.defaultHomeTab,
    onSelectedTabChanged: (Int) -> Unit = {},
    onRequestDrawerFocus: () -> Unit = {},
    recommendViewModel: RecommendViewModel = koinViewModel(),
    popularViewModel: PopularViewModel = koinViewModel(),
    dynamicViewModel: DynamicViewModel = koinViewModel(),
    favouriteViewModel: FavoriteViewModel = koinViewModel(),
    followingSeasonViewModel: FollowingSeasonViewModel = koinViewModel(),
    historyViewModel: HistoryViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("HomeContent")
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)
    val performanceProfile = LocalTvUiPerformanceProfile.current
    val preloadCoordinator = LocalTvPreloadCoordinator.current
    val enableFullPageAnimation =
        enableMainUiAnimation && performanceProfile.allowFullPageAnimation

    val homeVideoGridCacheWindow = remember(performanceProfile.tier) {
        val (aheadFraction, behindFraction) = when (performanceProfile.tier) {
            TvUiPerformanceTier.Conservative -> 0.35f to 0.10f
            TvUiPerformanceTier.Balanced -> 0.50f to 0.15f
            TvUiPerformanceTier.Standard -> 0.75f to 0.25f
        }
        LazyLayoutCacheWindow(
            aheadFraction = aheadFraction,
            behindFraction = behindFraction,
        )
    }
    val recommendState = rememberLazyGridState(cacheWindow = homeVideoGridCacheWindow)
    val popularState = rememberLazyGridState(cacheWindow = homeVideoGridCacheWindow)
    val dynamicState = rememberLazyGridState()
    val favoriteState = rememberLazyGridState()
    val followingSeasonState = rememberLazyGridState()
    val historyState = rememberLazyGridState()
    val toViewState = rememberLazyGridState()

    var focusLayer by remember { mutableStateOf<HomeFocusLayer?>(null) }
    val dynamicTabRowFocusRequester = remember { FocusRequester() }
    var topNavFocusSelectedToken by remember { mutableIntStateOf(0) }

    val dynamicSubTabs = remember {
        listOf(
            DynamicTabType.All,
            DynamicTabType.Video,
            DynamicTabType.Pgc,
            DynamicTabType.Article,
        )
    }

    // 记住动态页子 Tab 的选中索引，默认值从设置中读取
    var dynamicSubTabIndex by remember { mutableIntStateOf(Prefs.dynamicDefaultTab.ordinal) }

    fun selectedDynamicTabType(): DynamicTabType = dynamicSubTabs.getOrElse(dynamicSubTabIndex) {
        Prefs.dynamicDefaultTab.takeIf { it in dynamicSubTabs }
            ?: DynamicTabType.All
    }

    // 用于管理延迟加载的 Job（仅在快速连切时取消上一次）
    var loadJob by remember { mutableStateOf<Job?>(null) }

    // 根据设置获取过滤和排序后的导航项列表
    val homeNavItems by homeNavItemsFlow.collectAsState(
        initial = remember { parseHomeNavItemsOrder(Prefs.homeNavItemsOrder) }
    )

    // 处理空列表情况：如果所有导航项都被隐藏，强制显示推荐
    val effectiveNavItems = if (homeNavItems.isEmpty()) {
        listOf(HomeTopNavItem.Recommend)
    } else {
        homeNavItems
    }

    val selectedTab = HomeTopNavItem.entries
        .getOrElse(selectedTabOrdinal) {
            HomeTopNavItem.entries.getOrElse(Prefs.defaultHomeTab) { HomeTopNavItem.Recommend }
        }
        .takeIf { it in effectiveNavItems }
        ?: effectiveNavItems.first()
    var focusedTopNavItem by remember { mutableStateOf(selectedTab) }
    var dynamicAutoDrillGeneration by remember { mutableIntStateOf(0) }
    var pendingDynamicAutoDrill by remember { mutableStateOf<DynamicAutoDrillRequest?>(null) }
    var dynamicSubTabReadyIndex by remember { mutableIntStateOf(-1) }
    var suppressNextDynamicAutoDrill by remember { mutableStateOf(false) }

    fun requestDynamicSubTabFocus(targetIndex: Int) {
        val safeTargetIndex = targetIndex.coerceIn(0, dynamicSubTabs.lastIndex)
        dynamicSubTabIndex = safeTargetIndex
        dynamicAutoDrillGeneration++
        pendingDynamicAutoDrill = DynamicAutoDrillRequest(
            generation = dynamicAutoDrillGeneration,
            targetSubTabIndex = safeTargetIndex,
        )
    }

    fun lazyGridStateFor(tab: HomeTopNavItem): LazyGridState = when (tab) {
        HomeTopNavItem.Recommend -> recommendState
        HomeTopNavItem.Popular -> popularState
        HomeTopNavItem.Dynamics -> dynamicState
        HomeTopNavItem.Favorite -> favoriteState
        HomeTopNavItem.FollowingSeason -> followingSeasonState
        HomeTopNavItem.History -> historyState
        HomeTopNavItem.ToView -> toViewState
    }

    fun scrollToTabTop(tab: HomeTopNavItem) {
        scope.launch {
            lazyGridStateFor(tab).scrollToItemIfAvailable(0)
        }
    }

    suspend fun awaitListIdle(tab: HomeTopNavItem) {
        val state = lazyGridStateFor(tab)
        while (true) {
            snapshotFlow { state.isScrollInProgress }.first { !it }
            delay(HOME_ADJACENT_PRELOAD_IDLE_MS)
            if (!state.isScrollInProgress) {
                return
            }
        }
    }

    suspend fun initDataFor(tab: HomeTopNavItem) {
        when (tab) {
            HomeTopNavItem.Recommend -> {
                if (recommendViewModel.recommendVideoList.isEmpty()) {
                    recommendViewModel.loadMore()
                }
            }

            HomeTopNavItem.Popular -> {
                if (popularViewModel.popularVideoList.isEmpty()) {
                    popularViewModel.loadMore()
                }
            }

            HomeTopNavItem.Dynamics -> {
                if (!userViewModel.isLogin) return
                if (Prefs.dynamicPageStyle == DynamicPageStyle.New) {
                    val selectedType = selectedDynamicTabType()
                    // Home 只保证当前子页；相邻子页由 NewDynamicsScreen 在列表空闲后预加载。
                    dynamicViewModel.ensureFirstPage(selectedType)
                } else {
                    dynamicViewModel.ensureFirstPage(DynamicTabType.Video)
                }
            }

            HomeTopNavItem.Favorite -> {
                if (userViewModel.isLogin &&
                    favouriteViewModel.favoriteFolderMetadataList.isEmpty() &&
                    !favouriteViewModel.updatingFolders
                ) {
                    favouriteViewModel.updateFoldersInfo()
                }
            }

            HomeTopNavItem.FollowingSeason -> {
                if (userViewModel.isLogin &&
                    followingSeasonViewModel.followingSeasons.isEmpty() &&
                    !followingSeasonViewModel.updating
                ) {
                    followingSeasonViewModel.loadMore()
                }
            }

            HomeTopNavItem.History -> {
                if (userViewModel.isLogin &&
                    historyViewModel.histories.isEmpty() &&
                    !historyViewModel.updating
                ) {
                    historyViewModel.update()
                }
            }

            HomeTopNavItem.ToView -> {
                if (userViewModel.isLogin &&
                    toViewViewModel.histories.isEmpty() &&
                    !toViewViewModel.updating
                ) {
                    toViewViewModel.update()
                }
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab.ordinal != selectedTabOrdinal) {
            onSelectedTabChanged(selectedTab.ordinal)
        }
    }

    // 当前页优先，再按设备预算预取最多一个相邻页。
    LaunchedEffect(selectedTab, effectiveNavItems, userViewModel.isLogin) {
        loadJob?.cancel()
        val targets = boundedAdjacentNavItems(
            items = effectiveNavItems,
            current = selectedTab,
            step = TOP_NAV_PRELOAD_STEP,
            maxItems = performanceProfile.maxKeepPages,
        )
        loadJob = scope.launch(Dispatchers.IO) {
            // 当前页优先
            initDataFor(selectedTab)
            awaitListIdle(selectedTab)
            preloadCoordinator.runExclusive {
                targets.filter { it != selectedTab }.forEach { tab ->
                    initDataFor(tab)
                }
            }
        }
    }

    LaunchedEffect(selectedTab, focusLayer) {
        if (focusLayer != HomeFocusLayer.TopNav) {
            focusedTopNavItem = selectedTab
        }
    }

    val currentListOnTop by remember(selectedTab) {
        derivedStateOf {
            with(lazyGridStateFor(selectedTab)) {
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
    }

    LaunchedEffect(
        selectedTab,
        focusedTopNavItem,
        focusLayer,
        userViewModel.isLogin,
        pendingDynamicAutoDrill,
        dynamicSubTabReadyIndex,
    ) {
        val request = pendingDynamicAutoDrill ?: return@LaunchedEffect
        val shouldFocusDynamicSubTab =
            userViewModel.isLogin &&
                    Prefs.dynamicPageStyle == DynamicPageStyle.New &&
                    selectedTab == HomeTopNavItem.Dynamics &&
                    focusedTopNavItem == HomeTopNavItem.Dynamics &&
                    focusLayer == HomeFocusLayer.TopNav &&
                    dynamicSubTabReadyIndex == request.targetSubTabIndex

        if (!shouldFocusDynamicSubTab) return@LaunchedEffect

        withFrameNanos { }
        if (
            pendingDynamicAutoDrill?.generation == request.generation &&
            selectedTab == HomeTopNavItem.Dynamics &&
            focusedTopNavItem == HomeTopNavItem.Dynamics &&
            focusLayer == HomeFocusLayer.TopNav &&
            dynamicSubTabReadyIndex == request.targetSubTabIndex
        ) {
            pendingDynamicAutoDrill = null
            dynamicTabRowFocusRequester.requestFocus()
        }
    }

    BackHandler(focusLayer != null) {
        if (focusLayer == HomeFocusLayer.TopNav) {
            onRequestDrawerFocus()
            return@BackHandler
        }
        navFocusRequester.requestFocus(scope)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopNav(
                modifier = Modifier
                    .focusRequester(navFocusRequester)
                    .padding(end = 80.dp)
                    .onFocusChanged {
                        if (it.hasFocus) {
                            focusLayer = HomeFocusLayer.TopNav
                        } else if (focusLayer == HomeFocusLayer.TopNav) {
                            focusLayer = null
                        }
                    },
                items = effectiveNavItems,
                isLargePadding = focusLayer != HomeFocusLayer.Content && currentListOnTop,
                initialSelectedItem = selectedTab,
                focusSelectedToken = topNavFocusSelectedToken,
                onFocusedChanged = { nav ->
                    val homeNav = nav as HomeTopNavItem
                    val previousTopNavItem = focusedTopNavItem
                    focusedTopNavItem = homeNav

                    if (homeNav != HomeTopNavItem.Dynamics) {
                        pendingDynamicAutoDrill = null
                        suppressNextDynamicAutoDrill = false
                    } else if (suppressNextDynamicAutoDrill) {
                        pendingDynamicAutoDrill = null
                        suppressNextDynamicAutoDrill = false
                    } else {
                        val previousIndex = effectiveNavItems.indexOf(previousTopNavItem)
                        val dynamicsIndex = effectiveNavItems.indexOf(HomeTopNavItem.Dynamics)
                        val enteredFromAdjacentTopNav =
                            previousTopNavItem != HomeTopNavItem.Dynamics &&
                                    previousIndex >= 0 &&
                                    dynamicsIndex >= 0 &&
                                    (previousIndex == dynamicsIndex - 1 ||
                                            previousIndex == dynamicsIndex + 1)

                        if (enteredFromAdjacentTopNav) {
                            val targetSubTabIndex = if (previousIndex < dynamicsIndex) {
                                0
                            } else {
                                dynamicSubTabs.lastIndex
                            }
                            requestDynamicSubTabFocus(targetSubTabIndex)
                        }
                    }
                },
                onSelectedChanged = { nav ->
                    loadJob?.cancel()
                    val nextTab = nav as HomeTopNavItem
                    onSelectedTabChanged(nextTab.ordinal)
                    if (nextTab != HomeTopNavItem.Dynamics) {
                        pendingDynamicAutoDrill = null
                    }
                },
                onClick = { nav ->
                    loadJob?.cancel()
                    val homeNav = nav as HomeTopNavItem
                    scrollToTabTop(homeNav)

                    when (homeNav) {
                        HomeTopNavItem.Recommend -> {
                            logger.fInfo { "clear recommend data" }
                            recommendViewModel.clearData()
                            logger.fInfo { "reload recommend data" }
                            scope.launch(Dispatchers.IO) { recommendViewModel.loadMore() }
                        }

                        HomeTopNavItem.Popular -> {
                            logger.fInfo { "clear popular data" }
                            popularViewModel.clearData()
                            logger.fInfo { "reload popular data" }
                            scope.launch(Dispatchers.IO) { popularViewModel.loadMore() }
                        }

                        HomeTopNavItem.Dynamics -> {
                            val selectedType =
                                if (Prefs.dynamicPageStyle == DynamicPageStyle.New) {
                                    selectedDynamicTabType()
                                } else {
                                    DynamicTabType.Video
                                }
                            logger.fInfo { "clear dynamic data [$selectedType]" }
                            dynamicViewModel.refreshByType(selectedType)
                            logger.fInfo { "reload dynamic data [$selectedType]" }
                            scope.launch(Dispatchers.IO) { dynamicViewModel.loadMoreByType(selectedType) }
                        }

                        HomeTopNavItem.Favorite -> {
                            if (userViewModel.isLogin) {
                                favouriteViewModel.clearData()
                                favouriteViewModel.updateFoldersInfo()
                            }
                        }

                        HomeTopNavItem.FollowingSeason -> {
                            if (userViewModel.isLogin) {
                                followingSeasonViewModel.clearData()
                                followingSeasonViewModel.loadMore()
                            }
                        }

                        HomeTopNavItem.History -> {
                            if (userViewModel.isLogin) {
                                historyViewModel.clearData()
                                historyViewModel.update()
                            }
                        }

                        HomeTopNavItem.ToView -> {
                            if (userViewModel.isLogin) {
                                toViewViewModel.clearData()
                                toViewViewModel.update()
                            }
                        }
                    }
                },
                onLeftKeyEvent = {
                    // 顶部栏最左侧按左键时，跳转到左侧导航栏
                    onRequestDrawerFocus()
                },
                onPendingDownKeyEvent = {
                    val canEnterDynamicSubTabs =
                        userViewModel.isLogin &&
                                Prefs.dynamicPageStyle == DynamicPageStyle.New &&
                                focusedTopNavItem == HomeTopNavItem.Dynamics
                    if (canEnterDynamicSubTabs) {
                        suppressNextDynamicAutoDrill = false
                        requestDynamicSubTabFocus(dynamicSubTabIndex)
                    }
                    canEnterDynamicSubTabs
                },
                onDownKeyEvent = {
                    val canEnterDynamicSubTabs =
                        userViewModel.isLogin &&
                                Prefs.dynamicPageStyle == DynamicPageStyle.New &&
                                selectedTab == HomeTopNavItem.Dynamics
                    if (canEnterDynamicSubTabs) {
                        suppressNextDynamicAutoDrill = false
                        requestDynamicSubTabFocus(dynamicSubTabIndex)
                    }
                    canEnterDynamicSubTabs
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .onFocusChanged {
                    if (it.hasFocus) {
                        focusLayer = HomeFocusLayer.Content
                    } else if (focusLayer == HomeFocusLayer.Content) {
                        focusLayer = null
                    }
                }
        ) {
            KeepAlivePages(
                current = selectedTab,
                maxKeep = performanceProfile.maxKeepPages,
                enableAnimation = enableFullPageAnimation,
                orderedItems = effectiveNavItems,
                preloadStep = TOP_NAV_PRELOAD_STEP,
            ) { screen, _ ->
                when (screen) {
                    HomeTopNavItem.Recommend -> RecommendScreen(lazyGridState = recommendState)
                    HomeTopNavItem.Popular -> PopularScreen(lazyGridState = popularState)
                    HomeTopNavItem.Dynamics -> {
                        if (userViewModel.isLogin) {
                            if (Prefs.dynamicPageStyle == DynamicPageStyle.New) {
                                NewDynamicsScreen(
                                    tabRowFocusRequester = dynamicTabRowFocusRequester,
                                    lazyGridState = dynamicState,
                                    initialSelectedTabIndex = dynamicSubTabIndex,
                                    onSelectedTabChanged = { dynamicSubTabIndex = it },
                                    onSubTabRowReady = { index ->
                                        dynamicSubTabReadyIndex = index
                                    },
                                    onSubTabRowUnavailable = {
                                        dynamicSubTabReadyIndex = -1
                                    },
                                    onBackToParentTabRow = {
                                        pendingDynamicAutoDrill = null
                                        suppressNextDynamicAutoDrill = true
                                        navFocusRequester.requestFocus(scope)
                                    },
                                    onLeftKeyEvent = {
                                        val currentIndex = effectiveNavItems.indexOf(selectedTab)
                                        if (currentIndex > 0) {
                                            val targetTab = effectiveNavItems[currentIndex - 1]
                                            pendingDynamicAutoDrill = null
                                            onSelectedTabChanged(targetTab.ordinal)
                                            scope.launch {
                                                withFrameNanos { }
                                                topNavFocusSelectedToken++
                                            }
                                        }
                                    },
                                    onRightKeyEvent = {
                                        val currentIndex = effectiveNavItems.indexOf(selectedTab)
                                        if (currentIndex < effectiveNavItems.lastIndex) {
                                            val targetTab = effectiveNavItems[currentIndex + 1]
                                            pendingDynamicAutoDrill = null
                                            onSelectedTabChanged(targetTab.ordinal)
                                            scope.launch {
                                                withFrameNanos { }
                                                topNavFocusSelectedToken++
                                            }
                                        }
                                    }
                                )
                            } else {
                                DynamicsScreen(lazyGridState = dynamicState)
                            }
                        } else {
                            LoginRequiredScreen()
                        }
                    }
                    HomeTopNavItem.Favorite -> {
                        if (userViewModel.isLogin) {
                            FavoriteScreen(
                                showPageTitle = false,
                                lazyGridState = favoriteState
                            )
                        } else {
                            LoginRequiredScreen()
                        }
                    }
                    HomeTopNavItem.FollowingSeason -> {
                        if (userViewModel.isLogin) {
                            FollowingSeasonScreen(
                                showPageTitle = false,
                                lazyGridState = followingSeasonState
                            )
                        } else {
                            LoginRequiredScreen()
                        }
                    }
                    HomeTopNavItem.History -> {
                        if (userViewModel.isLogin) {
                            HistoryScreen(
                                showPageTitle = false,
                                lazyGridState = historyState
                            )
                        } else {
                            LoginRequiredScreen()
                        }
                    }
                    HomeTopNavItem.ToView -> {
                        if (userViewModel.isLogin) {
                            ToViewScreen(
                                showPageTitle = false,
                                lazyGridState = toViewState,
                                onListEmpty = {
                                    navFocusRequester.requestFocus()
                                }
                            )
                        } else {
                            LoginRequiredScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginRequiredScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "请先登录")
    }
}
