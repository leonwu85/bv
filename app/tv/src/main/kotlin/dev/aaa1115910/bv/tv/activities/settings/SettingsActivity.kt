package dev.aaa1115910.bv.tv.activities.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class SettingsActivity : TvComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialMenu = intent.getStringExtra(EXTRA_INITIAL_MENU)
            ?.let { runCatching { SettingsMenuNavItem.valueOf(it) }.getOrNull() }
            ?: SettingsMenuNavItem.Player
        setContent {
            BVTheme {
                SettingsScreen(initialMenu = initialMenu)
            }
        }
    }

    companion object {
        private const val EXTRA_INITIAL_MENU = "initial_menu"

        fun createIntent(
            context: Context,
            initialMenu: SettingsMenuNavItem = SettingsMenuNavItem.Player
        ): Intent {
            return Intent(context, SettingsActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_MENU, initialMenu.name)
            }
        }
    }
}
