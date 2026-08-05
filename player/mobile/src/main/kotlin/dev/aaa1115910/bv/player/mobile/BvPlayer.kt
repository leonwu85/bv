package dev.aaa1115910.bv.player.mobile

import android.graphics.Bitmap
import android.os.CountDownTimer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.AkDanmakuPlayer
import dev.aaa1115910.bv.player.BvVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerListener
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuSpeedMode
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.DefaultStartPosition
import dev.aaa1115910.bv.player.entity.LiveCodec
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
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoPlayerClockData
import dev.aaa1115910.bv.player.entity.VideoPlayerDebugInfoData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekData
import dev.aaa1115910.bv.player.entity.VideoPlayerStateData
import dev.aaa1115910.bv.player.mobile.controller.BvPlayerController
import dev.aaa1115910.bv.player.util.DanmakuMaskFinder
import dev.aaa1115910.bv.player.util.DanmakuSpeedPolicy
import dev.aaa1115910.bv.player.util.danmakuMaskBitmap
import dev.aaa1115910.bv.player.util.renderMaskFrameToBitmap
import dev.aaa1115910.bv.util.countDownTimer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToInt

private const val FULLSCREEN_DANMAKU_TEXT_SIZE_SCALE = 1f
private const val EMBEDDED_DANMAKU_TEXT_SIZE_SCALE = 0.8f

internal fun shouldResetPlaybackStartedState(previousCid: Long, currentCid: Long): Boolean =
    previousCid != 0L && currentCid != previousCid

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
    controlsEnabled: Boolean = true,
    onEnterFullScreen: () -> Unit,
    onExitFullScreen: () -> Unit,
    onBack: () -> Unit,
    onClearBackToHistoryData: () -> Unit,
    onReloadDanmakuAfterSeek: (Long, Boolean) -> Unit = { _, _ -> },
    onEnsureDanmakuCoverage: (Long) -> Unit = {},
    onRequestManualPlayback: () -> Boolean = { false },
    onChangeResolution: (Resolution, afterChange: suspend () -> Unit) -> Unit,
    onChangeVideoCodec: (VideoCodec, afterChange: suspend () -> Unit) -> Unit,
    onChangeAudio: (Audio, afterChange: suspend () -> Unit) -> Unit,
    onChangeLiveQuality: (Int) -> Unit = {},
    onChangeLiveCodec: (LiveCodec) -> Unit = {},
    onChangeLiveLine: (Int) -> Unit = {},
    onChangeSpeed: (Float) -> Unit,
    onToggleDanmaku: (Boolean) -> Unit,
    onEnabledDanmakuTypesChange: (List<DanmakuType>) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuScaleChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuSpeedModeChange: (DanmakuSpeedMode) -> Unit,
    onDanmakuPresentationSpeedChange: (Float) -> Unit,
    onDanmakuMergeChange: (Boolean) -> Unit,
    onDanmakuFilterLevelChange: (Int) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onLoadNextVideo: () -> Unit,
    onLoadNewVideo: (VideoListItem) -> Unit,
    onSendHeartbeat: suspend (Int) -> Unit,
    videoPlayer: AbstractVideoPlayer,
    danmakuPlayer: DanmakuPlayer?,
    danmakuOpacity: Float,
    isLive: Boolean = false,
    onLiveDanmakuPlayerReady: ((com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer) -> Unit)? = null,
    enableSponsorBlock: Boolean = false,
    sponsorBlockSkipMode: SponsorBlockSkipMode = SponsorBlockSkipMode.Manual,
    sponsorSegments: List<SponsorSegment> = emptyList(),
    showSponsorBlockTip: Boolean = false,
    currentSponsorSegment: SponsorSegment? = null,
    onShowSponsorBlockTip: (SponsorSegment) -> Unit = {},
    onSkipSponsorSegment: (SponsorSegment?) -> Unit = {},
    onDismissSponsorBlockTip: () -> Unit = {},
) {
    val logger = KotlinLogging.logger("BvPlayer")
    val scope = rememberCoroutineScope()
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
    var baseDanmakuDurationMs by remember { mutableLongStateOf(danmakuConfig.durationMs) }
    var baseRollingDanmakuDurationMs by remember { mutableLongStateOf(danmakuConfig.rollingDurationMs) }
    var appliedDanmakuDurationScale by remember { mutableFloatStateOf(1f) }

    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPercentage by remember { mutableStateOf(0) }
    // var currentVideoAspectRatio by remember { mutableStateOf(VideoAspectRatio.Default) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var currentPlaySpeed by remember { mutableFloatStateOf(videoPlayerConfigData.currentVideoSpeed) }
    var aspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var lastPlayed by remember { mutableLongStateOf(0L) }
    var initialPlaybackPositionMs by remember(videoPlayerConfigData.currentVideoCid) {
        mutableLongStateOf(
            if (videoPlayerHistoryData.isInitialPlaybackPositionResolved) {
                videoPlayerHistoryData.initialPlaybackPositionMs.coerceAtLeast(0L)
            } else {
                videoPlayerHistoryData.lastPlayed
                    .takeIf {
                        videoPlayerConfigData.defaultStartPosition == DefaultStartPosition.History
                    }
                    ?.toLong()
                    ?: 0L
            }
        )
    }
    var lastHeartbeatPosition by remember { mutableLongStateOf(0L) }
    var playbackStateCid by remember { mutableLongStateOf(videoPlayerConfigData.currentVideoCid) }
    var hasStartedPlaybackOnce by remember { mutableStateOf(false) }
    var pendingDanmakuPlaySyncPosition by remember { mutableLongStateOf(-1L) }
    var showAutoSkipSponsorTip by remember { mutableStateOf(false) }
    var autoSkipSponsorSeconds by remember { mutableIntStateOf(0) }
    var autoSkipSponsorTipToken by remember { mutableLongStateOf(0L) }
    var processedSponsorSegments by remember { mutableStateOf(setOf<SponsorSegment>()) }

    var clock: Triple<Int, Int, Int> by remember { mutableStateOf(Triple(0, 0, 0)) }

    // var hideLogsTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var clockRefreshTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var hideBackToHistoryTimer: CountDownTimer? by remember { mutableStateOf(null) }

    var currentDanmakuMaskFrame: DanmakuMaskFrame? by remember { mutableStateOf(null) }
    var currentDanmakuMaskBitmap: Bitmap? by remember { mutableStateOf(null) }
    val danmakuMaskBitmapPool = remember { DanmakuMaskBitmapPool() }

    LaunchedEffect(videoPlayerConfigData.currentVideoCid) {
        val currentVideoCid = videoPlayerConfigData.currentVideoCid
        if (shouldResetPlaybackStartedState(playbackStateCid, currentVideoCid)) {
            hasStartedPlaybackOnce = false
        }
        playbackStateCid = currentVideoCid
    }


    fun updatePlaybackProgress(position: Long, durationMs: Long, buffered: Int) {
        if (isBuffering && (videoPlayer.isPlaying || position > currentPosition)) {
            isBuffering = false
        }
        currentPosition = position
        duration = durationMs
        bufferedPercentage = buffered
    }

    val updatePosition = {
        updatePlaybackProgress(
            position = videoPlayer.currentPosition,
            durationMs = videoPlayer.duration,
            buffered = videoPlayer.bufferedPercentage
        )
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

    fun refreshDanmakuDurationBaseline(config: DanmakuConfig) {
        val expectedDuration = DanmakuSpeedPolicy.scaleDuration(
            baseDanmakuDurationMs,
            appliedDanmakuDurationScale
        )
        val expectedRollingDuration = DanmakuSpeedPolicy.scaleDuration(
            baseRollingDanmakuDurationMs,
            appliedDanmakuDurationScale
        )
        if (config.durationMs != expectedDuration) {
            baseDanmakuDurationMs = config.durationMs
        }
        if (config.rollingDurationMs != expectedRollingDuration) {
            baseRollingDanmakuDurationMs = config.rollingDurationMs
        }
    }

    fun applyDanmakuSpeedPolicy() {
        if (isLive) return
        val player = mDanmakuPlayer ?: return
        refreshDanmakuDurationBaseline(danmakuConfig)
        val timing = DanmakuSpeedPolicy.resolve(
            playbackSpeed = currentPlaySpeed,
            mode = videoPlayerConfigData.currentDanmakuSpeedMode,
            customPresentationSpeed = videoPlayerConfigData.currentDanmakuPresentationSpeed
        )
        val compensatedConfig = DanmakuSpeedPolicy.applyDurationScale(
            config = danmakuConfig,
            timing = timing,
            baseDurationMs = baseDanmakuDurationMs,
            baseRollingDurationMs = baseRollingDanmakuDurationMs
        )
        danmakuConfig = compensatedConfig
        appliedDanmakuDurationScale = timing.durationScale
        player.updateConfig(compensatedConfig)
        player.updatePlaySpeed(timing.timerSpeed)
    }

    val updateAllDanmakuPlayerConfig: () -> Unit = {
        if (isLive) {
            mDanmakuPlayer?.updateConfig(danmakuConfig)
        } else {
            applyDanmakuSpeedPolicy()
        }
        mLiveDanmakuPlayer?.updateConfig(danmakuConfig)
    }

    val currentEnableSponsorBlock by rememberUpdatedState(enableSponsorBlock)
    val currentSponsorBlockSkipMode by rememberUpdatedState(sponsorBlockSkipMode)
    val currentSponsorSegments by rememberUpdatedState(sponsorSegments)
    val currentShowSponsorBlockTip by rememberUpdatedState(showSponsorBlockTip)
    val currentSponsorSegment by rememberUpdatedState(currentSponsorSegment)
    val currentOnShowSponsorBlockTip by rememberUpdatedState(onShowSponsorBlockTip)
    val currentOnSkipSponsorSegment by rememberUpdatedState(onSkipSponsorSegment)
    val currentOnDismissSponsorBlockTip by rememberUpdatedState(onDismissSponsorBlockTip)

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

    val syncProcessedSponsorSegmentsForPosition: (Long) -> Unit = { targetPosition ->
        processedSponsorSegments = currentSponsorSegments
            .filterTo(mutableSetOf()) { it.endTime <= targetPosition }
    }

    fun skipSponsorSegment(segment: SponsorSegment?) {
        if (segment == null) {
            currentOnSkipSponsorSegment(null)
            return
        }
        syncProcessedSponsorSegmentsForPosition(segment.endTime)
        currentOnSkipSponsorSegment(segment)
    }

    val sendHeartbeat: () -> Unit = sendHeartbeat@{
        if (isLive || videoPlayerConfigData.incognitoMode) return@sendHeartbeat
        scope.launch(Dispatchers.IO) {
            val time = withContext(Dispatchers.Main) {
                val currentTime = (videoPlayer.currentPosition.coerceAtLeast(0L) / 1000).toInt()
                val totalTime = (videoPlayer.duration.coerceAtLeast(0L) / 1000).toInt()

                if (totalTime == 0) {
                    -2
                } else if (currentTime >= totalTime - 1) {
                    -1
                } else {
                    currentTime
                }
            }
            if (time > -2) {
                onSendHeartbeat(time)
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
            isBuffering = false
            exception = null
            initDanmakuConfig()

            updateVideoAspectRatio()


            if (videoPlayerConfigData.autoPlay || isLive || hasStartedPlaybackOnce) {
                videoPlayer.start()
            }

            //reset default play speed
            logger.info { "Reset default play speed: ${videoPlayerConfigData.currentVideoSpeed}" }
            currentPlaySpeed = videoPlayerConfigData.currentVideoSpeed
            videoPlayer.speed = currentPlaySpeed
            applyDanmakuSpeedPolicy()
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            logger.info { "onVideoSizeChanged: ${width}x${height}" }
            if (width > 0 && height > 0) {
                aspectRatio = width / height.toFloat()
            }
        }

        override fun onPlay() {
            logger.info { "onPlay" }
            val syncPosition = pendingDanmakuPlaySyncPosition.takeIf { it >= 0L }
                ?: if (!hasStartedPlaybackOnce) {
                    if (initialPlaybackPositionMs > 0L) {
                        initialPlaybackPositionMs
                    } else {
                        videoPlayer.currentPosition
                    }
                } else {
                    null
                }

            if (syncPosition != null) {
                logger.info { "onPlay: sync danmaku to $syncPosition" }
                mDanmakuPlayer?.seekTo(syncPosition)
                pendingDanmakuPlaySyncPosition = -1L
            } else {
                logger.info { "onPlay: resume danmaku without seek" }
            }

            hasStartedPlaybackOnce = true
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
            sendHeartbeat()
        }

        override fun onBuffering() {
            logger.info { "onBuffering" }
            isBuffering = true
            mDanmakuPlayer?.pause()
        }

        override fun onProgress(position: Long, duration: Long, buffered: Int) {
            updatePlaybackProgress(position, duration, buffered)
            if (
                !isLive &&
                videoPlayerConfigData.currentDanmakuEnabled &&
                isPlaying &&
                !isBuffering
            ) {
                onEnsureDanmakuCoverage(position)
            }
        }

        override fun onEnd() {
            logger.info { "onEnd" }
            mDanmakuPlayer?.pause()
            isPlaying = false
            sendHeartbeat()

            onLoadNextVideo()
        }

        override fun onIdle() {
            logger.info { "onIdle" }
            mDanmakuPlayer?.pause()
        }

        override fun onSeekBack(seekBackIncrementMs: Long) {
            syncProcessedSponsorSegmentsForPosition(currentPosition)
            if (!isPlaying) pendingDanmakuPlaySyncPosition = currentPosition
            onReloadDanmakuAfterSeek(currentPosition, isPlaying)
        }

        override fun onSeekForward(seekForwardIncrementMs: Long) {
            syncProcessedSponsorSegmentsForPosition(currentPosition)
            if (!isPlaying) pendingDanmakuPlaySyncPosition = currentPosition
            onReloadDanmakuAfterSeek(currentPosition, isPlaying)
        }

    }

    LaunchedEffect(Unit) {
        while (true) {
            updatePosition()
            delay(200)
        }
    }

    LaunchedEffect(videoPlayerConfigData.currentVideoCid) {
        lastHeartbeatPosition = 0L
    }

    LaunchedEffect(sponsorSegments) {
        processedSponsorSegments = emptySet()
    }

    LaunchedEffect(autoSkipSponsorTipToken) {
        if (autoSkipSponsorTipToken == 0L) return@LaunchedEffect
        showAutoSkipSponsorTip = true
        delay(3_000)
        showAutoSkipSponsorTip = false
    }

    // 同步 videoPlayerHistoryData.lastPlayed 到本地变量
    LaunchedEffect(videoPlayerHistoryData.lastPlayed) {
        lastPlayed = videoPlayerHistoryData.lastPlayed.toLong()
    }

    LaunchedEffect(
        videoPlayerHistoryData.initialPlaybackPositionMs,
        videoPlayerHistoryData.isInitialPlaybackPositionResolved
    ) {
        if (videoPlayerHistoryData.isInitialPlaybackPositionResolved) {
            initialPlaybackPositionMs =
                videoPlayerHistoryData.initialPlaybackPositionMs.coerceAtLeast(0L)
        }
    }

    LaunchedEffect(danmakuPlayer) {
        mDanmakuPlayer = danmakuPlayer
        updateAllDanmakuPlayerConfig()
    }

    LaunchedEffect(videoPlayerConfigData.currentVideoSpeed) {
        currentPlaySpeed = videoPlayerConfigData.currentVideoSpeed
        applyDanmakuSpeedPolicy()
    }

    LaunchedEffect(
        videoPlayerConfigData.currentDanmakuSpeedMode,
        videoPlayerConfigData.currentDanmakuPresentationSpeed
    ) {
        applyDanmakuSpeedPolicy()
    }

    LaunchedEffect(isFullScreen) {
        updateDanmakuConfig()
    }

    LaunchedEffect(
        videoPlayerConfigData.currentDanmakuEnabled,
        videoPlayerConfigData.currentDanmakuEnabledList
    ) {
        toggleDanmakuEnabled(videoPlayerConfigData.currentDanmakuEnabled)
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

    LaunchedEffect(currentPosition, isPlaying, isLive, videoPlayerConfigData.incognitoMode) {
        if (
            !isLive &&
            currentEnableSponsorBlock &&
            currentSponsorSegments.isNotEmpty() &&
            isPlaying &&
            !currentShowSponsorBlockTip
        ) {
            val segment = currentSponsorSegments.firstOrNull {
                currentPosition >= it.startTime && currentPosition < it.endTime
            }
            if (segment != null && segment !in processedSponsorSegments) {
                processedSponsorSegments = processedSponsorSegments + segment
                logger.info {
                    "SponsorBlock segment matched at ${currentPosition}ms, range=${segment.startTime}-${segment.endTime}, mode=$currentSponsorBlockSkipMode"
                }
                if (currentSponsorBlockSkipMode == SponsorBlockSkipMode.Auto) {
                    autoSkipSponsorSeconds = (segment.duration / 1000.0).roundToInt().coerceAtLeast(1)
                    autoSkipSponsorTipToken++
                    skipSponsorSegment(segment)
                } else {
                    currentOnShowSponsorBlockTip(segment)
                }
            }
        }

        if (!isLive && !videoPlayerConfigData.incognitoMode && isPlaying) {
            if (currentPosition - lastHeartbeatPosition >= 15_000) {
                lastHeartbeatPosition = currentPosition
                sendHeartbeat()
            }
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
                clock = Triple(hour, minute, second)
            }
        )
        onDispose {
            sendHeartbeat()
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
            showBackToHistory = showBackToHistory,
            hasStartedPlaybackOnce = hasStartedPlaybackOnce
        ),
        LocalVideoPlayerDebugInfoData provides VideoPlayerDebugInfoData(
            debugInfo = videoPlayer.debugInfo
        ),
    ) {
        BvPlayerController(
            modifier = modifier,
            isFullScreen = isFullScreen,
            controlsEnabled = controlsEnabled,
            onEnterFullScreen = onEnterFullScreen,
            onExitFullScreen = onExitFullScreen,
            onBack = onBack,
            onPlay = onPlay@{
                val shouldRequestManualPlayback =
                    !videoPlayerConfigData.autoPlay && !isLive && !hasStartedPlaybackOnce
                if (!hasStartedPlaybackOnce) {
                    pendingDanmakuPlaySyncPosition = if (initialPlaybackPositionMs > 0L) {
                        initialPlaybackPositionMs
                    } else {
                        videoPlayer.currentPosition
                    }
                    hasStartedPlaybackOnce = true
                }
                if (shouldRequestManualPlayback && onRequestManualPlayback()) {
                    isBuffering = true
                    return@onPlay
                }
                videoPlayer.start()
            },
            onPause = {
                if (isLive) {
                    videoPlayer.start()
                    mLiveDanmakuPlayer?.play()
                } else {
                    videoPlayer.pause()
                }
            },
            onSeekToPosition = { position ->
                syncProcessedSponsorSegmentsForPosition(position)
                if (!isPlaying) pendingDanmakuPlaySyncPosition = position
                onReloadDanmakuAfterSeek(position, isPlaying)
                videoPlayer.seekTo(position)
            },
            onChangeResolution = {
                val currentTime = currentPosition
                pendingDanmakuPlaySyncPosition = currentTime
                onChangeResolution(it) {
                    withContext(Dispatchers.Main) {
                        if (videoPlayerConfigData.autoPlay || isLive || hasStartedPlaybackOnce) {
                            videoPlayer.seekTo(currentTime)
                            videoPlayer.start()
                        }
                    }
                }
            },
            onChangeVideoCodec = {
                val currentTime = currentPosition
                pendingDanmakuPlaySyncPosition = currentTime
                onChangeVideoCodec(it) {
                    withContext(Dispatchers.Main) {
                        if (videoPlayerConfigData.autoPlay || isLive || hasStartedPlaybackOnce) {
                            videoPlayer.seekTo(currentTime)
                            videoPlayer.start()
                        }
                    }
                }
            },
            onChangeLiveQuality = onChangeLiveQuality,
            onChangeLiveCodec = onChangeLiveCodec,
            onChangeLiveLine = onChangeLiveLine,
            onChangeAudio = {
                val currentTime = currentPosition
                pendingDanmakuPlaySyncPosition = currentTime
                onChangeAudio(it) {
                    withContext(Dispatchers.Main) {
                        if (videoPlayerConfigData.autoPlay || isLive || hasStartedPlaybackOnce) {
                            videoPlayer.seekTo(currentTime)
                            videoPlayer.start()
                        }
                    }
                }
            },
            onChangeSpeed = { speed ->
                currentPlaySpeed = speed
                onChangeSpeed(speed)
                videoPlayer.speed = speed
                applyDanmakuSpeedPolicy()
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
            onDanmakuSpeedModeChange = { mode ->
                onDanmakuSpeedModeChange(mode)
                applyDanmakuSpeedPolicy()
            },
            onDanmakuPresentationSpeedChange = { speed ->
                onDanmakuPresentationSpeedChange(DanmakuSpeedPolicy.sanitizePresentationSpeed(speed))
                applyDanmakuSpeedPolicy()
            },
            onDanmakuMergeChange = onDanmakuMergeChange,
            onDanmakuFilterLevelChange = onDanmakuFilterLevelChange,
            onPlayModeChange = onPlayModeChange,
            onPlayNewVideo = {
                sendHeartbeat()
                onLoadNewVideo(it)
            },
            showSponsorBlockTip = showSponsorBlockTip,
            showAutoSkipSponsorTip = showAutoSkipSponsorTip,
            autoSkipSponsorSeconds = autoSkipSponsorSeconds,
            currentSponsorSegment = currentSponsorSegment,
            onSkipSponsorSegment = {
                logger.info { "Skip sponsor segment" }
                skipSponsorSegment(currentSponsorSegment)
            },
            onDismissSponsorBlockTip = currentOnDismissSponsorBlockTip
        ) {
            BvVideoPlayer(
                modifier = Modifier
                    .aspectRatio(aspectRatio)
                    .align(Alignment.Center),
                videoPlayer = videoPlayer, playerListener = videoPlayerListener
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .danmakuMaskBitmap(
                        bitmap = currentDanmakuMaskBitmap.takeIf { videoPlayerConfigData.currentDanmakuMask },
                        videoAspectRatio = aspectRatio
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(videoPlayerConfigData.currentDanmakuArea)
                ) {
                    AkDanmakuPlayer(
                        modifier = Modifier.fillMaxSize(),
                        danmakuPlayer = mDanmakuPlayer,
                        visible = !isLive || isFullScreen,
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
}
