package dev.aaa1115910.bv.mobile.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.mobile.settings.MobileRuntime

@Composable
fun BVMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themePalette: MobileThemePalette = MobileThemePalette.Default,
    content: @Composable () -> Unit
) {
    MobileRuntime.install()

    val context = LocalContext.current
    val window by lazy { (context as Activity).window }
    val view = LocalView.current
    val systemUiController = rememberSystemUiController()

    val themeType by if (view.isInEditMode) {
        androidx.compose.runtime.remember(darkTheme) {
            androidx.compose.runtime.mutableStateOf(if (darkTheme) ThemeType.Dark else ThemeType.Light)
        }
    } else {
        MobilePrefs.themeTypeFlow.collectAsState(initial = MobilePrefs.themeType)
    }
    val dynamicColorEnabled by if (view.isInEditMode) {
        androidx.compose.runtime.remember(dynamicColor) {
            androidx.compose.runtime.mutableStateOf(dynamicColor)
        }
    } else {
        MobilePrefs.dynamicColorFlow.collectAsState(initial = MobilePrefs.dynamicColor)
    }
    val palette by if (view.isInEditMode) {
        androidx.compose.runtime.remember(themePalette) {
            androidx.compose.runtime.mutableStateOf(themePalette)
        }
    } else {
        MobilePrefs.themePaletteFlow.collectAsState(initial = MobilePrefs.themePalette)
    }
    val seedColorArgb by if (view.isInEditMode) {
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(MobilePrefs.DEFAULT_SEED_COLOR)
        }
    } else {
        MobilePrefs.seedColorFlow.collectAsState(initial = MobilePrefs.seedColor)
    }
    val resolvedDarkTheme = when (themeType) {
        ThemeType.Auto -> isSystemInDarkTheme()
        ThemeType.Dark -> true
        ThemeType.Light -> false
    }
    val useDarkIcons = !resolvedDarkTheme

    val colorScheme = mobileColorScheme(
        context = context,
        palette = palette,
        resolvedDarkTheme = resolvedDarkTheme,
        dynamicColorEnabled = dynamicColorEnabled,
        seedColor = Color(seedColorArgb)
    )

    if (!view.isInEditMode) {
        val currentWindow = (view.context as Activity).window
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(currentWindow, view)
                .isAppearanceLightStatusBars = useDarkIcons
        }
    }

    SideEffect {
        systemUiController.setStatusBarColor(color = Color.Transparent)
        systemUiController.setNavigationBarColor(color = Color.Transparent)
        if (!view.isInEditMode) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                useDarkIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        content()
    }
}

private fun mobileColorScheme(
    context: Context,
    palette: MobileThemePalette,
    resolvedDarkTheme: Boolean,
    dynamicColorEnabled: Boolean,
    seedColor: Color
): ColorScheme = when (palette) {
    MobileThemePalette.ChineseTraditional -> {
        if (resolvedDarkTheme) traditionalDarkColorScheme() else traditionalLightColorScheme()
    }

    MobileThemePalette.Default -> when {
        dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (resolvedDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        resolvedDarkTheme -> mobileDarkColorScheme(seedColor)
        else -> mobileLightColorScheme(seedColor)
    }
}

private fun mobileLightColorScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = readableContentColor(seed),
    primaryContainer = blend(seed, Color.White, 0.78f),
    onPrimaryContainer = blend(seed, Color.Black, 0.58f),
    secondary = blend(seed, Color(0xFF45656A), 0.45f),
    onSecondary = Color.White,
    secondaryContainer = blend(seed, Color.White, 0.84f),
    onSecondaryContainer = Color(0xFF1A2E30),
    tertiary = blend(seed, Color(0xFF8A5A44), 0.48f),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCC),
    onTertiaryContainer = Color(0xFF321206),
    surface = Color(0xFFFAFBF8),
    surfaceVariant = Color(0xFFE0E5DE),
    surfaceContainer = Color(0xFFF0F3EE),
    surfaceContainerLow = Color(0xFFF6F8F4),
    surfaceContainerHighest = Color(0xFFE3E7E0),
)

private fun mobileDarkColorScheme(seed: Color) = darkColorScheme(
    primary = blend(seed, Color.White, 0.28f),
    onPrimary = readableContentColor(blend(seed, Color.White, 0.28f)),
    primaryContainer = blend(seed, Color.Black, 0.48f),
    onPrimaryContainer = blend(seed, Color.White, 0.82f),
    secondary = blend(seed, Color(0xFF9AC7C2), 0.55f),
    onSecondary = Color(0xFF102023),
    secondaryContainer = Color(0xFF26373A),
    onSecondaryContainer = Color(0xFFD1ECE8),
    tertiary = Color(0xFFE7BDA8),
    onTertiary = Color(0xFF442417),
    tertiaryContainer = Color(0xFF5E392B),
    onTertiaryContainer = Color(0xFFFFDBCC),
    surface = Color(0xFF101411),
    surfaceVariant = Color(0xFF414940),
    surfaceContainer = Color(0xFF171C18),
    surfaceContainerLow = Color(0xFF121713),
    surfaceContainerHighest = Color(0xFF292F2A),
)

private fun traditionalLightColorScheme() = lightColorScheme(
    primary = Color(0xFF106697),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E4ED),
    onPrimaryContainer = Color(0xFF08446F),
    secondary = Color(0xFF037C63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC1D0B5),
    onSecondaryContainer = Color(0xFF123A2D),
    tertiary = Color(0xFFD5B112),
    onTertiary = Color(0xFF2A2100),
    tertiaryContainer = Color(0xFFF1B84D),
    onTertiaryContainer = Color(0xFF3F2E00),
    error = Color(0xFFB62A2C),
    onError = Color.White,
    errorContainer = Color(0xFFEBD9D6),
    onErrorContainer = Color(0xFF5C1011),
    background = Color(0xFFFFFCF6),
    onBackground = Color(0xFF211B18),
    surface = Color(0xFFFFFCF6),
    onSurface = Color(0xFF211B18),
    surfaceVariant = Color(0xFFE8E2DA),
    onSurfaceVariant = Color(0xFF5A514A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF6EC),
    surfaceContainer = Color(0xFFF7F0E4),
    surfaceContainerHigh = Color(0xFFF2EBDD),
    surfaceContainerHighest = Color(0xFFE8E2DA),
    surfaceBright = Color(0xFFFFFCF6),
    surfaceDim = Color(0xFFE3D9CA),
    outline = Color(0xFF8A7B6D),
    outlineVariant = Color(0xFFD3C8BA),
    inverseSurface = Color(0xFF362B24),
    inverseOnSurface = Color(0xFFF9EFE2),
    inversePrimary = Color(0xFF8BACD1),
)

private fun traditionalDarkColorScheme() = darkColorScheme(
    primary = Color(0xFF8BACD1),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF08446F),
    onPrimaryContainer = Color(0xFFD4E4ED),
    secondary = Color(0xFF6AAE95),
    onSecondary = Color(0xFF073729),
    secondaryContainer = Color(0xFF123A2D),
    onSecondaryContainer = Color(0xFFC1D0B5),
    tertiary = Color(0xFFF1B84D),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF6E4F24),
    onTertiaryContainer = Color(0xFFF3ECD6),
    error = Color(0xFFE94B2B),
    onError = Color(0xFF340507),
    errorContainer = Color(0xFFB62A2C),
    onErrorContainer = Color(0xFFEBD9D6),
    background = Color(0xFF171411),
    onBackground = Color(0xFFF2EBDD),
    surface = Color(0xFF171411),
    onSurface = Color(0xFFF2EBDD),
    surfaceVariant = Color(0xFF4A4038),
    onSurfaceVariant = Color(0xFFD8CCC0),
    surfaceContainerLowest = Color(0xFF100D0B),
    surfaceContainerLow = Color(0xFF1D1815),
    surfaceContainer = Color(0xFF211B18),
    surfaceContainerHigh = Color(0xFF2B231F),
    surfaceContainerHighest = Color(0xFF362B24),
    surfaceBright = Color(0xFF42362F),
    surfaceDim = Color(0xFF171411),
    outline = Color(0xFFA8998B),
    outlineVariant = Color(0xFF5A514A),
    inverseSurface = Color(0xFFF2EBDD),
    inverseOnSurface = Color(0xFF362B24),
    inversePrimary = Color(0xFF106697),
)

private fun blend(from: Color, to: Color, amount: Float): Color {
    val inverse = 1f - amount
    return Color(
        red = from.red * inverse + to.red * amount,
        green = from.green * inverse + to.green * amount,
        blue = from.blue * inverse + to.blue * amount,
        alpha = from.alpha * inverse + to.alpha * amount
    )
}

private fun readableContentColor(color: Color): Color =
    if (color.luminance() > 0.5f) Color.Black else Color.White
