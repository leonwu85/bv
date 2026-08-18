package dev.aaa1115910.bv.tv.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** 过渡时长 */
private const val PAGE_SLIDE_DURATION_MS = 160

/**
 * 水平位移相对容器宽度的比例（小幅滑动，非整页翻页）。
 * 约 6% 屏宽，配合淡入即可感知方向。
 */
private const val PAGE_SLIDE_OFFSET_FRACTION = 0.06f

/**
 * 冷启动时：等布局就绪后再播动画，并多等几帧让 LazyGrid item 落稳。
 * 已预热的邻页几乎不等。
 */
private const val PAGE_SLIDE_COLD_PREP_FRAMES = 3
private const val PAGE_SLIDE_WARM_PREP_FRAMES = 1

/**
 * 没有页面动画时也等待一段完整的无交互窗口，再在后台展开预加载页。
 * 新 D-pad 交互会重启等待，避免固定延迟到点时恰好与可见交互帧重叠。
 */
private const val PAGE_PRELOAD_INTERACTION_QUIET_MS = 250L

/** 等待容器宽度 / 页面首帧布局的上限 */
private const val PAGE_LAYOUT_WAIT_MS = 500L

/**
 * 保留最近访问 / 预加载窗口内的页面，切换时不销毁滚动状态。
 *
 * - **活跃页**全屏布局；窗口外非活跃页 0×0，减轻压力。
 * - 传入 [orderedItems] + [preloadStep] 时，在当前页稳定后逐个保留当前 ± step 的邻居页。
 * - [enableAnimation] 为 true 时：
 *   - **邻页预热**：当前页过渡完成后，才将预加载窗口内页面逐个以全屏、alpha=0
 *     布局，让 LazyGrid 提前 compose；避免动画期间同时创建下一批页面。
 *   - 旧页立刻退出活跃（预热窗内保留不可见全屏，窗外收 0×0）。
 *   - 仅新页小幅滑入 + 淡入；等目标页至少完成一次全屏布局后再播动画。
 * - 非活跃页 [canFocus]=false；全屏页不用 canFocus=false 误伤内部 TopNav
 *   （MainScreen 的 KeepAlive 可能包着整页）。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : Any> KeepAlivePages(
    current: T,
    modifier: Modifier = Modifier,
    maxKeep: Int = 3,
    enableAnimation: Boolean = false,
    orderedItems: List<T>? = null,
    preloadStep: Int = 0,
    prepareBeforeDisplay: Boolean = false,
    imageLoadDelayMillis: Long? = null,
    onDisplayedPageChanged: (T) -> Unit = {},
    content: @Composable (page: T, active: Boolean) -> Unit
) {
    val keptPages = remember { mutableStateListOf(current) }
    val currentOnDisplayedPageChanged by rememberUpdatedState(onDisplayedPageChanged)
    val performanceProfile = LocalTvUiPerformanceProfile.current
    val preloadCoordinator = LocalTvPreloadCoordinator.current
    val parentImageLoadingAllowed = LocalTvImageLoadingAllowed.current
    val effectiveImageLoadDelay = imageLoadDelayMillis
        ?: performanceProfile.imageLoadDelayMillis

    var preparedDisplayedCurrent by remember { mutableStateOf(current) }
    val displayedCurrent = if (prepareBeforeDisplay) preparedDisplayedCurrent else current
    val preparingPage = current.takeIf {
        prepareBeforeDisplay && it != preparedDisplayedCurrent
    }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var previousCurrent by remember { mutableStateOf(displayedCurrent) }
    // +1：索引增大，新页从右侧进入；-1：从左侧进入
    var slideDir by remember { mutableIntStateOf(1) }
    val slideProgress = remember { Animatable(1f) }
    var isEnterAnimating by remember { mutableStateOf(false) }
    var transitionGeneration by remember { mutableIntStateOf(0) }

    // 已完成过全屏布局的页（预热或展示过）；用于判断冷/热启动
    val laidOutPages = remember { mutableSetOf<T>() }
    var laidOutEpoch by remember { mutableIntStateOf(0) }

    // 只有进入该集合的非活跃页才会全屏预布局。集合在当前页动画结束后再更新，
    // 避免切换瞬间同时 measure/compose 新邻页拖慢可见动画。
    val warmedPages = remember { mutableStateListOf(current) }
    val hasUsableLayout = containerWidthPx > 0f
    var imageReadyPage by remember { mutableStateOf<T?>(null) }

    fun markLaidOut(page: T) {
        if (laidOutPages.add(page)) {
            laidOutEpoch++
        }
    }

    fun clearLaidOut(page: T) {
        if (laidOutPages.remove(page)) {
            laidOutEpoch++
        }
    }

    LaunchedEffect(current, prepareBeforeDisplay, hasUsableLayout) {
        if (!prepareBeforeDisplay || current == preparedDisplayedCurrent || !hasUsableLayout) {
            return@LaunchedEffect
        }

        val target = current
        if (target !in laidOutPages) {
            withTimeoutOrNull(PAGE_LAYOUT_WAIT_MS) {
                snapshotFlow { laidOutEpoch }.first { target in laidOutPages }
            }
        }
        // 目标页完成首屏布局后再跨一帧提交，旧页在此之前持续显示。
        // 少数自定义布局不回报 size 时，超时后也要提交，避免永久卡在旧页。
        withFrameNanos { }
        if (current == target && hasUsableLayout) {
            preparedDisplayedCurrent = target
        }
    }

    SideEffect(displayedCurrent) {
        currentOnDisplayedPageChanged(displayedCurrent)
    }

    LaunchedEffect(displayedCurrent, effectiveImageLoadDelay) {
        imageReadyPage = null
        if (effectiveImageLoadDelay > 0L) {
            delay(effectiveImageLoadDelay)
        }
        withFrameNanos { }
        imageReadyPage = displayedCurrent
    }

    val safeMaxKeep = maxKeep.coerceAtLeast(1)
    val safeStep = preloadStep.coerceAtLeast(0)
    val preloadWindow = remember(displayedCurrent, orderedItems, safeStep, safeMaxKeep) {
        if (orderedItems != null && safeStep > 0) {
            boundedAdjacentNavItems(
                items = orderedItems,
                current = displayedCurrent,
                step = safeStep,
                maxItems = safeMaxKeep,
            )
        } else {
            listOf(displayedCurrent)
        }
    }

    // 外层 KeepAlive 把本页收成 0×0 时，立即释放内部邻页的全屏预布局状态。
    // 否则外层再次显示时，多个嵌套 LazyGrid 会在同一帧一起恢复全屏布局。
    LaunchedEffect(hasUsableLayout) {
        if (!hasUsableLayout) {
            warmedPages.removeAll { it != displayedCurrent }
        }
    }

    LaunchedEffect(
        displayedCurrent,
        preparingPage,
        enableAnimation,
        safeMaxKeep,
        preloadWindow,
        hasUsableLayout,
    ) {
        val from = previousCurrent
        val generation = transitionGeneration + 1
        transitionGeneration = generation
        if (preparingPage != null) {
            isEnterAnimating = false
            slideProgress.snapTo(1f)
            return@LaunchedEffect
        }
        val shouldAnimate = from != displayedCurrent && enableAnimation

        if (from == displayedCurrent) {
            isEnterAnimating = false
            if (slideProgress.value != 1f) slideProgress.snapTo(1f)
        } else if (!enableAnimation) {
            previousCurrent = displayedCurrent
            isEnterAnimating = false
            slideProgress.snapTo(1f)
        } else {
            previousCurrent = displayedCurrent
            slideDir = resolveSlideDirection(orderedItems, from, displayedCurrent)
            // 切换瞬间读取是否已预热过（邻页 alpha=0 全屏布局后会进 laidOutPages）
            val alreadyWarm = displayedCurrent in laidOutPages
            isEnterAnimating = true

            try {
                slideProgress.snapTo(0f)

                if (containerWidthPx <= 0f) {
                    withTimeoutOrNull(PAGE_LAYOUT_WAIT_MS) {
                        snapshotFlow { containerWidthPx }.first { it > 0f }
                    }
                }

                // 等到目标页完成至少一次全屏 measure（冷页在此消化首帧成本）
                if (displayedCurrent !in laidOutPages) {
                    withTimeoutOrNull(PAGE_LAYOUT_WAIT_MS) {
                        snapshotFlow { laidOutEpoch }.first { displayedCurrent in laidOutPages }
                    }
                }

                val prepFrames =
                    if (alreadyWarm) PAGE_SLIDE_WARM_PREP_FRAMES else PAGE_SLIDE_COLD_PREP_FRAMES
                repeat(prepFrames) {
                    withFrameNanos { }
                }

                slideProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = PAGE_SLIDE_DURATION_MS,
                        easing = FastOutSlowInEasing,
                    ),
                )
            } finally {
                if (transitionGeneration == generation) {
                    isEnterAnimating = false
                    if (slideProgress.value != 1f) {
                        slideProgress.snapTo(1f)
                    }
                }
            }
        }

        if (transitionGeneration != generation) return@LaunchedEffect

        // 初次进入或关闭页面动画时，等待完整的无交互窗口。固定 160ms 延迟无法处理
        // 用户恰好在计时结束前按键的情况。
        if (!shouldAnimate) {
            preloadCoordinator.awaitInteractionIdle(PAGE_PRELOAD_INTERACTION_QUIET_MS)
        }
        if (transitionGeneration != generation) return@LaunchedEffect

        // 当前页优先进入 LRU；current 即使尚未写入 keptPages，也会由 pagesSnapshot
        // 立即参与本轮 composition，不需要在 composition 中修改 SnapshotStateList。
        keptPages.remove(displayedCurrent)
        keptPages.add(displayedCurrent)
        if (displayedCurrent !in warmedPages) warmedPages.add(displayedCurrent)

        // 一次只展开一个邻页，并至少跨一帧，避免多个 LazyGrid 同帧首次布局。
        preloadCoordinator.runExclusive {
            preloadWindow.asSequence()
                .filter { it != displayedCurrent }
                .forEach { page ->
                    if (page !in keptPages) keptPages.add(page)
                    // 关闭动画时仅以 0×0 组合邻页来预取数据，维持低内存占用；
                    // 开启动画才做全屏预布局，为下一次切换准备好 LazyGrid 首帧。
                    if (enableAnimation && hasUsableLayout && page !in warmedPages) {
                        warmedPages.add(page)
                    }
                    withFrameNanos { }
                }
        }
        if (transitionGeneration != generation) return@LaunchedEffect

        // 新窗口完成预热后再收起旧窗口，动画期间不触发额外的大范围重布局。
        val protectedPages = preloadWindow.toSet() + displayedCurrent
        val fullLayoutPages = if (enableAnimation && hasUsableLayout) {
            protectedPages
        } else {
            setOf(displayedCurrent)
        }
        warmedPages.removeAll { it !in fullLayoutPages }

        val limit = safeMaxKeep.coerceAtLeast(protectedPages.size.coerceAtLeast(1))
        while (keptPages.size > limit) {
            val evictIndex = keptPages.indexOfFirst {
                it != displayedCurrent && it !in protectedPages
            }
            if (evictIndex < 0) break
            val evicted = keptPages.removeAt(evictIndex)
            warmedPages.remove(evicted)
            clearLaidOut(evicted)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
    ) {
        val pagesSnapshot = buildList {
            addAll(keptPages)
            if (displayedCurrent !in this) add(displayedCurrent)
            // 新 target 在原子提交前先以透明全屏完成首屏布局。
            if (current !in this) add(current)
        }

        // LRU 淘汰的页会直接离开 composition，走不到下方 showFull=false 分支；
        // 必须按 kept 集合同步清掉 laidOut，否则冷页再进入会被误判为 warm。
        LaunchedEffect(pagesSnapshot) {
            val keptSet = pagesSnapshot.toSet()
            laidOutPages
                .filter { it !in keptSet }
                .forEach { clearLaidOut(it) }
        }

        pagesSnapshot.forEach { page ->
            key(page) {
                val active = page == displayedCurrent
                // 活跃页 or 已完成调度的预热邻页：全屏布局；其余 0×0
                val showFull = active || page == preparingPage || page in warmedPages

                // 仍在 kept 但收成 0×0：布局已销毁，清 laidOut
                LaunchedEffect(showFull, page) {
                    if (!showFull) {
                        clearLaidOut(page)
                    }
                }
                // 被 LRU 移出 composition 时兜底清理（与上方 LaunchedEffect 双保险）
                DisposableEffect(page) {
                    onDispose { clearLaidOut(page) }
                }

                Box(
                    modifier = Modifier
                        .zIndex(if (active) 1f else 0f)
                        .then(
                            if (showFull) {
                                Modifier
                                    .fillMaxSize()
                                    // 预热邻页：拦截焦点进入，避免不可见的 LazyGrid 抢走 TopNav
                                    .focusProperties {
                                        if (!active) {
                                            canFocus = false
                                            onEnter = { cancelFocusChange() }
                                        }
                                    }
                                    .onSizeChanged { size ->
                                        if (size.width > 0 && size.height > 0) {
                                            markLaidOut(page)
                                        }
                                    }
                                    .then(if (active && !enableAnimation) Modifier else Modifier.graphicsLayer {
                                        if (active) {
                                            if (enableAnimation && isEnterAnimating) {
                                                val p = slideProgress.value
                                                val offset = containerWidthPx
                                                    .coerceAtLeast(0f) * PAGE_SLIDE_OFFSET_FRACTION
                                                translationX = (1f - p) * slideDir * offset
                                                alpha = p
                                            } else {
                                                translationX = 0f
                                                alpha = 1f
                                            }
                                        } else {
                                            // 预热邻页：全屏布局但不绘制
                                            translationX = 0f
                                            alpha = 0f
                                        }
                                    })
                            } else {
                                Modifier
                                    .size(0.dp)
                                    .focusProperties { canFocus = false }
                            }
                        )
                ) {
                    val allowPageImages = parentImageLoadingAllowed &&
                            active && imageReadyPage == page
                    CompositionLocalProvider(
                        LocalTvImageLoadingAllowed provides allowPageImages
                    ) {
                        content(page, active)
                    }
                }
            }
        }
    }
}

/**
 * 根据导航顺序决定滑动方向：索引增大为 +1（从右进），减小为 -1（从左进）。
 * 无序或找不到时默认 +1。
 */
private fun <T> resolveSlideDirection(orderedItems: List<T>?, from: T, to: T): Int {
    if (orderedItems == null) return 1
    val fromIndex = orderedItems.indexOf(from)
    val toIndex = orderedItems.indexOf(to)
    if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return 1
    return if (toIndex > fromIndex) 1 else -1
}
