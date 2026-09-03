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
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme

@Composable
fun AdvanceContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kernelState = rememberPlayerKernelState()

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
                playerKernelPreferences(context = context, state = kernelState, includeVlcOptions = false)
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

    PlayerKernelDialogs(state = kernelState)
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
