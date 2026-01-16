package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

enum class PlayerLongPressAction(private val strRes: Int) {
    OpenMenu(R.string.player_long_press_action_open_menu),
    TripleLike(R.string.player_long_press_action_triple_like);

    fun getDisplayName(context: Context) = context.getString(strRes)

    companion object {
        fun fromValue(value: Int) = entries.getOrElse(value) { OpenMenu }
    }
}
