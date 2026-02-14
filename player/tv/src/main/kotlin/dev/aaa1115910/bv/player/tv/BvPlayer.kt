package dev.aaa1115910.bv.player.tv

import android.os.CountDownTimer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.tv.material3.Text
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.biliapi.http.entity.video.ClipInfo
import dev.aaa1115910.biliapi.http.entity.video.ClipType
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.BvVideoPlayer
import dev.aaa1115910.bv.player.impl.exo.ExoMediaPlayer
import dev.aaa1115910.bv.player.VideoPlayerListener
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerClockState
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDebugInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekState
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.RequestState
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoRotation
import dev.aaa1115910.bv.player.entity.VideoPlayerClockState
import dev.aaa1115910.bv.player.entity.VideoPlayerDebugInfoData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekState
import dev.aaa1115910.bv.player.entity.VideoPlayerStateData
import dev.aaa1115910.bv.player.entity.DefaultStartPosition
import dev.aaa1115910.bv.player.tv.controller.SkipEdTip
import dev.aaa1115910.bv.player.tv.controller.SkipOpTip
import dev.aaa1115910.bv.player.tv.controller.VideoPlayerController
import dev.aaa1115910.bv.util.countDownTimer
import dev.aaa1115910.bv.player.util.DanmakuMaskFinder
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.requestFocus
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun BvPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
    danmakuPlayer: DanmakuPlayer?,
    danmakuOpacity: Float,
    playerSeekForwardStep: Int = 10,
    playerSeekBackwardStep: Int = 5,
    showBottomProgressBar: Boolean = false,
    useTextureViewFixPortraitVideo: Boolean = false,
    onSendHeartbeat: suspend (Int) -> Unit,
    onClearBackToHistoryData: () -> Unit,
    onLoadNextVideo: (Boolean) -> Unit,
    onExit: () -> Unit,
    onLoadNewVideo: (VideoListItem) -> Unit,
    onResolutionChange: (Resolution, afterChange: suspend () -> Unit) -> Unit,
    onCodecChange: (VideoCodec, afterChange: suspend () -> Unit) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onAudioChange: (Audio, afterChange: suspend () -> Unit) -> Unit,
    onLiveQualityChange: (Int) -> Unit = {},
    onLiveCodecChange: (LiveCodec) -> Unit = {},
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onToggleRelatedVideos: (Boolean) -> Unit = {},
    onOpenUpSpace: () -> Unit = {},
    onShowDanmakuChange: (Boolean) -> Unit = {},
    onLoopPlayModeChange: (Boolean) -> Unit = {},
    onRefreshVideo: () -> Unit = {},
    onLiveRetry: () -> Unit = {},
    onShowComment: () -> Unit = {},
    onTripleLike: () -> Unit = {},
    useTripleLikeOnLongPress: Boolean = false,
    userActionContent: @Composable (
        modifier: Modifier,
        focusMap: Map<String, FocusRequester>,
        onFocus: (String) -> Unit,
        onPauseAutoHide: (Boolean) -> Unit
    ) -> Unit = { _, _, _, _ -> }
) {
//    // 调试重组次数: AtomicInteger，不被 Compose 追踪，只记录真实由外部状态引起的重组次数。
//    val recomposeCounter = remember { java.util.concurrent.atomic.AtomicInteger(0) }
//    SideEffect {
//        val value = recomposeCounter.incrementAndGet()
//        println("Recompose(BvPlayer): $value")
//    }

    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("BvPlayer")
    //val tvVideoPlayerData = LocalTvVideoPlayerData.current
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val videoPlayerDanmakuMaskData = LocalVideoPlayerDanmakuMasksData.current
    val videoPlayerHistoryData = LocalVideoPlayerHistoryData.current
    val videoPlayerLoadStateData = LocalVideoPlayerLoadStateData.current
    val videoPlayerLogsData = LocalVideoPlayerLogsData.current
    val videoPlayerVideoInfoData = LocalVideoPlayerVideoInfoData.current

    val focusRequester = remember { FocusRequester() }
//    println("isLoop: ${videoPlayerConfigData.isLoop}, showDanmaku: ${videoPlayerConfigData.showDanmaku}")

    // 直接调用 danmakuPlayer 会始终为 null
    var mDanmakuPlayer: DanmakuPlayer? by remember { mutableStateOf(null) }

    var showLogs by remember { mutableStateOf(false) }
    var showBackToHistory by remember { mutableStateOf(false) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var exception by remember { mutableStateOf<Exception?>(null) }
    //var proxyArea by remember { mutableStateOf(ProxyArea.MainLand) }

    val typeFilter by remember { mutableStateOf(TypeFilter()) }
    var danmakuConfig by remember { mutableStateOf(DanmakuConfig()) }

    val seekState = remember { VideoPlayerSeekState() }
    var currentVideoAspectRatio by remember { mutableStateOf(videoPlayerConfigData.currentVideoAspectRatio) }
    var currentVideoRotation by remember { mutableStateOf(videoPlayerConfigData.currentVideoRotation) }
    var currentPlaySpeed by remember { mutableFloatStateOf(videoPlayerConfigData.currentVideoSpeed) }
    var aspectRatioValue by remember { mutableFloatStateOf(16f / 9f) }
    var lastPlayed by remember { mutableLongStateOf(0L) }
    
    var pendingDanmakuPosition by remember { mutableLongStateOf(-1L) }
    
    var danmakuNeedsResume by remember { mutableStateOf(false) }
    
    var lastDanmakuSeekTime by remember { mutableLongStateOf(0L) }
    var defaultAspectRatio by remember { mutableFloatStateOf(16 / 9f) }
    var lastHeartbeatPosition by remember { mutableLongStateOf(0L) }
    var showInfoProvider: () -> Boolean by remember { mutableStateOf({ false }) }

    val clockState = remember { VideoPlayerClockState() }

    var hideLogsTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var clockRefreshTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var hideBackToHistoryTimer: CountDownTimer? by remember { mutableStateOf(null) }

    var currentDanmakuMaskFrame: DanmakuMaskFrame? by remember { mutableStateOf(null) }

    // 跳过片头片尾相关状态
    var showSkipOpTip by remember { mutableStateOf(false) }
    var showSkipEdTip by remember { mutableStateOf(false) }
    var skipOpTipText by remember { mutableStateOf("即将跳过片头") }
    var skipEdTipText by remember { mutableStateOf("即将跳过片尾") }
    var processedClipIndices by remember { mutableStateOf(setOf<Int>()) }

    // 使用 rememberUpdatedState 来跟踪 clipInfoList 和 skipPgcIntroOutro 的最新值
    // 这样可以在非 Composable 上下文（定时器回调）中读取到最新值
    val currentClipInfoList by rememberUpdatedState(videoPlayerConfigData.clipInfoList)
    val currentSkipPgcIntroOutro by rememberUpdatedState(videoPlayerConfigData.skipPgcIntroOutro)

    // 当 clipInfoList 变化时，重置已处理的 clip 索引
    // 这确保了切换到新视频时，跳过片头/片尾功能能够正常工作
    LaunchedEffect(videoPlayerConfigData.clipInfoList) {
        processedClipIndices = emptySet()
    }

    // 跳过片头片尾检测任务
    val checkSkipTask: (Long) -> Unit = { positionMs ->
        val currentPosition = (positionMs / 1000).toInt()  // 毫秒转秒
        // 使用 rememberUpdatedState 获取最新值
        if (currentSkipPgcIntroOutro && currentClipInfoList.isNotEmpty() && isPlaying) {
            currentClipInfoList.forEachIndexed { index, clipInfo ->
                // 跳过已处理的 clip
                if (index in processedClipIndices) return@forEachIndexed

                when (clipInfo.clipType) {
                    ClipType.CLIP_TYPE_OP -> {
                        // 检测是否到达片头开始时间
                        val inRange = currentPosition >= clipInfo.start && currentPosition < clipInfo.end
                        if (inRange) {
                            scope.launch(Dispatchers.Main) {
                                skipOpTipText = clipInfo.toastText.ifBlank { "即将跳过片头" }
                                showSkipOpTip = true
                                // 显示提示后短暂延迟再跳转
                                delay(1500)
                                videoPlayer.seekTo(clipInfo.end * 1000L)
                                mDanmakuPlayer?.seekTo(clipInfo.end * 1000L)
                                showSkipOpTip = false
                            }
                            processedClipIndices = processedClipIndices + index
                        }
                    }
                    ClipType.CLIP_TYPE_ED -> {
                        // 检测是否到达片尾开始时间
                        val inRange = currentPosition >= clipInfo.start && currentPosition < clipInfo.end
                        if (inRange) {
                            scope.launch(Dispatchers.Main) {
                                skipEdTipText = clipInfo.toastText.ifBlank { "即将跳过片尾" }
                                showSkipEdTip = true
                                delay(1500)
                                videoPlayer.seekTo(clipInfo.end * 1000L)
                                mDanmakuPlayer?.seekTo(clipInfo.end * 1000L)
                                showSkipEdTip = false
                            }
                            processedClipIndices = processedClipIndices + index
                        }
                    }
                    else -> {}  // 忽略其他类型
                }
            }
        }
    }

    val updateDanmakuMaskForPosition: suspend (Long) -> Unit = { position ->
        val maskFrame = DanmakuMaskFinder.findMaskFrame(
            videoPlayerDanmakuMaskData.danmakuMasks,
            position
        )
        withContext(Dispatchers.Main) {
            if (currentDanmakuMaskFrame != maskFrame) {
                currentDanmakuMaskFrame = maskFrame
            }
        }
    }


    // 独立弹幕层句柄（Stable），父级重组频率降低
    val danmakuLayerHandle = remember { DanmakuLayerHandle() }

    val initDanmakuConfig: () -> Unit = {
        val danmakuTypes = videoPlayerConfigData.currentDanmakuEnabledList
        if (!danmakuTypes.contains(DanmakuType.All)) {
            val types = DanmakuType.entries.toMutableList()
            types.remove(DanmakuType.All)
            types.removeAll(danmakuTypes)
            val filterTypes = types.mapNotNull {
                when (it) {
                    DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    else -> null
                }
            }
            filterTypes.forEach { typeFilter.addFilterItem(it) }
        }
        // Ensure screenPart is set from config (clamped 0f..1f)
        val safeArea = videoPlayerConfigData.currentDanmakuArea.coerceIn(0f, 1f)
        danmakuConfig = danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI,
            textSizeScale = videoPlayerConfigData.currentDanmakuScale,
            dataFilter = listOf(typeFilter),
            visibility = videoPlayerConfigData.showDanmaku,
            alpha = danmakuOpacity,
            screenPart = safeArea,
            liveMode = videoPlayerConfigData.isLive,
            maxLiveScreenDanmakuCount = 100,
            liveMaxPendingCount = 200,
            liveMergeCache = true
        )
        danmakuConfig.updateFilter()
        logger.info { "Init danmaku config (liveMode=${videoPlayerConfigData.isLive}): $danmakuConfig" }
        mDanmakuPlayer?.updateConfig(danmakuConfig)
    }

    val updateDanmakuConfigTypeFilter: () -> Unit = {
        val danmakuTypes = videoPlayerConfigData.currentDanmakuEnabledList
        typeFilter.clear()
        if (!danmakuTypes.contains(DanmakuType.All)) {
            val types = DanmakuType.entries.toMutableList()
            types.remove(DanmakuType.All)
            types.removeAll(danmakuTypes)
            val filterTypes = types.mapNotNull {
                when (it) {
                    DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    else -> null
                }
            }
            filterTypes.forEach { typeFilter.addFilterItem(it) }
        }
        logger.info { "Update danmaku type filters: ${typeFilter.filterSet}" }
        danmakuConfig.updateFilter()
        // ensure screenPart kept in sync
        danmakuConfig = danmakuConfig.copy(screenPart = videoPlayerConfigData.currentDanmakuArea.coerceIn(0f, 1f))
        mDanmakuPlayer?.updateConfig(danmakuConfig)
    }

    val updateDanmakuConfig: () -> Unit = {
        danmakuConfig = danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI,
            textSizeScale = videoPlayerConfigData.currentDanmakuScale,
            alpha = videoPlayerConfigData.currentDanmakuOpacity,
            screenPart = videoPlayerConfigData.currentDanmakuArea.coerceIn(0f, 1f),
            liveMode = videoPlayerConfigData.isLive
        )
        logger.info { "Update danmaku config (liveMode=${videoPlayerConfigData.isLive}): $danmakuConfig" }
        mDanmakuPlayer?.updateConfig(danmakuConfig)
    }

    val updateVideoAspectRatio: () -> Unit = {
        aspectRatioValue = when (currentVideoAspectRatio) {
            VideoAspectRatio.Default -> defaultAspectRatio
            VideoAspectRatio.FourToThree -> 4 / 3f
            VideoAspectRatio.SixteenToNine -> 16 / 9f
        }
        logger.info { "Update video player aspectRatio: $aspectRatioValue" }
    }

    val sendHeartbeat: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            val time = withContext(Dispatchers.Main) {
                val currentTime = (videoPlayer.currentPosition.coerceAtLeast(0L) / 1000).toInt()
                val totalTime = (videoPlayer.duration.coerceAtLeast(0L) / 1000).toInt()

                if (totalTime == 0) {
                    -2 // 无法正常播放
                } else if (currentTime >= totalTime - 1) {
                    -1 // 播放完后上报的时间应为 -1
                } else {
                    currentTime // 播放中上报当前时间
                }
            }
            if (time > -2) {
                onSendHeartbeat(time)
            }
        }
    }

    // updateBackToHistory() 中使用 videoPlayerHistoryData.lastPlayed 无法获取到新值
    LaunchedEffect(videoPlayerHistoryData.lastPlayed) {
        lastPlayed = videoPlayerHistoryData.lastPlayed.toLong()
    }

    LaunchedEffect(videoPlayerVideoInfoData.width, videoPlayerVideoInfoData.height) {
        val newAspectRatio =
            videoPlayerVideoInfoData.width / videoPlayerVideoInfoData.height.toFloat()
        defaultAspectRatio = newAspectRatio.takeIf { it > 0 } ?: (16 / 9f)
        updateVideoAspectRatio()
    }

    val updateBackToHistory: () -> Unit = {
        // 此处使用 videoPlayerHistoryData.lastPlayed 无法获取到新值
        //if (videoPlayerHistoryData.lastPlayed > 0 && hideBackToHistoryTimer == null) {
        if (lastPlayed > 0 && hideBackToHistoryTimer == null) {
            logger.info { "show showBackToHistory: ${videoPlayerHistoryData.lastPlayed}" }
            scope.launch(Dispatchers.Main) {
                showBackToHistory = true
                hideBackToHistoryTimer = countDownTimer(5000, 1000, "hideBackToHistoryTimer") {
                    scope.launch(Dispatchers.Main) {
                        showBackToHistory = false
                        hideBackToHistoryTimer = null
                        //playerViewModel.lastPlayed = 0
                        onClearBackToHistoryData()
                    }
                }
            }
        }
    }

    val videoPlayerListener = object : VideoPlayerListener {
        override fun onError(error: Exception) {
            logger.info { "onError: $error" }
            if (videoPlayerConfigData.isLive) {
                // 直播模式：自动重连，不立即显示错误 UI（参考 wiliwili 的 retryRequestData）
                logger.info { "Live mode: triggering auto retry" }
                scope.launch(Dispatchers.Main) {
                    isBuffering = true  // 显示缓冲状态代替错误状态
                }
                onLiveRetry()
            } else {
                scope.launch(Dispatchers.Main) {
                    isError = true
                    exception = error.cause as Exception?
                }
            }
        }

        override fun onReady() {
            logger.info { "onReady" }
            scope.launch(Dispatchers.Main) {
                isError = false
                exception = null
                initDanmakuConfig()
                updateVideoAspectRatio()

                //reset default play speed
                onPlaySpeedChange(currentPlaySpeed)
                logger.info { "Reset default play speed: $currentPlaySpeed" }
                videoPlayer.speed = currentPlaySpeed
                mDanmakuPlayer?.updatePlaySpeed(currentPlaySpeed)

                // 如果视频正在播放，同步恢复弹幕
                if (videoPlayer.isPlaying) {
                    logger.info { "onReady: video is playing, resuming danmaku" }
                    mDanmakuPlayer?.start()
                }
            }
        }

        override fun onPlay() {
            logger.info { "onPlay" }
            scope.launch(Dispatchers.Main) {
                val wasPlaying = isPlaying
                isPlaying = true
                isBuffering = false
                val currentTime = System.currentTimeMillis()

                if (danmakuNeedsResume && pendingDanmakuPosition >= 0) {
                    val pos = pendingDanmakuPosition
                    danmakuNeedsResume = false
                    pendingDanmakuPosition = -1
                    logger.info { "onPlay: resuming danmaku from pendingDanmakuPosition=${pos.formatHourMinSec()}" }
                    mDanmakuPlayer?.seekTo(pos)
                    mDanmakuPlayer?.start()
                    lastDanmakuSeekTime = currentTime
                    lastHeartbeatPosition = pos
                    updateBackToHistory()
                } else if (!wasPlaying) {
                    val timeSinceLastSeek = currentTime - lastDanmakuSeekTime
                    if (timeSinceLastSeek < 3000) {
                        logger.info { "onPlay: skip seek (timeSinceLastSeek=${timeSinceLastSeek}ms)" }
                        mDanmakuPlayer?.start()
                    } else {
                        val danmakuPosition = if (lastPlayed > 0) lastPlayed else videoPlayer.currentPosition
                        logger.info { "onPlay: danmakuPosition=${danmakuPosition.formatHourMinSec()}, currentPosition=${videoPlayer.currentPosition.formatHourMinSec()}" }
                        mDanmakuPlayer?.seekTo(danmakuPosition)
                        mDanmakuPlayer?.start()
                        lastDanmakuSeekTime = currentTime
                        lastHeartbeatPosition = danmakuPosition
                        updateBackToHistory()
                    }
                }
            }
        }

        override fun onPause() {
            logger.info { "onPause" }
            mDanmakuPlayer?.pause()
            scope.launch(Dispatchers.Main) {
                isPlaying = false
            }
        }

        override fun onBuffering() {
            logger.info { "onBuffering" }
            scope.launch(Dispatchers.Main) {
                isBuffering = true
                isPlaying = false
            }
            mDanmakuPlayer?.pause()
        }

        override fun onEnd() {
            if (videoPlayerConfigData.showRelatedVideos) {
                logger.info { "onEnd: show related videos, skip auto next" }
                scope.launch(Dispatchers.Main) {
                    isPlaying = false
                }
                return
            }

            if (videoPlayerConfigData.isLoop) {
                logger.info { "onEnd: replay" }
                scope.launch(Dispatchers.Main) {
                    videoPlayer.seekTo(0)
                    mDanmakuPlayer?.seekTo(0)
                    mDanmakuPlayer?.pause()
                    videoPlayer.start()
                }
                return
            }

            logger.info { "onEnd" }
            mDanmakuPlayer?.pause()
            scope.launch(Dispatchers.Main) {
                isPlaying = false
                if (!videoPlayerConfigData.incognitoMode) sendHeartbeat()
                // 当控制信息面板显示时不自动播放下一集
                if (!showInfoProvider()) {
                    onLoadNextVideo(false)
                } else {
                    logger.info { "Skip auto next because info panel visible" }
                }
            }
        }

        override fun onIdle() {
            //TODO("Not yet implemented")
        }

        override fun onSeekBack(seekBackIncrementMs: Long) {
            mDanmakuPlayer?.seekTo(seekState.position)
            mDanmakuPlayer?.pause()
        }

        override fun onSeekForward(seekForwardIncrementMs: Long) {
            mDanmakuPlayer?.seekTo(seekState.position)
            mDanmakuPlayer?.pause()
        }

        override fun onSeeked(position: Long) {
            logger.info { "onSeeked: ${position.formatHourMinSec()}" }
            // VLC seek操作完成后，同步弹幕位置
            mDanmakuPlayer?.seekTo(position)
            // 确保弹幕状态与视频播放状态一致
            if (isPlaying) {
                mDanmakuPlayer?.start()
            } else {
                mDanmakuPlayer?.pause()
            }
            lastHeartbeatPosition = position
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            logger.info { "onVideoSizeChanged: ${width}x${height}" }
            if (width > 0 && height > 0) {
                scope.launch(Dispatchers.Main) {
                    defaultAspectRatio = width / height.toFloat()
                    updateVideoAspectRatio()
                }
            }
        }

        override fun onProgress(position: Long, duration: Long, buffered: Int) {
            scope.launch(Dispatchers.Main.immediate) {
                val pos = position.coerceAtLeast(0L)
                val dur = duration.coerceAtLeast(0L)
                val buf = buffered.coerceIn(0, 100)

                if (seekState.position != pos) seekState.position = pos
                if (seekState.duration != dur) seekState.duration = dur
                if (seekState.bufferedPercentage != buf) seekState.bufferedPercentage = buf

                if (currentSkipPgcIntroOutro && currentClipInfoList.isNotEmpty() && isPlaying) {
                    checkSkipTask(pos)
                }

                if (videoPlayerDanmakuMaskData.danmakuMasks.isNotEmpty()) {
                    scope.launch(Dispatchers.Default) {
                        updateDanmakuMaskForPosition(pos)
                    }
                } else if (currentDanmakuMaskFrame != null) {
                    currentDanmakuMaskFrame = null
                }

                if (!videoPlayerConfigData.incognitoMode && isPlaying) {
                    if (pos - lastHeartbeatPosition >= 15_000) {
                        lastHeartbeatPosition = pos
                        sendHeartbeat()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
    }

    LaunchedEffect(videoPlayerConfigData.isLoop, videoPlayerConfigData.showDanmaku) {
        videoPlayer.setPlayerEventListener(videoPlayerListener)
    }

    LaunchedEffect(danmakuPlayer) {
        mDanmakuPlayer = danmakuPlayer
        danmakuLayerHandle.updateDanmakuPlayer(danmakuPlayer)
        // 当弹幕播放器可用时，立即设置正确的配置（包括 visibility 和 alpha）
        val safeArea = videoPlayerConfigData.currentDanmakuArea.coerceIn(0f, 1f)
        danmakuConfig = danmakuConfig.copy(
            screenPart = safeArea,
            alpha = danmakuOpacity
        )
        mDanmakuPlayer?.updateConfig(danmakuConfig)
    }

    // Sync currentDanmakuArea -> danmakuConfig.screenPart
    LaunchedEffect(videoPlayerConfigData.currentDanmakuArea) {
        val safeArea = videoPlayerConfigData.currentDanmakuArea.coerceIn(0f, 1f)
        // update the danmaku config used by akdanmaku
        danmakuConfig = danmakuConfig.copy(screenPart = safeArea)
        logger.info { "Sync danmaku screenPart: $safeArea" }
        mDanmakuPlayer?.updateConfig(danmakuConfig)
    }

    LaunchedEffect(videoPlayerLoadStateData.loadState) {
        when (videoPlayerLoadStateData.loadState) {
            RequestState.Ready -> {}
            RequestState.Doing -> {}
            RequestState.Done -> {}
            RequestState.Success -> {}
            RequestState.Failed -> {
                exception = Exception(videoPlayerLoadStateData.errorMessage)
                isError = true
            }
        }
    }

    LaunchedEffect(videoPlayerLogsData.logs) {
        hideLogsTimer?.cancel()
        showLogs = true
        hideLogsTimer = countDownTimer(3000, 1000, "hideLogsTimer") {
            showLogs = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 在释放播放器前发送心跳，确保退出时进度被正确记录
            if (!videoPlayerConfigData.incognitoMode) {
                // 获取当前时间并直接调用上传方法
                val currentTime = (videoPlayer.currentPosition.coerceAtLeast(0L) / 1000).toInt()
                val totalTime = (videoPlayer.duration.coerceAtLeast(0L) / 1000).toInt()
                val time = if (totalTime == 0) {
                    -2 // 无法正常播放
                } else if (currentTime >= totalTime - 1) {
                    -1 // 播放完后上报的时间应为 -1
                } else {
                    currentTime // 播放中上报当前时间
                }
                if (time > -2) {
                    scope.launch(Dispatchers.IO) {
                        onSendHeartbeat(time)
                    }
                }
            }

            // 先暂停播放，防止渲染线程继续工作
            videoPlayer.pause()

            // 如果是 VLC 播放器，先分离视图再释放
            // 这防止在 Surface 被销毁后 VLC 仍尝试渲染导致 BufferQueue abandoned 错误
            if (videoPlayer is dev.aaa1115910.bv.player.impl.vlc.VlcMediaPlayer) {
                (videoPlayer as dev.aaa1115910.bv.player.impl.vlc.VlcMediaPlayer).detachVideoLayout()
            }

            videoPlayer.release()
        }
    }

    DisposableEffect(Unit) {
        clockRefreshTimer = countDownTimer(
            millisInFuture = Long.MAX_VALUE,
            countDownInterval = 1000,
            tag = "clockRefreshTimer",
            showLogs = false,
            onTick = {
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val second = calendar.get(Calendar.SECOND)
                if (clockState.hour != hour) clockState.hour = hour
                if (clockState.minute != minute) clockState.minute = minute
                if (clockState.second != second) clockState.second = second
            }
        )
        onDispose { clockRefreshTimer?.cancel() }
    }

    CompositionLocalProvider(
        LocalVideoPlayerSeekState provides seekState,
        LocalVideoPlayerClockState provides clockState,
        //LocalVideoPlayerHistoryData provides LocalVideoPlayerHistoryData.current.copy(
        //    showBackToHistory = showBackToHistory
        //),
        //LocalVideoPlayerHistoryData provides VideoPlayerHistoryData(
        //    lastPlayed = videoPlayerHistoryData.lastPlayed,
        //    showBackToHistory = showBackToHistory
        //),
        LocalVideoPlayerStateData provides VideoPlayerStateData(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            isError = isError,
            exception = exception,
            showBackToHistory = showBackToHistory
        ),
        LocalVideoPlayerDebugInfoData provides VideoPlayerDebugInfoData(
            debugInfo = videoPlayer.debugInfo
        ),
    ) {
        VideoPlayerController(
            modifier = modifier
                .focusRequester(focusRequester),
            videoPlayer = videoPlayer,
            playerSeekForwardStep = playerSeekForwardStep,
            playerSeekBackwardStep = playerSeekBackwardStep,
            showBottomProgressBar = showBottomProgressBar,
            showRelatedVideos = videoPlayerConfigData.showRelatedVideos,
            onToggleRelatedVideos = onToggleRelatedVideos,
            registerShowInfoProvider = { provider -> showInfoProvider = provider },

            onPlay = { videoPlayer.start() },
            onPause = {
                videoPlayer.pause()
                if (!videoPlayerConfigData.incognitoMode) sendHeartbeat()
            },
            onExit = {
                if (!videoPlayerConfigData.incognitoMode) sendHeartbeat()
                onExit()
            },
            onGoTime = {
                videoPlayer.seekTo(it)
                mDanmakuPlayer?.seekTo(it)
                // 根据视频播放状态决定弹幕状态
                if (isPlaying) {
                    mDanmakuPlayer?.start()
                } else {
                    mDanmakuPlayer?.pause()
                }
            },
            onBackToHistory = {
                val time = if (videoPlayerConfigData.defaultStartPosition == DefaultStartPosition.History) {
                    0L
                } else {
                    videoPlayerHistoryData.lastPlayed.toLong()
                }
                logger.fInfo { "Back to history/beginning: ${time.formatHourMinSec()}" }
                videoPlayer.seekTo(time)
                mDanmakuPlayer?.seekTo(time)
                // 根据视频播放状态决定弹幕状态
                if (isPlaying) {
                    mDanmakuPlayer?.start()
                } else {
                    mDanmakuPlayer?.pause()
                }
                //playerViewModel.lastPlayed = 0
                onClearBackToHistoryData()
                showBackToHistory = false
                hideBackToHistoryTimer?.cancel()
                hideBackToHistoryTimer = null
            },
            onPlayNewVideo = {
                if (!videoPlayerConfigData.incognitoMode) sendHeartbeat()
                //playerViewModel.partTitle = it.title
                //playerViewModel.loadPlayUrl(
                //    avid = it.aid,
                //    cid = it.cid,
                //    epid = it.epid,
                //    seasonId = it.seasonId,
                //    continuePlayNext = true
                //)
                onLoadNewVideo(it)
            },
            onResolutionChange = { resolution ->
                videoPlayer.pause()
                val current = videoPlayer.currentPosition
                pendingDanmakuPosition = current
                danmakuNeedsResume = true
                onResolutionChange(resolution) {
                    //scope.launch(Dispatchers.Default) {
                    //    playerViewModel.updateAvailableCodec()
                    //    playerViewModel.playQuality(qualityId)
                    withContext(Dispatchers.Main) {
                        videoPlayer.seekTo(current)
                        videoPlayer.start()
                    }
                    //}
                }
                //playerViewModel.currentQuality = qualityId
            },
            onCodecChange = { videoCodec ->
                videoPlayer.pause()
                val current = videoPlayer.currentPosition
                pendingDanmakuPosition = current
                danmakuNeedsResume = true
                onCodecChange(videoCodec) {
                    withContext(Dispatchers.Main) {
                        videoPlayer.seekTo(current)
                        videoPlayer.start()
                    }
                }
            },
            onAspectRatioChange = { aspectRadio ->
                currentVideoAspectRatio = aspectRadio
                onAspectRatioChange(currentVideoAspectRatio)
                updateVideoAspectRatio()
            },
            onRotationChange = { rotation ->
//                if (videoPlayerConfigData.currentResolution > Resolution.R1080P60) {
//                    // 4k及以上的视频旋转后画面很卡、hdr、杜比世界的视频旋转后色彩和对比度不对， 所以先切换到<=R1080P60
//                    val tempList =
//                        videoPlayerConfigData.availableResolutions.sortedByDescending { it.code }
//                    val currentQuality = tempList.firstOrNull { it.code <= Resolution.R1080P60.code }
//                        ?: tempList.last()
//                    if (videoPlayerConfigData.currentResolution != currentQuality) {
//                        videoPlayer.pause()
//                        val current = videoPlayer.currentPosition
//                        onResolutionChange(currentQuality) {
//                            withContext(Dispatchers.Main) {
//                                videoPlayer.seekTo(current)
//                                videoPlayer.start()
//                            }
//                        }
//                    }
//                }

                currentVideoRotation = rotation
                onRotationChange(rotation)
            },
            onPlaySpeedChange = { speed ->
                logger.info { "Set default play speed: $speed" }
                currentPlaySpeed = speed
                onPlaySpeedChange(speed)
                videoPlayer.speed = speed
                mDanmakuPlayer?.updatePlaySpeed(speed)
            },
            onAudioChange = { audio ->
                videoPlayer.pause()
                val current = videoPlayer.currentPosition
                onAudioChange(audio) {
                    withContext(Dispatchers.Main) {
                        videoPlayer.seekTo(current)
                        videoPlayer.start()
                    }
                }
            },
            onLiveQualityChange = onLiveQualityChange,
            onLiveCodecChange = onLiveCodecChange,
            onDanmakuSwitchChange = { enabledDanmakuTypes ->
                logger.info { "On enabled danmaku type change: $enabledDanmakuTypes" }
                onDanmakuSwitchChange(enabledDanmakuTypes)
                updateDanmakuConfigTypeFilter()
            },
            onDanmakuSizeChange = { scale ->
                logger.info { "On danmaku scale change: $scale" }
                onDanmakuSizeChange(scale)
                updateDanmakuConfig()
            },
            onDanmakuOpacityChange = { opacity ->
                logger.info { "On danmaku opacity change: $opacity" }
                onDanmakuOpacityChange(opacity)
                danmakuConfig = danmakuConfig.copy(alpha = opacity, screenPart = videoPlayerConfigData.currentDanmakuArea.coerceIn(0f, 1f))
                mDanmakuPlayer?.updateConfig(danmakuConfig)
            },
            onDanmakuAreaChange = { area ->
                logger.info { "On danmaku area change: $area" }
                onDanmakuAreaChange(area)
            },
            onDanmakuMaskChange = { mask ->
                logger.info { "On danmaku mask change: $mask" }
                onDanmakuMaskChange(mask)
            },
            onSubtitleChange = { subtitle ->
                onSubtitleChange(subtitle)
            },
            onSubtitleSizeChange = { size ->
                logger.info { "On subtitle font size change: $size" }
                onSubtitleSizeChange(size)
            },
            onSubtitleBackgroundOpacityChange = { opacity ->
                logger.info { "On subtitle background opacity change: $opacity" }
                onSubtitleBackgroundOpacityChange(opacity)
            },
            onSubtitleBottomPadding = { padding ->
                logger.info { "On subtitle bottom padding change: $padding" }
                onSubtitleBottomPadding(padding)
            },
            onPlayModeChange = { playMode ->
                logger.info { "On play mode change: $playMode" }
                onPlayModeChange(playMode)
            },
            onRequestFocus = { focusRequester.requestFocus(scope) },
            onOpenUpSpace = onOpenUpSpace,
            onRefreshVideo = onRefreshVideo,
            onOpenDanmaku = {
                onShowDanmakuChange(true)
                videoPlayerConfigData.showDanmaku = true
                danmakuConfig = danmakuConfig.copy(visibility = true, screenPart = videoPlayerConfigData.currentDanmakuArea.coerceIn(0f, 1f))
                danmakuConfig.updateVisibility()
                logger.info { "Update danmaku config: $danmakuConfig" }
                mDanmakuPlayer?.updateConfig(danmakuConfig)
            },
            onHideDanmaku = {
                onShowDanmakuChange(false)
                videoPlayerConfigData.showDanmaku = false
                danmakuConfig = danmakuConfig.copy(visibility = false, screenPart = videoPlayerConfigData.currentDanmakuArea.coerceIn(0f, 1f))
                danmakuConfig.updateVisibility()
                logger.info { "Update danmaku config: $danmakuConfig" }
                mDanmakuPlayer?.updateConfig(danmakuConfig)
            },
            onLoopPlayModeChange = {
                videoPlayerConfigData.isLoop = it
                onLoopPlayModeChange(it)
            },
            userActionContent = userActionContent,
            onLoadNextVideo = onLoadNextVideo,
            onShowComment = onShowComment,
            onTripleLike = onTripleLike,
            useTripleLikeOnLongPress = useTripleLikeOnLongPress
        ) {
            LaunchedEffect(Unit) {
                videoPlayer.setOptions()
            }

            // 将弹幕层副作用独立到子树，保证父级其它状态变化不导致 handle 以外的重组
            DanmakuLayerSideEffects(
                danmakuLayerHandle = danmakuLayerHandle,
                visible = videoPlayerConfigData.showDanmaku,
                maskFrame = currentDanmakuMaskFrame.takeIf { videoPlayerConfigData.currentDanmakuMask },
                videoAspectRatio = aspectRatioValue
            )
        
            BvVideoPlayer(
                modifier = Modifier
                    .then(
                        if (videoPlayer is ExoMediaPlayer) {
                            Modifier.aspectRatio(aspectRatioValue)
                        } else {
                            Modifier
                        }
                    )
                    .align(Alignment.Center),
                videoPlayer = videoPlayer,
                playerListener = videoPlayerListener,
                rotationDegrees = currentVideoRotation.degrees,
                danmakuPlayer = danmakuPlayer,
                forceUseTextureView = useTextureViewFixPortraitVideo
            )

            DanmakuLayer(
                modifier = Modifier.align(Alignment.TopCenter),
                handle = danmakuLayerHandle
            )

            // 跳过片头片尾提示
            if (showSkipOpTip) {
                SkipOpTip(
                    modifier = Modifier.align(Alignment.BottomStart),
                    show = true,
                    text = skipOpTipText
                )
            }
            if (showSkipEdTip) {
                SkipEdTip(
                    modifier = Modifier.align(Alignment.BottomStart),
                    show = true,
                    text = skipEdTipText
                )
            }

            if (showLogs) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(text = videoPlayerLogsData.logs)
                }
            }
        }
    }
}

// 同步弹幕层 UI 相关的独立副作用（蒙版/可见性/视频宽高比）
@Composable
private fun DanmakuLayerSideEffects(
    danmakuLayerHandle: DanmakuLayerHandle,
    visible: Boolean,
    maskFrame: DanmakuMaskFrame?,
    videoAspectRatio: Float,
) {
    LaunchedEffect(visible, maskFrame, videoAspectRatio) {
        danmakuLayerHandle.update(
            mask = maskFrame,
            visible = visible,
            videoAspectRatio = videoAspectRatio,
        )
    }
}
