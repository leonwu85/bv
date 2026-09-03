package dev.aaa1115910.bv.mobile.component.player

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.CdnService
import dev.aaa1115910.bv.viewmodel.LowerResolutionReason
import dev.aaa1115910.bv.viewmodel.VodBufferRecoveryPrompt

/**
 * 点播卡顿恢复提示（换 CDN / 降清晰度 / 关闭超分），与 TV 端 `VideoPlayerV3Screen` 的同名弹窗对应。
 * 提示由共享的 `VideoPlayerV3ViewModel` 按缓冲与丢帧统计产生，这里只负责展示与转发选择。
 */
@Composable
fun VodBufferRecoveryDialog(
    prompt: VodBufferRecoveryPrompt,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val context = LocalContext.current
    val title: String
    val message: String
    val dismissText: String
    val confirmText: String

    when (prompt) {
        is VodBufferRecoveryPrompt.SwitchCdn -> {
            val targetLine = when (prompt.toService) {
                CdnService.BaseUrl -> "主线路"
                else -> "备选线路"
            }
            title = stringResource(R.string.vod_buffer_recovery_cdn_title)
            message = stringResource(R.string.vod_buffer_recovery_cdn_message, targetLine)
            dismissText = stringResource(R.string.vod_buffer_recovery_cdn_dismiss)
            confirmText = stringResource(R.string.vod_buffer_recovery_cdn_confirm)
        }

        is VodBufferRecoveryPrompt.LowerResolution -> {
            val fromResolution = prompt.fromResolution.getShortDisplayName(context)
            val toResolution = prompt.toResolution.getShortDisplayName(context)
            when (prompt.reason) {
                LowerResolutionReason.Rebuffering -> {
                    title = stringResource(R.string.vod_buffer_recovery_resolution_title)
                    message = stringResource(
                        R.string.vod_buffer_recovery_resolution_message,
                        fromResolution,
                        toResolution,
                    )
                }

                LowerResolutionReason.DecoderOverload -> {
                    title = stringResource(R.string.vod_decoder_overload_resolution_title)
                    message = stringResource(
                        R.string.vod_decoder_overload_resolution_message,
                        fromResolution,
                        toResolution,
                    )
                }
            }
            dismissText = stringResource(R.string.vod_buffer_recovery_resolution_dismiss)
            confirmText = stringResource(R.string.vod_buffer_recovery_resolution_confirm, toResolution)
        }

        is VodBufferRecoveryPrompt.DisableSuperResolution -> {
            title = stringResource(R.string.vod_super_resolution_overload_title)
            message = stringResource(R.string.vod_super_resolution_overload_message)
            dismissText = stringResource(R.string.vod_super_resolution_overload_dismiss)
            confirmText = stringResource(R.string.vod_super_resolution_overload_confirm)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
