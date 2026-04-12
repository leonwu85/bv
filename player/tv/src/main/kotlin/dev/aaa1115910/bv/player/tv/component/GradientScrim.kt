package dev.aaa1115910.bv.player.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import dev.aaa1115910.bv.player.tv.theme.PlayerColors

/**
 * 顶部渐变遮罩 — Black(0.7f) → Transparent，覆盖上方区域
 */
@Composable
fun TopGradientScrim(
    modifier: Modifier = Modifier,
    colors: List<androidx.compose.ui.graphics.Color> = PlayerColors.controllerScrimTop,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors))
    )
}

/**
 * 底部渐变遮罩 — Transparent → Black(0.7f)，覆盖下方区域
 */
@Composable
fun BottomGradientScrim(
    modifier: Modifier = Modifier,
    colors: List<androidx.compose.ui.graphics.Color> = PlayerColors.controllerScrimBottom,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors))
    )
}

/**
 * 播放器遮罩容器 — 同时提供顶部和底部渐变遮罩
 *
 * @param topContent 顶部渐变区域内容（如时钟）
 * @param bottomContent 底部渐变区域内容（如控制栏）
 */
@Composable
fun PlayerScrimLayout(
    modifier: Modifier = Modifier,
    topContent: @Composable BoxScope.() -> Unit = {},
    bottomContent: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        TopGradientScrim(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )
        Box(
            modifier = Modifier.align(Alignment.TopCenter),
            content = topContent
        )

        BottomGradientScrim(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
        Box(
            modifier = Modifier.align(Alignment.BottomCenter),
            content = bottomContent
        )
    }
}
