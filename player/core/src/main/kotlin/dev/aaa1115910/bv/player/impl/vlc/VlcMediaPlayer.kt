package dev.aaa1115910.bv.player.impl.vlc

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.util.formatHourMinSec
import io.github.oshai.kotlinlogging.KotlinLogging
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout

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

    // 保存 SurfaceView 引用用于尺寸调整
    private var currentSurfaceView: SurfaceView? = null

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
                // PositionChanged 事件提供进度百分比，补充上报
                dispatchProgress(positionFraction = event.positionChanged)
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

        // 手动加载 VLC native 库
        try {
            System.loadLibrary("c++_shared")
            logger.info { "Loaded libc++_shared" }
        } catch (e: UnsatisfiedLinkError) {
            logger.debug { "libc++_shared already loaded or not available: ${e.message}" }
        }

        try {
            System.loadLibrary("vlc")
            logger.info { "Loaded libvlc" }
        } catch (e: UnsatisfiedLinkError) {
            logger.debug { "libvlc already loaded or not available: ${e.message}" }
        }

        val vlcOptions = arrayListOf<String>().apply {
            // 网络缓存设置（毫秒）
            add(":network-caching=1500")
            // 硬件解码
            add(":codec=mediacodec,all")
            // AV_CODEC 格式
            add(":avcodec-fast=1")
            // 跳过帧
            add(":avcodec-skiploopfilter=1")
            // 线程数
            add(":avcodec-threads=${Runtime.getRuntime().availableProcessors()}")
            
            // HDR 配置
            // 启用 Android 显示输出
            add("--vout=android-display")
            // 自动色调映射：仅在显示器不支持 HDR 时才进行转换
            // 0=强制转换, 1=自动（根据显示能力决定）, 2=禁用
            add("--tone-mapping=1")
            // 设置色调映射算法（当需要转换时使用 Hable 算法）
            add("--tone-mapping-algorithm=1")
            // 设置目标峰值亮度（cd/m²）
            add("--tone-mapping-param=100")
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
    }

    override fun pause() {
        logger.debug { "Pausing VLC player" }
        mediaPlayer?.pause()
    }

    override fun stop() {
        logger.debug { "Stopping VLC player" }
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
            player: VLC ${if (libVlc != null) try { LibVLC.version() } catch (e: Exception) { "unknown" } else "not initialized"}
            time: ${currentPosition.formatHourMinSec()} / ${duration.formatHourMinSec()}
            buffered: $bufferedPercentage%
            resolution: ${videoWidth} x ${videoHeight}
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
     * 视频布局监听器
     * 当视频尺寸变化时，重新计算并设置窗口尺寸
     */
    private val videoLayoutListener = IVLCVout.OnNewVideoLayoutListener { vlcVout, width, height, visibleWidth, visibleHeight, sarNum, sarDen ->
        logger.info { "Video layout changed: ${width}x${height}, visible: ${visibleWidth}x${visibleHeight}, SAR: $sarNum:$sarDen" }
        
        // 保存视频尺寸
        _videoWidth = width
        _videoHeight = height
        
        // 在 SurfaceView 上更新窗口尺寸
        currentSurfaceView?.let { surfaceView ->
            surfaceView.post {
                val surfaceWidth = surfaceView.width
                val surfaceHeight = surfaceView.height
                if (surfaceWidth > 0 && surfaceHeight > 0) {
                    logger.info { "Updating VLC window size to surface: ${surfaceWidth}x${surfaceHeight}" }
                    vlcVout.setWindowSize(surfaceWidth, surfaceHeight)
                }
            }
        }
    }

    /**
     * 附加视频渲染视图
     * VLC 使用 IVLCVout 接口来附加视图
     */
    fun attachSurface(surfaceView: SurfaceView) {
        try {
            currentSurfaceView = surfaceView
            val ivlcVout = mediaPlayer?.getVLCVout()
            if (ivlcVout != null) {
                ivlcVout.setVideoView(surfaceView)

                // 设置窗口尺寸
                surfaceView.post {
                    val width = surfaceView.width
                    val height = surfaceView.height
                    if (width > 0 && height > 0) {
                        logger.info { "Setting VLC window size: ${width}x${height}" }
                        ivlcVout.setWindowSize(width, height)
                    }
                }

                // 使用 OnNewVideoLayoutListener 附加视图
                ivlcVout.attachViews(videoLayoutListener)
                
                // 设置视频缩放模式为 BEST_FIT
                mediaPlayer?.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT)
                
                // 重新设置事件监听器
                mediaPlayer?.setEventListener(vlcEventListener)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to attach SurfaceView" }
        }
    }

    /**
     * 分离视频渲染视图
     */
    fun detachSurface() {
        logger.debug { "Detaching SurfaceView from VLC player" }
        try {
            currentSurfaceView = null
            mediaPlayer?.getVLCVout()?.detachViews()
        } catch (e: Exception) {
            logger.error(e) { "Failed to detach SurfaceView" }
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
        return Media(libVlc, Uri.parse(url)).apply {
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
}
