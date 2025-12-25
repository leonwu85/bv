package dev.aaa1115910.bv.tv.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import dev.aaa1115910.bv.tv.component.HomeTopNavItem
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 导航项配置数据类
 */
data class NavItemConfig(
    val ordinal: Int,
    val hidden: Boolean
)

/**
 * 获取根据设置过滤和排序后的首页导航项列表
 */
val homeNavItemsFlow: Flow<List<HomeTopNavItem>>
    get() = Prefs.homeNavItemsOrderFlow.map { orderString ->
        parseHomeNavItemsOrder(orderString)
    }

/**
 * 解析导航项排序字符串
 * @param orderString 逗号分隔的 ordinal 列表，负数表示隐藏
 * @return 过滤和排序后的导航项列表
 */
fun parseHomeNavItemsOrder(orderString: String): List<HomeTopNavItem> {
    if (orderString.isBlank()) return HomeTopNavItem.entries

    return orderString
        .split(",")
        .mapNotNull { part ->
            val ordinal = part.toIntOrNull() ?: return@mapNotNull null
            val isHidden = ordinal < 0
            val actualOrdinal = if (isHidden) -ordinal else ordinal
            Pair(actualOrdinal, isHidden)
        }
        .filter { !it.second }
        .mapNotNull { (ordinal, _) -> HomeTopNavItem.entries.getOrNull(ordinal) }
}

/**
 * 将指定导航项移到第一位并取消隐藏
 * 用于切换默认标签时，将新默认标签移到第一位
 * @param orderString 当前排序配置字符串
 * @param ordinal 要移到第一位的导航项 ordinal
 * @return 更新后的排序配置字符串
 */
fun moveNavItemToFirstAndUnhide(orderString: String, ordinal: Int): String {
    if (orderString.isBlank()) return orderString

    val parts = orderString.split(",").map { part ->
        val num = part.toIntOrNull() ?: return@map part to false
        val absNum = if (num < 0) -num else num
        absNum to (num < 0)
    }

    // 找到目标项
    val targetItem = parts.find { it.first == ordinal }
    if (targetItem == null) return orderString

    // 构建新的顺序：目标项在前，其他项按原顺序在后
    val otherItems = parts.filter { it.first != ordinal }
    val newParts = listOf(
        ordinal.toString()  // 默认标签在第一位，取消隐藏
    ) + otherItems.map { (ord, hidden) ->
        if (hidden) "-$ord" else "$ord"
    }

    return newParts.joinToString(",")
}

/**
 * 解析排序字符串为配置列表
 * @param orderString 逗号分隔的 ordinal 列表，负数表示隐藏
 * @return 导航项配置列表（按显示顺序）
 */
fun parseNavItemsOrderToConfig(orderString: String): List<NavItemConfig> {
    if (orderString.isBlank()) {
        return HomeTopNavItem.entries.map { NavItemConfig(it.ordinal, false) }
    }

    return orderString
        .split(",")
        .mapNotNull { part ->
            val ordinal = part.toIntOrNull() ?: return@mapNotNull null
            val isHidden = ordinal < 0
            val actualOrdinal = if (isHidden) -ordinal else ordinal
            NavItemConfig(actualOrdinal, isHidden)
        }
}
