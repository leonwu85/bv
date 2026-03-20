package dev.aaa1115910.bv.player.impl.exo

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.LiveConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.Commands
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.OkHttpUtil
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.util.formatHourMinSec

/**
 * 智能缓冲配置
 */
private data class BufferConfig(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val backBufferMs: Int,
    val targetBufferBytes: Int,
    val prioritizeTime: Boolean // 是否优先考虑时间阈值
)

/**
 * 直播专用缓冲配置
 */
private data class LiveBufferConfig(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,      // 开始播放前的缓冲时间
    val bufferForPlaybackAfterRebufferMs: Int,  // 重新缓冲后的播放缓冲
    val backBufferMs: Int,
    val targetBufferBytes: Int,
    val prioritizeTime: Boolean
)

@OptIn(UnstableApi::class)
class ExoMediaPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer(), Player.Listener {
    var mPlayer: ExoPlayer? = null
    protected var mMediaSource: MediaSource? = null

    // 进度更新 Handler，用于定期触发 onProgress 回调
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            dispatchProgress()
            if (isPlaying) {
                // 播放中，每 500ms 更新一次进度
                progressHandler.postDelayed(this, 500)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private val dataSourceFactory =
        OkHttpDataSource.Factory(
            if (options.isLive) OkHttpUtil.generateLiveOkHttpClient(context)
            else OkHttpUtil.generateCustomSslOkHttpClient(context)
        ).apply {
            options.userAgent?.let { setUserAgent(it) }
            options.referer?.let { setDefaultRequestProperties(mapOf("referer" to it)) }
        }

    init {
        initPlayer()
    }

    @OptIn(UnstableApi::class)
    override fun initPlayer() {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                when (options.enableFfmpegAudioRenderer) {
                    true -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                    false -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                }
            )
            setEnableDecoderFallback(true)
            // 为 API 23-30 启用异步缓冲队列（API 31+ 已默认启用）
            if (options.enableAsyncQueueing && Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 31) {
                Log.d("ExoMediaPlayer", "Force enable asynchronous buffer queueing for API ${Build.VERSION.SDK_INT}")
                @Suppress("UNCHECKED_CAST")
                (this as DefaultRenderersFactory).forceEnableMediaCodecAsynchronousQueueing()
            }
            if (options.enableAudioPlaybackParams) {
                setEnableAudioOutputPlaybackParameters(true)
            }
        }

        // 为直播选择不同的缓冲策略
        val loadControl = if (options.isLive) {
            createLiveLoadControl()
        } else {
            createVodLoadControl()
        }

        val exoPlayerBuilder = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setSeekForwardIncrementMs(1000 * 10)
            .setSeekBackIncrementMs(1000 * 10)

        // 只有在启用隧道模式时才需要自定义 TrackSelector
        if (options.enableTunneling) {
            val trackSelector = DefaultTrackSelector(context).apply {
                parameters = buildUponParameters()
                    .setTunnelingEnabled(true)
                    .build()
            }
            exoPlayerBuilder.setTrackSelector(trackSelector)
        }

        mPlayer = exoPlayerBuilder.build()

        initListener()
    }

    private fun initListener() {
        mPlayer?.addListener(this)
    }

    @OptIn(UnstableApi::class)
    override fun setHeader(headers: Map<String, String>) {

    }

    @OptIn(UnstableApi::class)
    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        val videoMediaSource = videoUrl?.let { createMediaSource(it) }
        val audioMediaSource = audioUrl?.let { createMediaSource(it) }

        val mediaSources = listOfNotNull(videoMediaSource, audioMediaSource)
        mMediaSource = MergingMediaSource(*mediaSources.toTypedArray())
    }

    /**
     * 根据 URL 自动选择合适的 MediaSource
     * - .m3u8 URL 使用 HlsMediaSource（支持 HLS 直播/点播）
     * - 其他 URL 使用 ProgressiveMediaSource（支持 FLV/MP4 等逐行下载）
     */
    @OptIn(UnstableApi::class)
    private fun createMediaSource(url: String): MediaSource {
        val uri = android.net.Uri.parse(url)
        val path = uri.path?.lowercase() ?: ""
        return if (path.endsWith(".m3u8")) {
            createHlsMediaSource(uri)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
        }
    }

    /**
     * 创建 HLS 媒体源
     */
    @OptIn(UnstableApi::class)
    private fun createHlsMediaSource(uri: android.net.Uri): MediaSource {
        val hlsFactory = HlsMediaSource.Factory(dataSourceFactory)

        if (options.isLive) {
            // 直播专用配置
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setLiveConfiguration(
                    LiveConfiguration.Builder()
                        .setTargetOffsetMs(C.TIME_UNSET)  // 自动跟随播放列表
                        .setMinOffsetMs(1000)  // 最小延迟1秒
                        .setMaxOffsetMs(30000)  // 最大延迟30秒
                        .setMinPlaybackSpeed(0.95f)  // 最小播放速度（追帧）
                        .setMaxPlaybackSpeed(1.05f)  // 最大播放速度
                        .build()
                )
                .build()

            Log.d("ExoMediaPlayer", "HLS Live source created with live configuration")
            return hlsFactory.createMediaSource(mediaItem)
        } else {
            // 点播配置
            return hlsFactory.createMediaSource(MediaItem.fromUri(uri))
        }
    }

    @OptIn(UnstableApi::class)
    override fun prepare() {
        mPlayer?.setMediaSource(mMediaSource!!)
        mPlayer?.prepare()
        // 处理初始跳转位置，避免在 onReady 中 seek 导致的状态抖动
        if (pendingSeekPosition > 0) {
            mPlayer?.seekTo(pendingSeekPosition)
            clearPendingSeekPosition()
        }
    }

    override fun start() {
        mPlayer?.play()
    }

    override fun pause() {
        mPlayer?.pause()
    }

    override fun stop() {
        mPlayer?.stop()
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val isPlaying: Boolean
        get() = mPlayer?.isPlaying == true

    override val isSeekable: Boolean
        get() = mPlayer?.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) == true

    override fun seekTo(time: Long) {
        if (!isSeekable) {
            Log.w("ExoMediaPlayer", "Media is not seekable, ignoring seek to ${time}ms")
            return
        }
        mPlayer?.seekTo(time)
        dispatchProgress()
    }

    override fun release() {
        try {
            progressHandler.removeCallbacks(progressUpdateRunnable)
            mPlayer?.release()
            mMediaSource = null
            mPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override val currentPosition: Long
        get() = mPlayer?.currentPosition ?: 0
    override val duration: Long
        get() = mPlayer?.duration ?: 0
    override val bufferedPercentage: Int
        get() = mPlayer?.bufferedPercentage ?: 0

    override fun setOptions() {
        mPlayer?.playWhenReady = true
    }

    override var speed: Float
        get() = mPlayer?.playbackParameters?.speed ?: 1f
        set(value) {
            mPlayer?.setPlaybackSpeed(value)
        }
    override val tcpSpeed: Long
        get() = 0L

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> mPlayerEventListener?.onIdle()
            Player.STATE_BUFFERING -> mPlayerEventListener?.onBuffering()
            Player.STATE_READY -> {
                mPlayerEventListener?.onReady()
                dispatchProgress()
            }
            Player.STATE_ENDED -> mPlayerEventListener?.onEnd()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            mPlayerEventListener?.onPlay()
            // 启动进度更新
            progressHandler.removeCallbacks(progressUpdateRunnable)
            progressHandler.post(progressUpdateRunnable)
        } else {
            mPlayerEventListener?.onPause()
            // 停止进度更新
            progressHandler.removeCallbacks(progressUpdateRunnable)
            dispatchProgress()
        }
    }

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        mPlayerEventListener?.onSeekBack(seekBackIncrementMs)
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        mPlayerEventListener?.onSeekForward(seekForwardIncrementMs)
    }

    override fun onPositionDiscontinuity(
        oldPosition: PositionInfo,
        newPosition: PositionInfo,
        reason: Int
    ) {
        if (reason == DISCONTINUITY_REASON_SEEK) {
            val position = newPosition.positionMs.coerceAtLeast(0L)
            mPlayerEventListener?.onSeeked(position)
            dispatchProgress()
        }
    }

    override val debugInfo: String
        get() {
            return """
                player: ${androidx.media3.common.MediaLibraryInfo.VERSION_SLASHY}
                time: ${currentPosition.formatHourMinSec()} / ${duration.formatHourMinSec()}
                buffered: $bufferedPercentage%
                resolution: ${mPlayer?.videoSize?.width} x ${mPlayer?.videoSize?.height}
                audio: ${mPlayer?.audioFormat?.bitrate ?: 0} kbps
                video codec: ${mPlayer?.videoFormat?.sampleMimeType ?: "null"}
                audio codec: ${mPlayer?.audioFormat?.sampleMimeType ?: "null"} (${getAudioRendererName()})
            """.trimIndent()
        }

    private fun getAudioRendererName(): String {
        val rendererCount = mPlayer?.rendererCount ?: return "UnknownRenderer"
        for (i in 0 until rendererCount) {
            val renderer = mPlayer!!.getRenderer(i)
            if (renderer.trackType == C.TRACK_TYPE_AUDIO && renderer.state == Renderer.STATE_STARTED) {
                return renderer.name
            }
        }
        return "UnknownRenderer"
    }

    override val videoWidth: Int
        get() = mPlayer?.videoSize?.width ?: 0
    override val videoHeight: Int
        get() = mPlayer?.videoSize?.height ?: 0

    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
        mPlayerEventListener?.onVideoSizeChanged(videoSize.width, videoSize.height)
    }

    override fun onPlayerError(error: PlaybackException) {
        mPlayerEventListener?.onError(error)
    }

    /**
     * 直播专用的 LoadControl
     */
    @OptIn(UnstableApi::class)
    private fun createLiveLoadControl(): DefaultLoadControl {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val deviceTier = determineDeviceTier(activityManager, memoryInfo)
        val config = calculateLiveBufferConfig(deviceTier, memoryInfo.availMem)

        Log.d("ExoMediaPlayer", "Live buffer config: minBuffer=${config.minBufferMs}ms, " +
            "maxBuffer=${config.maxBufferMs}ms, bufferForPlayback=${config.bufferForPlaybackMs}ms, " +
            "bufferAfterRebuffer=${config.bufferForPlaybackAfterRebufferMs}ms")

        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                config.minBufferMs,
                config.maxBufferMs,
                config.bufferForPlaybackMs,
                config.bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(config.prioritizeTime)
            .setTargetBufferBytes(config.targetBufferBytes)
            .setBackBuffer(config.backBufferMs, false)
            .build()
    }

    /**
     * 点播用的 LoadControl
     * 保持现有的缓冲逻辑
     */
    @OptIn(UnstableApi::class)
    private fun createVodLoadControl(): DefaultLoadControl {
        val bufferConfig = calculateSmartBufferConfig()
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferConfig.minBufferMs,
                bufferConfig.maxBufferMs,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(bufferConfig.prioritizeTime)
            .setTargetBufferBytes(bufferConfig.targetBufferBytes)
            .setBackBuffer(bufferConfig.backBufferMs, false)
            .build()
    }

    /**
     * 确定设备性能等级
     */
    private fun determineDeviceTier(
        activityManager: android.app.ActivityManager,
        memoryInfo: android.app.ActivityManager.MemoryInfo
    ): DeviceTier {
        val totalMemory = memoryInfo.totalMem
        val isLowRam = activityManager.isLowRamDevice
        return when {
            isLowRam || totalMemory < 3L * 1024 * 1024 * 1024 -> DeviceTier.LOW
            totalMemory < 6L * 1024 * 1024 * 1024 -> DeviceTier.MID
            else -> DeviceTier.HIGH
        }
    }

    /**
     * 直播专用缓冲配置
     */
    private fun calculateLiveBufferConfig(deviceTier: DeviceTier, availableMemory: Long): LiveBufferConfig {
        return when (deviceTier) {
            DeviceTier.LOW -> LiveBufferConfig(
                minBufferMs = 3000,    // 3秒最小缓冲
                maxBufferMs = 8000,    // 8秒最大缓冲
                bufferForPlaybackMs = 1000,  // 1秒快速起播
                bufferForPlaybackAfterRebufferMs = 2000,  // 2秒快速恢复
                backBufferMs = 0,
                targetBufferBytes = calculateBufferSize(availableMemory, 0.06, 3, 30),
                prioritizeTime = true  // 直播优先时间阈值
            )
            DeviceTier.MID -> LiveBufferConfig(
                minBufferMs = 4000,
                maxBufferMs = 10000,
                bufferForPlaybackMs = 1000,
                bufferForPlaybackAfterRebufferMs = 2500,
                backBufferMs = 0,
                targetBufferBytes = calculateBufferSize(availableMemory, 0.10, 5, 80),
                prioritizeTime = true
            )
            DeviceTier.HIGH -> LiveBufferConfig(
                minBufferMs = 5000,
                maxBufferMs = 15000,
                bufferForPlaybackMs = 1500,
                bufferForPlaybackAfterRebufferMs = 3000,
                backBufferMs = 0,
                targetBufferBytes = calculateBufferSize(availableMemory, 0.15, 8, 150),
                prioritizeTime = true
            )
        }
    }

    /**
     * 计算智能缓冲配置
     * 根据设备性能、可用内存和预期视频质量动态调整缓冲策略
     */
    private fun calculateSmartBufferConfig(): BufferConfig {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // 获取当前可用内存（以字节为单位）
        val availableMemory = memoryInfo.availMem
        val totalMemory = memoryInfo.totalMem
        val isLowRam = activityManager.isLowRamDevice

        // 根据设备性能等级调整策略
        val deviceTier = when {
            isLowRam || totalMemory < 3L * 1024 * 1024 * 1024 -> DeviceTier.LOW // 低端设备：小于3GB RAM
            totalMemory < 6L * 1024 * 1024 * 1024 -> DeviceTier.MID // 中端设备：3-6GB RAM
            else -> DeviceTier.HIGH // 高端设备：6GB+ RAM
        }
        return when (deviceTier) {
            DeviceTier.LOW -> BufferConfig(
                minBufferMs = 7000,   // 7秒最小缓冲
                maxBufferMs = 11000,  // 11秒最大缓冲
                backBufferMs = 0, // 0秒回退缓冲
                targetBufferBytes = calculateBufferSize(availableMemory, 0.08, 5, 50), // 8%内存，5-50MB
                prioritizeTime = false // 改为优先大小限制，严格控制内存使用
            )
            DeviceTier.MID -> BufferConfig(
                minBufferMs = 11000,  // 11秒最小缓冲
                maxBufferMs = 16000,  // 16秒最大缓冲
                backBufferMs = 0, // 0秒回退缓冲
                targetBufferBytes = calculateBufferSize(availableMemory, 0.13, 5, 150), // 13%内存，5-150MB
                prioritizeTime = false
            )
            DeviceTier.HIGH -> BufferConfig(
                minBufferMs = 12000,  // 12秒最小缓冲
                maxBufferMs = 22000,  // 22秒最大缓冲
                backBufferMs = 11000, // 11秒回退缓冲
                targetBufferBytes = calculateBufferSize(availableMemory, 0.18, 10, 300), // 18%内存，10-300MB
                prioritizeTime = false
            )
        }
    }

    /**
     * 设备性能等级
     */
    private enum class DeviceTier {
        LOW, MID, HIGH
    }

    /**
     * 计算缓冲区大小
     */
    private fun calculateBufferSize(
        availableMemory: Long,
        memoryRatio: Double,
        minMB: Int,
        maxMB: Int
    ): Int {
        val calculatedSize = (availableMemory * memoryRatio).toLong()
        val minSize = minMB * 1024 * 1024L
        val maxSize = maxMB * 1024 * 1024L

        return when {
            calculatedSize < minSize -> minSize.toInt()
            calculatedSize > maxSize -> maxSize.toInt()
            else -> calculatedSize.toInt()
        }
    }

    /**
     * 统一分发进度信息
     * 类似 VLC 的 dispatchProgress() 方法，用于触发 onProgress 回调
     */
    private fun dispatchProgress() {
        val positionMs = mPlayer?.currentPosition ?: 0L
        val durationMs = mPlayer?.duration ?: 0L
        val buffered = mPlayer?.bufferedPercentage ?: 0
        mPlayerEventListener?.onProgress(positionMs, durationMs.coerceAtLeast(0L), buffered)
    }
}
