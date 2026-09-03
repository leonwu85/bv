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
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.mobile.util.MobileMpvOptions
import dev.aaa1115910.bv.player.entity.SuperResolutionType

/**
 * 手机端 MPV 参数，与 TV 端 `MpvSetting` 同一套取值表：所有项只能从固定选项里选
 * （mpv-android 这个构建没有 Vulkan/rkmpp，gpu-context/gpu-api 固定为 Android GL，不再暴露），
 * 存量的自由文本值在启动时归一化为“默认”。所有更改重进播放器后生效。
 */
@Composable
fun MpvContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        preferenceGroups(
            "画面" to {
                radioPreference(
                    title = "超分辨率",
                    prefReq = MobilePrefKeys.superResolutionTypeRequest,
                    values = SuperResolutionType.entries.associate { it.value to it.displayName(context) }
                )
                radioPreference(
                    title = "vo（视频输出）",
                    prefReq = MobilePrefKeys.mpvVideoOutputRequest,
                    values = MobileMpvOptions.videoOutputOptions
                )
            },
            "解码" to {
                radioPreference(
                    title = "hwdec（硬解方式）",
                    prefReq = MobilePrefKeys.hardwareDecodeModeRequest,
                    values = MobileMpvOptions.hardwareDecodeOptions
                )
                radioPreference(
                    title = "hwdec-codecs（允许硬解的编码）",
                    prefReq = MobilePrefKeys.mpvHardwareDecodeCodecsRequest,
                    values = MobileMpvOptions.hardwareDecodeCodecsOptions
                )
                radioPreference(
                    title = "vd-queue-enable（解码器输出队列）",
                    prefReq = MobilePrefKeys.mpvVdQueueEnableRequest,
                    values = MobileMpvOptions.vdQueueEnableOptions
                )
            },
            "缓存" to {
                radioPreference(
                    title = "cache（网络流缓存）",
                    prefReq = MobilePrefKeys.mpvCacheRequest,
                    values = MobileMpvOptions.cacheOptions
                )
                radioPreference(
                    title = "demuxer-max-bytes（前向缓存上限）",
                    prefReq = MobilePrefKeys.mpvDemuxerMaxBytesRequest,
                    values = MobileMpvOptions.demuxerMaxBytesOptions
                )
                radioPreference(
                    title = "demuxer-max-back-bytes（后向缓存上限）",
                    prefReq = MobilePrefKeys.mpvDemuxerMaxBackBytesRequest,
                    values = MobileMpvOptions.demuxerMaxBackBytesOptions
                )
            },
            "网络" to {
                switchPreference(
                    title = "CDN 使用 HTTP 直连",
                    summary = "把 bilivideo/akamaized 的 HTTPS 播放地址改写为 HTTP。MPV 已使用系统根证书校验 HTTPS，" +
                        "仅在系统根证书过期/损坏导致 TLS 校验失败时打开；带签名的播放地址与请求头会明文传输，" +
                        "部分 PCDN 节点不提供 HTTP",
                    prefReq = MobilePrefKeys.mpvPreferHttpCdnRequest,
                    onCheckedChange = { true }
                )
            }
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MpvContentPreview() {
    BVMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            MpvContent(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
