package dev.aaa1115910.bv.tv.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.player.entity.PlayerBottomControlPanelButtonIds
import dev.aaa1115910.bv.player.entity.PlayerBottomControlPanelConfig
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.util.Prefs

private enum class ScaleTarget {
    Title,
    Info,
    ActionRow,
    SeekBar,
    FunctionRow
}

private enum class MovableRow {
    Action,
    Function
}

private enum class EditModeType {
    Resize,
    Move
}

private data class EditTarget(
    val id: String,
    val title: String,
    val scaleTarget: ScaleTarget,
    val movableRow: MovableRow? = null
)

private data class ActiveEditMode(
    val target: EditTarget,
    val type: EditModeType
)

@Composable
fun PlayerBottomControlPanelCustomizeScreen(
    modifier: Modifier = Modifier
) {
    val config by Prefs.playerBottomControlPanelConfigFlow.collectAsState(
        initial = Prefs.playerBottomControlPanelConfig
    )
    var selectedTarget by remember { mutableStateOf<EditTarget?>(null) }
    var menuTarget by remember { mutableStateOf<EditTarget?>(null) }
    var activeMode by remember { mutableStateOf<ActiveEditMode?>(null) }
    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }

    fun focusRequester(id: String): FocusRequester {
        return focusRequesters.getOrPut(id) { FocusRequester() }
    }

    fun saveConfig(nextConfig: PlayerBottomControlPanelConfig) {
        Prefs.playerBottomControlPanelConfig = nextConfig.normalized()
    }

    fun currentScale(target: ScaleTarget): Float {
        return when (target) {
            ScaleTarget.Title -> config.titleScale
            ScaleTarget.Info -> config.infoScale
            ScaleTarget.ActionRow -> config.actionRowScale
            ScaleTarget.SeekBar -> config.seekBarScale
            ScaleTarget.FunctionRow -> config.functionRowScale
        }
    }

    fun updateScale(target: ScaleTarget, direction: Int) {
        val nextScale = PlayerBottomControlPanelConfig.stepScale(
            scale = currentScale(target),
            direction = direction,
            maxScale = if (target == ScaleTarget.Title) {
                PlayerBottomControlPanelConfig.TitleMaxScale
            } else {
                PlayerBottomControlPanelConfig.MaxScale
            }
        )
        saveConfig(
            when (target) {
                ScaleTarget.Title -> config.copy(titleScale = nextScale)
                ScaleTarget.Info -> config.copy(infoScale = nextScale)
                ScaleTarget.ActionRow -> config.copy(actionRowScale = nextScale)
                ScaleTarget.SeekBar -> config.copy(seekBarScale = nextScale)
                ScaleTarget.FunctionRow -> config.copy(functionRowScale = nextScale)
            }
        )
    }

    fun moveButton(target: EditTarget, direction: Int) {
        val row = target.movableRow ?: return
        val defaultOrder = when (row) {
            MovableRow.Action -> PlayerBottomControlPanelConfig.DefaultActionButtonOrder
            MovableRow.Function -> PlayerBottomControlPanelConfig.DefaultFunctionButtonOrder
        }
        val currentOrder = PlayerBottomControlPanelConfig.normalizeOrder(
            order = when (row) {
                MovableRow.Action -> config.actionButtonOrder
                MovableRow.Function -> config.functionButtonOrder
            },
            defaultOrder = defaultOrder
        )
        val currentIndex = currentOrder.indexOf(target.id)
        val nextIndex = (currentIndex + direction).coerceIn(currentOrder.indices)
        if (currentIndex == -1 || currentIndex == nextIndex) return

        val nextOrder = currentOrder.toMutableList().apply {
            val item = removeAt(currentIndex)
            add(nextIndex, item)
        }
        saveConfig(
            when (row) {
                MovableRow.Action -> config.copy(actionButtonOrder = nextOrder)
                MovableRow.Function -> config.copy(functionButtonOrder = nextOrder)
            }
        )
    }

    fun handleEditKey(key: Key): Boolean {
        val mode = activeMode ?: return false
        return when (key) {
            Key.Back -> {
                activeMode = null
                true
            }

            Key.DirectionLeft -> {
                if (mode.type == EditModeType.Resize) {
                    updateScale(mode.target.scaleTarget, -1)
                } else {
                    moveButton(mode.target, -1)
                }
                true
            }

            Key.DirectionRight -> {
                if (mode.type == EditModeType.Resize) {
                    updateScale(mode.target.scaleTarget, 1)
                } else {
                    moveButton(mode.target, 1)
                }
                true
            }

            Key.DirectionUp -> {
                if (mode.type == EditModeType.Resize) {
                    updateScale(mode.target.scaleTarget, 1)
                    true
                } else {
                    false
                }
            }

            Key.DirectionDown -> {
                if (mode.type == EditModeType.Resize) {
                    updateScale(mode.target.scaleTarget, -1)
                    true
                } else {
                    false
                }
            }

            else -> false
        }
    }

    BackHandler(enabled = activeMode != null) {
        activeMode = null
    }

    LaunchedEffect(Unit) {
        focusRequester(TitleTarget.id).requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080A10))
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && handleEditKey(event.key)
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 48.dp, top = 28.dp, end = 48.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    activeMode = null
                    menuTarget = null
                    saveConfig(PlayerBottomControlPanelConfig.Default)
                },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
            ) {
                Text(text = "重置")
            }
            Text(
                text = stringResource(R.string.settings_ui_bottom_control_panel_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val statusText = activeMode?.let { mode ->
                val scale = currentScale(mode.target.scaleTarget)
                val modeText = if (mode.type == EditModeType.Resize) "调整大小" else "移动"
                "${mode.target.title} · $modeText · ${scale}x"
            } ?: selectedTarget?.let { target ->
                "${target.title} · ${currentScale(target.scaleTarget)}x"
            } ?: ""
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BottomControlPanelPreview(
            modifier = Modifier.align(Alignment.BottomCenter),
            config = config,
            selectedTarget = selectedTarget,
            activeMode = activeMode,
            focusRequester = ::focusRequester,
            onFocusTarget = { selectedTarget = it },
            onOpenMenu = { menuTarget = it }
        )
    }

    menuTarget?.let { target ->
        TvAlertDialog(
            onDismissRequest = { menuTarget = null },
            title = { Text(text = target.title) },
            text = {
                Column {
                    ListItem(
                        selected = false,
                        onClick = {
                            activeMode = ActiveEditMode(target, EditModeType.Resize)
                            menuTarget = null
                        },
                        headlineContent = { Text(text = "调整大小") }
                    )
                    if (target.movableRow != null) {
                        ListItem(
                            selected = false,
                            onClick = {
                                activeMode = ActiveEditMode(target, EditModeType.Move)
                                menuTarget = null
                            },
                            headlineContent = { Text(text = "移动") }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun BottomControlPanelPreview(
    modifier: Modifier = Modifier,
    config: PlayerBottomControlPanelConfig,
    selectedTarget: EditTarget?,
    activeMode: ActiveEditMode?,
    focusRequester: (String) -> FocusRequester,
    onFocusTarget: (EditTarget) -> Unit,
    onOpenMenu: (EditTarget) -> Unit
) {
    val actionButtons = config.orderedActionButtons(PlayerBottomControlPanelConfig.DefaultActionButtonOrder)
    val functionButtons = config.orderedFunctionButtons(PlayerBottomControlPanelConfig.DefaultFunctionButtonOrder)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.76f),
                        Color.Black.copy(alpha = 0.96f)
                    )
                )
            )
            .padding(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EditableRegion(
            target = TitleTarget,
            selectedTarget = selectedTarget,
            activeMode = activeMode,
            focusRequester = focusRequester(TitleTarget.id),
            onFocusTarget = onFocusTarget,
            onOpenMenu = onOpenMenu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                text = "示例视频标题 · 自定义底部控制面板预览",
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = (16 * config.titleScale).sp,
                    lineHeight = (20 * config.titleScale).sp
                )
            )
        }

        EditableRegion(
            target = InfoTarget,
            selectedTarget = selectedTarget,
            activeMode = activeMode,
            focusRequester = focusRequester(InfoTarget.id),
            onFocusTarget = onFocusTarget,
            onOpenMenu = onOpenMenu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .height((30 * config.infoScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size((22 * config.infoScale).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "UP 主 · 12.3 万播放 · 4567 弹幕 · 发布于 2026-06-08",
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * config.infoScale).sp,
                        lineHeight = (18 * config.infoScale).sp
                    )
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy((8 * config.actionRowScale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actionButtons.forEach { id ->
                PreviewActionButton(
                    target = EditTarget(
                        id = id,
                        title = actionButtonLabel(id),
                        scaleTarget = ScaleTarget.ActionRow,
                        movableRow = MovableRow.Action
                    ),
                    label = actionButtonLabel(id),
                    scale = config.actionRowScale,
                    selectedTarget = selectedTarget,
                    activeMode = activeMode,
                    focusRequester = focusRequester(id),
                    onFocusTarget = onFocusTarget,
                    onOpenMenu = onOpenMenu
                )
            }
        }

        EditableRegion(
            target = SeekBarTarget,
            selectedTarget = selectedTarget,
            activeMode = activeMode,
            focusRequester = focusRequester(SeekBarTarget.id),
            onFocusTarget = onFocusTarget,
            onOpenMenu = onOpenMenu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((20 * config.seekBarScale).dp)
                    .padding(horizontal = 2.dp)
                    .wrapContentSize(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((6 * config.seekBarScale).dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height((6 * config.seekBarScale).dp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFBD26B8).copy(alpha = 0.85f))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((16 * config.functionRowScale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            functionButtons.forEach { id ->
                PreviewFunctionButton(
                    target = EditTarget(
                        id = id,
                        title = functionButtonLabel(id),
                        scaleTarget = ScaleTarget.FunctionRow,
                        movableRow = MovableRow.Function
                    ),
                    icon = functionButtonIcon(id),
                    label = functionButtonLabel(id),
                    scale = config.functionRowScale,
                    selectedTarget = selectedTarget,
                    activeMode = activeMode,
                    focusRequester = focusRequester(id),
                    onFocusTarget = onFocusTarget,
                    onOpenMenu = onOpenMenu
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "01:23 / 08:36",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EditableRegion(
    target: EditTarget,
    selectedTarget: EditTarget?,
    activeMode: ActiveEditMode?,
    focusRequester: FocusRequester,
    onFocusTarget: (EditTarget) -> Unit,
    onOpenMenu: (EditTarget) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var hasFocus by remember(target.id) { mutableStateOf(false) }
    val isActive = activeMode?.target?.id == target.id
    val showBorder = hasFocus || selectedTarget?.id == target.id || isActive
    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        showBorder -> Color.White.copy(alpha = 0.78f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .focusRequester(focusRequester)
            .onFocusChanged {
                hasFocus = it.hasFocus
                if (it.hasFocus) onFocusTarget(target)
            }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onOpenMenu(target)
                    true
                } else {
                    false
                }
            },
        content = content
    )
}

@Composable
private fun PreviewActionButton(
    target: EditTarget,
    label: String,
    scale: Float,
    selectedTarget: EditTarget?,
    activeMode: ActiveEditMode?,
    focusRequester: FocusRequester,
    onFocusTarget: (EditTarget) -> Unit,
    onOpenMenu: (EditTarget) -> Unit
) {
    EditableRegion(
        target = target,
        selectedTarget = selectedTarget,
        activeMode = activeMode,
        focusRequester = focusRequester,
        onFocusTarget = onFocusTarget,
        onOpenMenu = onOpenMenu,
        modifier = Modifier
            .height((26 * scale).dp)
            .background(Color.Transparent)
    ) {
        Text(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = (8 * scale).dp)
                .wrapContentSize(Alignment.Center),
            text = label,
            color = Color.White.copy(alpha = 0.86f),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun PreviewFunctionButton(
    target: EditTarget,
    icon: ImageVector,
    label: String,
    scale: Float,
    selectedTarget: EditTarget?,
    activeMode: ActiveEditMode?,
    focusRequester: FocusRequester,
    onFocusTarget: (EditTarget) -> Unit,
    onOpenMenu: (EditTarget) -> Unit
) {
    EditableRegion(
        target = target,
        selectedTarget = selectedTarget,
        activeMode = activeMode,
        focusRequester = focusRequester,
        onFocusTarget = onFocusTarget,
        onOpenMenu = onOpenMenu,
        modifier = Modifier
            .height((36 * scale).dp)
            .width(functionButtonWidth(target.id, scale))
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (6 * scale).dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size((18 * scale).dp),
                imageVector = icon,
                contentDescription = label,
                tint = Color.White.copy(alpha = 0.88f)
            )
            if (target.id == PlayerBottomControlPanelButtonIds.Speed) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "1x",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private val TitleTarget = EditTarget(
    id = "panel_title",
    title = "标题",
    scaleTarget = ScaleTarget.Title
)

private val InfoTarget = EditTarget(
    id = "panel_info",
    title = "信息区",
    scaleTarget = ScaleTarget.Info
)

private val SeekBarTarget = EditTarget(
    id = "panel_seek_bar",
    title = "进度条",
    scaleTarget = ScaleTarget.SeekBar
)

private fun actionButtonLabel(id: String): String {
    return when (id) {
        PlayerBottomControlPanelButtonIds.Like -> "点赞"
        PlayerBottomControlPanelButtonIds.Favorite -> "收藏"
        PlayerBottomControlPanelButtonIds.Cache -> "缓存"
        PlayerBottomControlPanelButtonIds.Coin -> "投币"
        PlayerBottomControlPanelButtonIds.TripleLike -> "三连"
        PlayerBottomControlPanelButtonIds.Description -> "简介"
        PlayerBottomControlPanelButtonIds.Playlist -> "列表"
        PlayerBottomControlPanelButtonIds.Related -> "推荐"
        else -> id
    }
}

private fun functionButtonLabel(id: String): String {
    return when (id) {
        PlayerBottomControlPanelButtonIds.Comment -> "评论"
        PlayerBottomControlPanelButtonIds.PrevVideo -> "上一个"
        PlayerBottomControlPanelButtonIds.NextVideo -> "下一个"
        PlayerBottomControlPanelButtonIds.AudioMode -> "音频"
        PlayerBottomControlPanelButtonIds.Danmaku -> "弹幕"
        PlayerBottomControlPanelButtonIds.Subtitle -> "字幕"
        PlayerBottomControlPanelButtonIds.Loop -> "循环"
        PlayerBottomControlPanelButtonIds.Speed -> "速度"
        PlayerBottomControlPanelButtonIds.Refresh -> "刷新"
        PlayerBottomControlPanelButtonIds.Rotation -> "旋转"
        PlayerBottomControlPanelButtonIds.Settings -> "设置"
        else -> id
    }
}

private fun functionButtonIcon(id: String): ImageVector {
    return when (id) {
        PlayerBottomControlPanelButtonIds.Comment -> Icons.AutoMirrored.Rounded.Chat
        PlayerBottomControlPanelButtonIds.AudioMode -> Icons.Rounded.Headphones
        PlayerBottomControlPanelButtonIds.Loop -> Icons.Rounded.Repeat
        PlayerBottomControlPanelButtonIds.Speed -> Icons.Rounded.Refresh
        PlayerBottomControlPanelButtonIds.Refresh -> Icons.Rounded.Refresh
        PlayerBottomControlPanelButtonIds.Rotation -> Icons.Rounded.ScreenRotation
        PlayerBottomControlPanelButtonIds.Settings -> Icons.Outlined.Settings
        PlayerBottomControlPanelButtonIds.PrevVideo -> Icons.AutoMirrored.Rounded.PlaylistPlay
        PlayerBottomControlPanelButtonIds.NextVideo -> Icons.AutoMirrored.Rounded.PlaylistPlay
        PlayerBottomControlPanelButtonIds.Danmaku -> Icons.AutoMirrored.Rounded.Chat
        PlayerBottomControlPanelButtonIds.Subtitle -> Icons.Outlined.Info
        else -> Icons.Rounded.KeyboardDoubleArrowDown
    }
}

private fun functionButtonWidth(id: String, scale: Float) = when (id) {
    PlayerBottomControlPanelButtonIds.Speed -> (52 * scale).dp
    else -> (36 * scale).dp
}
