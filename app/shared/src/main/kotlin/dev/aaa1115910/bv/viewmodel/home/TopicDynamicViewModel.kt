package dev.aaa1115910.bv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.user.DynamicTopicFeedItem
import dev.aaa1115910.biliapi.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TopicDynamicViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    val items = mutableStateListOf<DynamicTopicFeedItem>()

    var topicId by mutableLongStateOf(0L)
        private set
    var topicName by mutableStateOf("")
        private set
    var hasMore by mutableStateOf(true)
        private set
    var loading by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var expandingFold by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var offset: String? = null
    private var sortBy = 0
    private val loadMutex = Mutex()

    fun setTopic(id: Long, name: String) {
        if (id <= 0L || (topicId == id && topicName == name)) return
        topicId = id
        topicName = name
        offset = null
        sortBy = 0
        hasMore = true
        errorMessage = null
        items.clear()
    }

    suspend fun refresh() {
        loadMutex.withLock {
            if (topicId <= 0L) return
            loading = true
            refreshing = true
            errorMessage = null
            offset = null
            hasMore = true
            try {
                val page = withContext(Dispatchers.IO) {
                    userRepository.getDynamicTopicFeed(topicId = topicId, sortBy = sortBy)
                }
                items.clear()
                items.addAll(page.items)
                offset = page.offset.takeIf { it.isNotBlank() }
                hasMore = page.hasMore
            } catch (error: Throwable) {
                errorMessage = error.localizedMessage ?: "Unable to load topic dynamics"
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    suspend fun loadMore() {
        loadMutex.withLock {
            if (topicId <= 0L || loading || !hasMore) return
            loading = true
            errorMessage = null
            try {
                val page = withContext(Dispatchers.IO) {
                    userRepository.getDynamicTopicFeed(
                        topicId = topicId,
                        sortBy = sortBy,
                        offset = offset
                    )
                }
                items.addAll(page.items)
                offset = page.offset.takeIf { it.isNotBlank() }
                hasMore = page.hasMore
            } catch (error: Throwable) {
                errorMessage = error.localizedMessage ?: "Unable to load more topic dynamics"
            } finally {
                loading = false
            }
        }
    }

    suspend fun expandFold(index: Int) {
        loadMutex.withLock {
            val fold = items.getOrNull(index) as? DynamicTopicFeedItem.FoldCard ?: return
            expandingFold = true
            errorMessage = null
            try {
                val page = withContext(Dispatchers.IO) {
                    userRepository.expandDynamicTopicFold(topicId = topicId, sortBy = sortBy)
                }
                if (page.items.isNotEmpty() && items.getOrNull(index) == fold) {
                    items.removeAt(index)
                    items.addAll(index, page.items)
                }
            } catch (error: Throwable) {
                errorMessage = error.localizedMessage ?: "Unable to expand folded dynamics"
            } finally {
                expandingFold = false
            }
        }
    }
}
