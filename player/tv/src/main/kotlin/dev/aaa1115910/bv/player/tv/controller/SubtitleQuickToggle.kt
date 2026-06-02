package dev.aaa1115910.bv.player.tv.controller

import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleType

internal fun List<Subtitle>.preferredSubtitleForQuickToggle(): Subtitle? {
    val subtitles = filter { it.id != -1L }
    return subtitles.firstOrNull { it.isChineseSubtitle() && !it.isAiSubtitle() }
        ?: subtitles.firstOrNull { it.isChineseSubtitle() && it.isAiSubtitle() }
        ?: subtitles.firstOrNull { !it.isAiSubtitle() }
        ?: subtitles.firstOrNull { it.isAiSubtitle() }
}

private fun Subtitle.isAiSubtitle(): Boolean {
    return type == SubtitleType.AI ||
            lang.startsWith("ai-", ignoreCase = true) ||
            langDoc.startsWith("ai-", ignoreCase = true)
}

private fun Subtitle.isChineseSubtitle(): Boolean {
    fun isChineseCode(value: String): Boolean {
        val normalized = value.lowercase().removePrefix("ai-")
        return normalized == "zh" ||
                normalized.startsWith("zh-") ||
                normalized.startsWith("zh_")
    }

    return isChineseCode(lang) ||
            isChineseCode(langDoc) ||
            lang.contains("中文") ||
            langDoc.contains("中文")
}
