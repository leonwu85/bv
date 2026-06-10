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
import de.schnettler.datastore.manager.PreferenceRequest
import dev.aaa1115910.bv.mobile.component.preferences.PreferenceGroupScope
import dev.aaa1115910.bv.mobile.component.preferences.items.editTextPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.player.entity.SuperResolutionType

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
                    title = "vo",
                    prefReq = MobilePrefKeys.mpvVideoOutputRequest,
                    values = MpvVideoOutputOptions
                )
                radioPreference(
                    title = "gpu-context",
                    prefReq = MobilePrefKeys.mpvGpuContextRequest,
                    values = MpvGpuContextOptions
                )
                radioPreference(
                    title = "gpu-api",
                    prefReq = MobilePrefKeys.mpvGpuApiRequest,
                    values = MpvGpuApiOptions
                )
            },
            "解码与缓存" to {
                radioPreference(
                    title = "hwdec",
                    prefReq = MobilePrefKeys.hardwareDecodeModeRequest,
                    values = MpvHardwareDecodeOptions
                )
                radioPreference(
                    title = "hwdec-codecs",
                    prefReq = MobilePrefKeys.mpvHardwareDecodeCodecsRequest,
                    values = MpvHardwareDecodeCodecsOptions
                )
                radioPreference(
                    title = "vd-queue-enable",
                    prefReq = MobilePrefKeys.mpvVdQueueEnableRequest,
                    values = MpvVdQueueEnableOptions
                )
                mpvTextPreference(
                    title = "cache",
                    prefReq = MobilePrefKeys.mpvCacheRequest,
                    defaultValue = "yes",
                    defaultSummary = "默认 yes",
                    optionName = "cache"
                )
                mpvTextPreference(
                    title = "demuxer-max-bytes",
                    prefReq = MobilePrefKeys.mpvDemuxerMaxBytesRequest,
                    defaultValue = "150MiB",
                    defaultSummary = "默认 150MiB",
                    optionName = "demuxer-max-bytes"
                )
                mpvTextPreference(
                    title = "demuxer-max-back-bytes",
                    prefReq = MobilePrefKeys.mpvDemuxerMaxBackBytesRequest,
                    defaultValue = "50MiB",
                    defaultSummary = "默认 50MiB",
                    optionName = "demuxer-max-back-bytes"
                )
            }
        )
    }
}

private fun PreferenceGroupScope.mpvTextPreference(
    title: String,
    prefReq: PreferenceRequest<String>,
    defaultValue: String,
    defaultSummary: String,
    optionName: String
) {
    editTextPreference(
        title = title,
        prefReq = prefReq,
        emptySummary = "$defaultSummary（MPV --$optionName）",
        summary = { value ->
            value.takeIf { it.isNotBlank() }
                ?.let { "$it（MPV --$optionName）" }
                ?: "$defaultSummary（MPV --$optionName）"
        },
        transformValue = { value -> value.trim().replace("\n", "").ifBlank { defaultValue } }
    )
}

private val MpvHardwareDecodeOptions = linkedMapOf(
    "no" to "no（软解）",
    "auto-safe" to "auto-safe",
    "auto" to "auto",
    "auto-copy" to "auto-copy",
    "mediacodec" to "mediacodec",
    "mediacodec-copy" to "mediacodec-copy",
    "rkmpp" to "rkmpp",
    "vulkan" to "vulkan",
    "vulkan-copy" to "vulkan-copy"
)

private val MpvHardwareDecodeCodecsOptions = linkedMapOf(
    "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1" to "默认：h264, hevc, mpeg4, mpeg2video, vp8, vp9, av1",
    "h264,hevc" to "h264, hevc",
    "h264,hevc,vp9,av1" to "h264, hevc, vp9, av1",
    "all" to "all"
)

private val MpvVideoOutputOptions = linkedMapOf(
    "gpu" to "gpu",
    "gpu-next" to "gpu-next",
    "mediacodec_embed" to "mediacodec_embed"
)

private val MpvGpuContextOptions = linkedMapOf(
    "android" to "android",
    "auto" to "auto",
    "angle" to "angle"
)

private val MpvGpuApiOptions = linkedMapOf(
    "" to "默认",
    "opengl" to "opengl",
    "vulkan" to "vulkan"
)

private val MpvVdQueueEnableOptions = linkedMapOf(
    "" to "默认",
    "yes" to "yes",
    "no" to "no"
)

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
