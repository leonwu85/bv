package dev.aaa1115910.bv.tv.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * 保留最近访问 / 预加载窗口内的页面，切换时不销毁滚动状态。
 *
 * - **活跃页**全屏布局；**非活跃页** 0×0，Lazy 列表几乎不 compose item，减轻滚动压力。
 * - 传入 [orderedItems] + [preloadStep] 时，始终保留当前 ± step 的邻居页（数据预加载 + 快速切回）。
 * - 非活跃页 [canFocus]=false，避免抢焦点。
 * - [enableAnimation] 为 true 时，活跃页切入做短 fade-in（非活跃页仍为 0 尺寸，不做双页 slide，避免滚动卡顿）。
 */
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

    Box(modifier = modifier.fillMaxSize()) {
        val pagesSnapshot = keptPages.toList()
        pagesSnapshot.forEach { page ->
            key(page) {
                val active = page == current
                val targetAlpha = if (active) 1f else 0f
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = if (enableAnimation && active) {
                        // 仅活跃页淡入；非活跃立即 snap，避免与 0 尺寸布局冲突
                        tween(durationMillis = 120)
                    } else {
                        snap()
                    },
                    label = "keepAlivePageAlpha"
                )
                Box(
                    modifier = Modifier
                        .zIndex(if (active) 1f else 0f)
                        .then(
                            if (active) {
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { this.alpha = alpha }
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
