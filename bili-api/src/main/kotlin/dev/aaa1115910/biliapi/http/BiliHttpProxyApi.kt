package dev.aaa1115910.biliapi.http

import dev.aaa1115910.biliapi.BiliApiConstants
import dev.aaa1115910.biliapi.http.entity.BiliResponse
import dev.aaa1115910.biliapi.http.entity.VVoucherException
import dev.aaa1115910.biliapi.http.entity.search.SearchResultData
import dev.aaa1115910.biliapi.http.entity.video.PlayUrlData
import dev.aaa1115910.biliapi.http.entity.video.PlayUrlV2Data
import dev.aaa1115910.biliapi.http.plugins.BiliUserAgent
import dev.aaa1115910.biliapi.http.util.encApiSign
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object BiliHttpProxyApi {
    private var client: HttpClient? = null

    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun createClient(proxyServer: String) {
        client = HttpClient(OkHttp) {
            BiliUserAgent()
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
                    val proxyServerSpilt = proxyServer.split(":")
                    val endPoint = proxyServerSpilt.first()
                    val port = proxyServerSpilt.getOrNull(1)?.toInt()
                    host = endPoint
                    if (endPoint == "127.0.0.1") {
                        //local debug
                        this.port = 8080
                    } else {
                        if (port != null) {
                            this.port = port
                        } else {
                            protocol = URLProtocol.HTTPS
                        }
                    }
                }
            }
        }.apply {
            encApiSign()
        }
    }

    /**
     * 检查响应体是否包含风控 v_voucher。
     * 与 [BiliHttpApi] 保持一致，代理 playurl 命中 voucher 时抛 [VVoucherException]。
     */
    private fun checkForVVoucher(bodyText: String) {
        runCatching {
            val root = json.parseToJsonElement(bodyText).jsonObject
            val data = root["data"]?.jsonObject ?: root["result"]?.jsonObject ?: return
            val vVoucher = data["v_voucher"]?.jsonPrimitive?.contentOrNull
            if (!vVoucher.isNullOrBlank()) {
                throw VVoucherException(vVoucher)
            }
        }.onFailure {
            if (it is VVoucherException) throw it
            // JSON 解析失败不影响正常流程
        }
    }

    suspend fun getPgcVideoPlayUrl(
        av: Long? = null,
        bv: String? = null,
        epid: Int? = null,
        cid: Long? = null,
        qn: Int? = null,
        fnval: Int? = null,
        fnver: Int? = null,
        fourk: Int? = null,
        session: String? = null,
        supportMultiAudio: Boolean? = null,
        drmTechType: Int? = null,
        fromClient: String? = null,
        sessData: String? = null,
        dedeUserID: Long? = null,
        buvid3: String? = null,
        gaiaVtoken: String? = null
    ): BiliResponse<PlayUrlData> {
        val httpClient = client ?: throw IllegalStateException("no proxy server")
        val response = httpClient.get("/pgc/player/web/playurl") {
            require(av != null || bv != null) { "av and bv cannot be null at the same time" }
            require(epid != null || cid != null) { "epid and cid cannot be null at the same time" }
            av?.let { parameter("avid", it) }
            bv?.let { parameter("bvid", it) }
            epid?.let { parameter("ep_id", it) }
            cid?.let { parameter("cid", it) }
            qn?.let { parameter("qn", it) }
            fnval?.let { parameter("fnval", it) }
            fnver?.let { parameter("fnver", it) }
            fourk?.let { parameter("fourk", it) }
            session?.let { parameter("session", it) }
            supportMultiAudio?.let { parameter("support_multi_audio", it) }
            drmTechType?.let { parameter("drm_tech_type", it) }
            fromClient?.let { parameter("from_client", it) }
            gaiaVtoken?.takeIf { it.isNotBlank() }?.let { parameter("gaia_vtoken", it) }
            val cookieParts = mutableListOf<String>()
            sessData?.let { cookieParts.add("SESSDATA=$it") }
            dedeUserID?.let { cookieParts.add("DedeUserID=$it") }
            buvid3?.let { cookieParts.add("buvid3=$it") }
            gaiaVtoken?.takeIf { it.isNotBlank() }?.let { cookieParts.add("x-bili-gaia-vtoken=$it") }
            if (cookieParts.isNotEmpty()) header("Cookie", cookieParts.joinToString(";"))
            //必须得加上 referer 才能通过账号身份验证
            header("referer", "https://www.bilibili.com")
        }
        val bodyText = response.bodyAsText()
        checkForVVoucher(bodyText)
        return json.decodeFromString(bodyText)
    }

    suspend fun getPgcVideoPlayUrlV2(
        av: Long? = null,
        bv: String? = null,
        epid: Int? = null,
        cid: Long? = null,
        qn: Int? = null,
        fnval: Int? = null,
        fnver: Int? = null,
        fourk: Int? = null,
        session: String? = null,
        supportMultiAudio: Boolean? = null,
        drmTechType: Int? = null,
        fromClient: String? = null,
        sessData: String? = null,
        buvid3: String? = null,
        tryLook: Boolean = false,
        gaiaVtoken: String? = null
    ): BiliResponse<PlayUrlV2Data> {
        val httpClient = client ?: throw IllegalStateException("no proxy server")
        val response = httpClient.get("/pgc/player/web/v2/playurl") {
            require(av != null || bv != null) { "av and bv cannot be null at the same time" }
            require(epid != null || cid != null) { "epid and cid cannot be null at the same time" }
            av?.let { parameter("avid", it) }
            bv?.let { parameter("bvid", it) }
            epid?.let { parameter("ep_id", it) }
            cid?.let { parameter("cid", it) }
            qn?.let { parameter("qn", it) }
            fnval?.let { parameter("fnval", it) }
            fnver?.let { parameter("fnver", it) }
            fourk?.let { parameter("fourk", it) }
            session?.let { parameter("session", it) }
            supportMultiAudio?.let { parameter("support_multi_audio", it) }
            drmTechType?.let { parameter("drm_tech_type", it) }
            fromClient?.let { parameter("from_client", it) }
            if (tryLook) parameter("try_look", 1)
            gaiaVtoken?.takeIf { it.isNotBlank() }?.let { parameter("gaia_vtoken", it) }
            val cookieParts = mutableListOf<String>()
            sessData?.let { cookieParts.add("SESSDATA=$it") }
            buvid3?.let { cookieParts.add("buvid3=$it") }
            gaiaVtoken?.takeIf { it.isNotBlank() }?.let { cookieParts.add("x-bili-gaia-vtoken=$it") }
            if (cookieParts.isNotEmpty()) header("Cookie", cookieParts.joinToString(";"))
            //必须得加上 referer 才能通过账号身份验证
            header("referer", "https://www.bilibili.com")
        }
        val bodyText = response.bodyAsText()
        checkForVVoucher(bodyText)
        return json.decodeFromString(bodyText)
    }


    /**
     * 分类搜索与[keyword]相关的[type]类型的相关结果
     */
    suspend fun searchType(
        keyword: String,
        type: String,
        page: Int = 1,
        tid: Int? = null,
        order: String? = null,
        duration: Int? = null,
        sessData: String? = null,
        dedeUserID: Long? = null,
        buvid3: String? = null
    ): BiliResponse<SearchResultData> = client?.get("/x/web-interface/wbi/search/type") {
        parameter("keyword", keyword)
        parameter("search_type", type)
        parameter("page", page)
        tid?.let { parameter("tids", it) }
        order?.let { parameter("order", it) }
        duration?.let { parameter("duration", it) }
        val cookieParts = mutableListOf<String>()
        sessData?.let { cookieParts.add("SESSDATA=$it") }
        dedeUserID?.let { cookieParts.add("DedeUserID=$it") }
        buvid3?.let { cookieParts.add("buvid3=$it") }
        if (cookieParts.isNotEmpty()) {
            header("Cookie", cookieParts.joinToString(";") + ";")
        }
        header("referer", "https://search.bilibili.com/")
    }?.body() ?: throw IllegalStateException("no proxy server")
}
