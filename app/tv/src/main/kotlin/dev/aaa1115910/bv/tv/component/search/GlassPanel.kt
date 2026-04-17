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
 * 毛玻璃面板，按当前明暗主题切换深浅玻璃配色。
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    contentPadding: Modifier = Modifier.padding(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val borderColor = SearchTheme.glassBorderColor()
    val overlayColor = SearchTheme.glassOverlayColor()
    val backgroundColor = SearchTheme.glassBackgroundColor()
    val highlightBrush = Brush.verticalGradient(
        colors = listOf(
            SearchTheme.glassHighlightStartColor(),
            SearchTheme.glassHighlightEndColor()
        )
    )

    Box(
        modifier = modifier
            .clip(SearchTheme.panelShape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = SearchTheme.panelShape
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(overlayColor)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(backgroundColor)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(highlightBrush)
        )
        Box(
            modifier = contentPadding,
            content = content
        )
    }
}
