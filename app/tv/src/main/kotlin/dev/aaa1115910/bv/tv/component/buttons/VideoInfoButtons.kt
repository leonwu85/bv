package dev.aaa1115910.bv.tv.component.buttons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.offline.OfflineVideoCacheStatus
import dev.aaa1115910.bv.util.formatHourMinSec

private enum class VideoInfoAction {
    Like,
    Favorite,
    Coin,
    Cache,
    Charge,
    Description,
    Comment,
    Related,
}

private fun VideoInfoAction.buttonWidth(cacheStatus: OfflineVideoCacheStatus): Dp = when (this) {
    VideoInfoAction.Like,
    VideoInfoAction.Favorite,
    VideoInfoAction.Coin -> 72.dp
    VideoInfoAction.Cache -> when (cacheStatus) {
        OfflineVideoCacheStatus.Queued,
        OfflineVideoCacheStatus.Fetching,
        OfflineVideoCacheStatus.DownloadingVideo,
        OfflineVideoCacheStatus.DownloadingAudio,
        OfflineVideoCacheStatus.DownloadingDanmaku -> 116.dp
        else -> 72.dp
    }
    VideoInfoAction.Charge -> 92.dp
    VideoInfoAction.Description,
    VideoInfoAction.Comment,
    VideoInfoAction.Related -> 44.dp
}

@Composable
fun VideoInfoButtons(
    modifier: Modifier = Modifier,
    playButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    chargeButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    cacheButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    commentButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    relatedButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    lastPlayedTime: Int = 0,
    isLogin: Boolean,
    onPlay: () -> Unit,
    showChargeButton: Boolean = false,
    onCharge: () -> Unit = {},
    isLike: Boolean,
    onAddLike: () -> Unit = {},
    onDelLike: () -> Unit = {},
    isCoin: Boolean,
    onAddCoin: () -> Unit = {},
    isFavorite: Boolean,
    userFavoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    favoriteFolderIds: List<Long> = emptyList(),
    onAddToDefaultFavoriteFolder: () -> Unit = {},
    onUpdateFavoriteFolders: (List<Long>) -> Unit = {},
    offlineCacheStatus: OfflineVideoCacheStatus = OfflineVideoCacheStatus.Idle,
    offlineCacheProgress: Float = 0f,
    onCache: () -> Unit = {},
    hasDescription: Boolean = false,
    onShowDescription: () -> Unit = {},
    onShowComment: () -> Unit = {},
    hasRelatedVideos: Boolean = false,
    onShowRelated: () -> Unit = {},
) {
    val outlinedColors = OutlinedButtonDefaults.colors()
    val outlinedBorder = OutlinedButtonDefaults.border()
    val outlinedContentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    val playButtonContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    val playButtonWidth = if (lastPlayedTime > 0) 164.dp else 88.dp
    val itemSpacing = 10.dp
    val moreButtonWidth = 78.dp
    val moreMenuFocusRequester = remember { FocusRequester() }
    var showMoreActions by remember { mutableStateOf(false) }

    val mandatoryActions = buildList {
        if (isLogin) {
            add(VideoInfoAction.Like)
            add(VideoInfoAction.Favorite)
            add(VideoInfoAction.Coin)
        }
        add(VideoInfoAction.Cache)
    }
    val optionalActions = buildList {
        if (showChargeButton) add(VideoInfoAction.Charge)
        if (hasDescription) add(VideoInfoAction.Description)
        add(VideoInfoAction.Comment)
        if (hasRelatedVideos) add(VideoInfoAction.Related)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val allActions = mandatoryActions + optionalActions
        val allButtonsWidth = allActions.fold(playButtonWidth) { width, action ->
            width + itemSpacing + action.buttonWidth(offlineCacheStatus)
        }

        val visibleOptionalActions: List<VideoInfoAction>
        val overflowActions: List<VideoInfoAction>
        if (allButtonsWidth <= maxWidth) {
            visibleOptionalActions = optionalActions
            overflowActions = emptyList()
        } else {
            var occupiedWidth = playButtonWidth + moreButtonWidth + itemSpacing
            mandatoryActions.forEach { action ->
                occupiedWidth += itemSpacing + action.buttonWidth(offlineCacheStatus)
            }
            val fittingActions = mutableListOf<VideoInfoAction>()
            optionalActions.forEach { action ->
                val candidateWidth = occupiedWidth + itemSpacing + action.buttonWidth(offlineCacheStatus)
                if (candidateWidth <= maxWidth) {
                    fittingActions += action
                    occupiedWidth = candidateWidth
                }
            }
            visibleOptionalActions = fittingActions
            overflowActions = optionalActions - fittingActions.toSet()
        }
        val visibleActions = mandatoryActions + visibleOptionalActions

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .width(playButtonWidth)
                    .focusRequester(playButtonFocusRequester),
                contentPadding = playButtonContentPadding
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Text(
                        text = if (lastPlayedTime > 0) {
                            stringResource(
                                R.string.video_info_continue_play,
                                (lastPlayedTime * 1000L).formatHourMinSec()
                            )
                        } else {
                            stringResource(R.string.video_info_play)
                        },
                        maxLines = 1
                    )
                }
            }

            visibleActions.forEach { action ->
                val actionModifier = Modifier.width(action.buttonWidth(offlineCacheStatus))
                when (action) {
                    VideoInfoAction.Like -> LikeButton(
                        modifier = actionModifier,
                        isLike = isLike,
                        onToggleLike = { if (isLike) onDelLike() else onAddLike() },
                        colors = outlinedColors,
                        border = outlinedBorder,
                        contentPadding = outlinedContentPadding
                    )
                    VideoInfoAction.Favorite -> FavoriteButton(
                        modifier = actionModifier,
                        isFavorite = isFavorite,
                        userFavoriteFolders = userFavoriteFolders,
                        favoriteFolderIds = favoriteFolderIds,
                        onAddToDefaultFavoriteFolder = onAddToDefaultFavoriteFolder,
                        onUpdateFavoriteFolders = onUpdateFavoriteFolders,
                        colors = outlinedColors,
                        border = outlinedBorder,
                        contentPadding = outlinedContentPadding
                    )
                    VideoInfoAction.Cache -> CacheActionButton(
                        modifier = actionModifier.focusRequester(cacheButtonFocusRequester),
                        status = offlineCacheStatus,
                        progress = offlineCacheProgress,
                        onClick = onCache,
                        contentPadding = outlinedContentPadding
                    )
                    VideoInfoAction.Coin -> CoinButton(
                        modifier = actionModifier,
                        isCoin = isCoin,
                        onAddCoin = onAddCoin,
                        colors = outlinedColors,
                        border = outlinedBorder,
                        contentPadding = outlinedContentPadding
                    )
                    VideoInfoAction.Charge -> OutlinedButton(
                        modifier = actionModifier.focusRequester(chargeButtonFocusRequester),
                        onClick = onCharge,
                        colors = outlinedColors,
                        border = outlinedBorder,
                        contentPadding = outlinedContentPadding
                    ) { Text("前往充电", maxLines = 1) }
                    VideoInfoAction.Description -> VideoInfoIconAction(
                        modifier = actionModifier,
                        icon = Icons.Rounded.Info,
                        contentDescription = "简介",
                        onClick = onShowDescription
                    )
                    VideoInfoAction.Comment -> VideoInfoIconAction(
                        modifier = actionModifier.focusRequester(commentButtonFocusRequester),
                        icon = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "评论",
                        onClick = onShowComment
                    )
                    VideoInfoAction.Related -> VideoInfoIconAction(
                        modifier = actionModifier.focusRequester(relatedButtonFocusRequester),
                        icon = Icons.Rounded.VideoLibrary,
                        contentDescription = "推荐视频",
                        onClick = onShowRelated
                    )
                }
            }

            if (overflowActions.isNotEmpty()) {
                var moreModifier = Modifier.width(moreButtonWidth)
                if (VideoInfoAction.Charge in overflowActions) {
                    moreModifier = moreModifier.focusRequester(chargeButtonFocusRequester)
                }
                if (VideoInfoAction.Comment in overflowActions) {
                    moreModifier = moreModifier.focusRequester(commentButtonFocusRequester)
                }
                if (VideoInfoAction.Related in overflowActions) {
                    moreModifier = moreModifier.focusRequester(relatedButtonFocusRequester)
                }
                Box {
                    OutlinedButton(
                        modifier = moreModifier,
                        onClick = { showMoreActions = true },
                        colors = outlinedColors,
                        border = outlinedBorder,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.MoreHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("更多", maxLines = 1)
                        }
                    }
                    if (showMoreActions) {
                        MoreActionsPopup(
                            actions = overflowActions,
                            isCoin = isCoin,
                            firstItemFocusRequester = moreMenuFocusRequester,
                            onDismissRequest = { showMoreActions = false },
                            onAction = { action ->
                                showMoreActions = false
                                when (action) {
                                    VideoInfoAction.Coin -> onAddCoin()
                                    VideoInfoAction.Charge -> onCharge()
                                    VideoInfoAction.Description -> onShowDescription()
                                    VideoInfoAction.Comment -> onShowComment()
                                    VideoInfoAction.Related -> onShowRelated()
                                    else -> Unit
                                }
                            }
                        )
                    }
                }
            }
        }

        LaunchedEffect(showMoreActions, overflowActions) {
            if (showMoreActions && overflowActions.isNotEmpty()) {
                moreMenuFocusRequester.requestFocus()
            }
        }
    }
}

@Composable
private fun MoreActionsPopup(
    actions: List<VideoInfoAction>,
    isCoin: Boolean,
    firstItemFocusRequester: FocusRequester,
    onDismissRequest: () -> Unit,
    onAction: (VideoInfoAction) -> Unit,
) {
    val density = LocalDensity.current
    val verticalOffset = with(density) { 50.dp.roundToPx() }
    val popupColor = MaterialTheme.colorScheme.surface

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = 0, y = verticalOffset),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Canvas(
                modifier = Modifier
                    .padding(end = 30.dp)
                    .width(18.dp)
                    .height(10.dp)
            ) {
                drawPath(
                    path = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        close()
                    },
                    color = popupColor
                )
            }
            Surface(
                modifier = Modifier.width(260.dp),
                shape = MaterialTheme.shapes.large,
                colors = SurfaceDefaults.colors(containerColor = popupColor)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("更多操作", style = MaterialTheme.typography.titleMedium)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actions.forEachIndexed { index, action ->
                            OverflowActionButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (index == 0) {
                                            Modifier.focusRequester(firstItemFocusRequester)
                                        } else Modifier
                                    ),
                                action = action,
                                isCoin = isCoin,
                                onClick = { onAction(action) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheActionButton(
    modifier: Modifier,
    status: OfflineVideoCacheStatus,
    progress: Float,
    onClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        colors = OutlinedButtonDefaults.colors(),
        border = OutlinedButtonDefaults.border(),
        contentPadding = contentPadding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = when (status) {
                    OfflineVideoCacheStatus.Completed -> Icons.Rounded.CheckCircle
                    OfflineVideoCacheStatus.Paused -> Icons.Rounded.PlayArrow
                    OfflineVideoCacheStatus.Failed -> Icons.Rounded.Refresh
                    OfflineVideoCacheStatus.Queued,
                    OfflineVideoCacheStatus.Fetching,
                    OfflineVideoCacheStatus.DownloadingVideo,
                    OfflineVideoCacheStatus.DownloadingAudio,
                    OfflineVideoCacheStatus.DownloadingDanmaku -> Icons.Rounded.Pause
                    OfflineVideoCacheStatus.Idle -> Icons.Rounded.Download
                },
                contentDescription = null
            )
            Text(
                text = when (status) {
                    OfflineVideoCacheStatus.Completed -> "完成"
                    OfflineVideoCacheStatus.Paused -> "继续"
                    OfflineVideoCacheStatus.Failed -> "重试"
                    OfflineVideoCacheStatus.Queued -> "排队中"
                    OfflineVideoCacheStatus.Fetching -> "准备中"
                    OfflineVideoCacheStatus.DownloadingVideo,
                    OfflineVideoCacheStatus.DownloadingAudio,
                    OfflineVideoCacheStatus.DownloadingDanmaku -> "缓存 ${(progress * 100).toInt()}%"
                    OfflineVideoCacheStatus.Idle -> "缓存"
                },
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VideoInfoIconAction(
    modifier: Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
        colors = OutlinedButtonDefaults.colors(),
        border = OutlinedButtonDefaults.border()
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun OverflowActionButton(
    modifier: Modifier,
    action: VideoInfoAction,
    isCoin: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val icon = when (action) {
            VideoInfoAction.Coin -> Icons.Rounded.Paid
            VideoInfoAction.Description -> Icons.Rounded.Info
            VideoInfoAction.Comment -> Icons.Rounded.ChatBubbleOutline
            VideoInfoAction.Related -> Icons.Rounded.VideoLibrary
            else -> null
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(
                text = when (action) {
                    VideoInfoAction.Coin -> if (isCoin) "已投币" else "投币"
                    VideoInfoAction.Charge -> "前往充电"
                    VideoInfoAction.Description -> "查看简介"
                    VideoInfoAction.Comment -> "查看评论"
                    VideoInfoAction.Related -> "相关推荐"
                    else -> "更多操作"
                },
                maxLines = 1
            )
        }
    }
}
