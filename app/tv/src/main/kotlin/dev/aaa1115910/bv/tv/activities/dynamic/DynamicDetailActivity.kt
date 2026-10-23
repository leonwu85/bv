package dev.aaa1115910.bv.tv.activities.dynamic

import dev.aaa1115910.biliapi.entity.reply.CommentAnchor
import dev.aaa1115910.bv.util.putCommentAnchor
import dev.aaa1115910.bv.util.commentAnchor
import dev.aaa1115910.bv.tv.component.reply.CommentLinkDialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.tv.screens.DynamicDetailScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class DynamicDetailActivity : TvComponentActivity() {
    companion object {
        fun actionStart(context: Context, dynamicId: String, commentAnchor: CommentAnchor? = null) {
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
        val dynamicId = intent.getStringExtra("dynamicId") ?: ""
        setContent {
            BVTheme {
                DynamicDetailScreen(dynamicId = dynamicId)
                intent.commentAnchor()?.let { CommentLinkDialog(it, dynamicId = intent.getStringExtra("dynamicId")) }
            }
        }
    }
}
