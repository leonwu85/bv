package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.seekbar.ViewPointSegment
import dev.aaa1115910.bv.player.seekbar.viewPointAt
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
import dev.aaa1115910.bv.util.formatHourMinSec

/** One remote focus stop: left/right browse chapters, confirm seeks, down returns to scrubbing. */
@Composable
fun ViewPointSegmentBar(
    segments: List<ViewPointSegment>,
    positionMs: Long,
    onSeekToPosition: (Long) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val chapters = remember(segments) { segments.filter { it.title != null } }
    if (chapters.isEmpty()) return
    val currentChapter = segments.viewPointAt(positionMs)
    val density = LocalDensity.current
    var selectedIndex by remember(segments) { mutableIntStateOf(0) }
    var focused by remember { mutableStateOf(false) }
    val selected = chapters[selectedIndex.coerceIn(chapters.indices)]
    val displayed = if (focused) selected else currentChapter
    val displayTitle = displayed?.let {
        "${chapters.indexOf(it) + 1}/${chapters.size} · ${it.startMs.formatHourMinSec()} · ${it.title}"
    } ?: "视频分段 · ${chapters.size} 段"
    val shape = RoundedCornerShape(4.dp)

    Column(
        modifier = modifier
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) {
                    selectedIndex = chapters.indexOf(currentChapter).takeIf { index -> index >= 0 }
                        ?: chapters.indexOfFirst { chapter -> chapter.startMs >= positionMs }
                            .takeIf { index -> index >= 0 } ?: chapters.lastIndex
                }
            }
            .onPreviewKeyEvent { event ->
                when (event.key) {
                    Key.DirectionLeft, Key.DirectionRight -> {
                        if (event.type == KeyEventType.KeyDown) {
                            val step = if (event.key == Key.DirectionLeft) -1 else 1
                            selectedIndex = (selectedIndex + step).coerceIn(chapters.indices)
                        }
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        // Read the latest index: a fast confirm can arrive before recomposition.
                        if (event.type == KeyEventType.KeyUp) {
                            onSeekToPosition(chapters[selectedIndex.coerceIn(chapters.indices)].startMs)
                        }
                        true
                    }
                    else -> false
                }
            }
            .clearAndSetSemantics {
                contentDescription = "视频分段"
                stateDescription = displayTitle
                onClick(label = "播放此分段") {
                    val target = chapters[selectedIndex.coerceIn(chapters.indices)]
                    onSeekToPosition((if (focused) target else currentChapter ?: target).startMs)
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction("上一分段") {
                        val index = if (focused) selectedIndex else chapters.indexOf(currentChapter).coerceAtLeast(0)
                        selectedIndex = (index - 1).coerceAtLeast(0)
                        if (!focused) onSeekToPosition(chapters[selectedIndex].startMs)
                        true
                    },
                    CustomAccessibilityAction("下一分段") {
                        val index = if (focused) selectedIndex else chapters.indexOf(currentChapter).coerceAtLeast(0)
                        selectedIndex = (index + 1).coerceAtMost(chapters.lastIndex)
                        if (!focused) onSeekToPosition(chapters[selectedIndex].startMs)
                        true
                    },
                )
            }
            .focusable(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, if (focused) PlayerColors.seekBarFocusBorder else Color.Transparent, shape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayTitle,
                modifier = Modifier
                    .weight(1f)
                    .height(with(density) { (36 * scale).sp.toDp() }),
                color = PlayerColors.textPrimary,
                fontSize = (12 * scale).sp,
                lineHeight = (18 * scale).sp,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (focused) "← → 选段 · 确认播放" else "↑ 选择分段",
                modifier = Modifier.padding(start = 12.dp),
                color = PlayerColors.textSecondary,
                fontSize = (10 * scale).sp,
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val timelineWidth = maxWidth
            val duration = segments.last().endMs.toDouble()
            // Less than three glyphs only produces isolated ellipsis dots on short chapters.
            // Keep true time proportions; the shared title above remains readable and focusable.
            val minimumLabelWidth = with(density) { (11 * scale).sp.toDp() * 3 } + 12.dp
            val focusedShortSegment = displayed?.takeIf {
                focused && timelineWidth * ((it.endMs - it.startMs) / duration).toFloat() < minimumLabelWidth
            }
            Row(modifier = Modifier.fillMaxWidth().drawWithContent {
                drawContent()
                focusedShortSegment?.let {
                    val centerFraction = ((it.startMs.toDouble() + it.endMs) / 2 / duration).toFloat()
                    drawCircle(
                        color = PlayerColors.textPrimary,
                        radius = 3.dp.toPx(),
                        center = Offset(size.width * centerFraction, size.height / 2),
                    )
                }
            }) {
                segments.forEach { segment ->
                    val isSelected = segment == displayed
                    val segmentWidth = timelineWidth * ((segment.endMs - segment.startMs) / duration).toFloat()
                    val showLabel = segmentWidth >= minimumLabelWidth
                    Box(
                        modifier = Modifier
                            .weight((segment.endMs - segment.startMs).toFloat())
                            .padding(horizontal = 2.dp)
                            .heightIn(min = (24 * scale).dp)
                            .background(
                                if (isSelected && showLabel) PlayerColors.buttonFocused else Color.Transparent,
                                shape,
                            )
                            .border(
                                1.dp,
                                if (isSelected && focused && showLabel) PlayerColors.textPrimary else Color.Transparent,
                                shape,
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showLabel) Text(
                            text = segment.title.orEmpty(),
                            color = if (isSelected) PlayerColors.textPrimary else PlayerColors.textSecondary,
                            fontSize = (11 * scale).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
