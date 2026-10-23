package dev.aaa1115910.bv.mobile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.biliapi.entity.reply.CommentAnchor
import dev.aaa1115910.bv.component.LegacyArticle
import dev.aaa1115910.bv.mobile.component.reply.CommentLinkDialog
import dev.aaa1115910.bv.mobile.util.openBiliContent
import dev.aaa1115910.bv.util.commentAnchor
import dev.aaa1115910.bv.util.putCommentAnchor
import kotlinx.coroutines.launch

class ArticleActivity : ComponentActivity() {
    companion object {
        fun actionStart(context: Context, id: Long, anchor: CommentAnchor?) {
            context.startActivity(Intent(context, ArticleActivity::class.java).putExtra("id", id).putCommentAnchor(anchor?.copy(oid = id, type = 12)))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BVMobileTheme {
            LegacyArticle(intent.getLongExtra("id", 0), false, ::finish,
                onOpen = { link -> lifecycleScope.launch { openBiliContent(this@ArticleActivity, link) } })
            intent.commentAnchor()?.let { CommentLinkDialog(it) }
        } }
    }
}
