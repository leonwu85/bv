package dev.aaa1115910.bv.tv.render

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.view.Display
import dev.aaa1115910.bv.util.DeviceUtil

/**
 * TV 界面与视频层的分辨率管线。
 *
 * 仅供 TV Activity 使用：浏览界面在真 4K UI 上降为 1080p Surface，
 * 视频仍由它自身的 SurfaceView 按物理屏和视频源分辨率输出。
 */
internal const val TV_UI_TARGET_WIDTH_PX = 1920
internal const val TV_UI_TARGET_HEIGHT_PX = 1080

internal enum class TvUiRenderPath {
    Native,
    Embedded1080p,
}

/** TV 专属分辨率策略，不供 Mobile 端读取。 */
internal enum class TvUiRenderMode(val storageValue: String, val displayName: String) {
    Auto("auto", "自动"),
    Force1080p("1080p", "强制 1080p 界面"),
    Native("native", "系统原生分辨率"),
    ;

    companion object {
        fun fromStorageValue(value: String?): TvUiRenderMode =
            entries.firstOrNull { it.storageValue == value } ?: Auto
    }
}

internal object TvUiRenderSettings {
    private const val PreferencesName = "tv_ui_render_settings"
    private const val RenderModeKey = "tv_ui_render_mode"

    fun getMode(context: Context): TvUiRenderMode = TvUiRenderMode.fromStorageValue(
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .getString(RenderModeKey, TvUiRenderMode.Auto.storageValue),
    )

    fun setMode(context: Context, mode: TvUiRenderMode) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(RenderModeKey, mode.storageValue)
            .apply()
    }
}

internal data class TvDisplayPipeline(
    val isTelevision: Boolean,
    val physicalWidthPx: Int,
    val physicalHeightPx: Int,
    val uiWidthPx: Int,
    val uiHeightPx: Int,
    val renderPath: TvUiRenderPath,
)

internal object TvUiRenderPipeline {
    fun resolve(activity: Activity): TvDisplayPipeline {
        val display = activity.compatDisplay()
        val physicalSize = display.currentPhysicalSize()
        val uiSize = activity.resources.displayMetrics.let { metrics ->
            Point(metrics.widthPixels, metrics.heightPixels)
        }
        val isTelevision = DeviceUtil.isTvDevice(activity) ||
            (activity.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
                Configuration.UI_MODE_TYPE_TELEVISION
        val has4kOutput = physicalSize.x >= 3_200 && physicalSize.y >= 1_800
        val uiExceeds1080p = uiSize.x > TV_UI_TARGET_WIDTH_PX || uiSize.y > TV_UI_TARGET_HEIGHT_PX
        val canEmbedUi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val renderMode = TvUiRenderSettings.getMode(activity)
        val renderPath = resolveRenderPath(
            isTelevision = isTelevision,
            has4kOutput = has4kOutput,
            uiExceeds1080p = uiExceeds1080p,
            canEmbedUi = canEmbedUi,
            renderMode = renderMode,
        )

        return TvDisplayPipeline(
            isTelevision = isTelevision,
            physicalWidthPx = physicalSize.x,
            physicalHeightPx = physicalSize.y,
            uiWidthPx = uiSize.x,
            uiHeightPx = uiSize.y,
            renderPath = renderPath,
        )
    }

    /** 为 1920×1080 的 Compose 子表面保持当前界面的 dp 尺寸。 */
    fun createUiContext(baseContext: Context): Context {
        val original = baseContext.resources.configuration
        val config = Configuration(original)
        val widthDp = original.screenWidthDp.takeIf { it > 0 }
        val heightDp = original.screenHeightDp.takeIf { it > 0 }
        val density = when {
            widthDp != null -> TV_UI_TARGET_WIDTH_PX.toFloat() / widthDp
            heightDp != null -> TV_UI_TARGET_HEIGHT_PX.toFloat() / heightDp
            else -> 2f
        }
        config.densityDpi = (density * DisplayMetricsDpiBaseline).toInt().coerceAtLeast(1)
        return baseContext.createConfigurationContext(config)
    }

    private const val DisplayMetricsDpiBaseline = 160f
}

@Suppress("DEPRECATION")
private fun Activity.compatDisplay(): Display =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        requireNotNull(display) { "Activity display is unavailable" }
    } else {
        windowManager.defaultDisplay
    }

internal fun resolveRenderPath(
    isTelevision: Boolean,
    has4kOutput: Boolean,
    uiExceeds1080p: Boolean,
    canEmbedUi: Boolean,
    renderMode: TvUiRenderMode,
): TvUiRenderPath {
    val shouldEmbed = when (renderMode) {
        TvUiRenderMode.Native -> false
        TvUiRenderMode.Force1080p -> isTelevision && has4kOutput && canEmbedUi
        TvUiRenderMode.Auto -> isTelevision && has4kOutput && uiExceeds1080p && canEmbedUi
    }
    return if (shouldEmbed) TvUiRenderPath.Embedded1080p else TvUiRenderPath.Native
}

private fun Display.currentPhysicalSize(): Point {
    val mode = mode
    return Point(mode.physicalWidth, mode.physicalHeight)
}
