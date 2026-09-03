package dev.aaa1115910.bv.player

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.render.EmojiSupportRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuSurfaceView
import com.kuaishou.akdanmaku.ui.DanmakuView
import com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer
import com.kuaishou.akdanmaku.ui.VideoDanmakuSurfaceView

/**
 * 普通弹幕播放器组件
 */
@Composable
fun AkDanmakuPlayer(
    modifier: Modifier = Modifier,
    danmakuPlayer: DanmakuPlayer?,
    visible: Boolean = true,
    maskBitmap: Bitmap? = null,
    videoAspectRatio: Float = 0f,
    releaseDanmakuPlayerOnDispose: Boolean = true,
    onVideoDanmakuSurfaceViewReady: ((VideoDanmakuSurfaceView?) -> Unit)? = null,
    onVideoDanmakuSurfaceViewRelease: ((VideoDanmakuSurfaceView) -> Unit)? = null,
    onDanmakuPlayerBound: ((DanmakuPlayer) -> Unit)? = null,
) {
    AkDanmakuPlayer(
        modifier = modifier,
        danmakuPlayer = danmakuPlayer,
        visible = visible,
        maskBitmap = maskBitmap,
        videoAspectRatio = videoAspectRatio,
        releaseDanmakuPlayerOnDispose = releaseDanmakuPlayerOnDispose,
        onVideoDanmakuSurfaceViewReady = onVideoDanmakuSurfaceViewReady,
        onVideoDanmakuSurfaceViewRelease = onVideoDanmakuSurfaceViewRelease,
        onDanmakuPlayerBound = onDanmakuPlayerBound,
        useSurfaceViewForNormalMode = false,
        isLiveMode = false,
        onLiveDanmakuPlayerReady = null
    )
}

/**
 * 统一弹幕播放器组件
 *
 * @param isLiveMode
 * @param onLiveDanmakuPlayerReady
 * @param releaseDanmakuPlayerOnDispose whether this renderer owns and releases [danmakuPlayer].
 * Callers that keep the player in an Activity or ViewModel must pass `false`.
 */
@Composable
fun AkDanmakuPlayer(
    modifier: Modifier = Modifier,
    danmakuPlayer: DanmakuPlayer? = null,
    visible: Boolean = true,
    maskBitmap: Bitmap? = null,
    videoAspectRatio: Float = 0f,
    releaseDanmakuPlayerOnDispose: Boolean = true,
    onVideoDanmakuSurfaceViewReady: ((VideoDanmakuSurfaceView?) -> Unit)? = null,
    onVideoDanmakuSurfaceViewRelease: ((VideoDanmakuSurfaceView) -> Unit)? = null,
    onDanmakuPlayerBound: ((DanmakuPlayer) -> Unit)? = null,
    useSurfaceViewForNormalMode: Boolean = false,
    isLiveMode: Boolean = false,
    onLiveDanmakuPlayerReady: ((LiveDanmakuPlayer) -> Unit)? = null
) {
    var danmakuView: DanmakuView? by remember { mutableStateOf(null) }
    var liveDanmakuPlayer: LiveDanmakuPlayer? by remember { mutableStateOf(null) }
    val currentOnDanmakuPlayerBound by rememberUpdatedState(onDanmakuPlayerBound)
    val currentOnLiveDanmakuPlayerReady by rememberUpdatedState(onLiveDanmakuPlayerReady)
    val currentOnVideoDanmakuSurfaceViewReady by rememberUpdatedState(onVideoDanmakuSurfaceViewReady)
    val currentOnVideoDanmakuSurfaceViewRelease by rememberUpdatedState(onVideoDanmakuSurfaceViewRelease)

    DisposableEffect(danmakuPlayer, isLiveMode, releaseDanmakuPlayerOnDispose) {
        onDispose {
            if (!isLiveMode) {
                if (releaseDanmakuPlayerOnDispose) {
                    danmakuPlayer?.release()
                }
            } else {
                liveDanmakuPlayer?.release()
            }
        }
    }

    SideEffect(danmakuView, danmakuPlayer, isLiveMode) {
        if (!isLiveMode) {
            danmakuView?.let { view ->
                danmakuPlayer?.let { player ->
                    player.bindView(view)
                    currentOnDanmakuPlayerBound?.invoke(player)
                }
            }
        }
    }

    SideEffect(liveDanmakuPlayer) {
        android.util.Log.d("AkDanmakuPlayer", "SideEffect triggered: liveDanmakuPlayer=$liveDanmakuPlayer")
        liveDanmakuPlayer?.let { player ->
            currentOnLiveDanmakuPlayerReady?.invoke(player)
        }
    }

    if (isLiveMode) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                DanmakuSurfaceView(ctx).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    // 直播弹幕需要盖在视频 Surface 上，但不能压过 Compose 控制面板。
                    setZOrderMediaOverlay(true)
                    holder?.setFormat(PixelFormat.TRANSLUCENT)

                    val player = LiveDanmakuPlayer(
                        surfaceView = this,
                        renderer = EmojiSupportRenderer(),
                        config = DanmakuConfig(liveMode = true)
                    )
                    liveDanmakuPlayer = player
                    player.play()
                    android.util.Log.d("AkDanmakuPlayer", "LiveDanmakuPlayer created and started: $player")
                }
            },
            update = { surfaceView ->
                surfaceView.visibility = if (visible) View.VISIBLE else View.INVISIBLE
            }
        )
    } else if (useSurfaceViewForNormalMode) {
        key(danmakuPlayer, useSurfaceViewForNormalMode) {
            var danmakuSurfaceView: VideoDanmakuSurfaceView? by remember { mutableStateOf(null) }

            danmakuSurfaceView?.let { surfaceView ->
                DisposableEffect(surfaceView) {
                    onDispose {
                        if (currentOnVideoDanmakuSurfaceViewRelease != null) {
                            currentOnVideoDanmakuSurfaceViewRelease?.invoke(surfaceView)
                        } else {
                            currentOnVideoDanmakuSurfaceViewReady?.invoke(null)
                        }
                    }
                }
            }

            SideEffect(danmakuSurfaceView, danmakuPlayer) {
                danmakuSurfaceView?.let { surfaceView ->
                    danmakuPlayer?.let { player ->
                        player.bindSurfaceView(surfaceView)
                        currentOnDanmakuPlayerBound?.invoke(player)
                    }
                }
            }

            AndroidView(
                modifier = modifier,
                factory = { ctx ->
                    VideoDanmakuSurfaceView(ctx).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        setZOrderOnTop(false)
                        setZOrderMediaOverlay(true)
                        holder?.setFormat(PixelFormat.TRANSLUCENT)
                        if (currentOnVideoDanmakuSurfaceViewReady == null) {
                            updateMaskBitmap(maskBitmap, videoAspectRatio)
                        }
                    }.also {
                        danmakuSurfaceView = it
                        currentOnVideoDanmakuSurfaceViewReady?.invoke(it)
                    }
                },
                update = { surfaceView ->
                    surfaceView.visibility = if (visible) View.VISIBLE else View.INVISIBLE
                    if (currentOnVideoDanmakuSurfaceViewReady == null) {
                        if (maskBitmap != null && !maskBitmap.isRecycled) {
                            surfaceView.updateMaskBitmap(maskBitmap, videoAspectRatio)
                        } else {
                            surfaceView.clearMaskBitmap()
                        }
                    }
                }
            )
        }
    } else {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                DanmakuView(ctx).apply {
                    setBackgroundColor(Color.TRANSPARENT)

                    setWillNotDraw(false)

                    // 硬件加速层，减少弹幕绘制的 CPU 开销
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    // 兼容性优化：对于旧版本Android使用绘制缓存
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        @Suppress("DEPRECATION")
                        isDrawingCacheEnabled = true
                        @Suppress("DEPRECATION")
                        setDrawingCacheBackgroundColor(Color.TRANSPARENT)
                    }
                }.also { danmakuView = it }
            },
            update = { view ->
                view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
            }
        )
    }
}
