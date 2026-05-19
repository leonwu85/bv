package dev.aaa1115910.biliapi.http.entity.danmaku

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DanmakuPostData(
    @SerialName("dmid")
    val dmid: Long? = null
)
