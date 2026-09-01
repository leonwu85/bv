package dev.aaa1115910.bv.tv.util

/** Appends a page while keeping stable item IDs unique across and within pages. */
internal fun <T, K> MutableList<T>.appendDistinctBy(
    items: Iterable<T>,
    keySelector: (T) -> K,
) {
    val seenKeys = asSequence().mapTo(mutableSetOf(), keySelector)
    val newItems = items.filter { seenKeys.add(keySelector(it)) }
    addAll(newItems)
}
