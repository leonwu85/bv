package dev.aaa1115910.bv.r8test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.player.entity.PlayerLongPressAction
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.util.Prefs

/** Shell-only configuration hook packaged exclusively in the r8Test variant. */
class R8TestControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestedPlayer = intent.getStringExtra(EXTRA_PLAYER_TYPE)
        if (requestedPlayer != null) {
            val playerType = PlayerType.entries.firstOrNull {
                it.name.equals(requestedPlayer, ignoreCase = true)
            } ?: error("Unknown player type: $requestedPlayer")
            Prefs.playerType = playerType
        }

        if (intent.hasExtra(EXTRA_QUALITY_CODE)) {
            val qualityCode = intent.getIntExtra(EXTRA_QUALITY_CODE, Resolution.R1080P.code)
            val resolution = Resolution.fromCode(qualityCode)
                ?: error("Unknown resolution code: $qualityCode")
            Prefs.defaultQuality = resolution
        }

        val requestedLongPressAction = intent.getStringExtra(EXTRA_LONG_PRESS_ACTION)
        if (requestedLongPressAction != null) {
            val longPressAction = PlayerLongPressAction.entries.firstOrNull {
                it.name.equals(requestedLongPressAction, ignoreCase = true)
            } ?: error("Unknown long-press action: $requestedLongPressAction")
            Prefs.playerLongPressAction = longPressAction
        }

        Log.i(
            TAG,
            "Configured player=${Prefs.playerType}, defaultQuality=${Prefs.defaultQuality}, " +
                "longPressAction=${Prefs.playerLongPressAction}",
        )
    }

    private companion object {
        const val TAG = "R8TestControl"
        const val EXTRA_PLAYER_TYPE = "player_type"
        const val EXTRA_QUALITY_CODE = "quality_code"
        const val EXTRA_LONG_PRESS_ACTION = "long_press_action"
    }
}
