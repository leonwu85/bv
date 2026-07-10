package dev.aaa1115910.bv.tv.activities.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.message.ContactScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class ContactActivity : TvComponentActivity() {
    companion object {
        fun actionStart(context: Context) {
            context.startActivity(Intent(context, ContactActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                ContactScreen()
            }
        }
    }
}
