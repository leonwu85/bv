package dev.aaa1115910.bv.mobile.screen.common

data class MobileListUiState<T>(
    val items: List<T> = emptyList(),
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val errorMessage: String? = null,
    val endReached: Boolean = false,
    val emptyMessage: String = "暂无内容",
) {
    val isInitialLoading: Boolean get() = items.isEmpty() && refreshing && errorMessage == null
    val isEmpty: Boolean get() = items.isEmpty() && !refreshing && errorMessage == null
    val canLoadMore: Boolean get() = !loadingMore && !endReached && errorMessage == null
}
