package dev.aaa1115910.bv.mobile.screen.message

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.message.DirectMessage
import dev.aaa1115910.biliapi.entity.message.DirectMessageContent
import dev.aaa1115910.biliapi.entity.message.DirectMessageEmote
import dev.aaa1115910.biliapi.entity.user.DynamicEmoteDraft
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.mobile.component.emote.EmoteInputToken
import dev.aaa1115910.bv.mobile.component.emote.EmotePanel
import dev.aaa1115910.bv.mobile.component.emote.EmoteTextEditor
import dev.aaa1115910.bv.mobile.component.emote.EmoteTextSelection
import dev.aaa1115910.bv.mobile.component.emote.emoteDisplayName
import dev.aaa1115910.bv.mobile.component.reply.richReplyUriBytes
import dev.aaa1115910.bv.mobile.component.reply.richReplyUriFileName
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.message.ConversationViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    modifier: Modifier = Modifier,
    talkerId: Long,
    name: String,
    face: String,
    viewModel: ConversationViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var inputSelection by remember { mutableStateOf(EmoteTextSelection.Zero) }
    var showEmotePanel by remember { mutableStateOf(false) }
    val inputEmoteTokens = remember { mutableStateListOf<EmoteInputToken>() }
    val dismissInputPanels = {
        showEmotePanel = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    val currentDismissInputPanels = rememberUpdatedState(dismissInputPanels)
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileName = context.richReplyUriFileName(uri)
        val bytes = context.richReplyUriBytes(uri)
        if (bytes == null) {
            "无法读取图片".toast(context)
        } else {
            viewModel.sendImage(fileName = fileName, bytes = bytes)
        }
    }

    fun insertEmote(emote: DynamicEmoteDraft) {
        val marker = emote.text
        val start = inputSelection.start.coerceIn(0, input.length)
        val end = inputSelection.end.coerceIn(0, input.length)
        val replaceStart = minOf(start, end)
        val replaceEnd = maxOf(start, end)
        input = input.replaceRange(replaceStart, replaceEnd, marker)
        inputSelection = EmoteTextSelection.collapsed(replaceStart + marker.length)
        if (emote.url.isNotBlank()) {
            inputEmoteTokens.add(
                EmoteInputToken(
                    marker = marker,
                    preferredStart = replaceStart,
                    emoteUrl = emote.url,
                    emoteName = emote.emoteDisplayName
                )
            )
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

    LaunchedEffect(showEmotePanel) {
        if (showEmotePanel) viewModel.loadEmotePackages()
    }

    BackHandler {
        (context as Activity).finish()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable(enabled = talkerId > 0L) {
                            UserSpaceActivity.actionStart(
                                context = context,
                                mid = talkerId,
                                name = viewModel.title
                            )
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConversationAvatar(face = viewModel.face, size = 34)
                        Text(
                            text = viewModel.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        bottomBar = {
            ConversationInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                text = input,
                selection = inputSelection,
                emoteTokens = inputEmoteTokens,
                sending = viewModel.sending,
                showEmotePanel = showEmotePanel,
                onTextChange = { value, selection ->
                    input = value
                    inputSelection = selection
                },
                onToggleEmotePanel = {
                    if (showEmotePanel) {
                        showEmotePanel = false
                    } else {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showEmotePanel = true
                    }
                },
                onEditorTouched = { showEmotePanel = false },
                onPickImage = { imagePicker.launch("image/*") },
                onSend = {
                    viewModel.sendText(input) {
                        input = ""
                        inputSelection = EmoteTextSelection.Zero
                        inputEmoteTokens.clear()
                    }
                },
                emotePanel = if (showEmotePanel) {
                    {
                        EmotePanel(
                            packages = viewModel.emotePackages,
                            loading = viewModel.loadingEmotes,
                            onSelect = ::insertEmote
                        )
                    }
                } else {
                    null
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null) currentDismissInputPanels.value()
                    }
                }
        ) {
            when {
                viewModel.loading && viewModel.messages.isEmpty() -> ConversationCenterContent(text = "正在加载", loading = true)
                viewModel.messages.isEmpty() && !viewModel.isLogin -> ConversationCenterContent(text = "请先登录")
                viewModel.messages.isEmpty() && viewModel.errorMessage != null -> ConversationCenterContent(
                    text = viewModel.errorMessage.orEmpty(),
                    action = "重试",
                    onAction = viewModel::refresh
                )

                viewModel.messages.isEmpty() -> ConversationCenterContent(text = "暂无消息")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (viewModel.hasMore) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(
                                    enabled = !viewModel.loadingMore,
                                    onClick = viewModel::loadMore
                                ) {
                                    if (viewModel.loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
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
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        text = viewModel.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationInput(
    modifier: Modifier = Modifier,
    text: String,
    selection: EmoteTextSelection,
    emoteTokens: List<EmoteInputToken>,
    sending: Boolean,
    showEmotePanel: Boolean,
    onTextChange: (String, EmoteTextSelection) -> Unit,
    onToggleEmotePanel: () -> Unit,
    onEditorTouched: () -> Unit,
    onPickImage: () -> Unit,
    onSend: () -> Unit,
    emotePanel: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    modifier = Modifier.size(40.dp),
                    enabled = !sending,
                    onClick = onToggleEmotePanel
                ) {
                    Icon(
                        imageVector = if (showEmotePanel) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                        contentDescription = "表情",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EmoteTextEditor(
                    modifier = Modifier.weight(1f),
                    value = text,
                    selection = selection,
                    emoteTokens = emoteTokens,
                    placeholder = "发个消息聊聊",
                    label = null,
                    enabled = !sending,
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(6.dp),
                    border = null,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    onEditorTouched = onEditorTouched,
                    onValueChange = onTextChange
                )
                IconButton(
                    modifier = Modifier.size(40.dp),
                    enabled = !sending,
                    onClick = {
                        if (text.isBlank()) {
                            onPickImage()
                        } else {
                            onSend()
                        }
                    }
                ) {
                    if (sending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (text.isBlank()) {
                                Icons.Rounded.AddPhotoAlternate
                            } else {
                                Icons.AutoMirrored.Rounded.Send
                            },
                            contentDescription = if (text.isBlank()) "图片" else "发送",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            emotePanel?.invoke()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
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
                ConversationAvatar(face = peerFace, size = 34)
                Spacer(modifier = Modifier.size(8.dp))
            }
            var bubbleModifier = Modifier.widthIn(max = 292.dp)
            if (canWithdraw || canReport) {
                bubbleModifier = bubbleModifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        if (canWithdraw) {
                            showWithdrawDialog = true
                        } else {
                            showReportDialog = true
                        }
                    }
                )
            }
            Surface(
                modifier = bubbleModifier,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isSelf) 16.dp else 4.dp,
                    bottomEnd = if (isSelf) 4.dp else 16.dp
                ),
                color = if (isSelf) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (isSelf) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
                ) {
                    MessageContent(
                        content = displayedContent,
                        emotes = emotes
                    )
                    if (showAutoReplyMark) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "此条消息为自动回复",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
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
                    .widthIn(max = 240.dp)
                    .aspectRatio(ratio.coerceIn(0.45f, 2.2f))
                    .clip(RoundedCornerShape(8.dp)),
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
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 21.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        is DirectMessageContent.Unsupported -> Text(
            modifier = modifier,
            text = content.text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp)
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
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp)
        )
        return
    }

    val inlineContent = matchedEmotes.associate { emote ->
        val size = if (emote.size >= 2) 44.sp else 24.sp
        emote.text to InlineTextContent(
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

    Text(
        modifier = modifier,
        text = annotatedText,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp)
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
    val clickableModifier = if (card.jumpUrl.isNotBlank()) {
        Modifier.clickable {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(card.jumpUrl)))
            }.onFailure {
                (it.message ?: "无法打开链接").toast(context)
            }
        }
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .widthIn(max = 252.dp)
            .then(clickableModifier),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
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
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showBadge) {
                        Text(
                            text = card.badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (card.title.isNotBlank()) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
                            maxLines = if (isNotification) Int.MAX_VALUE else 2,
                            overflow = if (isNotification) TextOverflow.Clip else TextOverflow.Ellipsis
                        )
                    }
                    if (card.subtitle.isNotBlank()) {
                        Text(
                            text = card.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 21.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    .heightIn(max = 360.dp)
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

@Composable
private fun ConversationAvatar(face: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (face.isBlank()) {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = face,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ConversationCenterContent(
    text: String,
    loading: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.size(14.dp))
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        if (action != null) {
            TextButton(onClick = onAction) {
                Text(text = action)
            }
        }
    }
}

private fun formatMessageTime(seconds: Long): String {
    if (seconds <= 0L) return ""
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(seconds * 1000L))
}
