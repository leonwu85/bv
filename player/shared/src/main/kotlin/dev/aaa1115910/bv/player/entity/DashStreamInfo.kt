package dev.aaa1115910.bv.player.entity

/** 一个子段（sidx 的一条引用）：文件内闭区间字节范围与时长（以 [DashSegmentIndex.timescale] 计） */
data class DashSegment(
    val startByte: Long,
    val endByte: Long,
    val durationTicks: Long,
)

/**
 * 从 fMP4 的 `sidx` 解析出的子段表。有了它就能给内核一份带精确字节范围的段列表，
 * 而不必让内核自己下载并解释 sidx。
 */
data class DashSegmentIndex(
    val timescale: Long,
    val segments: List<DashSegment>,
) {
    val totalDurationTicks: Long
        get() = segments.sumOf { it.durationTicks }
}

/**
 * 一条 DASH on-demand 表示（单个 fMP4 文件 + sidx 索引），来自 B 站 playurl 的 `dash.video[]` / `dash.audio[]`。
 *
 * @param initRange `SegmentBase/Initialization@range`，形如 "0-1017"；未知为 null
 * @param indexRange `SegmentBase@indexRange`（sidx 所在字节范围），形如 "1018-1613"；未知为 null
 * @param segmentIndex 解析好的子段表；为 null 时只能依赖内核自己处理 sidx
 */
data class DashRepresentation(
    val url: String,
    val mimeType: String,
    val codecs: String? = null,
    val bandwidth: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val initRange: String? = null,
    val indexRange: String? = null,
    val segmentIndex: DashSegmentIndex? = null,
) {
    val hasSegmentIndex: Boolean
        get() = segmentIndex != null && segmentIndex.segments.isNotEmpty() && segmentIndex.timescale > 0
}

/**
 * 当前要播放的视频/音频表示对，供需要完整 DASH 描述的内核（如 VLC 4 的 adaptive 解复用器）
 * 在两个独立的 URL 之外还能拿到时长、编码与分段信息。
 */
data class DashStreamInfo(
    val durationMs: Long,
    val video: DashRepresentation,
    val audio: DashRepresentation?,
) {
    /** 视频与（如有）音频都带解析好的子段表，才能生成按段寻址的 MPD */
    val hasSegmentIndexes: Boolean
        get() = video.hasSegmentIndex && (audio == null || audio.hasSegmentIndex)
}
