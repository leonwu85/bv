package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.tv.component.settings.SettingsMenuSelectItem
import dev.aaa1115910.bv.util.Prefs

@Composable
fun OfflineAudioDialog(onDismiss: () -> Unit, onConfirm: (Audio) -> Unit) {
    var selected by remember { mutableStateOf(Prefs.offlineAudio) }
    val context = LocalContext.current
    TvAlertDialog(onDismissRequest = onDismiss, title = { Text("缓存音质") },
        text = { LazyColumn(Modifier.heightIn(max = 300.dp)) {
            item { Text("音轨不可用时自动降级；不改变在线播放音质。") }
            Audio.entries.sortedBy { it.rank }.forEach { audio ->
                item { SettingsMenuSelectItem(text = audio.getDisplayName(context), selected = selected == audio, onClick = { selected = audio }) }
            }
        } },
        confirmButton = { Button(onClick = { onConfirm(selected) }) { Text("确定") } },
        dismissButton = { Button(onClick = onDismiss) { Text("取消") } })
}
