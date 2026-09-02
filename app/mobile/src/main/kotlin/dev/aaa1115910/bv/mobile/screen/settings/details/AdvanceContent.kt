package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.mobile.component.LibMPVDownloaderDialog
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.player.impl.mpv.MpvLibsInstaller

@Composable
fun AdvanceContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMpvDownloadConfirmDialog by remember { mutableStateOf(false) }
    var showMpvDownloaderDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        preferenceGroups(
            "接口" to {
                radioPreference(
                    title = "接口偏好",
                    prefReq = MobilePrefKeys.apiTypeRequest,
                    values = ApiType.entries.associate { it.ordinal to it.name }
                        .toSortedMap { a, b -> a.compareTo(b) }
                )
            },
            "播放器" to {
                radioPreference(
                    title = "播放器内核",
                    prefReq = MobilePrefKeys.playerTypeRequest,
                    values = PlayerType.entries
                        .filter { it != PlayerType.VLC }
                        .associate { it.ordinal to it.name },
                    onValueChange = { ordinal ->
                        val newType = PlayerType.entries[ordinal]
                        if (newType == PlayerType.MPV && MpvLibsInstaller.needsUpdate(context)) {
                            showMpvDownloadConfirmDialog = true
                            false
                        } else {
                            true
                        }
                    }
                )
                switchPreference(
                    title = "启用 Tunneling",
                    summary = "仅影响 mobile 播放器",
                    prefReq = MobilePrefKeys.enableMobileTunnelingRequest,
                    onCheckedChange = { true }
                )
                switchPreference(
                    title = "FFmpeg 音频渲染器",
                    prefReq = MobilePrefKeys.enableFfmpegAudioRendererRequest,
                    onCheckedChange = { true }
                )
                switchPreference(
                    title = "异步队列",
                    prefReq = MobilePrefKeys.enableAsyncQueueingRequest,
                    onCheckedChange = { true }
                )
                switchPreference(
                    title = "系统倍速音频参数",
                    prefReq = MobilePrefKeys.enableAudioPlaybackParamsRequest,
                    onCheckedChange = { true }
                )
            },
            "CDN" to {
                switchPreference(
                    title = "优先官方 CDN",
                    prefReq = MobilePrefKeys.preferOfficialCdnRequest,
                    onCheckedChange = { true }
                )
            }
        )
    }

    if (showMpvDownloadConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showMpvDownloadConfirmDialog = false },
            title = { Text("需要下载 MPV 组件") },
            text = {
                Text(
                    "MPV 播放器需要下载官方 mpv-android 组件才能使用。\n\n" +
                            "来源：mpv-android 官方 GitHub Release\n" +
                            "连接失败时会自动尝试 GitHub 镜像\n" +
                            "建议在 Wi-Fi 环境下下载"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showMpvDownloadConfirmDialog = false
                    showMpvDownloaderDialog = true
                }) {
                    Text("下载")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMpvDownloadConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showMpvDownloaderDialog) {
        LibMPVDownloaderDialog(
            show = true,
            onDismissRequest = {
                showMpvDownloaderDialog = false
            },
            onDownloadComplete = {
                showMpvDownloaderDialog = false
                MobilePrefs.playerType = PlayerType.MPV
                Toast.makeText(context, "MPV 组件下载完成", Toast.LENGTH_SHORT).show()
            },
            onDownloadFailed = { errorMessage ->
                showMpvDownloaderDialog = false
                Toast.makeText(context, "下载失败: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdvanceContentPreview() {
    BVMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            AdvanceContent(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
