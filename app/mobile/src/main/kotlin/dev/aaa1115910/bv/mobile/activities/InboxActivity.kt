package dev.aaa1115910.bv.mobile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.mobile.screen.message.InboxScreen
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme

class InboxActivity : ComponentActivity() {
    companion object {
        fun actionStart(context: Context) {
            context.startActivity(Intent(context, InboxActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVMobileTheme {
                InboxScreen()
            }
        }
    }
}
