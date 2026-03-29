package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.live.LiveAreaResponse
import dev.aaa1115910.biliapi.entity.live.LiveFeedResponse
import dev.aaa1115910.biliapi.entity.live.LiveFollowResponse
import dev.aaa1115910.biliapi.entity.live.LiveRoomListResponse
import dev.aaa1115910.biliapi.entity.live.LiveRoomPlayInfoResponse
import dev.aaa1115910.biliapi.http.plugins.BiliUserAgent
import dev.aaa1115910.biliapi.http.util.IPv4PreferredDns
import dev.aaa1115910.biliapi.http.util.encAppGet
import dev.aaa1115910.biliapi.http.util.encWebAppGet
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class LiveRepository(
    private val authRepository: AuthRepository
) {
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                dns(IPv4PreferredDns)
            }
        }
        BiliUserAgent()
        install(ContentNegotiation) {
            json(Json {
                coerceInputValues = true
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    /**
     * 获取直播分区列表
     */
    suspend fun getLiveAreaList(): LiveAreaResponse = withContext(Dispatchers.IO) {
        client.get("https://api.live.bilibili.com/room/v1/Area/getList")
            .body()
    }

    /**
     * 获取直播间列表
     * @param parentAreaId 父分区ID
     * @param areaId 分区ID
     * @param page 页码
     * @param pageSize 每页数量
     */
    suspend fun getLiveRoomList(
        parentAreaId: String,
        areaId: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): LiveRoomListResponse = withContext(Dispatchers.IO) {
        client.get("https://api.live.bilibili.com/xlive/app-interface/v2/second/getList") {
            parameter("access_key", authRepository.accessToken ?: "")
            parameter("actionKey", "appkey")
            parameter("area_id", areaId)
            parameter("build", 8430300)
            parameter("device", "android")
            parameter("mobi_app", "android")
            parameter("page", page)
            parameter("page_size", pageSize)
            parameter("parent_area_id", parentAreaId)
            parameter("platform", "android")
            parameter("qn", 0)
            parameter("sort_type", "")
            encAppGet()
            header("buvid", authRepository.buvid ?: "")
            header("env", "prod")
            header("app-key", "android")
            header("x-bili-trace-id", "")
            header("x-bili-aurora-eid", "")
            header("x-bili-aurora-zone", "")
        }.body()
    }

    /**
     * 获取直播间播放信息
     * @param roomId 直播间ID
     * @param qn 画质编号，默认30000（杜比，最高），服务端会自动降级到实际最高可用画质
     * @param sessData 登录凭证，需要认证才能获取高画质流
     */
    suspend fun getLiveRoomPlayInfo(roomId: Int, qn: Int = 30000, sessData: String = ""): LiveRoomPlayInfoResponse =
        withContext(Dispatchers.IO) {
            client.get("https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo") {
                parameter("room_id", roomId)
                parameter("qn", qn)
                parameter("platform", "web")
                parameter("protocol", "0,1")
                parameter("format", "0,1,2")
                parameter("codec", "0,1,2")
                parameter("dolby", 5)
                parameter("panorama", 1)
                if (sessData.isNotEmpty()) {
                    header("Cookie", "SESSDATA=$sessData")
                }
            }.body()
        }

    /**
     * 获取直播推荐列表
     * @param parentAreaId 一级分区ID（0=推荐全部）
     * @param areaId 二级分区ID（0=该一级分区全部）
     * @param page 页码，从1开始
     */
    suspend fun getLiveFeed(parentAreaId: Int = 0, areaId: Int = 0, page: Int = 1): LiveFeedResponse = withContext(Dispatchers.IO) {
        client.get("https://api.live.bilibili.com/xlive/app-interface/v2/index/feedV2") {
            parameter("parent_area_id", parentAreaId)
            parameter("area_id", areaId)
            parameter("device", "switch")
            parameter("page", page)
            parameter("platform", "web")
            parameter("scale", "xxhdpi")
            parameter("source_name", "pc")
            parameter("mobi_app", "pc_electron")
            // 添加登录凭证以获取关注列表
            if (!authRepository.sessionData.isNullOrBlank()) {
                header("Cookie", "SESSDATA=${authRepository.sessionData}")
            }
            encWebAppGet()
        }.body()
    }

    /**
     * 获取关注的正在直播的主播列表
     * @param page 页码，从1开始
     * @param pageSize 每页数量，默认30
     */
    suspend fun getLiveFollowing(page: Int = 1, pageSize: Int = 30): LiveFollowResponse =
        withContext(Dispatchers.IO) {
            client.get("https://api.live.bilibili.com/xlive/web-ucenter/user/following") {
                parameter("page", page)
                parameter("page_size", pageSize)
                parameter("ignoreRecord", 1)
                parameter("hit_ab", true)
                if (authRepository.accessToken != null) {
                    parameter("access_key", authRepository.accessToken)
                }
            }.body()
        }
}
