package dev.aaa1115910.bv.mobile.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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

    val colorScheme = when {
        dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (resolvedDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        resolvedDarkTheme -> mobileDarkColorScheme(Color(seedColorArgb))
        else -> mobileLightColorScheme(Color(seedColorArgb))
    }

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
