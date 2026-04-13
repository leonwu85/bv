package dev.aaa1115910.bv.player.tv

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.bv.player.AkDanmakuPlayer
import dev.aaa1115910.bv.player.util.danmakuMaskBitmap
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer

/**
 * 可稳定引用的弹幕层句柄
 * 仅弹幕层自身重组，避免父级
 */
@Stable
class DanmakuLayerHandle(
    initialDanmakuPlayer: DanmakuPlayer? = null,
    initialIsLiveMode: Boolean = false
) {
    // 内部保存引用，对外通过 updateDanmakuPlayer 更新，避免与 JVM setter 同名冲突
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(initialDanmakuPlayer)
        private set

    // 是否为直播模式
    var isLiveMode: Boolean by mutableStateOf(initialIsLiveMode)
        private set

    // 当前蒙版帧（保留用于调试）
    var maskFrame: DanmakuMaskFrame? by mutableStateOf(null)
        private set

    // 预渲染的蒙版 Bitmap
    var maskBitmap: Bitmap? by mutableStateOf(null)
        private set

    // 是否显示弹幕
    var visible by mutableStateOf(true)
        private set

    // 视频宽高比，用于计算蒙版的正确覆盖区域
    var videoAspectRatio: Float by mutableStateOf(0f)
        private set

    // 直播模式回调
    var onLiveDanmakuPlayerReady: ((LiveDanmakuPlayer) -> Unit)? by mutableStateOf(null)
        private set

    fun updateOnLiveDanmakuPlayerReady(callback: ((LiveDanmakuPlayer) -> Unit)?) {
        if (onLiveDanmakuPlayerReady !== callback) onLiveDanmakuPlayerReady = callback
    }

    fun updateDanmakuPlayer(player: DanmakuPlayer?) {
        if (danmakuPlayer !== player) danmakuPlayer = player
    }

    fun updateIsLiveMode(isLive: Boolean) {
        if (isLiveMode != isLive) isLiveMode = isLive
    }

    fun update(
        mask: DanmakuMaskFrame? = maskFrame,
        bitmap: Bitmap? = maskBitmap,
        visible: Boolean? = null,
        videoAspectRatio: Float? = null
    ) {
        if (maskFrame !== mask) maskFrame = mask
        if (maskBitmap !== bitmap) maskBitmap = bitmap
        visible?.let { if (this.visible != it) this.visible = it }
        videoAspectRatio?.let { if (this.videoAspectRatio != it) this.videoAspectRatio = it }
    }
}

@Composable
fun DanmakuLayer(
    modifier: Modifier = Modifier,
    handle: DanmakuLayerHandle,
) {
    val player = handle.danmakuPlayer

    // 使用预渲染的 Bitmap
    val maskModifier = if (handle.maskBitmap != null && handle.videoAspectRatio > 0f) {
        Modifier.danmakuMaskBitmap(handle.maskBitmap, handle.videoAspectRatio)
    } else Modifier

    Box(
        modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .then(maskModifier)
    ) {
        AkDanmakuPlayer(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            danmakuPlayer = player,
            visible = handle.visible,
            isLiveMode = handle.isLiveMode,
            onLiveDanmakuPlayerReady = handle.onLiveDanmakuPlayerReady
        )
    }
}
