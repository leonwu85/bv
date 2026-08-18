package dev.aaa1115910.bv.player

import android.graphics.Matrix
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.bv.player.impl.exo.ExoMediaPlayer
import dev.aaa1115910.bv.player.impl.mpv.MpvMediaPlayer
import dev.aaa1115910.bv.player.impl.vlc.VlcMediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import io.github.oshai.kotlinlogging.KotlinLogging.logger

private class DelegatingVideoPlayerListener(
    private val delegate: () -> VideoPlayerListener,
) : VideoPlayerListener {
    override fun onError(error: Exception) = delegate().onError(error)
    override fun onReady() = delegate().onReady()
    override fun onPlay() = delegate().onPlay()
    override fun onPause() = delegate().onPause()
    override fun onBuffering() = delegate().onBuffering()
    override fun onProgress(position: Long, duration: Long, buffered: Int) =
        delegate().onProgress(position, duration, buffered)

    override fun onEnd() = delegate().onEnd()
    override fun onIdle() = delegate().onIdle()
    override fun onSeekBack(seekBackIncrementMs: Long) = delegate().onSeekBack(seekBackIncrementMs)
    override fun onSeekForward(seekForwardIncrementMs: Long) = delegate().onSeekForward(seekForwardIncrementMs)
    override fun onSeeked(position: Long) = delegate().onSeeked(position)
    override fun onSeekableChanged(seekable: Boolean) = delegate().onSeekableChanged(seekable)
    override fun onVideoSizeChanged(width: Int, height: Int) = delegate().onVideoSizeChanged(width, height)
    override fun onVideoFrameRateChanged(frameRate: Float?) = delegate().onVideoFrameRateChanged(frameRate)
}

/**
 * @param releasePlayerOnDispose whether this renderer owns and releases [videoPlayer]. Callers that
 * perform ordered shutdown themselves must pass `false`.
 */
@OptIn(UnstableApi::class)
@Composable
fun BvVideoPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
    playerListener: VideoPlayerListener,
    rotationDegrees: Float = 0f, // 新增参数，视频旋转角度
    danmakuPlayer: DanmakuPlayer? = null,
    forceUseTextureView: Boolean = false,
    preferSurfaceViewForHdr: Boolean = false,
    releasePlayerOnDispose: Boolean = true,
) {
    val logger = logger("BvVideoPlayer")
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenHeight = with(density) { context.resources.displayMetrics.heightPixels.toFloat() }
    val screenWidth = with(density) { context.resources.displayMetrics.widthPixels.toFloat() }
    val currentPlayerListener = rememberUpdatedState(playerListener)
    val delegatingPlayerListener = remember {
        DelegatingVideoPlayerListener { currentPlayerListener.value }
    }

    SideEffect(videoPlayer) {
        videoPlayer.setPlayerEventListener(delegatingPlayerListener)
        // The player can already be prepared before this Compose renderer is attached. Replay the
        // current size so portrait layout and danmaku-mask geometry do not stay at their fallback.
        val currentVideoWidth = videoPlayer.videoWidth
        val currentVideoHeight = videoPlayer.videoHeight
        if (currentVideoWidth > 0 && currentVideoHeight > 0) {
            delegatingPlayerListener.onVideoSizeChanged(currentVideoWidth, currentVideoHeight)
        }
    }

    DisposableEffect(videoPlayer, releasePlayerOnDispose) {
        onDispose {
            if (releasePlayerOnDispose) {
                videoPlayer.release()
            }
        }
    }

    when (videoPlayer) {
        is ExoMediaPlayer -> {
            var textureView: TextureView? by remember { mutableStateOf(null) }
            var surfaceView: SurfaceView? by remember { mutableStateOf(null) }
            var lastRotationDegrees by remember { mutableFloatStateOf(rotationDegrees) }

            fun clearVideoView() {
                surfaceView?.let {
                    videoPlayer.mPlayer?.clearVideoSurfaceView(it)
                }
                textureView?.let {
                    videoPlayer.mPlayer?.clearVideoTextureView(it)
                }
            }

            if (!preferSurfaceViewForHdr && (forceUseTextureView || rotationDegrees != 0f)) {
                fun applyTextureTransform(tv: TextureView?, degreesRaw: Float) {
                    tv ?: return
                    if (rotationDegrees != lastRotationDegrees) {
                        val time = videoPlayer.currentPosition
                        videoPlayer.stop()
                        tv.postDelayed({
                            val viewWidth = tv.width.toFloat()
                            val viewHeight = tv.height.toFloat()

                            val pivotX = viewWidth / 2f
                            val pivotY = viewHeight / 2f
                            val matrix = Matrix().apply {
                                // 旋转
                                setRotate(rotationDegrees, pivotX, pivotY)

                                if (rotationDegrees == 90f || rotationDegrees == -90f) {
                                    // 缩放（使内容适配反转后的宽高比）
                                    val scale = minOf(
                                        screenHeight / viewWidth,
                                        screenWidth / viewHeight
                                    )
                                    // 以中心缩放，需先将缩放偏移到中心
                                    postScale(scale, scale, pivotX, pivotY)
                                }
                            }
                            tv.setTransform(matrix)

                            videoPlayer.mPlayer?.setVideoTextureView(tv)
                            videoPlayer.prepare()
                            videoPlayer.seekTo(time)
                            danmakuPlayer?.seekTo(time)
                            danmakuPlayer?.pause()
                            videoPlayer.start()

                            lastRotationDegrees = rotationDegrees
                        }, 0)
                    }
                }

                AndroidView(
                    modifier = modifier.fillMaxSize(),
                    factory = { ctx ->
                        clearVideoView()
                        TextureView(ctx).also { tv ->
                            textureView = tv
                            videoPlayer.mPlayer?.setVideoTextureView(tv)
                            // 切换到 TextureView 当帧即尝试应用旋转
                            applyTextureTransform(tv, rotationDegrees)

                            logger.info { "Current view type is TextureView" }
                        }
                    },
                    update = { tv ->
                        applyTextureTransform(tv, rotationDegrees)
                    }
                )
            } else {
                // SurfaceView 渲染
                AndroidView(
                    modifier = modifier.fillMaxSize(),
                    factory = { ctx ->
                        lastRotationDegrees = rotationDegrees
                        clearVideoView()
                        SurfaceView(ctx).also { sv ->
                            surfaceView = sv
                            videoPlayer.mPlayer?.setVideoSurfaceView(sv)

                            logger.info { "Current view type is SurfaceView" }
                        }
                    }
                )
            }

            DisposableEffect(videoPlayer) {
                onDispose {
                    clearVideoView()
                    textureView = null
                    surfaceView = null
                }
            }
        }
        is VlcMediaPlayer -> {
            var vlcVideoLayout: VLCVideoLayout? by remember { mutableStateOf(null) }

            AndroidView(
                modifier = modifier.fillMaxSize(),
                factory = { ctx ->
                    VLCVideoLayout(ctx).also { layout ->
                        vlcVideoLayout = layout
                        videoPlayer.attachVideoLayout(layout)
                        logger.info { "Current view type is VLCVideoLayout" }
                    }
                }
            )

            DisposableEffect(videoPlayer) {
                onDispose {
                    videoPlayer.detachVideoLayout()
                    vlcVideoLayout = null
                }
            }
        }
        is MpvMediaPlayer -> {
            var surfaceView: SurfaceView? by remember { mutableStateOf(null) }

            val surfaceCallback = remember(videoPlayer) {
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        videoPlayer.attachSurface(
                            surface = holder.surface,
                            width = surfaceView?.width ?: 0,
                            height = surfaceView?.height ?: 0
                        )
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        videoPlayer.updateSurfaceSize(width, height)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        videoPlayer.detachSurface()
                    }
                }
            }

            AndroidView(
                modifier = modifier.fillMaxSize(),
                factory = { ctx ->
                    SurfaceView(ctx).also { sv ->
                        surfaceView = sv
                        sv.holder.addCallback(surfaceCallback)
                        logger.info { "Current view type is MPV SurfaceView" }
                    }
                }
            )

            DisposableEffect(videoPlayer) {
                onDispose {
                    surfaceView?.holder?.removeCallback(surfaceCallback)
                    videoPlayer.detachSurface()
                    surfaceView = null
                }
            }
        }
    }
}
