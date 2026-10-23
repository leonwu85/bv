package dev.aaa1115910.bv.mobile.component.reply

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.rememberPreviewerState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentAnchor
import dev.aaa1115910.biliapi.repositories.CommentRepository
import dev.aaa1115910.bv.util.rememberCommentLinkState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentLinkDialog(anchor: CommentAnchor, videoId: Long = 0, dynamicId: String? = null) {
    var visible by rememberSaveable(anchor) { mutableStateOf(true) }
    if (!visible) return
    val state = rememberCommentLinkState(anchor, videoId, dynamicId)
    val scope = rememberCoroutineScope()
    val repository = getKoin().get<CommentRepository>()
    val listState = rememberLazyListState()
    var located by remember(anchor) { mutableStateOf(false) }
    val pictures = remember { mutableStateListOf<Picture>() }
    val previewer = rememberPreviewerState(pageCount = { pictures.size }, getKey = { pictures[it].key })
    var replyTo by remember { mutableStateOf<Comment?>(null) }
    var text by remember { mutableStateOf("") }
    var sendError by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    LaunchedEffect(state.replies.size, state.root) {
        if (!located && state.root != null) {
            val index = state.replies.indexOfFirst { it.rpid == anchor.replyId }
            listState.scrollToItem(if (index >= 0) index + 1 else 0)
            located = true
        }
    }
    ModalBottomSheet(onDismissRequest = { visible = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text("定位评论", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        if (state.targetUnavailable) Text("目标回复已删除或暂不可见，已显示主评论", modifier = Modifier.padding(16.dp))
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().fillMaxHeight(.85f)) {
            val root = state.root
            if (root != null) item(key = root.rpid) {
                CommentItem(comment = root, previewerState = previewer, showReplies = false,
                    containerColor = if (anchor.replyId == null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    onShowPreviewer = { new, after -> pictures.clear(); pictures.addAll(new); after() }, onReply = { replyTo = it })
            }
            items(state.replies, key = { it.rpid }) { reply ->
                CommentItem(comment = reply, previewerState = previewer, showReplies = false,
                    containerColor = if (reply.rpid == anchor.replyId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    onShowPreviewer = { new, after -> pictures.clear(); pictures.addAll(new); after() }, onReply = { replyTo = it })
            }
            item {
                if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                state.error?.let { Text(it, modifier = Modifier.padding(16.dp)) }
                if (!state.loading && (state.hasNext || state.error != null)) TextButton(onClick = { scope.launch { state.load() } }) {
                    Text(if (state.error != null) "重试" else "加载更多回复")
                }
            }
        }
        ImagePreviewer(state = previewer, imageLoader = { rememberAsyncImagePainter(pictures[it].url) })
    }
    replyTo?.let { target ->
        AlertDialog(onDismissRequest = { if (!sending) replyTo = null }, title = { Text("回复 ${target.member.name}") },
            text = { Column { OutlinedTextField(value = text, onValueChange = { text = it }, enabled = !sending); sendError?.let { Text(it) } } },
            dismissButton = { TextButton(onClick = { replyTo = null }, enabled = !sending) { Text("取消") } },
            confirmButton = { TextButton(enabled = !sending && text.isNotBlank(), onClick = {
                scope.launch {
                    sending = true
                    try {
                        repository.addComment(state.type, state.oid, text, root = anchor.rootId, parent = target.rpid)
                        replyTo = null; text = ""; sendError = null
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { sendError = e.message ?: "发送失败" }
                    finally { sending = false }
                }
            }) { Text(if (sending) "发送中" else "发送") } })
    }
}
