package dev.aaa1115910.biliapi.http.entity.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserNavStatData(
    val following: Int = 0,
    val follower: Int = 0,
    @SerialName("dynamic_count")
    val dynamicCount: Int = 0
)
