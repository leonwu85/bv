package dev.aaa1115910.bv.player.tv

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntSize

/**
 * TV Activity 是否正在使用独立的 1080p UI Surface。
 *
 * 未提供时默认 false，因此 Mobile 和现有非 TV 调用不改变渲染路径。
 */
val LocalTvUiSurfaceEmbedded = staticCompositionLocalOf { false }

/**
 * UI 被嵌入 1080p Surface 时屏幕的物理分辨率（如 3840x2160）。
 *
 * 嵌入模式下视频 SurfaceView 也位于 1920x1080 的宿主里，缺省 buffer 只有 1080p；
 * 需要自行渲染画面的内核（MPV 的 gpu vo）应把 Surface 固定到这个尺寸才能保持原分辨率输出。
 * 未提供（null）表示无需处理。
 */
val LocalTvVideoSurfaceFixedSize = staticCompositionLocalOf<IntSize?> { null }
