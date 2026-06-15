package dev.aaa1115910.bv.player.impl.mpv

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.entity.SuperResolutionType
import dev.aaa1115910.bv.util.formatHourMinSec
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvEvent
import `is`.xyz.mpv.MPVLib.MpvFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URI
import java.util.Locale
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
    private var offlinePlaybackMode = false

    private var loadRequested = false
    private var mediaLoading = false
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
    private val videoOutput = resolveVideoOutputMode()
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var superResolutionShaderPaths = ""
    private var lastSuperResolutionStateLogKey: String? = null

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

    override fun setOfflinePlaybackMode(enabled: Boolean) {
        if (offlinePlaybackMode == enabled) return
        offlinePlaybackMode = enabled
        applyOfflinePlaybackMode()
    }

    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        val hasMediaToReplace = mediaLoaded || mediaLoading
        currentVideoUrl = videoUrl
        currentAudioUrl = audioUrl
        loadRequested = false
        mediaLoading = false
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
        suppressNextEndEvent = true
        stopProgressUpdates()
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
                vo: $videoOutput
                super resolution: ${options.superResolutionType}
                speed: $speed
            """.trimIndent()
        }

    override val videoWidth: Int
        get() = _videoWidth

    override val videoHeight: Int
        get() = _videoHeight

    val usesEmbeddedVideoOutput: Boolean
        get() = videoOutput == MEDIACODEC_EMBED_VIDEO_OUTPUT

    fun attachSurface(surface: Surface, width: Int, height: Int) {
        if (released) return
        runMpv("attach surface") {
            attachSurface(surface)
            surfaceAttached = true
            setOptionString("force-window", "yes")
            setPropertyString("vo", videoOutput)
            updateSurfaceSize(width, height)
        }

        if (loadRequested && !mediaLoaded) {
            loadMedia()
        }
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
        if (!surfaceAttached) return
        runMpv("detach surface") {
            setPropertyString("vo", "null")
            setOptionString("force-window", "no")
            detachSurface()
        }
        surfaceAttached = false
        surfaceWidth = 0
        surfaceHeight = 0
    }

    private fun configureBeforeInit() {
        val configDir = context.getDir("mpv", Context.MODE_PRIVATE)
        val cacheDir = context.cacheDir.resolve("mpv").also { it.mkdirs() }

        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir.absolutePath)
        MPVLib.setOptionString("gpu-shader-cache-dir", cacheDir.absolutePath)
        MPVLib.setOptionString("icc-cache-dir", cacheDir.absolutePath)
        MPVLib.setOptionString("profile", "fast")
        MPVLib.setOptionString("gpu-context", options.mpvGpuContext.ifBlank { DEFAULT_GPU_CONTEXT })
        options.mpvGpuApi.trim().takeIf { it.isNotBlank() }?.let { gpuApi ->
            MPVLib.setOptionString("gpu-api", gpuApi)
        }
        MPVLib.setOptionString("opengl-es", "yes")
        MPVLib.setOptionString("vo", videoOutput)
        MPVLib.setOptionString("ao", options.audioOutputDevices.ifBlank { DEFAULT_AUDIO_OUTPUT_DEVICES })
        MPVLib.setOptionString("ytdl", "no")
        MPVLib.setOptionString("audio-set-media-role", "yes")
        MPVLib.setOptionString("video-sync", options.videoSync.ifBlank { DEFAULT_VIDEO_SYNC })
        options.autoSync.trim().takeIf { it.isNotBlank() && it != "0" }?.let { autoSync ->
            MPVLib.setOptionString("autosync", autoSync)
        }
        MPVLib.setOptionString("hwdec", resolveHardwareDecodeMode())
        MPVLib.setOptionString(
            "hwdec-codecs",
            options.mpvHardwareDecodeCodecs.ifBlank { DEFAULT_HARDWARE_DECODE_CODECS }
        )
        options.mpvVdQueueEnable.trim().takeIf { it.isNotBlank() }?.let { vdQueueEnable ->
            MPVLib.setOptionString("vd-queue-enable", vdQueueEnable)
        }
        configureSuperResolutionShaders()
        options.mpvCache.trim().takeIf { it.isNotBlank() }?.let { cache ->
            MPVLib.setOptionString("cache", cache)
        }

        val cacheBytes = if (options.expandBuffer) {
            EXPANDED_DEMUXER_CACHE_BYTES
        } else if (options.isLive) {
            LIVE_DEMUXER_CACHE_BYTES
        } else {
            DEFAULT_DEMUXER_CACHE_BYTES
        }
        MPVLib.setOptionString(
            "demuxer-max-bytes",
            options.mpvDemuxerMaxBytes.ifBlank { cacheBytes.toString() }
        )
        MPVLib.setOptionString(
            "demuxer-max-back-bytes",
            options.mpvDemuxerMaxBackBytes.ifBlank { cacheBytes.toString() }
        )

        applyNetworkOptions()
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
        return if (shouldUseHardwareDecode()) {
            options.hardwareDecodeMode.ifBlank { DEFAULT_HARDWARE_DECODE_MODE }
        } else {
            "no"
        }
    }

    private fun resolveVideoOutputMode(): String {
        val mode = options.mpvVideoOutput.trim()
        return mode.ifBlank { DEFAULT_VIDEO_OUTPUT }
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

    private fun configureSuperResolutionShaders() {
        val shaderNames = options.superResolutionType.shaderNames()
        if (shaderNames.isEmpty()) {
            superResolutionShaderPaths = ""
            logger.info { "MPV super resolution disabled: type=${options.superResolutionType}" }
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
                setOptionString("hwdec", "no")
                setOptionString("glsl-shaders", "")
                logger.info { "MPV offline playback compatibility enabled: hwdec=no, glsl-shaders=<empty>" }
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
        audioTrackAdded = audioUrl != null
        videoCodec = null
        audioCodec = null
        hwdec = null
        mediaLoading = true
        mediaLoaded = false
        _bufferedPercentage = 0
        _currentPosition = 0L
        _duration = 0L

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

    private fun String.toMpvMediaUrl(): String {
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
        return runCatching {
            val trackCount = MPVLib.getPropertyInt("track-list/count") ?: return@runCatching false
            (0 until trackCount).any { index ->
                MPVLib.getPropertyString("track-list/$index/type") == "audio"
            }
        }.getOrDefault(false)
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
        mediaLoading = false
        mediaLoaded = true
        suppressNextEndEvent = false
        endDispatchedForCurrentMedia = false
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
            onMain {
                mPlayerEventListener?.onSeeked(position)
            }
        }
        dispatchProgress()
    }

    private fun handleEndFile() {
        mediaLoading = false
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
            logSuperResolutionState("video-size")
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
        private const val MEDIACODEC_EMBED_VIDEO_OUTPUT = "mediacodec_embed"
        private const val DEFAULT_GPU_CONTEXT = "android"
        private const val DEFAULT_AUDIO_OUTPUT_DEVICES = "audiotrack,opensles"
        private const val DEFAULT_VIDEO_SYNC = "audio"
        private const val DEFAULT_HARDWARE_DECODE_MODE = "mediacodec,mediacodec-copy"
        private const val DEFAULT_HARDWARE_DECODE_CODECS = "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1"
        private const val DEFAULT_DEMUXER_CACHE_BYTES = 64L * 1024L * 1024L
        private const val LIVE_DEMUXER_CACHE_BYTES = 32L * 1024L * 1024L
        private const val EXPANDED_DEMUXER_CACHE_BYTES = 256L * 1024L * 1024L
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        private const val FRAME_RATE_EPSILON = 0.001f
        private const val SUPER_RESOLUTION_UPSCALE_THRESHOLD = 1.2
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
    }
}
