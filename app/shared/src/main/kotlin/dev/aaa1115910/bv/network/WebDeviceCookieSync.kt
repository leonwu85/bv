package dev.aaa1115910.bv.network

import android.webkit.CookieManager
import dev.aaa1115910.biliapi.http.util.WebDeviceCookies
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging

/** Share the official verification page's device identity with native Web API requests. */
object WebDeviceCookieSync {
    private val logger = KotlinLogging.logger {}

    internal fun verificationCookies(): WebDeviceCookies? = runCatching {
        WebDeviceCookies.fromCookieHeader(CookieManager.getInstance().getCookie("https://api.bilibili.com/"))
    }.getOrNull()

    fun currentCookies(): WebDeviceCookies? = verificationCookies()?.takeIf { it.buvid3 == Prefs.buvid3 }

    fun synchronize(authRepository: AuthRepository) {
        val cookies = verificationCookies() ?: return
        // CookieManager owns persistence and expiry of the other device cookies. Keep the
        // canonical buvid3 in preferences too, so existing repository/login paths agree.
        if (Prefs.buvid3 != cookies.buvid3) Prefs.buvid3 = cookies.buvid3
        authRepository.buvid3 = cookies.buvid3
        CookieManager.getInstance().flush()
        logger.info { "Synchronized official WebView device cookies for Web API requests" }
    }
}
