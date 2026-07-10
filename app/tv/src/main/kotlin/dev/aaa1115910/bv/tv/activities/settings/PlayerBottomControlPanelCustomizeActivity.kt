package dev.aaa1115910.bv.tv.activities.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.settings.PlayerBottomControlPanelCustomizeScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class PlayerBottomControlPanelCustomizeActivity : TvComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BVTheme {
                PlayerBottomControlPanelCustomizeScreen()
            }
        }
    }

    companion object {
        fun actionStart(context: Context) {
            context.startActivity(Intent(context, PlayerBottomControlPanelCustomizeActivity::class.java))
        }
    }
}
