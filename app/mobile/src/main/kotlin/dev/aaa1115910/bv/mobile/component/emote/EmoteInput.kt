package dev.aaa1115910.bv.mobile.component.emote

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dev.aaa1115910.biliapi.entity.user.DynamicEmoteDraft
import dev.aaa1115910.biliapi.entity.user.DynamicEmotePackageDraft
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class EmoteTextSelection(
    val start: Int,
    val end: Int
) {
    companion object {
        val Zero = EmoteTextSelection(0, 0)

        fun collapsed(offset: Int) = EmoteTextSelection(offset, offset)
    }
}

data class EmoteInputToken(
    val marker: String,
    val preferredStart: Int = -1,
    val emoteUrl: String = "",
    val emoteName: String = ""
)

@Composable
fun EmoteTextEditor(
    modifier: Modifier = Modifier,
    value: String,
    selection: EmoteTextSelection,
    emoteTokens: List<EmoteInputToken>,
    placeholder: String,
    label: String?,
    enabled: Boolean = true,
    minLines: Int = 3,
    maxLines: Int = 6,
    shape: Shape = RoundedCornerShape(4.dp),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    onEditorTouched: () -> Unit = {},
    onValueChange: (String, EmoteTextSelection) -> Unit
) {
    val context = LocalContext.current
    val currentOnEditorTouched by rememberUpdatedState(onEditorTouched)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val applyingExternalChange = remember { mutableStateOf(false) }
    val emoteDrawables = remember { mutableStateMapOf<String, Drawable>() }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f).toArgb()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val emoteUrls = emoteTokens
        .mapNotNull { it.emoteUrl.takeIf(String::isNotBlank) }
        .distinct()

    LaunchedEffect(emoteUrls) {
        emoteUrls.forEach { url ->
            if (emoteDrawables[url] != null) return@forEach
            val result = withContext(Dispatchers.IO) {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .size(96, 96)
                        .build()
                )
            }
            val drawable = (result as? SuccessResult)?.drawable ?: return@forEach
            emoteDrawables[url] = drawable
        }
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = border
    ) {
        Column(modifier = Modifier.padding(top = if (label.isNullOrBlank()) 0.dp else 8.dp)) {
            if (!label.isNullOrBlank()) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = label,
                    color = labelColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = (28 * minLines).dp, max = (36 * maxLines).dp)
                    .padding(contentPadding),
                factory = { viewContext ->
                    SelectionEditText(viewContext).apply {
                        setBackgroundColor(TRANSPARENT)
                        setTextColor(textColor)
                        setHintTextColor(hintColor)
                        hint = placeholder
                        gravity = Gravity.TOP or Gravity.START
                        this.minLines = minLines
                        this.maxLines = maxLines
                        setSingleLine(false)
                        setTextSize(16f)
                        inputType = InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        imeOptions = EditorInfo.IME_ACTION_NONE
                        includeFontPadding = true
                        setPadding(0, 0, 0, 0)
                        isEnabled = enabled
                        setOnTouchListener { _, _ ->
                            currentOnEditorTouched()
                            false
                        }

                        val editText = this
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) = Unit

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) = Unit

                            override fun afterTextChanged(s: Editable?) {
                                if (applyingExternalChange.value) return
                                currentOnValueChange(
                                    s?.toString().orEmpty(),
                                    EmoteTextSelection(
                                        start = editText.selectionStart.coerceAtLeast(0),
                                        end = editText.selectionEnd.coerceAtLeast(0)
                                    )
                                )
                            }
                        })
                        onSelectionChangedListener = { start, end ->
                            if (!applyingExternalChange.value) {
                                currentOnValueChange(
                                    editText.text?.toString().orEmpty(),
                                    EmoteTextSelection(
                                        start = start.coerceAtLeast(0),
                                        end = end.coerceAtLeast(0)
                                    )
                                )
                            }
                        }
                    }
                },
                update = { editText ->
                    editText.isEnabled = enabled
                    editText.setTextColor(textColor)
                    editText.setHintTextColor(hintColor)
                    editText.hint = placeholder

                    val spanKey = buildEmoteEditorSpanKey(value, emoteTokens, emoteDrawables.keys)
                    if (editText.text?.toString().orEmpty() != value || editText.tag != spanKey) {
                        applyingExternalChange.value = true
                        editText.setText(
                            buildEmoteEditorSpannable(
                                context = context,
                                text = value,
                                tokens = emoteTokens,
                                emoteDrawables = emoteDrawables
                            )
                        )
                        editText.tag = spanKey
                        applyingExternalChange.value = false
                    }

                    val textLength = editText.text?.length ?: 0
                    val start = selection.start.coerceIn(0, textLength)
                    val end = selection.end.coerceIn(0, textLength)
                    if (editText.selectionStart != start || editText.selectionEnd != end) {
                        applyingExternalChange.value = true
                        editText.setSelection(start, end)
                        applyingExternalChange.value = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmotePanel(
    packages: List<DynamicEmotePackageDraft>,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (DynamicEmoteDraft) -> Unit
) {
    val context = LocalContext.current
    val fallbackEmotes = remember {
        listOf(
            "[doge]", "[妙啊]", "[星星眼]", "[吃瓜]", "[滑稽]", "[笑哭]", "[喜极而泣]", "[脱单doge]",
            "[打call]", "[支持]", "[抱拳]", "[OK]", "[点赞]", "[鼓掌]", "[热词系列_知识增加]",
            "[热词系列_好家伙]", "[热词系列_破防了]", "[热词系列_泪目]", "[热词系列_三连]"
        ).map { DynamicEmoteDraft(text = it) }
    }
    val fallbackPackages = remember {
        listOf(DynamicEmotePackageDraft(type = 4, emotes = fallbackEmotes))
    }
    val panelPackages = packages.ifEmpty { fallbackPackages }
    var selectedPackageIndex by remember { mutableStateOf(0) }
    var previewEmote by remember { mutableStateOf<DynamicEmoteDraft?>(null) }
    val safeSelectedIndex = selectedPackageIndex.coerceIn(0, panelPackages.lastIndex)
    val selectedPackage = panelPackages[safeSelectedIndex]
    val isTextEmote = selectedPackage.type == 4
    val smallImageEmote = !isTextEmote && selectedPackage.emotes.firstOrNull()?.size == 1
    val cellSize = when {
        isTextEmote -> 96.dp
        smallImageEmote -> 40.dp
        else -> 60.dp
    }

    LaunchedEffect(panelPackages.size) {
        if (selectedPackageIndex !in panelPackages.indices) selectedPackageIndex = 0
    }
    LaunchedEffect(previewEmote) {
        if (previewEmote != null) {
            delay(1600)
            previewEmote = null
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "表情",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp),
                    columns = GridCells.Adaptive(cellSize),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(selectedPackage.emotes) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(width = cellSize, height = if (isTextEmote) 40.dp else cellSize)
                                .clip(RoundedCornerShape(6.dp))
                                .combinedClickable(
                                    onClick = { onSelect(emoji) },
                                    onLongClick = { previewEmote = emoji }
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTextEmote || emoji.url.isBlank()) {
                                Text(
                                    text = emoji.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                AsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    model = emoji.url,
                                    contentDescription = emoji.emoteDisplayName,
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = { "表情包管理暂未实装".toast(context) }
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.Settings,
                            contentDescription = "表情包管理",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(panelPackages.size) { index ->
                            val pack = panelPackages[index]
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedPackageIndex = index },
                                color = if (index == safeSelectedIndex) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (pack.url.isNotBlank()) {
                                        AsyncImage(
                                            modifier = Modifier.size(24.dp),
                                            model = pack.url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(
                                            modifier = Modifier.size(22.dp),
                                            imageVector = Icons.Default.EmojiEmotions,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            previewEmote?.let { emoji ->
                EmotePreview(
                    modifier = Modifier.align(Alignment.TopCenter),
                    emote = emoji
                )
            }
        }
    }
}

val DynamicEmoteDraft.emoteDisplayName: String
    get() = alias.ifBlank {
        text.removePrefix("[").removeSuffix("]").ifBlank { text }
    }

private data class EmoteInputTokenRange(
    val token: EmoteInputToken,
    val start: Int,
    val end: Int
)

@Composable
private fun EmotePreview(
    modifier: Modifier = Modifier,
    emote: DynamicEmoteDraft
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (emote.url.isNotBlank()) {
                AsyncImage(
                    modifier = Modifier.size(68.dp),
                    model = emote.url,
                    contentDescription = emote.emoteDisplayName,
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = emote.text,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = emote.emoteDisplayName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildEmoteEditorSpanKey(
    text: String,
    tokens: List<EmoteInputToken>,
    loadedEmoteUrls: Set<String>
): String = buildString {
    append(text)
    append('|')
    tokens.forEach {
        append(it.marker)
        append('@')
        append(it.preferredStart)
        append(':')
        append(it.emoteUrl)
        append(';')
    }
    append('|')
    loadedEmoteUrls.sorted().forEach {
        append(it)
        append(';')
    }
}

private fun buildEmoteEditorSpannable(
    context: Context,
    text: String,
    tokens: List<EmoteInputToken>,
    emoteDrawables: Map<String, Drawable>
): SpannableStringBuilder {
    val spannable = SpannableStringBuilder(text)
    if (text.isEmpty()) return spannable
    val imageSize = (context.resources.displayMetrics.density * 24).toInt()
    resolveEmoteInputTokenRanges(text, tokens)
        .filter { it.token.emoteUrl.isNotBlank() }
        .forEach { range ->
            val drawable = emoteDrawables[range.token.emoteUrl]?.freshDrawable() ?: return@forEach
            drawable.setBounds(0, 0, imageSize, imageSize)
            spannable.setSpan(
                ImageSpan(drawable, DynamicDrawableSpan.ALIGN_CENTER),
                range.start,
                range.end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    return spannable
}

private fun resolveEmoteInputTokenRanges(
    text: String,
    tokens: List<EmoteInputToken>
): List<EmoteInputTokenRange> {
    if (text.isEmpty() || tokens.isEmpty()) return emptyList()
    val occupied = BooleanArray(text.length)
    return tokens.mapNotNull { token ->
        if (token.marker.isEmpty()) return@mapNotNull null
        findEmoteTokenRange(text, token, occupied)?.also { range ->
            for (index in range.start until range.end) occupied[index] = true
        }
    }.sortedBy { it.start }
}

private fun findEmoteTokenRange(
    text: String,
    token: EmoteInputToken,
    occupied: BooleanArray
): EmoteInputTokenRange? {
    val preferredStart = token.preferredStart
    if (
        preferredStart >= 0 &&
        preferredStart + token.marker.length <= text.length &&
        text.regionMatches(preferredStart, token.marker, 0, token.marker.length) &&
        (preferredStart until preferredStart + token.marker.length).all { !occupied[it] }
    ) {
        return EmoteInputTokenRange(token, preferredStart, preferredStart + token.marker.length)
    }

    var searchStart = 0
    while (searchStart <= text.length - token.marker.length) {
        val start = text.indexOf(token.marker, searchStart)
        if (start < 0) return null
        val end = start + token.marker.length
        if ((start until end).all { !occupied[it] }) {
            return EmoteInputTokenRange(token, start, end)
        }
        searchStart = start + 1
    }
    return null
}

private fun Drawable.freshDrawable(): Drawable =
    constantState?.newDrawable()?.mutate() ?: mutate()

private class SelectionEditText(context: Context) : EditText(context) {
    var onSelectionChangedListener: ((Int, Int) -> Unit)? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedListener?.invoke(selStart, selEnd)
    }
}
