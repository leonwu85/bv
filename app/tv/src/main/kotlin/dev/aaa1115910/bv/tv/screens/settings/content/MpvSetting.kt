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
import dev.aaa1115910.bv.tv.component.settings.SettingListItemWithDialog
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.tv.util.TvMpvOptions
import dev.aaa1115910.bv.tv.util.TvMpvOptions.coerceForTv
import dev.aaa1115910.bv.util.Prefs

/**
 * TV 端 MPV 参数。所有项都只能从固定选项里选：mpv-android 这个构建没有 Vulkan/rkmpp，
 * 自由填写只会得到初始化报错；存量的自由文本值在读取时被归一化为“默认”。
 */
@Composable
fun MpvSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var superResolutionType by remember {
        mutableStateOf(
            Prefs.superResolutionType.let { stored ->
                val coerced = stored.coerceForTv()
                if (coerced != stored) Prefs.superResolutionType = coerced
                coerced
            }
        )
    }
    var hardwareDecodeMode by remember {
        mutableStateOf(normalizeChoice(Prefs.tvMpvHardwareDecodeMode, MpvTvHardwareDecodeOptions) { Prefs.tvMpvHardwareDecodeMode = it })
    }
    var mpvHardwareDecodeCodecs by remember {
        mutableStateOf(normalizeChoice(Prefs.tvMpvHardwareDecodeCodecs, MpvHardwareDecodeCodecsOptions) { Prefs.tvMpvHardwareDecodeCodecs = it })
    }
    var mpvVideoOutput by remember {
        mutableStateOf(normalizeChoice(Prefs.tvMpvVideoOutput, MpvVideoOutputOptions) { Prefs.tvMpvVideoOutput = it })
    }
    var mpvCache by remember {
        mutableStateOf(normalizeChoice(Prefs.tvMpvCache, MpvCacheOptions) { Prefs.tvMpvCache = it })
    }
    var mpvDemuxerMaxBytes by remember {
        mutableStateOf(normalizeChoice(Prefs.tvMpvDemuxerMaxBytes, MpvDemuxerMaxBytesOptions) { Prefs.tvMpvDemuxerMaxBytes = it })
    }
    var mpvDemuxerMaxBackBytes by remember {
        mutableStateOf(normalizeChoice(Prefs.tvMpvDemuxerMaxBackBytes, MpvDemuxerMaxBackBytesOptions) { Prefs.tvMpvDemuxerMaxBackBytes = it })
    }
    var mpvVdQueueEnable by remember {
        mutableStateOf(normalizeChoice(Prefs.tvMpvVdQueueEnable, MpvVdQueueEnableOptions) { Prefs.tvMpvVdQueueEnable = it })
    }
    var mpvPreferHttpCdn by remember { mutableStateOf(Prefs.tvMpvPreferHttpCdn) }

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
                        supportText = "仅 MPV 内核生效。实时 GPU 超分只适合高性能盒子，低配设备开启会持续丢帧；" +
                            "mediacodec_embed 输出下无效。重进播放器后生效",
                        options = TvMpvOptions.superResolutionChoices,
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
                        supportText = "硬解方式。默认先尝试零拷贝直出（需要 Android 8+），失败再回退拷贝",
                        value = hardwareDecodeMode,
                        options = MpvTvHardwareDecodeOptions,
                        rowLabels = MpvTvHardwareDecodeRowLabels,
                        onValueChange = {
                            hardwareDecodeMode = it
                            Prefs.tvMpvHardwareDecodeMode = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "hwdec-codecs",
                        supportText = "允许硬解的编码；不在列表内的编码走软解",
                        value = mpvHardwareDecodeCodecs,
                        options = MpvHardwareDecodeCodecsOptions,
                        rowLabels = MpvHardwareDecodeCodecsRowLabels,
                        onValueChange = {
                            mpvHardwareDecodeCodecs = it
                            Prefs.tvMpvHardwareDecodeCodecs = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "vo",
                        supportText = "视频输出。gpu 兼容性最好；gpu-next 需要 GLES 3.0，失败时自动回退 gpu；" +
                            "mediacodec_embed 由解码器直出，可输出 HDR/杜比视界，但超分与画质处理无效",
                        value = mpvVideoOutput,
                        options = MpvVideoOutputOptions,
                        rowLabels = MpvVideoOutputRowLabels,
                        onValueChange = {
                            mpvVideoOutput = it
                            Prefs.tvMpvVideoOutput = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "vd-queue-enable",
                        supportText = "解码器输出队列，可缓解部分设备的解码抖动，代价是更高的内存占用",
                        value = mpvVdQueueEnable,
                        options = MpvVdQueueEnableOptions,
                        onValueChange = {
                            mpvVdQueueEnable = it
                            Prefs.tvMpvVdQueueEnable = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "cache",
                        supportText = "网络流缓存开关",
                        value = mpvCache,
                        options = MpvCacheOptions,
                        onValueChange = {
                            mpvCache = it
                            Prefs.tvMpvCache = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "demuxer-max-bytes",
                        supportText = "前向缓存上限。自动 = 按设备内存 16/32/64 MiB，直播减半；音视频分离时实际占用约为两倍",
                        value = mpvDemuxerMaxBytes,
                        options = MpvDemuxerMaxBytesOptions,
                        onValueChange = {
                            mpvDemuxerMaxBytes = it
                            Prefs.tvMpvDemuxerMaxBytes = it
                        }
                    )
                }
                item {
                    MpvSelectItem(
                        title = "demuxer-max-back-bytes",
                        supportText = "后向缓存上限（快速回退用）。自动 = 前向的一半，直播 4 MiB",
                        value = mpvDemuxerMaxBackBytes,
                        options = MpvDemuxerMaxBackBytesOptions,
                        onValueChange = {
                            mpvDemuxerMaxBackBytes = it
                            Prefs.tvMpvDemuxerMaxBackBytes = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = "CDN 使用 HTTP 直连",
                        supportText = "把 bilivideo/akamaized 的 HTTPS 播放地址改写为 HTTP。MPV 已使用系统根证书校验 HTTPS，" +
                            "仅在系统根证书过期/损坏导致 TLS 校验失败时打开；带签名的播放地址与请求头会明文传输，" +
                            "部分 PCDN 节点不提供 HTTP。重进播放器后生效",
                        checked = mpvPreferHttpCdn,
                        onCheckedChange = {
                            mpvPreferHttpCdn = it
                            Prefs.tvMpvPreferHttpCdn = it
                        }
                    )
                }
            }
        }
    }
}

/**
 * @param options 选项值 → 弹窗里的完整说明
 * @param rowLabels 选项值 → 列表行右侧的简短标签；缺省时用完整说明
 */
@Composable
private fun MpvSelectItem(
    title: String,
    supportText: String,
    value: String,
    options: LinkedHashMap<String, String>,
    onValueChange: (String) -> Unit,
    rowLabels: Map<String, String> = emptyMap()
) {
    SettingListItemWithDialog(
        title = title,
        supportText = "$supportText。MPV --$title，重进播放器后生效",
        options = options.keys.toList(),
        getDisplayName = { item, _ -> options[item] ?: item.ifBlank { "默认" } },
        getValueText = { item, _ -> rowLabels[item] ?: options[item] ?: item.ifBlank { "默认" } },
        value = value,
        onValueChange = onValueChange
    )
}

/** 存量的自由文本或已下线的选项值一律回到“默认”（首个选项），并写回偏好 */
private fun normalizeChoice(
    stored: String,
    options: LinkedHashMap<String, String>,
    persist: (String) -> Unit
): String {
    val trimmed = stored.trim()
    if (trimmed in options.keys) return trimmed
    val fallback = options.keys.first()
    persist(fallback)
    return fallback
}

// mpv-android 的 ffmpeg 以 --disable-vulkan 构建且没有 rkmpp，Android 上 auto/auto-safe/auto-copy 也只是 mediacodec 的别名，
// 因此只保留真正有区别的四种。
private val MpvTvHardwareDecodeOptions = linkedMapOf(
    "mediacodec,mediacodec-copy" to "mediacodec,mediacodec-copy（默认，直出失败时回退拷贝）",
    "mediacodec" to "mediacodec（仅零拷贝直出）",
    "mediacodec-copy" to "mediacodec-copy（仅拷贝）",
    "no" to "no（软解）"
)

/** 行内标签：完整取值太长会把标题挤成竖排 */
private val MpvTvHardwareDecodeRowLabels = mapOf(
    "mediacodec,mediacodec-copy" to "直出，失败回退拷贝（默认）",
    "mediacodec" to "仅直出",
    "mediacodec-copy" to "仅拷贝",
    "no" to "软解"
)

private val MpvHardwareDecodeCodecsOptions = linkedMapOf(
    "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1" to "默认：h264, hevc, mpeg4, mpeg2video, vp8, vp9, av1",
    "h264,hevc" to "h264, hevc",
    "h264,hevc,vp9,av1" to "h264, hevc, vp9, av1",
    "all" to "all"
)

private val MpvHardwareDecodeCodecsRowLabels = mapOf(
    "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1" to "全部常见编码（默认）",
    "all" to "全部"
)

private val MpvVideoOutputOptions = linkedMapOf(
    "gpu" to "gpu（默认）",
    "gpu-next" to "gpu-next（失败自动回退 gpu）",
    "mediacodec_embed" to "mediacodec_embed（直通，支持 HDR，超分无效）"
)

private val MpvVideoOutputRowLabels = mapOf(
    "gpu-next" to "gpu-next",
    "mediacodec_embed" to "mediacodec_embed（直通）"
)

private val MpvVdQueueEnableOptions = linkedMapOf(
    "" to "默认",
    "yes" to "yes",
    "no" to "no"
)

private val MpvCacheOptions = linkedMapOf(
    "yes" to "yes（默认）",
    "auto" to "auto",
    "no" to "no"
)

private val MpvDemuxerMaxBytesOptions = linkedMapOf(
    "" to "自动（按设备内存）",
    "16MiB" to "16 MiB",
    "32MiB" to "32 MiB",
    "64MiB" to "64 MiB",
    "128MiB" to "128 MiB",
    "256MiB" to "256 MiB"
)

private val MpvDemuxerMaxBackBytesOptions = linkedMapOf(
    "" to "自动（前向的一半）",
    "4MiB" to "4 MiB",
    "8MiB" to "8 MiB",
    "16MiB" to "16 MiB",
    "32MiB" to "32 MiB",
    "64MiB" to "64 MiB"
)
