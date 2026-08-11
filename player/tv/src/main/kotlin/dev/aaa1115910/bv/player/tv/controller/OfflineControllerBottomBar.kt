package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import coil.compose.AsyncImage
import dev.aaa1115910.bv.player.entity.PlayerBottomControlPanelButtonIds
import dev.aaa1115910.bv.player.entity.PlayerBottomControlPanelConfig
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekState
import dev.aaa1115910.bv.player.entity.VideoPlayerStateData
import dev.aaa1115910.bv.player.seekbar.SeekMoveState
import dev.aaa1115910.bv.player.shared.R
import dev.aaa1115910.bv.player.tv.VideoSeekBar
import dev.aaa1115910.bv.player.tv.component.PlayerAnimations
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
import dev.aaa1115910.bv.util.formatHourMinSec
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

private const val OfflinePanelAutoHideMs = 5_000L
private const val OfflinePanelBaseDensity = 2f
private val OfflinePanelTopPadding = 16.dp
private val OfflinePanelInfoTopPadding = 4.dp
private val OfflinePanelRowGap = 6.dp
private val OfflinePanelBottomPadding = 2.dp
private val OfflinePanelSeekTopPadding = 4.dp
private val OfflinePanelIconSize = 28.dp
private val OfflinePanelFunctionButtonSize = 36.dp
private val OfflinePanelActionButtonHeight = 26.dp
private val OfflinePanelMinActionButtonHeight = 22.dp
private val OfflinePanelAvatarSize = 22.dp
private val OfflinePanelTitleFontSize = 16.sp
private val OfflinePanelTitleLineHeight = 20.sp
private val OfflinePanelInfoFontSize = 12.sp
private val OfflinePanelInfoLineHeight = 18.sp

private data class OfflineFunctionButton(
    val id: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val painterId: Int? = null,
    val text: String? = null,
    val scale: Float = 1f,
    val selected: Boolean = false,
    val alwaysShowBorder: Boolean = false,
    val width: Int? = null,
)

@Composable
fun BoxScope.OfflineControllerBottomBar(
    show: Boolean,
    title: String,
    partTitle: String,
    upName: String,
    upAvatar: String,
    danmaku: Int,
    currentCid: Long,
    videoList: List<VideoListItem>,
    seekState: VideoPlayerSeekState,
    playerState: VideoPlayerStateData,
    playSpeed: Float,
    showDanmaku: Boolean,
    isLoop: Boolean,
    bottomProgressBarColor: Color,
    bottomControlPanelConfig: PlayerBottomControlPanelConfig,
    onHide: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onRefreshVideo: () -> Unit,
    onOpenPlayList: () -> Unit,
    onLoadPrevVideo: () -> Unit,
    onLoadNextVideo: (Boolean) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onOpenDanmaku: () -> Unit,
    onHideDanmaku: () -> Unit,
    onLoopPlayModeChange: (Boolean) -> Unit,
) {
    val playableItems = videoList.filterIsInstance<VideoListItemData>()
    val currentIndex = playableItems.indexOfFirst { it.cid == currentCid }
    val panelConfig = bottomControlPanelConfig.normalized()
    val seekFocusRequester = remember { FocusRequester() }
    val playlistFocusRequester = remember { FocusRequester() }
    var seekBarFocused by remember { mutableStateOf(false) }
    var speed by remember(playSpeed) { mutableFloatStateOf(playSpeed) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var interactionVersion by remember { mutableIntStateOf(0) }
    var danmakuVisible by remember { mutableStateOf(showDanmaku) }

    LaunchedEffect(showDanmaku) {
        danmakuVisible = showDanmaku
    }

    fun markInteraction() {
        interactionVersion++
    }

    val buttons = listOfNotNull(
        OfflineFunctionButton(
            id = PlayerBottomControlPanelButtonIds.PrevVideo,
            painterId = R.drawable.prev_play_fill,
            scale = 0.7f,
            onClick = onLoadPrevVideo
        ).takeIf { currentIndex > 0 },
        OfflineFunctionButton(
            id = PlayerBottomControlPanelButtonIds.NextVideo,
            painterId = R.drawable.next_play_fill,
            scale = 0.7f,
            onClick = { onLoadNextVideo(true) }
        ).takeIf { currentIndex in 0 until playableItems.lastIndex },
        OfflineFunctionButton(
            id = PlayerBottomControlPanelButtonIds.Danmaku,
            painterId = if (danmakuVisible) R.drawable.ic_danmaku_on else R.drawable.ic_danmaku_hide,
            selected = danmakuVisible,
            onClick = {
                danmakuVisible = !danmakuVisible
                if (danmakuVisible) onOpenDanmaku() else onHideDanmaku()
            }
        ),
        OfflineFunctionButton(
            id = PlayerBottomControlPanelButtonIds.Loop,
            icon = if (isLoop) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            selected = isLoop,
            onClick = { onLoopPlayModeChange(!isLoop) }
        ),
        OfflineFunctionButton(
            id = PlayerBottomControlPanelButtonIds.Speed,
            text = formatOfflineSpeed(speed),
            width = 36,
            selected = speed != 1f,
            alwaysShowBorder = true,
            onClick = { showSpeedDialog = true }
        ),
        OfflineFunctionButton(
            id = PlayerBottomControlPanelButtonIds.Refresh,
            icon = Icons.Rounded.Refresh,
            onClick = onRefreshVideo
        )
    )
    val orderedButtons = panelConfig
        .orderedFunctionButtons(buttons.map { it.id })
        .mapNotNull { id -> buttons.firstOrNull { it.id == id } }
    val functionFocusRequesters = remember(orderedButtons.map { it.id }) {
        orderedButtons.associate { it.id to FocusRequester() }
    }
    val firstFunctionFocusRequester = orderedButtons.firstOrNull()?.id
        ?.let(functionFocusRequesters::get)
        ?: FocusRequester()

    LaunchedEffect(show) {
        if (show) {
            withFrameNanos { }
            runCatching { seekFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(show, interactionVersion, showSpeedDialog) {
        if (show && !showSpeedDialog) {
            delay(OfflinePanelAutoHideMs)
            onHide()
        }
    }

    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        visible = show,
        enter = PlayerAnimations.controllerEnter,
        exit = PlayerAnimations.controllerExit,
        label = "OfflineControllerBottomBar"
    ) {
        val currentDensity = LocalDensity.current
        val panelScale = (currentDensity.density / OfflinePanelBaseDensity).coerceAtLeast(0.01f)
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = OfflinePanelBaseDensity,
                fontScale = currentDensity.fontScale
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(PlayerColors.controllerScrimBottom))
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) markInteraction()
                        false
                    },
                verticalArrangement = Arrangement.Bottom
            ) {
                Spacer(modifier = Modifier.height(OfflinePanelTopPadding.scaledBy(panelScale)))
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = formatControllerTitle(title, partTitle),
                    color = PlayerColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = OfflinePanelTitleFontSize.scaledBy(panelScale * panelConfig.titleScale),
                        lineHeight = OfflinePanelTitleLineHeight.scaledBy(panelScale * panelConfig.titleScale)
                    )
                )

                if (upName.isNotBlank()) {
                    OfflineLocalUploaderInfo(
                        modifier = Modifier.padding(
                            start = 32.dp,
                            end = 32.dp,
                            top = OfflinePanelInfoTopPadding.scaledBy(panelScale)
                        ),
                        upName = upName,
                        upAvatar = upAvatar,
                        danmaku = danmaku,
                        scale = panelScale * panelConfig.infoScale
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp)
                        .focusProperties { down = seekFocusRequester },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OfflinePlaylistButton(
                        modifier = Modifier
                            .height(
                                OfflinePanelActionButtonHeight.scaledByAtLeast(
                                    panelScale * panelConfig.actionRowScale,
                                    OfflinePanelMinActionButtonHeight
                                )
                            )
                            .focusRequester(playlistFocusRequester)
                            .onFocusChanged { if (it.isFocused) markInteraction() },
                        contentPadding = PaddingValues(
                            horizontal = 8.dp.scaledBy(panelConfig.actionRowScale),
                            vertical = 0.dp
                        ),
                        onClick = {
                            markInteraction()
                            onOpenPlayList()
                        }
                    )
                }

                VideoSeekBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 30.dp,
                            end = 30.dp,
                            top = OfflinePanelSeekTopPadding.scaledBy(
                                panelScale * panelConfig.seekBarScale
                            )
                        )
                        .focusRequester(seekFocusRequester)
                        .onFocusChanged {
                            seekBarFocused = it.isFocused
                            if (it.isFocused) markInteraction()
                        }
                        .focusProperties {
                            up = playlistFocusRequester
                            down = firstFunctionFocusRequester
                        }
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (!seekBarFocused || event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.key) {
                                Key.DirectionLeft -> onSeekBack()
                                Key.DirectionRight -> onSeekForward()
                                Key.Enter, Key.DirectionCenter -> {
                                    if (playerState.isPlaying) onPause() else onPlay()
                                }
                                else -> return@onPreviewKeyEvent false
                            }
                            markInteraction()
                            true
                        },
                    duration = seekState.duration,
                    position = seekState.position,
                    bufferedPercentage = seekState.bufferedPercentage,
                    playedTrackBrush = SolidColor(bottomProgressBarColor),
                    moveState = SeekMoveState.Idle,
                    isFocused = seekBarFocused,
                    showThumb = false,
                    trackBottomMargin = 4.dp.scaledBy(panelScale * panelConfig.seekBarScale),
                    trackHeightScale = panelConfig.seekBarScale
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 32.dp,
                            end = 32.dp,
                            bottom = OfflinePanelBottomPadding.scaledBy(panelScale)
                        )
                        .focusProperties { up = seekFocusRequester },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    orderedButtons.forEach { button ->
                        OfflineFunctionButton(
                            modifier = Modifier
                                .height(
                                    OfflinePanelFunctionButtonSize.scaledBy(
                                        panelScale * panelConfig.functionRowScale
                                    )
                                )
                                .width(
                                    (button.width ?: OfflinePanelFunctionButtonSize.value.toInt())
                                        .dp
                                        .scaledBy(panelScale * panelConfig.functionRowScale)
                                )
                                .focusRequester(
                                    functionFocusRequesters[button.id] ?: FocusRequester()
                                )
                                .onFocusChanged { if (it.isFocused) markInteraction() },
                            button = button,
                            onClick = {
                                markInteraction()
                                button.onClick()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        modifier = Modifier.padding(top = 8.dp.scaledBy(panelScale)),
                        text = "${seekState.position.formatHourMinSec()} / ${seekState.duration.formatHourMinSec()}",
                        color = PlayerColors.textPrimary
                    )
                }
            }
        }
    }

    if (showSpeedDialog) {
        SpeedDialog(
            onHideDialog = {
                showSpeedDialog = false
                markInteraction()
            },
            speed = speed,
            onSpeedChange = {
                speed = it
                onPlaySpeedChange(it)
            }
        )
    }
}

@Composable
private fun OfflineLocalUploaderInfo(
    modifier: Modifier,
    upName: String,
    upAvatar: String,
    danmaku: Int,
    scale: Float,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (upAvatar.isNotBlank()) {
            AsyncImage(
                modifier = Modifier
                    .size(OfflinePanelAvatarSize.scaledBy(scale))
                    .clip(CircleShape),
                model = upAvatar,
                contentDescription = upName,
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = upName,
            color = PlayerColors.textPrimary,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = OfflinePanelInfoFontSize.scaledBy(scale),
                lineHeight = OfflinePanelInfoLineHeight.scaledBy(scale)
            )
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = if (danmaku > 0) "  ·  $danmaku 弹幕  ·  离线播放" else "  ·  离线播放",
            color = PlayerColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = OfflinePanelInfoFontSize.scaledBy(scale),
                lineHeight = OfflinePanelInfoLineHeight.scaledBy(scale)
            )
        )
    }
}

@Composable
private fun OfflinePlaylistButton(
    modifier: Modifier,
    contentPadding: PaddingValues,
    onClick: () -> Unit,
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
            border = Border(BorderStroke(1.dp, Color.Transparent)),
            focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)))
        )
    ) {
        Icon(
            modifier = Modifier.scale(0.9f),
            imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("列表", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun OfflineFunctionButton(
    modifier: Modifier,
    button: OfflineFunctionButton,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
        scale = ButtonDefaults.scale(focusedScale = 1.1f),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.colors(
            containerColor = if (button.selected) {
                PlayerColors.buttonSelected
            } else {
                PlayerColors.buttonDefault
            },
            focusedContainerColor = PlayerColors.buttonFocused
        ),
        border = ButtonDefaults.border(
            border = Border(
                BorderStroke(
                    width = 1.dp,
                    color = when {
                        button.selected -> Color.White.copy(alpha = 0.8f)
                        button.alwaysShowBorder -> PlayerColors.buttonAlwaysShowBorder
                        else -> PlayerColors.buttonDefault
                    }
                )
            ),
            focusedBorder = Border(BorderStroke(1.5.dp, PlayerColors.buttonFocusedBorder))
        )
    ) {
        when {
            button.text != null -> Text(
                text = button.text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
            button.painterId != null -> Icon(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .size(OfflinePanelIconSize)
                    .scale(button.scale),
                painter = painterResource(button.painterId),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f)
            )
            button.icon != null -> Icon(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .size(OfflinePanelIconSize)
                    .scale(button.scale),
                imageVector = button.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

private fun Dp.scaledBy(scale: Float): Dp = (value * scale).dp

private fun TextUnit.scaledBy(scale: Float): TextUnit = (value * scale).sp

private fun Dp.scaledByAtLeast(scale: Float, min: Dp): Dp =
    max(value * scale, min.value).dp

private fun formatOfflineSpeed(speed: Float): String =
    "${(speed * 100).roundToInt() / 100f}x"
