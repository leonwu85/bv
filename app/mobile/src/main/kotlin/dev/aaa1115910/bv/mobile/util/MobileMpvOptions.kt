package dev.aaa1115910.bv.mobile.util

import android.os.Build
import dev.aaa1115910.bv.player.impl.vlc.VlcMediaPlayer

/**
 * 手机端 MPV / VLC 参数的取值表，与 TV 端 `TvMpvOptions` / `MpvSetting` 保持一致的口径：
 * 所有项都只能从固定选项里选，mpv-android 这个构建没有 Vulkan/rkmpp，自由填写只会得到初始化报错；
 * 存量的自由文本值在启动时由 `MobilePrefs.sanitizeMpvOptions()` 归一化到“默认”。
 *
 * 与 TV 端的差别只有超分：手机 GPU 普遍强于电视盒子，这里保留全部档位，
 * 跑不动时由播放器的丢帧检测弹出“关闭超分”提示。
 */
object MobileMpvOptions {
    /**
     * 固定使用 Android GL 上下文：mpv-android 的 ffmpeg 以 `--disable-vulkan` 构建，
     * `angle` 只存在于 Windows，历史偏好里残留的这些值只会让初始化失败。
     */
    const val GPU_CONTEXT = "android"
    const val GPU_API = ""

    /** `vo=gpu + hwdec=mediacodec` 零拷贝依赖 AImageReader（API 26）；更低版本只能拷贝，4K 会卡 */
    val supportsZeroCopyHwdec: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    const val LOW_API_MPV_HINT = "此设备 Android 版本较低，MPV 无法零拷贝硬解，默认清晰度将限制为 1080P60"

    // mpv-android 的 ffmpeg 以 --disable-vulkan 构建且没有 rkmpp，Android 上 auto/auto-safe/auto-copy 也只是
    // mediacodec 的别名，因此只保留真正有区别的四种。
    val hardwareDecodeOptions: LinkedHashMap<String, String> = linkedMapOf(
        "mediacodec,mediacodec-copy" to "mediacodec,mediacodec-copy\n默认，直出失败时回退拷贝",
        "mediacodec" to "mediacodec\n仅零拷贝直出",
        "mediacodec-copy" to "mediacodec-copy\n仅拷贝",
        "no" to "no\n软解"
    )

    val hardwareDecodeCodecsOptions: LinkedHashMap<String, String> = linkedMapOf(
        "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1" to "默认：h264, hevc, mpeg4, mpeg2video, vp8, vp9, av1",
        "h264,hevc" to "h264, hevc",
        "h264,hevc,vp9,av1" to "h264, hevc, vp9, av1",
        "all" to "all"
    )

    val videoOutputOptions: LinkedHashMap<String, String> = linkedMapOf(
        "gpu" to "gpu（默认）",
        "gpu-next" to "gpu-next（需要 GLES 3.0，失败自动回退 gpu）",
        "mediacodec_embed" to "mediacodec_embed（解码器直出，支持 HDR，超分无效）"
    )

    val vdQueueEnableOptions: LinkedHashMap<String, String> = linkedMapOf(
        "" to "默认",
        "yes" to "yes",
        "no" to "no"
    )

    val cacheOptions: LinkedHashMap<String, String> = linkedMapOf(
        "yes" to "yes（默认）",
        "auto" to "auto",
        "no" to "no"
    )

    val demuxerMaxBytesOptions: LinkedHashMap<String, String> = linkedMapOf(
        "" to "自动（按设备内存）",
        "16MiB" to "16 MiB",
        "32MiB" to "32 MiB",
        "64MiB" to "64 MiB",
        "128MiB" to "128 MiB",
        "256MiB" to "256 MiB"
    )

    val demuxerMaxBackBytesOptions: LinkedHashMap<String, String> = linkedMapOf(
        "" to "自动（前向的一半）",
        "4MiB" to "4 MiB",
        "8MiB" to "8 MiB",
        "16MiB" to "16 MiB",
        "32MiB" to "32 MiB",
        "64MiB" to "64 MiB"
    )

    /** VLC `--vout`：空串交给 libvlc 自选 */
    val vlcVideoOutputOptions: LinkedHashMap<String, String> = linkedMapOf(
        "" to "自动（默认，libvlc 自选）",
        VlcMediaPlayer.VLC_VIDEO_OUTPUT_GLES2 to "gles2（OpenGL 渲染）",
        VlcMediaPlayer.VLC_VIDEO_OUTPUT_ANDROID_DISPLAY to "android_display（强制直出）"
    )

    /** 存量的自由文本或已下线的选项值一律回到“默认”（首个选项） */
    fun normalizeChoice(stored: String, options: LinkedHashMap<String, String>): String {
        val trimmed = stored.trim()
        return if (trimmed in options.keys) trimmed else options.keys.first()
    }
}
