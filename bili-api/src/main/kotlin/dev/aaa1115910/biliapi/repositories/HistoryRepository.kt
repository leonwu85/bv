package dev.aaa1115910.biliapi.repositories

import bilibili.app.interfaces.v1.HistoryGrpcKt
import bilibili.app.interfaces.v1.cursor
import bilibili.app.interfaces.v1.cursorV2Req
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.HistoryData
import dev.aaa1115910.biliapi.http.BiliHttpApi
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

@Single
class HistoryRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val historyStub
        get() = runCatching {
            HistoryGrpcKt.HistoryCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getHistories(
        cursor: Long,
        preferApiType: ApiType = ApiType.Web
    ): HistoryData {
        return when (preferApiType) {
            ApiType.Web -> {
                val data = BiliHttpApi.getHistories(
                    viewAt = cursor,
                    sessData = authRepository.sessionData!!,
                ).getResponseData()
                HistoryData.fromHistoryResponse(data)
            }

            ApiType.App -> {
                val reply = historyStub?.cursorV2(cursorV2Req {
                    this.cursor = cursor {
                        max = cursor
                    }
                    business = "archive"
                })
                HistoryData.fromHistoryResponse(reply!!)
            }
        }
    }

    suspend fun deleteHistory(
        business: String,
        kid: Long
    ): Boolean = deleteHistories(listOf(business to kid))

    suspend fun deleteHistories(items: List<Pair<String, Long>>): Boolean {
        if (items.isEmpty()) return true
        return runCatching {
            BiliHttpApi.deleteHistory(
                kid = items.joinToString(",") { (business, kid) -> "${business}_$kid" },
                csrf = authRepository.biliJct!!,
                sessData = authRepository.sessionData!!
            ).code == 0
        }.getOrElse { false }
    }

    suspend fun searchHistories(
        keyword: String,
        pageNumber: Int = 1
    ): HistoryData {
        val data = BiliHttpApi.searchHistory(
            keyword = keyword,
            pageNum = pageNumber,
            sessData = authRepository.sessionData ?: error("账号未登录")
        ).getResponseData()
        return HistoryData.fromHistoryResponse(data)
    }

    suspend fun getHistoryPaused(): Boolean = BiliHttpApi.getHistoryPaused(
        sessData = authRepository.sessionData ?: error("账号未登录")
    ).getResponseData()

    suspend fun setHistoryPaused(paused: Boolean) {
        BiliHttpApi.setHistoryPaused(
            paused = paused,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录")
        ).requireSuccess()
    }

    suspend fun clearHistory() {
        BiliHttpApi.clearHistory(
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录")
        ).requireSuccess()
    }
}
