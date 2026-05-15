package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class LikeRepository(
    private val authRepository: AuthRepository
) {
    suspend fun checkVideoLike(
        aid: Long,
        preferApiType: ApiType = ApiType.Web
    ): Boolean {
        return when (preferApiType) {
            ApiType.Web -> BiliHttpApi.checkVideoLiked(
                avid = aid,
                sessData = authRepository.sessionData
            )

            ApiType.App -> BiliHttpApi.checkVideoLiked(
                avid = aid,
                accessKey = authRepository.accessToken
            )
        }
    }

    suspend fun addVideoLike(
        aid: Long,
        preferApiType: ApiType = ApiType.Web
    ) {
        val (success, message) = when (preferApiType) {
            ApiType.Web -> BiliHttpApi.sendVideoLike(
                avid = aid,
                like = true,
                sessData = authRepository.sessionData,
                csrf = authRepository.biliJct
            )

            ApiType.App -> BiliHttpApi.sendVideoLike(
                avid = aid,
                like = true,
                accessKey = authRepository.accessToken
            )
        }
        if (!success) {
            throw Exception("点赞失败: $message")
        }
    }

    suspend fun delVideoLike(
        aid: Long,
        preferApiType: ApiType = ApiType.Web
    ) {
        val (success, message) = when (preferApiType) {
            ApiType.Web -> BiliHttpApi.sendVideoLike(
                avid = aid,
                like = false,
                sessData = authRepository.sessionData,
                csrf = authRepository.biliJct
            )

            ApiType.App -> BiliHttpApi.sendVideoLike(
                avid = aid,
                like = false,
                accessKey = authRepository.accessToken
            )
        }
        if (!success) {
            throw Exception("取消点赞失败: $message")
        }
    }

    suspend fun addVideoDislike(
        aid: Long
    ) {
        val (success, message) = BiliHttpApi.sendVideoDislike(
            avid = aid,
            dislike = true,
            accessKey = authRepository.accessToken
        )
        if (!success) {
            throw Exception("点踩失败: $message")
        }
    }

    suspend fun delVideoDislike(
        aid: Long
    ) {
        val (success, message) = BiliHttpApi.sendVideoDislike(
            avid = aid,
            dislike = false,
            accessKey = authRepository.accessToken
        )
        if (!success) {
            throw Exception("取消点踩失败: $message")
        }
    }

    suspend fun addDynamicLike(
        dynamicId: String,
        preferApiType: ApiType = ApiType.Web
    ) {
        val (success, message) = BiliHttpApi.sendDynamicLike(
            dynamicId = dynamicId,
            like = true,
            sessData = authRepository.sessionData,
            csrf = authRepository.biliJct
        )
        if (!success) {
            throw Exception("动态点赞失败: $message")
        }
    }

    suspend fun delDynamicLike(
        dynamicId: String,
        preferApiType: ApiType = ApiType.Web
    ) {
        val (success, message) = BiliHttpApi.sendDynamicLike(
            dynamicId = dynamicId,
            like = false,
            sessData = authRepository.sessionData,
            csrf = authRepository.biliJct
        )
        if (!success) {
            throw Exception("取消动态点赞失败: $message")
        }
    }

    suspend fun checkDynamicLike(
        dynamicId: String,
        preferApiType: ApiType = ApiType.Web
    ): Boolean {
        return BiliHttpApi.checkDynamicLiked(
            dynamicId = dynamicId,
            sessData = authRepository.sessionData
        )
    }
}
