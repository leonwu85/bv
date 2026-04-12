package dev.aaa1115910.bv.player.tv.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * TV 播放器统一动画规格
 */
object PlayerAnimations {

    // ── 控制层 显示/隐藏 ──

    val controllerEnter: EnterTransition =
        fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }

    val controllerExit: ExitTransition =
        fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 4 }

    val controllerTopEnter: EnterTransition =
        fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 4 }

    val controllerTopExit: ExitTransition =
        fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 4 }

    // ── 右侧菜单 ──

    val menuEnter: EnterTransition =
        slideInHorizontally(tween(350, easing = EaseOutCubic)) { it } +
                fadeIn(tween(250))

    val menuExit: ExitTransition =
        slideOutHorizontally(tween(250, easing = EaseOutCubic)) { it } +
                fadeOut(tween(200))

    // ── 左侧列表面板 ──

    val listPanelEnter: EnterTransition =
        slideInHorizontally(tween(350, easing = EaseOutCubic)) { -it } +
                fadeIn(tween(250))

    val listPanelExit: ExitTransition =
        slideOutHorizontally(tween(250, easing = EaseOutCubic)) { -it } +
                fadeOut(tween(200))

    // ── Seek 卡片 ──

    val seekEnter: EnterTransition =
        fadeIn(tween(200)) + scaleIn(tween(250), initialScale = 0.85f)

    val seekExit: ExitTransition =
        fadeOut(tween(150)) + scaleOut(tween(200), targetScale = 0.85f)

    // ── 提示 pill ──

    val tipEnter: EnterTransition =
        expandHorizontally(tween(300, easing = EaseOutCubic)) +
                fadeIn(tween(250))

    val tipExit: ExitTransition =
        shrinkHorizontally(tween(200, easing = EaseOutCubic)) +
                fadeOut(tween(150))

    // ── 通用淡入淡出 ──

    val fadeEnter: EnterTransition = fadeIn(tween(250))
    val fadeExit: ExitTransition = fadeOut(tween(200))
}
