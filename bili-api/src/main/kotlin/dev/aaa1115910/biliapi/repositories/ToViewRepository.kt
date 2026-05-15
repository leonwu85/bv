package dev.aaa1115910.biliapi.repositories

import bilibili.app.interfaces.v1.HistoryGrpcKt
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.ToViewData
import dev.aaa1115910.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class ToViewRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val historyStub
        get() = runCatching {
            HistoryGrpcKt.HistoryCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getToView(
        cursor: Long,
        preferApiType: ApiType = ApiType.Web
    ): ToViewData {
        return when (preferApiType) {
            ApiType.Web -> {
                val data = BiliHttpApi.getToView(
                    // viewAt = cursor,
                    sessData = authRepository.sessionData!!,
                ).getResponseData()
                print(data)
                ToViewData.fromToViewResponse(data)
            }

            ApiType.App -> {
                val data = BiliHttpApi.getToView(
                    // viewAt = cursor,
                    sessData = authRepository.sessionData!!,
                ).getResponseData()
                ToViewData.fromToViewResponse(data)
            }
        }
    }

    suspend fun deleteToView(
        avid: Long,
        preferApiType: ApiType = ApiType.Web
    ): Boolean {
        return runCatching {
            BiliHttpApi.deleteToView(
                avid = avid,
                csrf = authRepository.biliJct!!,
                sessData = authRepository.sessionData!!
            ).code == 0
        }.getOrElse { false }
    }

    suspend fun addToView(
        avid: Long? = null,
        bvid: String? = null
    ) {
        val response = BiliHttpApi.addToView(
            avid = avid,
            bvid = bvid,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录")
        )
        if (response.code != 0) {
            throw Exception("添加稍后再看失败: ${response.message}")
        }
    }

    suspend fun deleteToViewOrThrow(
        avid: Long
    ) {
        val response = BiliHttpApi.deleteToView(
            avid = avid,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录")
        )
        if (response.code != 0) {
            throw Exception("移出稍后再看失败: ${response.message}")
        }
    }
}
