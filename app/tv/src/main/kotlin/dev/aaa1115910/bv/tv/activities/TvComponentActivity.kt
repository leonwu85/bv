package dev.aaa1115910.bv.tv.activities

import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent as setComposeContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.aaa1115910.bv.tv.render.TV_UI_TARGET_HEIGHT_PX
import dev.aaa1115910.bv.tv.render.TV_UI_TARGET_WIDTH_PX
import dev.aaa1115910.bv.tv.render.TvUiRenderPath
import dev.aaa1115910.bv.tv.render.TvUiRenderPipeline
import dev.aaa1115910.bv.player.tv.LocalTvUiSurfaceEmbedded
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * TV 端公共 Compose Activity。
 *
 * 仅在「物理输出为 4K，且系统 UI 层确实为高于 1080p」时，将 Compose
 * 放入 1920×1080 的 SurfaceControlViewHost。已经使用 1080p UI 层的 TV 继续使用系统原生路径，
 * 不额外增加 Surface。
 */
abstract class TvComponentActivity : ComponentActivity() {
    private val logger = KotlinLogging.logger("TvComponentActivity")
    private var uiSurfaceHost: TvUiSurfaceHost? = null

    fun setContent(content: @Composable () -> Unit) {
        val pipeline = TvUiRenderPipeline.resolve(this)
        logger.info {
            "TV UI render path=${pipeline.renderPath}, " +
                "physical=${pipeline.physicalWidthPx}x${pipeline.physicalHeightPx}, " +
                "ui=${pipeline.uiWidthPx}x${pipeline.uiHeightPx}"
        }
        if (pipeline.renderPath == TvUiRenderPath.Native) {
            setComposeContent(content = content)
            return
        }

        uiSurfaceHost = TvUiSurfaceHost(
            activity = this,
            onFallback = { setComposeContent(content = content) },
        ).also { it.attach(content) }
    }

    override fun onDestroy() {
        uiSurfaceHost?.release()
        uiSurfaceHost = null
        super.onDestroy()
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private class TvUiSurfaceHost(
    private val activity: ComponentActivity,
    private val onFallback: () -> Unit,
) {
    private val logger = KotlinLogging.logger("TvUiSurfaceHost")
    private var surfaceHost: SurfaceControlViewHost? = null
    private var hostSurfaceView: SurfaceView? = null
    private var released = false

    fun attach(content: @Composable () -> Unit) {
        val root = FrameLayout(activity)
        val surfaceView = SurfaceView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            holder.setFormat(PixelFormat.TRANSLUCENT)
            setZOrderOnTop(true)
            isFocusable = true
            isFocusableInTouchMode = true
        }
        hostSurfaceView = surfaceView
        root.addView(surfaceView)
        activity.setContentView(root)

        surfaceView.post {
            if (released) return@post
            runCatching {
                val hostedContext = TvUiRenderPipeline.createUiContext(activity)
                val composeView = ComposeView(hostedContext).apply {
                    setViewTreeLifecycleOwner(activity)
                    setViewTreeViewModelStoreOwner(activity)
                    setViewTreeSavedStateRegistryOwner(activity)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setContent {
                        CompositionLocalProvider(LocalTvUiSurfaceEmbedded provides true) {
                            content()
                        }
                    }
                }
                val display = requireNotNull(activity.display) { "Activity display is unavailable" }
                val host = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val rootSurfaceControl = requireNotNull(surfaceView.rootSurfaceControl) {
                        "Root surface control is unavailable"
                    }
                    SurfaceControlViewHost(
                        activity,
                        display,
                        rootSurfaceControl.inputTransferToken,
                    )
                } else {
                    createLegacySurfaceControlViewHost(surfaceView, display)
                }
                host.setView(composeView, TV_UI_TARGET_WIDTH_PX, TV_UI_TARGET_HEIGHT_PX)
                surfaceView.setChildSurfacePackage(
                    requireNotNull(host.surfacePackage) { "UI SurfacePackage is unavailable" },
                )
                surfaceHost = host
                surfaceView.requestFocus()
            }.onFailure { throwable ->
                logger.error(throwable) { "Attach 1080p TV UI surface failed; falling back to native UI" }
                release()
                onFallback()
            }
        }
    }

    fun release() {
        if (released) return
        released = true
        surfaceHost?.release()
        surfaceHost = null
        hostSurfaceView = null
    }

    @Suppress("DEPRECATION")
    private fun createLegacySurfaceControlViewHost(
        surfaceView: SurfaceView,
        display: android.view.Display,
    ): SurfaceControlViewHost = SurfaceControlViewHost(
        activity,
        display,
        surfaceView.hostToken,
    )
}
