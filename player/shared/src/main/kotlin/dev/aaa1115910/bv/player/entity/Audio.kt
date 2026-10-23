package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

class Audio private constructor(val code: Int, val rank: Int, private val strRes: Int = 0) {
    val name get() = "Audio$code"
    override fun equals(other: Any?) = other is Audio && code == other.code
    override fun hashCode() = code
    override fun toString() = name
    fun getDisplayName(context: Context) = if (strRes != 0) context.getString(strRes) else "音质 $code"

    companion object {
        val A64K = Audio(30216, 20, R.string.audio_64k)
        val A132K = Audio(30232, 30, R.string.audio_132k)
        val A192K = Audio(30280, 40, R.string.audio_192k)
        val ADolbyAtmos = Audio(30250, 50, R.string.audio_dolby_atmos)
        val ADolbyAtmos30255 = Audio(30255, 50, R.string.audio_dolby_atmos)
        val AHiRes = Audio(30251, 60, R.string.audio_hi_res)
        // Preferences select an audio tier; 30255 is another encoding of the Dolby tier.
        val entries = listOf(A64K, A132K, A192K, ADolbyAtmos, AHiRes)
        fun fromCode(code: Int): Audio? = if (code <= 0) null else
            if (code == ADolbyAtmos30255.code) ADolbyAtmos30255 else entries.find { it.code == code } ?: Audio(code, 0)
    }
}
