package dev.aaa1115910.biliapi.entity

enum class FavoriteTransferMode(val apiValue: String) { Copy("copy"), Move("move") }

/** The service inserts at the front: reverse display order once, independently of click order. */
data class FavoriteTransferRequest(
    val sourceId: Long,
    val targetId: Long,
    val resourcesInDisplayOrder: List<Pair<Long, FavoriteItemType>>
) {
    init {
        require(sourceId > 0 && targetId > 0 && sourceId != targetId) { "请选择不同的目标收藏夹" }
        require(resourcesInDisplayOrder.isNotEmpty()) { "请先选择收藏内容" }
        require(resourcesInDisplayOrder.all { it.first > 0 && it.second.value > 0 }) { "无效的收藏资源" }
    }
    val resources: String get() = resourcesInDisplayOrder.distinct().asReversed()
        .joinToString(",") { (id, type) -> "$id:${type.value}" }
}
