package dev.aaa1115910.bv.tv.screens.message

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PushPin
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.message.DirectMessageSession
import dev.aaa1115910.biliapi.entity.message.MessageFeedType
import dev.aaa1115910.bv.tv.activities.message.ContactActivity
import dev.aaa1115910.bv.tv.activities.message.ConversationActivity
import dev.aaa1115910.bv.tv.activities.message.MessageFeedActivity
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.viewmodel.message.InboxViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteListDialog by remember { mutableStateOf(false) }

    listState.OnBottomReached(
        loading = viewModel.refreshing || viewModel.loadingMore || !viewModel.hasMore
    ) {
        viewModel.loadMore()
    }

    BackHandler {
        (context as? Activity)?.finish()
    }

    if (showClearDialog) {
        TvAlertDialog(
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
        TvAlertDialog(
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

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TvMessageTopBar(title = "消息") {
                Button(onClick = { ContactActivity.actionStart(context) }) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "通讯录")
                }
                Button(onClick = { showClearDialog = true }) {
                    Icon(Icons.Rounded.MarkChatUnread, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "已读")
                }
                Button(onClick = { showDeleteListDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "清空")
                }
                Button(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "刷新")
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                viewModel.sessions.isEmpty() && !viewModel.isLogin -> {
                    TvMessageCenterContent(text = "请先登录")
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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

                        when {
                            viewModel.initialLoading -> item(key = "loading") {
                                TvMessageCenterContent(
                                    modifier = Modifier.height(300.dp),
                                    text = "正在加载",
                                    loading = true
                                )
                            }

                            viewModel.sessions.isEmpty() && viewModel.errorMessage != null -> item(key = "error") {
                                TvMessageCenterContent(
                                    modifier = Modifier.height(300.dp),
                                    text = viewModel.errorMessage.orEmpty(),
                                    action = "重试",
                                    onAction = viewModel::refresh
                                )
                            }

                            viewModel.sessions.isEmpty() -> item(key = "empty") {
                                TvMessageCenterContent(
                                    modifier = Modifier.height(300.dp),
                                    text = "暂无私信"
                                )
                            }

                            else -> {
                                item(key = "session_title") {
                                    Text(
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                                        text = "私信会话",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                                }
                                if (viewModel.loadingMore) {
                                    item(key = "loading_more") {
                                        TvMessageCenterContent(
                                            modifier = Modifier.height(140.dp),
                                            text = "正在加载",
                                            loading = true
                                        )
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        entries.forEach { entry ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(118.dp),
                colors = tvMessageClickableSurfaceColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                ),
                shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
                onClick = { onClick(entry.index) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
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
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = entry.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxSessionRow(
    session: DirectMessageSession,
    onClick: () -> Unit,
    onSetPinned: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember(session.talkerId) { mutableStateOf(false) }

    if (showRemoveDialog) {
        TvAlertDialog(
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        colors = tvMessageClickableSurfaceColors(
            containerColor = if (session.isPinned) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TvMessageAvatar(url = session.face)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = session.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatMessageSessionTime(session.timestampMicros),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.72f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = session.summary.ifBlank { " " },
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (session.isPinned) {
                        Icon(
                            modifier = Modifier.size(18.dp),
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
            Button(onClick = onSetPinned) {
                Icon(Icons.Rounded.PushPin, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = if (session.isPinned) "取消" else "置顶")
            }
            Button(onClick = { showRemoveDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = "删除")
            }
        }
    }
}

private fun openMessageFeed(context: android.content.Context, index: Int) {
    val type = when (index) {
        0 -> MessageFeedType.Reply
        1 -> MessageFeedType.At
        2 -> MessageFeedType.Like
        3 -> MessageFeedType.System
        else -> return
    }
    MessageFeedActivity.actionStart(context, type)
}
