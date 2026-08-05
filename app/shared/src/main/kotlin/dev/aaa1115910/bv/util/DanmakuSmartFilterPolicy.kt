package dev.aaa1115910.bv.util

import android.os.Build

object DanmakuSmartFilterPolicy {
    const val MIN_SUPPORTED_SDK_INT = Build.VERSION_CODES.N_MR1

    fun isSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= MIN_SUPPORTED_SDK_INT

    fun coerceEnabled(
        enabled: Boolean,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = enabled && isSupported(sdkInt)
}
