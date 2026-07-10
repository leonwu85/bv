package dev.aaa1115910.bv.tv.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import dev.aaa1115910.bv.tv.component.TopNav
import dev.aaa1115910.bv.tv.component.UgcTopNavItem
import dev.aaa1115910.bv.tv.screens.main.ugc.CreateUgcContent
import dev.aaa1115910.bv.tv.util.KeepAlivePages
import dev.aaa1115910.bv.tv.util.LocalTvUiPerformanceProfile
import dev.aaa1115910.bv.tv.util.TOP_NAV_PRELOAD_STEP
import dev.aaa1115910.bv.tv.util.boundedAdjacentNavItems
import dev.aaa1115910.bv.tv.util.parseUgcNavItemsOrder
import dev.aaa1115910.bv.tv.util.ugcNavItemsFlow
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.ugc.UgcAiViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcAnimalViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcCarViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcCinephileViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcDanceViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcDougaViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcEmotionViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcEntViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcFashionViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcFoodViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcGameViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcGymViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcHandmakeViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcHealthViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcHomeViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcInformationViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcKichikuViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcKnowledgeViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcLifeExperienceViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcLifeJoyViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcMusicViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcMysticismViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcOutdoorsViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcPaintingViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcParentingViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcRuralViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcShortplayViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcSportsViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcTechViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcTravelViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcVlogViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.androidx.compose.koinViewModel

private enum class UgcFocusLayer {
    TopNav,
    Content,
}

@Composable
fun UgcContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    selectedTabOrdinal: Int = UgcTopNavItem.Douga.ordinal,
    onSelectedTabChanged: (Int) -> Unit = {},
    onRequestDrawerFocus: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("UgcContent")
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)
    val performanceProfile = LocalTvUiPerformanceProfile.current
    val enableFullPageAnimation =
        enableMainUiAnimation && performanceProfile.allowFullPageAnimation
    val ugcNavItems by ugcNavItemsFlow.collectAsState(
        initial = remember { parseUgcNavItemsOrder(Prefs.ugcNavItemsOrder) }
    )

    // 每个 Tab 独立滚动状态，配合 keep-alive 保留位置
    val gridStates = remember { mutableStateMapOf<UgcTopNavItem, LazyGridState>() }
    val instantiatedViewModels = remember { mutableMapOf<UgcTopNavItem, UgcViewModel>() }
    var focusLayer by remember { mutableStateOf<UgcFocusLayer?>(null) }

    val selectedTab = UgcTopNavItem.entries
        .getOrElse(selectedTabOrdinal) { UgcTopNavItem.Douga }
        .takeIf { it in ugcNavItems }
        ?: ugcNavItems.first()
    val currentViewModel = rememberUgcViewModel(selectedTab)

    fun gridStateFor(tab: UgcTopNavItem): LazyGridState {
        return gridStates.getOrPut(tab) { LazyGridState() }
    }

    // 预加载窗口：当前页 + 设备预算内的相邻页（用于数据预取）。
    val preloadTabs = remember(selectedTab, ugcNavItems) {
        boundedAdjacentNavItems(
            items = ugcNavItems,
            current = selectedTab,
            step = TOP_NAV_PRELOAD_STEP,
            maxItems = performanceProfile.maxKeepPages,
        )
    }

    LaunchedEffect(selectedTab, currentViewModel) {
        if (selectedTab.ordinal != selectedTabOrdinal) {
            onSelectedTabChanged(selectedTab.ordinal)
        }

        instantiatedViewModels[selectedTab] = currentViewModel

        // 取消不在预加载窗口内的延迟任务，避免乱切时浪费
        instantiatedViewModels.forEach { (navItem, viewModel) ->
            if (navItem !in preloadTabs) {
                viewModel.cancelDelayedLoad()
            }
        }

        // 当前页立即加载
        currentViewModel.loadDataWithDelay(0L)
    }

    BackHandler(focusLayer != null) {
        logger.fInfo { "onFocusBackToNav" }
        if (focusLayer == UgcFocusLayer.TopNav) {
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
                    .onFocusChanged {
                        if (it.hasFocus) {
                            focusLayer = UgcFocusLayer.TopNav
                        } else if (focusLayer == UgcFocusLayer.TopNav) {
                            focusLayer = null
                        }
                    },
                items = ugcNavItems,
                isLargePadding = focusLayer != UgcFocusLayer.Content,
                initialSelectedItem = selectedTab,
                onSelectedChanged = { nav ->
                    onSelectedTabChanged((nav as UgcTopNavItem).ordinal)
                },
                onClick = { nav ->
                    if (nav == selectedTab) {
                        currentViewModel.reloadAll()
                    }
                },
                onLeftKeyEvent = {
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
                        focusLayer = UgcFocusLayer.Content
                    } else if (focusLayer == UgcFocusLayer.Content) {
                        focusLayer = null
                    }
                }
        ) {
            KeepAlivePages(
                current = selectedTab,
                maxKeep = performanceProfile.maxKeepPages,
                enableAnimation = enableFullPageAnimation,
                orderedItems = ugcNavItems,
                preloadStep = TOP_NAV_PRELOAD_STEP,
            ) { screen, active ->
                val screenViewModel = rememberUgcViewModel(screen)
                LaunchedEffect(screen, screenViewModel, active) {
                    instantiatedViewModels[screen] = screenViewModel
                    // 预加载窗口内的页（含邻居）在空数据时拉取，避免首次切入等待
                    if (screen in preloadTabs || active) {
                        screenViewModel.loadDataWithDelay(0L)
                    }
                }

                CreateUgcContent(
                    navItem = screen,
                    lazyGridState = gridStateFor(screen),
                    ugcViewModel = screenViewModel
                )
            }
        }
    }
}

@Composable
private fun rememberUgcViewModel(navItem: UgcTopNavItem): UgcViewModel {
    return when (navItem) {
        UgcTopNavItem.Douga -> koinViewModel<UgcDougaViewModel>()
        UgcTopNavItem.Game -> koinViewModel<UgcGameViewModel>()
        UgcTopNavItem.Kichiku -> koinViewModel<UgcKichikuViewModel>()
        UgcTopNavItem.Music -> koinViewModel<UgcMusicViewModel>()
        UgcTopNavItem.Dance -> koinViewModel<UgcDanceViewModel>()
        UgcTopNavItem.Cinephile -> koinViewModel<UgcCinephileViewModel>()
        UgcTopNavItem.Ent -> koinViewModel<UgcEntViewModel>()
        UgcTopNavItem.Knowledge -> koinViewModel<UgcKnowledgeViewModel>()
        UgcTopNavItem.Tech -> koinViewModel<UgcTechViewModel>()
        UgcTopNavItem.Information -> koinViewModel<UgcInformationViewModel>()
        UgcTopNavItem.Food -> koinViewModel<UgcFoodViewModel>()
        UgcTopNavItem.ShortPlay -> koinViewModel<UgcShortplayViewModel>()
        UgcTopNavItem.Car -> koinViewModel<UgcCarViewModel>()
        UgcTopNavItem.Fashion -> koinViewModel<UgcFashionViewModel>()
        UgcTopNavItem.Sports -> koinViewModel<UgcSportsViewModel>()
        UgcTopNavItem.Animal -> koinViewModel<UgcAnimalViewModel>()
        UgcTopNavItem.Vlog -> koinViewModel<UgcVlogViewModel>()
        UgcTopNavItem.Painting -> koinViewModel<UgcPaintingViewModel>()
        UgcTopNavItem.Ai -> koinViewModel<UgcAiViewModel>()
        UgcTopNavItem.Home -> koinViewModel<UgcHomeViewModel>()
        UgcTopNavItem.Outdoors -> koinViewModel<UgcOutdoorsViewModel>()
        UgcTopNavItem.Gym -> koinViewModel<UgcGymViewModel>()
        UgcTopNavItem.Handmake -> koinViewModel<UgcHandmakeViewModel>()
        UgcTopNavItem.Travel -> koinViewModel<UgcTravelViewModel>()
        UgcTopNavItem.Rural -> koinViewModel<UgcRuralViewModel>()
        UgcTopNavItem.Parenting -> koinViewModel<UgcParentingViewModel>()
        UgcTopNavItem.Health -> koinViewModel<UgcHealthViewModel>()
        UgcTopNavItem.Emotion -> koinViewModel<UgcEmotionViewModel>()
        UgcTopNavItem.LifeJoy -> koinViewModel<UgcLifeJoyViewModel>()
        UgcTopNavItem.LifeExperience -> koinViewModel<UgcLifeExperienceViewModel>()
        UgcTopNavItem.Mysticism -> koinViewModel<UgcMysticismViewModel>()
    }
}
