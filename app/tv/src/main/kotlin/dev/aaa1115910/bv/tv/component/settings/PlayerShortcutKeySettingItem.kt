package dev.aaa1115910.bv.tv.component.settings

import android.content.Context
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.player.entity.PlayerShortcutAction
import dev.aaa1115910.bv.tv.component.TvAlertDialog

@Composable
fun PlayerShortcutKeyBindingsDialog(
    show: Boolean,
    keyBindings: Map<PlayerShortcutAction, Int>,
    onKeyCodeChange: (PlayerShortcutAction, Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (!show) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.68f).dp

    TvAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = context.getString(R.string.settings_player_shortcut_section_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 4.dp),
                    text = context.getString(R.string.settings_player_shortcut_dialog_top_text),
                    style = MaterialTheme.typography.bodyMedium
                )
                PlayerShortcutAction.entries.forEach { action ->
                    PlayerShortcutKeySettingItem(
                        action = action,
                        keyCode = keyBindings[action] ?: AndroidKeyEvent.KEYCODE_UNKNOWN,
                        onKeyCodeChange = { keyCode -> onKeyCodeChange(action, keyCode) }
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text(text = context.getString(R.string.common_confirm))
            }
        }
    )
}

@Composable
fun PlayerShortcutKeySettingItem(
    modifier: Modifier = Modifier,
    action: PlayerShortcutAction,
    keyCode: Int,
    onKeyCodeChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val actionName = action.displayName(context)
    val keyName = formatPlayerShortcutKeyCode(context, keyCode)

    SettingListItem(
        modifier = modifier,
        title = actionName,
        supportText = context.getString(R.string.settings_player_shortcut_support_text),
        valueText = keyName,
        onClick = {
            capturing = false
            showDialog = true
        }
    )

    if (showDialog) {
        if (capturing) {
            ShortcutKeyCaptureDialog(
                actionName = actionName,
                onDismissRequest = {
                    capturing = false
                    showDialog = false
                },
                onKeyCaptured = { capturedKeyCode ->
                    onKeyCodeChange(capturedKeyCode)
                    capturing = false
                    showDialog = false
                }
            )
        } else {
            TvAlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = actionName) },
                text = {
                    Text(
                        text = context.getString(
                            R.string.settings_player_shortcut_dialog_text,
                            keyName
                        )
                    )
                },
                confirmButton = {
                    Button(onClick = { capturing = true }) {
                        Text(text = context.getString(R.string.settings_player_shortcut_capture_button))
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        onKeyCodeChange(AndroidKeyEvent.KEYCODE_UNKNOWN)
                        showDialog = false
                    }) {
                        Text(text = context.getString(R.string.settings_player_shortcut_clear_button))
                    }
                }
            )
        }
    }
}

@Composable
private fun ShortcutKeyCaptureDialog(
    actionName: String,
    onDismissRequest: () -> Unit,
    onKeyCaptured: (Int) -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TvAlertDialog(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                val keyCode = keyEvent.nativeKeyEvent.keyCode
                if (keyEvent.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                when (keyCode) {
                    AndroidKeyEvent.KEYCODE_BACK -> onDismissRequest()
                    AndroidKeyEvent.KEYCODE_HOME,
                    AndroidKeyEvent.KEYCODE_UNKNOWN -> Unit
                    else -> onKeyCaptured(keyCode)
                }
                true
            },
        onDismissRequest = onDismissRequest,
        title = { Text(text = actionName) },
        text = {
            Text(
                text = context.getString(
                    R.string.settings_player_shortcut_capture_text,
                    actionName
                )
            )
        },
        confirmButton = {}
    )
}

private fun PlayerShortcutAction.displayName(context: Context): String = when (this) {
    PlayerShortcutAction.ToggleDanmaku -> context.getString(R.string.settings_player_shortcut_action_danmaku)
    PlayerShortcutAction.ToggleComment -> context.getString(R.string.settings_player_shortcut_action_comment)
    PlayerShortcutAction.ToggleSubtitle -> context.getString(R.string.settings_player_shortcut_action_subtitle)
    PlayerShortcutAction.TripleLike -> context.getString(R.string.settings_player_shortcut_action_triple_like)
    PlayerShortcutAction.ToggleRelatedVideos -> context.getString(R.string.settings_player_shortcut_action_related_videos)
}

private fun formatPlayerShortcutKeyCode(context: Context, keyCode: Int): String = when (keyCode) {
    AndroidKeyEvent.KEYCODE_UNKNOWN -> context.getString(R.string.settings_player_shortcut_unbound)
    AndroidKeyEvent.KEYCODE_DPAD_UP -> "方向上"
    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> "方向下"
    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> "方向左"
    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> "方向右"
    AndroidKeyEvent.KEYCODE_DPAD_CENTER -> "确认键"
    AndroidKeyEvent.KEYCODE_ENTER -> "Enter"
    AndroidKeyEvent.KEYCODE_MENU -> "菜单键"
    AndroidKeyEvent.KEYCODE_INFO -> "信息键"
    AndroidKeyEvent.KEYCODE_CAPTIONS -> "字幕键"
    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "播放/暂停"
    AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> "播放键"
    AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> "暂停键"
    AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "快进键"
    AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> "快退键"
    AndroidKeyEvent.KEYCODE_MEDIA_NEXT -> "下一首"
    AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS -> "上一首"
    AndroidKeyEvent.KEYCODE_CHANNEL_UP -> "频道+"
    AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> "频道-"
    AndroidKeyEvent.KEYCODE_PROG_RED -> "红色键"
    AndroidKeyEvent.KEYCODE_PROG_GREEN -> "绿色键"
    AndroidKeyEvent.KEYCODE_PROG_YELLOW -> "黄色键"
    AndroidKeyEvent.KEYCODE_PROG_BLUE -> "蓝色键"
    else -> AndroidKeyEvent.keyCodeToString(keyCode)
        .removePrefix("KEYCODE_")
        .replace('_', ' ')
}
