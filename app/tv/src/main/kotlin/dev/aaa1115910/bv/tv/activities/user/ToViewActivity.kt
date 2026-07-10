package dev.aaa1115910.bv.tv.activities.user

import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
// import dev.aaa1115910.bv.screen.user.HistoryScreen
import dev.aaa1115910.bv.tv.screens.user.ToViewScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class ToViewActivity : TvComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                // HistoryScreen()
                ToViewScreen()
            }
        }
    }
}
