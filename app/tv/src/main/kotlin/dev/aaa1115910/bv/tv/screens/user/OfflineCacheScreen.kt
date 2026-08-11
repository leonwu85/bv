package dev.aaa1115910.bv.tv.screens.user

import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import dev.aaa1115910.bv.offline.OfflineVideoCacheEntry
import dev.aaa1115910.bv.offline.OfflineVideoCacheService
import dev.aaa1115910.bv.offline.OfflineVideoCacheStatus
import dev.aaa1115910.bv.offline.OfflineVideoCacheTaskState
import dev.aaa1115910.bv.tv.activities.video.OfflineVideoPlayerActivity
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

private enum class OfflineCacheFilter(val label: String) {
    All("全部"), Active("缓存中"), Completed("已完成")
}

private data class OfflineCacheCardModel(
    val key: String,
    val aid: Long,
    val cid: Long,
    val title: String,
    val seriesTitle: String,
    val cover: String,
    val upName: String,
    val upFace: String,
    val danmakuCount: Int,
    val durationMs: Long,
    val qualityText: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val status: OfflineVideoCacheStatus,
    val entry: OfflineVideoCacheEntry? = null,
    val task: OfflineVideoCacheTaskState? = null,
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

@Composable
fun OfflineCacheScreen(
    onBack: () -> Unit,
    offlineCacheService: OfflineVideoCacheService = getKoin().get(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialFocusRequester = remember { FocusRequester() }
    var selectedFilter by remember { mutableStateOf(OfflineCacheFilter.All) }
    var manageMode by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(emptySet<String>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val activeCards = offlineCacheService.taskStates.values
        .filter { it.status != OfflineVideoCacheStatus.Idle && it.status != OfflineVideoCacheStatus.Completed }
        .map { task ->
            OfflineCacheCardModel(
                key = offlineCacheService.key(task.aid, task.cid),
                aid = task.aid,
                cid = task.cid,
                title = task.partTitle.ifBlank { task.title },
                seriesTitle = task.title,
                cover = task.cover,
                upName = task.upName,
                upFace = task.upFace,
                danmakuCount = task.danmakuCount,
                durationMs = task.durationMs,
                qualityText = task.qualityText,
                downloadedBytes = task.downloadedBytes,
                totalBytes = task.totalBytes,
                status = task.status,
                task = task
            )
        }
    val completedCards = offlineCacheService.entries.map { entry ->
        OfflineCacheCardModel(
            key = offlineCacheService.key(entry.aid, entry.cid),
            aid = entry.aid,
            cid = entry.cid,
            title = entry.displayTitle,
            seriesTitle = entry.title,
            cover = offlineCacheService.getCachedCoverUri(entry).orEmpty(),
            upName = entry.upName,
            upFace = offlineCacheService.getCachedUpFaceUri(entry).orEmpty(),
            danmakuCount = entry.danmakuCount,
            durationMs = entry.durationMs,
            qualityText = entry.qualityText,
            downloadedBytes = entry.totalBytes,
            totalBytes = entry.totalBytes,
            status = OfflineVideoCacheStatus.Completed,
            entry = entry
        )
    }
    val allCards = activeCards + completedCards
    val displayedCards = when (selectedFilter) {
        OfflineCacheFilter.All -> allCards
        OfflineCacheFilter.Active -> activeCards
        OfflineCacheFilter.Completed -> completedCards
    }
    val displayedKeys = displayedCards.mapTo(mutableSetOf()) { it.key }
    val allDisplayedSelected = displayedKeys.isNotEmpty() && selectedKeys.containsAll(displayedKeys)
    val cacheBytes = allCards.sumOf { it.downloadedBytes }
    val statFs = remember(cacheBytes) { StatFs(context.filesDir.absolutePath) }
    val totalDeviceBytes = statFs.totalBytes
    val availableDeviceBytes = statFs.availableBytes
    val usedDeviceBytes = (totalDeviceBytes - availableDeviceBytes).coerceAtLeast(0L)
    val deviceUsedRatio = if (totalDeviceBytes > 0) {
        ((totalDeviceBytes - availableDeviceBytes).toFloat() / totalDeviceBytes).coerceIn(0f, 1f)
    } else 0f

    fun toggleSelected(key: String) {
        selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
    }

    fun handleCardClick(card: OfflineCacheCardModel) {
        if (manageMode) {
            toggleSelected(card.key)
            return
        }
        when (card.status) {
            OfflineVideoCacheStatus.Completed -> card.entry?.let { entry ->
                OfflineVideoPlayerActivity.actionStart(
                    context = context,
                    aid = entry.aid,
                    cid = entry.cid
                )
            }
            OfflineVideoCacheStatus.Paused -> {
                offlineCacheService.resume(card.aid, card.cid).fold(
                    onSuccess = { it.toast(context) },
                    onFailure = { (it.localizedMessage ?: "继续缓存失败").toast(context) }
                )
            }
            OfflineVideoCacheStatus.Failed -> {
                scope.launch {
                    offlineCacheService.clearTask(card.aid, card.cid).fold(
                        onSuccess = { it.toast(context) },
                        onFailure = { (it.localizedMessage ?: "清除失败").toast(context) }
                    )
                }
            }
            OfflineVideoCacheStatus.Queued,
            OfflineVideoCacheStatus.Fetching,
            OfflineVideoCacheStatus.DownloadingVideo,
            OfflineVideoCacheStatus.DownloadingAudio,
            OfflineVideoCacheStatus.DownloadingDanmaku -> offlineCacheService.pause(card.aid, card.cid)
                .fold(onSuccess = { it.toast(context) }, onFailure = { (it.localizedMessage ?: "暂停失败").toast(context) })
            OfflineVideoCacheStatus.Idle -> Unit
        }
    }

    BackHandler {
        when {
            showDeleteConfirm -> showDeleteConfirm = false
            manageMode -> {
                manageMode = false
                selectedKeys = emptySet()
            }
            else -> onBack()
        }
    }

    LaunchedEffect(Unit) {
        offlineCacheService.refreshEntries()
    }

    LaunchedEffect(activeCards.firstOrNull()?.key, completedCards.firstOrNull()?.key) {
        initialFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "离线缓存",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(
                modifier = Modifier.width(280.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "已用 ${formatCacheBytes(usedDeviceBytes)} / ${formatCacheBytes(totalDeviceBytes)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                LinearProgressIndicator(
                    progress = { deviceUsedRatio },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    gapSize = 0.dp
                )
            }
            OutlinedButton(
                modifier = Modifier
                    .width(70.dp)
                    .height(36.dp),
                contentPadding = PaddingValues(0.dp),
                onClick = {
                    manageMode = !manageMode
                    if (!manageMode) selectedKeys = emptySet()
                }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (manageMode) "完成" else "管理", maxLines = 1)
                }
            }
            if (manageMode) {
                OutlinedButton(
                    modifier = Modifier
                        .width(if (allDisplayedSelected) 92.dp else 70.dp)
                        .height(36.dp),
                    enabled = displayedKeys.isNotEmpty(),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        selectedKeys = if (allDisplayedSelected) {
                            selectedKeys - displayedKeys
                        } else {
                            selectedKeys + displayedKeys
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (allDisplayedSelected) "取消全选" else "全选", maxLines = 1)
                    }
                }
                Button(
                    modifier = Modifier.height(36.dp),
                    enabled = selectedKeys.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text("删除 ${selectedKeys.size}", maxLines = 1)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OfflineCacheFilter.entries.forEachIndexed { index, filter ->
                val selected = selectedFilter == filter
                OutlinedButton(
                    modifier = Modifier
                        .width(88.dp)
                        .height(28.dp)
                        .then(
                            if (allCards.isEmpty() && index == 0) {
                                Modifier.focusRequester(initialFocusRequester)
                            } else Modifier
                        ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    onClick = { selectedFilter = filter },
                    colors = ButtonDefaults.colors(
                        containerColor = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.primaryContainer,
                        focusedContentColor = if (selected) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    val count = when (filter) {
                        OfflineCacheFilter.All -> allCards.size
                        OfflineCacheFilter.Active -> activeCards.size
                        OfflineCacheFilter.Completed -> completedCards.size
                    }
                    Text("${filter.label} $count")
                }
            }
        }

        if (displayedCards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Rounded.Download,
                        null,
                        modifier = Modifier.size(62.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                    Text(
                        if (selectedFilter == OfflineCacheFilter.Completed) "还没有已缓存视频" else "暂无缓存任务",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text("可在视频详情页或播放页加入缓存", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedFilter != OfflineCacheFilter.Completed && activeCards.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "缓存中",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(
                        items = activeCards,
                        key = { it.key },
                        span = { GridItemSpan(2) }
                    ) { card ->
                        ActiveOfflineCacheCard(
                            card = card,
                            selected = card.key in selectedKeys,
                            manageMode = manageMode,
                            actionModifier = if (card.key == activeCards.first().key) {
                                Modifier.focusRequester(initialFocusRequester)
                            } else Modifier,
                            onClick = { handleCardClick(card) },
                            onCancel = {
                                scope.launch {
                                    offlineCacheService.clearTask(card.aid, card.cid).fold(
                                        onSuccess = { it.toast(context) },
                                        onFailure = { (it.localizedMessage ?: "取消缓存失败").toast(context) }
                                    )
                                }
                            }
                        )
                    }
                }

                if (selectedFilter != OfflineCacheFilter.Active && completedCards.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "已完成 ${completedCards.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(completedCards, key = { it.key }) { card ->
                        OfflineCacheCard(
                            modifier = if (activeCards.isEmpty() && card.key == completedCards.first().key) {
                                Modifier.focusRequester(initialFocusRequester)
                            } else Modifier,
                            card = card,
                            selected = card.key in selectedKeys,
                            manageMode = manageMode,
                            onClick = { handleCardClick(card) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        TvAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除离线缓存") },
            text = { Text("将删除选中的 ${selectedKeys.size} 个缓存或任务，此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedCards = allCards.filter { it.key in selectedKeys }
                        scope.launch {
                            val failures = selectedCards.mapNotNull { card ->
                                val result = if (card.status == OfflineVideoCacheStatus.Completed) {
                                    offlineCacheService.delete(card.aid, card.cid)
                                } else {
                                    offlineCacheService.clearTask(card.aid, card.cid)
                                }
                                result.exceptionOrNull()
                            }
                            selectedKeys = emptySet()
                            showDeleteConfirm = false
                            manageMode = false
                            offlineCacheService.refreshEntries()
                            if (failures.isEmpty()) {
                                "已删除选中缓存".toast(context)
                            } else {
                                "有 ${failures.size} 个缓存删除失败".toast(context)
                            }
                        }
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ActiveOfflineCacheCard(
    card: OfflineCacheCardModel,
    selected: Boolean,
    manageMode: Boolean,
    actionModifier: Modifier,
    onClick: () -> Unit,
    onCancel: () -> Unit,
) {
    val coverModel = rememberOfflineCacheImageModel(card.cover)
    Surface(
        modifier = Modifier.height(134.dp),
        colors = SurfaceDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        ),
        shape = MaterialTheme.shapes.large,
        border = if (selected) Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) else Border.None
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (card.cover.isNotBlank()) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = card.title.ifBlank { "离线视频" },
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(card.qualityText.ifBlank { "缓存视频" })
                        if (card.totalBytes > 0L) {
                            append(" · ")
                            append(formatCacheBytes(card.downloadedBytes))
                            append(" / ")
                            append(formatCacheBytes(card.totalBytes))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { card.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    gapSize = 0.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = cacheStatusText(card),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val showCancel = !manageMode && card.status != OfflineVideoCacheStatus.Failed
                    OutlinedButton(
                        modifier = actionModifier
                            .width(if (showCancel) 58.dp else 72.dp)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        onClick = onClick
                    ) {
                        Text(
                            text = when {
                                manageMode && selected -> "取消选择"
                                manageMode -> "选择"
                                card.status == OfflineVideoCacheStatus.Paused -> "继续"
                                card.status == OfflineVideoCacheStatus.Failed -> "清除"
                                else -> "暂停"
                            },
                            maxLines = 1
                        )
                    }
                    if (showCancel) {
                        OutlinedButton(
                            modifier = Modifier
                                .width(58.dp)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(0.dp),
                            onClick = onCancel
                        ) {
                            Text("取消", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineCacheCard(
    modifier: Modifier = Modifier,
    card: OfflineCacheCardModel,
    selected: Boolean,
    manageMode: Boolean,
    onClick: () -> Unit,
) {
    val coverModel = rememberOfflineCacheImageModel(card.cover)
    val avatarModel = rememberOfflineCacheImageModel(card.upFace)
    Surface(
        modifier = modifier.height(220.dp),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) else Border.None,
            focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.92f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (card.cover.isNotBlank()) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (card.durationMs > 0L) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = SurfaceDefaults.colors(
                            containerColor = Color.Black.copy(alpha = 0.76f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = card.durationMs.formatHourMinSec(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp),
                    shape = CircleShape,
                    colors = SurfaceDefaults.colors(
                        containerColor = if (manageMode && selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
                    ),
                    border = if (manageMode && !selected) {
                        Border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant))
                    } else Border.None
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (!manageMode || selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = if (manageMode) "已选择" else "已缓存",
                                modifier = Modifier.size(15.dp),
                                tint = if (manageMode && selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    card.title.ifBlank { "离线视频" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card.upFace.isNotBlank()) {
                            AsyncImage(
                                model = avatarModel,
                                contentDescription = "${card.upName}头像",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        card.upName.ifBlank { "未知 UP 主" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "弹幕数",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatOfflineStatCount(card.danmakuCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = buildString {
                            append(card.qualityText.ifBlank { "缓存视频" })
                            if (card.downloadedBytes > 0L) {
                                append(" · ")
                                append(formatCacheBytes(card.downloadedBytes))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberOfflineCacheImageModel(url: String): ImageRequest? {
    val context = LocalContext.current
    return remember(context, url) {
        url.takeIf { it.isNotBlank() }?.let {
            ImageRequest.Builder(context)
                .data(it)
                .networkCachePolicy(CachePolicy.DISABLED)
                .build()
        }
    }
}

private fun formatOfflineStatCount(count: Int): String = when {
    count >= 100_000 -> "${count / 10_000}万"
    count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
    else -> count.coerceAtLeast(0).toString()
}

private fun cacheStatusText(card: OfflineCacheCardModel): String = when (card.status) {
    OfflineVideoCacheStatus.Queued -> "等待缓存"
    OfflineVideoCacheStatus.Fetching -> "准备中"
    OfflineVideoCacheStatus.DownloadingVideo,
    OfflineVideoCacheStatus.DownloadingAudio,
    OfflineVideoCacheStatus.DownloadingDanmaku -> "缓存中 ${(card.progress * 100).toInt()}%"
    OfflineVideoCacheStatus.Paused -> "已暂停 · 点击继续"
    OfflineVideoCacheStatus.Completed -> "已缓存 · 点击播放"
    OfflineVideoCacheStatus.Failed -> "失败 · 点击清除"
    OfflineVideoCacheStatus.Idle -> ""
}

private fun formatCacheBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (value >= 10 || index == 0) "${value.toInt()} ${units[index]}"
    else "%.1f %s".format(value, units[index])
}
