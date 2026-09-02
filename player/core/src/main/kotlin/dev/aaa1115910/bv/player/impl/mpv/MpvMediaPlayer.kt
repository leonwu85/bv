package dev.aaa1115910.bv.player.impl.mpv

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Surface
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.SuperResolutionType
import dev.aaa1115910.bv.player.impl.NativeCxxRuntime
import dev.aaa1115910.bv.player.impl.NativeRuntimeConflictException
import dev.aaa1115910.bv.player.playbackRefererFor
import dev.aaa1115910.bv.util.formatHourMinSec
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvEvent
import `is`.xyz.mpv.MPVLib.MpvFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URI
import java.util.Locale
import kotlin.math.abs

/**
 * MPV 播放器实现（基于官方 mpv-android 的 libmpv/libplayer JNI）。
 *
 * 所有权：`MPVLib` 封装的是进程级唯一的 mpv 句柄，重复 `create()` 或在 `destroy()` 之后调用任何
 * JNI 都会让 native 层直接 `exit(1)`。因此本类在 [initPlayer] 中会先释放仍然存活的上一个实例，
 * 并用 [mpvInitialized]/[released] 保护每一次 JNI 调用。
 *
 * 线程模型：mpv 事件由 native 事件线程回调，这里统一投递到主线程处理，所有可变状态只在主线程读写。
 */
class MpvMediaPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer() {
    private val logger = KotlinLogging.logger { }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mpvInitialized = false
    private var initializationError: Exception? = null
    private var currentVideoUrl: String? = null
    private var currentAudioUrl: String? = null
    private var offlinePlaybackMode = false

    private var loadRequested = false
    private var mediaLoading = false
    private var mediaLoaded = false
    private var audioTrackAdded = false
    private var playWhenReady = false
    private var surfaceAttached = false
    private var released = false

    /**
     * loadfile/stop 之后、下一次 START_FILE 之前收到的 END_FILE 属于被替换/停止的旧文件，直接忽略。
     * 依赖 mpv 事件的严格顺序：END_FILE(old) → START_FILE(new) → FILE_LOADED(new) → … → END_FILE(new)。
     */
    private var awaitingStartFile = false
    private var endDispatchedForCurrentMedia = false
    private var eofReachedForCurrentMedia = false
    private var pendingSeekCallbackPosition: Long? = null

    private var _isPlaying = false
    private var _isSeekable = true
    private var _currentPosition = 0L
    private var _duration = 0L

    /** paused-for-cache 时的缓冲进度（0..100） */
    private var cacheBufferingPercent = 0

    /** demuxer 已缓存的前向时长（毫秒），用于按 Exo 语义换算 bufferedPercentage */
    private var demuxerCacheAheadMs = 0L
    private var _videoWidth = 0
    private var _videoHeight = 0
    private var videoFrameRate: Float? = null
    private var videoCodec: String? = null
    private var audioCodec: String? = null
    private var hwdec: String? = null
    private val videoOutput = resolveVideoOutputMode()
    private var attachedSurface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var superResolutionShaderPaths = ""
    private var lastSuperResolutionStateLogKey: String? = null

    /** 本次会话实际生效的 demuxer 缓存参数（用户未覆盖时按设备内存分档） */
    private var cacheConfig: MpvCacheConfig? = null

    /**
     * 是否把 B 站 CDN 的 HTTPS 地址改写为 HTTP。默认跟随选项；只有在系统根证书导出失败、
     * libmpv 无法校验任何 HTTPS 证书时才被强制打开，避免整个内核不可用。
     */
    private var preferHttpForCdn = options.mpvPreferHttpForCdn

    /** 最近的 mpv 错误日志，用于给 onError 提供可读原因 */
    private val recentErrorLogs = ArrayDeque<String>()

    // ========== 解码能力检测（基于 mpv frame-drop-count） ==========
    private var voFrameDropCount = 0L
    private var lastSampledFrameDropCount = 0L
    private var frameDropBaselineValid = false
    private var overloadedSampleCount = 0
    private var decoderOverloadReported = false
    private var progressTicksSinceDropSample = 0

    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            dispatchProgress()
            if (++progressTicksSinceDropSample >= DROP_SAMPLE_EVERY_PROGRESS_TICKS) {
                progressTicksSinceDropSample = 0
                sampleFrameDrops()
            }
            if (_isPlaying && !released) {
                mainHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    // mpv 事件线程 → 主线程
    private val eventObserver = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) = onMain { handlePropertyNone(property) }

        override fun eventProperty(property: String, value: Long) = onMain { handlePropertyLong(property, value) }

        override fun eventProperty(property: String, value: Double) = onMain { handlePropertyDouble(property, value) }

        override fun eventProperty(property: String, value: Boolean) = onMain { handlePropertyBoolean(property, value) }

        override fun eventProperty(property: String, value: String) = onMain { handlePropertyString(property, value) }

        override fun event(eventId: Int) = onMain { handleEvent(eventId) }
    }

    private val logObserver = object : MPVLib.LogObserver {
        override fun logMessage(prefix: String, level: Int, text: String) {
            if (level <= MPV_LOG_LEVEL_ERROR) {
                onMain { recordErrorLog("[$prefix] ${text.trim()}") }
            }
        }
    }

    init {
        initPlayer()
    }

    override fun initPlayer() {
        logger.info { "Initializing MPV player" }
        try {
            MpvLibsInstaller.loadNativeLibs(context)
            synchronized(instanceLock) {
                // MPVLib 是进程级单例：上一个实例还活着就先释放，否则 native create() 会 exit(1)
                activeInstance?.takeIf { it !== this }?.let { previous ->
                    logger.warn { "Releasing previous MpvMediaPlayer before creating a new mpv handle" }
                    previous.release()
                }
                MPVLib.create(context.applicationContext)
                activeInstance = this
            }
            configureBeforeInit()
            MPVLib.init()
            mpvInitialized = true
            configureAfterInit()
            MPVLib.addObserver(eventObserver)
            MPVLib.addLogObserver(logObserver)
            observePlaybackProperties()

            logger.info { "MPV player initialized successfully" }
        } catch (error: Throwable) {
            initializationError = Exception("MPV 播放器不可用：${describeInitFailure(error)}", error)
            logger.error(error) { "Failed to initialize MPV player" }
        }
    }

    private fun describeInitFailure(error: Throwable): String {
        return when {
            // 运行库冲突：组件已安装，但进程里已经有一份更旧的 libc++，只能重启进程
            error is NativeRuntimeConflictException -> error.message.orEmpty()
            error is UnsatisfiedLinkError && !MpvLibsInstaller.isInstalled(context) ->
                "请在播放器内核选择 MPV 时下载官方 mpv-android 组件"
            error is UnsatisfiedLinkError -> {
                val loadedRuntime = NativeCxxRuntime.loaded
                val hint = if (loadedRuntime != null && loadedRuntime.owner != MpvLibsInstaller.COMPONENT_NAME) {
                    "（当前进程已加载 ${loadedRuntime.owner} 组件的 C++ 运行库，可尝试完全退出并重新打开应用）"
                } else {
                    ""
                }
                "${error.message ?: "原生库加载失败"}$hint"
            }
            else -> error.message ?: error.javaClass.simpleName
        }
    }

    override fun setOfflinePlaybackMode(enabled: Boolean) {
        if (offlinePlaybackMode == enabled) return
        offlinePlaybackMode = enabled
        applyOfflinePlaybackMode()
    }

    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        // 纯设置：不触碰正在播放的媒体，状态重置在 loadMedia() 中进行（见 AbstractVideoPlayer.playUrl 契约）
        currentVideoUrl = videoUrl
        currentAudioUrl = audioUrl
        clearPendingSeekPosition()
    }

    override fun prepare() {
        initializationError?.let {
            dispatchError(it)
            return
        }

        loadRequested = true
        dispatchBuffering()

        if (surfaceAttached || currentVideoUrl == null) {
            loadMedia()
        }
    }

    override fun start() {
        playWhenReady = true
        if (!mediaLoaded) return
        runMpv("start") {
            setPropertyBoolean("pause", false)
        }
        updatePlayingState(true)
    }

    override fun pause() {
        playWhenReady = false
        runMpv("pause") {
            setPropertyBoolean("pause", true)
        }
        updatePlayingState(false)
    }

    override fun stop() {
        playWhenReady = false
        awaitingStartFile = true
        stopProgressUpdates()
        clearPendingSeekPosition()
        runMpv("stop") {
            command(arrayOf("stop"))
        }
        mediaLoading = false
        mediaLoaded = false
        updatePlayingState(false)
        dispatchIdle()
    }

    override fun reset() {
        stop()
        currentVideoUrl = null
        currentAudioUrl = null
        loadRequested = false
        audioTrackAdded = false
        mediaLoading = false
        pendingSeekCallbackPosition = null
    }

    override val isPlaying: Boolean
        get() = _isPlaying

    override val isSeekable: Boolean
        get() = _isSeekable

    override fun seekTo(time: Long) {
        if (!_isSeekable) {
            logger.warn { "Media is not seekable, ignoring seek to ${time}ms" }
            return
        }

        val target = time.coerceAtLeast(0L)
        if (!mediaLoaded) {
            // 文件尚未加载时 time-pos 不可写，改为在 FILE_LOADED 时执行
            logger.debug { "Media not loaded yet, defer seek to ${target}ms" }
            setInitialSeekPosition(target)
            _currentPosition = target
            dispatchProgress()
            return
        }

        pendingSeekCallbackPosition = target
        _currentPosition = target

        runMpv("seek") {
            setPropertyDouble("time-pos", target / 1000.0)
        }
        dispatchProgress()
    }

    override fun release() {
        if (released) return
        released = true
        logger.info { "Releasing MPV player" }

        stopProgressUpdates()
        mainHandler.removeCallbacksAndMessages(null)

        try {
            if (mpvInitialized) {
                MPVLib.removeObserver(eventObserver)
                MPVLib.removeLogObserver(logObserver)
            }
            detachSurfaceInternal()
            synchronized(instanceLock) {
                if (activeInstance === this) {
                    if (mpvInitialized) {
                        MPVLib.command(arrayOf("stop"))
                    }
                    MPVLib.destroy()
                    activeInstance = null
                }
            }
        } catch (error: Throwable) {
            logger.error(error) { "Error releasing MPV player" }
        } finally {
            mpvInitialized = false
            mPlayerEventListener = null
        }
    }

    override val currentPosition: Long
        get() = _currentPosition

    override val duration: Long
        get() = _duration

    /** 已缓冲到的时长比例（与 Exo 语义一致）；无时长（直播）时退化为缓冲填充比例 */
    override val bufferedPercentage: Int
        get() = computeBufferedPercentage()

    override fun setOptions() {
        playWhenReady = true
        if (mediaLoaded) start()
    }

    override var speed: Float
        get() = if (mpvInitialized && !released) {
            runCatching { MPVLib.getPropertyDouble("speed")?.toFloat() }.getOrNull() ?: 1f
        } else {
            1f
        }
        set(value) {
            runMpv("set speed") {
                setPropertyDouble("speed", value.toDouble())
            }
        }

    override val tcpSpeed: Long
        get() = 0L

    override val debugInfo: String
        get() {
            val version = if (mpvInitialized && !released) {
                runCatching { MPVLib.getPropertyString("mpv-version") }.getOrNull()
            } else {
                null
            } ?: "not initialized"
            return """
                player: $version
                time: ${currentPosition.formatHourMinSec()} / ${duration.formatHourMinSec()}
                buffered: $bufferedPercentage% (demuxer cache ${demuxerCacheAheadMs}ms, cache fill $cacheBufferingPercent%)
                resolution: $videoWidth x $videoHeight
                video fps: ${videoFrameRate ?: 0f}
                video codec: ${videoCodec ?: "unknown"}
                audio codec: ${audioCodec ?: "unknown"}
                hwdec: ${hwdec ?: "unknown"}
                vo: $videoOutput
                super resolution: ${options.superResolutionType} (active=$isSuperResolutionActive)
                demuxer cache: ${cacheConfig?.let { "${it.maxBytesMiB}/${it.maxBackBytesMiB}MiB, ${it.cacheSecs}s" } ?: "user"}
                cdn transport: ${if (preferHttpForCdn) "http (rewritten)" else "https"}
                speed: $speed
            """.trimIndent()
        }

    override val videoWidth: Int
        get() = _videoWidth

    override val videoHeight: Int
        get() = _videoHeight

    val usesEmbeddedVideoOutput: Boolean
        get() = videoOutput == MEDIACODEC_EMBED_VIDEO_OUTPUT

    /** gpu/gpu-next 路径下 shader 在跑且未被离线模式/直通输出禁用 */
    override val isSuperResolutionActive: Boolean
        get() = superResolutionShaderPaths.isNotBlank() && !offlinePlaybackMode && !usesEmbeddedVideoOutput

    /**
     * 只有 `mediacodec_embed` 让解码器直接输出到 Surface，由 SurfaceFlinger 完成 HDR 信令；
     * gpu/gpu-next 经 AImageReader 采样后拿不到 PQ/HLG 元数据，HDR 内容无法按 HDR 输出。
     */
    override val supportsHdrOutput: Boolean
        get() = usesEmbeddedVideoOutput

    /**
     * `vo=gpu + hwdec=mediacodec` 零拷贝依赖 AImageReader（API 26）。更低版本只能 `mediacodec-copy`，
     * 4K 帧拷贝在电视盒子 CPU 上撑不住，默认清晰度限制到 1080P60（用户手动选择不受限）。
     */
    override val preferredMaxResolutionCode: Int?
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !usesEmbeddedVideoOutput) {
            Resolution.R1080P60.code
        } else {
            null
        }

    /** 运行时卸载超分 shader（仅本次会话），并让丢帧检测重新开始计数 */
    override fun disableSuperResolution() {
        if (superResolutionShaderPaths.isBlank()) return
        superResolutionShaderPaths = ""
        runMpv("disable super resolution") {
            setOptionString("glsl-shaders", "")
        }
        decoderOverloadReported = false
        overloadedSampleCount = 0
        frameDropBaselineValid = false
        logger.info { "MPV super resolution disabled for this session" }
    }

    fun attachSurface(surface: Surface, width: Int, height: Int) {
        if (released || !mpvInitialized) return
        runMpv("attach surface") {
            attachSurface(surface)
            attachedSurface = surface
            surfaceAttached = true
            setOptionString("force-window", "yes")
            setPropertyString("vo", videoOutput)
            updateSurfaceSize(width, height)
        }
        applySurfaceFrameRate()

        if (loadRequested && !mediaLoaded && !mediaLoading) {
            loadMedia()
        }
    }

    /**
     * 与 Exo 的 `VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS` 对齐：让支持“匹配内容帧率”的
     * Android TV 12+ 设备在不黑屏的前提下切到 24/25/50 Hz。TV 端默认关闭该策略，此时不做任何事。
     */
    private fun applySurfaceFrameRate() {
        if (!options.enableVideoFrameRateStrategy || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val surface = attachedSurface?.takeIf { it.isValid } ?: return
        val fps = videoFrameRate?.takeIf { it > 1f } ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                surface.setFrameRate(
                    fps,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS
                )
            } else {
                surface.setFrameRate(fps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
            }
        }.onFailure { logger.debug { "Surface.setFrameRate($fps) failed: ${it.message}" } }
    }

    fun updateSurfaceSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        surfaceWidth = width
        surfaceHeight = height
        runMpv("update surface size") {
            setPropertyString("android-surface-size", "${width}x$height")
        }
        logSuperResolutionState("surface-size")
    }

    fun detachSurface() {
        if (released) return
        detachSurfaceInternal()
    }

    /** 与上游 MPVView 一致：先禁用 vo 再解绑 Surface；release() 期间也需要执行，因此不经过 runMpv 的 released 检查 */
    private fun detachSurfaceInternal() {
        if (!surfaceAttached) return
        if (mpvInitialized) {
            runCatching {
                MPVLib.setPropertyString("vo", "null")
                MPVLib.setOptionString("force-window", "no")
                MPVLib.detachSurface()
            }.onFailure {
                logger.error(it) { "MPV detach surface failed" }
            }
        }
        surfaceAttached = false
        attachedSurface = null
        surfaceWidth = 0
        surfaceHeight = 0
    }

    private fun configureBeforeInit() {
        val configDir = context.getDir("mpv", Context.MODE_PRIVATE)
        val cacheDir = context.cacheDir.resolve("mpv").also { it.mkdirs() }

        setOption("config", "yes")
        setOption("config-dir", configDir.absolutePath)
        setOption("gpu-shader-cache-dir", cacheDir.absolutePath)
        setOption("icc-cache-dir", cacheDir.absolutePath)
        setOption("profile", "fast")
        // OSD/OSC/统计/控制台等内置 Lua 脚本全部用不上，低端盒子上省掉它们的内存与 hook 开销
        BUILTIN_SCRIPT_OPTIONS.forEach { (name, value) -> setOption(name, value) }
        setOption("gpu-context", options.mpvGpuContext.ifBlank { DEFAULT_GPU_CONTEXT })
        options.mpvGpuApi.trim().takeIf { it.isNotBlank() }?.let { gpuApi ->
            setOption("gpu-api", gpuApi)
        }
        setOption("opengl-es", "yes")
        setOption("vo", videoOutput)
        setOption("ao", options.audioOutputDevices.ifBlank { DEFAULT_AUDIO_OUTPUT_DEVICES })
        setOption("ytdl", "no")
        setOption("video-sync", options.videoSync.ifBlank { DEFAULT_VIDEO_SYNC })
        options.autoSync.trim().takeIf { it.isNotBlank() && it != "0" }?.let { autoSync ->
            setOption("autosync", autoSync)
        }
        // Android 上 mpv 拿不到显示刷新率；display-resample 等 display-* 同步模式和 vsync 统计都依赖它
        displayRefreshRate()?.let { refreshRate ->
            setOption("display-fps-override", String.format(Locale.US, "%.3f", refreshRate))
        }
        setOption("hwdec", resolveHardwareDecodeMode())
        setOption(
            "hwdec-codecs",
            options.mpvHardwareDecodeCodecs.ifBlank { DEFAULT_HARDWARE_DECODE_CODECS }
        )
        options.mpvVdQueueEnable.trim().takeIf { it.isNotBlank() }?.let { vdQueueEnable ->
            setOption("vd-queue-enable", vdQueueEnable)
        }
        configureSuperResolutionShaders()
        options.mpvCache.trim().takeIf { it.isNotBlank() }?.let { cache ->
            setOption("cache", cache)
        }
        configureDemuxerCache()
        configureTls()
        // 仅约束 TCP/TLS 建连；总传输超时（network-timeout）保持 mpv 默认，curl 后端会把它当作整体超时
        setOption("curl-connect-timeout", CURL_CONNECT_TIMEOUT_SECS.toString())

        applyNetworkOptions()
    }

    /**
     * demuxer 缓存：用户显式填写的值优先；否则按设备内存分档（见 [MpvCachePolicy]）。
     * `cache-secs` 始终下发，防止低码率音轨 demuxer 把字节配额填满。
     */
    private fun configureDemuxerCache() {
        val config = resolveCacheConfig().also { cacheConfig = it }
        val maxBytes = options.mpvDemuxerMaxBytes.trim().ifBlank { config.maxBytes.toString() }
        val maxBackBytes = options.mpvDemuxerMaxBackBytes.trim().ifBlank { config.maxBackBytes.toString() }
        setOption("demuxer-max-bytes", maxBytes)
        setOption("demuxer-max-back-bytes", maxBackBytes)
        setOption("cache-secs", config.cacheSecs.toString())
        setOption("cache-pause-wait", String.format(Locale.US, "%.1f", config.cachePauseWaitSecs))
        logger.info {
            "MPV demuxer cache: max=$maxBytes back=$maxBackBytes cache-secs=${config.cacheSecs} " +
                "pause-wait=${config.cachePauseWaitSecs}s (live=${options.isLive}, expand=${options.expandBuffer})"
        }
    }

    private fun resolveCacheConfig(): MpvCacheConfig {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        runCatching { activityManager?.getMemoryInfo(memoryInfo) }
        return MpvCachePolicy.resolve(
            totalMemBytes = memoryInfo.totalMem,
            isLowRamDevice = activityManager?.isLowRamDevice == true,
            isLive = options.isLive,
            expandBuffer = options.expandBuffer,
        )
    }

    /**
     * libmpv（libcurl + mbedtls）没有系统证书库，必须显式给 CA 文件，否则所有 HTTPS 都会因证书校验失败而打不开。
     * 证书导出失败时退回 HTTP 改写，保证内核仍可用。
     */
    private fun configureTls() {
        val bundle = MpvCaBundle.ensure(context)
        if (bundle != null) {
            setOption("tls-ca-file", bundle.absolutePath)
            setOption("tls-verify", "yes")
        } else if (!preferHttpForCdn) {
            logger.warn { "System CA bundle unavailable; falling back to HTTP for Bilibili CDN hosts" }
            preferHttpForCdn = true
        }
    }

    private fun displayRefreshRate(): Float? = runCatching {
        context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
            ?.refreshRate
            ?.takeIf { it > 1f }
    }.getOrNull()

    /** `mpv_set_option_string` 失败只返回负数错误码；记录下来，避免拼错/不支持的选项被静默吞掉 */
    private fun setOption(name: String, value: String) {
        val result = MPVLib.setOptionString(name, value)
        if (result < 0) {
            logger.warn { "mpv rejected option $name=$value (error $result)" }
        }
    }

    private fun configureAfterInit() {
        MPVLib.setOptionString("save-position-on-quit", "no")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.setOptionString("idle", "yes")
        MPVLib.setPropertyBoolean("pause", true)
        applyOfflinePlaybackMode()
        logSuperResolutionMpvOption()
    }

    private fun shouldUseHardwareDecode(): Boolean {
        return options.enableHardwareDecode
    }

    private fun resolveHardwareDecodeMode(): String {
        return when {
            // mediacodec_embed 只能显示 mediacodec 直出的帧：回退到 copy 或软解都会黑屏
            usesEmbeddedVideoOutput -> HWDEC_MEDIACODEC_DIRECT
            shouldUseHardwareDecode() -> options.hardwareDecodeMode.ifBlank { DEFAULT_HARDWARE_DECODE_MODE }
            else -> "no"
        }
    }

    /**
     * `gpu-next` 需要 GLES 3.0+，Mali-400/450 一类只有 GLES 2.0 的盒子上 libplacebo 直接初始化失败；
     * 借 mpv 的 `--vo` 优先级列表在这种设备上自动退回 `gpu`。
     */
    private fun resolveVideoOutputMode(): String {
        val mode = options.mpvVideoOutput.trim().ifBlank { DEFAULT_VIDEO_OUTPUT }
        return if (mode == GPU_NEXT_VIDEO_OUTPUT) "$GPU_NEXT_VIDEO_OUTPUT,$DEFAULT_VIDEO_OUTPUT" else mode
    }

    private fun applyNetworkOptions() {
        runCatching {
            options.userAgent?.let { MPVLib.setOptionString("user-agent", it) }
            MPVLib.setOptionString(
                "referrer",
                options.playbackRefererFor(currentVideoUrl, currentAudioUrl).orEmpty()
            )
        }.onFailure {
            logger.warn(it) { "Failed to apply MPV network options" }
        }
    }

    private fun configureSuperResolutionShaders() {
        val shaderNames = options.superResolutionType.shaderNames()
        if (shaderNames.isEmpty()) {
            superResolutionShaderPaths = ""
            logger.info { "MPV super resolution disabled: type=${options.superResolutionType}" }
            return
        }
        if (usesEmbeddedVideoOutput) {
            // 直通输出不经过 GPU 渲染管线，shader 无处可挂
            superResolutionShaderPaths = ""
            logger.info { "MPV super resolution ignored: vo=$videoOutput renders without the GPU pipeline" }
            return
        }

        val shaderDir = copyShaderAssets(shaderNames) ?: run {
            superResolutionShaderPaths = ""
            logger.warn { "MPV super resolution inactive: failed to prepare shaders for ${options.superResolutionType}" }
            return
        }
        val shaderPaths = shaderNames.joinToString(SHADER_LIST_DELIMITER) { shaderName ->
            File(shaderDir, shaderName).absolutePath
        }
        superResolutionShaderPaths = shaderPaths
        MPVLib.setOptionString("glsl-shaders", shaderPaths)
        logger.info {
            "MPV super resolution configured: type=${options.superResolutionType}, " +
                    "shaderCount=${shaderNames.size}, shaders=${shaderNames.joinToString()}, paths=$shaderPaths"
        }
    }

    private fun copyShaderAssets(shaderNames: List<String>): File? {
        val shaderDir = context.getDir("mpv_shaders", Context.MODE_PRIVATE).also { it.mkdirs() }
        shaderNames.forEach { shaderName ->
            val target = File(shaderDir, shaderName)
            runCatching {
                context.assets.open("$SHADER_ASSET_DIR/$shaderName").use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }.onFailure { error ->
                logger.warn(error) { "Failed to copy MPV shader asset: $shaderName" }
                return null
            }
        }
        return shaderDir
    }

    private fun logSuperResolutionMpvOption() {
        val property = runCatching {
            MPVLib.getPropertyString("glsl-shaders")
        }.getOrNull()
        logger.info {
            "MPV super resolution option: type=${options.superResolutionType}, " +
                    "configuredPaths=${superResolutionShaderPaths.ifBlank { "<empty>" }}, " +
                    "mpvProperty=${property?.ifBlank { "<empty>" } ?: "<unavailable>"}"
        }
    }

    private fun applyOfflinePlaybackMode() {
        runMpv("apply offline playback mode") {
            if (offlinePlaybackMode) {
                // 直通输出下软解帧无法显示，离线兼容模式也必须保留 mediacodec 直出
                val offlineHwdec = if (usesEmbeddedVideoOutput) HWDEC_MEDIACODEC_DIRECT else "no"
                setOptionString("hwdec", offlineHwdec)
                setOptionString("glsl-shaders", "")
                logger.info { "MPV offline playback compatibility enabled: hwdec=$offlineHwdec, glsl-shaders=<empty>" }
            } else {
                setOptionString("hwdec", resolveHardwareDecodeMode())
                setOptionString("glsl-shaders", superResolutionShaderPaths)
                logger.info {
                    "MPV offline playback compatibility disabled: hwdec=${resolveHardwareDecodeMode()}, " +
                        "glsl-shaders=${superResolutionShaderPaths.ifBlank { "<empty>" }}"
                }
            }
        }
    }

    private fun logSuperResolutionState(reason: String) {
        val videoWidth = _videoWidth
        val videoHeight = _videoHeight
        val outputWidth = surfaceWidth
        val outputHeight = surfaceHeight
        if (videoWidth <= 0 || videoHeight <= 0 || outputWidth <= 0 || outputHeight <= 0) return

        val requested = options.superResolutionType != SuperResolutionType.Disable
        val shaderConfigured = superResolutionShaderPaths.isNotBlank()
        val scaleX = outputWidth.toDouble() / videoWidth.toDouble()
        val scaleY = outputHeight.toDouble() / videoHeight.toDouble()
        val upscalingLikely = scaleX > SUPER_RESOLUTION_UPSCALE_THRESHOLD &&
                scaleY > SUPER_RESOLUTION_UPSCALE_THRESHOLD
        val state = when {
            !requested -> "disabled"
            !shaderConfigured -> "requested_but_shader_not_configured"
            upscalingLikely -> "likely_active"
            else -> "configured_not_upscaling"
        }
        val key = "$state|${options.superResolutionType}|${videoWidth}x$videoHeight|${outputWidth}x$outputHeight"
        if (lastSuperResolutionStateLogKey == key) return
        lastSuperResolutionStateLogKey = key

        logger.info {
            "MPV super resolution state: reason=$reason, state=$state, " +
                    "type=${options.superResolutionType}, shaderConfigured=$shaderConfigured, " +
                    "source=${videoWidth}x$videoHeight, surface=${outputWidth}x$outputHeight, " +
                    "scale=${scaleX.formatScale()}x${scaleY.formatScale()}, " +
                    "threshold=$SUPER_RESOLUTION_UPSCALE_THRESHOLD"
        }
    }

    private fun observePlaybackProperties() {
        listOf(
            ObservedProperty("time-pos/full", MpvFormat.MPV_FORMAT_DOUBLE),
            ObservedProperty("duration/full", MpvFormat.MPV_FORMAT_DOUBLE),
            ObservedProperty("pause", MpvFormat.MPV_FORMAT_FLAG),
            ObservedProperty("paused-for-cache", MpvFormat.MPV_FORMAT_FLAG),
            ObservedProperty("eof-reached", MpvFormat.MPV_FORMAT_FLAG),
            ObservedProperty("idle-active", MpvFormat.MPV_FORMAT_FLAG),
            ObservedProperty("seekable", MpvFormat.MPV_FORMAT_FLAG),
            ObservedProperty("cache-buffering-state", MpvFormat.MPV_FORMAT_INT64),
            ObservedProperty("demuxer-cache-time", MpvFormat.MPV_FORMAT_DOUBLE),
            ObservedProperty("frame-drop-count", MpvFormat.MPV_FORMAT_INT64),
            ObservedProperty("video-params/w", MpvFormat.MPV_FORMAT_INT64),
            ObservedProperty("video-params/h", MpvFormat.MPV_FORMAT_INT64),
            ObservedProperty("dwidth", MpvFormat.MPV_FORMAT_INT64),
            ObservedProperty("dheight", MpvFormat.MPV_FORMAT_INT64),
            ObservedProperty("estimated-vf-fps", MpvFormat.MPV_FORMAT_DOUBLE),
            ObservedProperty("container-fps", MpvFormat.MPV_FORMAT_DOUBLE),
            ObservedProperty("video-codec", MpvFormat.MPV_FORMAT_STRING),
            ObservedProperty("audio-codec", MpvFormat.MPV_FORMAT_STRING),
            ObservedProperty("hwdec-current", MpvFormat.MPV_FORMAT_STRING),
        ).forEach { property ->
            runCatching {
                MPVLib.observeProperty(property.name, property.format)
            }.onFailure {
                logger.debug { "Failed to observe MPV property ${property.name}: ${it.message}" }
            }
        }
    }

    private fun loadMedia() {
        val url = currentVideoUrl ?: currentAudioUrl
        if (url.isNullOrBlank()) {
            dispatchError(Exception("MPV 播放地址为空"))
            return
        }

        applyNetworkOptions()
        applyOfflinePlaybackMode()
        val videoUrl = url.toMpvMediaUrl()
        val audioUrl = externalAudioUrl()?.toMpvMediaUrl()

        // 新一轮加载：重置上一媒体的所有瞬态状态
        stopProgressUpdates()
        awaitingStartFile = true
        endDispatchedForCurrentMedia = false
        eofReachedForCurrentMedia = false
        pendingSeekCallbackPosition = null
        recentErrorLogs.clear()
        audioTrackAdded = audioUrl != null
        videoCodec = null
        audioCodec = null
        hwdec = null
        mediaLoading = true
        mediaLoaded = false
        _isPlaying = false
        cacheBufferingPercent = 0
        demuxerCacheAheadMs = 0L
        _currentPosition = 0L
        _duration = 0L
        voFrameDropCount = 0L
        frameDropBaselineValid = false
        overloadedSampleCount = 0
        decoderOverloadReported = false

        runMpv("load media") {
            setPropertyBoolean("pause", true)
            command(buildLoadFileCommand(videoUrl, audioUrl))
        }
    }

    private fun addAudioTrackIfNeeded() {
        if (audioTrackAdded) return
        val normalizedAudioUrl = externalAudioUrl()
            ?: return

        audioTrackAdded = true
        val audioUrl = normalizedAudioUrl.toMpvMediaUrl()
        runMpv("add audio") {
            command(arrayOf("audio-add", audioUrl, "select"))
        }
    }

    private fun externalAudioUrl(): String? {
        val videoUrl = currentVideoUrl?.takeIf { it.isNotBlank() } ?: return null
        return currentAudioUrl?.takeIf { it.isNotBlank() && it != videoUrl }
    }

    /**
     * 可选地把 B 站 CDN 的 HTTPS 地址改写为 HTTP（[VideoPlayerOptions.mpvPreferHttpForCdn]，
     * 或系统证书导出失败时的兜底，见 [configureTls]）。明文传输会暴露带签名的播放地址与 Referer/UA。
     */
    private fun String.toMpvMediaUrl(): String {
        if (!preferHttpForCdn) return this
        val uri = runCatching { URI(this) }.getOrNull() ?: return this
        val scheme = uri.scheme ?: return this
        val host = uri.host ?: return this
        if (!scheme.equals("https", ignoreCase = true) || !host.isBiliMediaHost()) {
            return this
        }

        val rewrittenUrl = buildString {
            append("http://")
            append(uri.rawAuthority ?: host)
            uri.rawPath?.let { append(it) }
            uri.rawQuery?.let {
                append('?')
                append(it)
            }
            uri.rawFragment?.let {
                append('#')
                append(it)
            }
        }
        logger.info { "Rewrite MPV media URL to HTTP for CDN host: $host" }
        return rewrittenUrl
    }

    private fun String.isBiliMediaHost(): Boolean {
        val host = lowercase()
        return host == "bilivideo.com" ||
                host.endsWith(".bilivideo.com") ||
                host == "bilivideo.cn" ||
                host.endsWith(".bilivideo.cn") ||
                host.endsWith(".akamaized.net")
    }

    private fun buildLoadFileCommand(videoUrl: String, audioUrl: String?): Array<String> {
        return if (audioUrl == null) {
            arrayOf("loadfile", videoUrl, "replace")
        } else {
            arrayOf(
                "loadfile",
                videoUrl,
                "replace",
                "-1",
                "audio-file=${audioUrl.toMpvFixedLengthOptionValue()}"
            )
        }
    }

    private fun String.toMpvFixedLengthOptionValue(): String {
        return "%${toByteArray(Charsets.UTF_8).size}%$this"
    }

    private fun hasAudioTrack(): Boolean {
        if (!mpvInitialized || released) return false
        return runCatching {
            val trackCount = MPVLib.getPropertyInt("track-list/count") ?: return@runCatching false
            (0 until trackCount).any { index ->
                MPVLib.getPropertyString("track-list/$index/type") == "audio"
            }
        }.getOrDefault(false)
    }

    // ------------------------------------------------------------------------------------------
    // 事件处理（主线程）
    // ------------------------------------------------------------------------------------------

    private fun handleEvent(eventId: Int) {
        if (released) return
        when (eventId) {
            MpvEvent.MPV_EVENT_START_FILE -> handleStartFile()
            MpvEvent.MPV_EVENT_FILE_LOADED -> handleFileLoaded()
            MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> handlePlaybackRestart()
            MpvEvent.MPV_EVENT_END_FILE -> handleEndFile()
        }
    }

    private fun handleStartFile() {
        awaitingStartFile = false
        endDispatchedForCurrentMedia = false
        eofReachedForCurrentMedia = false
        dispatchBuffering()
    }

    private fun handleFileLoaded() {
        mediaLoading = false
        mediaLoaded = true
        if (audioTrackAdded && externalAudioUrl() != null && !hasAudioTrack()) {
            logger.warn { "MPV did not expose the loadfile audio track, falling back to audio-add" }
            audioTrackAdded = false
        }
        addAudioTrackIfNeeded()
        refreshVideoInfo()

        val initialSeekPosition = pendingSeekPosition
        if (initialSeekPosition > 0L) {
            clearPendingSeekPosition()
            seekTo(initialSeekPosition)
        }

        dispatchReady()
        if (playWhenReady) {
            start()
        }
    }

    private fun handlePlaybackRestart() {
        pendingSeekCallbackPosition?.let { position ->
            pendingSeekCallbackPosition = null
            mPlayerEventListener?.onSeeked(position)
        }
        dispatchProgress()
    }

    /**
     * mpv-android 的 JNI 只回传事件 ID，拿不到 END_FILE 的 reason/error。
     * 区分“正常结束”与“出错”的依据：观察到 eof-reached，或结束位置已接近时长；
     * 加载阶段就结束、直播（无时长）中途结束都视为错误，交给上层重试/兜底。
     */
    private fun handleEndFile() {
        if (awaitingStartFile) {
            logger.debug { "Ignore END_FILE of the replaced/stopped media" }
            return
        }
        if (endDispatchedForCurrentMedia) return
        endDispatchedForCurrentMedia = true

        val wasLoading = mediaLoading && !mediaLoaded
        mediaLoading = false
        stopProgressUpdates()
        updatePlayingState(false)
        clearPendingSeekPosition()

        val reachedEnd = eofReachedForCurrentMedia ||
            (_duration > 0L && _currentPosition >= _duration - END_OF_FILE_TOLERANCE_MS)
        if (!wasLoading && reachedEnd) {
            mPlayerEventListener?.onEnd()
            return
        }

        val detail = recentErrorLogs.lastOrNull()
        val message = buildString {
            append(if (wasLoading) "MPV 打开媒体失败" else "MPV 播放中断")
            append("（position=${_currentPosition}ms, duration=${_duration}ms）")
            detail?.let { append(": ").append(it) }
        }
        logger.error { message }
        mPlayerEventListener?.onError(Exception(message))
    }

    private fun handlePropertyNone(property: String) {
        if (released) return
        if (property == "idle-active") {
            dispatchIdle()
        }
    }

    private fun handlePropertyLong(property: String, value: Long) {
        if (released) return
        when (property) {
            "cache-buffering-state" -> {
                cacheBufferingPercent = value.toInt().coerceIn(0, 100)
                dispatchProgress()
            }
            "frame-drop-count" -> voFrameDropCount = value
            "video-params/w", "dwidth" -> updateVideoSize(width = value.toInt())
            "video-params/h", "dheight" -> updateVideoSize(height = value.toInt())
        }
    }

    private fun handlePropertyDouble(property: String, value: Double) {
        if (released) return
        when (property) {
            "time-pos/full" -> {
                _currentPosition = secondsToMillis(value)
                dispatchProgress()
            }
            "duration/full" -> {
                _duration = secondsToMillis(value)
                dispatchProgress()
            }
            "demuxer-cache-time" -> {
                demuxerCacheAheadMs = secondsToMillis(value)
            }
            "estimated-vf-fps", "container-fps" -> updateFrameRate(value.toFloat())
        }
    }

    private fun handlePropertyBoolean(property: String, value: Boolean) {
        if (released) return
        when (property) {
            "pause" -> updatePlayingState(!value && mediaLoaded)
            "paused-for-cache" -> {
                if (value) {
                    dispatchBuffering()
                } else if (_isPlaying) {
                    dispatchPlay()
                }
            }
            "eof-reached" -> if (value) {
                eofReachedForCurrentMedia = true
                handleEndFile()
            }
            "idle-active" -> if (value) dispatchIdle()
            "seekable" -> {
                _isSeekable = value
                mPlayerEventListener?.onSeekableChanged(value)
            }
        }
    }

    private fun handlePropertyString(property: String, value: String) {
        if (released) return
        when (property) {
            "time-pos/full" -> value.toDoubleOrNull()?.let { handlePropertyDouble(property, it) }
            "duration/full" -> value.toDoubleOrNull()?.let { handlePropertyDouble(property, it) }
            "demuxer-cache-time" -> value.toDoubleOrNull()?.let { handlePropertyDouble(property, it) }
            "video-codec" -> videoCodec = value
            "audio-codec" -> audioCodec = value
            "hwdec-current" -> hwdec = value
        }
    }

    private fun recordErrorLog(line: String) {
        if (line.isBlank()) return
        if (recentErrorLogs.size >= MAX_RECENT_ERROR_LOGS) recentErrorLogs.removeFirst()
        recentErrorLogs.addLast(line)
    }

    private fun refreshVideoInfo() {
        if (!mpvInitialized || released) return
        runCatching {
            val width = MPVLib.getPropertyInt("video-params/w") ?: MPVLib.getPropertyInt("dwidth") ?: 0
            val height = MPVLib.getPropertyInt("video-params/h") ?: MPVLib.getPropertyInt("dheight") ?: 0
            updateVideoSize(width = width, height = height)
            MPVLib.getPropertyDouble("estimated-vf-fps")
                ?: MPVLib.getPropertyDouble("container-fps")
        }.onSuccess { fps ->
            fps?.let { updateFrameRate(it.toFloat()) }
        }.onFailure {
            logger.debug { "Failed to refresh MPV video info: ${it.message}" }
        }
    }

    private fun updateVideoSize(width: Int? = null, height: Int? = null) {
        val nextWidth = width?.takeIf { it > 0 } ?: _videoWidth
        val nextHeight = height?.takeIf { it > 0 } ?: _videoHeight
        if (nextWidth == _videoWidth && nextHeight == _videoHeight) return

        _videoWidth = nextWidth
        _videoHeight = nextHeight

        if (_videoWidth > 0 && _videoHeight > 0) {
            mPlayerEventListener?.onVideoSizeChanged(_videoWidth, _videoHeight)
            logSuperResolutionState("video-size")
        }
    }

    private fun updateFrameRate(frameRate: Float) {
        val usableFrameRate = frameRate.takeIf { it.isFinite() && it > 0f }
        if (videoFrameRate.isSameFrameRate(usableFrameRate)) return

        videoFrameRate = usableFrameRate
        mPlayerEventListener?.onVideoFrameRateChanged(usableFrameRate)
        applySurfaceFrameRate()
    }

    private fun updatePlayingState(isPlaying: Boolean) {
        if (_isPlaying == isPlaying) return
        _isPlaying = isPlaying
        if (isPlaying) {
            dispatchPlay()
            startProgressUpdates()
        } else {
            stopProgressUpdates()
            mPlayerEventListener?.onPause()
            dispatchProgress()
        }
    }

    /**
     * 每 [DROP_SAMPLE_EVERY_PROGRESS_TICKS] 个进度周期比较一次 vo 丢帧计数与按帧率推算的应显示帧数；
     * 连续 [OVERLOAD_SAMPLES_REQUIRED] 个窗口丢帧占比 ≥ 50% 视为解码过载，上报一次供上层建议降低清晰度。
     */
    private fun sampleFrameDrops() {
        if (!_isPlaying || !mediaLoaded || released) {
            frameDropBaselineValid = false
            overloadedSampleCount = 0
            return
        }
        val dropCount = voFrameDropCount
        if (frameDropBaselineValid) {
            val dropped = (dropCount - lastSampledFrameDropCount).coerceAtLeast(0L).toInt()
            val windowMs = PROGRESS_UPDATE_INTERVAL_MS * DROP_SAMPLE_EVERY_PROGRESS_TICKS
            val fps = videoFrameRate?.takeIf { it > 1f } ?: DEFAULT_ASSUMED_FRAME_RATE
            val expectedFrames = (fps * windowMs / 1000f).toInt().coerceAtLeast(1)
            val overloaded = dropped * 2 >= expectedFrames
            overloadedSampleCount = if (overloaded) overloadedSampleCount + 1 else 0
            if (overloaded) {
                logger.debug { "MPV dropping frames: $dropped / ~$expectedFrames in last ${windowMs}ms" }
            }
            if (overloadedSampleCount >= OVERLOAD_SAMPLES_REQUIRED && !decoderOverloadReported) {
                decoderOverloadReported = true
                // frame-drop-count 统计的是 VO 丢帧：解码慢和 GPU 渲染慢（超分 shader）都会触发，
                // 上层通过 isSuperResolutionActive 区分该建议关超分还是降清晰度
                logger.warn {
                    "MPV video overloaded: $dropped / ~$expectedFrames frames dropped per ${windowMs}ms " +
                        "(video ${_videoWidth}x${_videoHeight}, hwdec=${hwdec ?: "unknown"}, " +
                        "superResolution=$isSuperResolutionActive)"
                }
                mPlayerEventListener?.onDecoderOverloaded(dropped, expectedFrames)
            }
        }
        lastSampledFrameDropCount = dropCount
        frameDropBaselineValid = true
    }

    private fun computeBufferedPercentage(): Int {
        val durationMs = _duration
        if (durationMs <= 0L) return cacheBufferingPercent.coerceIn(0, 100)
        val bufferedEndMs = _currentPosition.coerceAtLeast(0L) + demuxerCacheAheadMs.coerceAtLeast(0L)
        return (bufferedEndMs * 100L / durationMs).toInt().coerceIn(0, 100)
    }

    private fun dispatchReady() = onMain { mPlayerEventListener?.onReady() }

    private fun dispatchPlay() = onMain { mPlayerEventListener?.onPlay() }

    private fun dispatchBuffering() = onMain { mPlayerEventListener?.onBuffering() }

    private fun dispatchIdle() = onMain { mPlayerEventListener?.onIdle() }

    private fun dispatchProgress() {
        val positionMs = _currentPosition.coerceAtLeast(0L)
        val durationMs = _duration.coerceAtLeast(0L)
        val buffered = computeBufferedPercentage()
        onMain {
            mPlayerEventListener?.onProgress(positionMs, durationMs, buffered)
        }
    }

    private fun dispatchError(error: Exception) = onMain { mPlayerEventListener?.onError(error) }

    private fun startProgressUpdates() {
        mainHandler.removeCallbacks(progressUpdateRunnable)
        mainHandler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdates() {
        mainHandler.removeCallbacks(progressUpdateRunnable)
    }

    private fun runMpv(action: String, block: MPVLib.() -> Unit) {
        if (!mpvInitialized || released) return
        runCatching {
            MPVLib.block()
        }.onFailure {
            logger.error(it) { "MPV $action failed" }
            dispatchError(Exception("MPV $action failed", it))
        }
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun secondsToMillis(seconds: Double): Long {
        return if (seconds.isFinite() && seconds > 0.0) {
            (seconds * 1000.0).toLong()
        } else {
            0L
        }
    }

    private fun Float?.isSameFrameRate(other: Float?): Boolean {
        return when {
            this == null && other == null -> true
            this == null || other == null -> false
            else -> abs(this - other) <= FRAME_RATE_EPSILON
        }
    }

    private fun SuperResolutionType.shaderNames(): List<String> =
        when (this) {
            SuperResolutionType.Disable -> emptyList()
            SuperResolutionType.EfficiencyAnime -> ANIME4K_MODE_A_FAST_SHADERS
            SuperResolutionType.EfficiencyFsrcnnx -> FSRCNNX_FAST_SHADERS
            SuperResolutionType.QualityAnime -> ANIME4K_MODE_A_HQ_SHADERS
            SuperResolutionType.QualityFsrcnnx -> FSRCNNX_QUALITY_SHADERS
        }

    private fun Double.formatScale(): String = String.format(Locale.US, "%.3f", this)

    private data class ObservedProperty(
        val name: String,
        val format: Int
    )

    companion object {
        private const val DEFAULT_VIDEO_OUTPUT = "gpu"
        private const val GPU_NEXT_VIDEO_OUTPUT = "gpu-next"
        private const val MEDIACODEC_EMBED_VIDEO_OUTPUT = "mediacodec_embed"
        private const val DEFAULT_GPU_CONTEXT = "android"
        private const val DEFAULT_AUDIO_OUTPUT_DEVICES = "audiotrack,opensles"
        private const val DEFAULT_VIDEO_SYNC = "audio"
        private const val DEFAULT_HARDWARE_DECODE_MODE = "mediacodec,mediacodec-copy"
        private const val HWDEC_MEDIACODEC_DIRECT = "mediacodec"
        private const val DEFAULT_HARDWARE_DECODE_CODECS = "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1"
        private const val CURL_CONNECT_TIMEOUT_SECS = 15

        /** mpv 默认自动加载的内置脚本；全部由应用自己的 UI 替代 */
        private val BUILTIN_SCRIPT_OPTIONS = listOf(
            "osc" to "no",
            "load-stats-overlay" to "no",
            "load-console" to "no",
            "load-auto-profiles" to "no",
            "load-select" to "no",
            "load-commands" to "no",
            "load-positioning" to "no",
        )
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        private const val DROP_SAMPLE_EVERY_PROGRESS_TICKS = 4
        private const val OVERLOAD_SAMPLES_REQUIRED = 3
        private const val DEFAULT_ASSUMED_FRAME_RATE = 30f
        private const val FRAME_RATE_EPSILON = 0.001f
        private const val SUPER_RESOLUTION_UPSCALE_THRESHOLD = 1.2
        private const val END_OF_FILE_TOLERANCE_MS = 2_000L
        private const val MAX_RECENT_ERROR_LOGS = 3
        private const val MPV_LOG_LEVEL_ERROR = 20
        private const val SHADER_ASSET_DIR = "shaders"
        private const val SHADER_LIST_DELIMITER = ":"
        private const val ANIME4K_CLAMP_HIGHLIGHTS = "Anime4K_Clamp_Highlights.glsl"
        private const val ANIME4K_RESTORE_CNN_M = "Anime4K_Restore_CNN_M.glsl"
        private const val ANIME4K_RESTORE_CNN_VL = "Anime4K_Restore_CNN_VL.glsl"
        private const val ANIME4K_UPSCALE_CNN_X2_S = "Anime4K_Upscale_CNN_x2_S.glsl"
        private const val ANIME4K_UPSCALE_CNN_X2_M = "Anime4K_Upscale_CNN_x2_M.glsl"
        private const val ANIME4K_UPSCALE_CNN_X2_VL = "Anime4K_Upscale_CNN_x2_VL.glsl"
        private const val ANIME4K_AUTO_DOWNSCALE_PRE_X2 = "Anime4K_AutoDownscalePre_x2.glsl"
        private const val ANIME4K_AUTO_DOWNSCALE_PRE_X4 = "Anime4K_AutoDownscalePre_x4.glsl"
        private const val FSRCNNX_FAST = "FSRCNNX_x2_8-0-4-1.glsl"
        private const val FSRCNNX_QUALITY = "FSRCNNX_x2_16-0-4-1.glsl"
        private val ANIME4K_MODE_A_FAST_SHADERS = listOf(
            ANIME4K_CLAMP_HIGHLIGHTS,
            ANIME4K_RESTORE_CNN_M,
            ANIME4K_UPSCALE_CNN_X2_M,
            ANIME4K_AUTO_DOWNSCALE_PRE_X2,
            ANIME4K_AUTO_DOWNSCALE_PRE_X4,
            ANIME4K_UPSCALE_CNN_X2_S
        )
        private val ANIME4K_MODE_A_HQ_SHADERS = listOf(
            ANIME4K_CLAMP_HIGHLIGHTS,
            ANIME4K_RESTORE_CNN_VL,
            ANIME4K_UPSCALE_CNN_X2_VL,
            ANIME4K_AUTO_DOWNSCALE_PRE_X2,
            ANIME4K_AUTO_DOWNSCALE_PRE_X4,
            ANIME4K_UPSCALE_CNN_X2_M
        )
        private val FSRCNNX_FAST_SHADERS = listOf(FSRCNNX_FAST)
        private val FSRCNNX_QUALITY_SHADERS = listOf(FSRCNNX_QUALITY)

        /** 进程内唯一持有 mpv 句柄的实例 */
        private val instanceLock = Any()
        private var activeInstance: MpvMediaPlayer? = null
    }
}
