package dev.aaa1115910.bv.tv.screens.search

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.aaa1115910.bv.tv.component.screenBackgroundGradient

/**
 * 搜索页面专用视觉常量
 */
object SearchTheme {
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

    @Composable
    private fun useDarkGlass(): Boolean {
        return MaterialTheme.colorScheme.surface.luminance() < 0.5f
    }

    @Composable
    fun glassBackgroundColor(): Color {
        return if (useDarkGlass()) {
            Color(0xFF1A1A2E).copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        }
    }

    @Composable
    fun glassOverlayColor(): Color {
        return if (useDarkGlass()) {
            Color.Black.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        }
    }

    @Composable
    fun glassBorderColor(): Color {
        return if (useDarkGlass()) {
            Color.White.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        }
    }

    @Composable
    fun glassHighlightStartColor(): Color {
        return if (useDarkGlass()) {
            Color.White.copy(alpha = 0.05f)
        } else {
            Color.White.copy(alpha = 0.72f)
        }
    }

    @Composable
    fun glassHighlightEndColor(): Color {
        return if (useDarkGlass()) {
            Color.Transparent
        } else {
            Color.White.copy(alpha = 0.14f)
        }
    }
}

/**
 * 页面背景渐变 — 取决于当前主题
 */
@Composable
fun searchBackgroundGradient(): Brush {
    return screenBackgroundGradient()
}
