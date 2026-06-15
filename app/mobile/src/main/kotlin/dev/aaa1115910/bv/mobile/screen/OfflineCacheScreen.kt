package dev.aaa1115910.bv.mobile.screen

import android.app.Activity
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.offline.OfflineVideoCacheEntry
import dev.aaa1115910.bv.offline.OfflineVideoCacheService
import dev.aaa1115910.bv.offline.OfflineVideoCacheStatus
import dev.aaa1115910.bv.offline.OfflineVideoCacheTaskState
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun OfflineCacheScreen(
    offlineVideoCacheService: OfflineVideoCacheService = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val entries = offlineVideoCacheService.entries
    val activeStates = offlineVideoCacheService.taskStates.values
        .filter { state ->
            state.status != OfflineVideoCacheStatus.Idle &&
                state.status != OfflineVideoCacheStatus.Completed
        }
        .sortedWith(compareBy<OfflineVideoCacheTaskState> { it.status.ordinal }.thenBy { it.title })

    fun refresh() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            offlineVideoCacheService.refreshEntries()
            refreshing = false
        }
    }

    fun runCacheAction(action: () -> Result<String>) {
        scope.launch(Dispatchers.IO) {
            val result = action()
            withContext(Dispatchers.Main) {
                result
                    .onSuccess { it.toast(context) }
                    .onFailure { (it.localizedMessage ?: "操作失败").toast(context) }
            }
        }
    }

    LaunchedEffect(Unit) {
        offlineVideoCacheService.refreshEntries()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            OfflineCacheTopBar(
                refreshing = refreshing,
                onBack = { (context as? Activity)?.finish() },
                onRefresh = ::refresh
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeStates.isEmpty() && entries.isEmpty()) {
                    item {
                        OfflineCacheEmptyState()
                    }
                }
                if (activeStates.isNotEmpty()) {
                    item {
                        Text(
                            text = "缓存任务",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(
                        items = activeStates,
                        key = { "task-${it.aid}-${it.cid}" }
                    ) { state ->
                        OfflineCacheTaskItem(
                            state = state,
                            onPause = {
                                runCacheAction {
                                    offlineVideoCacheService.pause(state.aid, state.cid)
                                }
                            },
                            onResume = {
                                runCacheAction {
                                    offlineVideoCacheService.resume(state.aid, state.cid)
                                }
                            },
                            onClear = {
                                runCacheAction {
                                    offlineVideoCacheService.clearTask(state.aid, state.cid)
                                }
                            }
                        )
                    }
                }
                if (entries.isNotEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.padding(top = if (activeStates.isEmpty()) 0.dp else 10.dp),
                            text = "已缓存",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(
                        items = entries,
                        key = { "entry-${it.aid}-${it.cid}" }
                    ) { entry ->
                        OfflineCacheEntryItem(
                            entry = entry,
                            onPlay = {
                                VideoPlayerActivity.actionStart(
                                    context = context,
                                    aid = entry.aid,
                                    cid = entry.cid,
                                    cover = entry.cover,
                                    title = entry.title,
                                    partTitle = entry.partTitle,
                                    upName = entry.upName,
                                    playOfflineCache = true
                                )
                            },
                            onDelete = {
                                runCacheAction {
                                    offlineVideoCacheService.delete(entry.aid, entry.cid)
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineCacheTopBar(
    refreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(
            text = "离线缓存",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            if (refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
            }
        }
    }
}

@Composable
private fun OfflineCacheEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                modifier = Modifier.size(42.dp),
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "暂无离线缓存",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OfflineCacheTaskItem(
    state: OfflineVideoCacheTaskState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = state.partTitle.ifBlank { state.title.ifBlank { "缓存任务" } },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = state.message.ifBlank { state.status.label },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.totalBytes > 0L) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { state.progress },
                        gapSize = 0.dp
                    )
                }
            }
            when {
                state.status == OfflineVideoCacheStatus.Paused -> {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "继续")
                    }
                }

                state.isActive -> {
                    IconButton(onClick = onPause) {
                        Icon(Icons.Rounded.Pause, contentDescription = "暂停")
                    }
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Rounded.Delete, contentDescription = "清除任务")
            }
        }
    }
}

@Composable
private fun OfflineCacheEntryItem(
    entry: OfflineVideoCacheEntry,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .width(104.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                model = entry.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = entry.displayTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = listOf(
                        entry.qualityText.ifBlank { "${entry.quality}P" },
                        formatCacheBytes(entry.totalBytes)
                    ).joinToString(" · "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "播放")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "删除")
            }
        }
    }
}

private val OfflineVideoCacheStatus.label: String
    get() = when (this) {
        OfflineVideoCacheStatus.Idle -> "未缓存"
        OfflineVideoCacheStatus.Queued -> "等待缓存"
        OfflineVideoCacheStatus.Fetching -> "准备缓存"
        OfflineVideoCacheStatus.DownloadingVideo -> "正在缓存视频"
        OfflineVideoCacheStatus.DownloadingAudio -> "正在缓存音频"
        OfflineVideoCacheStatus.DownloadingDanmaku -> "正在缓存弹幕"
        OfflineVideoCacheStatus.Paused -> "已暂停"
        OfflineVideoCacheStatus.Completed -> "缓存完成"
        OfflineVideoCacheStatus.Failed -> "缓存失败"
    }

private fun formatCacheBytes(bytes: Long): String {
    if (bytes <= 0L) return "大小未知"
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1024.0) {
        String.format("%.1f GB", mb / 1024.0)
    } else {
        String.format("%.1f MB", mb)
    }
}
