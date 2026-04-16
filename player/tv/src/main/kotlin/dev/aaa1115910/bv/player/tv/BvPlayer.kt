package dev.aaa1115910.bv.player.tv

import android.graphics.Bitmap
import android.os.CountDownTimer
import android.util.LruCache
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
import androidx.compose.ui.graphics.Color
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
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
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
import dev.aaa1115910.bv.player.entity.PlaybackMediaMode
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
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.player.tv.controller.SkipEdTip
import dev.aaa1115910.bv.player.tv.controller.SkipOpTip
import dev.aaa1115910.bv.player.tv.controller.VideoPlayerController
import dev.aaa1115910.bv.util.countDownTimer
import dev.aaa1115910.bv.player.util.DanmakuMaskFinder
import dev.aaa1115910.bv.player.util.renderMaskFrameToBitmap
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.requestFocus
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val DANMAKU_MASK_BITMAP_CACHE_MAX_BYTES = 64 * 1024 * 1024

private fun Bitmap.safeCacheSize(): Int = if (isRecycled) 0 else byteCount

private fun Bitmap.safeRecycle() {
    if (!isRecycled) {
        recycle()
    }
}

@Composable
fun BvPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
    danmakuPlayer: DanmakuPlayer?,
    danmakuOpacity: Float,
    playerSeekForwardStep: Int = 10,
    playerSeekBackwardStep: Int = 5,
    showBottomProgressBar: Boolean = false,
    bottomProgressBarColor: Color = Color(0xFFBD26B8).copy(alpha = 0.5f),
    useTextureViewFixPortraitVideo: Boolean = false,
    onSendHeartbeat: suspend (Int) -> Unit,
    onClearBackToHistoryData: () -> Unit,
    onReloadDanmakuAfterSeek: (Long, Boolean) -> Unit = { _, _ -> },
    onLoadNextVideo: (Boolean) -> Unit,
    onLoadPrevVideo: () -> Unit = {},
    onExit: () -> Unit,
    onLoadNewVideo: (VideoListItem) -> Unit,
    onResolutionChange: (Resolution, afterChange: suspend () -> Unit) -> Unit,
    onCodecChange: (VideoCodec, afterChange: suspend () -> Unit) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onAudioChange: (Audio, afterChange: suspend () -> Unit) -> Unit,
    onPlaybackMediaModeChange: (PlaybackMediaMode, Long, afterChange: suspend () -> Unit) -> Unit,
    onLiveQualityChange: (Int) -> Unit = {},
    onLiveCodecChange: (LiveCodec) -> Unit = {},
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onDanmakuMergeChange: (Boolean) -> Unit = {},
    onDanmakuFilterLevelChange: (Int) -> Unit = {},
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onToggleRelatedVideos: (Boolean) -> Unit = {},
    autoOpenPlayListOnVideoEnd: Boolean = false,
    onOpenUpSpace: () -> Unit = {},
    onShowDanmakuChange: (Boolean) -> Unit = {},
    onLoopPlayModeChange: (Boolean) -> Unit = {},
    onRefreshVideo: () -> Unit = {},
    onLiveRetry: () -> Unit = {},
    onInfoVisibilityChanged: (Boolean) -> Unit = {},
    onShowComment: () -> Unit = {},
    onShowDescription: () -> Unit = {},
    onTripleLike: () -> Unit = {},
    useTripleLikeOnLongPress: Boolean = false,
    onToggleFollow: () -> Unit = {},
    onReportLiveHistory: () -> Unit = {},
    liveIncognitoMode: Boolean = false,
    isLive: Boolean = false,
    onLiveDanmakuPlayerReady: ((com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer) -> Unit)? = null,

    // SponsorBlock 相关参数
    enableSponsorBlock: Boolean = false,
    sponsorBlockSkipMode: SponsorBlockSkipMode = SponsorBlockSkipMode.Manual,
    sponsorSegments: List<dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment> = emptyList(),
    showSponsorBlockTip: Boolean = false,
    currentSponsorSegment: dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment? = null,
    onShowSponsorBlockTip: (dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment) -> Unit = {},
    onSkipSponsorSegment: (dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment?) -> Unit = {},
    onDismissSponsorBlockTip: () -> Unit = {},

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
    // 直播弹幕播放器引用，用于同步配置更新
    var mLiveDanmakuPlayer: com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer? by remember { mutableStateOf(null) }

    // 统一更新所有弹幕播放器配置的辅助函数
    val updateAllDanmakuPlayerConfig: (DanmakuConfig) -> Unit = { config ->
        mDanmakuPlayer?.updateConfig(config)
        mLiveDanmakuPlayer?.updateConfig(config)
    }

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
    var openPlayListRequestToken by remember { mutableLongStateOf(0L) }
    
    var pendingDanmakuPosition by remember { mutableLongStateOf(-1L) }
    
    var danmakuNeedsResume by remember { mutableStateOf(false) }

    var pendingSeekDanmakuPosition by remember { mutableLongStateOf(-1L) }

    var pendingSeekDanmakuShouldPlay by remember { mutableStateOf(false) }
    var pendingReloadDanmakuPosition by remember { mutableLongStateOf(-1L) }
    var pendingReloadDanmakuShouldPlay by remember { mutableStateOf(false) }
    var hasStartedPlaybackOnce by remember(videoPlayerConfigData.currentVideoCid) { mutableStateOf(false) }
    val initialHistoryPositionSnapshot = remember(videoPlayerConfigData.currentVideoCid) {
        videoPlayerHistoryData.lastPlayed.toLong().coerceAtLeast(0L)
    }
    var pendingInitialHistoryDanmakuReload by remember(videoPlayerConfigData.currentVideoCid) {
        mutableStateOf(
            videoPlayerConfigData.enableStartPositionSwitch && initialHistoryPositionSnapshot > 0L
        )
    }
    var initialHistoryDanmakuPosition by remember(videoPlayerConfigData.currentVideoCid) {
        mutableLongStateOf(initialHistoryPositionSnapshot)
    }
    
    var lastDanmakuSeekTime by remember { mutableLongStateOf(0L) }
    var defaultAspectRatio by remember { mutableFloatStateOf(16 / 9f) }
    var lastHeartbeatPosition by remember { mutableLongStateOf(0L) }
    var showInfoProvider: () -> Boolean by remember { mutableStateOf({ false }) }
    var controllerInteractionProvider: () -> Boolean by remember { mutableStateOf({ false }) }

    val clockState = remember { VideoPlayerClockState() }

    var hideLogsTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var clockRefreshTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var hideBackToHistoryTimer: CountDownTimer? by remember { mutableStateOf(null) }

    var currentDanmakuMaskFrame: DanmakuMaskFrame? by remember { mutableStateOf(null) }
    var currentDanmakuMaskBitmap: Bitmap? by remember { mutableStateOf(null) }
    var danmakuMaskGeneration by remember { mutableLongStateOf(0L) }
    var isDanmakuMaskDisposed by remember { mutableStateOf(false) }

    // 预渲染的蒙版 Bitmap 缓存
    val danmakuMaskBitmapCache = remember {
        object : LruCache<DanmakuMaskFrame, Bitmap>(DANMAKU_MASK_BITMAP_CACHE_MAX_BYTES) {
            override fun sizeOf(key: DanmakuMaskFrame, value: Bitmap): Int {
                return value.safeCacheSize()
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: DanmakuMaskFrame,
                oldValue: Bitmap,
                newValue: Bitmap?
            ) {
                // 当缓存条目被移除时，释放 Bitmap
                if (evicted) {
                    oldValue.safeRecycle()
                }
            }
        }
    }

    // 跳过片头片尾相关状态
    var showSkipOpTip by remember { mutableStateOf(false) }
    var showSkipEdTip by remember { mutableStateOf(false) }
    var skipOpTipText by remember { mutableStateOf("即将跳过片头") }
    var skipEdTipText by remember { mutableStateOf("即将跳过片尾") }
    var processedClipIndices by remember { mutableStateOf(setOf<Int>()) }
    var processedSponsorSegments by remember { mutableStateOf(setOf<SponsorSegment>()) }

    // 使用 rememberUpdatedState 来跟踪 clipInfoList 和 skipPgcIntroOutro 的最新值
    // 这样可以在非 Composable 上下文（定时器回调）中读取到最新值
    val currentClipInfoList by rememberUpdatedState(videoPlayerConfigData.clipInfoList)
    val currentSkipPgcIntroOutro by rememberUpdatedState(videoPlayerConfigData.skipPgcIntroOutro)
    val currentEnableSponsorBlock by rememberUpdatedState(enableSponsorBlock)
    val currentSponsorBlockSkipMode by rememberUpdatedState(sponsorBlockSkipMode)
    val currentSponsorSegments by rememberUpdatedState(sponsorSegments)
    val currentShowSponsorBlockTip by rememberUpdatedState(showSponsorBlockTip)
    val currentSponsorSegment by rememberUpdatedState(currentSponsorSegment)
    val currentOnShowSponsorBlockTip by rememberUpdatedState(onShowSponsorBlockTip)
    val currentOnSkipSponsorSegment by rememberUpdatedState(onSkipSponsorSegment)
    val currentDanmakuMasks by rememberUpdatedState(videoPlayerDanmakuMaskData.danmakuMasks)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentDanmakuMaskEnabled by rememberUpdatedState(videoPlayerConfigData.currentDanmakuMask)
    val currentIsLive by rememberUpdatedState(videoPlayerConfigData.isLive)
    val currentVideoCid by rememberUpdatedState(videoPlayerConfigData.currentVideoCid)

    // 独立弹幕层句柄（Stable），父级重组频率降低
    val danmakuLayerHandle = remember { DanmakuLayerHandle(initialIsLiveMode = isLive) }

    val clearDanmakuMaskState: (Boolean) -> Unit = { clearCache ->
        danmakuMaskGeneration++
        currentDanmakuMaskFrame = null
        currentDanmakuMaskBitmap = null
        danmakuLayerHandle.update(mask = null, bitmap = null)
        if (clearCache) {
            danmakuMaskBitmapCache.evictAll()
        }
    }

    // 当 clipInfoList 变化时，重置已处理的 clip 索引
    // 这确保了切换到新视频时，跳过片头/片尾功能能够正常工作
    LaunchedEffect(videoPlayerConfigData.clipInfoList) {
        processedClipIndices = emptySet()
    }

    LaunchedEffect(sponsorSegments) {
        processedSponsorSegments = emptySet()
    }

    val syncProcessedSponsorSegmentsForPosition: (Long) -> Unit = { targetPosition ->
        processedSponsorSegments = currentSponsorSegments
            .filterTo(mutableSetOf()) { it.endTime <= targetPosition }
    }

    val scheduleDanmakuSeekSync: (Long, Boolean) -> Unit = { targetPosition, shouldPlayAfterSeek ->
        pendingSeekDanmakuPosition = targetPosition
        pendingSeekDanmakuShouldPlay = shouldPlayAfterSeek
        mDanmakuPlayer?.pause()
        syncProcessedSponsorSegmentsForPosition(targetPosition)
    }

    val scheduleDanmakuReload: (Long, Boolean) -> Unit = { targetPosition, shouldPlayAfterSeek ->
        pendingReloadDanmakuPosition = targetPosition
        pendingReloadDanmakuShouldPlay = shouldPlayAfterSeek
        onReloadDanmakuAfterSeek(targetPosition, shouldPlayAfterSeek)
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
                                scheduleDanmakuSeekSync(clipInfo.end * 1000L, true)
                                videoPlayer.seekTo(clipInfo.end * 1000L)
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
                                scheduleDanmakuSeekSync(clipInfo.end * 1000L, true)
                                videoPlayer.seekTo(clipInfo.end * 1000L)
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

    val updateDanmakuMaskForPosition: suspend (Long) -> Unit = update@{ position ->
        if (isDanmakuMaskDisposed) return@update

        val expectedGeneration = danmakuMaskGeneration
        val expectedVideoCid = currentVideoCid
        val maskFrame = DanmakuMaskFinder.findMaskFrame(currentDanmakuMasks, position)

        // 只有找到新帧时才更新，null 时保持当前蒙版（避免帧空隙导致蒙版闪烁）
        if (maskFrame != null && currentDanmakuMaskFrame != maskFrame) {
            val cachedBitmap = danmakuMaskBitmapCache.get(maskFrame)?.also { bitmap ->
                if (bitmap.isRecycled) {
                    danmakuMaskBitmapCache.remove(maskFrame)
                }
            }?.takeUnless { it.isRecycled }

            // 优先使用缓存中的 Bitmap，避免实时渲染
            val bitmap = cachedBitmap ?: run {
                // 缓存未命中，实时渲染并缓存
                val renderedBitmap = withContext(Dispatchers.Default) {
                    renderMaskFrameToBitmap(maskFrame)
                }

                if (
                    isDanmakuMaskDisposed ||
                    expectedGeneration != danmakuMaskGeneration ||
                    expectedVideoCid != currentVideoCid ||
                    renderedBitmap.isRecycled
                ) {
                    renderedBitmap.safeRecycle()
                    return@update
                }

                danmakuMaskBitmapCache.put(maskFrame, renderedBitmap)
                renderedBitmap
            }

            if (
                isDanmakuMaskDisposed ||
                expectedGeneration != danmakuMaskGeneration ||
                expectedVideoCid != currentVideoCid ||
                controllerInteractionProvider() ||
                bitmap.isRecycled
            ) {
                return@update
            }

            withContext(Dispatchers.Main) {
                if (
                    isDanmakuMaskDisposed ||
                    expectedGeneration != danmakuMaskGeneration ||
                    expectedVideoCid != currentVideoCid ||
                    controllerInteractionProvider() ||
                    bitmap.isRecycled
                ) {
                    return@withContext
                }
                currentDanmakuMaskFrame = maskFrame
                currentDanmakuMaskBitmap = bitmap
            }
        }
        // 如果 maskFrame 为 null（帧空隙），不做任何操作，保持当前蒙版
    }

    // 设置直播弹幕回调，保存引用并同步配置
    LaunchedEffect(onLiveDanmakuPlayerReady) {
        danmakuLayerHandle.updateOnLiveDanmakuPlayerReady { livePlayer ->
            mLiveDanmakuPlayer = livePlayer
            // 初始化时应用当前配置
            livePlayer.updateConfig(danmakuConfig)
            onLiveDanmakuPlayerReady?.invoke(livePlayer)
        }
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

    val buildDanmakuConfig: (Boolean) -> DanmakuConfig = { syncTypeFilter ->
        if (syncTypeFilter) {
            updateEnabledDanmakuTypeFilter(videoPlayerConfigData.currentDanmakuEnabledList)
        }
        danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI,
            textSizeScale = videoPlayerConfigData.currentDanmakuScale,
            dataFilter = listOf(typeFilter),
            visibility = videoPlayerConfigData.showDanmaku,
            alpha = videoPlayerConfigData.currentDanmakuOpacity,
            screenPart = videoPlayerConfigData.currentDanmakuArea.coerceIn(0f, 1f),
            liveMode = videoPlayerConfigData.isLive,
            maxLiveScreenDanmakuCount = 100,
            liveMaxPendingCount = 200,
            liveMergeCache = true
        )
    }

    val applyDanmakuConfig: (String, Boolean, Boolean, Boolean) -> Unit = {
            reason,
            syncTypeFilter,
            markFilterChanged,
            markVisibilityChanged,
        ->
        danmakuConfig = buildDanmakuConfig(syncTypeFilter)
        if (markFilterChanged) {
            danmakuConfig.updateFilter()
        }
        if (markVisibilityChanged) {
            danmakuConfig.updateVisibility()
        }
        logger.info { "$reason (liveMode=${videoPlayerConfigData.isLive}): $danmakuConfig" }
        updateAllDanmakuPlayerConfig(danmakuConfig)
        danmakuLayerHandle.updateIsLiveMode(isLive)
    }

    val initDanmakuConfig: () -> Unit = {
        applyDanmakuConfig(
            "Init danmaku config",
            true,
            true,
            false
        )
    }

    val updateDanmakuConfigTypeFilter: () -> Unit = {
        applyDanmakuConfig(
            "Update danmaku type filters: ${typeFilter.filterSet}",
            true,
            true,
            false
        )
    }

    val updateDanmakuConfig: () -> Unit = {
        applyDanmakuConfig(
            "Update danmaku config",
            false,
            false,
            false
        )
    }

    val updateVideoAspectRatio: () -> Unit = {
        aspectRatioValue = when (currentVideoAspectRatio) {
            VideoAspectRatio.Default -> defaultAspectRatio
            VideoAspectRatio.FourToThree -> 4 / 3f
            VideoAspectRatio.SixteenToNine -> 16 / 9f
        }
        logger.info { "Update video player aspectRatio: $aspectRatioValue" }
    }

    val sendHeartbeat: () -> Unit = sendHeartbeat@{
        if (videoPlayerConfigData.isLive) return@sendHeartbeat
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

    LaunchedEffect(videoPlayerConfigData.currentVideoCid, videoPlayerHistoryData.lastPlayed) {
        val historyPosition = videoPlayerHistoryData.lastPlayed.toLong().coerceAtLeast(0L)
        if (historyPosition > 0L && initialHistoryDanmakuPosition <= 0L) {
            initialHistoryDanmakuPosition = historyPosition
            pendingInitialHistoryDanmakuReload = videoPlayerConfigData.enableStartPositionSwitch
        }
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
        if (videoPlayerConfigData.enableStartPositionSwitch && lastPlayed > 0 && hideBackToHistoryTimer == null) {
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

                if (videoPlayerConfigData.isLive) {
                    logger.info { "Live playback ignores currentPlaySpeed=$currentPlaySpeed" }
                    videoPlayer.speed = 1f
                } else {
                    // Apply the current session playback speed to a newly prepared player.
                    onPlaySpeedChange(currentPlaySpeed)
                    logger.info { "Reset default play speed: $currentPlaySpeed" }
                    videoPlayer.speed = currentPlaySpeed
                    mDanmakuPlayer?.updatePlaySpeed(currentPlaySpeed)
                }

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
                hasStartedPlaybackOnce = true
                val wasPlaying = isPlaying
                isPlaying = true
                isBuffering = false
                val currentTime = System.currentTimeMillis()

                if (pendingSeekDanmakuPosition >= 0) {
                    logger.info {
                        "onPlay: defer danmaku sync until onSeeked, pending=${pendingSeekDanmakuPosition.formatHourMinSec()}"
                    }
                    return@launch
                }

                if (pendingInitialHistoryDanmakuReload && initialHistoryDanmakuPosition > 0L) {
                    logger.info {
                        "onPlay: defer initial history danmaku rebuild until progress reaches ${initialHistoryDanmakuPosition.formatHourMinSec()}"
                    }
                    updateBackToHistory()
                    return@launch
                }

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
            if (pendingSeekDanmakuPosition >= 0) {
                pendingSeekDanmakuShouldPlay = false
            }
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
                    scheduleDanmakuSeekSync(0, true)
                    videoPlayer.seekTo(0)
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
                    if (autoOpenPlayListOnVideoEnd) {
                        openPlayListRequestToken = System.currentTimeMillis()
                    } else {
                        onLoadNextVideo(false)
                    }
                } else {
                    logger.info { "Skip auto next because info panel visible" }
                }
            }
        }

        override fun onIdle() {
            //TODO("Not yet implemented")
        }

        override fun onSeekBack(seekBackIncrementMs: Long) {
            scheduleDanmakuSeekSync(seekState.position, isPlaying)
        }

        override fun onSeekForward(seekForwardIncrementMs: Long) {
            scheduleDanmakuSeekSync(seekState.position, isPlaying)
        }

        override fun onSeeked(position: Long) {
            logger.info { "onSeeked: ${position.formatHourMinSec()}" }
            val syncPosition = pendingSeekDanmakuPosition.takeIf { it >= 0 } ?: position
            val shouldPlayAfterSeek = if (pendingSeekDanmakuPosition >= 0) {
                pendingSeekDanmakuShouldPlay
            } else {
                isPlaying
            }
            pendingSeekDanmakuPosition = -1L
            pendingSeekDanmakuShouldPlay = false

            val isInitialHistorySeek =
                pendingInitialHistoryDanmakuReload &&
                    initialHistoryDanmakuPosition > 0L &&
                    kotlin.math.abs(syncPosition - initialHistoryDanmakuPosition) <= 1_500L
            val isStartupSeekBeforePlay =
                !hasStartedPlaybackOnce &&
                    syncPosition > 0L &&
                    pendingSeekDanmakuPosition < 0L
            if (isStartupSeekBeforePlay) {
                initialHistoryDanmakuPosition = syncPosition
                pendingInitialHistoryDanmakuReload = true
                logger.info {
                    "onSeeked: treat startup seek as initial history seek, defer danmaku rebuild until playback progresses to ${syncPosition.formatHourMinSec()}"
                }
                lastDanmakuSeekTime = System.currentTimeMillis()
                lastHeartbeatPosition = syncPosition
                return
            }
            if (isInitialHistorySeek) {
                logger.info {
                    "onSeeked: detected initial history seek, defer danmaku rebuild until playback progresses to ${initialHistoryDanmakuPosition.formatHourMinSec()}"
                }
                lastDanmakuSeekTime = System.currentTimeMillis()
                lastHeartbeatPosition = syncPosition
                return
            }

            scheduleDanmakuReload(syncPosition, shouldPlayAfterSeek)
            lastDanmakuSeekTime = System.currentTimeMillis()
            lastHeartbeatPosition = syncPosition
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
            if (videoPlayerConfigData.isLive) return

            scope.launch(Dispatchers.Main.immediate) {
                val pos = position.coerceAtLeast(0L)
                val dur = duration.coerceAtLeast(0L)
                val buf = buffered.coerceIn(0, 100)

                if (seekState.position != pos) seekState.position = pos
                if (seekState.duration != dur) seekState.duration = dur
                if (seekState.bufferedPercentage != buf) seekState.bufferedPercentage = buf

                if (pendingInitialHistoryDanmakuReload && isPlaying && initialHistoryDanmakuPosition > 0L) {
                    val triggerPosition = (initialHistoryDanmakuPosition - 1_500L).coerceAtLeast(0L)
                    if (pos >= triggerPosition) {
                        pendingInitialHistoryDanmakuReload = false
                        logger.info {
                            "onProgress: rebuild danmaku after initial history seek, current=${pos.formatHourMinSec()}, target=${initialHistoryDanmakuPosition.formatHourMinSec()}"
                        }
                        scheduleDanmakuReload(initialHistoryDanmakuPosition, true)
                        return@launch
                    }
                }

                if (currentSkipPgcIntroOutro && currentClipInfoList.isNotEmpty() && isPlaying) {
                    checkSkipTask(pos)
                }

                // SponsorBlock 片段检测
                if (currentEnableSponsorBlock && currentSponsorSegments.isNotEmpty() && isPlaying && !currentShowSponsorBlockTip) {
                    val thresholdMs = 0  // 提前 0 秒开始提示
                    val segment = currentSponsorSegments.firstOrNull {
                        pos >= (it.startTime + thresholdMs) && pos < it.endTime
                    }
                    if (segment != null && segment !in processedSponsorSegments) {
                        processedSponsorSegments = processedSponsorSegments + segment
                        logger.info {
                            "SponsorBlock segment matched at ${pos}ms, range=${segment.startTime}-${segment.endTime}, mode=$currentSponsorBlockSkipMode, firstHit=true"
                        }
                        if (currentSponsorBlockSkipMode == SponsorBlockSkipMode.Auto) {
                            // 自动
                            currentOnSkipSponsorSegment(segment)
                        } else {
                            // 手动
                            currentOnShowSponsorBlockTip(segment)
                        }
                    }
                }

                // 蒙版更新已移至独立定时器

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

    // 独立定时器：高频更新蒙版（每 33ms，约 30fps）
    // 直播模式下不启用蒙版
    LaunchedEffect(Unit) {
        while (true) {
            if (isDanmakuMaskDisposed) break

            if (
                !currentIsLive &&
                currentDanmakuMasks.isNotEmpty() &&
                currentIsPlaying &&
                currentDanmakuMaskEnabled &&
                !controllerInteractionProvider()
            ) {
                val position = runCatching { videoPlayer.currentPosition }.getOrElse {
                    logger.warn(it) { "Read currentPosition for danmaku mask failed" }
                    -1L
                }
                if (position >= 0) {
                    updateDanmakuMaskForPosition(position)
                }
            }
            delay(33)
        }
    }

    // 当蒙版数据变化时，清除旧缓存
    LaunchedEffect(videoPlayerConfigData.currentVideoCid, videoPlayerDanmakuMaskData.danmakuMasks.size) {
        if (!isDanmakuMaskDisposed) {
            clearDanmakuMaskState(true)
        }
    }

    LaunchedEffect(videoPlayerConfigData.isLoop, videoPlayerConfigData.showDanmaku) {
        videoPlayer.setPlayerEventListener(videoPlayerListener)
    }

    LaunchedEffect(danmakuPlayer) {
        mDanmakuPlayer = danmakuPlayer
        danmakuLayerHandle.updateDanmakuPlayer(danmakuPlayer)
        applyDanmakuConfig(
            "Apply danmaku config to new player",
            true,
            true,
            false
        )
        val reloadPosition = pendingReloadDanmakuPosition.takeIf { it >= 0L }
        if (reloadPosition != null) {
            mDanmakuPlayer?.seekTo(reloadPosition)
            if (pendingReloadDanmakuShouldPlay) {
                mDanmakuPlayer?.start()
            } else {
                mDanmakuPlayer?.pause()
            }
            lastDanmakuSeekTime = System.currentTimeMillis()
            lastHeartbeatPosition = reloadPosition
            logger.info {
                "Resume rebuilt danmaku player at ${reloadPosition.formatHourMinSec()}, shouldPlay=$pendingReloadDanmakuShouldPlay"
            }
            pendingReloadDanmakuPosition = -1L
            pendingReloadDanmakuShouldPlay = false
        }
    }

    // Sync currentDanmakuArea -> danmakuConfig.screenPart
    LaunchedEffect(videoPlayerConfigData.currentDanmakuArea) {
        applyDanmakuConfig(
            "Sync danmaku screenPart",
            false,
            false,
            false
        )
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
            isDanmakuMaskDisposed = true

            // 在释放播放器前发送心跳，确保退出时进度被正确记录
            if (!videoPlayerConfigData.incognitoMode && !videoPlayerConfigData.isLive) {
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

            clearDanmakuMaskState(true)

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
            bottomProgressBarColor = bottomProgressBarColor,
            showRelatedVideos = videoPlayerConfigData.showRelatedVideos,
            onToggleRelatedVideos = onToggleRelatedVideos,
            registerShowInfoProvider = { provider -> showInfoProvider = provider },
            registerControllerInteractionProvider = { provider -> controllerInteractionProvider = provider },
            onInfoVisibilityChanged = onInfoVisibilityChanged,

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
                scheduleDanmakuSeekSync(it, isPlaying)
                videoPlayer.seekTo(it)
            },
            onSeekToVideoEnd = {
                mDanmakuPlayer?.pause()
                scope.launch(Dispatchers.Main) {
                    isPlaying = false
                    if (!videoPlayerConfigData.incognitoMode) sendHeartbeat()
                    if (autoOpenPlayListOnVideoEnd) {
                        openPlayListRequestToken = System.currentTimeMillis()
                    } else {
                        onLoadNextVideo(true)
                    }
                }
            },
            onBackToHistory = {
                val time = if (videoPlayerConfigData.defaultStartPosition == DefaultStartPosition.History) {
                    0L
                } else {
                    videoPlayerHistoryData.lastPlayed.toLong()
                }
                logger.fInfo { "Back to history/beginning: ${time.formatHourMinSec()}" }
                scheduleDanmakuSeekSync(time, isPlaying)
                videoPlayer.seekTo(time)
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
                if (videoPlayerConfigData.isLive) {
                    logger.info { "Ignore playback speed change in live mode: $speed" }
                    return@VideoPlayerController
                }
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
            onPlaybackMediaModeChange = { mediaMode ->
                videoPlayer.pause()
                val current = seekState.position.takeIf { it > 0L } ?: videoPlayer.currentPosition
                pendingDanmakuPosition = current
                danmakuNeedsResume = true
                videoPlayer.setInitialSeekPosition(current)
                onPlaybackMediaModeChange(mediaMode, current) {
                    withContext(Dispatchers.Main) {
                        if (current > 0L) {
                            videoPlayer.seekTo(current)
                        }
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
                applyDanmakuConfig(
                    "Update danmaku opacity",
                    false,
                    false,
                    false
                )
            },
            onDanmakuAreaChange = { area ->
                logger.info { "On danmaku area change: $area" }
                onDanmakuAreaChange(area)
            },
            onDanmakuMaskChange = { mask ->
                logger.info { "On danmaku mask change: $mask" }
                onDanmakuMaskChange(mask)
            },
            onDanmakuMergeChange = { enabled ->
                logger.info { "On danmaku merge change: $enabled" }
                onDanmakuMergeChange(enabled)
            },
            onDanmakuFilterLevelChange = { filterLevel ->
                logger.info { "On danmaku filter level change: $filterLevel" }
                onDanmakuFilterLevelChange(filterLevel)
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
                applyDanmakuConfig(
                    "Update danmaku visibility",
                    false,
                    false,
                    true
                )
            },
            onHideDanmaku = {
                onShowDanmakuChange(false)
                videoPlayerConfigData.showDanmaku = false
                applyDanmakuConfig(
                    "Update danmaku visibility",
                    false,
                    false,
                    true
                )
            },
            onLoopPlayModeChange = {
                videoPlayerConfigData.isLoop = it
                onLoopPlayModeChange(it)
            },
            userActionContent = userActionContent,
            onLoadPrevVideo = onLoadPrevVideo,
            onLoadNextVideo = onLoadNextVideo,
            openPlayListRequestToken = openPlayListRequestToken,
            onShowComment = onShowComment,
            onShowDescription = onShowDescription,
            onTripleLike = onTripleLike,
            useTripleLikeOnLongPress = useTripleLikeOnLongPress,
            onToggleFollow = onToggleFollow,
            onReportLiveHistory = onReportLiveHistory,
            liveIncognitoMode = liveIncognitoMode,

            // SponsorBlock 相关参数
            enableSponsorBlock = enableSponsorBlock,
            sponsorSegments = sponsorSegments,
            showSponsorBlockTip = showSponsorBlockTip,
            currentSponsorSegment = currentSponsorSegment,
            onSkipSponsorSegment = {
                logger.info { "Skip sponsor segment" }
                onSkipSponsorSegment(currentSponsorSegment)
            },
            onDismissSponsorBlockTip = onDismissSponsorBlockTip
        ) {
            LaunchedEffect(Unit) {
                videoPlayer.setOptions()
            }

            // 将弹幕层副作用独立到子树，保证父级其它状态变化不导致 handle 以外的重组
            DanmakuLayerSideEffects(
                danmakuLayerHandle = danmakuLayerHandle,
                visible = videoPlayerConfigData.showDanmaku,
                maskFrame = currentDanmakuMaskFrame.takeIf { videoPlayerConfigData.currentDanmakuMask },
                maskBitmap = currentDanmakuMaskBitmap.takeIf { videoPlayerConfigData.currentDanmakuMask },
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
    maskBitmap: Bitmap?,
    videoAspectRatio: Float,
) {
    LaunchedEffect(visible, maskFrame, maskBitmap, videoAspectRatio) {
        danmakuLayerHandle.update(
            mask = maskFrame,
            bitmap = maskBitmap,
            visible = visible,
            videoAspectRatio = videoAspectRatio,
        )
    }
}
