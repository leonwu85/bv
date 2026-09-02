package dev.aaa1115910.bv.player.impl.vlc

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.core.BuildConfig
import dev.aaa1115910.bv.player.playbackRefererFor
import dev.aaa1115910.bv.util.formatHourMinSec
import io.github.oshai.kotlinlogging.KotlinLogging
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.concurrent.Executors

/**
 * VLC 播放器实现（libvlc-all 3.6.x）。
 *
 * 线程模型：libvlc-android 的所有事件都在主线程回调，本类的可变状态也只在主线程读写；
 * 唯一的例外是 [release] 中阻塞的 stop/release 被移到后台单线程执行。
 *
 * 状态约定：
 * - [playRequested] 记录上层期望的播放状态（start/pause），seek 完成后据此决定是否恢复播放；
 * - seek 期间内部会暂停 VLC 等待缓冲完成，期间的 `Paused` 事件不会上报，避免 UI 闪烁；
 * - 播放中的重缓冲（VLC 状态仍为 Playing）经防抖后以 onBuffering/onPlay 上报，供上层缓冲恢复策略使用。
 *
 * 画面布局：完全交给 libvlc 的 VideoHelper（SURFACE_BEST_FIT）。VLC 的 vout 以整个 VLCVideoLayout 的尺寸
 * 作为显示区域计算画面位置（gles2 路径直接用它设置 glViewport），因此应用侧绝不能自行改动 VLC 创建的
 * SurfaceView 尺寸：缩小 Surface 会让画面被绘制到 Surface 之外（黑屏）或被拉伸变形。
 */
class VlcMediaPlayer(
    context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer() {
    private val logger = KotlinLogging.logger { }
    private val appContext: Context = context.applicationContext

    var libVlc: LibVLC? = null
        private set
    var mediaPlayer: MediaPlayer? = null
        private set

    private var initializationError: Exception? = null
    private var currentVideoUrl: String? = null
    private var currentAudioUrl: String? = null

    /** 上层期望的播放状态（start()/pause() 记录） */
    private var playRequested = false
    private var readyDispatchedForCurrentMedia = false

    /**
     * VLC 3 在 Init 结束、缓存尚未填满时就进入 Playing 状态；这期间画面停在首帧。
     * 为对齐 Exo 的 BUFFERING → READY 语义，Playing 后直到 Buffering 100% / 首个 TimeChanged 之前仍按缓冲上报。
     */
    private var startupBuffering = false

    private var _isSeekable = true

    /** VLC Buffering 事件给出的缓存填充比例（0..100，相对 network-caching） */
    private var cacheFillPercent = 0
    private var lastKnownPositionMs = 0L
    private var lastKnownDurationMs = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    // ========== seek ==========
    private var isSeeking = false
    private var seekCommandIssued = false
    private var seekGeneration = 0
    private var seekTargetPosition = 0L
    private var seekTimeoutRunnable: Runnable? = null

    // ========== 播放中重缓冲 ==========
    private var isRebuffering = false
    private var rebufferNoticeRunnable: Runnable? = null

    /** 是否已向上层报告缓冲中（避免 VLC 每个百分比事件都触发一次 onBuffering） */
    private var bufferingNotified = false

    // ========== 视频尺寸 / 视图 ==========
    private var _videoWidth = 0
    private var _videoHeight = 0
    private var videoFrameRate: Float? = null
    private var attachedVideoLayout: VLCVideoLayout? = null

    // ========== 画面卡死检测 ==========
    // 基于 TimeChanged（绝对时间）而非 PositionChanged：直播 duration=0 时 position 比例不会变化
    private var lastFrameUpdateTime = System.currentTimeMillis()
    private var lastTimeChangedValue = -1L
    private var freezeDetectionRunnable: Runnable? = null

    // ========== 解码能力检测（基于 libvlc 统计的丢帧比例） ==========
    private var decodeStatsRunnable: Runnable? = null
    private var decodeStatsBaselineValid = false
    private var lastDisplayedPictures = 0
    private var lastLostPictures = 0
    private var overloadedSampleCount = 0
    private var decoderOverloadReported = false

    private val networkCachingMs = resolveNetworkCachingMs(options)

    private val vlcEventListener = MediaPlayer.EventListener { event -> handleEvent(event) }

    // ------------------------------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------------------------------

    override fun initPlayer() {
        logger.info { "Initializing VLC player" }

        // 原生库不可用时直接抛出 LinkageError，由 VlcPlayerFactory 回退到 Media3
        VlcNativeLibs.load(appContext)

        try {
            val lib = obtainSharedLibVlc(appContext)
            libVlc = lib
            mediaPlayer = MediaPlayer(lib).apply { setEventListener(vlcEventListener) }
            logger.info {
                "LibVLC ${runCatching { LibVLC.version() }.getOrDefault("unknown")} ready, " +
                    "networkCaching=${networkCachingMs}ms"
            }
        } catch (e: Exception) {
            // 事件监听器此时尚未绑定，错误在 prepare() 时上报
            logger.error(e) { "Failed to initialize VLC player" }
            initializationError = Exception("VLC 播放器初始化失败：${e.message}", e)
        }
    }

    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        // 纯设置：不触碰当前 Media，避免直播 URL 续期时中断播放（见 AbstractVideoPlayer.playUrl 契约）
        currentVideoUrl = videoUrl
        currentAudioUrl = audioUrl
        clearPendingSeekPosition()
    }

    override fun prepare() {
        initializationError?.let {
            mPlayerEventListener?.onError(it)
            return
        }
        val mp = mediaPlayer ?: run {
            mPlayerEventListener?.onError(IllegalStateException("VLC MediaPlayer 未初始化"))
            return
        }
        val url = currentVideoUrl ?: currentAudioUrl
        if (url == null) {
            mPlayerEventListener?.onError(IllegalStateException("VLC 播放地址为空"))
            return
        }

        resetTransientState()
        readyDispatchedForCurrentMedia = false
        startupBuffering = true
        resetVideoInfo()
        resetDecodeStats()
        decoderOverloadReported = false
        cacheFillPercent = 0
        lastKnownPositionMs = 0L
        lastKnownDurationMs = 0L

        // 释放旧 Media（多次 prepare 不泄漏），设置新 Media；MediaPlayer 会 retain，本地引用立即 release
        mp.media = null
        val media = buildMedia(url, currentAudioUrl)
        mp.media = media
        media.release()

        // 与 ExoPlayer 不同，VLC 设置 media 后不会自动触发事件，这里直接开始播放；
        // 初始跳转位置在 Playing 事件中处理，确保 VLC 已真正开始播放
        playRequested = true
        mp.play()
    }

    override fun start() {
        playRequested = true
        if (isSeeking) {
            logger.debug { "start(): deferred until the pending seek completes" }
            return
        }
        mediaPlayer?.play()
        startFreezeDetection()
        startDecodeStatsMonitor()
    }

    override fun pause() {
        playRequested = false
        stopFreezeDetection()
        stopDecodeStatsMonitor()
        mediaPlayer?.pause()
    }

    override fun stop() {
        playRequested = false
        resetTransientState()
        stopFreezeDetection()
        stopDecodeStatsMonitor()
        clearPendingSeekPosition()
        mediaPlayer?.stop()
    }

    override fun reset() {
        stop()
        mediaPlayer?.media = null
        currentVideoUrl = null
        currentAudioUrl = null
    }

    override fun release() {
        logger.info { "Releasing VLC player" }
        stopFreezeDetection()
        stopDecodeStatsMonitor()
        cancelSeek()
        cancelRebufferNotice()
        mainHandler.removeCallbacksAndMessages(null)
        mPlayerEventListener = null

        val mp = mediaPlayer ?: return
        mediaPlayer = null
        libVlc = null // 进程级共享实例，不在此释放

        try {
            mp.setEventListener(null)
            // 先分离 Surface，防止 VLC 继续渲染到已销毁的 Surface（BufferQueue abandoned）
            attachedVideoLayout = null
            mp.detachViews()
        } catch (e: Exception) {
            logger.error(e) { "Error detaching VLC views" }
        }

        // VLC 3 的 stop()/release() 会同步 join input 线程，网络卡顿时可达秒级，放到后台执行避免 ANR
        releaseExecutor.execute {
            try {
                mp.stop()
                mp.release()
            } catch (e: Exception) {
                logger.error(e) { "Error releasing VLC MediaPlayer" }
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // 播放控制 / 状态
    // ------------------------------------------------------------------------------------------

    override val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    override val isSeekable: Boolean
        get() = _isSeekable

    override fun seekTo(time: Long) {
        if (!_isSeekable) {
            logger.warn { "Media is not seekable, ignoring seek to ${time}ms" }
            return
        }
        if (mediaPlayer == null) {
            logger.warn { "MediaPlayer is null, ignoring seek to ${time}ms" }
            return
        }
        beginSeek(target = time.coerceAtLeast(0L), pausePlayer = true)
    }

    override val currentPosition: Long
        get() = if (isSeeking) seekTargetPosition else mediaPlayer?.time ?: lastKnownPositionMs

    override val duration: Long
        get() = mediaPlayer?.length?.takeIf { it > 0L } ?: lastKnownDurationMs

    /** 已缓冲到的时长比例（与 Exo 语义一致），由当前位置 + 缓存填充量 × network-caching 估算 */
    override val bufferedPercentage: Int
        get() = computeBufferedPercentage(lastKnownPositionMs, lastKnownDurationMs)

    override fun setOptions() {
        // 保持兼容接口：VLC 在 prepare() 时即开始播放
    }

    override var speed: Float
        get() = mediaPlayer?.rate ?: 1f
        set(value) {
            mediaPlayer?.setRate(value)
        }

    override val tcpSpeed: Long
        get() = 0L // VLC 不支持网速查询

    override val debugInfo: String
        get() = """
            player: VLC ${if (libVlc != null) runCatching { LibVLC.version() }.getOrDefault("unknown") else "not initialized"}
            time: ${currentPosition.formatHourMinSec()} / ${duration.formatHourMinSec()}
            buffered: $bufferedPercentage% (cache fill $cacheFillPercent% of ${networkCachingMs}ms)
            resolution: $videoWidth x $videoHeight
            video fps: ${videoFrameRate ?: 0f}
            pictures displayed/lost: $lastDisplayedPictures / $lastLostPictures
            speed: $speed
        """.trimIndent()

    override val videoWidth: Int
        get() = _videoWidth

    override val videoHeight: Int
        get() = _videoHeight

    // ------------------------------------------------------------------------------------------
    // 事件处理
    // ------------------------------------------------------------------------------------------

    private fun handleEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Opening -> {
                readyDispatchedForCurrentMedia = false
                startupBuffering = true
                isRebuffering = false
                cancelRebufferNotice()
                notifyBuffering()
            }

            MediaPlayer.Event.Playing -> onPlayingEvent()

            MediaPlayer.Event.Paused -> {
                // seek 期间的内部暂停不上报，避免暂停图标闪烁
                if (!isSeeking) notifyPause()
            }

            MediaPlayer.Event.Stopped -> {
                resetTransientState()
                mPlayerEventListener?.onIdle()
            }

            MediaPlayer.Event.EncounteredError -> {
                logger.error { "VLC: EncounteredError" }
                resetTransientState()
                stopFreezeDetection()
                stopDecodeStatsMonitor()
                clearPendingSeekPosition()
                mPlayerEventListener?.onError(Exception("VLC playback error"))
            }

            MediaPlayer.Event.EndReached -> {
                resetTransientState()
                stopFreezeDetection()
                stopDecodeStatsMonitor()
                clearPendingSeekPosition()
                if (options.isLive) {
                    // 直播流没有“自然结束”：连接被 CDN 关闭（如 URL 过期）时 VLC 只会给 EndReached，
                    // 按错误上报以触发上层的直播重连（重新获取播放地址）
                    logger.warn { "VLC: live stream reached end, reporting as interruption" }
                    mPlayerEventListener?.onError(Exception("VLC live stream interrupted"))
                } else {
                    mPlayerEventListener?.onEnd()
                }
            }

            MediaPlayer.Event.Buffering -> onBufferingEvent(event.buffering)

            MediaPlayer.Event.TimeChanged -> {
                val timeMs = event.timeChanged
                lastKnownPositionMs = timeMs
                if (timeMs != lastTimeChangedValue) {
                    lastFrameUpdateTime = System.currentTimeMillis()
                    lastTimeChangedValue = timeMs
                }
                // 时间开始推进说明起播缓冲已经结束（兜底：个别流不会发出 Buffering 100%）
                if (startupBuffering && !isSeeking) {
                    finishStartupBuffering()
                }
                dispatchProgress(timeMs = timeMs)
            }

            MediaPlayer.Event.PositionChanged -> dispatchProgress(positionFraction = event.positionChanged)

            MediaPlayer.Event.LengthChanged -> {
                lastKnownDurationMs = event.lengthChanged.coerceAtLeast(0L)
                dispatchProgress()
            }

            MediaPlayer.Event.SeekableChanged -> {
                _isSeekable = event.seekable
                logger.debug { "Seekable changed: ${event.seekable}" }
                mPlayerEventListener?.onSeekableChanged(event.seekable)
            }

            // 视频输出建立/视频轨道就绪时刷新尺寸：Playing 时容器可能还没有可靠的尺寸（尤其是直播 FLV）
            MediaPlayer.Event.Vout -> if (event.voutCount > 0) updateVideoSize()

            MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESSelected -> {
                if (event.esChangedType == IMedia.Track.Type.Video) updateVideoSize()
            }
        }
    }

    private fun onPlayingEvent() {
        val mp = mediaPlayer ?: return

        if (_videoWidth <= 0) updateVideoSize()

        // 主动查询 seekable 状态（SeekableChanged 事件可能不可靠）
        val seekable = mp.isSeekable
        if (_isSeekable != seekable) {
            _isSeekable = seekable
            logger.debug { "Seekable (queried): $seekable" }
            mPlayerEventListener?.onSeekableChanged(seekable)
        }

        // 初始跳转：Playing 之后再 seek，确保 VLC 已真正开始播放；就绪/播放状态由 completeSeek() 上报
        val initialSeekPosition = pendingSeekPosition
        if (initialSeekPosition > 0L) {
            clearPendingSeekPosition()
            if (_isSeekable) {
                logger.debug { "Initial seek to ${initialSeekPosition}ms" }
                startupBuffering = false
                beginSeek(target = initialSeekPosition, pausePlayer = false)
                return
            }
            logger.warn { "Media is not seekable, ignoring initial seek to ${initialSeekPosition}ms" }
        }

        if (isSeeking) return

        // 缓存尚未填满：保持缓冲状态，等待 Buffering 100% / TimeChanged 再上报就绪与播放
        if (startupBuffering && cacheFillPercent < 100) {
            notifyBuffering()
            return
        }
        startupBuffering = false

        markReadyIfNeeded()
        lastFrameUpdateTime = System.currentTimeMillis()
        lastTimeChangedValue = -1L
        notifyPlay()
    }

    /** 与 Exo 的 STATE_READY 对齐：每个媒体只上报一次 onReady */
    private fun markReadyIfNeeded() {
        if (readyDispatchedForCurrentMedia) return
        readyDispatchedForCurrentMedia = true
        bufferingNotified = false
        mPlayerEventListener?.onReady()
    }

    /** VLC 每个缓冲百分比都会发事件，这里只在进入缓冲状态时上报一次 */
    private fun notifyBuffering() {
        if (bufferingNotified) return
        bufferingNotified = true
        mPlayerEventListener?.onBuffering()
    }

    /** 上报播放中；VLC 在 prepare() 后会直接开始播放而不经过 start()，因此监控随播放状态启停 */
    private fun notifyPlay() {
        bufferingNotified = false
        startFreezeDetection()
        startDecodeStatsMonitor()
        mPlayerEventListener?.onPlay()
    }

    private fun notifyPause() {
        bufferingNotified = false
        stopFreezeDetection()
        stopDecodeStatsMonitor()
        mPlayerEventListener?.onPause()
    }

    /** 起播缓冲结束：上报就绪，并按 VLC 当前状态上报播放/暂停 */
    private fun finishStartupBuffering() {
        startupBuffering = false
        markReadyIfNeeded()
        lastFrameUpdateTime = System.currentTimeMillis()
        lastTimeChangedValue = -1L
        if (mediaPlayer?.isPlaying == true) {
            notifyPlay()
        } else {
            notifyPause()
        }
    }

    private fun onBufferingEvent(cache: Float) {
        cacheFillPercent = cache.toInt().coerceIn(0, 100)

        if (isSeeking) {
            // seek 命令尚未真正下发时收到的 Buffering 属于上一状态的残留事件，不能当作 seek 完成
            if (!seekCommandIssued) {
                dispatchProgress()
                return
            }
            if (cache >= 100f) {
                completeSeek()
            } else {
                notifyBuffering()
                dispatchProgress()
            }
            return
        }

        if (startupBuffering) {
            // 起播阶段：Opening/Playing 之后的首次缓存填充，100% 即就绪
            if (cache >= 100f) {
                finishStartupBuffering()
            } else {
                notifyBuffering()
            }
            dispatchProgress()
            return
        }

        val playing = mediaPlayer?.isPlaying == true
        if (cache < 100f) {
            if (playing) {
                // VLC 在播放中重缓冲时状态仍为 Playing，这里防抖后上报，避免短暂抖动导致 UI 闪烁
                scheduleRebufferNotice()
            } else {
                notifyBuffering()
            }
        } else {
            cancelRebufferNotice()
            val wasRebuffering = isRebuffering
            isRebuffering = false
            if (wasRebuffering) {
                logger.debug { "Rebuffering finished" }
                lastFrameUpdateTime = System.currentTimeMillis()
                lastTimeChangedValue = -1L
            }
            if (playing) {
                notifyPlay()
            } else {
                bufferingNotified = false
                mPlayerEventListener?.onReady()
            }
        }
        dispatchProgress()
    }

    private fun scheduleRebufferNotice() {
        if (rebufferNoticeRunnable != null) return
        rebufferNoticeRunnable = Runnable {
            rebufferNoticeRunnable = null
            if (cacheFillPercent < 100 && !isSeeking && mediaPlayer?.isPlaying == true) {
                isRebuffering = true
                logger.debug { "Rebuffering while playing: $cacheFillPercent%" }
                notifyBuffering()
            }
        }.also { mainHandler.postDelayed(it, REBUFFER_NOTICE_DELAY_MS) }
    }

    private fun cancelRebufferNotice() {
        rebufferNoticeRunnable?.let { mainHandler.removeCallbacks(it) }
        rebufferNoticeRunnable = null
    }

    private fun resetTransientState() {
        cancelSeek()
        cancelRebufferNotice()
        isRebuffering = false
        startupBuffering = false
        bufferingNotified = false
    }

    // ------------------------------------------------------------------------------------------
    // seek
    // ------------------------------------------------------------------------------------------

    /**
     * 启动一次 seek：暂停（可选）→ 设置时间 → 等待 Buffering 100% 或超时后 [completeSeek]。
     * seek 命令异步下发，避免阻塞事件回调；[seekCommandIssued] 用来过滤下发前的残留 Buffering 事件。
     */
    private fun beginSeek(target: Long, pausePlayer: Boolean) {
        cancelSeek()
        val generation = ++seekGeneration
        isSeeking = true
        seekCommandIssued = false
        seekTargetPosition = target
        lastKnownPositionMs = target
        notifyBuffering()

        seekTimeoutRunnable = Runnable {
            if (isSeeking && generation == seekGeneration) {
                logger.warn { "Seek to ${target}ms timed out after ${SEEK_TIMEOUT_MS}ms, forcing completion" }
                completeSeek()
            }
        }.also { mainHandler.postDelayed(it, SEEK_TIMEOUT_MS) }

        mainHandler.post {
            val mp = mediaPlayer
            if (!isSeeking || generation != seekGeneration || mp == null) return@post
            if (pausePlayer && mp.isPlaying) mp.pause()
            mp.time = target
            seekCommandIssued = true
        }
    }

    /** seek 完成（缓冲就绪或超时）：上报 onSeeked，并按上层期望的状态恢复播放或保持暂停 */
    private fun completeSeek() {
        val target = seekTargetPosition
        cancelSeek()
        startupBuffering = false
        lastFrameUpdateTime = System.currentTimeMillis()
        lastTimeChangedValue = -1L
        markReadyIfNeeded()
        mPlayerEventListener?.onSeeked(target)

        val mp = mediaPlayer
        if (playRequested && mp != null) {
            mp.play()
            startFreezeDetection()
            startDecodeStatsMonitor()
            notifyPlay()
        } else {
            notifyPause()
        }
        dispatchProgress()
    }

    private fun cancelSeek() {
        seekTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        seekTimeoutRunnable = null
        isSeeking = false
        seekCommandIssued = false
    }

    // ------------------------------------------------------------------------------------------
    // 进度
    // ------------------------------------------------------------------------------------------

    /** 统一分发进度信息，兼容 TimeChanged/PositionChanged/LengthChanged 事件来源 */
    private fun dispatchProgress(timeMs: Long? = null, positionFraction: Float? = null) {
        val mp = mediaPlayer ?: return
        val durationMs = (mp.length.takeIf { it > 0L } ?: lastKnownDurationMs).coerceAtLeast(0L)
        lastKnownDurationMs = durationMs
        val positionMs = when {
            isSeeking -> seekTargetPosition
            timeMs != null && timeMs >= 0 -> timeMs
            positionFraction != null && durationMs > 0 -> (durationMs * positionFraction).toLong()
            else -> mp.time
        }.coerceAtLeast(0L)
        lastKnownPositionMs = positionMs
        mPlayerEventListener?.onProgress(positionMs, durationMs, computeBufferedPercentage(positionMs, durationMs))
    }

    private fun computeBufferedPercentage(positionMs: Long, durationMs: Long): Int {
        if (durationMs <= 0L) return cacheFillPercent.coerceIn(0, 100)
        val bufferedEndMs = positionMs + networkCachingMs.toLong() * cacheFillPercent / 100L
        return (bufferedEndMs * 100L / durationMs).toInt().coerceIn(0, 100)
    }

    // ------------------------------------------------------------------------------------------
    // 视频尺寸
    // ------------------------------------------------------------------------------------------

    private fun resetVideoInfo() {
        _videoWidth = 0
        _videoHeight = 0
        videoFrameRate = null
    }

    /** 从当前 Media 的视频轨道读取尺寸/帧率（VLC 3 需要主动查询），有变化时上报 */
    private fun updateVideoSize() {
        val mp = mediaPlayer ?: return
        // getMedia() 会增加引用计数，必须 release
        val media = mp.media ?: return
        try {
            for (index in 0 until media.trackCount) {
                val track = media.getTrack(index) as? IMedia.VideoTrack ?: continue
                if (track.width <= 0 || track.height <= 0) continue
                applyVideoTrackInfo(track)
                return
            }
            logger.debug { "No video track with a valid size yet" }
        } catch (e: Exception) {
            logger.debug { "Failed to read video track info: ${e.message}" }
        } finally {
            media.release()
        }
    }

    private fun applyVideoTrackInfo(track: IMedia.VideoTrack) {
        if (track.frameRateDen > 0 && track.frameRateNum > 0) {
            val frameRate = track.frameRateNum.toFloat() / track.frameRateDen.toFloat()
            if (frameRate.isFinite() && frameRate > 0f && frameRate != videoFrameRate) {
                videoFrameRate = frameRate
                mPlayerEventListener?.onVideoFrameRateChanged(frameRate)
            }
        }

        // VLC 报告的是编码尺寸（如 1920x1088），换算为可见尺寸再上报
        val (width, height) = VlcVideoSizeNormalizer.normalize(track.width, track.height)
        if (width == _videoWidth && height == _videoHeight) return
        _videoWidth = width
        _videoHeight = height
        logger.info { "Video size: ${width}x$height (coded ${track.width}x${track.height})" }
        mPlayerEventListener?.onVideoSizeChanged(width, height)
    }

    // ------------------------------------------------------------------------------------------
    // 视图绑定
    // ------------------------------------------------------------------------------------------

    /**
     * 附加视频渲染视图（官方推荐方式 attachViews）。
     *
     * 画面缩放固定为 SURFACE_BEST_FIT：横屏/竖屏视频都由 libvlc 在整个容器内 fit-center 放置，
     * SurfaceView 始终与容器同尺寸（也就天然满足硬解对 Surface 尺寸 16/32 对齐的要求）。
     *
     * @param videoLayout VLC 将在其中创建和管理 SurfaceView
     */
    fun attachVideoLayout(videoLayout: VLCVideoLayout) {
        val mp = mediaPlayer ?: return
        if (attachedVideoLayout === videoLayout) return
        if (attachedVideoLayout != null) detachVideoLayout()
        try {
            // 参数：FrameLayout, DisplayManager, enableSubtitles, enableTextureView
            // enableSubtitles=true 会额外创建一个透明的字幕 SurfaceView：libvlc 的 android_display（MediaCodec
            // 直出、零拷贝）要求有它才能启用，否则会回退到 gles2 走 GL 拷贝，4K 时每帧多一次 GPU 上传。
            mp.attachViews(videoLayout, null, true, false)
            // 缩放模式必须在 attachViews 之后设置才会生效（VideoHelper 由 attachViews 创建）
            mp.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
            attachedVideoLayout = videoLayout
            logger.info { "Attached VLC views to video layout (scale=SURFACE_BEST_FIT)" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to attach video layout" }
        }
    }

    /**
     * 分离视频渲染视图。
     *
     * 也用于 Activity 进入后台（ON_STOP）：SurfaceView 的 Surface 会随窗口隐藏而销毁，VLC 3 的 vout 随之关闭且
     * 不会在 Surface 重建后自动恢复（画面变黑、只剩声音）。libvlc 的 detachViews/attachViews 会关闭/重新启用视频轨，
     * 回到前台时重新 attach 即可让 VLC 用新的 Surface 重建 vout。
     */
    fun detachVideoLayout() {
        if (attachedVideoLayout == null) return
        logger.debug { "Detaching video layout from VLC player" }
        attachedVideoLayout = null
        try {
            mediaPlayer?.detachViews()
        } catch (e: Exception) {
            logger.error(e) { "Failed to detach video layout" }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Media 构建
    // ------------------------------------------------------------------------------------------

    private fun buildMedia(url: String, audioUrl: String?): Media {
        val normalizedAudioUrl = audioUrl?.takeIf { it.isNotBlank() && it != url }
        return Media(libVlc, url.toUri()).apply {
            // 必须在 setMedia() 之前设置：否则 libvlc-android 的 setDefaultMediaPlayerOptions()
            // 会为每个 Media 追加 :network-caching=1500，覆盖 LibVLC 级别的 --network-caching
            addOption(":network-caching=$networkCachingMs")
            options.playbackRefererFor(url, normalizedAudioUrl)?.let {
                addOption(":http-referrer=$it")
            }
            options.userAgent?.let {
                addOption(":http-user-agent=$it")
            }
            // 合入 DASH 音频流
            normalizedAudioUrl?.let { audio ->
                addSlave(IMedia.Slave(IMedia.Slave.Type.Audio, 0, audio))
            }
            VLCOptions.setMediaOptions(
                media = this,
                noHardwareAcceleration = !options.enableHardwareDecode,
                hardwareAcceleration = if (options.enableHardwareDecode) {
                    VLCOptions.HW_ACCELERATION_AUTOMATIC
                } else {
                    VLCOptions.HW_ACCELERATION_DISABLED
                }
            )
        }
    }

    // ------------------------------------------------------------------------------------------
    // 画面卡死检测和恢复
    // ------------------------------------------------------------------------------------------

    /**
     * 定期检查 TimeChanged 是否停止更新。仅在缓存已满（非重缓冲）、非 seek 期间判定为卡死，
     * 避免把网络重缓冲误判为解码卡死而反复 pause/play。
     */
    private fun startFreezeDetection() {
        stopFreezeDetection()
        lastFrameUpdateTime = System.currentTimeMillis()
        lastTimeChangedValue = -1L
        freezeDetectionRunnable = object : Runnable {
            override fun run() {
                if (isPlaying && !isSeeking && !isRebuffering && cacheFillPercent >= 100) {
                    val timeSinceLastFrame = System.currentTimeMillis() - lastFrameUpdateTime
                    if (timeSinceLastFrame > FREEZE_THRESHOLD_MS) {
                        logger.warn { "Detected video freeze (no TimeChanged for ${timeSinceLastFrame}ms), attempting recovery" }
                        attemptFreezeRecovery()
                    }
                }
                mainHandler.postDelayed(this, FREEZE_CHECK_INTERVAL_MS)
            }
        }.also { mainHandler.postDelayed(it, FREEZE_CHECK_INTERVAL_MS) }
    }

    private fun stopFreezeDetection() {
        freezeDetectionRunnable?.let { mainHandler.removeCallbacks(it) }
        freezeDetectionRunnable = null
    }

    /** 策略：暂停再播放（最简单且有效的恢复方式） */
    private fun attemptFreezeRecovery() {
        val mp = mediaPlayer ?: return
        if (!mp.isPlaying) return
        logger.info { "Freeze recovery: toggling pause/play" }
        lastFrameUpdateTime = System.currentTimeMillis()
        mp.pause()
        mainHandler.postDelayed({
            val player = mediaPlayer ?: return@postDelayed
            if (playRequested && !isSeeking && !player.isPlaying) {
                player.play()
            }
        }, FREEZE_RECOVERY_RESUME_DELAY_MS)
    }

    // ------------------------------------------------------------------------------------------
    // 解码能力检测
    // ------------------------------------------------------------------------------------------

    /**
     * 每 [DECODE_STATS_INTERVAL_MS] 读取一次 libvlc 统计（--stats），比较采样窗口内被 vout 丢弃的帧数
     * （`lostPictures`，即 "picture is too late to be displayed"）与显示的帧数。硬件/软件解码器跟不上时
     * VLC 3 无法跳过解码，帧会持续晚到并全部被丢弃、画面越来越落后；这里连续 [OVERLOAD_SAMPLES_REQUIRED] 个
     * 窗口丢帧占比 ≥ 50% 即判定解码过载，上报一次供上层建议降低清晰度。
     */
    private fun startDecodeStatsMonitor() {
        if (decodeStatsRunnable != null) return
        decodeStatsBaselineValid = false
        decodeStatsRunnable = object : Runnable {
            override fun run() {
                sampleDecodeStats()
                mainHandler.postDelayed(this, DECODE_STATS_INTERVAL_MS)
            }
        }.also { mainHandler.postDelayed(it, DECODE_STATS_INTERVAL_MS) }
    }

    private fun stopDecodeStatsMonitor() {
        decodeStatsRunnable?.let { mainHandler.removeCallbacks(it) }
        decodeStatsRunnable = null
        decodeStatsBaselineValid = false
        overloadedSampleCount = 0
    }

    private fun resetDecodeStats() {
        decodeStatsBaselineValid = false
        lastDisplayedPictures = 0
        lastLostPictures = 0
        overloadedSampleCount = 0
    }

    private fun sampleDecodeStats() {
        val mp = mediaPlayer ?: return
        if (!mp.isPlaying || isSeeking || isRebuffering || startupBuffering) {
            // 非稳态播放期间的丢帧不具参考意义，重新建立基线
            decodeStatsBaselineValid = false
            overloadedSampleCount = 0
            return
        }
        val media = mp.media ?: return
        val stats = try {
            media.stats
        } catch (e: Exception) {
            logger.debug { "Failed to read VLC stats: ${e.message}" }
            null
        } finally {
            media.release()
        } ?: return

        val displayed = stats.displayedPictures
        val lost = stats.lostPictures
        if (decodeStatsBaselineValid) {
            val displayedDelta = (displayed - lastDisplayedPictures).coerceAtLeast(0)
            val lostDelta = (lost - lastLostPictures).coerceAtLeast(0)
            val total = displayedDelta + lostDelta
            val overloaded = total >= MIN_FRAMES_PER_STATS_SAMPLE && lostDelta * 2 >= total
            overloadedSampleCount = if (overloaded) overloadedSampleCount + 1 else 0
            if (overloaded) {
                logger.debug { "Decoder falling behind: lost $lostDelta / $total frames in last ${DECODE_STATS_INTERVAL_MS}ms" }
            }
            if (overloadedSampleCount >= OVERLOAD_SAMPLES_REQUIRED && !decoderOverloadReported) {
                decoderOverloadReported = true
                logger.warn {
                    "Decoder overloaded: $lostDelta / $total frames dropped per ${DECODE_STATS_INTERVAL_MS}ms " +
                        "for ${overloadedSampleCount * DECODE_STATS_INTERVAL_MS}ms (video ${_videoWidth}x${_videoHeight})"
                }
                mPlayerEventListener?.onDecoderOverloaded(lostDelta, total)
            }
        }
        lastDisplayedPictures = displayed
        lastLostPictures = lost
        decodeStatsBaselineValid = true
    }

    init {
        // 必须放在所有属性初始化器之后：Kotlin 按声明顺序执行初始化，
        // 提前调用会让 initPlayer() 读到尚未初始化的字段（如 vlcEventListener）。
        initPlayer()
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        private const val SEEK_TIMEOUT_MS = 10_000L // VLC 缓冲可能很慢
        private const val REBUFFER_NOTICE_DELAY_MS = 400L
        private const val FREEZE_THRESHOLD_MS = 5_000L
        private const val FREEZE_CHECK_INTERVAL_MS = 5_000L
        private const val FREEZE_RECOVERY_RESUME_DELAY_MS = 500L
        private const val DECODE_STATS_INTERVAL_MS = 2_000L
        private const val MIN_FRAMES_PER_STATS_SAMPLE = 20
        private const val OVERLOAD_SAMPLES_REQUIRED = 3

        /** network-caching 同时是起播前的预缓冲量和稳态目标缓冲量，过大会拖慢起播和 seek */
        private const val NETWORK_CACHING_LIVE_MS = 1_500
        private const val NETWORK_CACHING_VOD_MS = 3_000
        private const val NETWORK_CACHING_EXPANDED_MS = 8_000

        private val releaseExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "vlc-release")
        }

        @Volatile
        private var sharedLibVlc: LibVLC? = null
        private val libVlcLock = Any()

        internal fun resolveNetworkCachingMs(options: VideoPlayerOptions): Int = when {
            options.expandBuffer -> NETWORK_CACHING_EXPANDED_MS
            options.isLive -> NETWORK_CACHING_LIVE_MS
            else -> NETWORK_CACHING_VOD_MS
        }

        /**
         * LibVLC 实例进程级复用：加载模块列表耗时较长且实例间没有差异（网络缓存等按 Media 设置），
         * 与官方 VLC-Android 的 VLCInstance 单例做法一致。
         */
        private fun obtainSharedLibVlc(context: Context): LibVLC {
            sharedLibVlc?.let { return it }
            synchronized(libVlcLock) {
                sharedLibVlc?.let { return it }
                val libOptions = VLCOptions.getLibOptions(
                    context = context,
                    config = VLCConfig.Builder()
                        // 倍速播放时保持音调（scaletempo），本项目倍速是核心功能
                        .setEnableTimeStretching(true)
                        // LibVLC 级别默认值；实际生效的是 buildMedia() 中的 :network-caching
                        .setNetworkCaching(NETWORK_CACHING_VOD_MS)
                        // release 包不输出 -vv 级别日志
                        .setVerboseMode(BuildConfig.DEBUG)
                        .build()
                )
                return LibVLC(context, libOptions).also {
                    sharedLibVlc = it
                    logger.info { "Created shared LibVLC instance" }
                }
            }
        }
    }
}
