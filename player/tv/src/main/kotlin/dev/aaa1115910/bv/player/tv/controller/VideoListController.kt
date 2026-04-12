package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.player.shared.R
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.delay

@Composable
fun VideoListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    onPlayNewVideo: (VideoListItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val focusRequester = remember { FocusRequester() }
    val videoListContainsUgcEpisode by remember {
        derivedStateOf {
            videoPlayerConfigData.availableVideoList.any { it is VideoListUgcEpisode }
        }
    }

    Box {
        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            // 在动画内容中处理滚动和焦点请求
            LaunchedEffect(Unit) {
                val currentIndex = videoPlayerConfigData.availableVideoList
                    .indexOfFirst {
                        when (it) {
                            is VideoListInteractiveNode -> it.isCurrent
                                || (videoPlayerConfigData.currentVideoCid == it.cid && !videoPlayerConfigData.availableVideoList.any { listItem ->
                                    listItem is VideoListInteractiveNode && listItem.isCurrent
                                })
                            is VideoListItemData -> it.cid == videoPlayerConfigData.currentVideoCid
                            else -> false
                        }
                    }
                if (currentIndex >= 0 && currentIndex < videoPlayerConfigData.availableVideoList.size) {
                    listState.scrollToItem(currentIndex)
                }
                focusRequester.requestFocus(scope)
            }
            Surface(
                modifier = modifier,
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(400.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 120.dp)
                    ) {
                        items(items = videoPlayerConfigData.availableVideoList) { video ->
                            when (video) {
                                is VideoListPart -> {
                                    val isSelected =
                                        video.cid == videoPlayerConfigData.currentVideoCid
                                    val itemModifier = if (isSelected) {
                                        Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }
                                    ListItem(
                                        modifier = itemModifier,
                                        headlineContent = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    modifier = Modifier.weight(1f),
                                                    text = (" - ".takeIf { videoListContainsUgcEpisode }
                                                        ?: "") + "P${video.index + 1} ${if (video.partTitle.isNotEmpty()) video.partTitle else video.title}",
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (video.duration > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = formatDuration(video.duration),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = LocalContentColor.current.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = { if (!isSelected) onPlayNewVideo(video) },
                                        selected = isSelected
                                    )
                                }

                                is VideoListUgcEpisode -> {
                                    val isSelected =
                                        video.cid == videoPlayerConfigData.currentVideoCid
                                    val itemModifier = if (isSelected) {
                                        Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }
                                    ListItem(
                                        modifier = itemModifier,
                                        headlineContent = {
                                            EpisodeItemContent(
                                                title = "EP${video.index + 1} ${if (video.partTitle.isNotEmpty()) video.partTitle else video.title}",
                                                cover = video.cover,
                                                duration = video.duration,
                                                viewCount = video.viewCount,
                                                danmakuCount = video.danmakuCount
                                            )
                                        },
                                        onClick = { if (!isSelected) onPlayNewVideo(video) },
                                        selected = isSelected
                                    )
                                }

                                is VideoListInteractiveNode -> {
                                    val isSelected =
                                        video.isCurrent || (
                                            video.cid == videoPlayerConfigData.currentVideoCid &&
                                                !videoPlayerConfigData.availableVideoList.any { it is VideoListInteractiveNode && it.isCurrent }
                                            )
                                    val itemModifier = if (isSelected) {
                                        Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }
                                    ListItem(
                                        modifier = itemModifier,
                                        headlineContent = {
                                            Text(
                                                text = "分支${video.index + 1} ${if (video.partTitle.isNotEmpty()) video.partTitle else video.title}"
                                            )
                                        },
                                        onClick = { if (!isSelected) onPlayNewVideo(video) },
                                        selected = isSelected
                                    )
                                }

                                is VideoListPgcEpisode -> {
                                    val isSelected =
                                        video.cid == videoPlayerConfigData.currentVideoCid
                                    val itemModifier = if (isSelected) {
                                        Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }
                                    ListItem(
                                        modifier = itemModifier,
                                        headlineContent = {
                                            EpisodeItemContent(
                                                title = video.partTitle,
                                                cover = video.cover,
                                                duration = video.duration,
                                                viewCount = video.viewCount,
                                                danmakuCount = video.danmakuCount
                                            )
                                        },
                                        onClick = { if (!isSelected) onPlayNewVideo(video) },
                                        selected = isSelected
                                    )
                                }

                                is VideoListUgcEpisodeTitle -> {
                                    Text(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 12.dp
                                        ),
                                        text = "EP${video.index + 1} ${video.title}",
                                        style = MaterialTheme.typography.titleMedium
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

@Composable
private fun EpisodeItemContent(
    title: String,
    cover: String?,
    duration: Int,
    viewCount: Long,
    danmakuCount: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!cover.isNullOrEmpty()) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_count),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = LocalContentColor.current.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatCount(viewCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalContentColor.current.copy(alpha = 0.6f)
                        )
                    }
                }
                if (danmakuCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_danmaku_count),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = LocalContentColor.current.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatCount(danmakuCount.toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalContentColor.current.copy(alpha = 0.6f)
                        )
                    }
                }
                if (duration > 0) {
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

private fun formatCount(count: Long): String {
    return if (count >= 10000) {
        String.format("%.1f万", count / 10000.0)
    } else {
        "$count"
    }
}