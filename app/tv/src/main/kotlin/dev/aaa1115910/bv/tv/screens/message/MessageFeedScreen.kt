package dev.aaa1115910.bv.tv.screens.message

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.message.MessageFeedItem
import dev.aaa1115910.biliapi.entity.message.MessageFeedType
import dev.aaa1115910.biliapi.repositories.MessageRepository
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.viewmodel.message.MessageFeedViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageFeedScreen(
    type: MessageFeedType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val messageRepository: MessageRepository = koinInject()
    val userRepository: UserRepository = koinInject()
    val viewModel: MessageFeedViewModel = viewModel(
        key = "tv-message-feed-${type.name}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MessageFeedViewModel(type, messageRepository, userRepository) as T
        }
    )
    var pendingDelete by remember { mutableStateOf<MessageFeedItem?>(null) }

    listState.OnBottomReached(
        loading = viewModel.refreshing || viewModel.loadingMore || !viewModel.hasMore
    ) {
        viewModel.loadMore()
    }

    BackHandler {
        (context as? Activity)?.finish()
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

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TvMessageTopBar(title = type.title) {
                Button(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "刷新")
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        when {
            viewModel.items.isEmpty() && !viewModel.isLogin -> TvMessageCenterContent(
                modifier = Modifier.padding(innerPadding),
                text = "请先登录"
            )

            viewModel.initialLoading -> TvMessageCenterContent(
                modifier = Modifier.padding(innerPadding),
                text = "正在加载",
                loading = true
            )

            viewModel.items.isEmpty() && viewModel.errorMessage != null -> TvMessageCenterContent(
                modifier = Modifier.padding(innerPadding),
                text = viewModel.errorMessage.orEmpty(),
                action = "重试",
                onAction = viewModel::refresh
            )

            viewModel.items.isEmpty() -> TvMessageCenterContent(
                modifier = Modifier.padding(innerPadding),
                text = "暂无通知"
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = listState,
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                var lastSection = ""
                viewModel.items.forEachIndexed { index, feedItem ->
                    if (feedItem.section.isNotBlank() && feedItem.section != lastSection) {
                        lastSection = feedItem.section
                        item(key = "section-${feedItem.section}-$index") {
                            Text(
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                                text = feedItem.section,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    item(key = "${feedItem.type.name}-${feedItem.section}-${feedItem.id}-$index") {
                        MessageFeedRow(
                            item = feedItem,
                            onClick = { openFeedItem(context, feedItem) },
                            onAvatarClick = {
                                feedItem.userMid?.takeIf { it > 0L }?.let { mid ->
                                    UpInfoActivity.actionStart(context, mid, feedItem.username, feedItem.avatar)
                                }
                            },
                            onDelete = { pendingDelete = feedItem }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
                    }
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

@Composable
private fun MessageFeedRow(
    item: MessageFeedItem,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        colors = tvMessageClickableSurfaceColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (item.type != MessageFeedType.System) {
                Surface(
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                    onClick = onAvatarClick
                ) {
                    TvMessageAvatar(url = item.avatar)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MarkChatUnread,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalContentColor.current,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.body.isNotBlank()) {
                    Text(
                        text = if (item.type == MessageFeedType.System) cleanSystemContent(item.body) else item.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.72f),
                        maxLines = if (item.type == MessageFeedType.System) 5 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.quote.isNotBlank()) {
                    Text(
                        text = "| ${item.quote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatFeedTime(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.62f)
                )
            }
            MessageFeedCover(url = item.image)
            Button(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = "删除")
            }
        }
    }
}

@Composable
private fun MessageFeedCover(url: String) {
    if (url.isBlank()) return
    Box(
        modifier = Modifier
            .width(110.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(10.dp))
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

private fun openFeedItem(context: Context, item: MessageFeedItem) {
    val rawUri = item.jumpUri.trim()
    if (rawUri.isBlank() || rawUri.startsWith("?")) return
    val uri = when {
        rawUri.startsWith("//") -> "https:$rawUri"
        else -> rawUri
    }
    openMessageExternal(context, uri)
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
