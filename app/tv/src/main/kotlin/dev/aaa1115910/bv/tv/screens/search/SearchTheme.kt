package dev.aaa1115910.bv.tv.screens.search

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.tv.component.screenBackgroundGradient

/**
 * 搜索页面专用视觉常量
 */
object SearchTheme {
    // ── 毛玻璃面板 ──
    val glassBackground = Color(0xFF1a1a2e).copy(alpha = 0.7f)
    val glassOverlay = Color.Black.copy(alpha = 0.4f)
    val glassBorder = Color.White.copy(alpha = 0.08f)
    val glassHighlightStart = Color.White.copy(alpha = 0.05f)
    val glassHighlightEnd = Color.Transparent

    // ── 渐变强调色 ──
    val accentPink = Color(0xFFFB7299)
    val accentPurple = Color(0xFFBD26B8)
    val accentGradient = Brush.horizontalGradient(
        colors = listOf(accentPink, accentPurple)
    )

    // ── 圆角 ──
    val panelShape = RoundedCornerShape(16.dp)
    val keyShape = RoundedCornerShape(12.dp)
    val searchFieldShape = RoundedCornerShape(24.dp)
    val pillShape = RoundedCornerShape(20.dp)
    val chipShape = RoundedCornerShape(16.dp)
    val itemShape = RoundedCornerShape(8.dp)

    // ── 焦点指示器 ──
    val focusIndicatorWidth = 2.dp
}

/**
 * 页面背景渐变 — 取决于当前主题
 */
@Composable
fun searchBackgroundGradient(): Brush {
    return screenBackgroundGradient()
}
