package dev.aaa1115910.bv.viewmodel.message

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.message.MessageFeedItem
import dev.aaa1115910.biliapi.entity.message.MessageFeedType
import dev.aaa1115910.biliapi.repositories.MessageRepository
import dev.aaa1115910.bv.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageFeedViewModel(
    private val type: MessageFeedType,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    val items = mutableStateListOf<MessageFeedItem>()

    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var hasMore by mutableStateOf(true)
        private set

    private var cursorId: Long? = null
    private var cursorTime: Long? = null

    val isLogin: Boolean get() = userRepository.isLogin
    val initialLoading: Boolean get() = items.isEmpty() && refreshing

    init {
        refresh()
    }

    fun refresh() {
        if (refreshing) return
        if (!userRepository.isLogin) {
            items.clear()
            errorMessage = "请先登录"
            hasMore = false
            cursorId = null
            cursorTime = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                refreshing = true
                errorMessage = null
            }
            runCatching {
                messageRepository.getMessageFeed(type)
            }.onSuccess { page ->
                withContext(Dispatchers.Main) {
                    items.clear()
                    items.addAll(page.items.distinctBy(::itemKey))
                    cursorId = page.cursorId
                    cursorTime = page.cursorTime
                    hasMore = page.hasMore && page.items.isNotEmpty() && page.cursorId != null
                    errorMessage = null
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "加载失败"
                }
            }
            withContext(Dispatchers.Main) {
                refreshing = false
            }
        }
    }

    fun loadMore() {
        if (loadingMore || refreshing || !hasMore || !userRepository.isLogin) return
        val oldCursorId = cursorId ?: return
        val oldCursorTime = cursorTime
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingMore = true
            }
            runCatching {
                messageRepository.getMessageFeed(
                    type = type,
                    cursorId = oldCursorId,
                    cursorTime = oldCursorTime
                )
            }.onSuccess { page ->
                withContext(Dispatchers.Main) {
                    val existed = items.map(::itemKey).toSet()
                    val newItems = page.items.filterNot { itemKey(it) in existed }
                    val paginationMoved = page.cursorId != oldCursorId || page.cursorTime != oldCursorTime
                    items.addAll(newItems)
                    cursorId = page.cursorId
                    cursorTime = page.cursorTime
                    hasMore = page.hasMore && newItems.isNotEmpty() && paginationMoved
                    errorMessage = null
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "加载失败"
                }
            }
            withContext(Dispatchers.Main) {
                loadingMore = false
            }
        }
    }

    fun remove(item: MessageFeedItem) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                messageRepository.deleteMessageFeedItem(item)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    items.removeAll { it.type == item.type && it.id == item.id }
                    errorMessage = null
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "删除失败"
                }
            }
        }
    }

    private fun itemKey(item: MessageFeedItem): String =
        "${item.type.name}:${item.section}:${item.id}"
}
