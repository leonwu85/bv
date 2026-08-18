package dev.aaa1115910.bv.tv.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import dev.aaa1115910.bv.tv.util.KeepAlivePages
import dev.aaa1115910.bv.tv.util.LocalTvUiPerformanceProfile
import dev.aaa1115910.bv.tv.util.TOP_NAV_PRELOAD_STEP
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
    val currentOnSelectedTabChanged by rememberUpdatedState(onSelectedTabChanged)
    val logger = KotlinLogging.logger("PgcContent")
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)
    val performanceProfile = LocalTvUiPerformanceProfile.current
    val enableFullPageAnimation =
        enableMainUiAnimation && performanceProfile.allowFullPageAnimation

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

    SideEffect(selectedTab) {
        if (selectedTab.ordinal != selectedTabOrdinal) {
            currentOnSelectedTabChanged(selectedTab.ordinal)
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

    // 当前页加载由 KeepAlive 内容区统一处理（含邻居预加载）

    BackHandler(focusLayer != null) {
        logger.fInfo { "onFocusBackToNav" }
        if (focusLayer == PgcFocusLayer.TopNav) {
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
            KeepAlivePages(
                current = selectedTab,
                maxKeep = performanceProfile.maxKeepPages,
                enableAnimation = enableFullPageAnimation,
                orderedItems = PgcTopNavItem.entries,
                preloadStep = TOP_NAV_PRELOAD_STEP,
            ) { screen, _ ->
                val screenViewModel = rememberPgcViewModel(screen)
                LaunchedEffect(screen, screenViewModel) {
                    if (screenViewModel.feedItems.isEmpty()) {
                        logger.fInfo { "预加载/加载 $screen 数据" }
                        screenViewModel.init()
                    }
                }
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
