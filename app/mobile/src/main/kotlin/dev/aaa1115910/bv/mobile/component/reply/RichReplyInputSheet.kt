package dev.aaa1115910.bv.mobile.component.reply

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.user.DynamicEmoteDraft
import dev.aaa1115910.biliapi.entity.user.DynamicMentionDraft
import dev.aaa1115910.bv.mobile.component.emote.EmoteInputToken
import dev.aaa1115910.bv.mobile.component.emote.EmoteTextSelection
import dev.aaa1115910.bv.mobile.component.emote.EmotePanel
import dev.aaa1115910.bv.mobile.component.emote.EmoteTextEditor
import dev.aaa1115910.bv.mobile.component.emote.emoteDisplayName
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class RichReplyLocalImage(
    val uri: Uri,
    val fileName: String
)

data class RichReplySendDraft(
    val message: String,
    val images: List<RichReplyLocalImage>,
    val atNameToMid: Map<String, Long>,
    val syncToDynamic: Boolean
)

data class RichReplyMoreAction(
    val title: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: ((String) -> Unit) -> Unit
)

private data class RichReplyMentionToken(
    val marker: String,
    val name: String,
    val uid: Long,
    val preferredStart: Int
)

private enum class RichReplyInputPanel {
    Emoji,
    Mention,
    More
}

fun Context.richReplyUriFileName(uri: Uri): String {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: "reply_${System.currentTimeMillis()}.jpg"
}

fun Context.richReplyUriBytes(uri: Uri): ByteArray? =
    contentResolver.openInputStream(uri)?.use { it.readBytes() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichReplyInputSheet(
    commentViewModel: CommentViewModel,
    title: String,
    placeholder: String,
    sending: Boolean,
    canUploadImages: Boolean,
    showSyncToDynamic: Boolean = true,
    moreActions: List<RichReplyMoreAction> = emptyList(),
    onDismiss: () -> Unit,
    onSend: (RichReplySendDraft) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    var text by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(EmoteTextSelection.Zero) }
    var activePanel by remember { mutableStateOf<RichReplyInputPanel?>(null) }
    var mentionKeyword by remember { mutableStateOf("") }
    var loadingMentions by remember { mutableStateOf(false) }
    var syncToDynamic by remember { mutableStateOf(false) }
    val mentionSuggestions = remember { mutableStateListOf<DynamicMentionDraft>() }
    val selectedImages = remember { mutableStateListOf<RichReplyLocalImage>() }
    val emoteTokens = remember { mutableStateListOf<EmoteInputToken>() }
    val mentionTokens = remember { mutableStateListOf<RichReplyMentionToken>() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val remaining = 9 - selectedImages.size
        if (remaining <= 0) {
            "最多选择 9 张图片".toast(context)
            return@rememberLauncherForActivityResult
        }
        selectedImages.addAll(
            uris.take(remaining).map { uri ->
                RichReplyLocalImage(
                    uri = uri,
                    fileName = context.richReplyUriFileName(uri)
                )
            }
        )
        if (uris.size > remaining) "已达到 9 张图片上限".toast(context)
    }

    fun insertText(
        marker: String,
        emote: DynamicEmoteDraft? = null,
        mention: DynamicMentionDraft? = null
    ) {
        val start = selection.start.coerceIn(0, text.length)
        val end = selection.end.coerceIn(0, text.length)
        val replaceStart = minOf(start, end)
        val replaceEnd = maxOf(start, end)
        text = text.replaceRange(replaceStart, replaceEnd, marker)
        selection = EmoteTextSelection.collapsed(replaceStart + marker.length)
        if (emote != null && emote.url.isNotBlank()) {
            emoteTokens.add(
                EmoteInputToken(
                    marker = marker,
                    preferredStart = replaceStart,
                    emoteUrl = emote.url,
                    emoteName = emote.emoteDisplayName
                )
            )
        }
        if (mention != null) {
            mentionTokens.add(
                RichReplyMentionToken(
                    marker = marker,
                    name = mention.name,
                    uid = mention.uid.toLongOrNull() ?: 0L,
                    preferredStart = replaceStart
                )
            )
        }
    }

    LaunchedEffect(activePanel) {
        if (activePanel == RichReplyInputPanel.Emoji) {
            commentViewModel.loadEmotePackages()
        }
    }

    LaunchedEffect(activePanel, mentionKeyword) {
        if (activePanel != RichReplyInputPanel.Mention) return@LaunchedEffect
        if (mentionKeyword.isNotBlank()) delay(250)
        loadingMentions = true
        val result = withContext(Dispatchers.IO) {
            commentViewModel.searchMention(mentionKeyword)
        }
        mentionSuggestions.clear()
        mentionSuggestions.addAll(result.getOrDefault(emptyList()))
        loadingMentions = false
    }

    val canSend = text.isNotBlank() || selectedImages.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = {
            if (!sending) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    enabled = !sending,
                    onClick = onDismiss
                ) {
                    Text(text = "取消")
                }
                Button(
                    enabled = !sending && canSend,
                    onClick = {
                        onSend(
                            RichReplySendDraft(
                                message = text,
                                images = selectedImages.toList(),
                                atNameToMid = mentionTokens
                                    .filter { token ->
                                        token.uid > 0L && text.contains(token.marker)
                                    }
                                    .associate { it.name to it.uid },
                                syncToDynamic = syncToDynamic
                            )
                        )
                    }
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null
                        )
                    }
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = "发送"
                    )
                }
            }

            EmoteTextEditor(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                selection = selection,
                emoteTokens = emoteTokens,
                placeholder = placeholder,
                label = "回复内容",
                enabled = !sending,
                minLines = 4,
                maxLines = 8,
                onValueChange = { value, newSelection ->
                    text = value
                    selection = newSelection
                }
            )

            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedImages) { image ->
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            AsyncImage(
                                modifier = Modifier.fillMaxSize(),
                                model = image.uri,
                                contentDescription = image.fileName,
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    ),
                                onClick = { selectedImages.remove(image) }
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除图片",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RichReplyToolButton(
                    selected = activePanel == RichReplyInputPanel.Emoji,
                    icon = if (activePanel == RichReplyInputPanel.Emoji) {
                        Icons.Default.Keyboard
                    } else {
                        Icons.Default.EmojiEmotions
                    },
                    contentDescription = "表情",
                    onClick = {
                        activePanel = if (activePanel == RichReplyInputPanel.Emoji) {
                            null
                        } else {
                            RichReplyInputPanel.Emoji
                        }
                    }
                )
                RichReplyToolButton(
                    selected = selectedImages.isNotEmpty(),
                    icon = if (canUploadImages) Icons.Default.Image else Icons.Default.ImageNotSupported,
                    contentDescription = "图片",
                    onClick = {
                        if (canUploadImages) {
                            imagePicker.launch("image/*")
                        } else {
                            "当前评论区不支持发送图片".toast(context)
                        }
                    }
                )
                RichReplyToolButton(
                    selected = activePanel == RichReplyInputPanel.Mention,
                    icon = Icons.Default.AlternateEmail,
                    contentDescription = "@",
                    onClick = {
                        activePanel = if (activePanel == RichReplyInputPanel.Mention) {
                            null
                        } else {
                            RichReplyInputPanel.Mention
                        }
                    }
                )
                RichReplyToolButton(
                    selected = activePanel == RichReplyInputPanel.More,
                    icon = if (activePanel == RichReplyInputPanel.More) {
                        Icons.Default.Keyboard
                    } else {
                        Icons.Default.AddCircleOutline
                    },
                    contentDescription = "更多",
                    onClick = {
                        activePanel = if (activePanel == RichReplyInputPanel.More) {
                            null
                        } else {
                            RichReplyInputPanel.More
                        }
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                if (showSyncToDynamic) {
                    FilledTonalButton(
                        onClick = { syncToDynamic = !syncToDynamic },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            modifier = Modifier.size(22.dp),
                            checked = syncToDynamic,
                            onCheckedChange = { syncToDynamic = it }
                        )
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = "转动态"
                        )
                    }
                }
            }

            when (activePanel) {
                RichReplyInputPanel.Emoji -> EmotePanel(
                    packages = commentViewModel.emotePackages,
                    loading = commentViewModel.loadingEmotes,
                    onSelect = { emoji ->
                        insertText(
                            marker = emoji.text,
                            emote = emoji
                        )
                    }
                )

                RichReplyInputPanel.Mention -> RichReplyMentionPanel(
                    keyword = mentionKeyword,
                    onKeywordChange = { mentionKeyword = it },
                    mentions = mentionSuggestions,
                    loading = loadingMentions,
                    onSelect = { mention ->
                        insertText(
                            marker = "@${mention.name} ",
                            mention = mention
                        )
                    }
                )

                RichReplyInputPanel.More -> RichReplyMorePanel(
                    actions = moreActions,
                    onInsertText = { marker -> insertText(marker) }
                )

                null -> Unit
            }
        }
    }
}

@Composable
private fun RichReplyToolButton(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        enabled = enabled,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun RichReplyMentionPanel(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    mentions: List<DynamicMentionDraft>,
    loading: Boolean,
    onSelect: (DynamicMentionDraft) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = keyword,
                onValueChange = onKeywordChange,
                label = { Text(text = "搜索 UP 主") },
                placeholder = { Text(text = "输入昵称后添加 @") },
                singleLine = true
            )
            if (loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
            mentions.take(8).forEach { item ->
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelect(item) },
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        model = item.face,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "UID ${item.uid}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RichReplyMorePanel(
    actions: List<RichReplyMoreAction>,
    onInsertText: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (actions.isEmpty()) {
                RichReplyMoreItem(
                    title = "插入内容",
                    icon = Icons.AutoMirrored.Filled.Comment,
                    enabled = false,
                    onClick = {}
                )
            } else {
                actions.forEach { action ->
                    RichReplyMoreItem(
                        title = action.title,
                        icon = action.icon,
                        enabled = action.enabled,
                        onClick = { action.onClick(onInsertText) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RichReplyMoreItem(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
