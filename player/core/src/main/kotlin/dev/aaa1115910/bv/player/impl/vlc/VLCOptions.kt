/*****************************************************************************
 * VLCOptions.kt
 *
 * 基于 VLC 官方配置完全同步
 * Copyright © 2015 VLC authors and VideoLAN
 *
 * 源文件:
 * /Volumes/MyMacData/WorkSpace/vlc-android-master/application/resources/src/main/java/org/videolan/resources/VLCOptions.kt
 *****************************************************************************/

package dev.aaa1115910.bv.player.impl.vlc

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.libvlc.util.VLCUtil
import java.io.File
import java.util.Collections

object VLCOptions {
    private const val TAG = "VLC/VLCConfig"

    const val AOUT_AAUDIO = 0
    const val AOUT_AUDIOTRACK = 1
    const val AOUT_OPENSLES = 2

    const val HW_ACCELERATION_AUTOMATIC = -1
    const val HW_ACCELERATION_DISABLED = 0
    const val HW_ACCELERATION_DECODING = 1
    const val HW_ACCELERATION_FULL = 2

    var audiotrackSessionId = 0
        private set

    /**
     * 获取 LibVLC 启动选项列表
     *
     * 完全同步官方 VLCOptions.libOptions() 实现
     *
     * @param context 应用上下文
     * @param config VLC 配置
     * @return VLC 选项列表
     */
    fun getLibOptions(
        context: Context,
        config: VLCConfig = VLCConfig()
    ): ArrayList<String> {
        // 生成 audio session id（官方实现）
        // generateAudioSessionId() 需要 API 23 (M)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audiotrackSessionId == 0) {
            val audioManager = context.getSystemService<AudioManager>()
            if (audioManager != null) {
                audiotrackSessionId = audioManager.generateAudioSessionId()
            }
        }

        val options = ArrayList<String>(50)

        // Chromecast audio only（官方）
        if (config.castingAudioOnly) {
            options.add("--no-sout-chromecast-video")
        }

        // ========== 音频配置 ==========
        // 音频时间拉伸（官方默认: false）
        options.add(if (config.enableTimeStretching) "--audio-time-stretch" else "--no-audio-time-stretch")

        // ========== 视频解码配置 ==========
        // 去块滤波器（官方默认: -1 自动）
        val deblocking = getDeblocking(config.deblocking)
        options.add("--avcodec-skiploopfilter")
        options.add(deblocking.toString())

        // 跳帧设置（官方默认: false）
        options.add("--avcodec-skip-frame")
        options.add(if (config.enableFrameSkip) "2" else "0")

        options.add("--avcodec-skip-idct")
        options.add(if (config.enableFrameSkip) "2" else "0")

        // ========== 字幕编码 ==========
        options.add("--subsdec-encoding")
        options.add(if (config.subtitleEncoding.isNotEmpty()) config.subtitleEncoding else "")

        // ========== 统计信息（官方） ==========
        options.add("--stats")

        // ========== 缓存配置 ==========
        // 网络缓存（官方默认: 0）
        if (config.networkCaching > 0) {
            options.add("--network-caching=$config.networkCaching")
        }

        // ========== 音频重采样器（官方: soxr） ==========
        options.add("--audio-resampler")
        options.add(config.audioResampler)

        // AudioTrack session id（官方）
        options.add("--audiotrack-session-id=$audiotrackSessionId")

        // ========== 字幕样式配置（官方） ==========
        // 字幕大小/缩放
        if (config.isVLC4) {
            options.add("--sub-text-scale=" + (1600 / config.subtitlesSize.toFloat()))
        } else {
            options.add("--freetype-rel-fontsize=$config.subtitlesSize")
        }

        // 字幕粗体
        if (config.subtitlesBold) {
            options.add("--freetype-bold")
        }

        // 字幕颜色
        val freetypeColor = Integer.decode(String.format("0x%06X", (0xFFFFFF and config.subtitlesColor)))
        options.add("--freetype-color=$freetypeColor")
        options.add("--freetype-opacity=$config.subtitlesColorOpacity")

        // 字幕背景
        if (config.subtitlesBackground) {
            val freetypeBackgroundColor = Integer.decode(
                String.format("0x%06X", (0xFFFFFF and config.subtitlesBackgroundColor))
            )
            options.add("--freetype-background-color=$freetypeBackgroundColor")
            options.add("--freetype-background-opacity=$config.subtitlesBackgroundColorOpacity")
        } else {
            options.add("--freetype-background-opacity=0")
        }

        // 字幕阴影
        if (config.subtitlesShadow) {
            val freetypeShadowColor = Integer.decode(
                String.format("0x%06X", (0xFFFFFF and config.subtitlesShadowColor))
            )
            options.add("--freetype-shadow-color=$freetypeShadowColor")
            options.add("--freetype-shadow-opacity=$config.subtitlesShadowOpacity")
        } else {
            options.add("--freetype-shadow-opacity=0")
        }

        // 字幕描边
        if (config.subtitlesOutline) {
            options.add("--freetype-outline-thickness=$config.subtitlesOutlineSize")
            val freetypeOutlineColor = Integer.decode(
                String.format("0x%06X", (0xFFFFFF and config.subtitlesOutlineColor))
            )
            options.add("--freetype-outline-color=$freetypeOutlineColor")
            options.add("--freetype-outline-opacity=$config.subtitlesOutlineOpacity")
        } else {
            options.add("--freetype-outline-opacity=0")
        }

        // ========== 视频输出配置（官方） ==========
        when (config.opengl) {
            OpenGLMode.Enabled -> options.add("--vout=gles2,none")
            OpenGLMode.Disabled -> options.add("--vout=android_display,none")
            OpenGLMode.Automatic -> {
                // 官方自动模式不设置 vout
            }
        }

        // ========== Keystore 配置（官方） ==========
        options.add("--keystore")
        options.add(
            if (AndroidUtil.isMarshMallowOrLater) "file_crypt,none"
            else "file_plaintext,none"
        )
        options.add("--keystore-file")
        options.add(File(context.getDir("keystore", Context.MODE_PRIVATE), "file").absolutePath)

        // ========== 日志级别（官方默认: true 即 -vv） ==========
        options.add(if (config.verboseMode) "-vv" else "-v")

        // ========== Chromecast 配置（官方，仅 VLC3） ==========
        if (!config.isVLC4) {
            if (config.castingPassthrough) {
                options.add("--sout-chromecast-audio-passthrough")
            } else {
                options.add("--no-sout-chromecast-audio-passthrough")
            }
            options.add("--sout-chromecast-conversion-quality=$config.castingQuality")
        }

        // ========== Sout keep（官方） ==========
        options.add("--sout-keep")

        // ========== 自定义选项（官方） ==========
        if (config.customOptions.isNotEmpty()) {
            val optionsArray = config.customOptions.split("\\r?\\n".toRegex()).toTypedArray()
            if (optionsArray.isNotEmpty()) {
                Collections.addAll(options, *optionsArray)
            }
        }

        // ========== SMB v1（官方） ==========
        if (config.preferSmbV1) {
            options.add("--smb-force-v1")
        }

        // ========== Ambisonic/空间音频（官方，非 TV） ==========
        if (config.enableSpatialAudio && !config.showTvUi) {
            val hstfDir = context.getDir("vlc", Context.MODE_PRIVATE)
            val hstfPath = "${hstfDir.absolutePath}/.share/hrtfs/dodeca_and_7channel_3DSL_HRTF.sofa"
            options.add("--spatialaudio-headphones")
            options.add("--hrtf-file")
            options.add(hstfPath)
        }

        // ========== Audio Replay Gain（官方） ==========
        if (config.enableAudioReplayGain) {
            options.add("--audio-replay-gain-mode=${config.audioReplayGainMode}")
            options.add("--audio-replay-gain-preamp=${config.audioReplayGainPreamp}")
            options.add("--audio-replay-gain-default=${config.audioReplayGainDefault}")
            if (config.audioReplayGainPeakProtection) {
                options.add("--audio-replay-gain-peak-protection")
            } else {
                options.add("--no-audio-replay-gain-peak-protection")
            }
        }

        // ========== SoundFont（官方） ==========
        val soundFontFile = getSoundFontFile(context)
        if (soundFontFile.exists()) {
            options.add("--soundfont=${soundFontFile.path}")
        }

        // ========== Preferred resolution（官方） ==========
        options.add("--preferred-resolution=$config.preferredResolution")

        // 调试日志
        if (config.debugLogging) {
            Log.d(TAG, "VLC Options: ${options.joinToString(" ")}")
        }

        return options
    }

    /**
     * 获取音频输出类型
     *
     * 基于官方 VLCOptions.getAout() 实现
     */
    fun getAout(
        aout: Int = -1,
        hwAout: HwAudioOutput? = null
    ): String? {
        var finalAout = aout

        // 硬件检测的音频输出优先
        if (hwAout == HwAudioOutput.OPENSLES) {
            finalAout = AOUT_OPENSLES
        }

        return when (finalAout) {
            AOUT_OPENSLES -> "opensles"
            AOUT_AUDIOTRACK -> "audiotrack"
            else -> null // AOUT_AAUDIO 或自动，返回 null 使用默认
        }
    }

    /**
     * 获取去块滤波器值
     *
     * 基于官方 VLCOptions.getDeblocking() 实现
     */
    fun getDeblocking(deblocking: Int): Int {
        var ret = deblocking
        if (deblocking < 0) {
            /**
             * 设置合理的去块默认值（官方逻辑）：
             *
             * 跳过全部 (4) - 适用于 armv6 和 MIPS
             * 跳过非参考帧 (1) - 适用于 armv7 > 1.2 GHz 且 > 2 核
             * 跳过非关键帧 (3) - 其他设备
             */
            val m = VLCUtil.getMachineSpecs() ?: return ret
            if (m.hasArmV6 && !m.hasArmV7 || m.hasMips)
                ret = 4
            else if (m.frequency >= 1200 && m.processors > 2)
                ret = 1
            else if (m.bogoMIPS >= 1200 && m.processors > 2) {
                ret = 1
                Log.d(TAG, "Used bogoMIPS due to lack of frequency info")
            } else
                ret = 3
        } else if (deblocking > 4) { // 边界检查
            ret = 3
        }
        return ret
    }

    /**
     * 设置 Media 选项
     *
     * 基于官方 VLCOptions.setMediaOptions() 实现
     */
    fun setMediaOptions(
        media: IMedia,
        noHardwareAcceleration: Boolean = false,
        noVideo: Boolean = false,
        paused: Boolean = false,
        hardwareAcceleration: Int = HW_ACCELERATION_AUTOMATIC,
        subtitlesAutoload: Boolean = true,
        hasRenderer: Boolean = false,
        castingPassthrough: Boolean = true,
        castingQuality: String = "2"
    ) {
        // 硬件加速
        if (noHardwareAcceleration) {
            media.setHWDecoderEnabled(false, false)
        } else {
            when (hardwareAcceleration) {
                HW_ACCELERATION_DISABLED -> media.setHWDecoderEnabled(false, false)
                HW_ACCELERATION_FULL -> media.setHWDecoderEnabled(true, true)
                HW_ACCELERATION_DECODING -> {
                    media.setHWDecoderEnabled(true, true)
                    media.addOption(":no-mediacodec-dr")
                    media.addOption(":no-omxil-dr")
                }
                // HW_ACCELERATION_AUTOMATIC: 使用默认选项
            }
        }

        // 无视频
        if (noVideo) media.addOption(":no-video")

        // 暂停启动
        if (paused) media.addOption(":start-paused")

        // 字幕自动加载
        if (!subtitlesAutoload) media.addOption(":sub-language=none")

        // Chromecast
        if (hasRenderer) {
            media.addOption(":sout-chromecast-audio-passthrough=$castingPassthrough")
            media.addOption(":sout-chromecast-conversion-quality=$castingQuality")
        }
    }

    /**
     * 获取 SoundFont 文件
     *
     * 基于官方 VLCOptions.getSoundFontFile() 实现
     */
    fun getSoundFontFile(context: Context): File {
        return File(context.getDir("soundfont", Context.MODE_PRIVATE).path + "/soundfont.sf2")
    }

    /**
     * 硬件检测的音频输出类型
     */
    enum class HwAudioOutput {
        OPENSLES
    }
}

/**
 * VLC 配置参数
 *
 * 完全同步官方 VLCOptions 的所有参数和默认值
 */
data class VLCConfig(
    // ========== 音频配置 ==========
    /** 是否启用音频时间拉伸（官方默认: false） */
    val enableTimeStretching: Boolean = false,

    /** 音频重采样器类型（官方: "soxr"） */
    val audioResampler: String = "soxr",

    // ========== 视频解码配置 ==========
    /**
     * 去块滤波器设置（官方默认: -1 自动）
     * -1: 自动（根据设备性能选择）
     * 0: 无
     * 1: 跳过非参考帧
     * 3: 跳过非关键帧
     * 4: 跳过全部
     */
    val deblocking: Int = -1,

    /** 是否启用跳帧（官方默认: false） */
    val enableFrameSkip: Boolean = false,

    // ========== 缓存配置 ==========
    /**
     * 网络缓存时间（毫秒）
     * 0: 使用 VLC 默认值（官方默认）
     * >0: 自定义缓存时间（最大 60000）
     */
    val networkCaching: Int = 0,

    // ========== 字幕编码 ==========
    /** 字幕编码（官方默认: 空字符串） */
    val subtitleEncoding: String = "",

    // ========== 字幕样式配置（官方） ==========
    /** 字幕大小（官方默认: "16"） */
    val subtitlesSize: String = "16",

    /** 字幕粗体（官方默认: false） */
    val subtitlesBold: Boolean = false,

    /** 字幕颜色（官方默认: 16777215 = 白色） */
    val subtitlesColor: Int = 16777215,

    /** 字幕颜色不透明度（官方默认: 255） */
    val subtitlesColorOpacity: Int = 255,

    /** 字幕背景启用（官方默认: false） */
    val subtitlesBackground: Boolean = false,

    /** 字幕背景颜色（官方默认: 16777215 = 白色） */
    val subtitlesBackgroundColor: Int = 16777215,

    /** 字幕背景颜色不透明度（官方默认: 255） */
    val subtitlesBackgroundColorOpacity: Int = 255,

    /** 字幕阴影启用（官方默认: true） */
    val subtitlesShadow: Boolean = true,

    /** 字幕阴影颜色（官方默认: 0 = 黑色） */
    val subtitlesShadowColor: Int = 0,

    /** 字幕阴影颜色不透明度（官方默认: 128） */
    val subtitlesShadowOpacity: Int = 128,

    /** 字幕描边启用（官方默认: true） */
    val subtitlesOutline: Boolean = true,

    /** 字幕描边大小（官方默认: "4"） */
    val subtitlesOutlineSize: String = "4",

    /** 字幕描边颜色（官方默认: 0 = 黑色） */
    val subtitlesOutlineColor: Int = 0,

    /** 字幕描边颜色不透明度（官方默认: 255） */
    val subtitlesOutlineOpacity: Int = 255,

    // ========== 视频输出配置 ==========
    /**
     * OpenGL 视频输出模式（官方默认: Automatic）
     * -1: 自动
     * 0: 禁用 (android_display)
     * 1: 启用 (gles2)
     */
    val opengl: OpenGLMode = OpenGLMode.Automatic,

    // ========== Chromecast 配置（官方） ==========
    /** Chromecast 仅音频（官方默认: false） */
    val castingAudioOnly: Boolean = false,

    /** Chromecast 音频透传（官方默认: false） */
    val castingPassthrough: Boolean = false,

    /** Chromecast 转换质量（官方默认: "2"） */
    val castingQuality: String = "2",

    // ========== 其他配置 ==========
    /** 是否启用详细日志（官方默认: true，即 -vv） */
    val verboseMode: Boolean = true,

    /** 是否输出调试日志 */
    val debugLogging: Boolean = false,

    /** 自定义选项（官方默认: 空字符串，按行分割） */
    val customOptions: String = "",

    /** 是否优先使用 SMB v1（官方默认: true） */
    val preferSmbV1: Boolean = true,

    /** 是否启用空间音频（官方默认: false） */
    val enableSpatialAudio: Boolean = false,

    /** 是否显示 TV UI（官方默认: false） */
    val showTvUi: Boolean = false,

    // ========== Audio Replay Gain（官方） ==========
    /** 是否启用音频回放增益（官方默认: false） */
    val enableAudioReplayGain: Boolean = false,

    /** 音频回放增益模式（官方默认: "track"） */
    val audioReplayGainMode: String = "track",

    /** 音频回放增益预放大（官方默认: "0.0"） */
    val audioReplayGainPreamp: String = "0.0",

    /** 音频回放增益默认值（官方默认: "-7.0"） */
    val audioReplayGainDefault: String = "-7.0",

    /** 音频回放增益峰值保护（官方默认: true） */
    val audioReplayGainPeakProtection: Boolean = true,

    // ========== Preferred resolution（官方） ==========
    /** 首选分辨率（官方默认: "-1"） */
    val preferredResolution: String = "-1",

    // ========== VLC 版本 ==========
    /** 是否为 VLC 4（官方默认: false） */
    val isVLC4: Boolean = false
) {

    /**
     * VLC 配置 Builder
     */
    class Builder {
        private var enableTimeStretching: Boolean = false
        private var audioResampler: String = "soxr"
        private var deblocking: Int = -1
        private var enableFrameSkip: Boolean = false
        private var networkCaching: Int = 0
        private var subtitleEncoding: String = ""
        private var subtitlesSize: String = "16"
        private var subtitlesBold: Boolean = false
        private var subtitlesColor: Int = 16777215
        private var subtitlesColorOpacity: Int = 255
        private var subtitlesBackground: Boolean = false
        private var subtitlesBackgroundColor: Int = 16777215
        private var subtitlesBackgroundColorOpacity: Int = 255
        private var subtitlesShadow: Boolean = true
        private var subtitlesShadowColor: Int = 0
        private var subtitlesShadowOpacity: Int = 128
        private var subtitlesOutline: Boolean = true
        private var subtitlesOutlineSize: String = "4"
        private var subtitlesOutlineColor: Int = 0
        private var subtitlesOutlineOpacity: Int = 255
        private var opengl: OpenGLMode = OpenGLMode.Automatic
        private var castingAudioOnly: Boolean = false
        private var castingPassthrough: Boolean = false
        private var castingQuality: String = "2"
        private var verboseMode: Boolean = true
        private var debugLogging: Boolean = false
        private var customOptions: String = ""
        private var preferSmbV1: Boolean = true
        private var enableSpatialAudio: Boolean = false
        private var showTvUi: Boolean = false
        private var enableAudioReplayGain: Boolean = false
        private var audioReplayGainMode: String = "track"
        private var audioReplayGainPreamp: String = "0.0"
        private var audioReplayGainDefault: String = "-7.0"
        private var audioReplayGainPeakProtection: Boolean = true
        private var preferredResolution: String = "-1"
        private var isVLC4: Boolean = false

        fun setEnableTimeStretching(value: Boolean) = apply { enableTimeStretching = value }
        fun setAudioResampler(value: String) = apply { audioResampler = value }
        fun setDeblocking(value: Int) = apply { deblocking = value }
        fun setEnableFrameSkip(value: Boolean) = apply { enableFrameSkip = value }
        fun setNetworkCaching(value: Int) = apply { networkCaching = value.coerceIn(0, 60000) }
        fun setSubtitleEncoding(value: String) = apply { subtitleEncoding = value }
        fun setSubtitlesSize(value: String) = apply { subtitlesSize = value }
        fun setSubtitlesBold(value: Boolean) = apply { subtitlesBold = value }
        fun setSubtitlesColor(value: Int) = apply { subtitlesColor = value }
        fun setSubtitlesColorOpacity(value: Int) = apply { subtitlesColorOpacity = value }
        fun setSubtitlesBackground(value: Boolean) = apply { subtitlesBackground = value }
        fun setSubtitlesBackgroundColor(value: Int) = apply { subtitlesBackgroundColor = value }
        fun setSubtitlesBackgroundColorOpacity(value: Int) = apply { subtitlesBackgroundColorOpacity = value }
        fun setSubtitlesShadow(value: Boolean) = apply { subtitlesShadow = value }
        fun setSubtitlesShadowColor(value: Int) = apply { subtitlesShadowColor = value }
        fun setSubtitlesShadowOpacity(value: Int) = apply { subtitlesShadowOpacity = value }
        fun setSubtitlesOutline(value: Boolean) = apply { subtitlesOutline = value }
        fun setSubtitlesOutlineSize(value: String) = apply { subtitlesOutlineSize = value }
        fun setSubtitlesOutlineColor(value: Int) = apply { subtitlesOutlineColor = value }
        fun setSubtitlesOutlineOpacity(value: Int) = apply { subtitlesOutlineOpacity = value }
        fun setOpenGL(value: OpenGLMode) = apply { opengl = value }
        fun setCastingAudioOnly(value: Boolean) = apply { castingAudioOnly = value }
        fun setCastingPassthrough(value: Boolean) = apply { castingPassthrough = value }
        fun setCastingQuality(value: String) = apply { castingQuality = value }
        fun setVerboseMode(value: Boolean) = apply { verboseMode = value }
        fun setDebugLogging(value: Boolean) = apply { debugLogging = value }
        fun setCustomOptions(value: String) = apply { customOptions = value }
        fun setPreferSmbV1(value: Boolean) = apply { preferSmbV1 = value }
        fun setEnableSpatialAudio(value: Boolean) = apply { enableSpatialAudio = value }
        fun setShowTvUi(value: Boolean) = apply { showTvUi = value }
        fun setEnableAudioReplayGain(value: Boolean) = apply { enableAudioReplayGain = value }
        fun setAudioReplayGainMode(value: String) = apply { audioReplayGainMode = value }
        fun setAudioReplayGainPreamp(value: String) = apply { audioReplayGainPreamp = value }
        fun setAudioReplayGainDefault(value: String) = apply { audioReplayGainDefault = value }
        fun setAudioReplayGainPeakProtection(value: Boolean) = apply { audioReplayGainPeakProtection = value }
        fun setPreferredResolution(value: String) = apply { preferredResolution = value }
        fun setIsVLC4(value: Boolean) = apply { isVLC4 = value }

        fun build() = VLCConfig(
            enableTimeStretching = enableTimeStretching,
            audioResampler = audioResampler,
            deblocking = deblocking,
            enableFrameSkip = enableFrameSkip,
            networkCaching = networkCaching,
            subtitleEncoding = subtitleEncoding,
            subtitlesSize = subtitlesSize,
            subtitlesBold = subtitlesBold,
            subtitlesColor = subtitlesColor,
            subtitlesColorOpacity = subtitlesColorOpacity,
            subtitlesBackground = subtitlesBackground,
            subtitlesBackgroundColor = subtitlesBackgroundColor,
            subtitlesBackgroundColorOpacity = subtitlesBackgroundColorOpacity,
            subtitlesShadow = subtitlesShadow,
            subtitlesShadowColor = subtitlesShadowColor,
            subtitlesShadowOpacity = subtitlesShadowOpacity,
            subtitlesOutline = subtitlesOutline,
            subtitlesOutlineSize = subtitlesOutlineSize,
            subtitlesOutlineColor = subtitlesOutlineColor,
            subtitlesOutlineOpacity = subtitlesOutlineOpacity,
            opengl = opengl,
            castingAudioOnly = castingAudioOnly,
            castingPassthrough = castingPassthrough,
            castingQuality = castingQuality,
            verboseMode = verboseMode,
            debugLogging = debugLogging,
            customOptions = customOptions,
            preferSmbV1 = preferSmbV1,
            enableSpatialAudio = enableSpatialAudio,
            showTvUi = showTvUi,
            enableAudioReplayGain = enableAudioReplayGain,
            audioReplayGainMode = audioReplayGainMode,
            audioReplayGainPreamp = audioReplayGainPreamp,
            audioReplayGainDefault = audioReplayGainDefault,
            audioReplayGainPeakProtection = audioReplayGainPeakProtection,
            preferredResolution = preferredResolution,
            isVLC4 = isVLC4
        )
    }
}

/**
 * OpenGL 视频输出模式（官方）
 */
enum class OpenGLMode {
    /** 自动选择（官方默认: -1） */
    Automatic,
    /** 禁用 OpenGL，使用 android_display（官方: 0） */
    Disabled,
    /** 启用 OpenGL，使用 gles2（官方: 1） */
    Enabled
}
