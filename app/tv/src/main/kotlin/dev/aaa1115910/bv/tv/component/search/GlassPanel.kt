package dev.aaa1115910.bv.tv.component.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.tv.screens.search.SearchTheme

/**
 * 毛玻璃面板 — 三层叠加实现 glassmorphism 效果
 *
 * Layer 1: 深色半透明覆盖
 * Layer 2: 蓝紫色调玻璃背景
 * Layer 3: 顶部白色高光渐变
 * Border:  白色半透明边框
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    contentPadding: Modifier = Modifier.padding(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(SearchTheme.panelShape)
            .border(
                width = 1.dp,
                color = SearchTheme.glassBorder,
                shape = SearchTheme.panelShape
            )
    ) {
        // Layer 1: 深色覆盖
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(SearchTheme.glassOverlay)
        )
        // Layer 2: 蓝紫玻璃背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(SearchTheme.glassBackground)
        )
        // Layer 3: 顶部高光
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SearchTheme.glassHighlightStart,
                            SearchTheme.glassHighlightEnd
                        )
                    )
                )
        )
        // Content
        Box(
            modifier = contentPadding,
            content = content
        )
    }
}
