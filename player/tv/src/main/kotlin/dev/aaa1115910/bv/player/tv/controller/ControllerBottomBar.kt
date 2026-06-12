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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.PlayerBottomControlPanelButtonIds
import dev.aaa1115910.bv.player.entity.PlayerBottomControlPanelConfig
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
import kotlin.math.max
import kotlin.math.roundToInt

private fun formatSpeed(speed: Float): String {
    return "${(speed * 100).roundToInt() / 100f}x"
}

private fun controllerSeekBarPlayedTrackBrush(progressColor: Color): Brush {
    return SolidColor(progressColor)
}

internal const val ControllerPanelBaseDensity = 2f
private val ControllerButtonIconSize = 28.dp
private val ControllerPanelTopPadding = 16.dp
private val ControllerPanelInfoTopPadding = 4.dp
private val ControllerPanelRowGap = 6.dp
private val ControllerPanelBottomPadding = 2.dp
private val ControllerActionRowOffset = 0.dp
private val ControllerActionRowBottomPadding = 0.dp
private val ControllerSeekBarTopPadding = 4.dp
private val ControllerSeekBarBottomPadding = 0.dp
private val ControllerSeekBarTrackBottomMargin = 4.dp
private val ControllerTitleFontSize = 16.sp
private val ControllerTitleLineHeight = 20.sp
private val ControllerInfoFontSize = 12.sp
private val ControllerInfoLineHeight = 18.sp
private val ControllerUpAvatarSize = 22.dp
private val ControllerInfoButtonHeight = 30.dp
private val ControllerActionButtonHeight = 26.dp
private val ControllerFunctionButtonSize = 36.dp
private val ControllerMinInfoButtonHeight = 24.dp
private val ControllerMinActionButtonHeight = 22.dp

private class ControllerBottomBarAutoHideState {
    var hideVideoInfoJob: Job? = null
    var pauseAutoHide: Boolean = false
}

private fun Modifier.controllerButtonIconSize(
    scale: Float = 1f,
    size: Dp = ControllerButtonIconSize
): Modifier = this
    .size(size)
    .ifElse(scale != 1f, Modifier.scale(scale))

private fun Modifier.centeredControllerButtonIconSize(
    scale: Float = 1f,
    size: Dp = ControllerButtonIconSize
): Modifier = this
    .fillMaxSize()
    .wrapContentSize(Alignment.Center)
    .size(size)
    .ifElse(scale != 1f, Modifier.scale(scale))

private fun Dp.scaledBy(scale: Float): Dp = (value * scale).dp

private fun TextUnit.scaledBy(scale: Float): TextUnit = (value * scale).sp

private fun Dp.scaledByAtLeast(scale: Float, min: Dp): Dp =
    max(value * scale, min.value).dp

internal fun formatControllerTitle(title: String, partTitle: String): String =
    "${if (title.contains(partTitle)) "" else "$partTitle ｜ "}$title"

@Composable
private fun ControllerActionTextButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    iconScale: Float = 0.9f,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        contentPadding = contentPadding,
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
            modifier = Modifier.scale(iconScale),
            imageVector = icon,
            contentDescription = text
        )
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ControllerBottomBar(
    show: Boolean,
    onHideInfo: () -> Unit,
    modifier: Modifier = Modifier,
    playSpeed: Float = 1f,
    bottomProgressBarColor: Color = PlayerColors.bottomProgressBar,
    bottomControlPanelConfig: PlayerBottomControlPanelConfig = PlayerBottomControlPanelConfig.Default,
    rotation: VideoRotation,
    title: String,
    partTitle: String,
    showTitle: Boolean = true,
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
    onLiveLineChange: (Int) -> Unit = {},
    onOpenPlayList: () -> Unit,
    onOpenRelatedVideo: () -> Unit,
    onOpenSetting: () -> Unit,
    onLoopPlayModeChange: (Boolean) -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
    fromSeason: Boolean = false,
    showRelatedButton: Boolean = !fromSeason,
    isFollowingUp: Boolean = false,
    userActionContent: @Composable (
        modifier: Modifier,
        focusMap: Map<String, FocusRequester>,
        onFocus: (String) -> Unit,
        onPauseAutoHide: (Boolean) -> Unit,
    ) -> Unit = { _, _, _, _ -> },
    userActionButtonIds: Set<String> = emptySet(),
    userActionButtonContent: @Composable (
        buttonId: String,
        modifier: Modifier,
        contentPadding: PaddingValues,
        onPauseAutoHide: (Boolean) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    availableSubtitleTracks: List<Subtitle> = emptyList(),
    currentSubtitleId: Long,
    onSubtitleChange: (Long) -> Unit,
    onLoadNextVideo: (Boolean) -> Unit,
    onLoadPrevVideo: () -> Unit = {},
    commentPanelVisible: Boolean = false,
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
    var showLiveLineDialog by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(playSpeed) }
    val danmakuIconId =
        if (showDanmaku) R.drawable.ic_danmaku_on else R.drawable.ic_danmaku_hide
    val subtitleIconId =
        if (currentSubtitleId > 0) R.drawable.ic_subtitle_on else R.drawable.ic_subtitle_off
    val hasSubtitles = availableSubtitleTracks.isNotEmpty()
    val availableLiveLines = videoPlayerConfigData.availableLiveLines
    val currentLiveLineButtonText = availableLiveLines
        .firstOrNull { it.index == videoPlayerConfigData.currentLiveLineIndex }
        ?.let { "线路 ${it.index + 1}" }
        ?: "线路"
    val seekBarPlayedTrackBrush = remember(bottomProgressBarColor) {
        controllerSeekBarPlayedTrackBrush(bottomProgressBarColor)
    }

    // ── 核心按钮 ──
    val buttons = remember(
        fromSeason, showDanmaku, isPlaying, isLoop,
        showPrevVideoBtn, showNextVideoBtn, isLive,
        speed, isAudioOnly, rotation, hasSubtitles, currentSubtitleId,
        availableSubtitleTracks,
        commentPanelVisible, videoPlayerConfigData.supportManualVideoRotation
    ) {
        listOf(
            ControlButton(
                id = "comment",
                icon = Icons.AutoMirrored.Rounded.Chat,
                onClick = onShowComment,
                selected = commentPanelVisible
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
                onClick = {
                    val targetSubtitle = if (currentSubtitleId != -1L) {
                        availableSubtitleTracks.firstOrNull { it.id == -1L }
                    } else {
                        availableSubtitleTracks.preferredSubtitleForQuickToggle()
                    }
                    targetSubtitle?.let { onSubtitleChange(it.id) }
                },
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
                width = 36,
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
                visible = !isLive && videoPlayerConfigData.supportManualVideoRotation,
                selected = rotation != VideoRotation.Original
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
    val liveButtons = remember(
        showDanmaku,
        isLive,
        isFollowingUp,
        liveIncognitoMode,
        availableLiveLines,
        currentLiveLineButtonText
    ) {
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
            if (availableLiveLines.isNotEmpty()) {
                add(ControlButton(
                    id = "liveLine",
                    text = currentLiveLineButtonText,
                    width = 80,
                    onClick = { showLiveLineDialog = true },
                    alwaysShowBorder = true
                ))
            }
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
                "playlist" to FocusRequester(),
                "related" to FocusRequester()
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
            !showLiveLineDialog &&
            !autoHideState.pauseAutoHide
        ) {
            autoHideState.hideVideoInfoJob = scope.launch {
                delay(5000)
                withContext(Dispatchers.Main) { onHideInfo() }
            }
        }
    }

    LaunchedEffect(show, showSpeedDialog, showRotationDialog, showLiveLineDialog) {
        scheduleHideJob()
    }
    DisposableEffect(Unit) {
        onDispose { cancelHideJob() }
    }

    val currentDensity = LocalDensity.current
    val controllerPanelScale =
        (currentDensity.density / ControllerPanelBaseDensity).coerceAtLeast(0.01f)
    val panelConfig = bottomControlPanelConfig.normalized()
    val effectiveTitleScale = if (isLive) 1f else panelConfig.titleScale
    val effectiveInfoScale = if (isLive) 1f else panelConfig.infoScale
    val actionButtonIds = remember(
        panelConfig.actionButtonOrder,
        userActionButtonIds,
        showRelatedButton
    ) {
        panelConfig.orderedActionButtons(
            buildSet {
                addAll(userActionButtonIds)
                add(PlayerBottomControlPanelButtonIds.TripleLike)
                add(PlayerBottomControlPanelButtonIds.Description)
                add(PlayerBottomControlPanelButtonIds.Playlist)
                if (showRelatedButton) add(PlayerBottomControlPanelButtonIds.Related)
            }
        )
    }
    val orderedButtons = remember(buttons, panelConfig.functionButtonOrder) {
        val buttonsById = buttons.associateBy { it.id }
        panelConfig
            .orderedFunctionButtons(buttonsById.keys)
            .mapNotNull { buttonsById[it] }
    }
    val firstActionFocusRequester =
        actionButtonIds.firstNotNullOfOrNull { userActionFocusRequesters.value[it] } ?: FocusRequester()

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = ControllerPanelBaseDensity,
            fontScale = currentDensity.fontScale
        )
    ) {
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
        if (showTitle) {
            Spacer(modifier = Modifier.height(ControllerPanelTopPadding.scaledBy(controllerPanelScale)))

            // ── 标题 ──
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = formatControllerTitle(title, partTitle),
                color = PlayerColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = ControllerTitleFontSize.scaledBy(controllerPanelScale * effectiveTitleScale),
                    lineHeight = ControllerTitleLineHeight.scaledBy(controllerPanelScale * effectiveTitleScale)
                ),
            )
        }

        if (isLive) {
            // ══════════════ 直播专用布局 ══════════════

            // ── UP主信息 + 关注按钮 ──
            if (upName.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(
                            start = 32.dp,
                            end = 32.dp,
                            top = ControllerPanelInfoTopPadding.scaledBy(controllerPanelScale)
                        )
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // UP主按钮
                    Button(
                        modifier = Modifier
                            .height(
                                ControllerInfoButtonHeight.scaledByAtLeast(
                                    controllerPanelScale * effectiveInfoScale,
                                    ControllerMinInfoButtonHeight
                                )
                            )
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
                                modifier = Modifier
                                    .size(ControllerUpAvatarSize.scaledBy(controllerPanelScale))
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
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = ControllerInfoFontSize.scaledBy(controllerPanelScale),
                                lineHeight = ControllerInfoLineHeight.scaledBy(controllerPanelScale)
                            )
                        )
                    }

                    // 关注/取消关注按钮
                    Button(
                        modifier = Modifier
                            .height(
                                ControllerInfoButtonHeight.scaledByAtLeast(
                                    controllerPanelScale * effectiveInfoScale,
                                    ControllerMinInfoButtonHeight
                                )
                            )
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
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = ControllerInfoFontSize.scaledBy(controllerPanelScale),
                                lineHeight = ControllerInfoLineHeight.scaledBy(controllerPanelScale)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(ControllerPanelRowGap.scaledBy(controllerPanelScale)))

            // ── 直播功能按钮行 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 32.dp,
                        end = 32.dp,
                        bottom = ControllerPanelBottomPadding.scaledBy(controllerPanelScale)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                liveButtons.forEach { button ->
                    Button(
                        modifier = Modifier
                            .height(ControllerFunctionButtonSize.scaledBy(controllerPanelScale))
                            .width(
                                (button.width ?: ControllerFunctionButtonSize.value.toInt())
                                    .dp
                                    .scaledBy(controllerPanelScale)
                            )
                            .focusRequester(liveFocusRequesters[button.id] ?: FocusRequester())
                            .onFocusChanged { if (it.isFocused) scheduleHideJob() },
                        onClick = button.onClick,
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        scale = ButtonDefaults.scale(focusedScale = 1.1f),
                        contentPadding = PaddingValues(0.dp),
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
                                        .controllerButtonIconSize(button.scale),
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
                        } else if (button.text != null) {
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
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .controllerButtonIconSize(button.scale),
                                    painter = painterResource(id = button.painterId),
                                    contentDescription = null,
                                    tint = button.tint
                                )
                            }
                        } else {
                            button.icon?.let {
                                Icon(
                                    modifier = Modifier
                                        .centeredControllerButtonIconSize(button.scale),
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
                    .padding(
                        start = 32.dp,
                        end = 32.dp,
                        top = ControllerPanelInfoTopPadding.scaledBy(controllerPanelScale)
                    )
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier
                        .height(
                            ControllerInfoButtonHeight.scaledByAtLeast(
                                controllerPanelScale * effectiveInfoScale,
                                ControllerMinInfoButtonHeight
                            )
                        )
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
                                .size(ControllerUpAvatarSize.scaledBy(controllerPanelScale * effectiveInfoScale))
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = ControllerInfoFontSize.scaledBy(controllerPanelScale * effectiveInfoScale),
                            lineHeight = ControllerInfoLineHeight.scaledBy(controllerPanelScale * effectiveInfoScale)
                        )
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
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = ControllerInfoFontSize.scaledBy(controllerPanelScale * effectiveInfoScale),
                        lineHeight = ControllerInfoLineHeight.scaledBy(controllerPanelScale * effectiveInfoScale)
                    )
                )
            }
        }

        // ── 用户操作(点赞/收藏/投币/简介/列表/推荐) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, bottom = ControllerActionRowBottomPadding.scaledBy(controllerPanelScale))
                .offset(y = ControllerActionRowOffset.scaledBy(controllerPanelScale))
                .focusProperties {
                    down = seekbarFocusRequester
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val actionScale = controllerPanelScale * panelConfig.actionRowScale
            val actionHeight = ControllerActionButtonHeight.scaledByAtLeast(
                actionScale,
                ControllerMinActionButtonHeight
            )
            val actionContentPadding = PaddingValues(
                horizontal = 8.dp.scaledBy(panelConfig.actionRowScale),
                vertical = 0.dp
            )
            val pauseAutoHide: (Boolean) -> Unit = { pause ->
                autoHideState.pauseAutoHide = pause
                if (pause) cancelHideJob() else scheduleHideJob()
            }
            fun actionModifier(id: String): Modifier {
                return Modifier
                    .height(actionHeight)
                    .onFocusChanged { if (it.isFocused) scheduleHideJob() }
                    .then(
                        userActionFocusRequesters.value[id]?.let { Modifier.focusRequester(it) }
                            ?: Modifier
                    )
            }

            if (userActionButtonIds.isEmpty()) {
                userActionContent(
                    Modifier,
                    userActionFocusRequesters.value,
                    { scheduleHideJob() },
                    pauseAutoHide
                )
            }

            actionButtonIds.forEach { buttonId ->
                when (buttonId) {
                    PlayerBottomControlPanelButtonIds.Like,
                    PlayerBottomControlPanelButtonIds.Favorite,
                    PlayerBottomControlPanelButtonIds.Coin -> {
                        userActionButtonContent(
                            buttonId,
                            actionModifier(buttonId),
                            actionContentPadding,
                            pauseAutoHide
                        )
                    }

                    PlayerBottomControlPanelButtonIds.TripleLike -> {
                        ControllerActionTextButton(
                            modifier = actionModifier(buttonId),
                            icon = Icons.Outlined.Star,
                            text = "三连",
                            onClick = onTripleLike,
                            iconScale = 0.8f,
                            contentPadding = actionContentPadding
                        )
                    }

                    PlayerBottomControlPanelButtonIds.Description -> {
                        ControllerActionTextButton(
                            modifier = actionModifier(buttonId),
                            icon = Icons.Outlined.Info,
                            text = "简介",
                            onClick = onShowDescription,
                            iconScale = 0.8f,
                            contentPadding = actionContentPadding
                        )
                    }

                    PlayerBottomControlPanelButtonIds.Playlist -> {
                        ControllerActionTextButton(
                            modifier = actionModifier(buttonId),
                            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            text = "列表",
                            onClick = onOpenPlayList,
                            iconScale = 0.9f,
                            contentPadding = actionContentPadding
                        )
                    }

                    PlayerBottomControlPanelButtonIds.Related -> {
                        ControllerActionTextButton(
                            modifier = actionModifier(buttonId),
                            icon = Icons.Rounded.KeyboardDoubleArrowDown,
                            text = "推荐",
                            onClick = onOpenRelatedVideo,
                            iconScale = 0.9f,
                            contentPadding = actionContentPadding
                        )
                    }
                }
            }
        }

        // ── Row 1: 进度条 ──
        VideoSeekBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 30.dp,
                    end = 30.dp,
                    top = ControllerSeekBarTopPadding.scaledBy(controllerPanelScale * panelConfig.seekBarScale),
                    bottom = ControllerSeekBarBottomPadding.scaledBy(controllerPanelScale * panelConfig.seekBarScale)
                )
                .focusRequester(seekbarFocusRequester)
                .onFocusChanged {
                    scheduleHideJob()
                    seekbarHasFocus = it.isFocused
                }
                .focusProperties {
                    up = firstActionFocusRequester
                    down = focusRequesters[orderedButtons.firstOrNull()?.id ?: "settings"]
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
            isFocused = seekbarHasFocus,
            playedTrackBrush = seekBarPlayedTrackBrush,
            showThumb = false,
            trackBottomMargin = ControllerSeekBarTrackBottomMargin.scaledBy(controllerPanelScale * panelConfig.seekBarScale),
            trackHeightScale = panelConfig.seekBarScale
        )

        // ── Row 2: 功能按钮 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    bottom = ControllerPanelBottomPadding.scaledBy(controllerPanelScale)
                )
                .focusProperties { up = seekbarFocusRequester },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            orderedButtons.forEach { button ->
                Button(
                    modifier = Modifier
                        .height(ControllerFunctionButtonSize.scaledBy(controllerPanelScale * panelConfig.functionRowScale))
                        .width(
                            (button.width ?: ControllerFunctionButtonSize.value.toInt())
                                .dp
                                .scaledBy(controllerPanelScale * panelConfig.functionRowScale)
                        )
                        .focusRequester(focusRequesters[button.id] ?: FocusRequester()),
                    onClick = button.onClick,
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                    scale = ButtonDefaults.scale(focusedScale = 1.1f),
                    contentPadding = PaddingValues(0.dp),
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
                                .centeredControllerButtonIconSize(button.scale),
                            painter = painterResource(id = button.painterId),
                            contentDescription = null,
                            tint = button.tint
                        )
                    } else {
                        button.icon?.let {
                            Icon(
                                modifier = Modifier
                                    .centeredControllerButtonIconSize(button.scale),
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
                modifier = Modifier.padding(top = 8.dp.scaledBy(controllerPanelScale)),
                text = "${seekData.position.formatHourMinSec()} / ${seekData.duration.formatHourMinSec()}",
                color = PlayerColors.textPrimary
            )
        }
        } // end else (non-live)
        }
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

    if (showLiveLineDialog && availableLiveLines.isNotEmpty()) {
        LiveLineDialog(
            lines = availableLiveLines,
            currentLineIndex = videoPlayerConfigData.currentLiveLineIndex,
            onHideDialog = { showLiveLineDialog = false },
            onLineChange = onLiveLineChange
        )
    }

}
