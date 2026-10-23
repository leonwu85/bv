package dev.aaa1115910.bv.mobile.activities

import dev.aaa1115910.biliapi.entity.reply.CommentAnchor
import dev.aaa1115910.bv.util.putCommentAnchor
import dev.aaa1115910.bv.util.commentAnchor
import dev.aaa1115910.bv.mobile.component.reply.CommentLinkDialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.mobile.screen.DynamicDetailScreen
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import io.github.oshai.kotlinlogging.KotlinLogging

class DynamicDetailActivity : ComponentActivity() {
    companion object {
        private val logger = KotlinLogging.logger { }

        fun actionStart(context: Context, dynamicId: String, commentAnchor: CommentAnchor? = null) {
            logger.info { "actionStart: dynamicId=$dynamicId" }
            context.startActivity(
                Intent(context, DynamicDetailActivity::class.java).apply {
                    putExtra("dynamicId", dynamicId)
                    putCommentAnchor(commentAnchor)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVMobileTheme {
                DynamicDetailScreen()
                intent.commentAnchor()?.let { CommentLinkDialog(it, dynamicId = intent.getStringExtra("dynamicId")) }
            }
        }
    }
}