package dev.aaa1115910.bv.tv.activities.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import dev.aaa1115910.bv.tv.screens.user.OfflineCacheScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class OfflineCacheActivity : TvComponentActivity() {
    companion object {
        fun actionStart(context: Context) {
            context.startActivity(Intent(context, OfflineCacheActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                OfflineCacheScreen(onBack = ::finish)
            }
        }
    }
}
