package dev.aaa1115910.biliapi.http.entity.dynamic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Opus 详情响应数据
 * API: /x/polymer/web-dynamic/v1/opus/detail
 *
 * 注意：modules 是一个数组，每个元素有 module_type 字段标识类型
 */
@Serializable
data class OpusDetailData(
    val item: OpusItem? = null,
    val fallback: OpusFallback? = null
)

/**
 * Fallback 信息 - 当 Opus 内容实际为传统专栏时返回
 */
@Serializable
data class OpusFallback(
    val id: String? = null,  // 传统专栏 ID (cvid)
    val type: Int? = null    // 类型: 2 表示需要回退
)

@Serializable
data class OpusItem(
    @SerialName("id_str")
    val idStr: String,
    val basic: OpusBasic? = null,
    val modules: List<OpusModule> = emptyList()
)

@Serializable
data class OpusBasic(
    @SerialName("comment_id_str")
    val commentIdStr: String? = null,
    @SerialName("comment_type")
    val commentType: Int? = null
)

/**
 * Opus 模块 - 根据 module_type 区分不同类型
 */
@Serializable
data class OpusModule(
    @SerialName("module_type")
    val moduleType: String? = null,
    @SerialName("module_author")
    val moduleAuthor: OpusAuthorModule? = null,
    @SerialName("module_stat")
    val moduleStat: OpusStatModule? = null,
    @SerialName("module_title")
    val moduleTitle: OpusTitleModule? = null,
    @SerialName("module_content")
    val moduleContent: OpusContentModule? = null
)

@Serializable
data class OpusAuthorModule(
    val mid: Long,
    val name: String,
    val face: String,
    @SerialName("pub_action")
    val pubAction: String? = null,
    @SerialName("pub_time")
    val pubTime: String? = null,
    @SerialName("pub_ts")
    val pubTs: Long? = null
)

@Serializable
data class OpusStatModule(
    val comment: OpusStat? = null,
    val forward: OpusStat? = null,
    val like: OpusStat? = null,
    val favorite: OpusStat? = null
) {
    @Serializable
    data class OpusStat(
        val count: Int? = null,
        val status: Boolean? = null
    )
}

@Serializable
data class OpusTitleModule(
    val text: String? = null
)

/**
 * 专栏内容模块，包含段落列表
 */
@Serializable
data class OpusContentModule(
    val paragraphs: List<OpusParagraph> = emptyList()
)

/**
 * 段落数据
 * @param paraType 段落类型：1=文本, 2=图片, 3=分割线
 * @param text 文本段落内容
 * @param pic 图片段落内容
 * @param line 分割线内容
 */
@Serializable
data class OpusParagraph(
    @SerialName("para_type")
    val paraType: Int,
    val text: OpusTextParagraph? = null,
    val pic: OpusPicParagraph? = null,
    val line: OpusLineParagraph? = null,
    val format: OpusParagraphFormat? = null
)

@Serializable
data class OpusParagraphFormat(
    val align: Int? = null
)

// ============== 文本段落 ==============

@Serializable
data class OpusTextParagraph(
    val nodes: List<OpusTextNode> = emptyList()
)

/**
 * 文本节点
 * @param rawText 原始文本
 * @param word 普通文字节点
 * @param rich 富文本节点（链接、@等）
 * @param emote 表情节点
 */
@Serializable
data class OpusTextNode(
    @SerialName("raw_text")
    val rawText: String = "",
    val type: String? = null,
    val word: OpusWord? = null,
    val rich: OpusRich? = null,
    val emote: OpusEmote? = null
)

@Serializable
data class OpusWord(
    val words: String? = null,
    @SerialName("font_size")
    val fontSize: Int? = null,
    val style: OpusStyle? = null
)

@Serializable
data class OpusStyle(
    val bold: Boolean? = null,
    val italic: Boolean? = null
)

@Serializable
data class OpusRich(
    val text: String? = null,
    @SerialName("orig_text")
    val origText: String? = null,
    @SerialName("jump_url")
    val jumpUrl: String? = null,
    val type: String? = null,
    val rid: String? = null
)

@Serializable
data class OpusEmote(
    val id: Long? = null,
    val text: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    val size: Int? = null
)

// ============== 图片段落 ==============

@Serializable
data class OpusPicParagraph(
    val pics: List<OpusPic> = emptyList(),
    val style: Int? = null
)

@Serializable
data class OpusPic(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val size: Double? = null,
    @SerialName("live_url")
    val liveUrl: String? = null
)

// ============== 分割线 ==============

@Serializable
data class OpusLineParagraph(
    val pic: OpusLinePic? = null
)

@Serializable
data class OpusLinePic(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)
