package dev.aaa1115910.bv.player

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
    val mpvVideoOutput: String = "gpu",
    val audioOutputDevices: String = "audiotrack,opensles",
    val enableVideoFrameRateStrategy: Boolean = true,
    val isLive: Boolean = false
)
