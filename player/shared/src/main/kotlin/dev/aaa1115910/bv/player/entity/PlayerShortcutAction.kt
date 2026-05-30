package dev.aaa1115910.bv.player.entity

enum class PlayerShortcutAction(val value: Int) {
    ToggleDanmaku(0),
    ToggleComment(1),
    ToggleSubtitle(2),
    TripleLike(3),
    ToggleRelatedVideos(4);

    companion object {
        fun fromValue(value: Int): PlayerShortcutAction =
            entries.find { it.value == value } ?: ToggleDanmaku
    }
}
