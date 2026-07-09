package dev.aaa1115910.bv.tv.component.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.util.PlaybackPreferenceSelector

/** 优先级 UI 使用实际编码标识（如 hev1/hvc1/dvh1），避免显示「H.265」这类泛称。 */
private fun VideoCodec.codecLabel(): String = prefix

/**
 * 仅用于 H.265 变体（hvc1 / hev1）优先级配置。
 * dvh1 不在此配置，仅在杜比视界画质下自动启用并最优选。
 */
@Composable
fun SettingH265CodecPriorityListItem(
    modifier: Modifier = Modifier,
    title: String,
    supportText: String,
    value: List<VideoCodec>,
    onValueChange: (List<VideoCodec>) -> Unit,
    defaultHasFocus: Boolean = false,
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayPriority = remember(value) {
        PlaybackPreferenceSelector.normalizeH265CodecPriority(value)
    }
    val valueText = remember(displayPriority) {
        displayPriority.joinToString(" > ") { it.codecLabel() }
    }

    SettingListItem(
        modifier = modifier,
        title = title,
        supportText = supportText,
        defaultHasFocus = defaultHasFocus,
        valueText = valueText,
        onClick = { showDialog = true }
    )

    H265CodecPriorityDialog(
        show = showDialog,
        title = title,
        priority = displayPriority,
        onHideDialog = { showDialog = false },
        onConfirm = { nextPriority ->
            onValueChange(nextPriority)
            showDialog = false
        }
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun H265CodecPriorityDialog(
    show: Boolean,
    title: String,
    priority: List<VideoCodec>,
    onHideDialog: () -> Unit,
    onConfirm: (List<VideoCodec>) -> Unit,
) {
    if (!show) return

    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.55).dp
    var editingPriority by remember(priority) {
        mutableStateOf(PlaybackPreferenceSelector.normalizeH265CodecPriority(priority))
    }

    fun move(index: Int, offset: Int) {
        val target = index + offset
        if (index !in editingPriority.indices || target !in editingPriority.indices) return
        editingPriority = editingPriority.toMutableList().also {
            val item = it.removeAt(index)
            it.add(target, item)
        }
    }

    TvAlertDialog(
        onDismissRequest = onHideDialog,
        title = { Text(text = title) },
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "仅影响默认编码为 H.265 时的自动选择，越靠前优先级越高。dvh1 仅在杜比视界画质下启用并最优选。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                editingPriority.forEachIndexed { index, codec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. ${codec.codecLabel()}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { move(index, -1) },
                                enabled = index > 0
                            ) {
                                Text("上移")
                            }
                            Button(
                                onClick = { move(index, 1) },
                                enabled = index < editingPriority.lastIndex
                            ) {
                                Text("下移")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                Button(
                    onClick = {
                        editingPriority = PlaybackPreferenceSelector.defaultH265CodecPriority()
                    }
                ) {
                    Text("恢复默认")
                }
                Button(onClick = onHideDialog) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        onConfirm(
                            PlaybackPreferenceSelector.normalizeH265CodecPriority(editingPriority)
                        )
                    }
                ) {
                    Text("确定")
                }
            }
        }
    )
}
