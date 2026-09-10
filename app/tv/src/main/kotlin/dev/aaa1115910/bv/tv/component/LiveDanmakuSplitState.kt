package dev.aaa1115910.bv.tv.component

import dev.aaa1115910.bv.viewmodel.LiveDanmakuMessage

/** All updates run on the UI thread, with scrolling performed only after a batch is committed. */
internal class LiveDanmakuSplitState(
    val visibleMessages: MutableList<LiveDanmakuMessage>,
) {
    private val incomingMessages = LiveDanmakuPriorityBuffer()
    private var initialized = false
    private var lastQueuedMessageId = -1L

    /** Returns whether existing history changed and needs an immediate scroll to the end. */
    fun updateMessages(messages: List<LiveDanmakuMessage>, minimumUserLevel: Int): Boolean {
        // Read the batch and its cursor together. A LaunchedEffect key captured during composition
        // may already be stale when the effect starts; using it as the cursor requeues messages.
        val messageSnapshot = messages.toList()
        val latestMessageId = messageSnapshot.lastOrNull()?.id
        incomingMessages.removeBelowUserLevel(minimumUserLevel)
        val historyChanged = visibleMessages.removeAll {
            !it.passesLiveDanmakuSplitFilter(minimumUserLevel)
        }

        if (!initialized) {
            visibleMessages.addAll(
                messageSnapshot
                    .distinctBy { it.id }
                    .filter { it.passesLiveDanmakuSplitFilter(minimumUserLevel) }
                    .takeLast(LIVE_DANMAKU_SPLIT_INITIAL_MESSAGES)
            )
            lastQueuedMessageId = latestMessageId ?: -1L
            initialized = true
            return visibleMessages.isNotEmpty()
        }

        if (latestMessageId != null && latestMessageId < lastQueuedMessageId) {
            incomingMessages.clear()
            visibleMessages.clear()
            lastQueuedMessageId = -1L
        }

        messageSnapshot
            .asSequence()
            .filter { it.id > lastQueuedMessageId }
            .filter { it.passesLiveDanmakuSplitFilter(minimumUserLevel) }
            .forEach { incomingMessages.offer(it) }
        if (latestMessageId != null) {
            lastQueuedMessageId = latestMessageId
        }
        return historyChanged && visibleMessages.isNotEmpty()
    }

    suspend fun appendNextMessage(): LiveDanmakuMessage? {
        while (true) {
            val message = incomingMessages.take() ?: return null
            // Keep LazyColumn keys unique even if a duplicate delivery reaches the consumer.
            if (visibleMessages.any { it.id == message.id }) continue
            visibleMessages.add(message)
            trimLiveDanmakuSplitHistory(visibleMessages)
            return message
        }
    }

    fun close() {
        incomingMessages.close()
    }
}
