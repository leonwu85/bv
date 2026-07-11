package dev.aaa1115910.bv.mobile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.mobile.screen.TopicDynamicScreen
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme

class TopicDynamicActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_TOPIC_ID = "topicId"
        private const val EXTRA_TOPIC_NAME = "topicName"

        fun actionStart(context: Context, topicId: Long, topicName: String = "") {
            if (topicId <= 0L) return
            context.startActivity(
                Intent(context, TopicDynamicActivity::class.java).apply {
                    putExtra(EXTRA_TOPIC_ID, topicId)
                    putExtra(EXTRA_TOPIC_NAME, topicName)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val topicId = intent.getLongExtra(EXTRA_TOPIC_ID, 0L)
        val topicName = intent.getStringExtra(EXTRA_TOPIC_NAME).orEmpty()
        setContent {
            BVMobileTheme {
                TopicDynamicScreen(topicId = topicId, topicName = topicName)
            }
        }
    }
}
