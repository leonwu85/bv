package dev.aaa1115910.bv.tv.render

import kotlin.test.Test
import kotlin.test.assertEquals

class TvUiRenderPipelineTest {
    @Test
    fun nativeAndEmbeddedSurfacesHaveTheSameLogicalWidth() {
        for ((windowWidth, renderWidth) in listOf(3840 to 3840, 3840 to 1920, 1920 to 1920)) {
            val density = TvUiDensity(windowWidth, renderWidth, customDensity = null)
            assertEquals(960f, renderWidth / density.renderDensity)
            assertEquals(density.renderDensity, density.defaultRenderDensity)
        }
    }

    @Test
    fun savedWindowDensityKeepsItsZoomWhenSwitchingRenderModes() {
        val native = TvUiDensity(3840, 3840, customDensity = 3f)
        val embedded = TvUiDensity(3840, 1920, customDensity = 3f)
        assertEquals(native.preference, embedded.preference)
        assertEquals(3f, native.renderDensity)
        assertEquals(1.5f, embedded.renderDensity)
        assertEquals(2f, embedded.defaultRenderDensity)
    }

    @Test
    fun autoUsesEmbeddedSurfaceOnlyForTrue4kUiOnTv() {
        assertEquals(
            TvUiRenderPath.Embedded1080p,
            resolveRenderPath(
                isTelevision = true,
                has4kOutput = true,
                uiExceeds1080p = true,
                sdkInt = 31,
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
                sdkInt = 31,
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
                sdkInt = 31,
                renderMode = TvUiRenderMode.Force1080p,
            ),
        )
        assertEquals(
            TvUiRenderPath.Native,
            resolveRenderPath(
                isTelevision = true,
                has4kOutput = true,
                uiExceeds1080p = true,
                sdkInt = 29,
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
                sdkInt = 31,
                renderMode = TvUiRenderMode.Native,
            ),
        )
    }

    @Test
    fun android11KeepsNativeInputEvenWhen1080pIsForced() {
        for (mode in TvUiRenderMode.entries) {
            assertEquals(
                TvUiRenderPath.Native,
                resolveRenderPath(
                    isTelevision = true,
                    has4kOutput = true,
                    uiExceeds1080p = true,
                    sdkInt = 30,
                    renderMode = mode,
                ),
                "Android 11 must retain D-pad input with mode=$mode",
            )
        }
    }

    @Test
    fun supportedVersionsCanForce1080pButStillRequire4kOutput() {
        for (sdkInt in listOf(31, 32, 33, 34, 35, 36, 37)) {
            for (has4kOutput in listOf(false, true)) {
                assertEquals(
                    if (has4kOutput) TvUiRenderPath.Embedded1080p else TvUiRenderPath.Native,
                    resolveRenderPath(
                        isTelevision = true,
                        has4kOutput = has4kOutput,
                        uiExceeds1080p = false,
                        sdkInt = sdkInt,
                        renderMode = TvUiRenderMode.Force1080p,
                    ),
                    "sdk=$sdkInt, has4kOutput=$has4kOutput",
                )
            }
        }
    }
}
