package dev.aaa1115910.bv.player.entity

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.graphics.vector.ImageVector
import dev.aaa1115910.bv.player.shared.R

enum class VideoPlayerMenuNavItem(private val strRes: Int, val icon: ImageVector) {
    Picture(R.string.video_player_menu_nav_picture, Icons.Filled.Image),
    Danmaku(R.string.video_player_menu_nav_danmaku, Icons.AutoMirrored.Filled.FormatListBulleted),
    ClosedCaption(R.string.video_player_menu_nav_subtitle, Icons.Filled.ClosedCaption);

    fun getDisplayName(context: Context) = context.getString(strRes)
}