package dev.aaa1115910.biliapi.entity.sponsorblock

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SponsorBlock 片段类型
 */
enum class SponsorCategory(val key: String, val displayName: String) {
    SPONSOR("sponsor", "恰饭广告"),
    INTRO("intro", "片头"),
    OUTRO("outro", "片尾"),
    INTERACTION("interaction", "互动提醒"),
    SELF_PROMO("selfpromo", "自我推广"),
    PREVIEW("preview", "预告/回顾"),
    MUSIC_OFF_TOPIC("music_offtopic", "非音乐部分"),
    FILLER("filler", "填充内容");

    companion object {
        fun fromKey(key: String): SponsorCategory? {
            return entries.find { it.key == key }
        }
    }
}

/**
 * SponsorBlock API 返回的片段数据
 * API: GET /api/skipSegments?videoID={bvid}&cid={cid}
 */
@Serializable
data class SponsorSegment(
    val category: String,
    @SerialName("actionType")
    val actionType: String = "skip",
    val segment: List<Double>,
    val UUID: String,
    @SerialName("videoDuration")
    val videoDuration: Double? = null,
    val locked: Int = 0,
    val votes: Int = 0,
    val description: String? = null
) {
    /**
     * 片段开始时间（毫秒）
     */
    val startTime: Long
        get() = (segment.getOrElse(0) { 0.0 } * 1000).toLong()

    /**
     * 片段结束时间（毫秒）
     */
    val endTime: Long
        get() = (segment.getOrElse(1) { 0.0 } * 1000).toLong()

    /**
     * 片段时长（毫秒）
     */
    val duration: Long
        get() = endTime - startTime

    /**
     * 获取片段类型枚举
     */
    val categoryEnum: SponsorCategory?
        get() = SponsorCategory.fromKey(category)
}
