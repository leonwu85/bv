package dev.aaa1115910.bv.mobile.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.mobile.screen.settings.SettingsScreen
import dev.aaa1115910.bv.mobile.settings.MobileRuntime
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        MobileRuntime.install()
        super.onCreate(savedInstanceState)
        setContent {
            BVMobileTheme {
                SettingsScreen()
            }
        }
    }
}
