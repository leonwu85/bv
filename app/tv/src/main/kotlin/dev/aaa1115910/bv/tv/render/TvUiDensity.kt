package dev.aaa1115910.bv.tv.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import dev.aaa1115910.bv.util.Prefs

private const val TvLayoutWidthDp = 960f

internal class TvUiDensity(windowWidthPx: Int, renderWidthPx: Int, customDensity: Float?) {
    private val windowWidth = windowWidthPx.takeIf { it > 0 } ?: renderWidthPx.coerceAtLeast(1)
    private val renderWidth = renderWidthPx.takeIf { it > 0 } ?: windowWidth

    // The existing preference is expressed in native window pixels per dp.
    val preference = customDensity?.takeIf { it.isFinite() && it > 0f } ?: (windowWidth / TvLayoutWidthDp)
    val defaultRenderDensity = renderWidth / TvLayoutWidthDp
    val renderDensity = preference * (renderWidth.toFloat() / windowWidth)
}

internal val LocalTvUiDensity = compositionLocalOf {
    TvUiDensity(TV_UI_TARGET_WIDTH_PX, TV_UI_TARGET_WIDTH_PX, null)
}

@Composable
internal fun ProvideTvUiDensity(windowWidthPx: Int, content: @Composable () -> Unit) {
    val customDensity by remember { Prefs.customDensityFlow }.collectAsState(initial = null)
    BoxWithConstraints(propagateMinConstraints = true) {
        val density = TvUiDensity(windowWidthPx, constraints.maxWidth, customDensity)
        CompositionLocalProvider(
            LocalTvUiDensity provides density,
            LocalDensity provides Density(density.renderDensity, LocalDensity.current.fontScale),
            content = content,
        )
    }
}
