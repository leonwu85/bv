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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val ugcNavItems by ugcNavItemsFlow.collectAsState(
        initial = remember { parseUgcNavItemsOrder(Prefs.ugcNavItemsOrder) }
    )

    // 为当前选中的tab创建LazyGridState
    val currentLazyGridState = rememberLazyGridState()
    val instantiatedViewModels = remember { mutableMapOf<UgcTopNavItem, UgcViewModel>() }
    var focusLayer by remember { mutableStateOf<UgcFocusLayer?>(null) }

    val selectedTab = UgcTopNavItem.entries
        .getOrElse(selectedTabOrdinal) { UgcTopNavItem.Douga }
        .takeIf { it in ugcNavItems }
        ?: ugcNavItems.first()
    val currentViewModel = rememberUgcViewModel(selectedTab)

    LaunchedEffect(selectedTab, currentViewModel) {
        if (selectedTab.ordinal != selectedTabOrdinal) {
            onSelectedTabChanged(selectedTab.ordinal)
        }

        instantiatedViewModels[selectedTab] = currentViewModel

        instantiatedViewModels.forEach { (navItem, viewModel) ->
            if (navItem != selectedTab) {
                viewModel.cancelDelayedLoad()
            }
        }

        currentViewModel.loadDataWithDelay(300L)
    }

    BackHandler(focusLayer != null) {
        logger.fInfo { "onFocusBackToNav" }
        if (focusLayer == UgcFocusLayer.TopNav) {
            onRequestDrawerFocus()
            return@BackHandler
        }
        navFocusRequester.requestFocus(scope)
        // 滚动到顶部（如果需要的话）
        // scope.launch(Dispatchers.Main) {
        //     currentLazyGridState.animateScrollToItem(0)
        // }
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
                        focusLayer = UgcFocusLayer.Content
                    } else if (focusLayer == UgcFocusLayer.Content) {
                        focusLayer = null
                    }
                }
        ) {
            AnimatedContent(
                targetState = selectedTab,
                label = "ugc animated content",
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
                val screenViewModel = rememberUgcViewModel(screen)
                LaunchedEffect(screen, screenViewModel) {
                    instantiatedViewModels[screen] = screenViewModel
                }

                CreateUgcContent(
                    navItem = screen,
                    lazyGridState = currentLazyGridState,
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