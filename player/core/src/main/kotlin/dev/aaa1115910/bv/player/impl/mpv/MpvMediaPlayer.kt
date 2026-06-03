package dev.aaa1115910.bv.player.impl.mpv

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.util.formatHourMinSec
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvEvent
import `is`.xyz.mpv.MPVLib.MpvFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.abs

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
    private var headers: Map<String, String> = emptyMap()
    private val proxyTokens = mutableSetOf<String>()

    private var loadRequested = false
    private var mediaLoaded = false
    private var audioTrackAdded = false
    private var playWhenReady = false
    private var surfaceAttached = false
    private var released = false
    private var suppressNextEndEvent = false
    private var endDispatchedForCurrentMedia = false
    private var pendingSeekCallbackPosition: Long? = null

    private var _isPlaying = false
    private var _isSeekable = true
    private var _currentPosition = 0L
    private var _duration = 0L
    private var _bufferedPercentage = 0
    private var _videoWidth = 0
    private var _videoHeight = 0
    private var videoFrameRate: Float? = null
    private var videoCodec: String? = null
    private var audioCodec: String? = null
    private var hwdec: String? = null

    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            dispatchProgress()
            if (_isPlaying && !released) {
                mainHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private val eventObserver = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) = handlePropertyNone(property)

        override fun eventProperty(property: String, value: Long) = handlePropertyLong(property, value)

        override fun eventProperty(property: String, value: Double) = handlePropertyDouble(property, value)

        override fun eventProperty(property: String, value: Boolean) = handlePropertyBoolean(property, value)

        override fun eventProperty(property: String, value: String) = handlePropertyString(property, value)

        override fun event(eventId: Int) = handleEvent(eventId)
    }

    init {
        initPlayer()
    }

    override fun initPlayer() {
        logger.info { "Initializing MPV player" }
        try {
            MpvLibsInstaller.loadNativeLibs(context)
            MPVLib.create(context.applicationContext)
            configureBeforeInit()
            MPVLib.init()
            mpvInitialized = true
            configureAfterInit()
            MPVLib.addObserver(eventObserver)
            observePlaybackProperties()

            logger.info { "MPV player initialized successfully" }
        } catch (error: Throwable) {
            initializationError = Exception(
                "MPV 播放器不可用：请在播放器内核选择 MPV 时下载官方 mpv-android 组件",
                error
            )
            logger.error(error) { "Failed to initialize MPV player" }
        }
    }

    override fun setHeader(headers: Map<String, String>) {
        this.headers = headers
        applyNetworkOptions()
    }

    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        val hasMediaToReplace = mediaLoaded
        currentVideoUrl = videoUrl
        currentAudioUrl = audioUrl
        loadRequested = false
        mediaLoaded = false
        audioTrackAdded = false
        if (hasMediaToReplace) suppressNextEndEvent = true
        pendingSeekCallbackPosition = null
        stopProgressUpdates()
        _isPlaying = false
        _currentPosition = 0L
        _duration = 0L
        _bufferedPercentage = 0
        videoCodec = null
        audioCodec = null
        hwdec = null
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
        runMpv("start") {
            setPropertyBoolean("pause", false)
        }
        if (mediaLoaded) updatePlayingState(true)
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
        suppressNextEndEvent = true
        stopProgressUpdates()
        runMpv("stop") {
            command(arrayOf("stop"))
        }
        mediaLoaded = false
        updatePlayingState(false)
        dispatchIdle()
    }

    override fun reset() {
        stop()
        clearProxyTokens()
        currentVideoUrl = null
        currentAudioUrl = null
        loadRequested = false
        audioTrackAdded = false
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
            }
            if (surfaceAttached) {
                detachSurface()
            }
            if (mpvInitialized) {
                MPVLib.command(arrayOf("stop"))
                MPVLib.destroy()
            }
        } catch (error: Throwable) {
            logger.error(error) { "Error releasing MPV player" }
        } finally {
            clearProxyTokens()
            mpvInitialized = false
            mPlayerEventListener = null
        }
    }

    override val currentPosition: Long
        get() = _currentPosition

    override val duration: Long
        get() = _duration

    override val bufferedPercentage: Int
        get() = _bufferedPercentage

    override fun setOptions() {
        playWhenReady = true
        if (mediaLoaded) start()
    }

    override var speed: Float
        get() = if (mpvInitialized) {
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
            val version = if (mpvInitialized) {
                runCatching { MPVLib.getPropertyString("mpv-version") }.getOrNull()
            } else {
                null
            } ?: "not initialized"
            return """
                player: $version
                time: ${currentPosition.formatHourMinSec()} / ${duration.formatHourMinSec()}
                buffered: $bufferedPercentage%
                resolution: $videoWidth x $videoHeight
                video fps: ${videoFrameRate ?: 0f}
                video codec: ${videoCodec ?: "unknown"}
                audio codec: ${audioCodec ?: "unknown"}
                hwdec: ${hwdec ?: "unknown"}
                speed: $speed
            """.trimIndent()
        }

    override val videoWidth: Int
        get() = _videoWidth

    override val videoHeight: Int
        get() = _videoHeight

    fun attachSurface(surface: Surface, width: Int, height: Int) {
        if (released) return
        runMpv("attach surface") {
            attachSurface(surface)
            surfaceAttached = true
            setOptionString("force-window", "yes")
            setPropertyString("vo", MPV_VIDEO_OUTPUT)
            updateSurfaceSize(width, height)
        }

        if (loadRequested && !mediaLoaded) {
            loadMedia()
        }
    }

    fun updateSurfaceSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        runMpv("update surface size") {
            setPropertyString("android-surface-size", "${width}x$height")
        }
    }

    fun detachSurface() {
        if (!surfaceAttached) return
        runMpv("detach surface") {
            setPropertyString("vo", "null")
            setOptionString("force-window", "no")
            detachSurface()
        }
        surfaceAttached = false
    }

    private fun configureBeforeInit() {
        val configDir = context.getDir("mpv", Context.MODE_PRIVATE)
        val cacheDir = context.cacheDir.resolve("mpv").also { it.mkdirs() }

        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir.absolutePath)
        MPVLib.setOptionString("gpu-shader-cache-dir", cacheDir.absolutePath)
        MPVLib.setOptionString("icc-cache-dir", cacheDir.absolutePath)
        MPVLib.setOptionString("profile", "fast")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")
        MPVLib.setOptionString("vo", MPV_VIDEO_OUTPUT)
        MPVLib.setOptionString("ao", options.audioOutputDevices.ifBlank { DEFAULT_AUDIO_OUTPUT_DEVICES })
        MPVLib.setOptionString("ytdl", "no")
        MPVLib.setOptionString("audio-set-media-role", "yes")
        MPVLib.setOptionString("video-sync", options.videoSync.ifBlank { DEFAULT_VIDEO_SYNC })
        options.autoSync.trim().takeIf { it.isNotBlank() && it != "0" }?.let { autoSync ->
            MPVLib.setOptionString("autosync", autoSync)
        }
        MPVLib.setOptionString("hwdec", resolveHardwareDecodeMode())
        MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")

        val cacheBytes = if (options.expandBuffer) {
            EXPANDED_DEMUXER_CACHE_BYTES
        } else if (options.isLive) {
            LIVE_DEMUXER_CACHE_BYTES
        } else {
            DEFAULT_DEMUXER_CACHE_BYTES
        }
        MPVLib.setOptionString("demuxer-max-bytes", cacheBytes.toString())
        MPVLib.setOptionString("demuxer-max-back-bytes", cacheBytes.toString())

        applyNetworkOptions()
    }

    private fun configureAfterInit() {
        MPVLib.setOptionString("save-position-on-quit", "no")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.setOptionString("idle", "yes")
        MPVLib.setPropertyBoolean("pause", true)
    }

    private fun shouldUseHardwareDecode(): Boolean {
        return options.enableHardwareDecode && !isAndroidEmulator()
    }

    private fun resolveHardwareDecodeMode(): String {
        return if (shouldUseHardwareDecode()) {
            options.hardwareDecodeMode.ifBlank { DEFAULT_HARDWARE_DECODE_MODE }
        } else {
            "no"
        }
    }

    private fun isAndroidEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk", ignoreCase = true) ||
                Build.MODEL.contains("Emulator", ignoreCase = true) ||
                Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
                Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
                Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
                Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
                Build.PRODUCT.contains("sdk", ignoreCase = true) ||
                Build.DEVICE.contains("emulator", ignoreCase = true) ||
                Build.DEVICE.contains("emu", ignoreCase = true) ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                Build.PRODUCT == "google_sdk"
    }

    private fun applyNetworkOptions() {
        runCatching {
            options.userAgent?.let { MPVLib.setOptionString("user-agent", it) }
            options.referer?.let { MPVLib.setOptionString("referrer", it) }

            val headerFields = buildList {
                headers.forEach { (key, value) ->
                    if (key.isNotBlank() && value.isNotBlank()) add("$key: $value")
                }
            }
            if (headerFields.isNotEmpty()) {
                MPVLib.setOptionString("http-header-fields", headerFields.joinToString(","))
            }
        }.onFailure {
            logger.warn(it) { "Failed to apply MPV network options" }
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
        val previousProxyTokens = proxyTokens.toSet()
        val proxiedUrl = createProxyUrl(url)
        audioTrackAdded = false
        videoCodec = null
        audioCodec = null
        hwdec = null
        mediaLoaded = true
        _bufferedPercentage = 0
        _currentPosition = 0L
        _duration = 0L

        runMpv("load media") {
            setPropertyBoolean("pause", !playWhenReady)
            command(arrayOf("loadfile", proxiedUrl, "replace"))
        }
        clearProxyTokens(previousProxyTokens)
    }

    private fun addAudioTrackIfNeeded() {
        if (audioTrackAdded) return
        val normalizedAudioUrl = currentAudioUrl
            ?.takeIf { it.isNotBlank() && it != currentVideoUrl }
            ?: return

        audioTrackAdded = true
        val proxiedAudioUrl = createProxyUrl(normalizedAudioUrl)
        runMpv("add audio") {
            command(arrayOf("audio-add", proxiedAudioUrl, "select"))
        }
    }

    private fun createProxyUrl(url: String): String {
        return runCatching {
            MpvHttpProxyServer.register(
                context = context.applicationContext,
                url = url,
                headers = headers,
                userAgent = options.userAgent,
                referer = options.referer
            )
        }.onSuccess { proxiedUrl ->
            proxiedUrl.token?.let { proxyTokens += it }
        }.onFailure {
            logger.warn(it) { "Failed to create MPV local proxy URL, falling back to original URL" }
        }.getOrNull()?.url ?: url
    }

    private fun clearProxyTokens(tokens: Set<String> = proxyTokens.toSet()) {
        tokens.forEach { token -> MpvHttpProxyServer.unregister(token) }
        proxyTokens.removeAll(tokens)
    }

    private fun handleEvent(eventId: Int) {
        when (eventId) {
            MpvEvent.MPV_EVENT_START_FILE -> dispatchBuffering()
            MpvEvent.MPV_EVENT_FILE_LOADED -> handleFileLoaded()
            MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> handlePlaybackRestart()
            MpvEvent.MPV_EVENT_END_FILE -> handleEndFile()
        }
    }

    private fun handleFileLoaded() {
        suppressNextEndEvent = false
        endDispatchedForCurrentMedia = false
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
            onMain {
                mPlayerEventListener?.onSeeked(position)
            }
        }
        dispatchProgress()
    }

    private fun handleEndFile() {
        if (suppressNextEndEvent) {
            suppressNextEndEvent = false
            return
        }
        if (endDispatchedForCurrentMedia) return

        endDispatchedForCurrentMedia = true
        stopProgressUpdates()
        updatePlayingState(false)
        onMain {
            mPlayerEventListener?.onEnd()
        }
    }

    private fun handlePropertyNone(property: String) {
        if (property == "idle-active") {
            dispatchIdle()
        }
    }

    private fun handlePropertyLong(property: String, value: Long) {
        when (property) {
            "cache-buffering-state" -> {
                _bufferedPercentage = value.toInt().coerceIn(0, 100)
                dispatchProgress()
            }
            "video-params/w", "dwidth" -> updateVideoSize(width = value.toInt())
            "video-params/h", "dheight" -> updateVideoSize(height = value.toInt())
        }
    }

    private fun handlePropertyDouble(property: String, value: Double) {
        when (property) {
            "time-pos/full" -> {
                _currentPosition = secondsToMillis(value)
                dispatchProgress()
            }
            "duration/full" -> {
                _duration = secondsToMillis(value)
                dispatchProgress()
            }
            "estimated-vf-fps", "container-fps" -> updateFrameRate(value.toFloat())
        }
    }

    private fun handlePropertyBoolean(property: String, value: Boolean) {
        when (property) {
            "pause" -> updatePlayingState(!value && mediaLoaded)
            "paused-for-cache" -> {
                if (value) {
                    dispatchBuffering()
                } else if (_isPlaying) {
                    dispatchPlay()
                }
            }
            "eof-reached" -> if (value) handleEndFile()
            "idle-active" -> if (value) dispatchIdle()
            "seekable" -> {
                _isSeekable = value
                onMain {
                    mPlayerEventListener?.onSeekableChanged(value)
                }
            }
        }
    }

    private fun handlePropertyString(property: String, value: String) {
        when (property) {
            "time-pos/full" -> value.toDoubleOrNull()?.let { handlePropertyDouble(property, it) }
            "duration/full" -> value.toDoubleOrNull()?.let { handlePropertyDouble(property, it) }
            "video-codec" -> videoCodec = value
            "audio-codec" -> audioCodec = value
            "hwdec-current" -> hwdec = value
        }
    }

    private fun refreshVideoInfo() {
        if (!mpvInitialized) return
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
            onMain {
                mPlayerEventListener?.onVideoSizeChanged(_videoWidth, _videoHeight)
            }
        }
    }

    private fun updateFrameRate(frameRate: Float) {
        val usableFrameRate = frameRate.takeIf { it.isFinite() && it > 0f }
        if (videoFrameRate.isSameFrameRate(usableFrameRate)) return

        videoFrameRate = usableFrameRate
        onMain {
            mPlayerEventListener?.onVideoFrameRateChanged(usableFrameRate)
        }
    }

    private fun updatePlayingState(isPlaying: Boolean) {
        if (_isPlaying == isPlaying) return
        _isPlaying = isPlaying
        if (isPlaying) {
            dispatchPlay()
            startProgressUpdates()
        } else {
            stopProgressUpdates()
            onMain {
                mPlayerEventListener?.onPause()
            }
            dispatchProgress()
        }
    }

    private fun dispatchReady() {
        onMain {
            mPlayerEventListener?.onReady()
        }
    }

    private fun dispatchPlay() {
        onMain {
            mPlayerEventListener?.onPlay()
        }
    }

    private fun dispatchBuffering() {
        onMain {
            mPlayerEventListener?.onBuffering()
        }
    }

    private fun dispatchIdle() {
        onMain {
            mPlayerEventListener?.onIdle()
        }
    }

    private fun dispatchProgress() {
        val positionMs = _currentPosition.coerceAtLeast(0L)
        val durationMs = _duration.coerceAtLeast(0L)
        val buffered = _bufferedPercentage.coerceIn(0, 100)
        onMain {
            mPlayerEventListener?.onProgress(positionMs, durationMs, buffered)
        }
    }

    private fun dispatchError(error: Exception) {
        onMain {
            mPlayerEventListener?.onError(error)
        }
    }

    private fun startProgressUpdates() {
        mainHandler.removeCallbacks(progressUpdateRunnable)
        mainHandler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdates() {
        mainHandler.removeCallbacks(progressUpdateRunnable)
    }

    private fun runMpv(action: String, block: MPVLib.() -> Unit) {
        if (!mpvInitialized) return
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

    private data class ObservedProperty(
        val name: String,
        val format: Int
    )

    companion object {
        private const val MPV_VIDEO_OUTPUT = "gpu"
        private const val DEFAULT_AUDIO_OUTPUT_DEVICES = "audiotrack,opensles"
        private const val DEFAULT_VIDEO_SYNC = "audio"
        private const val DEFAULT_HARDWARE_DECODE_MODE = "mediacodec,mediacodec-copy"
        private const val DEFAULT_DEMUXER_CACHE_BYTES = 64L * 1024L * 1024L
        private const val LIVE_DEMUXER_CACHE_BYTES = 32L * 1024L * 1024L
        private const val EXPANDED_DEMUXER_CACHE_BYTES = 256L * 1024L * 1024L
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        private const val FRAME_RATE_EPSILON = 0.001f
    }
}
