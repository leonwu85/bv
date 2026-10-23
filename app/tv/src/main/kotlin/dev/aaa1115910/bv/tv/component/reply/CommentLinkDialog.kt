package dev.aaa1115910.bv.tv.component.reply

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.*
import dev.aaa1115910.biliapi.entity.reply.CommentAnchor
import dev.aaa1115910.bv.tv.component.SubCommentPanel
import dev.aaa1115910.bv.util.rememberCommentLinkState
import kotlinx.coroutines.launch

@Composable
fun CommentLinkDialog(anchor: CommentAnchor, videoId: Long = 0, dynamicId: String? = null) {
    var visible by rememberSaveable(anchor) { mutableStateOf(true) }
    if (!visible) return
    val state = rememberCommentLinkState(anchor, videoId, dynamicId)
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = { visible = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val root = state.root
        if (root != null) {
            SubCommentPanel(show = true, oid = state.oid, type = state.type, rootId = root.rpid,
                rootComment = root, onHide = { visible = false }, targetReplyId = anchor.replyId,
                initialData = state.firstPage, locateRoot = anchor.replyId == null)
        } else Surface {
            Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(state.error ?: "正在定位评论…")
                if (state.error != null) Button(onClick = { scope.launch { state.load() } }) { Text("重试") }
                Button(onClick = { visible = false }) { Text("关闭") }
            }
        }
    }
}
