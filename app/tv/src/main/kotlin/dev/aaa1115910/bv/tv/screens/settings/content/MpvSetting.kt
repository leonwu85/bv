package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.entity.SuperResolutionType
import dev.aaa1115910.bv.tv.component.settings.SettingListItemWithDialog
import dev.aaa1115910.bv.tv.component.settings.SettingTextListItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.util.Prefs

@Composable
fun MpvSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var superResolutionType by remember { mutableStateOf(Prefs.superResolutionType) }
    var hardwareDecodeMode by remember { mutableStateOf(Prefs.tvMpvHardwareDecodeMode) }
    var mpvHardwareDecodeCodecs by remember { mutableStateOf(Prefs.tvMpvHardwareDecodeCodecs) }
    var mpvVideoOutput by remember { mutableStateOf(Prefs.tvMpvVideoOutput) }
    var mpvGpuContext by remember { mutableStateOf(Prefs.tvMpvGpuContext) }
    var mpvGpuApi by remember { mutableStateOf(Prefs.tvMpvGpuApi) }
    var mpvCache by remember { mutableStateOf(Prefs.tvMpvCache) }
    var mpvDemuxerMaxBytes by remember { mutableStateOf(Prefs.tvMpvDemuxerMaxBytes) }
    var mpvDemuxerMaxBackBytes by remember { mutableStateOf(Prefs.tvMpvDemuxerMaxBackBytes) }
    var mpvVdQueueEnable by remember { mutableStateOf(Prefs.tvMpvVdQueueEnable) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = SettingsMenuNavItem.Mpv.getDisplayName(context),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingListItemWithDialog(
                        title = "超分辨率",
                        supportText = "仅 MPV 内核生效，重进播放器后生效",
                        options = SuperResolutionType.entries,
                        getDisplayName = { item, ctx -> item.displayName(ctx) },
                        value = superResolutionType,
                        onValueChange = {
                            superResolutionType = it
                            Prefs.superResolutionType = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "hwdec",
                        value = hardwareDecodeMode,
                        options = MpvTvHardwareDecodeOptions,
                        onValueChange = {
                            hardwareDecodeMode = it
                            Prefs.tvMpvHardwareDecodeMode = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "hwdec-codecs",
                        value = mpvHardwareDecodeCodecs,
                        options = MpvHardwareDecodeCodecsOptions,
                        onValueChange = {
                            mpvHardwareDecodeCodecs = it
                            Prefs.tvMpvHardwareDecodeCodecs = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "vo",
                        value = mpvVideoOutput,
                        options = MpvVideoOutputOptions,
                        onValueChange = {
                            mpvVideoOutput = it
                            Prefs.tvMpvVideoOutput = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "gpu-context",
                        value = mpvGpuContext,
                        options = MpvGpuContextOptions,
                        onValueChange = {
                            mpvGpuContext = it
                            Prefs.tvMpvGpuContext = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "gpu-api",
                        value = mpvGpuApi,
                        options = MpvGpuApiOptions,
                        onValueChange = {
                            mpvGpuApi = it
                            Prefs.tvMpvGpuApi = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "vd-queue-enable",
                        value = mpvVdQueueEnable,
                        options = MpvVdQueueEnableOptions,
                        onValueChange = {
                            mpvVdQueueEnable = it
                            Prefs.tvMpvVdQueueEnable = it
                        }
                    )
                }
                item {
                    MpvTextItem(
                        title = "cache",
                        value = mpvCache,
                        defaultValue = "yes",
                        defaultValueText = "默认 yes",
                        onValueChange = {
                            mpvCache = it
                            Prefs.tvMpvCache = it
                        }
                    )
                }
                item {
                    MpvTextItem(
                        title = "demuxer-max-bytes",
                        value = mpvDemuxerMaxBytes,
                        defaultValue = "150MiB",
                        defaultValueText = "默认 150MiB",
                        onValueChange = {
                            mpvDemuxerMaxBytes = it
                            Prefs.tvMpvDemuxerMaxBytes = it
                        }
                    )
                }
                item {
                    MpvTextItem(
                        title = "demuxer-max-back-bytes",
                        value = mpvDemuxerMaxBackBytes,
                        defaultValue = "50MiB",
                        defaultValueText = "默认 50MiB",
                        onValueChange = {
                            mpvDemuxerMaxBackBytes = it
                            Prefs.tvMpvDemuxerMaxBackBytes = it
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MpvSelectItem(
    title: String,
    value: String,
    options: LinkedHashMap<String, String>,
    onValueChange: (String) -> Unit
) {
    SettingListItemWithDialog(
        title = title,
        supportText = "MPV --$title，重进播放器后生效",
        options = options.keys.toList(),
        getDisplayName = { item, _ -> options[item] ?: item.ifBlank { "默认" } },
        value = value,
        onValueChange = onValueChange
    )
}

@Composable
private fun MpvTextItem(
    title: String,
    value: String,
    defaultValue: String,
    defaultValueText: String,
    onValueChange: (String) -> Unit
) {
    SettingTextListItem(
        title = title,
        supportText = "MPV --$title，重进播放器后生效",
        value = value,
        emptyValueText = defaultValueText,
        placeholder = defaultValueText.removePrefix("默认 "),
        transformValue = { value -> value.trim().replace("\n", "").ifBlank { defaultValue } },
        onValueChange = onValueChange
    )
}

private val MpvTvHardwareDecodeOptions = linkedMapOf(
    "no" to "no（软解）",
    "mediacodec" to "mediacodec",
    "mediacodec-copy" to "mediacodec-copy",
    "auto-safe" to "auto-safe",
    "auto" to "auto",
    "auto-copy" to "auto-copy",
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
