package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import kotlinx.coroutines.delay

@Composable
fun InteractiveOptionDialog(
    show: Boolean,
    options: List<VideoListInteractiveNode>,
    onSelectOption: (VideoListInteractiveNode) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!show || options.isEmpty()) return

    val defaultFocusRequester = remember(options) { FocusRequester() }

    LaunchedEffect(show, options.size) {
        if (show) {
            delay(120)
            defaultFocusRequester.requestFocus()
        }
    }

    TvAlertDialog(
        modifier = modifier,
        onDismissRequest = onExit,
        title = {
            Text(text = "互动选项")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "当前片段播放完成，请选择下一个互动分支",
                    style = MaterialTheme.typography.bodyMedium,
                )
                options.forEachIndexed { index, option ->
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (index == 0) Modifier.focusRequester(defaultFocusRequester)
                                else Modifier
                            ),
                        onClick = { onSelectOption(option) }
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 2.dp),
                            text = option.partTitle.ifBlank { "选项 ${index + 1}" },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onExit) {
                Text(text = "退出播放")
            }
        }
    )
}