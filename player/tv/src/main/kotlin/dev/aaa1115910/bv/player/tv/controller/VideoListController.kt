package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.player.entity.findCurrentVideoListItem
import dev.aaa1115910.bv.player.entity.matchesCurrentVideoCid
import dev.aaa1115910.bv.player.shared.R
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
import dev.aaa1115910.bv.util.requestFocus
import java.util.Locale

private val PlaylistPanelWidth = 520.dp
private val PlaylistContentWidth = 380.dp
private val PlaylistItemShape = RoundedCornerShape(10.dp)
private const val PlaylistContextItemsBeforeCurrent = 3

private enum class PlaylistKind {
    Pgc,
    Parts,
    Ugc,
    UgcMultiPart,
    Interactive,
}

private data class PlaylistHeaderInfo(
    val title: String,
    val current: Int,
    val total: Int,
    val kind: PlaylistKind,
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun VideoListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    onPlayNewVideo: (VideoListItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val videoInfoData = LocalVideoPlayerVideoInfoData.current
    val focusRequester = remember { FocusRequester() }
    val playlistBringIntoViewSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float = when {
                offset < 0f -> offset
                offset + size > containerSize -> offset + size - containerSize
                else -> 0f
            }
        }
    }
    // videoInfoRepository.videoList is a regular mutable list. Snapshot it on every
    // recomposition so opening the controller after an in-place data update does not
    // reuse a stale remembered current item and leave the playlist without focus.
    val videoList = videoPlayerConfigData.availableVideoList.toList()
    val currentItem = videoList.findCurrentVideoListItem(videoPlayerConfigData.currentVideoCid)
    val currentIndex = videoList.indexOf(currentItem)
    val focusTargetIndex = currentIndex.takeIf { it >= 0 }
        ?: videoList.indexOfFirst { it !is VideoListUgcEpisodeTitle }
    val headerInfo = buildPlaylistHeaderInfo(videoList, currentItem)
    val initialScrollIndex = resolveInitialScrollIndex(
        videoList = videoList,
        focusTargetIndex = focusTargetIndex,
        kind = headerInfo.kind,
    )

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(expandFrom = Alignment.Start),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            LaunchedEffect(
                show,
                videoList,
                videoPlayerConfigData.currentVideoCid,
                focusTargetIndex,
                initialScrollIndex,
            ) {
                if (show && focusTargetIndex in videoList.indices) {
                    listState.scrollToItem(initialScrollIndex)
                    // The target can be outside the previously composed lazy-list window.
                    // Wait for the scroll layout before requesting focus on its Surface.
                    withFrameNanos { }
                    focusRequester.requestFocus(scope)
                }
            }

            Box(
                modifier = Modifier
                    .width(PlaylistPanelWidth)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.98f),
                                Color.Black.copy(alpha = 0.94f),
                                Color.Black.copy(alpha = 0.82f),
                                Color.Black.copy(alpha = 0.48f),
                                Color.Transparent,
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .width(PlaylistContentWidth)
                        .fillMaxHeight()
                        .padding(start = 24.dp, end = 12.dp, top = 30.dp)
                ) {
                    PlaylistHeader(
                        info = headerInfo,
                        videoViewCount = videoInfoData.play,
                        videoDanmakuCount = videoInfoData.danmaku,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides playlistBringIntoViewSpec,
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .focusGroup(),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            videoList.forEachIndexed { index, video ->
                                val isPlaying = video.matchesCurrentVideoCid(
                                    currentVideoCid = videoPlayerConfigData.currentVideoCid,
                                    videoList = videoList,
                                )
                                val itemModifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (index == focusTargetIndex) {
                                            Modifier.focusRequester(focusRequester)
                                        } else {
                                            Modifier
                                        }
                                    )
                                val itemContent: @Composable () -> Unit = {
                                    PlaylistVideoItem(
                                        modifier = itemModifier,
                                        video = video,
                                        nextVideo = videoList.getOrNull(index + 1),
                                        nestedParts = headerInfo.kind == PlaylistKind.UgcMultiPart,
                                        isPlaying = isPlaying,
                                        onPlayNewVideo = onPlayNewVideo,
                                    )
                                }
                                val isEpisodeBoundary =
                                    headerInfo.kind == PlaylistKind.UgcMultiPart &&
                                        (video is VideoListUgcEpisodeTitle ||
                                            video is VideoListUgcEpisode)

                                if (isEpisodeBoundary) {
                                    stickyHeader(
                                        key = "playlist-episode-$index",
                                        contentType = video::class,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.92f))
                                        ) {
                                            itemContent()
                                        }
                                    }
                                } else {
                                    item(
                                        key = "playlist-item-$index",
                                        contentType = video::class,
                                    ) {
                                        itemContent()
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
private fun PlaylistVideoItem(
    modifier: Modifier,
    video: VideoListItem,
    nextVideo: VideoListItem?,
    nestedParts: Boolean,
    isPlaying: Boolean,
    onPlayNewVideo: (VideoListItem) -> Unit,
) {
    when (video) {
        is VideoListPart -> {
            val content: @Composable () -> Unit = {
                PartItemContent(
                    title = "P${video.index + 1} ${video.partTitle.ifBlank { video.title }}",
                    duration = video.duration,
                    isPlaying = isPlaying,
                )
            }
            if (nestedParts) {
                NestedPartItem(
                    modifier = modifier,
                    isLast = nextVideo !is VideoListPart,
                    isPlaying = isPlaying,
                    onClick = {
                        if (!isPlaying) onPlayNewVideo(video)
                    },
                    content = content,
                )
            } else {
                PlaylistClickableItem(
                    modifier = modifier,
                    isPlaying = isPlaying,
                    minHeight = 58.dp,
                    onClick = {
                        if (!isPlaying) onPlayNewVideo(video)
                    },
                    content = content,
                )
            }
        }

        is VideoListUgcEpisode -> {
            PlaylistClickableItem(
                modifier = modifier,
                isPlaying = isPlaying,
                minHeight = 82.dp,
                onClick = {
                    if (!isPlaying) onPlayNewVideo(video)
                }
            ) {
                EpisodeItemContent(
                    title = "EP${video.index + 1} ${video.partTitle.ifBlank { video.title }}",
                    cover = video.cover,
                    duration = formatDuration(video.duration),
                    viewCount = video.viewCount,
                    danmakuCount = video.danmakuCount,
                    isPlaying = isPlaying,
                )
            }
        }

        is VideoListInteractiveNode -> {
            PlaylistClickableItem(
                modifier = modifier,
                isPlaying = isPlaying,
                minHeight = 58.dp,
                onClick = {
                    if (!isPlaying) onPlayNewVideo(video)
                }
            ) {
                PartItemContent(
                    title = "分支${video.index + 1} ${video.partTitle.ifBlank { video.title }}",
                    duration = 0,
                    isPlaying = isPlaying,
                )
            }
        }

        is VideoListPgcEpisode -> {
            PlaylistClickableItem(
                modifier = modifier,
                isPlaying = isPlaying,
                minHeight = 82.dp,
                onClick = {
                    if (!isPlaying) onPlayNewVideo(video)
                }
            ) {
                EpisodeItemContent(
                    title = video.partTitle.ifBlank { video.title },
                    cover = video.cover,
                    duration = formatDuration(video.duration / 1000),
                    viewCount = video.viewCount,
                    danmakuCount = video.danmakuCount,
                    isPlaying = isPlaying,
                )
            }
        }

        is VideoListUgcEpisodeTitle -> {
            UgcParentEpisodeItem(
                modifier = modifier,
                episode = video,
            )
        }
    }
}

@Composable
private fun PlaylistHeader(
    info: PlaylistHeaderInfo,
    videoViewCount: Long,
    videoDanmakuCount: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = info.title,
                color = PlayerColors.textPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                )
            )
            Spacer(modifier = Modifier.width(18.dp))
            val usesEpisodeNumber =
                info.kind == PlaylistKind.Ugc || info.kind == PlaylistKind.UgcMultiPart
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (usesEpisodeNumber) "EP${info.current}" else info.current.toString(),
                    color = if (usesEpisodeNumber) {
                        PlayerColors.accentPink
                    } else {
                        PlayerColors.textSecondary
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = if (usesEpisodeNumber) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                )
                Text(
                    text = " / ${info.total}",
                    color = PlayerColors.textSecondary,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                )
            }
        }
        if (info.kind == PlaylistKind.Parts) {
            MetadataRow(
                viewCount = videoViewCount,
                danmakuCount = videoDanmakuCount,
                duration = "",
            )
        }
    }
}

@Composable
private fun PlaylistClickableItem(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    minHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .heightIn(min = minHeight)
            .semantics {
                if (isPlaying) stateDescription = "正在播放"
            },
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isPlaying) Color.White.copy(alpha = 0.045f) else Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.11f),
            pressedContainerColor = Color.White.copy(alpha = 0.14f),
        ),
        scale = ClickableSurfaceDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
            pressedScale = 1f,
        ),
        shape = ClickableSurfaceDefaults.shape(shape = PlaylistItemShape),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.94f)),
                shape = PlaylistItemShape,
            ),
            pressedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = PlaylistItemShape,
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                )
            ) {
                content()
            }
            if (isPlaying) {
                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .padding(vertical = 6.dp)
                            .width(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PlayerColors.accentPink)
                    )
                }
            }
        }
    }
}

@Composable
private fun PartItemContent(
    title: String,
    duration: Int,
    isPlaying: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                color = PlayerColors.textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (duration > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                DurationText(formatDuration(duration))
            }
        }
        if (isPlaying) PlaybackBadge()
    }
}

@Composable
private fun EpisodeItemContent(
    title: String,
    cover: String?,
    duration: String,
    viewCount: Long,
    danmakuCount: Int,
    isPlaying: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!cover.isNullOrBlank()) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 100.dp, height = 57.dp)
                    .clip(RoundedCornerShape(7.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = PlayerColors.textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isPlaying) PlaybackBadge()
            MetadataRow(
                viewCount = viewCount,
                danmakuCount = danmakuCount,
                duration = duration,
            )
        }
    }
}

@Composable
private fun UgcParentEpisodeItem(
    modifier: Modifier = Modifier,
    episode: VideoListUgcEpisodeTitle,
) {
    Row(
        modifier = modifier
            .heightIn(min = 72.dp)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!episode.cover.isNullOrBlank()) {
            AsyncImage(
                model = episode.cover,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 86.dp, height = 49.dp)
                    .clip(RoundedCornerShape(7.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(PlayerColors.accentPink.copy(alpha = 0.16f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "EP${episode.index + 1}",
                        color = PlayerColors.accentPink,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = episode.title,
                    color = PlayerColors.textPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MetadataRow(
                viewCount = episode.viewCount,
                danmakuCount = episode.danmakuCount,
                duration = formatDuration(episode.duration),
            )
        }
    }
}

@Composable
private fun NestedPartItem(
    modifier: Modifier = Modifier,
    isLast: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val connectorColor = PlayerColors.textGhost
    val connectorX = 10.dp
    val branchEndX = 24.dp
    val gapOverlap = 6.dp
    Box(
        modifier = modifier.drawBehind {
            val x = connectorX.toPx()
            val centerY = size.height / 2f
            drawLine(
                color = connectorColor,
                start = androidx.compose.ui.geometry.Offset(x, -gapOverlap.toPx()),
                end = androidx.compose.ui.geometry.Offset(
                    x,
                    if (isLast) centerY else size.height + gapOverlap.toPx(),
                ),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = connectorColor,
                start = androidx.compose.ui.geometry.Offset(x, centerY),
                end = androidx.compose.ui.geometry.Offset(branchEndX.toPx(), centerY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = connectorColor,
                radius = 2.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, centerY),
            )
        }
    ) {
        PlaylistClickableItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = branchEndX),
            isPlaying = isPlaying,
            minHeight = 56.dp,
            onClick = onClick,
            content = content,
        )
    }
}

@Composable
private fun PlaybackBadge() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Equalizer,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = PlayerColors.accentPink,
        )
        Text(
            text = "正在播放",
            color = PlayerColors.accentPink,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun MetadataRow(
    viewCount: Long,
    danmakuCount: Int,
    duration: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (viewCount > 0) {
                MetadataItem(
                    iconRes = R.drawable.ic_play_count,
                    text = "${formatCount(viewCount)}播放",
                )
            }
            if (danmakuCount > 0) {
                MetadataItem(
                    iconRes = R.drawable.ic_danmaku_count,
                    text = "${formatCount(danmakuCount.toLong())}弹幕",
                )
            }
        }
        if (duration.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            DurationText(duration)
        }
    }
}

@Composable
private fun MetadataItem(
    iconRes: Int,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = PlayerColors.textTertiary,
        )
        Text(
            text = text,
            color = PlayerColors.textTertiary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun DurationText(text: String) {
    Text(
        text = text,
        color = PlayerColors.textTertiary,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        maxLines = 1,
    )
}

private fun buildPlaylistHeaderInfo(
    videoList: List<VideoListItem>,
    currentItem: VideoListItem?,
): PlaylistHeaderInfo {
    val kind = when {
        videoList.any { it is VideoListPgcEpisode } -> PlaylistKind.Pgc
        videoList.any { it is VideoListUgcEpisodeTitle } -> PlaylistKind.UgcMultiPart
        videoList.any { it is VideoListUgcEpisode } -> PlaylistKind.Ugc
        videoList.any { it is VideoListInteractiveNode } -> PlaylistKind.Interactive
        else -> PlaylistKind.Parts
    }
    val currentFlatIndex = videoList.indexOf(currentItem)

    return when (kind) {
        PlaylistKind.Pgc -> {
            val episodes = videoList.filterIsInstance<VideoListPgcEpisode>()
            val current = (currentItem as? VideoListPgcEpisode)?.index?.plus(1) ?: 1
            PlaylistHeaderInfo("选集", current, episodes.size.coerceAtLeast(1), kind)
        }

        PlaylistKind.Parts -> {
            val parts = videoList.filterIsInstance<VideoListPart>()
            val current = (currentItem as? VideoListPart)?.index?.plus(1) ?: 1
            PlaylistHeaderInfo("分 P", current, parts.size.coerceAtLeast(1), kind)
        }

        PlaylistKind.Ugc -> {
            val episodes = videoList.filterIsInstance<VideoListUgcEpisode>()
            val current = (currentItem as? VideoListUgcEpisode)?.index?.plus(1) ?: 1
            val total = episodes.maxOfOrNull { it.index + 1 } ?: episodes.size.coerceAtLeast(1)
            PlaylistHeaderInfo("合集", current, total, kind)
        }

        PlaylistKind.UgcMultiPart -> {
            val episodeMarkers = videoList.mapNotNull {
                when (it) {
                    is VideoListUgcEpisode -> it.index
                    is VideoListUgcEpisodeTitle -> it.index
                    else -> null
                }
            }
            val currentEpisodeIndex = if (currentFlatIndex >= 0) {
                when (
                    val marker = videoList
                        .take(currentFlatIndex + 1)
                        .asReversed()
                        .firstOrNull {
                            it is VideoListUgcEpisode || it is VideoListUgcEpisodeTitle
                        }
                ) {
                    is VideoListUgcEpisode -> marker.index
                    is VideoListUgcEpisodeTitle -> marker.index
                    else -> null
                }
            } else {
                null
            }
            PlaylistHeaderInfo(
                title = "合集",
                current = (currentEpisodeIndex ?: episodeMarkers.firstOrNull() ?: 0) + 1,
                total = (episodeMarkers.maxOrNull()?.plus(1) ?: 1).coerceAtLeast(1),
                kind = kind,
            )
        }

        PlaylistKind.Interactive -> {
            val nodes = videoList.filterIsInstance<VideoListInteractiveNode>()
            val current = (currentItem as? VideoListInteractiveNode)?.index?.plus(1) ?: 1
            PlaylistHeaderInfo("互动", current, nodes.size.coerceAtLeast(1), kind)
        }
    }
}

private fun resolveInitialScrollIndex(
    videoList: List<VideoListItem>,
    focusTargetIndex: Int,
    kind: PlaylistKind,
): Int {
    if (focusTargetIndex !in videoList.indices) return focusTargetIndex
    if (kind != PlaylistKind.UgcMultiPart) return focusTargetIndex

    val parentIndex = videoList
        .take(focusTargetIndex + 1)
        .indexOfLast {
            it is VideoListUgcEpisodeTitle || it is VideoListUgcEpisode
        }
        .takeIf { it >= 0 }
        ?: focusTargetIndex
    val contextIndex = (focusTargetIndex - PlaylistContextItemsBeforeCurrent).coerceAtLeast(0)

    // Keep the EP marker when the current P is near it. For a deep P list, anchor
    // close enough to the current item that it is guaranteed to enter composition.
    return maxOf(parentIndex, contextIndex)
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.CHINA, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.CHINA, "%02d:%02d", m, s)
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 100_000_000L -> formatCompactCount(count / 100_000_000.0, "亿")
        count >= 10_000L -> formatCompactCount(count / 10_000.0, "万")
        else -> count.toString()
    }
}

private fun formatCompactCount(value: Double, unit: String): String {
    val formatted = String.format(Locale.CHINA, "%.1f", value).removeSuffix(".0")
    return "$formatted$unit"
}
