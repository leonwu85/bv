package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.Serializable

@Serializable
data class RankVideoData(
    val list: List<VideoInfo>
)
