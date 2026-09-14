package dev.aaa1115910.bv.mobile.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.user.SpaceVideoOrder
import dev.aaa1115910.bv.viewmodel.user.UserSpaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserSpaceVideoControls(
    viewModel: UserSpaceViewModel,
    onLoadPrevious: () -> Unit = { viewModel.loadPreviousVideos() }
) {
    var showLocate by rememberSaveable { mutableStateOf(false) }
    var target by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(SpaceVideoOrder.PubDate to "最新发布", SpaceVideoOrder.Click to "最多播放")
                .forEach { (order, title) ->
                    val selected = viewModel.videoOrder == order
                    FilterChip(
                        modifier = Modifier.heightIn(min = 48.dp),
                        selected = selected,
                        enabled = !viewModel.videoLoading,
                        onClick = { if (!selected) viewModel.changeVideoOrder() },
                        label = { Text(title) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            TextButton(
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = {
                    target = viewModel.fromViewAid.takeIf { it > 0 }?.let { "av$it" }.orEmpty()
                    showLocate = true
                },
                enabled = !viewModel.videoLoading
            ) {
                Icon(Icons.Rounded.MyLocation, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("定位视频")
            }
        }
        viewModel.locatingAid?.let { aid ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("已定位到 av$aid", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = viewModel::refreshSelectedTab, enabled = !viewModel.videoLoading) {
                        Text("回到开头")
                    }
                }
            }
        }
        if (viewModel.hasPreviousVideos) {
            TextButton(onClick = onLoadPrevious, enabled = !viewModel.videoLoading) {
                Icon(Icons.Rounded.ArrowUpward, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("加载前面的投稿")
            }
        }
        if (viewModel.videoLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        viewModel.videoError?.let { error ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onErrorContainer)
                    Text(error, Modifier.weight(1f).padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    TextButton(
                        onClick = { if (viewModel.retryLoadsPrevious) onLoadPrevious() else viewModel.retryVideos() },
                        enabled = !viewModel.videoLoading
                    ) { Text("重试", color = MaterialTheme.colorScheme.onErrorContainer) }
                }
            }
        }
    }
    if (showLocate) {
        ModalBottomSheet(
            onDismissRequest = { showLocate = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("定位投稿", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("输入视频编号或链接，跳转到它在这位 UP 投稿中的位置。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(), value = target, onValueChange = { target = it },
                    label = { Text("av 号、BV 号或视频链接") }, singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { showLocate = false }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text("取消")
                    }
                    Button(
                        onClick = { showLocate = false; viewModel.locateVideo(target.trim()) },
                        modifier = Modifier.weight(2f).heightIn(min = 48.dp), enabled = target.isNotBlank()
                    ) { Text("定位视频") }
                }
            }
        }
    }
}
