package dev.aaa1115910.bv.player

import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.render.EmojiSupportRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuSurfaceView
import com.kuaishou.akdanmaku.ui.DanmakuView
import com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer

/**
 * 普通弹幕播放器组件
 */
@Composable
fun AkDanmakuPlayer(
    modifier: Modifier = Modifier,
    danmakuPlayer: DanmakuPlayer?,
    visible: Boolean = true
) {
    AkDanmakuPlayer(
        modifier = modifier,
        danmakuPlayer = danmakuPlayer,
        visible = visible,
        isLiveMode = false,
        onLiveDanmakuPlayerReady = null
    )
}

/**
 * 统一弹幕播放器组件
 *
 * @param isLiveMode
 * @param onLiveDanmakuPlayerReady
 */
@Composable
fun AkDanmakuPlayer(
    modifier: Modifier = Modifier,
    danmakuPlayer: DanmakuPlayer? = null,
    visible: Boolean = true,
    isLiveMode: Boolean = false,
    onLiveDanmakuPlayerReady: ((LiveDanmakuPlayer) -> Unit)? = null
) {
    val context = LocalContext.current
    var danmakuView: DanmakuView? by remember { mutableStateOf(null) }
    var liveDanmakuPlayer: LiveDanmakuPlayer? by remember { mutableStateOf(null) }

    DisposableEffect(danmakuPlayer, isLiveMode) {
        onDispose {
            if (!isLiveMode) {
                danmakuPlayer?.release()
            } else {
                liveDanmakuPlayer?.release()
            }
        }
    }

    LaunchedEffect(danmakuView, danmakuPlayer) {
        if (!isLiveMode) {
            danmakuView?.let { view ->
                danmakuPlayer?.bindView(view)
            }
        }
    }

    LaunchedEffect(liveDanmakuPlayer, onLiveDanmakuPlayerReady) {
        android.util.Log.d("AkDanmakuPlayer", "LaunchedEffect triggered: liveDanmakuPlayer=$liveDanmakuPlayer, callback=$onLiveDanmakuPlayerReady")
        liveDanmakuPlayer?.let { player ->
            android.util.Log.d("AkDanmakuPlayer", "Invoking onLiveDanmakuPlayerReady callback with player: $player")
            onLiveDanmakuPlayerReady?.invoke(player)
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
