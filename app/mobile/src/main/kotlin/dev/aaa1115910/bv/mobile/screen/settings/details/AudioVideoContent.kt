package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.entity.CdnService
import dev.aaa1115910.bv.entity.LiveQualityPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.editTextPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.util.PlaybackPreferenceSelector

@Composable
fun AudioVideoContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        preferenceGroups(
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
                    summary = "关闭后 Media3 优先软解，VLC 禁用硬件加速",
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
                            ?.let { "$it（当前 Media3/VLC 内核仅保存配置）" }
                            ?: "当前 Media3/VLC 内核仅保存配置"
                    }
                )
                editTextPreference(
                    title = "自动同步",
                    prefReq = MobilePrefKeys.autoSyncRequest,
                    summary = { value ->
                        value.takeIf { it.isNotBlank() }
                            ?.let { "$it（当前 Media3/VLC 内核仅保存配置）" }
                            ?: "当前 Media3/VLC 内核仅保存配置"
                    }
                )
                radioPreference(
                    title = "视频同步",
                    prefReq = MobilePrefKeys.videoSyncRequest,
                    values = mapOf(
                        "audio" to "audio（仅保存）",
                        "display-resample" to "display-resample（仅保存）",
                        "display-resample-vdrop" to "display-resample-vdrop（仅保存）",
                        "display-vdrop" to "display-vdrop（仅保存）",
                        "display-adrop" to "display-adrop（仅保存）",
                        "desync" to "desync（仅保存）"
                    )
                )
                radioPreference(
                    title = "硬解模式",
                    prefReq = MobilePrefKeys.hardwareDecodeModeRequest,
                    values = mapOf(
                        "no" to "no（仅保存）",
                        "auto" to "auto（仅保存）",
                        "auto-safe" to "auto-safe（仅保存）",
                        "auto-copy" to "auto-copy（仅保存）",
                        "mediacodec" to "mediacodec（仅保存）",
                        "mediacodec-copy" to "mediacodec-copy（仅保存）"
                    )
                )
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
