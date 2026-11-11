package dev.aaa1115910.biliapi.http.util

/** Only device cookies from the official WebView; account and Gaia cookies stay separate. */
data class WebDeviceCookies(
    val buvid3: String,
    val buvid4: String,
    val buvidFp: String,
    val bNut: String? = null,
) {
    private fun values(): Map<String, String> = buildMap {
        put("buvid3", buvid3)
        put("buvid4", buvid4)
        put("buvid_fp", buvidFp)
        bNut?.let { put("b_nut", it) }
    }

    fun toCookieHeader(): String = values().toHeader()

    fun mergeInto(header: String): String {
        val existing = parse(header)
        // Never combine a fingerprint with a different request's device identity.
        if (existing["buvid3"]?.let { it != buvid3 } == true) return header
        return (existing + values()).toHeader()
    }

    companion object {
        fun fromCookieHeader(header: String?): WebDeviceCookies? {
            val cookies = parse(header.orEmpty())
            return WebDeviceCookies(
                buvid3 = cookies["buvid3"] ?: return null,
                buvid4 = cookies["buvid4"] ?: return null,
                buvidFp = cookies["buvid_fp"] ?: return null,
                bNut = cookies["b_nut"],
            )
        }

        private fun parse(header: String): Map<String, String> = header.split(';')
            .mapNotNull { part ->
                val pair = part.trim().split('=', limit = 2)
                if (pair.size != 2 || pair[0].isBlank() || pair[1].isBlank() ||
                    pair.any { value -> value.any { it <= ' ' || it == '\u007f' } }
                ) null else pair[0] to pair[1]
            }.toMap()

        private fun Map<String, String>.toHeader(): String = entries.joinToString("; ") { (key, value) -> "$key=$value" }
    }
}
