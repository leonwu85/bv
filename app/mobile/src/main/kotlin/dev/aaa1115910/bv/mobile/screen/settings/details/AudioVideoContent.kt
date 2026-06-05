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
import dev.aaa1115910.bv.entity.CdnService
import dev.aaa1115910.bv.entity.LiveQualityPreference
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.mobile.component.LibMPVDownloaderDialog
import dev.aaa1115910.bv.mobile.component.preferences.items.editTextPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.SuperResolutionType
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.impl.mpv.MpvLibsInstaller
import dev.aaa1115910.bv.util.PlaybackPreferenceSelector

@Composable
fun AudioVideoContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPlayerType by remember { mutableStateOf(MobilePrefs.playerType) }
    var showMpvDownloadConfirmDialog by remember { mutableStateOf(false) }
    var showMpvDownloaderDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        preferenceGroups(
            "播放器" to {
                radioPreference(
                    title = "播放器内核",
                    prefReq = MobilePrefKeys.playerTypeRequest,
                    values = PlayerType.entries
                        .filter { it != PlayerType.VLC }
                        .associate { it.ordinal to it.name },
                    onValueChange = { ordinal ->
                        val newType = PlayerType.entries[ordinal]
                        if (newType == PlayerType.MPV && MpvLibsInstaller.needsInstall(context)) {
                            showMpvDownloadConfirmDialog = true
                            false
                        } else {
                            selectedPlayerType = newType
                            true
                        }
                    }
                )
                if (selectedPlayerType == PlayerType.MPV) {
                    radioPreference(
                        title = "超分辨率",
                        prefReq = MobilePrefKeys.superResolutionTypeRequest,
                        values = SuperResolutionType.entries.associate { it.value to it.displayName(context) }
                    )
                }
            },
            "CDN" to {
                switchPreference(
                    title = "免登录 1080P",
                    summary = "未登录且使用 Web 接口请求播放地址时尝试追加 try_look",
                    prefReq = MobilePrefKeys.tryLook1080PRequest,
                    onCheckedChange = { true }
                )
                switchPreference(
                    title = "B站定向流量提示",
                    summary = "当前内核仅保留提示入口，不改变流量计费规则",
                    enabled = false,
                    checked = true,
                    onCheckedChange = {}
                )
                radioPreference(
                    title = "CDN 设置",
                    prefReq = MobilePrefKeys.cdnServiceRequest,
                    values = CdnService.entries.associate { it.ordinal to it.displayName }
                )
                editTextPreference(
                    title = "直播 CDN",
                    prefReq = MobilePrefKeys.liveCdnUrlRequest,
                    emptySummary = "默认",
                    transformValue = PlaybackPreferenceSelector::normalizeLiveCdnHost
                )
                switchPreference(
                    title = "CDN 测速",
                    summary = "当前内核仅保存配置，播放时仍按 CDN 设置选择",
                    prefReq = MobilePrefKeys.cdnSpeedTestRequest,
                    onCheckedChange = { true }
                )
                switchPreference(
                    title = "音频不跟随 CDN",
                    summary = "开启后音频流保留 B站返回的 URL",
                    prefReq = MobilePrefKeys.disableAudioCdnRequest,
                    onCheckedChange = { true }
                )
            },
            "画质/音质" to {
                radioPreference(
                    title = "默认画质",
                    prefReq = MobilePrefKeys.defaultQualityRequest,
                    values = Resolution.entries.associate { it.code to it.getDisplayName(context) }
                        .toSortedMap { a, b -> a.compareTo(b) }
                )
                radioPreference(
                    title = "蜂窝画质",
                    prefReq = MobilePrefKeys.defaultCellularQualityRequest,
                    values = Resolution.entries.associate { it.code to it.getDisplayName(context) }
                        .toSortedMap { a, b -> a.compareTo(b) }
                )
                radioPreference(
                    title = "默认音质",
                    prefReq = MobilePrefKeys.defaultAudioRequest,
                    values = Audio.entries.associate { it.code to it.getDisplayName(context) }
                        .toSortedMap { a, b -> a.compareTo(b) }
                )
                radioPreference(
                    title = "蜂窝音质",
                    prefReq = MobilePrefKeys.defaultCellularAudioRequest,
                    values = Audio.entries.associate { it.code to it.getDisplayName(context) }
                        .toSortedMap { a, b -> a.compareTo(b) }
                )
            },
            "直播" to {
                radioPreference(
                    title = "默认直播画质",
                    prefReq = MobilePrefKeys.defaultLiveQnRequest,
                    values = LiveQualityPreference.entries.associate { it.qn to it.getDisplayName(context) }
                )
                radioPreference(
                    title = "蜂窝直播画质",
                    prefReq = MobilePrefKeys.defaultCellularLiveQnRequest,
                    values = LiveQualityPreference.entries.associate { it.qn to it.getDisplayName(context) }
                )
                radioPreference(
                    title = "默认直播流",
                    prefReq = MobilePrefKeys.defaultLiveCodecRequest,
                    values = LiveCodec.entries.associate { it.ordinal to it.getDisplayName(context) }
                )
            },
            "解码与缓冲" to {
                switchPreference(
                    title = "开启硬解",
                    summary = "关闭后 Media3 优先软解，VLC 禁用硬件加速，MPV 使用 --hwdec=no",
                    prefReq = MobilePrefKeys.enableHardwareDecodeRequest,
                    onCheckedChange = { true }
                )
                radioPreference(
                    title = "首选解码格式",
                    prefReq = MobilePrefKeys.defaultVideoCodecRequest,
                    values = VideoCodec.entries.associate { it.ordinal to it.getDisplayName(context) }
                )
                radioPreference(
                    title = "次选解码格式",
                    prefReq = MobilePrefKeys.secondVideoCodecRequest,
                    values = VideoCodec.entries.associate { it.ordinal to it.getDisplayName(context) }
                )
                switchPreference(
                    title = "扩大缓冲区",
                    summary = "提升弱网稳定性，可能增加起播等待和内存占用",
                    prefReq = MobilePrefKeys.expandBufferRequest,
                    onCheckedChange = { true }
                )
            },
            "兼容性" to {
                editTextPreference(
                    title = "音频输出设备",
                    prefReq = MobilePrefKeys.audioOutputDevicesRequest,
                    summary = { value ->
                        value.takeIf { it.isNotBlank() }
                            ?.let { "$it（MPV --ao）" }
                            ?: "MPV --ao，逗号分隔"
                    },
                    transformValue = { value ->
                        value.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .joinToString(",")
                    }
                )
                editTextPreference(
                    title = "自动同步",
                    prefReq = MobilePrefKeys.autoSyncRequest,
                    summary = { value ->
                        value.takeIf { it.isNotBlank() }
                            ?.let { "$it（MPV --autosync）" }
                            ?: "0 或留空表示使用 MPV 默认"
                    },
                    transformValue = { value -> value.trim().filter { it.isDigit() } }
                )
                radioPreference(
                    title = "视频同步",
                    prefReq = MobilePrefKeys.videoSyncRequest,
                    values = mapOf(
                        "audio" to "audio",
                        "display-resample" to "display-resample",
                        "display-resample-vdrop" to "display-resample-vdrop",
                        "display-resample-desync" to "display-resample-desync",
                        "display-tempo" to "display-tempo",
                        "display-vdrop" to "display-vdrop",
                        "display-adrop" to "display-adrop",
                        "display-desync" to "display-desync",
                        "desync" to "desync"
                    )
                )
                radioPreference(
                    title = "硬解模式",
                    prefReq = MobilePrefKeys.hardwareDecodeModeRequest,
                    values = mapOf(
                        "no" to "no\n启用软解",
                        "auto" to "auto\n启用任意可用解码器",
                        "auto-safe" to "auto-safe\n启用最佳解码器",
                        "auto-copy" to "auto-copy\n启用带拷贝功能的最佳解码器",
                        "mediacodec" to "mediacodec\nMediaCodec (Android)",
                        "mediacodec-copy" to "mediacodec-copy\nMediaCodec (Android) (非直通)",
                        "rkmpp" to "rkmpp\nRockchip MPP (仅部分 Rockchip 芯片)",
                        "vulkan" to "vulkan\nVulkan (实验性)",
                        "vulkan-copy" to "vulkan-copy\nVulkan (实验性) (非直通)"
                    )
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
                selectedPlayerType = PlayerType.MPV
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
private fun AudioVideoContentPreview() {
    BVMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            AudioVideoContent(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
