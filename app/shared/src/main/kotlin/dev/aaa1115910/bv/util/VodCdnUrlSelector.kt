package dev.aaa1115910.bv.util

import dev.aaa1115910.bv.entity.CdnService
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class VodCdnSelection(
    val url: String,
    val reason: VodCdnSelectionReason
)

internal enum class VodCdnSelectionReason {
    NoUrl,
    BaseUrl,
    BackupUrl,
    RewrittenMirror,
    RewrittenSzbdyd,
    RewrittenMcdnUpgcxcode,
    ProxiedMcdnResource,
    UnchangedFallback
}

internal object VodCdnUrlSelector {
    private const val McdnProxyHost = "proxy-tf-all-ws.bilivideo.com"

    private val mirrorRegex = Regex(
        "^https?://(?:upos-\\w+-(?!302)\\w+|(?:upos|proxy)-tf-[^/]+)\\.(?:bilivideo|akamaized)\\.(?:com|net)/upgcxcode"
    )
    private val mcdnTfRegex = Regex(
        "^https?://(?:(?:(?:\\d{1,3}\\.){3}\\d{1,3}|[^/]+\\.mcdn\\.bilivideo\\.(?:com|cn|net))(?::\\d{1,5})?/v\\d/resource)"
    )

    fun select(
        urls: Iterable<String?>,
        cdnService: CdnService,
        isAudio: Boolean = false,
        disableAudioCdn: Boolean = false
    ): VodCdnSelection {
        val candidates = urls
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()

        if (candidates.isEmpty()) {
            return VodCdnSelection(url = "", reason = VodCdnSelectionReason.NoUrl)
        }

        if (cdnService == CdnService.BaseUrl) {
            return VodCdnSelection(candidates.first(), VodCdnSelectionReason.BaseUrl)
        }

        var mcdnTf: String? = null
        var mcdnUpgcxcode: String? = null
        var last = ""

        candidates.forEach { url ->
            last = url

            if (mirrorRegex.containsMatchIn(url)) {
                val uri = parseUri(url)
                if (uri?.queryParameters()?.get("os") == "mcdn") {
                    mcdnUpgcxcode = url
                } else {
                    val shouldUseBackup = cdnService == CdnService.BackupUrl ||
                            (isAudio && disableAudioCdn)
                    return if (shouldUseBackup) {
                        VodCdnSelection(url, VodCdnSelectionReason.BackupUrl)
                    } else {
                        VodCdnSelection(
                            url = replaceHost(url, cdnService.host ?: CdnService.Ali.host!!),
                            reason = VodCdnSelectionReason.RewrittenMirror
                        )
                    }
                }
            }

            if (mcdnTfRegex.containsMatchIn(url)) {
                mcdnTf = url
                return@forEach
            }

            // Matches common Bilibili mirror hosts using upgcxcode path.
            if (url.contains("/upgcxcode/")) {
                mcdnUpgcxcode = url
                return@forEach
            }

            if (url.contains("szbdyd.com")) {
                val uri = parseUri(url)
                val host = uri?.queryParameters()?.get("xy_usource")
                    ?: cdnService.host
                    ?: CdnService.Ali.host!!
                return VodCdnSelection(
                    url = replaceHost(url, host, scheme = "https", port = 443),
                    reason = VodCdnSelectionReason.RewrittenSzbdyd
                )
            }
        }

        val fallbackHost = cdnService.host ?: CdnService.Ali.host!!
        return when {
            mcdnUpgcxcode != null -> VodCdnSelection(
                url = replaceHost(mcdnUpgcxcode, fallbackHost),
                reason = VodCdnSelectionReason.RewrittenMcdnUpgcxcode
            )

            mcdnTf != null -> VodCdnSelection(
                url = proxyMcdnResource(mcdnTf),
                reason = VodCdnSelectionReason.ProxiedMcdnResource
            )

            else -> VodCdnSelection(
                url = last,
                reason = VodCdnSelectionReason.UnchangedFallback
            )
        }
    }

    private fun proxyMcdnResource(url: String): String {
        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return "https://$McdnProxyHost?url=$encodedUrl"
    }

    private fun URI.queryParameters(): Map<String, String> {
        val rawQuery = rawQuery ?: return emptyMap()
        return rawQuery.split('&')
            .asSequence()
            .filter(String::isNotEmpty)
            .map { parameter ->
                val key = parameter.substringBefore('=')
                val value = parameter.substringAfter('=', "")
                decodeUrlComponent(key) to decodeUrlComponent(value)
            }
            .toMap()
    }

    private fun decodeUrlComponent(value: String): String {
        return runCatching {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }.getOrDefault(value)
    }

    private fun replaceHost(
        url: String,
        host: String,
        scheme: String? = null,
        port: Int? = null
    ): String {
        val uri = parseUri(url) ?: return url
        val effectivePort = port ?: uri.port.takeIf { it != -1 }
        return buildString {
            append(scheme ?: uri.scheme ?: "https")
            append("://")
            append(host)
            effectivePort?.let {
                append(':')
                append(it)
            }
            uri.rawPath?.let { append(it) }
            uri.rawQuery?.let {
                append('?')
                append(it)
            }
            uri.rawFragment?.let {
                append('#')
                append(it)
            }
        }
    }

    private fun parseUri(url: String): URI? {
        return runCatching { URI(url) }.getOrNull()
    }
}
