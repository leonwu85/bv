package dev.aaa1115910.bv.mobile.dlna

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.viewmodel.DlnaMediaSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class DlnaCastSession(
    val device: DlnaDevice,
    val source: DlnaMediaSource,
    val durationMs: Long,
    val positionMs: Long,
    val isPlaying: Boolean,
)

@Composable
fun DlnaControlDialog(
    manager: DlnaManager,
    session: DlnaCastSession,
    onSessionChange: (DlnaCastSession) -> Unit,
    onStopped: () -> Unit,
    onChangeDevice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var seekPositionMs by remember(session.device.id, session.source.url) {
        mutableFloatStateOf(session.positionMs.coerceAtLeast(0L).toFloat())
    }
    val durationMs = session.durationMs.coerceAtLeast(session.positionMs).coerceAtLeast(0L)

    fun runRemoteAction(
        action: suspend () -> Unit,
        onSuccess: () -> Unit,
    ) {
        if (busy) return
        scope.launch {
            busy = true
            errorMessage = null
            try {
                action()
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                errorMessage = error.localizedMessage ?: "投屏设备操作失败"
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(text = "投屏控制") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = session.device.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = session.source.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (durationMs > 0L) {
                    Slider(
                        value = seekPositionMs.coerceIn(0f, durationMs.toFloat()),
                        onValueChange = { seekPositionMs = it },
                        valueRange = 0f..durationMs.toFloat(),
                        enabled = !busy,
                        onValueChangeFinished = {
                            val targetPositionMs = seekPositionMs.toLong().coerceIn(0L, durationMs)
                            runRemoteAction(
                                action = { manager.seek(session.device, targetPositionMs) },
                                onSuccess = {
                                    onSessionChange(session.copy(positionMs = targetPositionMs))
                                },
                            )
                        },
                    )
                    Text(
                        text = "${seekPositionMs.toLong().formatHourMinSec()} / ${durationMs.formatHourMinSec()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        enabled = !busy && !session.isPlaying,
                        onClick = {
                            runRemoteAction(
                                action = { manager.play(session.device) },
                                onSuccess = {
                                    onSessionChange(session.copy(isPlaying = true))
                                },
                            )
                        },
                    ) {
                        Text(text = "播放")
                    }
                    Button(
                        enabled = !busy && session.isPlaying,
                        onClick = {
                            runRemoteAction(
                                action = { manager.pause(session.device) },
                                onSuccess = {
                                    onSessionChange(session.copy(isPlaying = false))
                                },
                            )
                        },
                    ) {
                        Text(text = "暂停")
                    }
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            runRemoteAction(
                                action = { manager.stop(session.device) },
                                onSuccess = onStopped,
                            )
                        },
                    ) {
                        Text(text = "停止投屏")
                    }
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = onDismiss,
            ) {
                Text(text = "关闭")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    runRemoteAction(
                        action = { manager.stop(session.device) },
                        onSuccess = onChangeDevice,
                    )
                },
            ) {
                Text(text = "更换设备")
            }
        },
    )
}
