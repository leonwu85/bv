package dev.aaa1115910.biliapi.http

import dev.aaa1115910.biliapi.BiliApiConstants
import dev.aaa1115910.biliapi.http.entity.BiliResponse
import dev.aaa1115910.biliapi.http.entity.BiliResponseWithoutData
import dev.aaa1115910.biliapi.http.entity.live.DanmuInfoData
import dev.aaa1115910.biliapi.http.entity.live.HistoryDanmaku
import dev.aaa1115910.biliapi.http.entity.live.RoomPlayInfoData
import dev.aaa1115910.biliapi.http.plugins.BiliUserAgent
import dev.aaa1115910.biliapi.http.util.encWbi
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.URLProtocol
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BiliLiveHttpApi {
    private var endPoint: String = ""
    private lateinit var client: HttpClient
    private val logger = KotlinLogging.logger { }

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BiliUserAgent()
            install(ContentNegotiation) {
                json(Json {
                    coerceInputValues = true
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    host = "api.live.bilibili.com"
                    protocol = URLProtocol.HTTPS
                }
            }
        }
    }

    /**
     * 获取直播间[roomId]的弹幕连接地址等信息，例如 token
     * 需要 WBI 签名
     * @param sessData 用户登录凭证，传入后可获取已登录用户权限的弹幕 token
     */
    suspend fun getLiveDanmuInfo(roomId: Int, sessData: String = ""): BiliResponse<DanmuInfoData> =
        client.get("/xlive/web-room/v1/index/getDanmuInfo") {
            parameter("id", roomId)
            parameter("type", 0)
            encWbi()
            if (sessData.isNotEmpty()) header("Cookie", "SESSDATA=$sessData")
        }.body()

    /**
     * 获取直播间[roomId]的信息
     */
    suspend fun getLiveRoomPlayInfo(roomId: Int): BiliResponse<RoomPlayInfoData> =
        client.get("/xlive/web-room/v1/index/getRoomPlayInfo") {
            parameter("room_id", roomId)
        }.body()

    /**
     * 获取直播间[roomId]的历史弹幕
     */
    suspend fun getLiveDanmuHistory(roomId: Int): BiliResponse<HistoryDanmaku> =
        client.get("/xlive/web-room/v1/dM/gethistory") {
            parameter("roomid", roomId)
        }.body()

    /**
     * 上报进入直播间行为，用于写入直播观看历史。
     */
    suspend fun reportRoomEntryAction(
        roomId: Int,
        csrf: String,
        sessData: String,
        buvid3: String? = null,
        platform: String = "pc",
        visitId: String = ""
    ): BiliResponseWithoutData = client.post("/xlive/web-room/v1/index/roomEntryAction") {
        parameter("csrf", csrf)
        setBody(
            FormDataContent(
                Parameters.build {
                    append("room_id", roomId.toString())
                    append("platform", platform)
                    append("csrf_token", csrf)
                    append("csrf", csrf)
                    append("visit_id", visitId)
                }
            )
        )
        header("Origin", "https://live.bilibili.com")
        header("Referer", "https://live.bilibili.com/$roomId")
        header(
            "Cookie",
            buildString {
                buvid3?.takeIf { it.isNotBlank() }?.let {
                    append("buvid3=")
                    append(it)
                    append("; ")
                }
                append("bili_jct=")
                append(csrf)
                append("; SESSDATA=")
                append(sessData)
                append(";")
            }
        )
    }.body()

}