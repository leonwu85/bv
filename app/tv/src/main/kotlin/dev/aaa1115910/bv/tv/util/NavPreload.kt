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

/**
 * 按设备可承受的保留页数限制相邻窗口。优先当前页和右侧下一页，再补左侧页。
 */
fun <T> boundedAdjacentNavItems(
    items: List<T>,
    current: T,
    step: Int = TOP_NAV_PRELOAD_STEP,
    maxItems: Int,
): List<T> {
    val currentIndex = items.indexOf(current)
    if (currentIndex < 0 || maxItems <= 1) return listOf(current)

    val result = mutableListOf(current)
    for (distance in 1..step.coerceAtLeast(0)) {
        val nextIndex = currentIndex + distance
        if (nextIndex <= items.lastIndex && result.size < maxItems) {
            result += items[nextIndex]
        }
        val previousIndex = currentIndex - distance
        if (previousIndex >= 0 && result.size < maxItems) {
            result += items[previousIndex]
        }
        if (result.size >= maxItems) break
    }
    return result
}
