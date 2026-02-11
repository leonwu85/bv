package dev.aaa1115910.bv.player

interface VideoPlayerListener {
    /** 异常 */
    fun onError(error: Exception)

    /**
     * 准备
     */
    fun onReady()

    /** 播放 */
    fun onPlay()

    /** 暂停 */
    fun onPause()

    /** 缓冲中 */
    fun onBuffering()

    /** 进度变更（毫秒/百分比） */
    fun onProgress(position: Long, duration: Long, buffered: Int) {}

    /** 播放结束 */
    fun onEnd()

    /** 空闲，例如播放前 */
    fun onIdle()

    /** 后退 */
    fun onSeekBack(seekBackIncrementMs: Long)

    /** 前进 */
    fun onSeekForward(seekForwardIncrementMs: Long)

    /** Seek完成，位置发生变化（用于同步弹幕） */
    fun onSeeked(position: Long) {}

    /** seekable 状态变化 */
    fun onSeekableChanged(seekable: Boolean) {}

    /** 视频尺寸变化 */
    fun onVideoSizeChanged(width: Int, height: Int) {}

}