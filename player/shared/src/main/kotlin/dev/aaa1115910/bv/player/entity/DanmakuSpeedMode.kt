package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

enum class DanmakuSpeedMode(private val strRes: Int) {
    FollowVideo(R.string.video_player_danmaku_speed_mode_follow_video),
    ReadingFirst(R.string.video_player_danmaku_speed_mode_reading_first),
    Custom(R.string.video_player_danmaku_speed_mode_custom);

    fun getDisplayName(context: Context) = context.getString(strRes)

    companion object {
        fun fromOrdinal(ordinal: Int): DanmakuSpeedMode {
            return entries.getOrElse(ordinal) { FollowVideo }
        }
    }
}
