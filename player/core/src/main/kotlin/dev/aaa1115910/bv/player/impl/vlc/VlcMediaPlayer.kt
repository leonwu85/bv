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
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.SurfaceView
import android.view.ViewTreeObserver
import androidx.core.net.toUri
import org.videolan.libvlc.interfaces.IMedia
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

    // 保存 VLC 内部创建的 SurfaceView 引用用于手动缩放
    private var vlcSurfaceView: View? = null

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
                updateVideoSize()
                applyManualSurfaceViewScaling()
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
            add("--network-caching=3000")
            add("--file-caching=5000")

            // ========== 编解码器配置 ==========
            add("--codec=mediacodec-ndk,all")
            // add("--mediacodec-dr")
            // ========== 性能优化 ==========
            // add("--avcodec-hw=none")                 
            // add("--avcodec-fast")
            add("--avcodec-skiploopfilter=1")
            add("--avcodec-threads=${Runtime.getRuntime().availableProcessors()}")

            // ========== 跳帧策略 ==========
        //    add("--no-drop-late-frames")    // 防止過度丟幀造成的視覺卡頓
        //    add("--no-skip-frames")
//            add("--skip-frames")              // 启用跳帧
//            add("--drop-late-frames")         // 丢弃延迟帧

            // ========== 音视频同步策略 ==========
            add("--drop-late-frames")            // 丟棄延遲幀以維持同步
            // add("--skip-frames")                 // 必要時跳幀
            add("--clock-jitter=0")              // 減少時鐘抖動影響
            
            // ========== 音频配置 ==========
            // add("--spdif")                      // 启用音频透传
            // add("--aout=opensles")              // 音频输出模块

            // ========== 视频输出配置 ==========
            add("--vout=android-display")       // Android 显示输出
            // add("--vout=android-opengl")
            // add("--vout=opengles2")
            // add("--tone-mapping=3")          
            // 或指定算法（根据视频/设备测试）：
            // add("--tone-mapping=1")          // Reinhard（常见，默认之一，平衡亮度/颜色）
            // add("--tone-mapping=2")          // Hable（电影感强）
            // add("--tone-mapping=3")          // Mobius（避免过曝）
            // add("--tone-mapping-param=0.8")
            // add("--tone-mapping-desat=0.0")     // 去饱和度，0.0 为默认（不额外去饱和）
//            add("--tone-mapping-peak=1000")   // 峰值亮度（nits），HDR10 常见 1000，Dolby Vision 可更高；根据视频调整
        }

        // ========== 调试日志：输出 VLC 配置 ==========
        logger.debug { "VLC Options count: ${vlcOptions.size}" }
        vlcOptions.forEachIndexed { index, option ->
            logger.debug { "  VLC Option [$index]: $option" }
        }

        try {
            libVlc = LibVLC(context, vlcOptions)
            mediaPlayer = MediaPlayer(libVlc).apply {
                setEventListener(vlcEventListener)
            }

            // ========== 调试日志：验证 LibVLC 实例 ==========
            logger.info { "LibVLC created successfully" }
            logger.info { "LibVLC version: ${try { LibVLC.version() } catch (e: Exception) { "unknown" }}" }
            logger.info { "LibVLC hashCode: ${libVlc?.hashCode()}" }
            logger.info { "MediaPlayer created: ${mediaPlayer?.hashCode()}" }

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

        // 先让 MediaPlayer 释放对旧 Media 的引用，防止内存泄漏
        mediaPlayer?.media = null

        // 释放旧的 Media 对象的 native 资源
        media?.release()
        media = null

        // 注意：不在这里创建新的 Media，在 prepare() 中创建
        // 这样可以避免在多次调用 playUrl() 时创建多个未使用的 media 对象
    }

    override fun prepare() {
        // 先释放旧的 Media（防止多次调用 prepare() 时泄漏）
        mediaPlayer?.media = null
        media?.release()
        media = null

        // 创建新的 Media
        val url = currentVideoUrl ?: currentAudioUrl
        if (url != null) {
            val newMedia = buildMedia(url, currentAudioUrl)

            // 设置给 MediaPlayer
            mediaPlayer?.media = newMedia

            // 关键：立即释放我们的 Java 引用
            // MediaPlayer 会持有 native 引用，我们的 Java 引用不再需要
            // 这是防止 Media 内存泄漏的正确方式
            newMedia.release()

            // 保存到类成员（用于 setVideoRotation 等操作）
            // 注意：此时 Java 引用计数已释放，只有 MediaPlayer 持有 native 引用
            media = newMedia
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

        // 先让 MediaPlayer 释放对 Media 的引用，防止内存泄漏
        mediaPlayer?.media = null

        // 再释放 Media 对象的 native 资源
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
            // 1. 先移除事件监听器，防止后续回调导致泄露
            mediaPlayer?.setEventListener(null)

            // 2. 停止播放
            mediaPlayer?.stop()

            // 3. 先分离 Surface 视图，防止 VLC 继续渲染到已销毁的 Surface
            // 这一步必须在 release() 之前执行，否则会出现 BufferQueue abandoned 错误
            mediaPlayer?.detachViews()
            vlcSurfaceView = null
            currentVideoLayout = null

            // 4. 关键：先让 MediaPlayer 释放对 Media 的引用，防止内存泄漏
            // 这必须在 mediaPlayer.release() 之前执行
            mediaPlayer?.media = null

            // 5. 释放 Media 的 native 资源
            media?.release()
            media = null

            // 6. 释放 MediaPlayer
            mediaPlayer?.release()
            mediaPlayer = null

            // 7. 释放 LibVLC
            libVlc?.release()
            libVlc = null

            // 8. 清理 Handler 中的所有回调
            freezeDetectionHandler.removeCallbacksAndMessages(null)

            // 9. 清理事件监听器引用
            mPlayerEventListener = null

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
     * 更新视频尺寸
     * 从 VLC 的 IMedia.Track 获取视频尺寸
     * 确保 MediaPlayer 已经加载媒体并开始播放
     */
    private fun updateVideoSize() {
        try {
            val mp = mediaPlayer ?: return
            val media = mp.media ?: return

            // 獲取軌道數量
//            val trackCount = media.trackCount
            val tracks = media.tracks
            for (track in tracks) {
                if (track.type == IMedia.Track.Type.Video) {
                    val videoTrack = track as IMedia.VideoTrack
                    val width = videoTrack.width
                    val height = videoTrack.height
                    if (width > 0 && height > 0) {
                        _videoWidth = width
                        _videoHeight = height
                        logger.info { "Video size from IMedia.Track: ${_videoWidth}x${_videoHeight}" }
                        updateScaleMode()
                        return
                    }
                }
            }
            logger.debug { "No video track found or invalid size" }

        } catch (e: Exception) {
            logger.debug { "Failed to get video size: ${e.message}" }
        }
    }

    /**
     * 判断视频是否为竖屏
     * @return true 如果视频高度大于宽度（竖屏），false 否则（横屏或正方形）
     */
    private fun isPortraitVideo(): Boolean {
        return _videoHeight > _videoWidth
    }

    /**
     * 根据视频方向动态设置 VLC 缩放模式
     * - 竖屏视频：使用 SURFACE_FILL 填充整个屏幕
     * - 横屏视频：使用 SURFACE_BEST_FIT 保持宽高比适配屏幕
     */
    private fun updateScaleMode() {
        val scaleType = if (isPortraitVideo()) {
            MediaPlayer.ScaleType.SURFACE_FILL
        } else {
            MediaPlayer.ScaleType.SURFACE_BEST_FIT
        }
        mediaPlayer?.videoScale = scaleType
        logger.info { "Updated scale mode: $scaleType (video=${_videoWidth}x${_videoHeight}, isPortrait=${isPortraitVideo()})" }
    }

    /**
     * 手动计算并设置 SurfaceView 的尺寸
     * 保持视频原始比例，根据屏幕尺寸进行 fit-center 缩放
     * 确保宽高符合硬件解码器的像素对齐要求（32 字节对齐）
     * 注意：仅对竖屏视频应用手动缩放，横屏视频由 VLC 的 SURFACE_BEST_FIT 自动处理
     */
    private fun applyManualSurfaceViewScaling() {
        if (_videoWidth <= 0 || _videoHeight <= 0) {
            logger.debug { "Video size not available, skipping scaling" }
            return
        }

        // 横屏视频不应用手动缩放，由 VLC 的 SURFACE_BEST_FIT 自动处理
        if (!isPortraitVideo()) {
            logger.debug { "Landscape video, skipping manual scaling (VLC will handle it)" }
            return
        }

        val surfaceView = vlcSurfaceView ?: return
        val container = currentVideoLayout ?: return

        // 使用屏幕尺寸进行缩放计算，而非容器尺寸
        // 因为容器可能被外层的 aspectRatio 修饰符限制，导致视频显示区域过小
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val containerWidth = container.width
        val containerHeight = container.height

        if (screenWidth <= 0 || screenHeight <= 0) {
            logger.debug { "Screen size not available, skipping scaling" }
            return
        }

        // 计算视频宽高比和屏幕宽高比
        val videoRatio = _videoWidth.toFloat() / _videoHeight.toFloat()
        val screenRatio = screenWidth.toFloat() / screenHeight.toFloat()

        // 计算 fit-center 缩放后的原始尺寸（基于屏幕尺寸）
        val (rawWidth, rawHeight) = if (videoRatio > screenRatio) {
            screenWidth to (screenWidth / videoRatio).toInt()
        } else {
            (screenHeight * videoRatio).toInt() to screenHeight
        }

        // 应用像素对齐（最大值为屏幕尺寸）
        // 竖屏视频：宽度对齐，高度保持原值
        val surfaceWidth = calculateAlignedSize(rawWidth, screenWidth)
        val surfaceHeight = rawHeight  // 竖屏高度不需要对齐

        // 设置 SurfaceView 的 LayoutParams
        val params = surfaceView.layoutParams as? FrameLayout.LayoutParams
            ?: FrameLayout.LayoutParams(surfaceWidth, surfaceHeight)

        params.width = surfaceWidth
        params.height = surfaceHeight
        params.gravity = android.view.Gravity.CENTER

        surfaceView.layoutParams = params

        logger.info { "Applied manual SurfaceView scaling: " +
            "video=${_videoWidth}x${_videoHeight}, " +
            "screen=${screenWidth}x${screenHeight}, " +
            "container=${containerWidth}x${containerHeight}, " +
            "raw=${rawWidth}x${rawHeight}, " +
            "aligned=${surfaceWidth}x${surfaceHeight} " +
            "(alignment=$PIXEL_ALIGNMENT)" }
    }

    /**
     * 递归查找 VLC 创建的 SurfaceView
     * 
     */
    private fun findVlcSurfaceView(parent: ViewGroup): View? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is SurfaceView) {
                return child
            }
            if (child is ViewGroup) {
                val found = findVlcSurfaceView(child)
                if (found != null) return found
            }
        }
        return null
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

            // 根据视频方向动态设置缩放模式
            // 竖屏视频使用 SURFACE_FILL，横屏视频使用 SURFACE_BEST_FIT
            updateScaleMode()

            // 使用官方推荐的 attachViews 方法
            // 参数：FrameLayout, DisplayManager, enableSubtitles, enableTextureView
            mediaPlayer?.attachViews(videoLayout, null, false, false)

            // 获取 VLC 内部创建的 SurfaceView
            videoLayout.postDelayed({
                // 查找 VLC 创建的 SurfaceView
                vlcSurfaceView = findVlcSurfaceView(videoLayout)
                logger.info { "Found VLC SurfaceView: $vlcSurfaceView" }

                // 应用手动缩放
                applyManualSurfaceViewScaling()
            }, 100)

            // 监听布局尺寸变化
            videoLayout.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        applyManualSurfaceViewScaling()
                    }
                }
            )

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
            vlcSurfaceView = null
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

        // 保存当前位置和播放状态
        val position = currentPosition
        val wasPlaying = isPlaying

        // 从 MediaPlayer 获取当前 Media（而不是从类成员）
        // 这样可以确保获取到真正在使用的 Media 对象
        val oldMedia = mediaPlayer?.media
        val url = currentVideoUrl ?: currentAudioUrl
        url?.let {
            val newMedia = buildMedia(it, currentAudioUrl)

            // 先设置新 media（让 MediaPlayer 释放旧引用）
            mediaPlayer?.media = newMedia

            // 关键：立即释放我们的 Java 引用
            // MediaPlayer 会持有 native 引用，我们的 Java 引用不再需要
            newMedia.release()

            // 释放旧 media
            oldMedia?.release()

            // 更新类成员 media 引用
            this@VlcMediaPlayer.media = newMedia

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
            setHWDecoderEnabled(true, false)
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
        // 清理所有可能的 pending callbacks，防止 Handler 导致内存泄露
        freezeDetectionHandler.removeCallbacksAndMessages(null)
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
         * 像素对齐常量
         * MTK 和部分高通芯片要求 SurfaceView 的宽高必须是 16 或 32 的倍数
         */
        private const val PIXEL_ALIGNMENT = 32

        /**
         * 将数值对齐到指定的倍数
         * @param value 原始值
         * @param alignment 对齐倍数（16 或 32）
         * @return 对齐后的值（向下取整到最接近的 alignment 倍数）
         */
        private fun alignTo(value: Int, alignment: Int = PIXEL_ALIGNMENT): Int {
            return (value / alignment) * alignment
        }

        /**
         * 将数值对齐到指定的倍数（向上取整）
         * @param value 原始值
         * @param alignment 对齐倍数（16 或 32）
         * @return 对齐后的值（向上取整到最接近的 alignment 倍数）
         */
        private fun alignToCeil(value: Int, alignment: Int = PIXEL_ALIGNMENT): Int {
            return ((value + alignment - 1) / alignment) * alignment
        }

        /**
         * 计算对齐后的尺寸，确保不超过容器尺寸
         * @param desired 期望的尺寸
         * @param max 容器的最大尺寸
         * @param alignment 对齐倍数
         * @return 对齐后的尺寸（保证 <= max）
         */
        private fun calculateAlignedSize(desired: Int, max: Int, alignment: Int = PIXEL_ALIGNMENT): Int {
            val aligned = alignTo(desired, alignment)
            // 如果对齐后为 0 或超过最大值，尝试向下对齐
            return when {
                aligned == 0 -> alignment
                aligned > max -> alignTo(max, alignment)
                else -> aligned
            }
        }

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
                    logger.info { "[VLC-DEBUG] Loading VLC libs from: $vlcLibsDir" }
                    // 按序加载
                    // 1. libc++_shared
                    System.load(cxxFile.absolutePath)
                    logger.info { "[VLC-DEBUG] Loaded libc++_shared from ${cxxFile.absolutePath}" }

                    // 2. libvlc.so
                    System.load(libvlcFile.absolutePath)
                    logger.info { "[VLC-DEBUG] Loaded libvlc from ${libvlcFile.absolutePath}" }

                    // 3. libvlcjni.so
                    if (libvlcjniFile.exists()) {
                        System.load(libvlcjniFile.absolutePath)
                        logger.info { "[VLC-DEBUG] Loaded libvlcjni from ${libvlcjniFile.absolutePath}" }
                    }
                } else {
                    // 回退到 APK 内置库
                    logger.info { "[VLC-DEBUG] Loading VLC libs from APK (AAR built-in)" }
                    try {
                        System.loadLibrary("c++_shared")
                        logger.info { "[VLC-DEBUG] Loaded libc++_shared from APK" }
                    } catch (e: UnsatisfiedLinkError) {
                        logger.debug { "libc++_shared already loaded or not available: ${e.message}" }
                    }

                    try {
                        System.loadLibrary("vlc")
                        logger.info { "[VLC-DEBUG] Loaded libvlc from APK" }
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
