package dev.aaa1115910.bv.tv.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.viewmodel.LiveDanmakuMessage
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal const val LIVE_DANMAKU_SPLIT_BUFFER_CAPACITY = 16
internal const val LIVE_DANMAKU_SPLIT_MAX_HISTORY_MESSAGES = 500
internal const val LIVE_DANMAKU_SPLIT_INITIAL_MESSAGES = 8
internal const val LIVE_DANMAKU_SPLIT_SCALE_REDUCTION = 0.2f
internal val LiveDanmakuSplitVideoTopBarHeight = 40.dp
internal val LiveDanmakuSplitVideoBottomBarHeight = 40.dp

internal fun LiveDanmakuMessage.passesLiveDanmakuSplitFilter(
    minimumUserLevel: Int,
): Boolean = userLevel >= minimumUserLevel

internal fun trimLiveDanmakuSplitHistory(
    messages: MutableList<LiveDanmakuMessage>,
    maxSize: Int = LIVE_DANMAKU_SPLIT_MAX_HISTORY_MESSAGES,
) {
    require(maxSize > 0)
    while (messages.size > maxSize) messages.removeAt(0)
}

/**
 * A bounded chronological queue that rejects or evicts the lowest-level message first. When
 * levels match, the older message is discarded so recent context remains readable.
 */
internal class LiveDanmakuPriorityBuffer(
    private val capacity: Int = LIVE_DANMAKU_SPLIT_BUFFER_CAPACITY,
) {
    private val pending = mutableListOf<LiveDanmakuMessage>()
    private val available = Channel<Unit>(capacity = Channel.CONFLATED)

    init {
        require(capacity > 0)
    }

    fun offer(message: LiveDanmakuMessage): Boolean {
        if (pending.size >= capacity) {
            val dropCandidate = (pending + message)
                .withIndex()
                .minWithOrNull(
                    compareBy<IndexedValue<LiveDanmakuMessage>>(
                        { it.value.userLevel },
                        { it.value.timestampMs },
                        { it.value.id },
                    )
                )
            if (dropCandidate?.index == pending.size) return false
            dropCandidate?.let { pending.removeAt(it.index) }
        }

        pending.add(message)
        available.trySend(Unit)
        return true
    }

    suspend fun take(): LiveDanmakuMessage {
        while (true) {
            if (pending.isNotEmpty()) {
                val next = pending.removeAt(0)
                if (pending.isEmpty()) available.tryReceive()
                return next
            }
            available.receive()
        }
    }

    fun clear() {
        pending.clear()
        drainAvailabilitySignal()
    }

    fun removeBelowUserLevel(minimumUserLevel: Int) {
        pending.removeAll { !it.passesLiveDanmakuSplitFilter(minimumUserLevel) }
        if (pending.isEmpty()) drainAvailabilitySignal()
    }

    fun close() {
        available.close()
    }

    internal fun snapshot(): List<LiveDanmakuMessage> = pending.toList()

    private fun drainAvailabilitySignal() {
        while (available.tryReceive().isSuccess) {
            // Drain stale availability signals together with the pending messages.
        }
    }
}

/**
 * Keeps the list readable under bursts while still letting short messages feel live.
 */
internal fun liveDanmakuSplitInsertIntervalMs(content: String): Long {
    val codePointCount = content.codePointCount(0, content.length)
    return (300L + codePointCount.coerceAtMost(44) * 30L)
        .coerceIn(330L, 670L)
}

/**
 * Keeps each message's original font size, but renders the split list at the current global
 * danmaku scale minus twenty percentage points.
 */
internal fun resolveLiveDanmakuSplitTextSizeSp(
    sourceFontSize: Int,
    danmakuScale: Float,
    density: Float,
    fontScale: Float,
): Float {
    val safeDensity = density.coerceAtLeast(0.1f)
    val safeFontScale = fontScale.coerceAtLeast(0.1f)
    val densityFactor = (safeDensity - 0.6f).coerceAtLeast(0.4f)
    val splitScale = (danmakuScale - LIVE_DANMAKU_SPLIT_SCALE_REDUCTION)
        .coerceAtLeast(0.1f)
    val renderedSizePx = sourceFontSize.coerceAtLeast(1) * densityFactor * splitScale
    return renderedSizePx / (safeDensity * safeFontScale)
}

internal fun selectLiveDanmakuEmoji(
    content: String,
    emojiMap: Map<String, String>,
    showEmoji: Boolean,
): List<Map.Entry<String, String>> {
    if (!showEmoji) return emptyList()
    return emojiMap.entries
        .filter { (marker, url) ->
            marker.isNotBlank() && url.isNotBlank() && content.contains(marker)
        }
        .distinctBy { it.key }
        .sortedByDescending { it.key.length }
}

@Composable
fun LiveDanmakuSplitPanel(
    messages: List<LiveDanmakuMessage>,
    roomId: Long,
    danmakuScale: Float,
    showEmoji: Boolean,
    minimumUserLevel: Int,
    modifier: Modifier = Modifier,
) {
    val visibleMessages = remember(roomId) { mutableStateListOf<LiveDanmakuMessage>() }
    val incomingMessages = remember(roomId) { LiveDanmakuPriorityBuffer() }
    val listState = rememberLazyListState()
    var initialized by remember(roomId) { mutableStateOf(false) }
    var lastQueuedMessageId by remember(roomId) { mutableLongStateOf(-1L) }
    val latestMessageId = messages.lastOrNull()?.id

    LaunchedEffect(roomId, latestMessageId, minimumUserLevel) {
        incomingMessages.removeBelowUserLevel(minimumUserLevel)
        val visibleMessagesChanged = visibleMessages.removeAll {
            !it.passesLiveDanmakuSplitFilter(minimumUserLevel)
        }
        if (visibleMessagesChanged && visibleMessages.isNotEmpty()) {
            listState.scrollToItem(visibleMessages.lastIndex)
        }

        if (!initialized) {
            visibleMessages.addAll(
                messages
                    .filter { it.passesLiveDanmakuSplitFilter(minimumUserLevel) }
                    .takeLast(LIVE_DANMAKU_SPLIT_INITIAL_MESSAGES)
            )
            lastQueuedMessageId = latestMessageId ?: -1L
            initialized = true
            if (visibleMessages.isNotEmpty()) {
                listState.scrollToItem(visibleMessages.lastIndex)
            }
            return@LaunchedEffect
        }

        if (latestMessageId != null && latestMessageId < lastQueuedMessageId) {
            incomingMessages.clear()
            visibleMessages.clear()
            lastQueuedMessageId = -1L
        }

        messages
            .asSequence()
            .filter { it.id > lastQueuedMessageId }
            .filter { it.passesLiveDanmakuSplitFilter(minimumUserLevel) }
            .forEach { message -> incomingMessages.offer(message) }
        if (latestMessageId != null) {
            lastQueuedMessageId = latestMessageId
        }
    }

    LaunchedEffect(roomId) {
        while (isActive) {
            val message = incomingMessages.take()
            visibleMessages.add(message)
            trimLiveDanmakuSplitHistory(visibleMessages)
            listState.animateScrollToItem(visibleMessages.lastIndex)
            delay(liveDanmakuSplitInsertIntervalMs(message.content))
        }
    }

    DisposableEffect(roomId) {
        onDispose { incomingMessages.close() }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF111218))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "弹幕",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFFB7299).copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                text = "直播",
                color = Color(0xFFFB7299),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            userScrollEnabled = false,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
        ) {
            items(visibleMessages, key = { it.id }) { message ->
                LiveDanmakuSplitMessage(
                    message = message,
                    danmakuScale = danmakuScale,
                    showEmoji = showEmoji,
                )
            }
        }
    }
}

@Composable
private fun LiveDanmakuSplitMessage(
    message: LiveDanmakuMessage,
    danmakuScale: Float,
    showEmoji: Boolean,
) {
    val density = LocalDensity.current
    val textSize = resolveLiveDanmakuSplitTextSizeSp(
        sourceFontSize = message.fontSize,
        danmakuScale = danmakuScale,
        density = density.density,
        fontScale = density.fontScale,
    ).sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(liveDanmakuLevelColor(message.userLevel))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                text = "LV${message.userLevel}",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = message.username.ifBlank { "匿名用户" },
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LiveDanmakuContent(
            content = message.content,
            color = Color(message.color),
            fontSize = textSize,
            emojiMap = message.emojiMap,
            showEmoji = showEmoji,
        )
    }
}

@Composable
private fun LiveDanmakuContent(
    content: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    emojiMap: Map<String, String>,
    showEmoji: Boolean,
) {
    val usableEmoji = remember(content, emojiMap, showEmoji) {
        selectLiveDanmakuEmoji(content, emojiMap, showEmoji)
    }
    val style = TextStyle(
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * 1.32f,
        fontWeight = FontWeight.Medium,
        shadow = Shadow(color = Color.Black.copy(alpha = 0.88f), offset = Offset.Zero, blurRadius = 2.5f),
    )

    if (usableEmoji.isEmpty()) {
        BasicText(
            text = content,
            style = style,
        )
        return
    }

    val inlineContent = remember(usableEmoji) {
        usableEmoji.associate { (marker, url) ->
            marker to InlineTextContent(
                placeholder = Placeholder(
                    width = 1.15.em,
                    height = 1.15.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = url,
                    contentDescription = marker,
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
    val annotatedContent = remember(content, usableEmoji) {
        buildAnnotatedString {
            var index = 0
            while (index < content.length) {
                val match = usableEmoji.firstOrNull { (marker, _) ->
                    content.regionMatches(
                        thisOffset = index,
                        other = marker,
                        otherOffset = 0,
                        length = marker.length,
                    )
                }
                if (match == null) {
                    append(content[index])
                    index += 1
                } else {
                    appendInlineContent(match.key, match.key)
                    index += match.key.length
                }
            }
        }
    }

    BasicText(
        text = annotatedContent,
        inlineContent = inlineContent,
        style = style,
    )
}

@Composable
fun LiveDanmakuSplitVideoInfoOverlay(
    show: Boolean,
    upName: String,
    upAvatar: String,
    viewerCount: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    var currentTime by remember { mutableStateOf(formatCurrentTime()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = formatCurrentTime()
            val now = Calendar.getInstance()
            delay(60_000L - now.get(Calendar.SECOND) * 1_000L - now.get(Calendar.MILLISECOND) + 50L)
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(LiveDanmakuSplitVideoTopBarHeight)
                    .background(Color(0xFF08090D)),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(LiveDanmakuSplitVideoTopBarHeight)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    model = upAvatar,
                    contentDescription = upName,
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = upName.ifBlank { "直播中" },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = listOf(viewerCount, currentTime).filter { it.isNotBlank() }.joinToString("  ·  "),
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(LiveDanmakuSplitVideoBottomBarHeight)
                    .background(Color(0xFF08090D)),
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 24.dp),
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun liveDanmakuLevelColor(level: Int): Color = when {
    level >= 50 -> Color(0xFFE85778)
    level >= 30 -> Color(0xFF8267D8)
    level >= 15 -> Color(0xFF4D77BE)
    else -> Color(0xFF4B5568)
}

private fun formatCurrentTime(): String {
    val now = Calendar.getInstance()
    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        now.get(Calendar.HOUR_OF_DAY),
        now.get(Calendar.MINUTE),
    )
}
