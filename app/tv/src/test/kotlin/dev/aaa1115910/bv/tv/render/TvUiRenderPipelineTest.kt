package dev.aaa1115910.bv.tv.render

import kotlin.test.Test
import kotlin.test.assertEquals

class TvUiRenderPipelineTest {
    @Test
    fun autoUsesEmbeddedSurfaceOnlyForTrue4kUiOnTv() {
        assertEquals(
            TvUiRenderPath.Embedded1080p,
            resolveRenderPath(
                isTelevision = true,
                has4kOutput = true,
                uiExceeds1080p = true,
                canEmbedUi = true,
                renderMode = TvUiRenderMode.Auto,
            ),
        )
    }

    @Test
    fun autoKeepsNativePathWhenSystemAlreadyUpscales1080pUi() {
        assertEquals(
            TvUiRenderPath.Native,
            resolveRenderPath(
                isTelevision = true,
                has4kOutput = true,
                uiExceeds1080p = false,
                canEmbedUi = true,
                renderMode = TvUiRenderMode.Auto,
            ),
        )
    }

    @Test
    fun forceModeStillRequiresTv4kOutputAndApiSupport() {
        assertEquals(
            TvUiRenderPath.Native,
            resolveRenderPath(
                isTelevision = false,
                has4kOutput = true,
                uiExceeds1080p = true,
                canEmbedUi = true,
                renderMode = TvUiRenderMode.Force1080p,
            ),
        )
        assertEquals(
            TvUiRenderPath.Native,
            resolveRenderPath(
                isTelevision = true,
                has4kOutput = true,
                uiExceeds1080p = true,
                canEmbedUi = false,
                renderMode = TvUiRenderMode.Force1080p,
            ),
        )
    }

    @Test
    fun nativeModeAlwaysKeepsActivityWindowPath() {
        assertEquals(
            TvUiRenderPath.Native,
            resolveRenderPath(
                isTelevision = true,
                has4kOutput = true,
                uiExceeds1080p = true,
                canEmbedUi = true,
                renderMode = TvUiRenderMode.Native,
            ),
        )
    }
}
