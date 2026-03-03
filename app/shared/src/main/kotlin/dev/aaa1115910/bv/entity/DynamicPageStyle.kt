package dev.aaa1115910.bv.entity

import android.content.Context
import dev.aaa1115910.bv.R

enum class DynamicPageStyle(private val strRes: Int, val value: Int) {
    New(R.string.dynamic_style_new, 0),
    Legacy(R.string.dynamic_style_legacy, 1);

    fun getDisplayName(context: Context) = context.getString(strRes)

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: New
    }
}
