package dev.aaa1115910.bv.tv.activities.user

import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.login.LoginScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class LoginActivity : TvComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                LoginScreen()
            }
        }
    }
}
