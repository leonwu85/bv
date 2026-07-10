package dev.aaa1115910.bv.tv.activities.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.message.InboxScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class InboxActivity : TvComponentActivity() {
    companion object {
        fun actionStart(context: Context) {
            context.startActivity(Intent(context, InboxActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                InboxScreen()
            }
        }
    }
}
