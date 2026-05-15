package dev.aaa1115910.bv.entity

import android.content.Context
import dev.aaa1115910.bv.R

enum class DynamicTabType(private val strRes: Int, val value: Int) {
    All(R.string.dynamic_tab_all, 0),
    Video(R.string.dynamic_tab_video, 1),
    Pgc(R.string.dynamic_tab_pgc, 2),
    Article(R.string.dynamic_tab_article, 3),
    Up(R.string.dynamic_tab_up, 4);

    fun getDisplayName(context: Context) = context.getString(strRes)

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: All
    }
}
