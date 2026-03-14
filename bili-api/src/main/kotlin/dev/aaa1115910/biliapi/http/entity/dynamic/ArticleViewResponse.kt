package dev.aaa1115910.biliapi.http.entity.dynamic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 传统专栏详情响应数据
 * API: /x/article/view
 */
@Serializable
data class ArticleViewData(
    val id: Int? = null,
    val title: String? = null,
    val summary: String? = null,
    @SerialName("banner_url")
    val bannerUrl: String? = null,
    val content: String? = null,  // HTML 格式内容（旧版）
    val opus: ArticleOpus? = null // Opus 格式内容（新版）
)

/**
 * 传统专栏中的 Opus 内容
 */
@Serializable
data class ArticleOpus(
    @SerialName("opus_id")
    val opusId: Long? = null,
    val title: String? = null,
    val content: ArticleOpusContent? = null
)

/**
 * Opus 内容，包含段落列表
 */
@Serializable
data class ArticleOpusContent(
    val paragraphs: List<OpusParagraph> = emptyList()
)
