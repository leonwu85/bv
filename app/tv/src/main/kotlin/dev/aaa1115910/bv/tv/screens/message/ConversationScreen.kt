package dev.aaa1115910.bv.tv.screens.message

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.message.DirectMessage
import dev.aaa1115910.biliapi.entity.message.DirectMessageContent
import dev.aaa1115910.biliapi.entity.message.DirectMessageEmote
import dev.aaa1115910.biliapi.entity.user.DynamicEmoteDraft
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.message.ConversationViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ConversationScreen(
    modifier: Modifier = Modifier,
    talkerId: Long,
    name: String,
    face: String,
    viewModel: ConversationViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var showEmotePicker by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileName = context.messageUriFileName(uri)
        val bytes = context.messageUriBytes(uri)
        if (bytes == null) {
            "无法读取图片".toast(context)
        } else {
            viewModel.sendImage(fileName = fileName, bytes = bytes)
        }
    }

    LaunchedEffect(talkerId, name, face) {
        viewModel.initialize(
            talkerId = talkerId,
            title = name,
            face = face
        )
    }

    LaunchedEffect(viewModel.messages.size, viewModel.loading, viewModel.loadingMore) {
        if (!viewModel.loading && !viewModel.loadingMore && viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.lastIndex)
        }
    }

    LaunchedEffect(showEmotePicker) {
        if (showEmotePicker) viewModel.loadEmotePackages()
    }

    BackHandler {
        (context as? Activity)?.finish()
    }

    if (showEmotePicker) {
        EmotePickerDialog(
            emotes = viewModel.emotePackages.flatMap { it.emotes },
            loading = viewModel.loadingEmotes,
            onDismiss = { showEmotePicker = false },
            onSelect = { emote ->
                input += emote.text
                showEmotePicker = false
            }
        )
    }

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TvMessageTopBar(title = viewModel.title.ifBlank { "私信" }) {
                if (talkerId > 0L) {
                    Button(
                        onClick = {
                            UpInfoActivity.actionStart(
                                context = context,
                                mid = talkerId,
                                name = viewModel.title,
                                face = viewModel.face
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = "主页")
                    }
                }
                Button(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "刷新")
                }
            }
        },
        bottomBar = {
            ConversationInputBar(
                text = input,
                sending = viewModel.sending,
                onTextChange = { input = it },
                onPickEmote = { showEmotePicker = true },
                onPickImage = { imagePicker.launch("image/*") },
                onSend = {
                    viewModel.sendText(input) {
                        input = ""
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                viewModel.loading && viewModel.messages.isEmpty() -> TvMessageCenterContent(
                    text = "正在加载",
                    loading = true
                )

                viewModel.messages.isEmpty() && !viewModel.isLogin -> TvMessageCenterContent(text = "请先登录")

                viewModel.messages.isEmpty() && viewModel.errorMessage != null -> TvMessageCenterContent(
                    text = viewModel.errorMessage.orEmpty(),
                    action = "重试",
                    onAction = viewModel::refresh
                )

                viewModel.messages.isEmpty() -> TvMessageCenterContent(text = "暂无消息")

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (viewModel.hasMore) {
                        item(key = "load_more") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    enabled = !viewModel.loadingMore,
                                    onClick = viewModel::loadMore
                                ) {
                                    if (viewModel.loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                    Text(text = "加载更早消息")
                                }
                            }
                        }
                    }
                    items(
                        items = viewModel.messages,
                        key = { "${it.msgKey}:${it.msgSeqno}:${it.timestampSeconds}" }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            isSelf = message.senderUid == viewModel.selfUid,
                            peerFace = viewModel.face,
                            emotes = viewModel.emotes,
                            onWithdraw = { viewModel.withdraw(message) },
                            onReport = { reasonType, reasonDesc ->
                                viewModel.report(
                                    message = message,
                                    reasonType = reasonType,
                                    reasonDesc = reasonDesc
                                ) {
                                    "举报成功".toast(context)
                                }
                            }
                        )
                    }
                }
            }

            if (viewModel.errorMessage != null && viewModel.messages.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        text = viewModel.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationInputBar(
    text: String,
    sending: Boolean,
    onTextChange: (String) -> Unit,
    onPickEmote: () -> Unit,
    onPickImage: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f)
        ),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                enabled = !sending,
                onClick = onPickEmote
            ) {
                Icon(Icons.Default.EmojiEmotions, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "表情")
            }
            Button(
                enabled = !sending,
                onClick = onPickImage
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "图片")
            }
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = text,
                onValueChange = onTextChange,
                enabled = !sending,
                minLines = 1,
                maxLines = 3,
                placeholder = { Text(text = "发个消息聊聊") }
            )
            Button(
                enabled = !sending && text.isNotBlank(),
                onClick = onSend
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "发送")
            }
        }
    }
}

@Composable
private fun EmotePickerDialog(
    emotes: List<DynamicEmoteDraft>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSelect: (DynamicEmoteDraft) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "选择表情") },
        text = {
            when {
                loading -> TvMessageCenterContent(
                    modifier = Modifier.height(240.dp),
                    text = "正在加载",
                    loading = true
                )

                emotes.isEmpty() -> TvMessageCenterContent(
                    modifier = Modifier.height(240.dp),
                    text = "暂无表情"
                )

                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = emotes,
                        key = { "${it.text}-${it.url}" }
                    ) { emote ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            colors = tvMessageClickableSurfaceColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            ),
                            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
                            onClick = { onSelect(emote) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    modifier = Modifier.size(if (emote.size >= 2) 48.dp else 34.dp),
                                    model = emote.url,
                                    contentDescription = emote.text,
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    text = emote.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "关闭")
            }
        }
    )
}

@Composable
private fun MessageBubble(
    message: DirectMessage,
    isSelf: Boolean,
    peerFace: String,
    emotes: Map<String, DirectMessageEmote>,
    onWithdraw: () -> Unit,
    onReport: (reasonType: Int, reasonDesc: String) -> Unit
) {
    var showWithdrawDialog by remember(message.msgKey, message.msgSeqno, message.status) {
        mutableStateOf(false)
    }
    var showReportDialog by remember(message.msgKey, message.msgSeqno) {
        mutableStateOf(false)
    }
    val canWithdraw = isSelf && message.status != 1 && message.msgKey > 0L
    val canReport = !isSelf && message.senderUid > 0L && message.msgKey > 0L
    val displayedContent = if (message.status == 1) {
        DirectMessageContent.Notice("已撤回")
    } else {
        message.content
    }
    val showAutoReplyMark = message.status != 1 && message.source in 8..11

    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text(text = "消息操作") },
            text = { Text(text = "撤回这条消息？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWithdrawDialog = false
                        onWithdraw()
                    }
                ) {
                    Text(text = "撤回")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }

    if (showReportDialog) {
        DirectMessageReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reasonType, reasonDesc ->
                showReportDialog = false
                onReport(reasonType, reasonDesc)
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatMessageTime(message.timestampSeconds),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isSelf) {
                TvMessageAvatar(url = peerFace, size = 42)
                Spacer(modifier = Modifier.size(10.dp))
            }
            Column(
                horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    colors = SurfaceDefaults.colors(
                        containerColor = if (isSelf) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        }
                    ),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isSelf) 18.dp else 5.dp,
                        bottomEnd = if (isSelf) 5.dp else 18.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .padding(13.dp),
                        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
                    ) {
                        MessageContent(
                            content = displayedContent,
                            emotes = emotes
                        )
                        if (showAutoReplyMark) {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )
                            Text(
                                text = "此条消息为自动回复",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                when {
                    canWithdraw -> Button(onClick = { showWithdrawDialog = true }) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(text = "撤回")
                    }

                    canReport -> Button(onClick = { showReportDialog = true }) {
                        Icon(Icons.Rounded.Flag, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(text = "举报")
                    }
                }
            }
            if (isSelf) {
                Spacer(modifier = Modifier.size(10.dp))
                TvMessageAvatar(url = "", size = 42)
            }
        }
    }
}

@Composable
private fun MessageContent(
    modifier: Modifier = Modifier,
    content: DirectMessageContent,
    emotes: Map<String, DirectMessageEmote> = emptyMap()
) {
    when (content) {
        is DirectMessageContent.Text -> DirectMessageText(
            modifier = modifier,
            text = content.text,
            emotes = emotes
        )

        is DirectMessageContent.Image -> {
            val ratio = if (content.width > 0 && content.height > 0) {
                content.width.toFloat() / content.height.toFloat()
            } else {
                1f
            }
            AsyncImage(
                modifier = modifier
                    .widthIn(max = 360.dp)
                    .aspectRatio(ratio.coerceIn(0.45f, 2.2f))
                    .clip(RoundedCornerShape(10.dp)),
                model = content.url,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        is DirectMessageContent.Card -> MessageCard(
            modifier = modifier,
            card = content
        )

        is DirectMessageContent.Notice -> Text(
            modifier = modifier,
            text = content.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        is DirectMessageContent.Unsupported -> Text(
            modifier = modifier,
            text = content.text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun DirectMessageText(
    modifier: Modifier = Modifier,
    text: String,
    emotes: Map<String, DirectMessageEmote>
) {
    val emoteCandidates = emotes.values.toList()
    val matchedEmotes = remember(text, emoteCandidates) {
        emoteCandidates
            .filter { emote ->
                emote.text.isNotBlank() && emote.url.isNotBlank() && text.contains(emote.text)
            }
            .distinctBy { it.text }
            .sortedByDescending { it.text.length }
    }

    if (matchedEmotes.isEmpty()) {
        Text(
            modifier = modifier,
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
        )
        return
    }

    val inlineContent = matchedEmotes.associate { emote ->
        val size = if (emote.size >= 2) 48.sp else 28.sp
        emote.text to androidx.compose.foundation.text.InlineTextContent(
            placeholder = Placeholder(
                width = size,
                height = size,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = emote.url,
                contentDescription = emote.text,
                contentScale = ContentScale.Fit
            )
        }
    }
    val annotatedText = buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val match = matchedEmotes.firstOrNull { emote ->
                text.regionMatches(
                    thisOffset = index,
                    other = emote.text,
                    otherOffset = 0,
                    length = emote.text.length
                )
            }
            if (match == null) {
                append(text[index])
                index += 1
            } else {
                appendInlineContent(match.text, match.text)
                index += match.text.length
            }
        }
    }

    androidx.compose.foundation.text.BasicText(
        modifier = modifier,
        text = annotatedText,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, color = LocalContentColor.current)
    )
}

@Composable
private fun MessageCard(
    modifier: Modifier = Modifier,
    card: DirectMessageContent.Card
) {
    val context = LocalContext.current
    val isNotification = card.badge == "通知"
    val showBadge = card.badge.isNotBlank() && !isNotification

    Surface(
        modifier = modifier.widthIn(max = 380.dp),
        colors = tvMessageClickableSurfaceColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        onClick = {
            if (card.jumpUrl.isNotBlank()) {
                openMessageExternal(context, card.jumpUrl)
            }
        }
    ) {
        Column {
            if (card.cover.isNotBlank()) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    model = card.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            if (card.title.isNotBlank() || card.subtitle.isNotBlank() || showBadge) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showBadge) {
                        Text(
                            text = card.badge,
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current.copy(alpha = 0.82f)
                        )
                    }
                    if (card.title.isNotBlank()) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = if (isNotification) Int.MAX_VALUE else 2,
                            overflow = if (isNotification) TextOverflow.Clip else TextOverflow.Ellipsis
                        )
                    }
                    if (card.subtitle.isNotBlank()) {
                        Text(
                            text = card.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.72f),
                            maxLines = if (isNotification) Int.MAX_VALUE else 3,
                            overflow = if (isNotification) TextOverflow.Clip else TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class DirectMessageReportReason(
    val type: Int,
    val label: String
)

private val directMessageReportReasons = listOf(
    DirectMessageReportReason(1, "垃圾广告"),
    DirectMessageReportReason(2, "色情低俗"),
    DirectMessageReportReason(7, "人身攻击"),
    DirectMessageReportReason(15, "侵犯隐私"),
    DirectMessageReportReason(12, "赌博诈骗"),
    DirectMessageReportReason(0, "其他")
)

@Composable
private fun DirectMessageReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (reasonType: Int, reasonDesc: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<DirectMessageReportReason?>(null) }
    var reasonDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "举报") },
        text = {
            Column(
                modifier = Modifier
                    .height(360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = "请选择举报理由：",
                    style = MaterialTheme.typography.bodyMedium
                )
                directMessageReportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
            }
        },
        confirmButton = {
            val reason = selectedReason
            TextButton(
                enabled = reason != null && (reason.type != 0 || reasonDesc.isNotBlank()),
                onClick = {
                    val selected = selectedReason ?: return@TextButton
                    onSubmit(
                        selected.type,
                        if (selected.type == 0) reasonDesc.trim() else selected.label
                    )
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

private fun Context.messageUriFileName(uri: Uri): String {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: "message_${System.currentTimeMillis()}.jpg"
}

private fun Context.messageUriBytes(uri: Uri): ByteArray? =
    contentResolver.openInputStream(uri)?.use { it.readBytes() }
