package dev.aaa1115910.bv.tv.util

/** TopNav 相邻页预加载的默认步进：当前页 ±1 */
const val TOP_NAV_PRELOAD_STEP = 1

/**
 * 按导航顺序取 [current] 左右各 [step] 个（含当前），用于预加载。
 *
 * 例：step=1 时返回 [prev, current, next]（边界处可能不足 3 个）。
 */
fun <T> adjacentNavItems(
    items: List<T>,
    current: T,
    step: Int = TOP_NAV_PRELOAD_STEP,
): List<T> {
    if (items.isEmpty()) return emptyList()
    val index = items.indexOf(current).takeIf { it >= 0 } ?: return listOf(current)
    val safeStep = step.coerceAtLeast(0)
    val from = (index - safeStep).coerceAtLeast(0)
    val to = (index + safeStep).coerceAtMost(items.lastIndex)
    return items.subList(from, to + 1)
}
