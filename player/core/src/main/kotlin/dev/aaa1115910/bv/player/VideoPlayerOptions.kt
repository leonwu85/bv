package dev.aaa1115910.bv.player

import dev.aaa1115910.bv.player.entity.SuperResolutionType

data class VideoPlayerOptions(
    val userAgent: String? = null,
    val referer: String? = null,
    val enableFfmpegAudioRenderer: Boolean = false,
    val enableAsyncQueueing: Boolean = true,
    val enableTunneling: Boolean = true,
    val enableAudioPlaybackParams: Boolean = true,
    val enableHardwareDecode: Boolean = true,
    val expandBuffer: Boolean = false,
    val autoSync: String = "",
    val videoSync: String = "audio",
    val hardwareDecodeMode: String = "mediacodec,mediacodec-copy",
    val mpvHardwareDecodeCodecs: String = "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1",
    val mpvVideoOutput: String = "gpu",
    val mpvGpuContext: String = "android",
    val mpvGpuApi: String = "",
    val mpvCache: String = "yes",
    val mpvDemuxerMaxBytes: String = "150MiB",
    val mpvDemuxerMaxBackBytes: String = "50MiB",
    val mpvVdQueueEnable: String = "",
    /**
     * MPV 内核是否把 B 站 CDN（bilivideo.com / bilivideo.cn / akamaized.net）的 HTTPS 播放地址改写为 HTTP。
     * libmpv 的 HTTPS 已通过导出的系统根证书正常校验（见 MpvCaBundle），这只是根证书损坏/过期设备上的兜底：
     * 带签名的播放地址、Referer 与 UA 会明文传输，且 PCDN 节点未必提供 HTTP。
     */
    val mpvPreferHttpForCdn: Boolean = false,
    val superResolutionType: SuperResolutionType = SuperResolutionType.Disable,
    val audioOutputDevices: String = "audiotrack,opensles",
    val enableVideoFrameRateStrategy: Boolean = true,
    val isLive: Boolean = false
)
