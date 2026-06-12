package dev.aaa1115910.bv.viewmodel.message

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.message.DirectMessageAction
import dev.aaa1115910.biliapi.entity.message.DirectMessageFeedUnread
import dev.aaa1115910.biliapi.entity.message.DirectMessageOffset
import dev.aaa1115910.biliapi.entity.message.DirectMessageSession
import dev.aaa1115910.biliapi.repositories.MessageRepository
import dev.aaa1115910.bv.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class InboxViewModel(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    val sessions = mutableStateListOf<DirectMessageSession>()

    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var hasMore by mutableStateOf(true)
        private set
    var unreadCount by mutableStateOf(0)
        private set
    var feedUnread by mutableStateOf(DirectMessageFeedUnread())
        private set
    var actions by mutableStateOf(emptyList<DirectMessageAction>())
        private set
    var outsideActions by mutableStateOf(emptyList<DirectMessageAction>())
        private set

    private var nextCursor: Long? = null
    private var nextOffsets: Map<Int, DirectMessageOffset> = emptyMap()

    val isLogin: Boolean get() = userRepository.isLogin
    val initialLoading: Boolean get() = sessions.isEmpty() && refreshing

    init {
        refresh()
    }

    fun refresh() {
        if (refreshing) return
        if (!userRepository.isLogin) {
            sessions.clear()
            unreadCount = 0
            feedUnread = DirectMessageFeedUnread()
            actions = emptyList()
            outsideActions = emptyList()
            errorMessage = "请先登录"
            hasMore = false
            nextCursor = null
            nextOffsets = emptyMap()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                refreshing = true
                errorMessage = null
            }
            runCatching {
                val unread = messageRepository.getUnreadCount()
                val feed = runCatching { messageRepository.getFeedUnread() }
                    .getOrDefault(DirectMessageFeedUnread())
                val page = messageRepository.getSessions()
                withContext(Dispatchers.Main) {
                    unreadCount = unread
                    feedUnread = feed
                    sessions.clear()
                    sessions.addAll(page.sessions)
                    nextCursor = page.nextCursor
                    nextOffsets = page.nextOffsets
                    hasMore = page.hasMore &&
                            page.sessions.isNotEmpty() &&
                            (nextOffsets.isNotEmpty() || nextCursor != null)
                    actions = page.actions
                    outsideActions = page.outsideActions
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
        val cursor = nextCursor
        val offsets = nextOffsets
        if (cursor == null && offsets.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingMore = true
            }
            runCatching {
                messageRepository.getSessions(cursor = cursor, offsets = offsets)
            }.onSuccess { page ->
                withContext(Dispatchers.Main) {
                    val existed = sessions.map { it.talkerId }.toSet()
                    val newSessions = page.sessions.filterNot { it.talkerId in existed }
                    val paginationMoved = when {
                        offsets.isNotEmpty() || page.nextOffsets.isNotEmpty() -> page.nextOffsets != offsets
                        cursor != null || page.nextCursor != null -> page.nextCursor != cursor
                        else -> false
                    }
                    sessions.addAll(newSessions)
                    hasMore = page.hasMore && newSessions.isNotEmpty() && paginationMoved
                    nextCursor = page.nextCursor
                    nextOffsets = page.nextOffsets
                    if (page.actions.isNotEmpty()) actions = page.actions
                    if (page.outsideActions.isNotEmpty()) outsideActions = page.outsideActions
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

    fun markFeedUnreadRead(index: Int) {
        feedUnread = when (index) {
            0 -> feedUnread.copy(reply = 0)
            1 -> feedUnread.copy(at = 0)
            2 -> feedUnread.copy(like = 0)
            3 -> feedUnread.copy(sysMsg = 0)
            else -> feedUnread
        }
    }

    fun clearAllUnread() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                messageRepository.clearAllUnread()
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    for (index in sessions.indices) {
                        sessions[index] = sessions[index].copy(unreadCount = 0)
                    }
                    unreadCount = 0
                    feedUnread = DirectMessageFeedUnread()
                    errorMessage = null
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "清除失败"
                }
            }
        }
    }

    fun deleteAllSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                messageRepository.deleteSessionList()
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    sessions.clear()
                    unreadCount = 0
                    feedUnread = DirectMessageFeedUnread()
                    hasMore = false
                    nextCursor = null
                    nextOffsets = emptyMap()
                    errorMessage = null
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "清空失败"
                }
            }
        }
    }

    fun markLocalRead(talkerId: Long) {
        val index = sessions.indexOfFirst { it.talkerId == talkerId }
        if (index == -1) return
        val count = sessions[index].unreadCount
        sessions[index] = sessions[index].copy(unreadCount = 0)
        unreadCount = (unreadCount - count).coerceAtLeast(0)
    }

    fun setPinned(session: DirectMessageSession) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                messageRepository.setPinned(session.talkerId, !session.isPinned)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    val index = sessions.indexOfFirst { it.talkerId == session.talkerId }
                    if (index != -1) {
                        val updated = sessions.removeAt(index).copy(isPinned = !session.isPinned)
                        if (updated.isPinned) {
                            sessions.add(0, updated)
                        } else {
                            sessions.add(index.coerceAtMost(sessions.size), updated)
                        }
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "设置失败"
                }
            }
        }
    }

    fun remove(session: DirectMessageSession) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                messageRepository.removeSession(session.talkerId)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    sessions.removeAll { it.talkerId == session.talkerId }
                    unreadCount = (unreadCount - session.unreadCount).coerceAtLeast(0)
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "删除失败"
                }
            }
        }
    }
}
