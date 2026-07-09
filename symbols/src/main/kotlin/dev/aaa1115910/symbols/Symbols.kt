package dev.aaa1115910.symbols

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 字幕相关 Material Symbol 图标接口。
 *
 * 历史实现依赖 material-symbols-compose KSP 在编译期联网生成；
 * 现改为源码入库，避免网络不可用时出现：
 * `NoSuchElementException: No element of the sequence was transformed to a non-null value.`
 */
interface Subtitles {
    val Rounded: ImageVector
}

interface SubtitlesOff {
    val Rounded: ImageVector
}

interface SubtitlesGear {
    val Rounded: ImageVector
}
