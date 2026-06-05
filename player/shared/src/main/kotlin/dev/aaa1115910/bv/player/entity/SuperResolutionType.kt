package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

enum class SuperResolutionType(
    val value: Int,
    private val strRes: Int
) {
    Disable(0, R.string.super_resolution_disable),
    EfficiencyAnime(1, R.string.super_resolution_efficiency_anime),
    EfficiencyFsrcnnx(2, R.string.super_resolution_efficiency_fsrcnnx),
    QualityAnime(3, R.string.super_resolution_quality_anime),
    QualityFsrcnnx(4, R.string.super_resolution_quality_fsrcnnx);

    fun displayName(context: Context): String = context.getString(strRes)

    companion object {
        fun fromValue(value: Int): SuperResolutionType =
            entries.find { it.value == value } ?: Disable
    }
}
