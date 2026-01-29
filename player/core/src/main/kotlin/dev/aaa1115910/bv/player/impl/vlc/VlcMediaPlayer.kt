package dev.aaa1115910.bv.player.impl.vlc

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.videolan.libvlc.util.VLCVideoLayout
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.util.formatHourMinSec
import io.github.oshai.kotlinlogging.KotlinLogging
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import androidx.core.net.toUri
import java.io.File

/**
 * VLC 播放器实现
 *
 * 基于 LibVLC 的视频播放器实现，支持更广泛的视频格式
 *
 * VLC native 库在 initPlayer() 中手动加载
 */
class VlcMediaPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer() {
    private val logger = KotlinLogging.logger { }

    init {
        initPlayer()
    }

    var libVlc: LibVLC? = null
    var mediaPlayer: MediaPlayer? = null
    var media: Media? = null

    private var currentVideoUrl: String? = null
    private var currentAudioUrl: String? = null
    private var headers: Map<String, String> = emptyMap()

    // 视频旋转支持
    private var currentRotation: Int = 0

    // 缓冲百分比
    private var _bufferedPercentage: Int = 0

    // 视频尺寸
    private var _videoWidth: Int = 0
    private var _videoHeight: Int = 0

    // 保存 VideoLayout 引用用于尺寸调整
    private var currentVideoLayout: VLCVideoLayout? = null

    // ========== 画面卡死检测 ==========
    // 使用 PositionChanged 事件检测视频帧更新（基于视频帧，比 TimeChanged 更准确）
    // PositionChanged 长时间不触发通常意味着解码器卡住了
    private var lastFrameUpdateTime = System.currentTimeMillis()
    private var lastPositionFraction = -1f
    private val freezeDetectionHandler = Handler(Looper.getMainLooper())
    private var freezeDetectionRunnable: Runnable? = null
    private val FREEZE_THRESHOLD_MS = 5_000L  // 5秒无视频帧更新视为卡死

    // 保持事件监听器的强引用，防止被GC回收
    // @Volatile
    // private var eventListenerHolder: MediaPlayer.EventListener? = null

    private val vlcEventListener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.Playing -> {
                mPlayerEventListener?.onPlay()
            }
            MediaPlayer.Event.Paused -> {
                mPlayerEventListener?.onPause()
            }
            MediaPlayer.Event.Stopped -> {
                mPlayerEventListener?.onIdle()
            }
            MediaPlayer.Event.EncounteredError -> {
                logger.error { "VLC: Error event" }
                mPlayerEventListener?.onError(Exception("VLC playback error"))
            }
            MediaPlayer.Event.EndReached -> {
                mPlayerEventListener?.onEnd()
            }
            MediaPlayer.Event.Buffering -> {
                val cache = event.buffering
                _bufferedPercentage = cache.toInt()
                // 只有当播放器实际暂停时才报告缓冲状态
                if (mediaPlayer?.isPlaying == true) {
                    // 播放中，不报告缓冲状态，只更新缓冲百分比
                    if (cache >= 100f) {
                        // 缓冲完成，确保播放状态正确
                        mPlayerEventListener?.onPlay()
                    }
                } else {
                    // 播放器暂停，正常报告缓冲状态
                    if (cache < 100f) {
                        mPlayerEventListener?.onBuffering()
                    } else {
                        mPlayerEventListener?.onReady()
                    }
                }
                dispatchProgress()
            }
            MediaPlayer.Event.Opening -> {
                mPlayerEventListener?.onBuffering()
            }
            MediaPlayer.Event.TimeChanged -> {
                // TimeChanged 正常播放时也会触发，作为进度上报
                dispatchProgress(timeMs = event.timeChanged)
            }
            MediaPlayer.Event.PositionChanged -> {
                val position = event.positionChanged
                val now = System.currentTimeMillis()

                // 更新卡死检测状态（基于视频帧位置变化）
                // PositionChanged 长时间不触发通常意味着解码器卡住了
                if (isPlaying && position != lastPositionFraction) {
                    lastFrameUpdateTime = now
                    lastPositionFraction = position
                }

                // PositionChanged 事件提供进度百分比，补充上报
                dispatchProgress(positionFraction = position)
            }
            MediaPlayer.Event.SeekableChanged -> {
//                mPlayerEventListener?.onSeekableChanged(event.seekable)
            }
            MediaPlayer.Event.PausableChanged -> {
//                mPlayerEventListener?.onPausableChanged(event.pausable)
            }
            MediaPlayer.Event.RecordChanged -> {
                // 记录变化事件
            }
        }
    }

    override fun initPlayer() {
        logger.info { "Initializing VLC player" }

        // 加载 VLC native 库
        // 优先从 vlc_libs 目录加载按需下载的库，回退到 APK 内置库
        loadVlcNativeLibs(context)

        val vlcOptions = arrayListOf<String>().apply {
            // ========== 缓存配置 ==========
            add(":network-caching=3000")
            add(":file-caching=3000")

            // ========== 编解码器配置 ==========
            add(":codec=mediacodec-ndk,all")
            add(":mediacodec-dr=1")

            // ========== 性能优化 ==========
            add(":avcodec-fast=1")
            add(":avcodec-skiploopfilter=1")
            add(":avcodec-skip-frame=1")
            add(":avcodec-skip-idct=1")        // 跳过 IDCT 变换
            add(":avcodec-threads=${Runtime.getRuntime().availableProcessors()}")

            // ========== 跳帧策略 ==========
            add(":skip-frames=1")              // 启用跳帧
            add(":drop-late-frames=1")         // 丢弃延迟帧

            // ========== 音视频同步策略 ==========
            add(":audio-desync=500")           // 允许音视频偏差 500ms
            add(":clock-jitter=2000")          // 时钟抖动容差 2 秒
            add(":audio-resample-method=0")    // 禁用音频重采样

            // ========== 音频配置 ==========
            add(":spdif")                      // 启用音频透传
            add(":aout=opensles")              // 音频输出模块

            // ========== 视频输出配置 ==========
            add(":vout=android-display")       // Android 显示输出
        }

        try {
            libVlc = LibVLC(context, vlcOptions)
            mediaPlayer = MediaPlayer(libVlc).apply {
                setEventListener(vlcEventListener)
            }

        } catch (e: UnsatisfiedLinkError) {
            logger.error(e) { "VLC native library not available" }
            mPlayerEventListener?.onError(Exception("VLC 播放器不可用，请确保应用正确安装"))
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize VLC player" }
            mPlayerEventListener?.onError(e)
        }
    }

    override fun setHeader(headers: Map<String, String>) {
        this.headers = headers
        // VLC 通过 Media 选项设置请求头，需要重新创建 Media 才能生效
    }

    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        this.currentVideoUrl = videoUrl
        this.currentAudioUrl = audioUrl

        // 如果已有 Media 对象，先释放
        media?.release()

        // VLC 合入音频流
        val url = videoUrl ?: audioUrl
        if (url != null) {
            media = buildMedia(url, audioUrl)
        }
    }

    override fun prepare() {
        media?.let {
            mediaPlayer?.media = it
        }

        // 与 ExoPlayer 不同，VLC 不会在设置 media 后自动触发事件
        mediaPlayer?.play()

        // 处理初始跳转位置
        if (pendingSeekPosition > 0) {
            mediaPlayer?.time = pendingSeekPosition
            clearPendingSeekPosition()
        }
    }

    override fun start() {
        logger.debug { "Starting VLC player" }
        mediaPlayer?.play()
        startFreezeDetection() // 启动卡死检测
    }

    override fun pause() {
        logger.debug { "Pausing VLC player" }
        mediaPlayer?.pause()
        stopFreezeDetection() // 停止卡死检测
    }

    override fun stop() {
        logger.debug { "Stopping VLC player" }
        stopFreezeDetection() // 停止卡死检测
        mediaPlayer?.stop()
    }

    override fun reset() {
        logger.debug { "Resetting VLC player" }
        mediaPlayer?.stop()
        media?.release()
        media = null
    }

    override val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    override fun seekTo(time: Long) {
        logger.debug { "Seeking to ${time}ms" }
        mediaPlayer?.time = time
    }

    override fun release() {
        logger.info { "Releasing VLC player" }
        stopFreezeDetection() // 停止卡死检测
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            media?.release()
            media = null
            libVlc?.release()
            libVlc = null
        } catch (e: Exception) {
            logger.error(e) { "Error releasing VLC player" }
        }
    }

    override val currentPosition: Long
        get() = mediaPlayer?.time ?: 0L 

    override val duration: Long
        get() = mediaPlayer?.length ?: 0L 

    override val bufferedPercentage: Int
        get() = _bufferedPercentage

    override fun setOptions() {
        // 保持兼容接口
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
            player: VLC ${if (libVlc != null) try { LibVLC.version() } catch (_: Exception) { "unknown" } else "not initialized"}
            time: ${currentPosition.formatHourMinSec()} / ${duration.formatHourMinSec()}
            buffered: $bufferedPercentage%
            resolution: $videoWidth x $videoHeight
            speed: $speed
        """.trimIndent()

    override val videoWidth: Int
        get() = _videoWidth

    override val videoHeight: Int
        get() = _videoHeight

    /**
     * 统一分发进度信息，兼容 TimeChanged/PositionChanged 事件来源
     */
    private fun dispatchProgress(timeMs: Long? = null, positionFraction: Float? = null) {
        val durationMs = mediaPlayer?.length ?: 0L
        val positionMs = when {
            timeMs != null && timeMs >= 0 -> timeMs
            positionFraction != null && durationMs > 0 -> (durationMs * positionFraction).toLong()
            else -> mediaPlayer?.time ?: 0L
        }.coerceAtLeast(0L)

        val buffered = _bufferedPercentage.coerceIn(0, 100)
        mPlayerEventListener?.onProgress(positionMs, durationMs.coerceAtLeast(0L), buffered)
    }

    /**
     * 附加视频渲染视图（官方推荐方式）
     * 使用 mediaPlayer.attachViews() 而非 IVLCVout.setVideoView()
     *
     * @param videoLayout FrameLayout 容器，VLC 将在其中创建和管理 SurfaceView
     */
    fun attachVideoLayout(videoLayout: VLCVideoLayout) {
        try {
            currentVideoLayout = videoLayout

            // 使用官方推荐的 attachViews 方法
            // 参数：FrameLayout, DisplayManager, enableSubtitles, enableTextureView
            mediaPlayer?.attachViews(videoLayout, null, false, false)

            // 设置视频缩放模式
            mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT

            // 重新设置事件监听器
            mediaPlayer?.setEventListener(vlcEventListener)

            logger.info { "Attached VLC views to video layout" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to attach video layout" }
        }
    }

    /**
     * 分离视频渲染视图
     */
    fun detachVideoLayout() {
        logger.debug { "Detaching video layout from VLC player" }
        try {
            currentVideoLayout = null
            // 使用官方方法分离视图
            mediaPlayer?.detachViews()
        } catch (e: Exception) {
            logger.error(e) { "Failed to detach video layout" }
        }
    }

    /**
     * 设置视频旋转角度
     * VLC 通过 transform 滤镜实现视频旋转
     *
     * @param degrees 旋转角度（90、180、270 或 0）
     */
    fun setVideoRotation(degrees: Int) {
        if (currentRotation == degrees) return

        logger.info { "Setting video rotation to $degrees degrees" }
        currentRotation = degrees

        // 触发缓冲回调（显示加载提示）
        mPlayerEventListener?.onBuffering()

        // 保存当前位置
        val position = currentPosition
        val wasPlaying = isPlaying

        // 重新创建带旋转滤镜的 Media
        media?.let { oldMedia ->
            val url = currentVideoUrl ?: currentAudioUrl
            url?.let {
                val newMedia = buildMedia(it, currentAudioUrl)

                mediaPlayer?.media = newMedia
                oldMedia.release()

                // 恢复播放位置
                if (position > 0) {
                    seekTo(position)
                }

                // 恢复播放状态
                if (wasPlaying) {
                    start()
                }
            }
        }
    }

    private fun buildMedia(url: String, audioUrl: String?): Media {
        val normalizedAudioUrl = audioUrl?.takeIf { it.isNotBlank() && it != url }
        return Media(libVlc, url.toUri()).apply {
            // VLC 使用 :http-header= 格式设置自定义请求头
            headers.forEach { (key, value) ->
                addOption(":http-header=$key: $value")
            }
            // 设置 Referer
            options.referer?.let {
                addOption(":http-referrer=$it")
            }
            // 设置 User-Agent
            options.userAgent?.let {
                addOption(":http-user-agent=$it")
            }
            // 合入音频流
            normalizedAudioUrl?.let {
                addOption(":input-slave=$it")
            }
            // 如果有旋转设置，添加滤镜
            if (currentRotation != 0) {
                addOption(":video-filter=transform")
                addOption(":transform-type=${mapDegreesToTransform(currentRotation)}")
            }
        }
    }

    /**
     * 将角度映射到 VLC transform 类型的字符串表示
     */
    private fun mapDegreesToTransform(degrees: Int): String = when (degrees) {
        90 -> "90"
        180 -> "180"
        270, -90 -> "270"
        else -> "0"
    }

    // ========== 画面卡死检测和恢复 ==========

    /**
     * 启动画面卡死检测
     * 定期检查 PositionChanged 事件是否触发（基于视频帧），如果超过阈值未更新则尝试恢复
     */
    private fun startFreezeDetection() {
        stopFreezeDetection()
        // 重置检测时间，避免从暂停恢复时误判
        lastFrameUpdateTime = System.currentTimeMillis()
        freezeDetectionRunnable = object : Runnable {
            override fun run() {
                if (isPlaying) {
                    val now = System.currentTimeMillis()
                    val timeSinceLastFrame = now - lastFrameUpdateTime

                    if (timeSinceLastFrame > FREEZE_THRESHOLD_MS) {
                        logger.warn { "Detected video freeze (no PositionChanged for ${timeSinceLastFrame}ms), attempting recovery" }
                        attemptFreezeRecovery()
                    }
                }
                freezeDetectionHandler.postDelayed(this, 5_000) // 每5秒检查一次
            }
        }
        freezeDetectionHandler.post(freezeDetectionRunnable!!)
    }

    /**
     * 停止画面卡死检测
     */
    private fun stopFreezeDetection() {
        freezeDetectionRunnable?.let {
            freezeDetectionHandler.removeCallbacks(it)
        }
        freezeDetectionRunnable = null
    }

    /**
     * 尝试从画面卡死中恢复
     * 策略：暂停再播放（最简单且有效的恢复方式）
     */
    private fun attemptFreezeRecovery() {
        val wasPlaying = isPlaying
        if (wasPlaying) {
            logger.info { "Freeze recovery: toggling play/pause" }
            pause()
            freezeDetectionHandler.postDelayed({
                if (isPlaying.not()) {
                    start()
                }
            }, 500)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private var libsLoaded = false

        /**
         * 加载 VLC native 库
         * 优先从 vlc_libs 目录加载按需下载的库，回退到 APK 内置库
         */
        private fun loadVlcNativeLibs(context: Context) {
            if (libsLoaded) {
                logger.debug { "VLC libs already loaded" }
                return
            }

            try {
                val vlcLibsDir = File(context.filesDir, "vlc_libs")
                val libvlcFile = File(vlcLibsDir, "libvlc.so")
                val cxxFile = File(vlcLibsDir, "libc++_shared.so")
                val libvlcjniFile = File(vlcLibsDir, "libvlcjni.so")

                if (vlcLibsDir.exists() && libvlcFile.exists() && cxxFile.exists()) {
                    // 加载按需下载的库
                    logger.info { "Loading VLC libs from: $vlcLibsDir" }

                    // 先加载 libc++_shared（C++ 标准库）
                    System.load(cxxFile.absolutePath)
                    logger.info { "Loaded libc++_shared from ${cxxFile.absolutePath}" }

                    // 再加载 libvlcjni.so（JNI 绑定）
                    if (libvlcjniFile.exists()) {
                        System.load(libvlcjniFile.absolutePath)
                        logger.info { "Loaded libvlcjni from ${libvlcjniFile.absolutePath}" }
                    }

                    // 最后加载 libvlc.so（核心库）
                    System.load(libvlcFile.absolutePath)
                    logger.info { "Loaded libvlc from ${libvlcFile.absolutePath}" }
                } else {
                    // 回退到 APK 内置库
                    logger.info { "Loading VLC libs from APK" }

                    try {
                        System.loadLibrary("c++_shared")
                        logger.info { "Loaded libc++_shared from APK" }
                    } catch (e: UnsatisfiedLinkError) {
                        logger.debug { "libc++_shared already loaded or not available: ${e.message}" }
                    }

                    try {
                        System.loadLibrary("vlc")
                        logger.info { "Loaded libvlc from APK" }
                    } catch (e: UnsatisfiedLinkError) {
                        logger.debug { "libvlc already loaded or not available: ${e.message}" }
                    }
                }

                libsLoaded = true
            } catch (e: Exception) {
                logger.error(e) { "Failed to load VLC native libraries" }
                throw e
            }
        }
    }
}
