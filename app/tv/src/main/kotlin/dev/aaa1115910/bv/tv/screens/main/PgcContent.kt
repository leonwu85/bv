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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.tv.component.PgcTopNavItem
import dev.aaa1115910.bv.tv.component.TopNav
import dev.aaa1115910.bv.tv.screens.main.pgc.AnimeContent
import dev.aaa1115910.bv.tv.screens.main.pgc.DocumentaryContent
import dev.aaa1115910.bv.tv.screens.main.pgc.GuoChuangContent
import dev.aaa1115910.bv.tv.screens.main.pgc.MovieContent
import dev.aaa1115910.bv.tv.screens.main.pgc.TvContent
import dev.aaa1115910.bv.tv.screens.main.pgc.VarietyContent
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.pgc.PgcAnimeViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcDocumentaryViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcGuoChuangViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcMovieViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcTvViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcVarietyViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.androidx.compose.koinViewModel

private enum class PgcFocusLayer {
    TopNav,
    Content,
}

@Composable
fun PgcContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    selectedTabOrdinal: Int = PgcTopNavItem.Anime.ordinal,
    onSelectedTabChanged: (Int) -> Unit = {},
    onRequestDrawerFocus: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("PgcContent")
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)

    val animeState = rememberLazyListState()
    val guoChuangState = rememberLazyListState()
    val movieState = rememberLazyListState()
    val documentaryState = rememberLazyListState()
    val tvState = rememberLazyListState()
    val varietyState = rememberLazyListState()

    val animeButtonFocusRequester = remember { FocusRequester() }
    val guoChuangButtonFocusRequester = remember { FocusRequester() }
    val movieButtonFocusRequester = remember { FocusRequester() }
    val documentaryButtonFocusRequester = remember { FocusRequester() }
    val tvButtonFocusRequester = remember { FocusRequester() }
    val varietyButtonFocusRequester = remember { FocusRequester() }

    var focusLayer by remember { mutableStateOf<PgcFocusLayer?>(null) }

    val selectedTab = PgcTopNavItem.entries.getOrElse(selectedTabOrdinal) { PgcTopNavItem.Anime }
    val currentViewModel = rememberPgcViewModel(selectedTab)
    val currentButtonFocusRequester = when (selectedTab) {
        PgcTopNavItem.Anime -> animeButtonFocusRequester
        PgcTopNavItem.GuoChuang -> guoChuangButtonFocusRequester
        PgcTopNavItem.Movie -> movieButtonFocusRequester
        PgcTopNavItem.Documentary -> documentaryButtonFocusRequester
        PgcTopNavItem.Tv -> tvButtonFocusRequester
        PgcTopNavItem.Variety -> varietyButtonFocusRequester
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab.ordinal != selectedTabOrdinal) {
            onSelectedTabChanged(selectedTab.ordinal)
        }
    }

    val currentListOnTop by remember {
        derivedStateOf {
            with(
                when (selectedTab) {
                    PgcTopNavItem.Anime -> animeState
                    PgcTopNavItem.GuoChuang -> guoChuangState
                    PgcTopNavItem.Movie -> movieState
                    PgcTopNavItem.Documentary -> documentaryState
                    PgcTopNavItem.Tv -> tvState
                    PgcTopNavItem.Variety -> varietyState
                }
            ) {
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
    }

    //启动时加载当前选中tab的数据
    LaunchedEffect(selectedTab, currentViewModel) {
        if (currentViewModel.feedItems.isEmpty()) {
            logger.fInfo { "加载 $selectedTab 数据" }
            currentViewModel.init()
        }
    }

    BackHandler(focusLayer != null) {
        logger.fInfo { "onFocusBackToNav" }
        if (focusLayer == PgcFocusLayer.TopNav) {
            onRequestDrawerFocus()
            return@BackHandler
        }
        navFocusRequester.requestFocus(scope)
        // // scroll to top
        // scope.launch(Dispatchers.Main) {
        //     when (selectedTab) {
        //         PgcTopNavItem.Anime -> animeState.animateScrollToItem(0)
        //         PgcTopNavItem.GuoChuang -> guoChuangState.animateScrollToItem(0)
        //         PgcTopNavItem.Movie -> movieState.animateScrollToItem(0)
        //         PgcTopNavItem.Documentary -> documentaryState.animateScrollToItem(0)
        //         PgcTopNavItem.Tv -> tvState.animateScrollToItem(0)
        //         PgcTopNavItem.Variety -> varietyState.animateScrollToItem(0)
        //     }
        // }
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
                            focusLayer = PgcFocusLayer.TopNav
                        } else if (focusLayer == PgcFocusLayer.TopNav) {
                            focusLayer = null
                        }
                    },
                items = PgcTopNavItem.entries,
                isLargePadding = focusLayer != PgcFocusLayer.Content && currentListOnTop,
                initialSelectedItem = selectedTab,
                onSelectedChanged = { nav ->
                    onSelectedTabChanged((nav as PgcTopNavItem).ordinal)
                },
                onClick = { nav ->
                    if (nav == selectedTab) {
                        currentViewModel.reloadAll()
                    }
                },
                onLeftKeyEvent = {
                    // 顶部栏最左侧按左键时，跳转到左侧导航栏
                    onRequestDrawerFocus()
                },
                onDownKeyEvent = {
                    currentButtonFocusRequester.requestFocus(scope)
                    true
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
                        focusLayer = PgcFocusLayer.Content
                    } else if (focusLayer == PgcFocusLayer.Content) {
                        focusLayer = null
                    }
                }
        ) {
            AnimatedContent(
                targetState = selectedTab,
                label = "pgc animated content",
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
                    PgcTopNavItem.Anime -> AnimeContent(
                        lazyListState = animeState,
                        firstButtonFocusRequester = animeButtonFocusRequester
                    )
                    PgcTopNavItem.GuoChuang -> GuoChuangContent(
                        lazyListState = guoChuangState,
                        firstButtonFocusRequester = guoChuangButtonFocusRequester
                    )
                    PgcTopNavItem.Movie -> MovieContent(
                        lazyListState = movieState,
                        firstButtonFocusRequester = movieButtonFocusRequester
                    )
                    PgcTopNavItem.Documentary -> DocumentaryContent(
                        lazyListState = documentaryState,
                        firstButtonFocusRequester = documentaryButtonFocusRequester
                    )
                    PgcTopNavItem.Tv -> TvContent(
                        lazyListState = tvState,
                        firstButtonFocusRequester = tvButtonFocusRequester
                    )
                    PgcTopNavItem.Variety -> VarietyContent(
                        lazyListState = varietyState,
                        firstButtonFocusRequester = varietyButtonFocusRequester
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberPgcViewModel(navItem: PgcTopNavItem): PgcViewModel {
    return when (navItem) {
        PgcTopNavItem.Anime -> koinViewModel<PgcAnimeViewModel>()
        PgcTopNavItem.GuoChuang -> koinViewModel<PgcGuoChuangViewModel>()
        PgcTopNavItem.Movie -> koinViewModel<PgcMovieViewModel>()
        PgcTopNavItem.Documentary -> koinViewModel<PgcDocumentaryViewModel>()
        PgcTopNavItem.Tv -> koinViewModel<PgcTvViewModel>()
        PgcTopNavItem.Variety -> koinViewModel<PgcVarietyViewModel>()
    }
}