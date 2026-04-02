package dev.aaa1115910.bv.player.entity

import android.content.Context
import androidx.compose.ui.graphics.Color
import dev.aaa1115910.bv.R

enum class PlayerBottomProgressBarColor(val value: Int) {
    Purple(0),
    Black(1),
    White(2),
    Gold(3),
    Red(4),
    Blue(5),
    Green(6);

    fun displayName(context: Context): String = when (this) {
        Purple -> context.getString(R.string.settings_player_bottom_progress_bar_color_purple)
        Black -> context.getString(R.string.settings_player_bottom_progress_bar_color_black)
        White -> context.getString(R.string.settings_player_bottom_progress_bar_color_white)
        Gold -> context.getString(R.string.settings_player_bottom_progress_bar_color_gold)
        Red -> context.getString(R.string.settings_player_bottom_progress_bar_color_red)
        Blue -> context.getString(R.string.settings_player_bottom_progress_bar_color_blue)
        Green -> context.getString(R.string.settings_player_bottom_progress_bar_color_green)
    }

    fun toComposeColor(): Color = when (this) {
        Purple -> Color(0xFFBD26B8).copy(alpha = 0.5f)
        Black -> Color.Black.copy(alpha = 0.9f)
        White -> Color.White.copy(alpha = 0.9f)
        Gold -> Color(0xFFFFD700).copy(alpha = 0.9f)
        Red -> Color.Red.copy(alpha = 0.9f)
        Blue -> Color.Blue.copy(alpha = 0.9f)
        Green -> Color.Green.copy(alpha = 0.9f)
    }

    companion object {
        fun fromValue(value: Int): PlayerBottomProgressBarColor = entries.find { it.value == value } ?: Purple
    }
}