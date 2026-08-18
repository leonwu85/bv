package dev.aaa1115910.bv.tv.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TvUiPerformanceTier {
    Conservative,
    Balanced,
    Standard,
}

@Immutable
data class TvUiPerformanceProfile(
    val tier: TvUiPerformanceTier,
    val isMediaTek: Boolean,
    val drawerAnimationMillis: Int,
    val drawerRepeatSettleMillis: Long,
    val maxKeepPages: Int,
    val allowFullPageAnimation: Boolean,
    val imageLoadDelayMillis: Long,
) {
    companion object {
        val Default = TvUiPerformanceProfile(
            tier = TvUiPerformanceTier.Standard,
            isMediaTek = false,
            drawerAnimationMillis = 100,
            drawerRepeatSettleMillis = 140L,
            maxKeepPages = 3,
            allowFullPageAnimation = true,
            imageLoadDelayMillis = 64L,
        )
    }
}

object TvUiPerformancePolicy {
    private const val LOW_MEMORY_LIMIT_MB = 3_072L

    fun resolve(context: Context): TvUiPerformanceProfile {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER
        } else {
            ""
        }
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            ""
        }
        val hardwareIdentity = listOf(
            socManufacturer,
            socModel,
            Build.HARDWARE,
            Build.BOARD,
            Build.DEVICE,
        ).joinToString(separator = " ")

        return resolve(
            hardwareIdentity = hardwareIdentity,
            totalMemoryMb = memoryInfo.totalMem.takeIf { it > 0L }?.div(1024L * 1024L)
                ?: Long.MAX_VALUE,
            processorCount = Runtime.getRuntime().availableProcessors(),
        )
    }

    internal fun resolve(
        hardwareIdentity: String,
        totalMemoryMb: Long,
        processorCount: Int,
    ): TvUiPerformanceProfile {
        val normalizedHardware = hardwareIdentity.lowercase()
        val isMediaTek = normalizedHardware.contains("mediatek") ||
                normalizedHardware.contains("mtk") ||
                Regex("(?:^|\\s)mt\\d{4,}(?:\\s|$)").containsMatchIn(normalizedHardware)
        val isConstrained = totalMemoryMb <= LOW_MEMORY_LIMIT_MB || processorCount <= 4

        return when {
            isMediaTek && isConstrained -> TvUiPerformanceProfile(
                tier = TvUiPerformanceTier.Conservative,
                isMediaTek = true,
                drawerAnimationMillis = 80,
                drawerRepeatSettleMillis = 160L,
                maxKeepPages = 2,
                allowFullPageAnimation = false,
                imageLoadDelayMillis = 140L,
            )

            isMediaTek -> TvUiPerformanceProfile(
                tier = TvUiPerformanceTier.Balanced,
                isMediaTek = true,
                drawerAnimationMillis = 90,
                drawerRepeatSettleMillis = 150L,
                maxKeepPages = 2,
                allowFullPageAnimation = false,
                imageLoadDelayMillis = 100L,
            )

            isConstrained -> TvUiPerformanceProfile(
                tier = TvUiPerformanceTier.Balanced,
                isMediaTek = false,
                drawerAnimationMillis = 90,
                drawerRepeatSettleMillis = 150L,
                maxKeepPages = 2,
                allowFullPageAnimation = false,
                imageLoadDelayMillis = 100L,
            )

            else -> TvUiPerformanceProfile.Default
        }
    }
}

class TvPreloadCoordinator {
    private val mutex = Mutex()
    private val interactionGeneration = MutableStateFlow(0L)

    /**
     * Called from TV focus/navigation input. Background composition and non-essential visual
     * preparation use this signal instead of assuming a fixed launch delay is idle time.
     */
    fun notifyUserInteraction() {
        interactionGeneration.update { it + 1L }
    }

    /**
     * Waits for one complete quiet window. If another interaction arrives while waiting, the
     * window restarts so preload/shader preparation cannot race the next D-pad frame.
     */
    suspend fun awaitInteractionIdle(
        quietPeriodMillis: Long = DEFAULT_INTERACTION_QUIET_PERIOD_MS,
    ) {
        require(quietPeriodMillis >= 0L)
        if (quietPeriodMillis == 0L) return

        var observedGeneration = interactionGeneration.value
        while (true) {
            delay(quietPeriodMillis)
            val latestGeneration = interactionGeneration.value
            if (latestGeneration == observedGeneration) return
            observedGeneration = latestGeneration
        }
    }

    suspend fun <T> runExclusive(block: suspend () -> T): T = mutex.withLock { block() }

    private companion object {
        const val DEFAULT_INTERACTION_QUIET_PERIOD_MS = 250L
    }
}

val LocalTvUiPerformanceProfile = staticCompositionLocalOf { TvUiPerformanceProfile.Default }
val LocalTvPreloadCoordinator = staticCompositionLocalOf { TvPreloadCoordinator() }
val LocalTvImageLoadingAllowed = staticCompositionLocalOf { true }

@Composable
fun rememberTvUiPerformanceProfile(): TvUiPerformanceProfile {
    val context = LocalContext.current
    return androidx.compose.runtime.remember(context) {
        TvUiPerformancePolicy.resolve(context.applicationContext)
    }
}

@Composable
@ReadOnlyComposable
fun deferredTvImageModel(model: Any?): Any? {
    return model.takeIf { LocalTvImageLoadingAllowed.current }
}
