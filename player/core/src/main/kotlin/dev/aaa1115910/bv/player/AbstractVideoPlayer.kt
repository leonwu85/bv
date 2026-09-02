package dev.aaa1115910.bv.player

abstract class AbstractVideoPlayer {
    /** 播放器事件回调 */
    protected var mPlayerEventListener: VideoPlayerListener? = null

    /** 初始跳转位置（毫秒），用于避免在 onReady 中 seek 导致的状态抖动 */
    protected var pendingSeekPosition: Long = 0L

    /** 设置初始播放位置（毫秒），需在 prepare() 之前调用 */
    open fun setInitialSeekPosition(position: Long) {
        pendingSeekPosition = position
    }

    /** 清除初始播放位置 */
    protected fun clearPendingSeekPosition() {
        pendingSeekPosition = 0L
    }

    /** 切换离线播放兼容模式，由具体播放器按需处理本地文件源。 */
    open fun setOfflinePlaybackMode(enabled: Boolean) = Unit

    /**
     * 当前是否有实时超分 shader 在 GPU 上运行。丢帧上报（[VideoPlayerListener.onDecoderOverloaded]）
     * 无法区分解码慢和渲染慢，上层据此决定建议“关闭超分”还是“降低清晰度”。
     */
    open val isSuperResolutionActive: Boolean
        get() = false

    /** 运行时卸载超分 shader，仅影响本次会话；不支持超分的内核为空操作。 */
    open fun disableSuperResolution() = Unit

    /** 能否把 HDR / 杜比视界内容按 HDR 输出；为 false 时上层应从清晰度列表中去掉这些档位。 */
    open val supportsHdrOutput: Boolean
        get() = true

    /** 内核建议的默认清晰度上限（B 站 qn 编码），null 表示不限制；只影响自动选择，不影响用户手动选择。 */
    open val preferredMaxResolutionCode: Int?
        get() = null

    /**
     * 初始化播放器实例
     * 视频播放器第一步：创建视频播放器
     */
    abstract fun initPlayer()

    /**
     * 设置播放地址。
     *
     * 契约：这是一个纯设置操作，只记录下一次 [prepare] 要使用的地址（并丢弃尚未消费的
     * [pendingSeekPosition]），不得停止、重置或以其它方式影响当前正在进行的播放。
     * 上层依赖这一点在直播 URL 续期时“预置”新地址而不打断当前流。
     */
    abstract fun playUrl(videoUrl: String? = null, audioUrl: String? = null)

    /** 准备开始播放，使用最近一次 [playUrl] 设置的地址 */
    abstract fun prepare()

    /** 播放 */
    abstract fun start()

    /** 暂停 */
    abstract fun pause()

    /** 停止 */
    abstract fun stop()

    /** 重置播放器 */
    abstract fun reset()

    /** 是否正在播放 */
    abstract val isPlaying: Boolean

    /** 媒体是否可 seek */
    abstract val isSeekable: Boolean

    /** 跳转播放位置 */
    abstract fun seekTo(time: Long)

    /** 释放播放器 */
    abstract fun release()

    /** 当前播放位置 */
    abstract val currentPosition: Long

    /** 视频总时长 */
    abstract val duration: Long

    /** 缓冲百分比 */
    abstract val bufferedPercentage: Int

    /** 设置其他播放配置 */
    abstract fun setOptions()

    /** 播放速度 */
    abstract var speed: Float

    /** 当前缓冲的网速 */
    abstract val tcpSpeed: Long

    /** 调试信息 */
    abstract val debugInfo: String

    /** 视频宽度 */
    abstract val videoWidth: Int

    /** 视频高度 */
    abstract val videoHeight: Int

    /**
     * 绑定VideoView，监听播放异常，完成，开始准备，视频size变化，视频信息等操作
     */
    fun setPlayerEventListener(playerEventListener: VideoPlayerListener?) {
        mPlayerEventListener = playerEventListener
    }
}
