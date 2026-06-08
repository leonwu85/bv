package dev.aaa1115910.bv.mobile.component.reply

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.rememberPreviewerState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.EmoteSize
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.mobile.component.videocard.shareText
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommentItem(
    modifier: Modifier = Modifier,
    comment: Comment,
    previewerState: ImagePreviewerState,
    showReplies: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    onReply: (Comment) -> Unit = {},
    onShowReply: (rpid: Long) -> Unit = {},
    commentViewModel: CommentViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentComment by remember(comment) { mutableStateOf(comment) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }
    var showFreeCopyDialog by remember { mutableStateOf(false) }
    var actionInProgress by remember { mutableStateOf(false) }

    fun requireLogin(): Boolean {
        if (!Prefs.isLogin) {
            "账号未登录".toast(context)
            return false
        }
        return true
    }

    fun runCommentMutation(
        successText: String,
        action: suspend (Comment) -> Result<Comment>
    ) {
        if (!requireLogin() || actionInProgress) return
        val targetComment = currentComment
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                actionInProgress = true
            }
            val result = runCatching { action(targetComment) }
                .getOrElse { Result.failure(it) }
            withContext(Dispatchers.Main) {
                result
                    .onSuccess { updatedComment ->
                        currentComment = updatedComment
                        successText.toast(context)
                    }
                    .onFailure {
                        (it.localizedMessage ?: "操作失败").toast(context)
                    }
                actionInProgress = false
            }
        }
    }

    fun runUnitAction(
        successText: String,
        action: suspend () -> Result<Unit>
    ) {
        if (!requireLogin() || actionInProgress) return
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                actionInProgress = true
            }
            val result = runCatching { action() }
                .getOrElse { Result.failure(it) }
            withContext(Dispatchers.Main) {
                result
                    .onSuccess { successText.toast(context) }
                    .onFailure {
                        (it.localizedMessage ?: "操作失败").toast(context)
                    }
                actionInProgress = false
            }
        }
    }

    Surface(
        modifier = modifier.combinedClickable(
            enabled = true,
            onClick = {},
            onLongClick = { showMoreSheet = true }
        ),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row {
                AsyncImage(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    model = currentComment.member.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            modifier = Modifier
                                .width(200.dp)
                                .basicMarquee(),
                            text = currentComment.member.name
                        )
                        Text(
                            text = currentComment.timeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { showMoreSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "更多"
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(start = 72.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommentText(
                    content = currentComment.displayContent,
                    emotes = currentComment.displayEmotes
                )
                if (currentComment.pictures.isNotEmpty()) {
                    CommentPictures(
                        pictures = currentComment.pictures,
                        previewerState = previewerState,
                        onShowPreviewer = onShowPreviewer
                    )
                }
                if (showReplies && (currentComment.repliesCount != 0 || currentComment.replies.isNotEmpty())) {
                    CommentReplies(
                        replies = currentComment.replies,
                        repliesCount = currentComment.repliesCount,
                        onOpenCommentSheet = { onShowReply(currentComment.rpid) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        modifier = Modifier.height(32.dp),
                        onClick = { onReply(currentComment) }
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = "回复"
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        modifier = Modifier.height(32.dp),
                        enabled = !actionInProgress,
                        onClick = {
                            runCommentMutation(
                                if (currentComment.isDisliked) "已取消点踩" else "点踩成功",
                                commentViewModel::toggleCommentDislike
                            )
                        }
                    ) {
                        val color = if (currentComment.isDisliked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = if (currentComment.isDisliked) {
                                Icons.Rounded.ThumbDown
                            } else {
                                Icons.Outlined.ThumbDown
                            },
                            contentDescription = if (currentComment.isDisliked) "已踩" else "点踩",
                            tint = color
                        )
                    }
                    TextButton(
                        modifier = Modifier.height(32.dp),
                        enabled = !actionInProgress,
                        onClick = {
                            runCommentMutation(
                                if (currentComment.isLiked) "已取消点赞" else "点赞成功",
                                commentViewModel::toggleCommentLike
                            )
                        }
                    ) {
                        val color = if (currentComment.isLiked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = if (currentComment.isLiked) {
                                Icons.Rounded.ThumbUp
                            } else {
                                Icons.Outlined.ThumbUp
                            },
                            contentDescription = if (currentComment.isLiked) "已赞" else "点赞",
                            tint = color
                        )
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = currentComment.like.takeIf { it > 0 }?.toString() ?: "赞",
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { shareText(context, currentComment.toShareText(), "分享评论") }
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "分享"
                        )
                    }
                }
            }
        }
    }

    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                CommentSheetItem(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    text = "回复",
                    onClick = {
                        showMoreSheet = false
                        onReply(currentComment)
                    }
                )
                CommentSheetItem(
                    icon = Icons.Rounded.AccountCircle,
                    text = "访问：${currentComment.member.name}",
                    onClick = {
                        showMoreSheet = false
                        if (currentComment.member.mid > 0L) {
                            UserSpaceActivity.actionStart(
                                context = context,
                                mid = currentComment.member.mid,
                                name = currentComment.member.name
                            )
                        } else {
                            "未获取到用户信息".toast(context)
                        }
                    }
                )
                CommentSheetItem(
                    icon = Icons.Rounded.ContentCopy,
                    text = "复制评论",
                    onClick = {
                        showMoreSheet = false
                        copyTextToClipboard(context, "评论", currentComment.toPlainText())
                        "已复制评论".toast(context)
                    }
                )
                CommentSheetItem(
                    icon = Icons.Rounded.ContentCopy,
                    text = "自由复制",
                    onClick = {
                        showMoreSheet = false
                        showFreeCopyDialog = true
                    }
                )
                CommentSheetItem(
                    icon = Icons.Rounded.Share,
                    text = "分享评论",
                    onClick = {
                        showMoreSheet = false
                        shareText(context, currentComment.toShareText(), "分享评论")
                    }
                )
                CommentSheetItem(
                    icon = Icons.Rounded.Block,
                    text = "加入黑名单：${currentComment.member.name}",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        showMoreSheet = false
                        if (requireLogin()) showBlacklistDialog = true
                    }
                )
                CommentSheetItem(
                    icon = Icons.Rounded.Report,
                    text = "举报",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        showMoreSheet = false
                        if (requireLogin()) showReportDialog = true
                    }
                )
            }
        }
    }

    if (showFreeCopyDialog) {
        AlertDialog(
            onDismissRequest = { showFreeCopyDialog = false },
            title = { Text(text = "自由复制") },
            text = {
                SelectionContainer {
                    Text(
                        text = currentComment.toPlainText(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFreeCopyDialog = false }) {
                    Text(text = "完成")
                }
            }
        )
    }

    if (showBlacklistDialog) {
        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            title = { Text(text = "加入黑名单") },
            text = {
                Text(
                    text = "确定将 ${currentComment.member.name}(${currentComment.member.mid}) 加入黑名单？\n\n可在 B 站隐私设置的黑名单管理中解除。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlacklistDialog = false
                        runUnitAction("已加入黑名单：${currentComment.member.name}") {
                            commentViewModel.blacklistCommentUser(currentComment)
                        }
                    }
                ) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlacklistDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }

    if (showReportDialog) {
        CommentReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reasonType, reasonDesc, addBlacklist ->
                showReportDialog = false
                runUnitAction("举报成功") {
                    commentViewModel.reportComment(
                        comment = currentComment,
                        reasonType = reasonType,
                        reasonDesc = reasonDesc,
                        addBlacklist = addBlacklist
                    )
                }
            }
        )
    }
}

@Composable
private fun CommentSheetItem(
    icon: ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

private data class CommentReportReason(
    val type: Int,
    val label: String
)

private val commentReportReasonGroups = listOf(
    "违反法律法规" to listOf(
        CommentReportReason(9, "违法违规"),
        CommentReportReason(2, "色情"),
        CommentReportReason(10, "低俗"),
        CommentReportReason(12, "赌博诈骗"),
        CommentReportReason(23, "违法信息外链")
    ),
    "谣言类不实信息" to listOf(
        CommentReportReason(19, "涉政谣言"),
        CommentReportReason(22, "虚假不实信息"),
        CommentReportReason(20, "涉社会事件谣言")
    ),
    "侵犯个人权益" to listOf(
        CommentReportReason(7, "人身攻击"),
        CommentReportReason(15, "侵犯隐私")
    ),
    "有害社区环境" to listOf(
        CommentReportReason(1, "垃圾广告"),
        CommentReportReason(4, "引战"),
        CommentReportReason(5, "剧透"),
        CommentReportReason(3, "刷屏"),
        CommentReportReason(8, "视频不相关"),
        CommentReportReason(18, "违规抽奖"),
        CommentReportReason(17, "青少年不良信息")
    ),
    "其他" to listOf(CommentReportReason(0, "其他"))
)

@Composable
private fun CommentReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (reasonType: Int, reasonDesc: String?, addBlacklist: Boolean) -> Unit
) {
    var selectedReason by remember { mutableStateOf<CommentReportReason?>(null) }
    var reasonDesc by remember { mutableStateOf("") }
    var addBlacklist by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "举报") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = "请选择举报理由：",
                    style = MaterialTheme.typography.bodyMedium
                )
                commentReportReasonGroups.forEach { (group, reasons) ->
                    Text(
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                        text = group,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Text(text = reason.label)
                        }
                    }
                }
                if (selectedReason?.type == 0) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        value = reasonDesc,
                        onValueChange = { reasonDesc = it },
                        label = { Text(text = "补充说明") },
                        minLines = 2,
                        maxLines = 4
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addBlacklist = !addBlacklist }
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = addBlacklist,
                        onCheckedChange = { addBlacklist = it }
                    )
                    Text(text = "同时加入黑名单")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reason = selectedReason ?: return@TextButton
                    if (reason.type == 0 && reasonDesc.isBlank()) {
                        return@TextButton
                    }
                    onSubmit(reason.type, reasonDesc.takeIf { reason.type == 0 }, addBlacklist)
                }
            ) {
                Text(text = "确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

private fun copyTextToClipboard(
    context: Context,
    label: String,
    text: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun Comment.toPlainText(): String = displayContent.joinToString("")

private fun Comment.toShareText(): String {
    val link = shareUrl()
    val text = "${member.name} 的评论：\n${toPlainText()}"
    return if (link == null) text else "$text\n$link"
}

private fun Comment.shareUrl(): String? = when (type) {
    1L -> "https://www.bilibili.com/video/av$oid#reply$rpid"
    17L -> "https://t.bilibili.com/$oid#reply$rpid"
    else -> null
}

@Composable
private fun CommentText(
    modifier: Modifier = Modifier,
    content: List<String>,
    emotes: List<Comment.Emote>,
    maxLines: Int = 6,
    showMoreButton: Boolean = true
) {
    val emoteNameList = emotes.map { it.text }
    val inlineContentMap = emotes.map { emote ->
        emote.text to InlineTextContent(
            Placeholder(
                width = emote.size.fontSize.sp,
                height = emote.size.fontSize.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AsyncImage(model = emote.url, contentDescription = null)
        }
    }.toMap()

    var lineCount by remember { mutableIntStateOf(0) }
    var maxLinesValue by remember { mutableIntStateOf(maxLines) }
    val currentMaxLines by animateIntAsState(targetValue = maxLinesValue, label = "text max line")
    var textMoreThan6Lines by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
    ) {
        Text(
            text = buildAnnotatedString {
                content.forEach { text ->
                    if (emoteNameList.contains(text)) {
                        appendInlineContent(text)
                    } else {
                        append(text)
                    }
                }
            },
            inlineContent = inlineContentMap,
            maxLines = currentMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.hasVisualOverflow) textMoreThan6Lines = true
                lineCount = textLayoutResult.lineCount
            }
        )
        if (showMoreButton && textMoreThan6Lines) {
            if (maxLinesValue == maxLines) {
                Text(
                    modifier = Modifier.clickable { maxLinesValue = 999 },
                    text = "展开",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    modifier = Modifier.clickable { maxLinesValue = 6 },
                    text = "收起",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CommentPictures(
    modifier: Modifier = Modifier,
    pictures: List<Picture>,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val imageBaseShape = MaterialTheme.shapes.medium

    val onClickPicture: (index: Int) -> Unit = { index ->
        onShowPreviewer(pictures) {
            scope.launch {
                previewerState.open(index = index)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        when {
            pictures.size == 1 -> {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(2f),
                        color = Color.Gray,
                        shape = imageBaseShape
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f)
                                .clickable { onClickPicture(0) },
                            model = pictures.first().url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            pictures.size == 2 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pictures.forEachIndexed { index, picture ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            color = Color.Gray,
                            shape = when (index) {
                                0 -> imageBaseShape.copy(
                                    topEnd = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)
                                )

                                1 -> imageBaseShape.copy(
                                    topStart = CornerSize(0.dp), bottomStart = CornerSize(0.dp)
                                )

                                else -> RoundedCornerShape(0.dp)
                            }
                        ) {
                            AsyncImage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clickable { onClickPicture(index) },
                                model = picture.url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            pictures.size >= 3 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pictures.take(3).forEachIndexed { index, picture ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            color = Color.Gray,
                            shape = when (index) {
                                0 -> imageBaseShape.copy(
                                    topEnd = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)
                                )

                                2 -> imageBaseShape.copy(
                                    topStart = CornerSize(0.dp), bottomStart = CornerSize(0.dp)
                                )

                                else -> RoundedCornerShape(0.dp)
                            }
                        ) {
                            AsyncImage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clickable { onClickPicture(index) },
                                model = picture.url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                if (pictures.size > 3) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(
                                MaterialTheme.shapes.medium.copy(
                                    topEnd = CornerSize(0.dp),
                                    bottomStart = CornerSize(0.dp)
                                )
                            )
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp),
                        text = "+${pictures.size - 3}",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CommentReplies(
    modifier: Modifier = Modifier,
    replies: List<Comment>,
    repliesCount: Int,
    onOpenCommentSheet: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        onClick = onOpenCommentSheet,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            replies.forEach { reply ->
                val replyContent = if (reply.content.firstOrNull()?.startsWith("回复") == true) {
                    listOf("${reply.member.name} ")
                } else {
                    listOf("${reply.member.name} : ")
                } + reply.content
                CommentText(
                    content = replyContent,
                    emotes = reply.emotes,
                    maxLines = 2,
                    showMoreButton = false
                )
            }
            if (repliesCount > replies.size) {
                Text(
                    text = "共 $repliesCount 条回复",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private class CommentItemPreviewParameterProvider :
    PreviewParameterProvider<Comment> {
    override val values = sequenceOf(
        Comment(
            rpid = 0,
            mid = 0,
            oid = 0,
            parent = 0,
            type = 0,
            content = listOf("单行文字。你好", "[doge]", "World!"),
            member = Comment.Member(mid = 0, avatar = "", name = "username"),
            timeDesc = "4小时前",
            emotes = listOf(
                Comment.Emote(
                    text = "[doge]",
                    url = "https://i0.hdslb.com/bfs/emote/3087d273a78ccaff4bb1e9972e2ba2a7583c9f11.png",
                    size = EmoteSize.Small
                )
            ),
            pictures = emptyList(),
            replies = emptyList(),
            repliesCount = 0
        ),
        Comment(
            rpid = 0,
            mid = 0,
            oid = 0,
            parent = 0,
            type = 0,
            content = listOf("超长评论。If you were a web designer in the early days of the Internet, you might remember that there were few “web safe” typefaces, such as Arial and Georgia. As a result, many websites looked similar. To use a new typeface, you had to embed small Flash files for each heading in your layout."),
            member = Comment.Member(
                mid = 0,
                avatar = "",
                name = "超长用户名 超长用户名 超长用户名 超长用户名"
            ),
            timeDesc = "4小时前",
            emotes = listOf(
                Comment.Emote(
                    text = "[doge]",
                    url = "https://i0.hdslb.com/bfs/emote/3087d273a78ccaff4bb1e9972e2ba2a7583c9f11.png",
                    size = EmoteSize.Small
                )
            ),
            pictures = emptyList(),
            replies = emptyList(),
            repliesCount = 0
        ),
        Comment(
            rpid = 0,
            mid = 0,
            oid = 0,
            parent = 0,
            type = 0,
            content = listOf("单图片, 1 picture."),
            member = Comment.Member(mid = 0, avatar = "", name = "username"),
            timeDesc = "4小时前",
            emotes = emptyList(),
            pictures = listOf(
                Picture(
                    url = "",
                    width = 0,
                    height = 0,
                    key = ""
                )
            ),
            replies = emptyList(),
            repliesCount = 0
        ),
        Comment(
            rpid = 0,
            mid = 0,
            oid = 0,
            parent = 0,
            type = 0,
            content = listOf("双图片, 2 pictures."),
            member = Comment.Member(mid = 0, avatar = "", name = "username"),
            timeDesc = "4小时前",
            emotes = emptyList(),
            pictures = listOf(
                Picture(url = "", width = 0, height = 0, key = "1"),
                Picture(url = "", width = 0, height = 0, key = "2")
            ),
            replies = emptyList(),
            repliesCount = 0
        ),
        Comment(
            rpid = 0,
            mid = 0,
            oid = 0,
            parent = 0,
            type = 0,
            content = listOf("三图片, 3 pictures."),
            member = Comment.Member(mid = 0, avatar = "", name = "username"),
            timeDesc = "4小时前",
            emotes = emptyList(),
            pictures = listOf(
                Picture(url = "", width = 0, height = 0, key = "1"),
                Picture(url = "", width = 0, height = 0, key = "2"),
                Picture(url = "", width = 0, height = 0, key = "3")
            ),
            replies = emptyList(),
            repliesCount = 0
        ),
        Comment(
            rpid = 0,
            mid = 0,
            oid = 0,
            parent = 0,
            type = 0,
            content = listOf("四图片, four pictures."),
            member = Comment.Member(mid = 0, avatar = "", name = "username"),
            timeDesc = "4小时前",
            emotes = emptyList(),
            pictures = listOf(
                Picture(url = "", width = 0, height = 0, key = "1"),
                Picture(url = "", width = 0, height = 0, key = "2"),
                Picture(url = "", width = 0, height = 0, key = "3"),
                Picture(url = "", width = 0, height = 0, key = "4")
            ),
            replies = emptyList(),
            repliesCount = 0
        ),
        Comment(
            rpid = 0,
            mid = 0,
            oid = 0,
            parent = 0,
            type = 0,
            content = listOf("先兼容后慢慢过渡到完全自主，虽然看起来像安卓套壳，但能避免跨度太大扯到蛋。"),
            member = Comment.Member(mid = 0, avatar = "", name = "username"),
            timeDesc = "4小时前",
            emotes = emptyList(),
            pictures = listOf(
                Picture(url = "", width = 0, height = 0, key = "1"),
                Picture(url = "", width = 0, height = 0, key = "2"),
                Picture(url = "", width = 0, height = 0, key = "3"),
                Picture(url = "", width = 0, height = 0, key = "4")
            ),
            replies = listOf(
                Comment(
                    rpid = 0,
                    mid = 0,
                    oid = 0,
                    parent = 0,
                    type = 0,
                    content = listOf("其他视频的置顶：美国商务部的源文件里写的很清楚，对于消费用途的产品（consumer application）是exemption(豁免)。但是基于AD102的产品不得在中国大陆生产，也就是说未来国内销售的RTX 4090将会是在境外生产再运输回国内卖，这是唯一的不同点。估计后续也会是商家炒作显卡涨价的理由。"),
                    member = Comment.Member(mid = 0, avatar = "", name = "余Mercury"),
                    timeDesc = "4小时前",
                    emotes = emptyList(),
                    pictures = emptyList(),
                    replies = emptyList(),
                    repliesCount = 0
                ),
                Comment(
                    rpid = 0,
                    mid = 0,
                    oid = 0,
                    parent = 0,
                    type = 0,
                    content = listOf("回复 @余Mercury : 中东佬禁酒,用的泡沫水"),
                    member = Comment.Member(mid = 0, avatar = "", name = "铭轩-T"),
                    timeDesc = "4小时前",
                    emotes = emptyList(),
                    pictures = emptyList(),
                    replies = emptyList(),
                    repliesCount = 0
                ),
                Comment(
                    rpid = 0,
                    mid = 0,
                    oid = 0,
                    parent = 0,
                    type = 0,
                    content = listOf("澄清完更好笑了"),
                    member = Comment.Member(mid = 0, avatar = "", name = "Gemini好辣辣"),
                    timeDesc = "4小时前",
                    emotes = emptyList(),
                    pictures = emptyList(),
                    replies = emptyList(),
                    repliesCount = 0
                )
            ),
            repliesCount = 0
        )
    )
}

@Preview
@Composable
private fun CommentItemPreview(
    @PreviewParameter(CommentItemPreviewParameterProvider::class) comment: Comment
) {
    val previewerState = rememberPreviewerState(pageCount = { 0 })
    BVMobileTheme {
        CommentItem(
            comment = comment,
            previewerState = previewerState,
            onShowPreviewer = { _, _ -> }
        )
    }
}
