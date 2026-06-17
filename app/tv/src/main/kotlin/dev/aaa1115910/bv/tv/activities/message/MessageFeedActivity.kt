package dev.aaa1115910.bv.tv.activities.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.biliapi.entity.message.MessageFeedType
import dev.aaa1115910.bv.tv.screens.message.MessageFeedScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class MessageFeedActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_TYPE = "type"

        fun actionStart(context: Context, type: MessageFeedType) {
            context.startActivity(
                Intent(context, MessageFeedActivity::class.java).apply {
                    putExtra(EXTRA_TYPE, type.name)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = runCatching {
            MessageFeedType.valueOf(intent.getStringExtra(EXTRA_TYPE).orEmpty())
        }.getOrDefault(MessageFeedType.Reply)
        setContent {
            BVTheme {
                MessageFeedScreen(type = type)
            }
        }
    }
}
