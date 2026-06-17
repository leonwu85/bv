package dev.aaa1115910.bv.mobile.screen.message

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.message.DirectMessageAction
import dev.aaa1115910.biliapi.entity.message.DirectMessageSession
import dev.aaa1115910.biliapi.entity.message.MessageFeedType
import dev.aaa1115910.bv.mobile.activities.ContactActivity
import dev.aaa1115910.bv.mobile.activities.ConversationActivity
import dev.aaa1115910.bv.mobile.activities.MessageFeedActivity
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.message.InboxViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val refreshState = rememberPullToRefreshState()
    var showMoreMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteListDialog by remember { mutableStateOf(false) }

    listState.OnBottomReached(loading = viewModel.refreshing || viewModel.loadingMore || !viewModel.hasMore) {
        viewModel.loadMore()
    }

    BackHandler {
        (context as Activity).finish()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "消息") },
                navigationIcon = {
                    IconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { ContactActivity.actionStart(context) }) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "我的好友")
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "清除")
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            val actions = viewModel.actions.ifEmpty {
                                listOf(
                                    DirectMessageAction(title = "消息设置", url = "", type = ACTION_MESSAGE_SETTING, hasRedDot = false),
                                    DirectMessageAction(title = "自动回复", url = "", type = ACTION_AUTO_REPLY, hasRedDot = false)
                                )
                            }
                            actions.forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(text = action.title.ifBlank { "更多" }) },
                                    leadingIcon = { Icon(inboxActionIcon(action.type), contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        handleInboxAction(
                                            context = context,
                                            action = action,
                                            onClearUnread = { showClearDialog = true },
                                            onDeleteList = { showDeleteListDialog = true }
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(text = "一键已读") },
                text = { Text(text = "是否清除全部新消息提醒？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            viewModel.clearAllUnread()
                        }
                    ) {
                        Text(text = "清除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(text = "取消")
                    }
                }
            )
        }
        if (showDeleteListDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteListDialog = false },
                title = { Text(text = "清空列表") },
                text = { Text(text = "清空后所有消息将被删除，无法恢复") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteListDialog = false
                            viewModel.deleteAllSessions()
                        }
                    ) {
                        Text(text = "清空")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteListDialog = false }) {
                        Text(text = "取消")
                    }
                }
            )
        }

        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            state = refreshState,
            isRefreshing = viewModel.refreshing && !viewModel.initialLoading,
            onRefresh = viewModel::refresh
        ) {
            when {
                viewModel.sessions.isEmpty() && !viewModel.isLogin -> InboxCenterContent(text = "请先登录")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item(key = "feed_top") {
                        InboxFeedTopRow(
                            replyUnread = viewModel.feedUnread.reply,
                            atUnread = viewModel.feedUnread.at,
                            likeUnread = viewModel.feedUnread.like,
                            sysUnread = viewModel.feedUnread.sysMsg,
                            onClick = { index ->
                                viewModel.markFeedUnreadRead(index)
                                openMessageFeed(context, index)
                            }
                        )
                    }
                    item(key = "feed_divider") {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    }
                    when {
                        viewModel.initialLoading -> item(key = "loading") {
                            InboxInlineContent(text = "正在加载", loading = true)
                        }

                        viewModel.sessions.isEmpty() && viewModel.errorMessage != null -> item(key = "error") {
                            InboxInlineContent(
                                text = viewModel.errorMessage.orEmpty(),
                                action = "重试",
                                onAction = viewModel::refresh
                            )
                        }

                        viewModel.sessions.isEmpty() -> item(key = "empty") {
                            InboxInlineContent(text = "暂无私信")
                        }

                        else -> {
                            items(
                                items = viewModel.sessions,
                                key = { it.talkerId }
                            ) { session ->
                                InboxSessionRow(
                                    session = session,
                                    onClick = {
                                        viewModel.markLocalRead(session.talkerId)
                                        ConversationActivity.actionStart(
                                            context = context,
                                            talkerId = session.talkerId,
                                            name = session.name,
                                            face = session.face
                                        )
                                    },
                                    onSetPinned = { viewModel.setPinned(session) },
                                    onRemove = { viewModel.remove(session) }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                            }
                            if (viewModel.loadingMore) {
                                item(key = "loading_more") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val ACTION_READ_ALL = 1
private const val ACTION_MESSAGE_SETTING = 2
private const val ACTION_AUTO_REPLY = 3
private const val ACTION_CONTACTS = 8
private const val ACTION_CLEAR_LIST = 9

private data class InboxFeedEntry(
    val title: String,
    val icon: ImageVector,
    val unread: Int,
    val index: Int
)

@Composable
private fun InboxFeedTopRow(
    replyUnread: Int,
    atUnread: Int,
    likeUnread: Int,
    sysUnread: Int,
    onClick: (Int) -> Unit
) {
    val entries = listOf(
        InboxFeedEntry("回复我的", Icons.AutoMirrored.Rounded.Message, replyUnread, 0),
        InboxFeedEntry("@我", Icons.Rounded.AlternateEmail, atUnread, 1),
        InboxFeedEntry("收到的赞", Icons.Rounded.FavoriteBorder, likeUnread, 2),
        InboxFeedEntry("系统通知", Icons.Rounded.NotificationsNone, sysUnread, 3)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        entries.forEach { entry ->
            InboxFeedItem(
                entry = entry,
                onClick = { onClick(entry.index) }
            )
        }
    }
}

@Composable
private fun InboxFeedItem(
    entry: InboxFeedEntry,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BadgedBox(
            badge = {
                if (entry.unread > 0) {
                    Badge {
                        Text(text = if (entry.unread > 99) "99+" else entry.unread.toString())
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = entry.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = entry.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InboxSessionRow(
    session: DirectMessageSession,
    onClick: () -> Unit,
    onSetPinned: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(text = "删除会话") },
            text = { Text(text = "确定删除与 ${session.name} 的会话吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                    }
                ) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (session.isPinned) {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InboxAvatar(face = session.face)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = session.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatSessionTime(session.timestampMicros),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = session.summary.ifBlank { " " },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.isPinned) {
                    Icon(
                        modifier = Modifier.size(15.dp),
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (session.unreadCount > 0) {
                    Badge {
                        Text(text = if (session.unreadCount > 99) "99+" else session.unreadCount.toString())
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = if (session.isPinned) "取消置顶" else "置顶") },
                    leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onSetPinned()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = "删除") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        showRemoveDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun InboxAvatar(face: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
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
private fun InboxInlineContent(
    text: String,
    loading: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                modifier = Modifier.size(44.dp),
                imageVector = Icons.Rounded.MarkChatUnread,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.size(14.dp))
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

@Composable
private fun InboxCenterContent(
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
        } else {
            Icon(
                modifier = Modifier.size(48.dp),
                imageVector = Icons.Rounded.MarkChatUnread,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.size(14.dp))
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

private fun formatSessionTime(timestampMicros: Long): String {
    if (timestampMicros <= 0L) return ""
    val millis = timestampMicros / 1000L
    val now = System.currentTimeMillis()
    val pattern = if (now - millis < 24 * 60 * 60 * 1000L) "HH:mm" else "MM-dd"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

private fun inboxActionIcon(type: Int): ImageVector = when (type) {
    ACTION_READ_ALL -> Icons.Default.Delete
    ACTION_MESSAGE_SETTING -> Icons.Rounded.AccountCircle
    ACTION_AUTO_REPLY -> Icons.Rounded.MarkChatUnread
    ACTION_CONTACTS -> Icons.Rounded.AccountCircle
    ACTION_CLEAR_LIST -> Icons.Default.Delete
    else -> Icons.Default.MoreVert
}

private fun handleInboxAction(
    context: Context,
    action: DirectMessageAction,
    onClearUnread: () -> Unit,
    onDeleteList: () -> Unit
) {
    if (isContactAction(action)) {
        ContactActivity.actionStart(context)
        return
    }
    when (action.type) {
        ACTION_READ_ALL -> onClearUnread()
        ACTION_CLEAR_LIST -> onDeleteList()
        ACTION_CONTACTS -> ContactActivity.actionStart(context)
        ACTION_MESSAGE_SETTING -> "消息设置暂未支持".toast(context)
        ACTION_AUTO_REPLY -> "自动回复暂未支持".toast(context)
        else -> {
            if (action.url.isNotBlank()) {
                openExternal(context, action.url)
            } else {
                "${action.title.ifBlank { "该功能" }}暂未支持".toast(context)
            }
        }
    }
}

private fun isContactAction(action: DirectMessageAction): Boolean {
    val title = action.title
    val url = action.url
    return action.type == ACTION_CONTACTS ||
            title.contains("通讯录") ||
            title.contains("我的好友") ||
            url.contains("contacts", ignoreCase = true)
}

private fun openMessageFeed(context: Context, index: Int) {
    val type = when (index) {
        0 -> MessageFeedType.Reply
        1 -> MessageFeedType.At
        2 -> MessageFeedType.Like
        3 -> MessageFeedType.System
        else -> return
    }
    MessageFeedActivity.actionStart(context, type)
}

private fun openExternal(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure { error ->
        if (error is ActivityNotFoundException) {
            "没有可打开的应用".toast(context)
        } else {
            (error.localizedMessage ?: "打开失败").toast(context)
        }
    }
}
