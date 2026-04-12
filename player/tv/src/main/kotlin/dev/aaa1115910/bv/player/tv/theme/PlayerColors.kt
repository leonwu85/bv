package dev.aaa1115910.bv.player.tv.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * TV 播放器集中颜色系统 (Netflix 风格)
 */
@Immutable
object PlayerColors {
    // ── 强调色 ──
    val accentPink = Color(0xFFFB7299)
    val accentGold = Color(0xFFFFD54F)
    val progressPrimary = Color(0xFFBD26B8)

    // ── 渐变 ──
    val progressGradientColors = listOf(Color(0xFFBD26B8), Color(0xFF7B2FF7))
    val progressGradient = Brush.horizontalGradient(
        colors = progressGradientColors
    )

    // ── 文本 ──
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.8f)
    val textTertiary = Color.White.copy(alpha = 0.6f)
    val textGhost = Color.White.copy(alpha = 0.45f)

    // ── 控制栏背景 ──
    val controllerScrimTop = listOf(
        Color.Black.copy(alpha = 0.7f),
        Color.Black.copy(alpha = 0.35f),
        Color.Transparent,
    )
    val controllerScrimBottom = listOf(
        Color.Transparent,
        Color.Black.copy(alpha = 0.65f),
        Color.Black.copy(alpha = 0.92f),
    )

    // ── 容器/面板背景 ──
    val menuBackground = Color.Black.copy(alpha = 0.85f)
    val dialogBackground = Color.Black.copy(alpha = 0.8f)
    val tipBackground = Color.Black.copy(alpha = 0.6f)
    val listBackground = Color.Black.copy(alpha = 0.85f)
    val audioModeBackground = Color.Black.copy(alpha = 0.55f)

    // ── 按钮/项目状态 ──
    val buttonDefault = Color.Transparent
    val buttonFocused = Color.White.copy(alpha = 0.15f)
    val buttonSelected = Color.White.copy(alpha = 0.12f)
    val buttonFocusedBorder = Color.White.copy(alpha = 0.5f)
    val buttonAlwaysShowBorder = Color.White.copy(alpha = 0.45f)

    // ── 菜单项状态 ──
    val menuItemFocused = Color.White.copy(alpha = 0.12f)
    val menuItemIndicator = accentPink

    // ── 毛玻璃设置面板 ──
    val menuGlassBackground = Color(0xFF1a1a2e).copy(alpha = 0.7f)
    val menuGlassOverlay = Color.Black.copy(alpha = 0.4f)
    val menuGlassBorder = Color.White.copy(alpha = 0.08f)
    val menuGlassHighlightStart = Color.White.copy(alpha = 0.05f)
    val menuGlassHighlightEnd = Color.Transparent
    val menuNavBackground = Color.Black.copy(alpha = 0.3f)
    val menuItemFocusedGradientStart = accentPink.copy(alpha = 0.15f)
    val menuItemFocusedGradientEnd = Color.Transparent
    val menuItemIndicatorGlow = accentPink.copy(alpha = 0.3f)

    // ── 进度条 ──
    val seekBarTrack = Color.White.copy(alpha = 0.1f)
    val seekBarBuffered = Color.White.copy(alpha = 0.2f)
    val seekBarFocusBorder = Color.White.copy(alpha = 0.35f)
    val seekBarThumb = Color.White

    // ── 底部常驻进度条 ──
    val bottomProgressBar = Color(0xFFBD26B8).copy(alpha = 0.5f)

    // ── 观看人数等辅助 ──
    val viewerCountTint = Color.White.copy(alpha = 0.5f)
    val onlineViewerCountTint = Color.White.copy(alpha = 0.3f)
}
