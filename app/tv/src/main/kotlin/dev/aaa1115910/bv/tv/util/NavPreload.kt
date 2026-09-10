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
 * 按设备可承受的保留页数限制相邻窗口。优先当前页和指定的相邻页，
 * 再补右侧下一页、左侧页。未指定偏好时保持向右预取的默认顺序。
 */
fun <T> boundedAdjacentNavItems(
    items: List<T>,
    current: T,
    step: Int = TOP_NAV_PRELOAD_STEP,
    maxItems: Int,
    preferredNeighbor: T? = null,
): List<T> {
    val currentIndex = items.indexOf(current)
    if (currentIndex < 0 || maxItems <= 1) return listOf(current)

    val result = mutableListOf(current)
    if (preferredNeighbor != null && preferredNeighbor != current) {
        val preferredIndex = items.indexOf(preferredNeighbor)
        if (preferredIndex >= 0 &&
            kotlin.math.abs(preferredIndex - currentIndex) <= step.coerceAtLeast(0)
        ) {
            result += preferredNeighbor
        }
    }
    for (distance in 1..step.coerceAtLeast(0)) {
        val nextIndex = currentIndex + distance
        if (nextIndex <= items.lastIndex && result.size < maxItems && items[nextIndex] !in result) {
            result += items[nextIndex]
        }
        val previousIndex = currentIndex - distance
        if (previousIndex >= 0 && result.size < maxItems && items[previousIndex] !in result) {
            result += items[previousIndex]
        }
        if (result.size >= maxItems) break
    }
    return result
}
