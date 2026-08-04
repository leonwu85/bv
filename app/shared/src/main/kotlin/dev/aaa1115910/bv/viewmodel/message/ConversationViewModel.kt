package dev.aaa1115910.bv.viewmodel.message

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.message.DirectMessage
import dev.aaa1115910.biliapi.entity.message.DirectMessageContent
import dev.aaa1115910.biliapi.entity.message.DirectMessageEmote
import dev.aaa1115910.biliapi.entity.user.DynamicEmotePackageDraft
import dev.aaa1115910.biliapi.repositories.MessageRepository
import dev.aaa1115910.bv.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ConversationViewModel(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger("ConversationViewModel")

    val messages = mutableStateListOf<DirectMessage>()
    val emotes = mutableStateMapOf<String, DirectMessageEmote>()
    val emotePackages = mutableStateListOf<DynamicEmotePackageDraft>()

    var talkerId by mutableStateOf(0L)
        private set
    var title by mutableStateOf("")
        private set
    var face by mutableStateOf("")
        private set
    var loading by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var sending by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var hasMore by mutableStateOf(true)
        private set
    var loadingEmotes by mutableStateOf(false)
        private set

    private var initialized = false
    private var oldestSeqno: Long? = null

    val selfUid: Long get() = userRepository.uid
    val isLogin: Boolean get() = userRepository.isLogin

    fun initialize(
        talkerId: Long,
        title: String,
        face: String
    ) {
        if (initialized && this.talkerId == talkerId) return
        initialized = true
        this.talkerId = talkerId
        this.title = title.ifBlank { talkerId.toString() }
        this.face = face
        refresh()
    }

    fun refresh() {
        if (loading || talkerId <= 0L) return
        if (!userRepository.isLogin) {
            errorMessage = "请先登录"
            messages.clear()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loading = true
                errorMessage = null
            }
            runCatching {
                messageRepository.getConversation(talkerId = talkerId)
            }.onSuccess { page ->
                withContext(Dispatchers.Main) {
                    messages.clear()
                    messages.addAll(page.messages)
                    emotes.clear()
                    page.emotes.forEach { emotes[it.text] = it }
                    hasMore = page.hasMore
                    oldestSeqno = page.minSeqno ?: page.messages.firstOrNull()?.msgSeqno
                }
                val readSeqno = page.maxSeqno ?: page.messages.maxOfOrNull { it.msgSeqno }
                readSeqno?.let { seqno ->
                    runCatching {
                        messageRepository.markRead(talkerId, seqno)
                    }.onFailure { error ->
                        // 会话已成功加载；已读回执是可重试的附加操作，不应终止应用。
                        logger.warn(error) {
                            "Mark conversation as read failed: talkerId=$talkerId, seqno=$seqno"
                        }
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "加载失败"
                }
            }
            withContext(Dispatchers.Main) {
                loading = false
            }
        }
    }

    fun loadMore() {
        if (loading || loadingMore || !hasMore || talkerId <= 0L) return
        val cursor = oldestSeqno ?: return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingMore = true
            }
            runCatching {
                messageRepository.getConversation(
                    talkerId = talkerId,
                    endSeqno = cursor
                )
            }.onSuccess { page ->
                withContext(Dispatchers.Main) {
                    val existed = messages.map { it.msgKey to it.msgSeqno }.toSet()
                    val merged = (page.messages.filterNot { it.msgKey to it.msgSeqno in existed } + messages)
                        .sortedWith(compareBy<DirectMessage> { it.timestampSeconds }.thenBy { it.msgSeqno })
                    messages.clear()
                    messages.addAll(merged)
                    page.emotes.forEach { emotes[it.text] = it }
                    hasMore = page.hasMore
                    oldestSeqno = page.minSeqno ?: page.messages.firstOrNull()?.msgSeqno ?: oldestSeqno
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

    fun sendText(text: String, onSent: () -> Unit = {}) {
        val message = text.trim()
        if (message.isEmpty() || sending || talkerId <= 0L) return
        if (!userRepository.isLogin) {
            errorMessage = "请先登录"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                sending = true
                errorMessage = null
            }
            runCatching {
                messageRepository.sendText(
                    senderUid = userRepository.uid,
                    receiverId = talkerId,
                    text = message
                )
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    onSent()
                }
                refresh()
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "发送失败"
                }
            }
            withContext(Dispatchers.Main) {
                sending = false
            }
        }
    }

    fun sendImage(fileName: String, bytes: ByteArray) {
        if (bytes.isEmpty() || sending || talkerId <= 0L) return
        if (!userRepository.isLogin) {
            errorMessage = "请先登录"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                sending = true
                errorMessage = null
            }
            runCatching {
                val image = messageRepository.uploadImage(fileName = fileName, bytes = bytes)
                messageRepository.sendImage(
                    senderUid = userRepository.uid,
                    receiverId = talkerId,
                    image = image
                )
            }.onSuccess {
                refresh()
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "发送失败"
                }
            }
            withContext(Dispatchers.Main) {
                sending = false
            }
        }
    }

    fun withdraw(message: DirectMessage) {
        if (sending || talkerId <= 0L || message.msgKey <= 0L || message.status == 1) return
        if (message.senderUid != userRepository.uid) return
        if (!userRepository.isLogin) {
            errorMessage = "请先登录"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                sending = true
                errorMessage = null
            }
            runCatching {
                messageRepository.withdraw(
                    senderUid = userRepository.uid,
                    receiverId = talkerId,
                    msgKey = message.msgKey
                )
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    val index = messages.indexOfFirst {
                        it.msgKey == message.msgKey && it.msgSeqno == message.msgSeqno
                    }
                    if (index >= 0) {
                        messages[index] = messages[index].copy(
                            status = 1,
                            content = DirectMessageContent.Notice("已撤回")
                        )
                    }
                    errorMessage = null
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "撤回失败"
                }
            }
            withContext(Dispatchers.Main) {
                sending = false
            }
        }
    }

    fun report(
        message: DirectMessage,
        reasonType: Int,
        reasonDesc: String,
        onReported: () -> Unit = {}
    ) {
        if (sending || message.senderUid <= 0L || message.msgKey <= 0L) return
        if (message.senderUid == userRepository.uid) return
        if (!userRepository.isLogin) {
            errorMessage = "请先登录"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                sending = true
                errorMessage = null
            }
            runCatching {
                messageRepository.report(
                    message = message,
                    reasonType = reasonType,
                    reasonDesc = reasonDesc
                )
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    onReported()
                    errorMessage = null
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "举报失败"
                }
            }
            withContext(Dispatchers.Main) {
                sending = false
            }
        }
    }

    fun loadEmotePackages() {
        if (loadingEmotes || emotePackages.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingEmotes = true
            }
            runCatching {
                messageRepository.getEmotePackages()
            }.onSuccess { packages ->
                withContext(Dispatchers.Main) {
                    emotePackages.clear()
                    emotePackages.addAll(packages)
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    errorMessage = error.message ?: "表情加载失败"
                }
            }
            withContext(Dispatchers.Main) {
                loadingEmotes = false
            }
        }
    }
}
