package dev.aaa1115910.bv.entity

import android.content.Context
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.stringRes

enum class LiveQualityPreference(private val strRes: Int, val qn: Int) {
    Dolby(R.string.live_quality_dolby, 30000),
    Origin4K(R.string.live_quality_origin_4k, 25000),
    Super4K(R.string.live_quality_super_4k, 20000),
    Super2K(R.string.live_quality_super_2k, 15000),
    Origin(R.string.live_quality_origin, 10000),
    BluRay(R.string.live_quality_bluray, 400),
    SuperHD(R.string.live_quality_super_hd, 250),
    Smooth(R.string.live_quality_smooth, 150),
    Fluent(R.string.live_quality_fluent, 80);

    fun getDisplayName(context: Context): String = strRes.stringRes(context)

    companion object {
        fun fromQn(qn: Int): LiveQualityPreference =
            entries.find { it.qn == qn } ?: Origin

        fun resolveRequestedQn(preferredQn: Int, acceptQn: List<Int>): Int {
            val normalizedQn = fromQn(preferredQn).qn
            if (acceptQn.isEmpty()) return normalizedQn
            if (acceptQn.contains(normalizedQn)) return normalizedQn
            return acceptQn.maxOrNull() ?: normalizedQn
        }
    }
}