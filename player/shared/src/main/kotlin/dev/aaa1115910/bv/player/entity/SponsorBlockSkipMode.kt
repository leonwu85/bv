package dev.aaa1115910.bv.player.entity

/**
 * SponsorBlock 跳过方式
 */
enum class SponsorBlockSkipMode(val value: Int) {
    /** 手动跳过 */
    Manual(0),
    /** 自动跳过 */
    Auto(1);

    companion object {
        fun fromValue(value: Int): SponsorBlockSkipMode = entries.find { it.value == value } ?: Manual
    }
}
