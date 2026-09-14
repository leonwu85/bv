package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.BiliContentLink
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException

/** Short links use an isolated, unauthenticated client and a bounded redirect chain. */
object ContentLinkResolver {
    private val client by lazy {
        HttpClient(OkHttp) {
            followRedirects = false
            install(HttpTimeout) { requestTimeoutMillis = 8_000 }
        }
    }

    suspend fun resolve(input: String): BiliContentLink? = resolve(input) { url ->
        client.prepareGet(url).execute { response ->
            response.headers[HttpHeaders.Location].takeIf { response.status.value in 300..399 }
        }
    }

    internal suspend fun resolve(input: String, redirect: suspend (String) -> String?): BiliContentLink? {
        BiliContentLink.parse(input)?.let { return it }
        if (!BiliContentLink.isShortLink(input)) return null
        var uri = BiliContentLink.uri(input) ?: return null
        val visited = mutableSetOf<String>()
        try {
            repeat(5) {
                if (!visited.add(uri.toString())) return null
                val location = redirect(uri.toString()) ?: return null
                val next = uri.resolve(location)
                if (BiliContentLink.uri(next.toString()) == null) return null
                BiliContentLink.parse(next.toString())?.let { return it }
                if (!BiliContentLink.isShortLink(next.toString())) return null
                uri = next
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Unresolved links stay editable and can fall back to a normal search.
        }
        return null
    }
}
