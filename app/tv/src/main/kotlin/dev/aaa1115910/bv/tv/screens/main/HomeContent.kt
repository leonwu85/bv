package dev.aaa1115910.bv.tv.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
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
import dev.aaa1115910.bv.tv.util.homeNavItemsFlow
import dev.aaa1115910.bv.tv.util.parseHomeNavItemsOrder
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.scrollToItemIfAvailable
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.entity.DynamicPageStyle
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private enum class HomeFocusLayer {
    TopNav,
    Content,
}

@Composable
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
    val navigationFocusDelay = if (enableMainUiAnimation) 100L else 0L

    val recommendState = rememberLazyGridState()
    val popularState = rememberLazyGridState()
    val dynamicState = rememberLazyGridState()
    val favoriteState = rememberLazyGridState()
    val followingSeasonState = rememberLazyGridState()
    val historyState = rememberLazyGridState()
    val toViewState = rememberLazyGridState()

    var focusLayer by remember { mutableStateOf<HomeFocusLayer?>(null) }
    val dynamicTabRowFocusRequester = remember { FocusRequester() }
    var topNavFocusSelectedToken by remember { mutableIntStateOf(0) }

    // 记住动态页子 Tab 的选中索引，默认值从设置中读取
    var dynamicSubTabIndex by remember { mutableIntStateOf(Prefs.dynamicDefaultTab.ordinal) }
    
    // 用于管理延迟加载的Job
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
    var pendingDynamicSubTabFocus by remember { mutableStateOf(false) }

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

    fun initData () {
        scope.launch {
            when (selectedTab) {
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
                    if (dynamicViewModel.dynamicVideoList.isEmpty()) {
                        dynamicViewModel.loadMoreVideo()
                    }
                }

                HomeTopNavItem.Favorite -> {
//                    if (favouriteViewModel.favorites.isEmpty() && userViewModel.isLogin) {
//                        favouriteViewModel.updateFoldersInfo()
//                    }
                }

                HomeTopNavItem.FollowingSeason -> {
//                    if (followingSeasonViewModel.followingSeasons.isEmpty() && userViewModel.isLogin) {
//                        followingSeasonViewModel.loadMore()
//                    }
                }

                HomeTopNavItem.History -> {
//                    if (historyViewModel.histories.isEmpty() && userViewModel.isLogin) {
//                        historyViewModel.update()
//                    }
                }

                HomeTopNavItem.ToView -> {
//                    if (toViewViewModel.histories.isEmpty() && userViewModel.isLogin) {
//                        toViewViewModel.update()
//                    }
                }
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab.ordinal != selectedTabOrdinal) {
            onSelectedTabChanged(selectedTab.ordinal)
        }

        // 取消之前的延迟加载
        loadJob?.cancel()

        // 开始新的延迟加载
        loadJob = scope.launch(Dispatchers.IO) {
            delay(300L)
            initData()
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

    LaunchedEffect(Unit) {
        initData()
    }

    LaunchedEffect(selectedTab, focusedTopNavItem, focusLayer, userViewModel.isLogin, pendingDynamicSubTabFocus) {
        val shouldFocusDynamicSubTab =
            userViewModel.isLogin &&
                    Prefs.dynamicPageStyle == DynamicPageStyle.New &&
                    selectedTab == HomeTopNavItem.Dynamics &&
                    focusedTopNavItem == HomeTopNavItem.Dynamics &&
                    focusLayer == HomeFocusLayer.TopNav &&
                    pendingDynamicSubTabFocus

        if (!shouldFocusDynamicSubTab) return@LaunchedEffect

        delay(navigationFocusDelay)
        if (
            selectedTab == HomeTopNavItem.Dynamics &&
            focusedTopNavItem == HomeTopNavItem.Dynamics &&
            focusLayer == HomeFocusLayer.TopNav &&
            pendingDynamicSubTabFocus
        ) {
            pendingDynamicSubTabFocus = false
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
                    focusedTopNavItem = homeNav
                    pendingDynamicSubTabFocus =
                        homeNav == HomeTopNavItem.Dynamics &&
                                selectedTab != HomeTopNavItem.Dynamics
                },
                onSelectedChanged = { nav ->
                    loadJob?.cancel()
                    val nextTab = nav as HomeTopNavItem
                    onSelectedTabChanged(nextTab.ordinal)
                    if (nextTab != HomeTopNavItem.Dynamics) {
                        pendingDynamicSubTabFocus = false
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
                            logger.fInfo { "clear dynamic data" }
                            dynamicViewModel.clearVideoData()
                            logger.fInfo { "reload dynamic data" }
                            scope.launch(Dispatchers.IO) { dynamicViewModel.loadMoreVideo() }
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
            AnimatedContent(
                targetState = selectedTab,
                label = "home animated content",
                transitionSpec = {
                    if (!enableMainUiAnimation) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val coefficient = 10
                        if (targetState.ordinal < initialState.ordinal) {
                            fadeIn() + slideInHorizontally { -it / coefficient } togetherWith
                                    fadeOut() + slideOutHorizontally { it / coefficient }
                        } else {
                            fadeIn() + slideInHorizontally { it / coefficient } togetherWith
                                    fadeOut() + slideOutHorizontally { -it / coefficient }
                        }
                    }
                }
            ) { screen ->
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
                                    onBackToParentTabRow = {
                                        navFocusRequester.requestFocus(scope)
                                    },
                                    onLeftKeyEvent = {
                                        // 在子 Tab 最左侧按左键时，跳转到父级上一个 Tab
                                        val currentIndex = effectiveNavItems.indexOf(selectedTab)
                                        if (currentIndex > 0) {
                                            val targetTab = effectiveNavItems[currentIndex - 1]
                                            pendingDynamicSubTabFocus = false
                                            onSelectedTabChanged(targetTab.ordinal)
                                            scope.launch {
                                                delay(navigationFocusDelay)
                                                topNavFocusSelectedToken++
                                            }
                                        }
                                    },
                                    onRightKeyEvent = {
                                        // 在子 Tab 最右侧按右键时，跳转到父级下一个 Tab
                                        val currentIndex = effectiveNavItems.indexOf(selectedTab)
                                        if (currentIndex < effectiveNavItems.lastIndex) {
                                            val targetTab = effectiveNavItems[currentIndex + 1]
                                            pendingDynamicSubTabFocus = false
                                            onSelectedTabChanged(targetTab.ordinal)
                                            // 延迟请求焦点，确保在 AnimatedContent 切换后焦点正确设置
                                            scope.launch {
                                                delay(navigationFocusDelay)
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
