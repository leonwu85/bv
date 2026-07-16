package dev.aaa1115910.bv.mobile.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.aaa1115910.bv.mobile.screen.MobileMainScreen
import dev.aaa1115910.bv.mobile.screen.RegionBlockScreen
import dev.aaa1115910.bv.mobile.settings.MobileRuntime
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.util.NetworkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        MobileRuntime.install()
        var keepSplashScreen = true
        installSplashScreen().apply {
            setKeepOnScreenCondition { keepSplashScreen }
            setOnExitAnimationListener { splashScreenView ->
                splashScreenView.view.animate()
                    .alpha(0f)
                    .setDuration(SPLASH_EXIT_DURATION_MS)
                    .withEndAction { splashScreenView.remove() }
                    .start()
            }
        }
        super.onCreate(savedInstanceState)

        setContent {
            var isCheckingNetwork by remember { mutableStateOf(true) }
            var isMainlandChina by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                isMainlandChina = withContext(Dispatchers.IO) {
                    false // NetworkUtil.isMainlandChina()
                }
                isCheckingNetwork = false
                keepSplashScreen = false
            }

            BVMobileTheme {
                if (isCheckingNetwork) {
                    // 避免提前加载内容
//                } else if (isMainlandChina) {
//                    RegionBlockScreen()
                } else {
                    MobileMainScreen()
                }
            }
        }
    }

    private companion object {
        const val SPLASH_EXIT_DURATION_MS = 180L
    }
}
