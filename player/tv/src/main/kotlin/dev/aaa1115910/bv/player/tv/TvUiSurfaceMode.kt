package dev.aaa1115910.bv.player.tv

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * TV Activity 是否正在使用独立的 1080p UI Surface。
 *
 * 未提供时默认 false，因此 Mobile 和现有非 TV 调用不改变渲染路径。
 */
val LocalTvUiSurfaceEmbedded = staticCompositionLocalOf { false }
