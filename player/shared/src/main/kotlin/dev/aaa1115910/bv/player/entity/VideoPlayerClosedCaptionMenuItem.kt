package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

enum class VideoPlayerClosedCaptionMenuItem(
    private val strRes: Int,
    val isSecondary: Boolean = false
) {
    Switch(R.string.video_player_menu_subtitle_switch),
    Size(R.string.video_player_menu_subtitle_size),
    Opacity(R.string.video_player_menu_subtitle_background_opacity),
    Padding(R.string.video_player_menu_subtitle_bottom_padding),
    SecondarySwitch(R.string.video_player_menu_secondary_subtitle_switch, true),
    SecondarySize(R.string.video_player_menu_secondary_subtitle_size, true),
    SecondaryOpacity(R.string.video_player_menu_secondary_subtitle_background_opacity, true),
    SecondaryPadding(R.string.video_player_menu_secondary_subtitle_bottom_padding, true);

    fun getDisplayName(context: Context) = context.getString(strRes)
}