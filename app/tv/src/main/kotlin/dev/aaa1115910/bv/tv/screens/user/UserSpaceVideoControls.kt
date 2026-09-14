package dev.aaa1115910.bv.tv.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import dev.aaa1115910.biliapi.entity.user.SpaceVideoOrder
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.TvCollectionToolbar
import dev.aaa1115910.bv.tv.component.TvToolbarAction
import dev.aaa1115910.bv.tv.component.tvToolbarNavigation
import dev.aaa1115910.bv.viewmodel.user.UserSpaceViewModel

@Composable
internal fun UserSpaceVideoControls(
    viewModel: UserSpaceViewModel,
    focusRequester: FocusRequester,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onFocused: () -> Unit,
    onLoadPrevious: () -> Unit
) {
    var showLocate by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf("") }
    val firstAction = remember { FocusRequester() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 4.dp)) {
        TvCollectionToolbar(
            title = if (viewModel.locatingAid != null) "已定位投稿" else "全部投稿",
            subtitle = if (viewModel.videoLoading) "正在加载…" else "↓ 浏览投稿",
            compact = true,
            showContainer = false,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusRestorer(firstAction)
                .onFocusChanged { if (it.hasFocus) onFocused() }
                .tvToolbarNavigation(onUp, onDown)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SpaceVideoOrder.entries.forEach { order ->
                    TvToolbarAction(
                        text = if (order == SpaceVideoOrder.PubDate) "最新发布" else "最多播放",
                        modifier = if (order == SpaceVideoOrder.PubDate) Modifier.focusRequester(firstAction) else Modifier,
                        selected = viewModel.videoOrder == order,
                        available = !viewModel.videoLoading,
                        onClick = { if (viewModel.videoOrder != order) viewModel.changeVideoOrder() }
                    )
                }
            }
            TvToolbarAction("定位视频", icon = Icons.Rounded.MyLocation, onClick = {
                target = viewModel.fromViewAid.takeIf { it > 0 }?.let { "av$it" }.orEmpty()
                showLocate = true
            }, available = !viewModel.videoLoading)
            if (viewModel.locatingAid != null) {
                TvToolbarAction("回到开头", icon = Icons.Rounded.ArrowUpward,
                    onClick = viewModel::refreshSelectedTab, available = !viewModel.videoLoading)
            }
            if (viewModel.hasPreviousVideos) {
                TvToolbarAction("前面的投稿", onClick = onLoadPrevious, available = !viewModel.videoLoading)
            }
            if (viewModel.videoError != null) {
                TvToolbarAction("重试", icon = Icons.Rounded.Refresh,
                    onClick = { if (viewModel.retryLoadsPrevious) onLoadPrevious() else viewModel.retryVideos() },
                    available = !viewModel.videoLoading)
            }
        }
        viewModel.videoError?.let { error ->
            Text(error, modifier = Modifier.padding(start = 14.dp, top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (showLocate) {
        TvAlertDialog(
            onDismissRequest = { showLocate = false },
            title = { Text("定位该 UP 的投稿") },
            text = { OutlinedTextField(value = target, onValueChange = { target = it },
                label = { Text("av 号、BV 号或视频链接") }, singleLine = true) },
            confirmButton = { TvToolbarAction("定位", onClick = {
                showLocate = false
                viewModel.locateVideo(target)
            }, available = target.isNotBlank()) },
            dismissButton = { TvToolbarAction("取消", onClick = { showLocate = false }) }
        )
    }
}
