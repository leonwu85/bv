package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

/**
 * 直播编码格式选项
 *
 * @property protocolName 协议名称，对应 B站 API 返回的 protocolName
 * @property codecName 编码名称，null 表示自动选择最佳编码
 */
enum class LiveCodec(private val strRes: Int, val protocolName: String, val codecName: String?) {
    HLS(R.string.live_codec_hls, "http_hls", null),      // HLS 自动选择最佳编码
    FLV(R.string.live_codec_flv, "http_stream", "avc"),  // FLV 固定 AVC
    AVC(R.string.live_codec_avc, "http_hls", "avc");     // HLS 强制 AVC

    companion object {
        fun fromCode(code: Int) = runCatching {
            entries.find { it.ordinal == code }!!
        }.getOrDefault(HLS)
    }

    fun getDisplayName(context: Context) = context.getString(strRes)
}
