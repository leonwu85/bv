package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

/** Server quality IDs are extensible; unknown IDs remain playable. */
class Resolution private constructor(
    val code: Int,
    private val strResLong: Int = 0,
    private val strResShort: Int = strResLong,
    private val description: String? = null,
) : Comparable<Resolution> {
    val name get() = "QN$code"
    val isHdr get() = code == 125 || code == 126 || code == 129
    override fun compareTo(other: Resolution) = code.compareTo(other.code)
    override fun equals(other: Any?) = other is Resolution && code == other.code
    override fun hashCode() = code
    override fun toString() = name

    companion object {
        val R240P = Resolution(6, R.string.resolution_240p, R.string.resolution_240p_short)
        val R360P = Resolution(16, R.string.resolution_360p, R.string.resolution_360p_short)
        val R480P = Resolution(32, R.string.resolution_480p, R.string.resolution_480p_short)
        val R720P = Resolution(64, R.string.resolution_720p, R.string.resolution_720p_short)
        val R720P60 = Resolution(74, R.string.resolution__720p_60, R.string.resolution_720p_60_short)
        val R1080P = Resolution(80, R.string.resolution_1080p, R.string.resolution_1080p_short)
        val R1080PPlus = Resolution(112, R.string.resolution_1080p_plus, R.string.resolution_1080p_plus_short)
        val R1080P60 = Resolution(116, R.string.resolution_1080p_60, R.string.resolution_1080p_60_short)
        val R4K = Resolution(120, R.string.resolution_4k, R.string.resolution_4k_short)
        val RHdr = Resolution(125, R.string.resolution_hdr, R.string.resolution_hdr_short)
        val RDolby = Resolution(126, R.string.resolution_dolby_vision, R.string.resolution_dolby_vision_short)
        val R8K = Resolution(127, R.string.resolution_8k, R.string.resolution_8k_short)

        val RSuperResolution = Resolution(100, description = "智能修复")
        val RHdrVivid = Resolution(129, description = "HDR Vivid")
        val entries = listOf(R240P, R360P, R480P, R720P, R720P60, R1080P,
            RSuperResolution, R1080PPlus, R1080P60, R4K, RHdr, RDolby, R8K, RHdrVivid)

        fun fromCode(code: Int, description: String? = null): Resolution? {
            if (code <= 0) return null
            return entries.find { it.code == code }
                ?: Resolution(code, description = description?.takeIf(String::isNotBlank))
        }
    }

    fun getDisplayName(context: Context): String = description
        ?: if (strResLong != 0) context.getString(strResLong) else "清晰度 $code"
    fun getShortDisplayName(context: Context): String = description
        ?: if (strResShort != 0) context.getString(strResShort) else "清晰度 $code"
}
