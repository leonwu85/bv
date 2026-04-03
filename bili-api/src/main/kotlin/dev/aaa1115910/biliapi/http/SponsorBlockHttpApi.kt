package dev.aaa1115910.biliapi.http

import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
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
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * BilibiliSponsorBlock API 客户端
 * 官方文档: https://github.com/hanydd/BilibiliSponsorBlock/wiki/API
 */
object SponsorBlockHttpApi {
    private const val DEFAULT_BASE_URL = "bsbsb.top"
    private const val API_PATH = "/api"

    private var currentBaseUrl = DEFAULT_BASE_URL
    private var currentProtocol = URLProtocol.HTTPS
    private lateinit var client: HttpClient

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    init {
        createClient()
    }

    fun updateBaseUrl(baseUrl: String) {
        val useHttp = baseUrl.startsWith("http://")
        val normalizedUrl = baseUrl
            .replace("https://", "")
            .replace("http://", "")
            .trimEnd('/')
        val protocol = if (useHttp) URLProtocol.HTTP else URLProtocol.HTTPS
        if (normalizedUrl != currentBaseUrl || protocol != currentProtocol) {
            currentBaseUrl = normalizedUrl
            currentProtocol = protocol
            createClient()
        }
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
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
                    host = currentBaseUrl
                    protocol = currentProtocol
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

    /**
     * 检测服务器连通状态
     * @return 状态信息，成功返回响应内容摘要，失败返回错误信息
     */
    suspend fun checkServerStatus(): ServerStatus = runCatching {
        val response = client.get(API_PATH)
        when (response.status) {
            HttpStatusCode.OK -> {
                val body = response.bodyAsText().take(200)
                ServerStatus.Connected(body)
            }
            HttpStatusCode.NotFound -> {
                val body = response.bodyAsText().take(200)
                ServerStatus.Connected(body)
            }
            else -> ServerStatus.Error("HTTP ${response.status.value}")
        }
    }.getOrElse { error ->
        when (error) {
            is UnknownHostException -> ServerStatus.Error("无法解析主机名")
            is SocketTimeoutException -> ServerStatus.Error("连接超时")
            else -> ServerStatus.Error(error.message ?: "未知错误")
        }
    }
}

sealed class ServerStatus {
    data class Connected(val info: String) : ServerStatus()
    data class Error(val message: String) : ServerStatus()
    data object Checking : ServerStatus()
}
