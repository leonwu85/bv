package dev.aaa1115910.bv.player.mobile

import android.graphics.Bitmap
import android.os.CountDownTimer
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.AkDanmakuPlayer
import dev.aaa1115910.bv.player.BvVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerListener
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerClockData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDebugInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoPlayerClockData
import dev.aaa1115910.bv.player.entity.VideoPlayerDebugInfoData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekData
import dev.aaa1115910.bv.player.entity.VideoPlayerStateData
import dev.aaa1115910.bv.player.mobile.controller.BvPlayerController
import dev.aaa1115910.bv.player.util.DanmakuMaskFinder
import dev.aaa1115910.bv.player.util.danmakuMaskBitmap
import dev.aaa1115910.bv.player.util.renderMaskFrameToBitmap
import dev.aaa1115910.bv.util.countDownTimer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val FULLSCREEN_DANMAKU_TEXT_SIZE_SCALE = 1f
private const val EMBEDDED_DANMAKU_TEXT_SIZE_SCALE = 0.8f

private fun DanmakuConfig.invalidateTextSizeDependentState() {
    updateMeasure()
    updateLayout()
    updateRetainer()
    updateCache()
    updateRender()
}

private fun Bitmap.safeRecycle() {
    if (!isRecycled) {
        recycle()
    }
}

private class DanmakuMaskBitmapPool {
    private val bitmaps = arrayOfNulls<Bitmap>(2)
    private var nextIndex = 0

    fun render(frame: DanmakuMaskFrame): Bitmap {
        val index = nextIndex
        nextIndex = (nextIndex + 1) % bitmaps.size
        val reusableBitmap = bitmaps[index]?.takeUnless { it.isRecycled }
        return renderMaskFrameToBitmap(frame, reusableBitmap).also { renderedBitmap ->
            bitmaps[index] = renderedBitmap
        }
    }

    fun release() {
        bitmaps.indices.forEach { index ->
            bitmaps[index]?.safeRecycle()
            bitmaps[index] = null
        }
    }
}

@Composable
fun BvPlayer(
    modifier: Modifier = Modifier,
    isFullScreen: Boolean,
    onEnterFullScreen: () -> Unit,
    onExitFullScreen: () -> Unit,
    onBack: () -> Unit,
    onClearBackToHistoryData: () -> Unit,
    onReloadDanmakuAfterSeek: (Long, Boolean) -> Unit = { _, _ -> },
    onChangeResolution: (Resolution, afterChange: suspend () -> Unit) -> Unit,
    onChangeVideoCodec: (VideoCodec, afterChange: suspend () -> Unit) -> Unit,
    onChangeAudio: (Audio, afterChange: suspend () -> Unit) -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onToggleDanmaku: (Boolean) -> Unit,
    onEnabledDanmakuTypesChange: (List<DanmakuType>) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuScaleChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onLoadNextVideo: () -> Unit,
    onLoadNewVideo: (VideoListItem) -> Unit,
    videoPlayer: AbstractVideoPlayer,
    danmakuPlayer: DanmakuPlayer?,
    danmakuOpacity: Float,
    isLive: Boolean = false,
    onLiveDanmakuPlayerReady: ((com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer) -> Unit)? = null,
) {
    val logger = KotlinLogging.logger("BvPlayer")
    // 直接调用 danmakuPlayer 会始终为 null
    var mDanmakuPlayer: DanmakuPlayer? by remember { mutableStateOf(null) }
    var mLiveDanmakuPlayer: LiveDanmakuPlayer? by remember { mutableStateOf(null) }

    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val videoPlayerDanmakuMaskData = LocalVideoPlayerDanmakuMasksData.current
    val videoPlayerHistoryData = LocalVideoPlayerHistoryData.current
    // val videoPlayerLoadStateData = LocalVideoPlayerLoadStateData.current
    // val videoPlayerLogsData = LocalVideoPlayerLogsData.current
    // val videoPlayerVideoInfoData = LocalVideoPlayerVideoInfoData.current

    var showLogs by remember { mutableStateOf(false) }
    var showBackToHistory by remember { mutableStateOf(false) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var exception by remember { mutableStateOf<Exception?>(null) }

    val typeFilter by remember { mutableStateOf(TypeFilter()) }
    var danmakuConfig by remember { mutableStateOf(DanmakuConfig()) }

    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPercentage by remember { mutableStateOf(0) }
    // var currentVideoAspectRatio by remember { mutableStateOf(VideoAspectRatio.Default) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    //var currentPlaySpeed by remember { mutableFloatStateOf(Prefs.defaultPlaySpeed) }
    var aspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var lastPlayed by remember { mutableLongStateOf(0L) }

    var clock: Triple<Int, Int, Int> by remember { mutableStateOf(Triple(0, 0, 0)) }

    // var hideLogsTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var clockRefreshTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var hideBackToHistoryTimer: CountDownTimer? by remember { mutableStateOf(null) }

    var currentDanmakuMaskFrame: DanmakuMaskFrame? by remember { mutableStateOf(null) }
    var currentDanmakuMaskBitmap: Bitmap? by remember { mutableStateOf(null) }
    val danmakuMaskBitmapPool = remember { DanmakuMaskBitmapPool() }


    val updatePosition = {
        currentPosition = videoPlayer.currentPosition
        duration = videoPlayer.duration
        bufferedPercentage = videoPlayer.bufferedPercentage
    }

    val updateEnabledDanmakuTypeFilter: (List<DanmakuType>) -> Unit = { danmakuTypes ->
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
    }

    val danmakuTextSizeScale: () -> Float = {
        if (isFullScreen) {
            FULLSCREEN_DANMAKU_TEXT_SIZE_SCALE
        } else {
            EMBEDDED_DANMAKU_TEXT_SIZE_SCALE
        }
    }

    val buildDanmakuConfig: (Float) -> DanmakuConfig = { alpha ->
        val textSizeScale = danmakuTextSizeScale()
        val textSizeChanged = danmakuConfig.textSizeScale != textSizeScale
        danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI,
            textSizeScale = textSizeScale,
            dataFilter = listOf(typeFilter),
            alpha = alpha,
            liveMode = isLive
        ).also { config ->
            if (textSizeChanged) {
                config.invalidateTextSizeDependentState()
            }
        }
    }

    val updateAllDanmakuPlayerConfig: () -> Unit = {
        mDanmakuPlayer?.updateConfig(danmakuConfig)
        mLiveDanmakuPlayer?.updateConfig(danmakuConfig)
    }

    val initDanmakuConfig: () -> Unit = {
        updateEnabledDanmakuTypeFilter(videoPlayerConfigData.currentDanmakuEnabledList)
        danmakuConfig = buildDanmakuConfig(danmakuOpacity)
        danmakuConfig.updateFilter()
        updateAllDanmakuPlayerConfig()
    }

    val updateDanmakuConfigTypeFilter: () -> Unit = {
        updateEnabledDanmakuTypeFilter(videoPlayerConfigData.currentDanmakuEnabledList)
        danmakuConfig = buildDanmakuConfig(videoPlayerConfigData.currentDanmakuOpacity)
        danmakuConfig.updateFilter()
        updateAllDanmakuPlayerConfig()
    }

    val toggleDanmakuEnabled: (Boolean) -> Unit = { enabled ->
        updateEnabledDanmakuTypeFilter(if (enabled) videoPlayerConfigData.currentDanmakuEnabledList else listOf())
        danmakuConfig = buildDanmakuConfig(videoPlayerConfigData.currentDanmakuOpacity)
        danmakuConfig.updateFilter()
        updateAllDanmakuPlayerConfig()
    }

    val updateDanmakuConfig: () -> Unit = {
        danmakuConfig = buildDanmakuConfig(videoPlayerConfigData.currentDanmakuOpacity)
        updateAllDanmakuPlayerConfig()
    }

    val updateVideoAspectRatio: () -> Unit = {
        val aspectRatioValue = videoPlayer.videoWidth / videoPlayer.videoHeight.toFloat()
        aspectRatio = if (aspectRatioValue > 0) aspectRatioValue else 16 / 9f
    }

    val clearDanmakuMaskState: () -> Unit = {
        currentDanmakuMaskFrame = null
        currentDanmakuMaskBitmap = null
    }

    val updateDanmakuMaskForPosition: suspend (Long) -> Unit = update@{ position ->
        if (isLive || !videoPlayerConfigData.currentDanmakuMask || videoPlayerDanmakuMaskData.danmakuMasks.isEmpty()) {
            clearDanmakuMaskState()
            return@update
        }

        val maskFrame = DanmakuMaskFinder.findMaskFrame(videoPlayerDanmakuMaskData.danmakuMasks, position)
        if (maskFrame === currentDanmakuMaskFrame) return@update

        if (maskFrame == null) {
            clearDanmakuMaskState()
            return@update
        }

        val renderedBitmap = withContext(Dispatchers.Default) {
            danmakuMaskBitmapPool.render(maskFrame)
        }

        currentDanmakuMaskFrame = maskFrame
        currentDanmakuMaskBitmap = renderedBitmap
    }

    val updateBackToHistory: () -> Unit = {
        // 此处使用 videoPlayerHistoryData.lastPlayed 无法获取到新值
        //if (videoPlayerHistoryData.lastPlayed > 0 && hideBackToHistoryTimer == null) {
        if (lastPlayed > 0 && hideBackToHistoryTimer == null) {
            logger.info { "show showBackToHistory: ${videoPlayerHistoryData.lastPlayed}" }
            showBackToHistory = true
            hideBackToHistoryTimer = countDownTimer(5000, 1000, "hideBackToHistoryTimer") {
                showBackToHistory = false
                hideBackToHistoryTimer = null
                //playerViewModel.lastPlayed = 0
                onClearBackToHistoryData()
            }
        }
    }

    val videoPlayerListener = object : VideoPlayerListener {
        override fun onError(error: Exception) {
            println("onError: $error")
            isError = true
            exception = (error.cause as? Exception) ?: error
        }

        override fun onReady() {
            logger.info { "onReady" }
            isError = false
            exception = null
            initDanmakuConfig()

            updateVideoAspectRatio()


            videoPlayer.start()

            //reset default play speed
            logger.info { "Reset default play speed: ${videoPlayerConfigData.currentVideoSpeed}" }
            videoPlayer.speed = videoPlayerConfigData.currentVideoSpeed
            mDanmakuPlayer?.updatePlaySpeed(videoPlayerConfigData.currentVideoSpeed)
        }

        override fun onPlay() {
            logger.info { "onPlay" }
            // 同步弹幕到视频当前位置
            val currentPosition = videoPlayer.currentPosition
            mDanmakuPlayer?.seekTo(currentPosition)
            mDanmakuPlayer?.start()
            isPlaying = true
            isBuffering = false
            updateBackToHistory()
        }

        override fun onPause() {
            logger.info { "onPause" }
            if (isLive) {
                videoPlayer.start()
                mLiveDanmakuPlayer?.play()
                isPlaying = true
                isBuffering = false
                return
            }
            mDanmakuPlayer?.pause()
            isPlaying = false
        }

        override fun onBuffering() {
            logger.info { "onBuffering" }
            isBuffering = true
            mDanmakuPlayer?.pause()
        }

        override fun onEnd() {
            logger.info { "onEnd" }
            mDanmakuPlayer?.pause()
            isPlaying = false
            //if (!Prefs.incognitoMode) sendHeartbeat()

            onLoadNextVideo()
        }

        override fun onIdle() {
            logger.info { "onIdle" }
            mDanmakuPlayer?.pause()
        }

        override fun onSeekBack(seekBackIncrementMs: Long) {
            onReloadDanmakuAfterSeek(currentPosition, isPlaying)
        }

        override fun onSeekForward(seekForwardIncrementMs: Long) {
            onReloadDanmakuAfterSeek(currentPosition, isPlaying)
        }

    }

    LaunchedEffect(Unit) {
        while (true) {
            updatePosition()
            delay(200)
        }
    }

    // 同步 videoPlayerHistoryData.lastPlayed 到本地变量
    LaunchedEffect(videoPlayerHistoryData.lastPlayed) {
        lastPlayed = videoPlayerHistoryData.lastPlayed.toLong()
    }

    LaunchedEffect(danmakuPlayer) {
        mDanmakuPlayer = danmakuPlayer
        updateAllDanmakuPlayerConfig()
    }

    LaunchedEffect(isFullScreen) {
        updateDanmakuConfig()
    }

    LaunchedEffect(
        currentPosition,
        isLive,
        videoPlayerConfigData.currentDanmakuMask,
        videoPlayerConfigData.currentVideoCid,
        videoPlayerDanmakuMaskData.danmakuMasks.size
    ) {
        updateDanmakuMaskForPosition(currentPosition)
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
                clock = Triple(hour, minute, second)
            }
        )
        onDispose {
            clockRefreshTimer?.cancel()
            clearDanmakuMaskState()
            danmakuMaskBitmapPool.release()
        }
    }

    CompositionLocalProvider(
        LocalVideoPlayerSeekData provides VideoPlayerSeekData(
            duration = duration,
            position = currentPosition,
            bufferedPercentage = bufferedPercentage
        ),
        LocalVideoPlayerClockData provides VideoPlayerClockData(
            hour = clock.first,
            minute = clock.second,
            second = clock.third
        ),
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
        BvPlayerController(
            modifier = modifier,
            isFullScreen = isFullScreen,
            onEnterFullScreen = onEnterFullScreen,
            onExitFullScreen = onExitFullScreen,
            onBack = onBack,
            onPlay = { videoPlayer.start() },
            onPause = {
                if (isLive) {
                    videoPlayer.start()
                    mLiveDanmakuPlayer?.play()
                } else {
                    videoPlayer.pause()
                }
            },
            onSeekToPosition = { position ->
                onReloadDanmakuAfterSeek(position, isPlaying)
                videoPlayer.seekTo(position)
            },
            onChangeResolution = {
                val currentTime = currentPosition
                onChangeResolution(it) {
                    withContext(Dispatchers.Main) {
                        videoPlayer.seekTo(currentTime)
                        videoPlayer.start()
                    }
                }
            },
            onChangeVideoCodec = {
                val currentTime = currentPosition
                onChangeVideoCodec(it) {
                    withContext(Dispatchers.Main) {
                        videoPlayer.seekTo(currentTime)
                        videoPlayer.start()
                    }
                }
            },
            onChangeAudio = {
                val currentTime = currentPosition
                onChangeAudio(it) {
                    withContext(Dispatchers.Main) {
                        videoPlayer.seekTo(currentTime)
                        videoPlayer.start()
                    }
                }
            },
            onChangeSpeed = { speed ->
                onChangeSpeed(speed)
                videoPlayer.speed = speed
                mDanmakuPlayer?.updatePlaySpeed(speed)
            },
            onToggleDanmaku = { enabled ->
                toggleDanmakuEnabled(enabled)
                onToggleDanmaku(enabled)
            },
            onEnabledDanmakuTypesChange = { enabledDanmakuTypes ->
                onEnabledDanmakuTypesChange(enabledDanmakuTypes)
                updateDanmakuConfigTypeFilter()
            },
            onDanmakuOpacityChange = { opacity ->
                onDanmakuOpacityChange(opacity)
                danmakuConfig = buildDanmakuConfig(opacity)
                updateAllDanmakuPlayerConfig()
            },
            onDanmakuScaleChange = { scale ->
                onDanmakuScaleChange(scale)
                updateDanmakuConfig()
            },
            onDanmakuAreaChange = onDanmakuAreaChange,
            onPlayModeChange = onPlayModeChange,
            onPlayNewVideo = {
                //if (!Prefs.incognitoMode) sendHeartbeat()
                onLoadNewVideo(it)
            }
        ) {
            BvVideoPlayer(
                modifier = Modifier
                    .aspectRatio(aspectRatio)
                    .align(Alignment.Center),
                videoPlayer = videoPlayer, playerListener = videoPlayerListener
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight(videoPlayerConfigData.currentDanmakuArea)
                    .danmakuMaskBitmap(
                        bitmap = currentDanmakuMaskBitmap.takeIf { videoPlayerConfigData.currentDanmakuMask },
                        videoAspectRatio = aspectRatio
                    )
            ) {
                AkDanmakuPlayer(
                    modifier = Modifier.fillMaxSize(),
                    danmakuPlayer = mDanmakuPlayer,
                    isLiveMode = isLive,
                    onLiveDanmakuPlayerReady = { player ->
                        mLiveDanmakuPlayer = player
                        updateDanmakuConfig()
                        onLiveDanmakuPlayerReady?.invoke(player)
                    }
                )
            }
        }
    }
}
