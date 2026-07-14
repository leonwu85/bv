package dev.aaa1115910.bv.viewmodel

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.entity.PlayDataUnavailableException
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.http.entity.RiskControlException
import dev.aaa1115910.biliapi.http.entity.VVoucherException
import kotlinx.coroutines.CancellationException
import java.io.IOException

internal suspend fun resolvePlayableVodPlayData(
    preferredApi: ApiType,
    fetch: suspend (ApiType) -> PlayData,
    onFailure: suspend (ApiType, Throwable) -> Unit = { _, _ -> },
    onEmpty: suspend (ApiType, PlayData) -> Unit = { _, _ -> },
    onFallback: suspend (ApiType, ApiType) -> Unit = { _, _ -> },
    onFallbackSuccess: suspend (ApiType) -> Unit = {}
): PlayData {
    val fallbackApi = when (preferredApi) {
        ApiType.Web -> ApiType.App
        ApiType.App -> ApiType.Web
    }
    val failures = mutableListOf<Throwable>()
    val attemptDetails = mutableListOf<String>()

    listOf(preferredApi, fallbackApi).forEachIndexed { index, api ->
        val result = runCatching { fetch(api) }
        val failure = result.exceptionOrNull()
        if (failure is CancellationException) throw failure
        if (failure is VVoucherException) throw failure

        if (failure != null) {
            failures += failure
            attemptDetails += "${api.playUrlSourceName()}失败：${failure.localizedMessage}"
            onFailure(api, failure)
        } else {
            val playData = result.getOrThrow()
            if (playData.needPay || playData.hasPlayableVodStreams()) {
                if (index > 0) onFallbackSuccess(api)
                return playData
            }
            attemptDetails +=
                "${api.playUrlSourceName()}空流(video=${playData.dashVideos.size}, audio=${playData.playableAudioCount()})"
            onEmpty(api, playData)
        }

        if (index == 0) onFallback(api, fallbackApi)
    }

    failures.firstOrNull { !it.isExpectedPlayDataFailure() }?.let { throw it }
    val exception = PlayDataUnavailableException(
        "WEB、APP 接口均未返回可播放音视频流：${attemptDetails.joinToString("；")}",
        failures.firstOrNull()
    )
    failures.drop(1).forEach(exception::addSuppressed)
    throw exception
}

internal fun ApiType.playUrlSourceName(): String = when (this) {
    ApiType.Web -> "WEB 接口"
    ApiType.App -> "APP 接口"
}

private fun Throwable.isExpectedPlayDataFailure(): Boolean =
    this is PlayDataUnavailableException ||
        this is AuthFailureException ||
        this is RiskControlException ||
        this is IOException ||
        this is IllegalStateException
