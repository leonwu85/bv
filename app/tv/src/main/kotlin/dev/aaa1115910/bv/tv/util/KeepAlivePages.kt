package dev.aaa1115910.bv.tv.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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

/** 等待容器宽度 / 页面首帧布局的上限 */
private const val PAGE_LAYOUT_WAIT_MS = 500L

/**
 * 保留最近访问 / 预加载窗口内的页面，切换时不销毁滚动状态。
 *
 * - **活跃页**全屏布局；窗口外非活跃页 0×0，减轻压力。
 * - 传入 [orderedItems] + [preloadStep] 时，始终保留当前 ± step 的邻居页。
 * - [enableAnimation] 为 true 时：
 *   - **邻页预热**：预加载窗口内页面以全屏、alpha=0 布局，让 LazyGrid 提前 compose，
 *     避免「首次切入」时边建页边做动画导致卡顿。
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
    content: @Composable (page: T, active: Boolean) -> Unit
) {
    val keptPages = rememberKeepAlivePages(
        current = current,
        maxKeep = maxKeep,
        orderedItems = orderedItems,
        preloadStep = preloadStep,
    )

    // 动画开启时：邻页全屏预热（不可见），首次切入时 LazyGrid 已建好
    val warmWindow = remember(current, orderedItems, preloadStep, enableAnimation) {
        if (enableAnimation && orderedItems != null && preloadStep > 0) {
            adjacentNavItems(orderedItems, current, preloadStep).toSet()
        } else {
            setOf(current)
        }
    }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var previousCurrent by remember { mutableStateOf(current) }
    // +1：索引增大，新页从右侧进入；-1：从左侧进入
    var slideDir by remember { mutableIntStateOf(1) }
    val slideProgress = remember { Animatable(1f) }
    var isEnterAnimating by remember { mutableStateOf(false) }
    var transitionGeneration by remember { mutableIntStateOf(0) }

    // 已完成过全屏布局的页（预热或展示过）；用于判断冷/热启动
    val laidOutPages = remember { mutableSetOf<T>() }
    var laidOutEpoch by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(current, enableAnimation) {
        val from = previousCurrent
        val generation = transitionGeneration + 1
        transitionGeneration = generation

        if (from == current) {
            isEnterAnimating = false
            if (slideProgress.value != 1f) slideProgress.snapTo(1f)
            return@LaunchedEffect
        }
        previousCurrent = current

        if (!enableAnimation) {
            isEnterAnimating = false
            slideProgress.snapTo(1f)
            return@LaunchedEffect
        }

        slideDir = resolveSlideDirection(orderedItems, from, current)
        // 切换瞬间读取是否已预热过（邻页 alpha=0 全屏布局后会进 laidOutPages）
        val alreadyWarm = current in laidOutPages
        isEnterAnimating = true

        try {
            slideProgress.snapTo(0f)

            if (containerWidthPx <= 0f) {
                withTimeoutOrNull(PAGE_LAYOUT_WAIT_MS) {
                    snapshotFlow { containerWidthPx }.first { it > 0f }
                }
            }

            // 等到目标页完成至少一次全屏 measure（冷页在此消化首帧成本）
            if (current !in laidOutPages) {
                withTimeoutOrNull(PAGE_LAYOUT_WAIT_MS) {
                    snapshotFlow { laidOutEpoch }.first { current in laidOutPages }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
    ) {
        val pagesSnapshot = keptPages.toList()

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
                val active = page == current
                // 活跃页 or 预热邻页：全屏布局；其余 0×0
                val showFull = active || page in warmWindow

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
                                    .graphicsLayer {
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
                                    }
                            } else {
                                Modifier
                                    .size(0.dp)
                                    .focusProperties { canFocus = false }
                            }
                        )
                ) {
                    content(page, active)
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

@Composable
private fun <T : Any> rememberKeepAlivePages(
    current: T,
    maxKeep: Int,
    orderedItems: List<T>?,
    preloadStep: Int,
): SnapshotStateList<T> {
    val keptPages = remember {
        mutableStateListOf(current)
    }
    val safeMaxKeep = maxKeep.coerceAtLeast(1)
    val safeStep = preloadStep.coerceAtLeast(0)

    // 预加载窗口：始终保留当前 ± step
    val preloadWindow = if (orderedItems != null && safeStep > 0) {
        adjacentNavItems(orderedItems, current, safeStep)
    } else {
        listOf(current)
    }

    // 同步保证预加载窗口与当前页在列表中
    preloadWindow.forEach { page ->
        if (page !in keptPages) {
            keptPages.add(page)
        }
    }
    if (current !in keptPages) {
        keptPages.add(current)
    }

    LaunchedEffect(current, safeMaxKeep, preloadWindow) {
        // LRU：当前页移到末尾
        if (current in keptPages) {
            keptPages.remove(current)
        }
        keptPages.add(current)

        // 确保预加载邻居存在
        preloadWindow.forEach { page ->
            if (page !in keptPages) {
                keptPages.add(page)
            }
        }

        val minKeep = preloadWindow.size.coerceAtLeast(1)
        val limit = safeMaxKeep.coerceAtLeast(minKeep)
        while (keptPages.size > limit) {
            // 优先淘汰不在预加载窗口且非当前的最旧页
            val evictIndex = keptPages.indexOfFirst { it != current && it !in preloadWindow }
            if (evictIndex < 0) break
            keptPages.removeAt(evictIndex)
        }
    }

    return keptPages
}
