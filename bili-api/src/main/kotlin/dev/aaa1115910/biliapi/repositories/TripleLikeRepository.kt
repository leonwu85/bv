package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class TripleLikeRepository(
    private val authRepository: AuthRepository
) {
    suspend fun tripleLike(
        aid: Long,
        bvid: String? = null,
        preferApiType: ApiType = ApiType.Web
    ): TripleLikeResult {
        val (success, message) = BiliHttpApi.tripleLike(
            avid = aid,
            bvid = bvid,
            csrf = authRepository.biliJct,
            sessData = authRepository.sessionData
        )
        if (!success) {
            throw Exception("一键三连失败: $message")
        }
        return TripleLikeResult(like = true, coin = true, fav = true)
    }
}

data class TripleLikeResult(
    val like: Boolean,
    val coin: Boolean,
    val fav: Boolean
)
