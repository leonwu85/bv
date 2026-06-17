package dev.aaa1115910.bv.tv.activities.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.message.ConversationScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class ConversationActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_TALKER_ID = "talker_id"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_FACE = "face"

        fun actionStart(
            context: Context,
            talkerId: Long,
            name: String,
            face: String = ""
        ) {
            context.startActivity(
                Intent(context, ConversationActivity::class.java).apply {
                    putExtra(EXTRA_TALKER_ID, talkerId)
                    putExtra(EXTRA_NAME, name)
                    putExtra(EXTRA_FACE, face)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                ConversationScreen(
                    talkerId = intent.getLongExtra(EXTRA_TALKER_ID, 0L),
                    name = intent.getStringExtra(EXTRA_NAME).orEmpty(),
                    face = intent.getStringExtra(EXTRA_FACE).orEmpty()
                )
            }
        }
    }
}
