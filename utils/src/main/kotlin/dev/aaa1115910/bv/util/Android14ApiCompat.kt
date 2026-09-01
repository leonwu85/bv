package dev.aaa1115910.bv.util

import android.content.res.Resources
import java.lang.reflect.Method

/** Compatibility calls for API 34 methods omitted by some Android TV firmware builds. */
object Android14ApiCompat {
    private val getWindowMetricsDensity: Method? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        runCatching {
            Class.forName("android.view.WindowMetrics").getMethod("getDensity")
        }.getOrNull()
    }

    private val getSystemOverlaysType: Method? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        runCatching {
            Class.forName("android.view.WindowInsets\$Type").getMethod("systemOverlays")
        }.getOrNull()
    }

    @JvmStatic
    fun windowMetricsDensity(windowMetrics: Any): Float {
        val frameworkDensity = runCatching {
            (getWindowMetricsDensity?.invoke(windowMetrics) as? Number)?.toFloat()
        }.getOrNull()

        if (frameworkDensity != null && frameworkDensity.isFinite() && frameworkDensity > 0f) {
            return frameworkDensity
        }

        return Resources.getSystem().displayMetrics.density
            .takeIf { it.isFinite() && it > 0f }
            ?: 1f
    }

    @JvmStatic
    fun systemOverlays(): Int {
        return runCatching {
            (getSystemOverlaysType?.invoke(null) as? Number)?.toInt()
        }.getOrNull() ?: 0
    }
}
