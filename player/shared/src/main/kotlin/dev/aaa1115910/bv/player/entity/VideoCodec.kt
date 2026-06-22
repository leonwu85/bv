package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.biliapi.entity.CodeType
import dev.aaa1115910.bv.player.shared.R

enum class VideoCodec(
    private val strRes: Int,
    val prefix: String,
    val codecId: Int,
    vararg aliases: String
) {
    AVC(R.string.video_codec_avc, "avc1", 7),
    HEVC(R.string.video_codec_hevc, "hev1", 12),
    AV1(R.string.video_codec_av1, "av01", 13),
    DVH1(R.string.video_codec_dvh1, "dvh1", 0, "dvhe"),
    HVC1(R.string.video_codec_hvc1, "hvc1", 0);

    private val codecPrefixes = listOf(prefix, *aliases)

    companion object {
        fun fromCode(code: Int?) = runCatching {
            entries.find { it.ordinal == code }!!
        }.getOrDefault(AVC)

        fun fromCodecString(codec: String?) = runCatching {
            entries.forEach {
                if (it.matchesCodecString(codec)) return@runCatching it
            }
            return@runCatching null
        }.getOrNull()

        fun fromCodecId(codecId: Int) = runCatching {
            entries.find { it.codecId == codecId }!!
        }.getOrDefault(AVC)
    }

    fun getDisplayName(context: Context) = context.getString(strRes)

    fun matchesCodecString(codec: String?) = codec?.let { codecString ->
        codecPrefixes.any { codecString.startsWith(it, ignoreCase = true) }
    } ?: false

    fun toBiliApiCodeType() = when (this) {
        AVC -> CodeType.Code264
        HEVC -> CodeType.Code265
        AV1 -> CodeType.CodeAv1
        DVH1, HVC1 -> CodeType.Code265
    }
}
