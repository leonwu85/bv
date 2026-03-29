package dev.aaa1115910.biliapi.http

import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
import dev.aaa1115910.biliapi.http.util.IPv4PreferredDns
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * BilibiliSponsorBlock API 客户端
 * 官方文档: https://github.com/hanydd/BilibiliSponsorBlock/wiki/API
 */
object SponsorBlockHttpApi {
    private const val BASE_URL = "bsbsb.top"
    private const val API_PATH = "/api"

    private lateinit var client: HttpClient

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            engine {
                config {
                    dns(IPv4PreferredDns)
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            install(HttpRequestRetry) {
                retryOnException(maxRetries = 2)
            }
            defaultRequest {
                url {
                    host = BASE_URL
                    protocol = URLProtocol.HTTPS
                }
            }
        }
    }

    /**
     * 获取视频的跳过片段
     * @param bvid 视频BV号
     * @param cid 视频CID
     * @param categories 需要查询的类别列表，默认查询所有
     * @return 片段列表，404时返回空列表
     */
    suspend fun getSkipSegments(
        bvid: String,
        cid: Long,
        categories: List<String>? = null
    ): Result<List<SponsorSegment>> = runCatching {
        val response = client.get("$API_PATH/skipSegments") {
            parameter("videoID", bvid)
            parameter("cid", cid.toString())

            categories?.forEach { category ->
                parameter("category", category)
            }
        }

        if (response.status == HttpStatusCode.NotFound) {
            emptyList()  // 没有片段数据时返回空列表
        } else {
            val responseText = response.bodyAsText()
            json.decodeFromString<List<SponsorSegment>>(responseText)
        }
    }
}
