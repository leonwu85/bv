package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekState
import dev.aaa1115910.bv.player.entity.VideoPlayerStateData
import dev.aaa1115910.bv.player.entity.VideoRotation
import dev.aaa1115910.bv.player.seekbar.SeekMoveState
import dev.aaa1115910.bv.player.shared.R
import dev.aaa1115910.bv.player.tv.VideoSeekBar
import coil.compose.AsyncImage
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private fun formatSpeed(speed: Float): String {
    return "${(speed * 100).roundToInt() / 100f}x"
}

private class ControllerBottomBarAutoHideState {
    var hideVideoInfoJob: Job? = null
    var pauseAutoHide: Boolean = false
}

@Composable
fun ControllerBottomBar(
    show: Boolean,
    onHideInfo: () -> Unit,
    modifier: Modifier = Modifier,
    playSpeed: Float = 1f,
    rotation: VideoRotation,
    title: String,
    partTitle: String,
    seekData: VideoPlayerSeekState,
    stateData: VideoPlayerStateData,
    idleIcon: String,
    movingIcon: String,
    play: Long,
    danmaku: Int,
    like: Int,
    coin: Int,
    favorite: Int,
    upName: String,
    upAvatar: String = "",
    pubTime: String,
    isPlaying: Boolean,
    isLoop: Boolean,
    showDanmaku: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    isAudioOnly: Boolean = false,
    onTogglePlaybackMediaMode: () -> Unit = {},
    onOpenUpSpace: () -> Unit,
    onRefreshVideo: () -> Unit,
    onOpenDanmaku: () -> Unit,
    onHideDanmaku: () -> Unit,
    onOpenPlayList: () -> Unit,
    onOpenRelatedVideo: () -> Unit,
    onOpenSetting: () -> Unit,
    onLoopPlayModeChange: (Boolean) -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
    fromSeason: Boolean = false,
    isFollowingUp: Boolean = false,
    userActionContent: @Composable (
        modifier: Modifier,
        focusMap: Map<String, FocusRequester>,
        onFocus: (String) -> Unit,
        onPauseAutoHide: (Boolean) -> Unit,
    ) -> Unit = { _, _, _, _ -> },
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    availableSubtitleTracks: List<Subtitle> = emptyList(),
    currentSubtitleId: Long,
    onSubtitleChange: (Long) -> Unit,
    onLoadNextVideo: (Boolean) -> Unit,
    onLoadPrevVideo: () -> Unit = {},
    onShowComment: () -> Unit = {},
    onShowDescription: () -> Unit = {},
    onTripleLike: () -> Unit = {},
    onToggleFollow: () -> Unit = {},
    onReportLiveHistory: () -> Unit = {},
    liveIncognitoMode: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val isLive = videoPlayerConfigData.isLive

    // 根据播放列表位置决定上/下一集按钮可见性
    val videoList = videoPlayerConfigData.availableVideoList
        .filterIsInstance<dev.aaa1115910.bv.player.entity.VideoListItemData>()
    val currentIndex = videoList.indexOfFirst { it.cid == videoPlayerConfigData.currentVideoCid }
    val showPrevVideoBtn = currentIndex > 0
    val showNextVideoBtn = currentIndex in 0 until videoList.size - 1

    val autoHideState = remember { ControllerBottomBarAutoHideState() }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showRotationDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(playSpeed) }
    val danmakuIconId =
        if (showDanmaku) R.drawable.ic_danmaku_on else R.drawable.ic_danmaku_hide
    val subtitleIconId =
        if (currentSubtitleId > 0) R.drawable.ic_subtitle_on else R.drawable.ic_subtitle_off
    val hasSubtitles = availableSubtitleTracks.isNotEmpty()

    // ── 核心按钮 ──
    val buttons = remember(
        fromSeason, showDanmaku, isPlaying, isLoop,
        showPrevVideoBtn, showNextVideoBtn, isLive,
        speed, isAudioOnly, rotation, hasSubtitles, currentSubtitleId
    ) {
        listOf(
            ControlButton(
                id = "comment",
                icon = Icons.AutoMirrored.Rounded.Chat,
                onClick = onShowComment,
            ),
            ControlButton(
                id = "prevVideo",
                painterId = R.drawable.prev_play_fill,
                scale = 0.7f,
                onClick = { onLoadPrevVideo() },
                visible = showPrevVideoBtn
            ),
            ControlButton(
                id = "nextVideo",
                painterId = R.drawable.next_play_fill,
                scale = 0.7f,
                onClick = { onLoadNextVideo(true) },
                visible = showNextVideoBtn
            ),
            ControlButton(
                id = "audioMode",
                icon = Icons.Rounded.Headphones,
                onClick = onTogglePlaybackMediaMode,
                selected = isAudioOnly
            ),
            ControlButton(
                id = "danmaku",
                painterId = danmakuIconId,
                onClick = { if (showDanmaku) onHideDanmaku() else onOpenDanmaku() }
            ),
            ControlButton(
                id = "subtitle",
                painterId = subtitleIconId,
                onClick = { showSubtitleDialog = true },
                visible = hasSubtitles,
                selected = currentSubtitleId > 0
            ),
            ControlButton(
                id = "loop",
                icon = if (isLoop) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                onClick = { onLoopPlayModeChange(!isLoop) }
            ),
            ControlButton(
                id = "speed",
                text = formatSpeed(speed),
                width = 48,
                onClick = { showSpeedDialog = true },
                visible = !isLive,
                selected = speed != 1f,
                alwaysShowBorder = true
            ),
            ControlButton(
                id = "refresh",
                icon = Icons.Rounded.Refresh,
                onClick = onRefreshVideo
            ),
            ControlButton(
                id = "rotation",
                icon = Icons.Rounded.ScreenRotation,
                onClick = { showRotationDialog = true },
                visible = !isLive,
                selected = rotation != VideoRotation.Original
            ),
            ControlButton(
                id = "related",
                icon = Icons.Rounded.KeyboardDoubleArrowDown,
                onClick = onOpenRelatedVideo,
                visible = !fromSeason && !isLive
            ),
            ControlButton(
                id = "settings",
                icon = Icons.Outlined.Settings,
                onClick = onOpenSetting,
                scale = 0.9f
            )
        ).filter { it.visible }
    }

    val focusRequesters = remember(buttons) {
        buttons.associate { it.id to FocusRequester() }.toMutableMap()
    }

    // ── 直播专用按钮 ──
    val liveButtons = remember(showDanmaku, isLive, isFollowingUp, liveIncognitoMode) {
        if (!isLive) emptyList()
        else buildList {
            add(ControlButton(
                id = "liveRefresh",
                icon = Icons.Rounded.Refresh,
                text = "刷新",
                width = 80,
                onClick = onRefreshVideo
            ))
            add(ControlButton(
                id = "liveDanmaku",
                painterId = danmakuIconId,
                onClick = { if (showDanmaku) onHideDanmaku() else onOpenDanmaku() }
            ))
            if (liveIncognitoMode) {
                add(ControlButton(
                    id = "liveHistory",
                    icon = Icons.Rounded.History,
                    text = "上报历史",
                    width = 110,
                    onClick = onReportLiveHistory
                ))
            }
            add(ControlButton(
                id = "liveSettings",
                icon = Icons.Outlined.Settings,
                onClick = onOpenSetting,
                scale = 0.9f
            ))
        }
    }

    val liveFocusRequesters = remember(liveButtons) {
        liveButtons.associate { it.id to FocusRequester() }.toMutableMap()
    }

    val userActionFocusRequesters = remember {
        mutableStateOf(
            mapOf(
                "like" to FocusRequester(),
                "fav" to FocusRequester(),
                "coin" to FocusRequester(),
                "tripleLike" to FocusRequester(),
                "description" to FocusRequester(),
                "playlist" to FocusRequester()
            )
        )
    }

    val seekbarFocusRequester = remember { FocusRequester() }
    var seekbarHasFocus by remember { mutableStateOf(false) }

    val statString by remember {
        mutableStateOf(
            if (upName.isNotEmpty()) {
                "${
                    if (play >= 10000) String.format("%.1f", play / 10000.0) + " 万" else "$play "
                }播放  ·  ${
                    if (danmaku >= 10000) String.format("%.1f", danmaku / 10000.0) + "万" else "$danmaku "
                }弹幕  ·  ${
                    if (like >= 10000) String.format("%.1f", like / 10000.0) + "万" else "$like "
                }点赞  ·  ${
                    if (favorite >= 10000) String.format("%.1f", favorite / 10000.0) + "万" else "$favorite "
                }收藏  ·  ${
                    if (coin >= 10000) String.format("%.1f", coin / 10000.0) + "万" else "$coin "
                }投币  ·  发布于 $pubTime"
            } else ""
        )
    }

    LaunchedEffect(show) {
        if (show) {
            if (isLive) {
                liveFocusRequesters[liveButtons.firstOrNull()?.id]?.requestFocus()
            } else {
                seekbarFocusRequester.requestFocus()
            }
        }
    }

    fun cancelHideJob() {
        autoHideState.hideVideoInfoJob?.cancel()
        autoHideState.hideVideoInfoJob = null
    }

    fun scheduleHideJob() {
        cancelHideJob()
        if (
            show &&
            !showSpeedDialog &&
            !showRotationDialog &&
            !showSubtitleDialog &&
            !autoHideState.pauseAutoHide
        ) {
            autoHideState.hideVideoInfoJob = scope.launch {
                delay(5000)
                withContext(Dispatchers.Main) { onHideInfo() }
            }
        }
    }

    LaunchedEffect(show, showSpeedDialog, showRotationDialog, showSubtitleDialog) {
        scheduleHideJob()
    }
    DisposableEffect(Unit) {
        onDispose { cancelHideJob() }
    }

    Column(
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    scheduleHideJob()
                }
                false
            }
            .background(
                Brush.verticalGradient(PlayerColors.controllerScrimBottom)
            ),
        verticalArrangement = Arrangement.Bottom
    ) {
        Spacer(modifier = Modifier.padding(top = 32.dp))

        // ── 标题 ──
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = "${if (title.contains(partTitle)) "" else "$partTitle ｜ "}$title",
            color = PlayerColors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineSmall,
        )

        if (isLive) {
            // ══════════════ 直播专用布局 ══════════════

            // ── UP主信息 + 关注按钮 ──
            if (upName.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(start = 32.dp, end = 32.dp, top = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // UP主按钮
                    Button(
                        modifier = Modifier
                            .height(30.dp)
                            .onFocusChanged { if (it.isFocused) scheduleHideJob() },
                        onClick = onOpenUpSpace,
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(15.dp)),
                        scale = ButtonDefaults.scale(focusedScale = 1.05f),
                        contentPadding = PaddingValues(start = 0.dp, end = 10.dp, top = 0.dp, bottom = 0.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            focusedContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = ButtonDefaults.border(
                            border = Border(border = BorderStroke(0.dp, Color.Transparent)),
                            focusedBorder = Border(border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)))
                        )
                    ) {
                        if (upAvatar.isNotEmpty()) {
                            AsyncImage(
                                modifier = Modifier.size(28.dp).clip(CircleShape),
                                model = upAvatar,
                                contentDescription = upName,
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = upName,
                            color = PlayerColors.textPrimary,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // 关注/取消关注按钮
                    Button(
                        modifier = Modifier
                            .height(30.dp)
                            .onFocusChanged { if (it.isFocused) scheduleHideJob() },
                        onClick = onToggleFollow,
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(15.dp)),
                        scale = ButtonDefaults.scale(focusedScale = 1.05f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = if (isFollowingUp) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = ButtonDefaults.border(
                            border = Border(border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))),
                            focusedBorder = Border(border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)))
                        )
                    ) {
                        Icon(
                            modifier = Modifier.scale(0.7f),
                            imageVector = if (isFollowingUp) Icons.Outlined.PersonRemove else Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            tint = PlayerColors.textPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isFollowingUp) "已关注" else "关注",
                            color = PlayerColors.textPrimary,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 直播功能按钮行 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                liveButtons.forEach { button ->
                    Button(
                        modifier = Modifier
                            .height(48.dp)
                            .width(if (button.text != null) (button.width ?: 48).dp else 48.dp)
                            .focusRequester(liveFocusRequesters[button.id] ?: FocusRequester())
                            .onFocusChanged { if (it.isFocused) scheduleHideJob() },
                        onClick = button.onClick,
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        scale = ButtonDefaults.scale(focusedScale = 1.1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = PlayerColors.buttonDefault,
                            focusedContainerColor = PlayerColors.buttonFocused
                        ),
                        border = ButtonDefaults.border(
                            border = Border(border = BorderStroke(1.dp, PlayerColors.buttonDefault)),
                            focusedBorder = Border(border = BorderStroke(1.5.dp, PlayerColors.buttonFocusedBorder))
                        )
                    ) {
                        if (button.text != null && button.icon != null) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .ifElse(button.scale != 1f, Modifier.scale(button.scale)),
                                    imageVector = button.icon,
                                    contentDescription = null,
                                    tint = button.tint
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = button.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = button.tint
                                )
                            }
                        } else if (button.painterId != null) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .ifElse(button.scale != 1f, Modifier.scale(button.scale)),
                                    painter = painterResource(id = button.painterId),
                                    contentDescription = null,
                                    tint = button.tint
                                )
                            }
                        } else {
                            button.icon?.let {
                                Icon(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .ifElse(button.scale != 1f, Modifier.scale(button.scale)),
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = button.tint
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ══════════════ 普通视频布局 ══════════════

            // ── 统计信息 ──
            if (upName.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(start = 32.dp, end = 32.dp, top = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier
                        .height(30.dp)
                        .onFocusChanged { if (it.isFocused) scheduleHideJob() },
                    onClick = onOpenUpSpace,
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(15.dp)),
                    scale = ButtonDefaults.scale(focusedScale = 1.05f),
                    contentPadding = PaddingValues(start = 0.dp, end = 10.dp, top = 0.dp, bottom = 0.dp),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(0.dp, Color.Transparent)
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                        )
                    )
                ) {
                    if (upAvatar.isNotEmpty()) {
                        AsyncImage(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            model = upAvatar,
                            contentDescription = upName,
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = upName,
                        color = PlayerColors.textPrimary,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                    text = "  ·  $statString",
                    color = PlayerColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ── 用户操作(点赞/收藏/投币/简介/列表) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, bottom = 4.dp)
                .offset(y = 8.dp)
                .focusProperties {
                    down = seekbarFocusRequester
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 原有用户操作内容（点赞/收藏/投币）
            userActionContent(
                Modifier,
                userActionFocusRequesters.value,
                { scheduleHideJob() },
                { pause ->
                    autoHideState.pauseAutoHide = pause
                    if (pause) cancelHideJob() else scheduleHideJob()
                }
            )

            // 三连按钮
            val tripleLikeFocus = userActionFocusRequesters.value["tripleLike"]
            Button(
                modifier = Modifier
                    .height(26.dp)
                    .onFocusChanged { if (it.isFocused) scheduleHideJob() }
                    .then(tripleLikeFocus?.let { Modifier.focusRequester(it) } ?: Modifier),
                onClick = onTripleLike,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    focusedContentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = ButtonDefaults.border(
                    border = Border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.Transparent
                        )
                    ),
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    )
                )
            ) {
                Icon(
                    modifier = Modifier.scale(0.8f),
                    imageVector = Icons.Outlined.Star,
                    contentDescription = "三连"
                )
                Spacer(Modifier.width(4.dp))
                Text("三连", style = MaterialTheme.typography.bodySmall)
            }

            // 简介按钮
            val descFocus = userActionFocusRequesters.value["description"]
            Button(
                modifier = Modifier
                    .height(26.dp)
                    .onFocusChanged { if (it.isFocused) scheduleHideJob() }
                    .then(descFocus?.let { Modifier.focusRequester(it) } ?: Modifier),
                onClick = onShowDescription,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    focusedContentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = ButtonDefaults.border(
                    border = Border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.Transparent
                        )
                    ),
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    )
                )
            ) {
                Icon(
                    modifier = Modifier.scale(0.8f),
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "简介"
                )
                Spacer(Modifier.width(4.dp))
                Text("简介", style = MaterialTheme.typography.bodySmall)
            }

            // 列表按钮
            if (!isLive) {
                val playlistFocus = userActionFocusRequesters.value["playlist"]
                Button(
                    modifier = Modifier
                        .height(26.dp)
                        .onFocusChanged { if (it.isFocused) scheduleHideJob() }
                        .then(playlistFocus?.let { Modifier.focusRequester(it) } ?: Modifier),
                    onClick = onOpenPlayList,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color.Transparent
                            )
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.45f)
                            )
                        )
                    )
                ) {
                    Icon(
                        modifier = Modifier.scale(0.9f),
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        contentDescription = "列表"
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("列表", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Row 1: 进度条 ──
        VideoSeekBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, end = 30.dp, bottom = 2.dp)
                .focusRequester(seekbarFocusRequester)
                .onFocusChanged {
                    scheduleHideJob()
                    seekbarHasFocus = it.isFocused
                }
                .focusProperties {
                    up = userActionFocusRequesters.value["like"] ?: FocusRequester()
                    down = focusRequesters[buttons.firstOrNull()?.id ?: "settings"]
                        ?: FocusRequester()
                }
                .focusable()
                .onPreviewKeyEvent {
                    if (seekbarHasFocus && it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.DirectionLeft -> onSeekBack()
                            Key.DirectionRight -> onSeekForward()
                            Key.Enter -> if (isPlaying) onPause() else onPlay()
                            Key.DirectionCenter -> if (isPlaying) onPause() else onPlay()
                        }
                    }
                    false
                },
            duration = seekData.duration,
            position = seekData.position,
            bufferedPercentage = seekData.bufferedPercentage,
            moveState = SeekMoveState.Idle,
            idleIcon = idleIcon,
            movingIcon = movingIcon,
            isFocused = seekbarHasFocus
        )

        // ── Row 2: 功能按钮 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, bottom = 10.dp)
                .focusProperties { up = seekbarFocusRequester }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (!fromSeason && !isLive && event.key == Key.DirectionDown) {
                            onOpenRelatedVideo()
                        }
                    }
                    false
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            buttons.forEach { button ->
                Button(
                    modifier = Modifier
                        .height(48.dp)
                        .width(if (button.text != null) (button.width ?: 48).dp else 48.dp)
                        .focusRequester(focusRequesters[button.id] ?: FocusRequester()),
                    onClick = button.onClick,
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                    scale = ButtonDefaults.scale(focusedScale = 1.1f),
                    contentPadding = PaddingValues(4.dp),
                    colors = ButtonDefaults.colors(
                        containerColor = if (button.selected) PlayerColors.buttonSelected else PlayerColors.buttonDefault,
                        focusedContainerColor = PlayerColors.buttonFocused
                    ),
                    border = ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = when {
                                    button.selected -> button.tint.copy(alpha = 0.85f)
                                    button.alwaysShowBorder -> PlayerColors.buttonAlwaysShowBorder
                                    else -> PlayerColors.buttonDefault
                                }
                            )
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = PlayerColors.buttonFocusedBorder
                            )
                        )
                    )
                ) {
                    if (button.text != null) {
                        Text(
                            text = button.text,
                            style = MaterialTheme.typography.titleMedium,
                            color = button.tint,
                            fontWeight = button.fontWeight,
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center)
                                .ifElse(
                                    button.scale != 1f,
                                    Modifier.scale(button.scale)
                                )
                        )
                    } else if (button.painterId != null) {
                        Icon(
                            modifier = Modifier
                                .ifElse(button.scale != 1f, Modifier.scale(button.scale)),
                            painter = painterResource(id = button.painterId),
                            contentDescription = null,
                            tint = button.tint
                        )
                    } else {
                        button.icon?.let {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .ifElse(button.scale != 1f, Modifier.scale(button.scale)),
                                imageVector = it,
                                contentDescription = null,
                                tint = button.tint
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "${seekData.position.formatHourMinSec()} / ${seekData.duration.formatHourMinSec()}",
                color = PlayerColors.textPrimary
            )
        }
        } // end else (non-live)
    }

    // ── 对话框 ──

    if (showSpeedDialog) {
        SpeedDialog(
            onHideDialog = { showSpeedDialog = false },
            speed = speed,
            onSpeedChange = {
                speed = it
                onPlaySpeedChange(it)
            }
        )
    }

    if (showRotationDialog) {
        RotationDialog(
            onHideDialog = { showRotationDialog = false },
            rotation = rotation,
            onRotationChange = onRotationChange
        )
    }

    if (showSubtitleDialog) {
        val currentSubtitle =
            availableSubtitleTracks.firstOrNull { it.id == currentSubtitleId }
        if (currentSubtitle != null) {
            SubtitleDialog(
                onHideDialog = { showSubtitleDialog = false },
                subtitle = currentSubtitle,
                availableSubtitleTracks = availableSubtitleTracks,
                onSubtitleChange = { subtitle -> onSubtitleChange(subtitle.id) }
            )
        }
    }
}
