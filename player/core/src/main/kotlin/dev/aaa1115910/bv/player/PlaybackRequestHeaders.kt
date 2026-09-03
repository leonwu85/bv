package dev.aaa1115910.bv.player

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * APP play URLs are signed for the Android client and Bilibili rejects them when a web Referer is
 * attached. Keep the Referer for live/WEB URLs, including WEB URLs returned by API fallback.
 */
internal fun VideoPlayerOptions.playbackRefererFor(vararg urls: String?): String? {
    return PlaybackRequestHeaders.refererFor(referer, *urls)
}

/** Header rules shared by the players and by app code that fetches the same media URLs (e.g. sidx probing). */
object PlaybackRequestHeaders {
    /**
     * The Referer to send for [urls], or null when any of them is an APP-signed URL that Bilibili
     * would reject with a web Referer.
     */
    fun refererFor(configuredReferer: String?, vararg urls: String?): String? {
        val referer = configuredReferer?.takeIf { it.isNotBlank() } ?: return null
        return referer.takeUnless {
            urls.filterNotNull().any(String::isAndroidAppMediaUrl)
        }
    }
}

private fun String.isAndroidAppMediaUrl(): Boolean {
    return isAndroidAppMediaUrl(nestingDepth = 0)
}

private fun String.isAndroidAppMediaUrl(nestingDepth: Int): Boolean {
    val rawQuery = runCatching { URI(this).rawQuery }.getOrNull() ?: return false
    val parameters = rawQuery
        .split('&')
        .map { parameter ->
            decodeQueryComponent(parameter.substringBefore('=')) to
                decodeQueryComponent(parameter.substringAfter('=', ""))
        }
    val platform = parameters
        .firstOrNull { (key) -> key.equals("platform", ignoreCase = true) }
        ?.second

    if (
        platform.equals("android", ignoreCase = true) ||
        platform?.startsWith("android_", ignoreCase = true) == true
    ) {
        return true
    }

    if (nestingDepth >= 1) return false
    return parameters.any { (key, value) ->
        key.equals("url", ignoreCase = true) &&
            value.isAndroidAppMediaUrl(nestingDepth = nestingDepth + 1)
    }
}

private fun decodeQueryComponent(value: String): String = runCatching {
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}.getOrDefault(value)
