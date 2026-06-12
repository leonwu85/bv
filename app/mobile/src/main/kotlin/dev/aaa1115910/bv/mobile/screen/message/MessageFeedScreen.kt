package dev.aaa1115910.bv.mobile.screen.message

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.message.MessageFeedItem
import dev.aaa1115910.biliapi.entity.message.MessageFeedType
import dev.aaa1115910.biliapi.repositories.MessageRepository
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.message.MessageFeedViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessageFeedScreen(
    type: MessageFeedType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val refreshState = rememberPullToRefreshState()
    val messageRepository: MessageRepository = koinInject()
    val userRepository: UserRepository = koinInject()
    val viewModel: MessageFeedViewModel = viewModel(
        key = "message-feed-${type.name}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MessageFeedViewModel(type, messageRepository, userRepository) as T
        }
    )
    var pendingDelete by remember { mutableStateOf<MessageFeedItem?>(null) }

    listState.OnBottomReached(loading = viewModel.refreshing || viewModel.loadingMore || !viewModel.hasMore) {
        viewModel.loadMore()
    }

    BackHandler {
        (context as Activity).finish()
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(text = "确定删除该通知?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        viewModel.remove(item)
                    }
                ) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = "取消")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = type.title) },
                navigationIcon = {
                    IconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            state = refreshState,
            isRefreshing = viewModel.refreshing && !viewModel.initialLoading,
            onRefresh = viewModel::refresh
        ) {
            when {
                viewModel.items.isEmpty() && !viewModel.isLogin -> MessageFeedCenterContent(text = "请先登录")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 24.dp)
                ) {
                    when {
                        viewModel.initialLoading -> item(key = "loading") {
                            MessageFeedCenterContent(text = "正在加载", loading = true)
                        }

                        viewModel.items.isEmpty() && viewModel.errorMessage != null -> item(key = "error") {
                            MessageFeedCenterContent(
                                text = viewModel.errorMessage.orEmpty(),
                                action = "重试",
                                onAction = viewModel::refresh
                            )
                        }

                        viewModel.items.isEmpty() -> item(key = "empty") {
                            MessageFeedCenterContent(text = "暂无通知")
                        }

                        else -> {
                            var lastSection = ""
                            viewModel.items.forEachIndexed { index, feedItem ->
                                if (feedItem.section.isNotBlank() && feedItem.section != lastSection) {
                                    lastSection = feedItem.section
                                    item(key = "section-${feedItem.section}-$index") {
                                        MessageFeedSectionHeader(title = feedItem.section)
                                    }
                                }
                                item(key = "${feedItem.type.name}-${feedItem.section}-${feedItem.id}-$index") {
                                    if (feedItem.type == MessageFeedType.System) {
                                        MessageFeedSystemRow(
                                            item = feedItem,
                                            onLongClick = { pendingDelete = feedItem }
                                        )
                                    } else {
                                        MessageFeedRow(
                                            item = feedItem,
                                            onClick = { openFeedItem(context, feedItem) },
                                            onAvatarClick = {
                                                feedItem.userMid?.takeIf { it > 0L }?.let { mid ->
                                                    UserSpaceActivity.actionStart(context, mid, feedItem.username)
                                                }
                                            },
                                            onLongClick = { pendingDelete = feedItem }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = if (feedItem.type == MessageFeedType.System) 16.dp else 72.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    )
                                }
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

@Composable
private fun MessageFeedSectionHeader(title: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageFeedRow(
    item: MessageFeedItem,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MessageFeedAvatar(
            url = item.avatar,
            modifier = Modifier.clickable(onClick = onAvatarClick)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MessageFeedTitle(item)
            if (item.body.isNotBlank()) {
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.type == MessageFeedType.Reply) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.quote.isNotBlank()) {
                Text(
                    text = "| ${item.quote}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatFeedTime(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        MessageFeedCover(url = item.image)
    }
}

@Composable
private fun MessageFeedTitle(item: MessageFeedItem) {
    val title = buildAnnotatedString {
        if (item.username.isNotBlank() && item.title.startsWith(item.username)) {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(item.username)
            }
            append(item.title.removePrefix(item.username))
        } else {
            append(item.title)
        }
    }
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageFeedSystemRow(
    item: MessageFeedItem,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        if (item.body.isNotBlank()) {
            Text(
                text = cleanSystemContent(item.body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
        Text(
            modifier = Modifier.align(Alignment.End),
            text = formatFeedTime(item),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MessageFeedAvatar(
    url: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(45.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (url.isBlank()) {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun MessageFeedCover(url: String) {
    if (url.isBlank()) return
    Box(
        modifier = Modifier
            .size(45.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun MessageFeedCenterContent(
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

private fun openFeedItem(context: Context, item: MessageFeedItem) {
    val rawUri = item.jumpUri.trim()
    if (rawUri.isBlank() || rawUri.startsWith("?")) return
    val uri = when {
        rawUri.startsWith("//") -> "https:$rawUri"
        else -> rawUri
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }.onFailure { error ->
        if (error is ActivityNotFoundException) {
            "没有可打开的应用".toast(context)
        } else {
            (error.localizedMessage ?: "打开失败").toast(context)
        }
    }
}

private fun formatFeedTime(item: MessageFeedItem): String {
    if (item.timeText.isNotBlank()) return item.timeText
    val seconds = item.timestampSeconds ?: return ""
    if (seconds <= 0L) return ""
    val millis = seconds * 1000L
    val now = System.currentTimeMillis()
    val pattern = if (now - millis < 24 * 60 * 60 * 1000L) "HH:mm" else "MM-dd"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

private fun cleanSystemContent(content: String): String =
    content.replace(Regex("""#\{([^}]*)\}\{([^}]*)\}"""), "$1")
