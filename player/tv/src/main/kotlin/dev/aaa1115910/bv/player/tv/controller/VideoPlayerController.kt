package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDebugInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekState
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.PlaybackMediaMode
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoRotation
import dev.aaa1115910.bv.player.seekbar.SeekMoveState
import dev.aaa1115910.bv.player.shared.BuildConfig
import dev.aaa1115910.bv.player.shared.R
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VideoPlayerController(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
    playerSeekForwardStep: Int = 10,
    playerSeekBackwardStep: Int = 5,
    showBottomProgressBar: Boolean = false,

    showRelatedVideos: Boolean = false,
    onToggleRelatedVideos: (Boolean) -> Unit,
    registerShowInfoProvider: ((() -> Boolean) -> Unit) = {},

    //player events
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onGoTime: (time: Long) -> Unit,
    onBackToHistory: () -> Unit,
    onPlayNewVideo: (VideoListItem) -> Unit,

    onOpenUpSpace: () -> Unit,
    onRefreshVideo: () -> Unit,
    onOpenDanmaku: () -> Unit,
    onHideDanmaku: () -> Unit,
    onLoopPlayModeChange: (Boolean) -> Unit,
    userActionContent: @Composable (
        modifier: Modifier,
        focusMap: Map<String, FocusRequester>,
        onFocus: (String) -> Unit,
        onPauseAutoHide: (Boolean) -> Unit
    ) -> Unit,

    //menu events
    onResolutionChange: (Resolution) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onPlaybackMediaModeChange: (PlaybackMediaMode) -> Unit,
    onLiveQualityChange: (Int) -> Unit = {},
    onLiveCodecChange: (LiveCodec) -> Unit = {},
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onDanmakuFilterLevelChange: (Int) -> Unit = {},
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onLoadNextVideo: (Boolean) -> Unit,

    onRequestFocus: () -> Unit,
    onShowComment: () -> Unit = {},
    onTripleLike: () -> Unit = {},
    useTripleLikeOnLongPress: Boolean = false,

    // SponsorBlock 相关参数
    enableSponsorBlock: Boolean = false,
    sponsorSegments: List<dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment> = emptyList(),
    showSponsorBlockTip: Boolean = false,
    currentSponsorSegment: dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment? = null,
    onSkipSponsorSegment: () -> Unit = {},
    onDismissSponsorBlockTip: () -> Unit = {},

    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val videoPlayerSeekState = LocalVideoPlayerSeekState.current
    val videoPlayerStateData = LocalVideoPlayerStateData.current
    val videoPlayerVideoInfoData = LocalVideoPlayerVideoInfoData.current
    val videoPlayerDebugInfoData = LocalVideoPlayerDebugInfoData.current
    val logger = KotlinLogging.logger {}
    val scope = rememberCoroutineScope()

    var showListController by remember { mutableStateOf(false) }
    var showMenuController by remember { mutableStateOf(false) }
    var showSeekController by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val showClickableControllers by remember { derivedStateOf { showInfo || showListController || showMenuController } }

    var lastPressBack by remember { mutableLongStateOf(0L) }
    var lastPressDown by remember { mutableLongStateOf(0L) }
    var hasFocus by remember { mutableStateOf(false) }
    var justTriggeredLongPress by remember { mutableStateOf(false) }
    var longPressResetJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var goTime by remember { mutableLongStateOf(0L) }
    var seekChangeCount by remember { mutableIntStateOf(0) }
    var lastSeekChangeTime by remember { mutableLongStateOf(0L) }
    var moveState by remember { mutableStateOf(SeekMoveState.Idle) }

    // 使用协程Job来替代CountDownTimer以确保线程安全
    var hideVideoInfoJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var autoSeekConfirmJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var doublePressDownJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val openSeekController = {
        if (!showSeekController) goTime = videoPlayerSeekState.position
        showSeekController = true
        showInfo = false
    }

    val resetAutoSeekConfirmTimer = {
        autoSeekConfirmJob?.cancel()
        if (showSeekController) {
            autoSeekConfirmJob = scope.launch {
                delay(1000)
                if (showSeekController) {
                    onGoTime(goTime)
                    if (!videoPlayer.isPlaying) onPlay()
                    withContext(Dispatchers.Main) {
                        moveState = SeekMoveState.Idle
                        showSeekController = false
                    }
                }
            }
        }
    }

    val calCoefficient = {
        if (System.currentTimeMillis() - lastSeekChangeTime < 200) {
            seekChangeCount++
            seekChangeCount / 5
        } else {
            seekChangeCount = 0
            0
        }
    }

    val onTimeForward = {
        val baseTime = playerSeekForwardStep * 1000L // 转换为毫秒
        val targetTime = goTime + (baseTime + calCoefficient() * 5000)
        val duration = videoPlayerSeekState.duration
        goTime = if (targetTime > duration) duration else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        moveState = SeekMoveState.Forward
        resetAutoSeekConfirmTimer()
        logger.info { "onTimeForward: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }
    val onTimeBack = {
        val baseTime = playerSeekBackwardStep * 1000L // 转换为毫秒
        val targetTime = goTime - (baseTime + calCoefficient() * 5000)
        goTime = if (targetTime < 0) 0 else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        moveState = SeekMoveState.Backward
        resetAutoSeekConfirmTimer()
        logger.info { "onTimeBack: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }

    // 对外暴露 showInfo
    LaunchedEffect(Unit) { registerShowInfoProvider { showInfo } }

    Box(
        modifier = modifier
            .background(Color.Black)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable()
            //.ifElse(hasFocus, Modifier.border(2.dp, Color.Yellow))
            .onPreviewKeyEvent {

                if (showClickableControllers || showRelatedVideos) {
                    if (listOf(Key.Back, Key.Menu).contains(it.key)) {
                        if (it.type == KeyEventType.KeyUp) {
                            logger.fInfo { "[${it.key}] hide all controllers" }
                            scope.launch(Dispatchers.Main) {
                                showInfo = false
                                showMenuController = false
                                showListController = false
                                showSeekController = false
                                onToggleRelatedVideos(false)
                            }
                        }
                        onRequestFocus()
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                }

                if (showSeekController) {
                    if (listOf(
                            Key.Back,
                            Key.Menu,
                            Key.DirectionDown,
                            Key.DirectionUp
                        ).contains(it.key)
                    ) {
                        if (it.type != KeyEventType.KeyDown) {
                            scope.launch(Dispatchers.Main) {
                                showSeekController = false
                            }
                        }
                        onRequestFocus()
                        return@onPreviewKeyEvent true
                    }
                }

                when (it.key) {
                    Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                        if (showSponsorBlockTip && enableSponsorBlock) {
                            if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                            logger.fInfo { "[${it.key}] skip sponsor segment" }
                            onSkipSponsorSegment()
                            return@onPreviewKeyEvent true
                        }

                        @Suppress("KotlinConstantConditions")
                        if (
                            videoPlayerConfigData.enableStartPositionSwitch &&
                            !showClickableControllers &&
                            videoPlayerStateData.showBackToHistory
                        ) {
                            if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                            onBackToHistory()
                            return@onPreviewKeyEvent true
                        }

                        if (showSeekController) {
                            if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                            onGoTime(goTime)
                            if (!videoPlayer.isPlaying) onPlay()
                            scope.launch(Dispatchers.Main) {
                                moveState = SeekMoveState.Idle
                                showSeekController = false
                            }
                            return@onPreviewKeyEvent true
                        }

                        if (it.nativeKeyEvent.isLongPress) {
                            logger.fInfo { "[${it.key}] long press" }
                            justTriggeredLongPress = true
                            longPressResetJob?.cancel()
                            longPressResetJob = scope.launch(Dispatchers.Main) {
                                delay(1000)
                                justTriggeredLongPress = false
                            }
                            scope.launch(Dispatchers.Main) {
                                if (useTripleLikeOnLongPress) {
                                    onTripleLike()
                                } else {
                                    showMenuController = true
                                }
                            }
                            return@onPreviewKeyEvent true
                        }

                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true

                        if (justTriggeredLongPress) {
                            logger.fInfo { "[${it.key}] ignore key up after long press" }
                            return@onPreviewKeyEvent true
                        }

                        logger.fInfo { "[${it.key}] short press" }
                        if (videoPlayer.isPlaying)
                            onPause()
                        else if (videoPlayer.currentPosition >= videoPlayer.duration) {
                            goTime = 0
                            onGoTime(0)
                        } else
                            onPlay()
                        return@onPreviewKeyEvent false
                    }

                    // KEYCODE_CENTER_LONG
                    // 一切设备上长按 DirectionCenter 键会是这个按键事件
                    Key(763) -> {
                        justTriggeredLongPress = true
                        longPressResetJob?.cancel()
                        longPressResetJob = scope.launch(Dispatchers.Main) {
                            delay(500)
                            justTriggeredLongPress = false
                        }
                        scope.launch(Dispatchers.Main) {
                            if (useTripleLikeOnLongPress) {
                                onTripleLike()
                            } else {
                                showMenuController = true
                            }
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionUp -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        if (videoPlayerConfigData.isLive) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        scope.launch(Dispatchers.Main) {
                            showListController = true
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionDown -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        if (videoPlayerConfigData.isLive) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }

                        // 检查是否为连按两次（间隔小于300ms且上次按键时间不为0）
                        val currentTime = System.currentTimeMillis()
                        val isDoublePress = lastPressDown != 0L && currentTime - lastPressDown < 300
                        lastPressDown = currentTime

                        doublePressDownJob?.cancel()
                        doublePressDownJob = scope.launch(Dispatchers.Main) {
                            delay(300)
                            lastPressDown = 0L // 重置时间，避免第三次按下时误判
                            if ((isDoublePress || showInfo) && !showRelatedVideos) {
                                showInfo = false
                                onToggleRelatedVideos(true)
                            } else if(!showInfo && !showRelatedVideos) {
                                showInfo = true
                            }
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.Menu -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        showMenuController = !showMenuController
                        if(!showMenuController) onRequestFocus()
                        return@onPreviewKeyEvent true
                    }

                    Key.Back -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }

                        // 有任何控制器显示中，先隐藏控制器
                        if (showSeekController || showListController || showMenuController || showInfo || showRelatedVideos) {
                            logger.fInfo { "隐藏控制器" }
                            scope.launch(Dispatchers.Main) {
                                showSeekController = false
                                showListController = false
                                showMenuController = false
                                showInfo = false
                                onToggleRelatedVideos(false)
                                hideVideoInfoJob?.cancel()
                            }
                            return@onPreviewKeyEvent true
                        }

                        if (!videoPlayer.isPlaying) {
                            logger.fInfo { "Exiting video player" }
                            onExit()
                            return@onPreviewKeyEvent true
                        }

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastPressBack < 1000 * 3) {
                            logger.fInfo { "Exiting video player" }
                            onExit()
                        } else {
                            lastPressBack = currentTime
                            R.string.video_player_press_back_again_to_exit.toast(context)
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaPlayPause -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (videoPlayer.isPlaying) onPause() else onPlay()
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaPlay -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (!videoPlayer.isPlaying) onPlay()
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaPause -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (videoPlayer.isPlaying) onPause()
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaFastForward -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        openSeekController()
                        onTimeForward()
                    }

                    Key.MediaRewind -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        openSeekController()
                        onTimeBack()
                    }

                    Key.DirectionLeft -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        openSeekController()
                        onTimeBack()
                    }

                    Key.DirectionRight -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        openSeekController()
                        onTimeForward()
                    }
                }

                false
            }
    ) {
        content()
        if (videoPlayerConfigData.currentPlaybackMediaMode == PlaybackMediaMode.AudioOnly && !showClickableControllers && !showSeekController) {
            AudioOnlyModeTip(
                title = videoPlayerVideoInfoData.title,
                partTitle = videoPlayerVideoInfoData.partTitle
            )
        }
//        if (BuildConfig.DEBUG) {
//            Box(
//                modifier = Modifier
//                    .align(Alignment.TopStart)
//                    .padding(8.dp)
//                    .clip(MaterialTheme.shapes.medium)
//                    .background(Color.Black.copy(alpha = 0.3f))
//            ) {
//                Text(
//                    modifier = Modifier.padding(8.dp),
//                    text = videoPlayerDebugInfoData.debugInfo
//                )
//            }
//        }
        BottomSubtitle()
        SkipTips()
        SponsorBlockTip(
            show = showSponsorBlockTip && !showClickableControllers,
            segment = currentSponsorSegment,
            onSkip = onSkipSponsorSegment,
            onDismiss = onDismissSponsorBlockTip
        )
        PlayStateTips(
            canShowPause = !showInfo && !showSeekController
        )
        ControllerVideoInfo(
            show = showInfo,
            playSpeed = videoPlayer.speed,
            onHideInfo = { showInfo = false },
            onPlay = {
                if (videoPlayer.currentPosition >= videoPlayer.duration) {
                    goTime = 0
                    onGoTime(0)
                } else {
                    onPlay()
                }
            },
            onPause = onPause,
            onPlaySpeedChange = onPlaySpeedChange,
            isAudioOnly = videoPlayerConfigData.currentPlaybackMediaMode == PlaybackMediaMode.AudioOnly,
            onTogglePlaybackMediaMode = {
                val nextMode = if (videoPlayerConfigData.currentPlaybackMediaMode == PlaybackMediaMode.AudioOnly) {
                    PlaybackMediaMode.Normal
                } else {
                    PlaybackMediaMode.AudioOnly
                }
                onPlaybackMediaModeChange(nextMode)
            },
            onOpenUpSpace = onOpenUpSpace,
            onRefreshVideo = {
                if (videoPlayer.duration > 0 && videoPlayer.currentPosition >= videoPlayer.duration) {
                    goTime = 0
                    onGoTime(0)
                } else {
                    onRefreshVideo()
                }
            },
            onOpenDanmaku = onOpenDanmaku,
            onHideDanmaku = onHideDanmaku,
            onOpenPlayList = {
                showInfo = false
                showListController = true
            },
            onOpenRelatedVideo = {
                onToggleRelatedVideos(true)

                scope.launch(Dispatchers.Main) {
                    delay(50)
                    showInfo = false
                }
            },
            onOpenSetting = {
                showInfo = false
                showMenuController = true
            },
            onLoopPlayModeChange = onLoopPlayModeChange,
            onRotationChange = onRotationChange,
            userActionContent = userActionContent,
            onSeekBack = {
                scope.launch(Dispatchers.Main) {
                    delay(100)
                    openSeekController()
                    onTimeBack()
                }
            },
            onSeekForward = {
                scope.launch(Dispatchers.Main) {
                    delay(100)
                    openSeekController()
                    onTimeForward()
                }
            },
            onSubtitleChange = onSubtitleChange,
            onLoadNextVideo = onLoadNextVideo,
            onShowComment = onShowComment
        )
        SeekController(
            show = showSeekController,
            goTime = goTime,
            moveState = moveState
        )
        VideoListController(
            show = showListController,
            onPlayNewVideo = onPlayNewVideo
        )
        MenuController(
            show = showMenuController,
            onResolutionChange = onResolutionChange,
            onCodecChange = onCodecChange,
            onAspectRatioChange = onAspectRatioChange,
            onRotationChange = onRotationChange,
            onPlaySpeedChange = onPlaySpeedChange,
            onAudioChange = onAudioChange,
            onLiveQualityChange = onLiveQualityChange,
            onLiveCodecChange = onLiveCodecChange,
            onDanmakuSwitchChange = onDanmakuSwitchChange,
            onDanmakuSizeChange = onDanmakuSizeChange,
            onDanmakuOpacityChange = onDanmakuOpacityChange,
            onDanmakuAreaChange = onDanmakuAreaChange,
            onDanmakuMaskChange = onDanmakuMaskChange,
            onDanmakuFilterLevelChange = onDanmakuFilterLevelChange,
            isLive = videoPlayerConfigData.isLive,
            onSubtitleChange = onSubtitleChange,
            onSubtitleSizeChange = onSubtitleSizeChange,
            onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
            onSubtitleBottomPadding = onSubtitleBottomPadding,
            onPlayModeChange = onPlayModeChange,
            onTripleLike = onTripleLike
        )
        // 缓存底部进度条显示条件，避免频繁计算
        val shouldShowBottomProgressBar by remember { 
            derivedStateOf { 
                showBottomProgressBar && !videoPlayerConfigData.isLive && !showInfo && !showSeekController 
            } 
        }
        
        // 底部常驻进度条组件
        if (shouldShowBottomProgressBar) {
            var throttledProgress by remember { mutableStateOf(0f) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay( 500L)
                    val currentPosition = videoPlayer.currentPosition
                    val duration = videoPlayer.duration
                    val currentProgress = if (duration > 0) {
                        currentPosition.toFloat() / duration.toFloat()
                    } else {
                        0f
                    }
                    throttledProgress = currentProgress
                }
            }
            
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                progress = { throttledProgress },
                color = Color(0xFFBD26B8).copy(alpha = 0.5f),
                trackColor = Color.Black.copy(alpha = 0.2f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
        // 推荐视频组件（在连按两次下键时显示）, UI实现在VideoPlayerV3Screen.kt中
    }
}

@Composable
private fun BoxScope.AudioOnlyModeTip(
    title: String,
    partTitle: String
) {
    val displayTitle = if (title.contains(partTitle) || partTitle.isBlank()) title else "$partTitle | $title"
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .background(Color.Black.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.video_player_audio_only_tip),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        if (displayTitle.isNotBlank()) {
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = displayTitle,
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
