package dev.aaa1115910.bv.tv.screens.user

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import dev.aaa1115910.biliapi.entity.FavoriteTransferMode
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.TvCollectionToolbar
import dev.aaa1115910.bv.tv.component.TvToolbarAction
import dev.aaa1115910.bv.tv.component.tvToolbarNavigation
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel

@Composable
internal fun FavoriteTransferControls(
    viewModel: FavoriteViewModel,
    focusRequester: FocusRequester,
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    var pendingMode by remember { mutableStateOf<FavoriteTransferMode?>(null) }
    var targetId by remember { mutableStateOf<Long?>(null) }
    val manageAction = remember { FocusRequester() }
    BackHandler(enabled = viewModel.selectionMode && pendingMode == null) {
        // Focus a surviving action before removing the selection-only buttons.
        manageAction.requestFocus()
        viewModel.toggleSelectionMode()
    }
    Column(Modifier.fillMaxWidth()) {
        TvCollectionToolbar(
            title = if (viewModel.selectionMode) "已选 ${viewModel.selectedIds.size} 项" else "收藏管理",
            subtitle = when {
                viewModel.operating -> "正在处理，请稍候…"
                viewModel.selectionMode -> "确定键选择 · 返回键退出"
                viewModel.updatingFolderItems -> "正在加载…"
                else -> "已加载 ${viewModel.favorites.size} 个视频"
            },
            modifier = Modifier.focusRequester(focusRequester)
                .focusRestorer(manageAction)
                .tvToolbarNavigation(onUp, onDown)
        ) {
            if (viewModel.selectionMode) {
                TvToolbarAction(
                    if (viewModel.favorites.isNotEmpty() && viewModel.selectedIds.size == viewModel.favorites.size) "取消全选" else "全选已加载",
                    icon = Icons.Rounded.Checklist, onClick = viewModel::selectAllLoaded,
                    available = !viewModel.operating
                )
                FavoriteTransferMode.entries.forEach { mode ->
                    TvToolbarAction(
                        text = if (mode == FavoriteTransferMode.Copy) "复制到" else "移动到",
                        icon = if (mode == FavoriteTransferMode.Copy) Icons.Rounded.ContentCopy else Icons.AutoMirrored.Rounded.DriveFileMove,
                        onClick = { pendingMode = mode; targetId = null },
                        available = !viewModel.operating && viewModel.selectedIds.isNotEmpty()
                    )
                }
            }
            TvToolbarAction(
                text = if (viewModel.selectionMode) "完成" else "批量管理",
                icon = if (viewModel.selectionMode) Icons.Rounded.Done else Icons.Rounded.Checklist,
                modifier = Modifier.focusRequester(manageAction),
                onClick = viewModel::toggleSelectionMode,
                available = !viewModel.operating && !viewModel.updatingFolders && viewModel.favorites.isNotEmpty()
            )
        }
        viewModel.transferError?.let {
            Text(it, modifier = Modifier.padding(start = 14.dp, top = 6.dp), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    pendingMode?.let { mode ->
        val targets = viewModel.transferTargets
        val folderFocus = remember(targets.map { it.id }) { targets.map { FocusRequester() } }
        val confirmFocus = remember { FocusRequester() }
        val cancelFocus = remember { FocusRequester() }
        LaunchedEffect(mode) {
            withFrameNanos { }
            (folderFocus.firstOrNull() ?: cancelFocus).requestFocus()
        }
        TvAlertDialog(
            title = { Text("${if (mode == FavoriteTransferMode.Copy) "复制" else "移动"} ${viewModel.selectedIds.size} 项收藏") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("选择目标收藏夹", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (targets.isEmpty()) Text("暂无其他可用收藏夹，请先创建收藏夹。")
                    else LazyColumn(Modifier.heightIn(max = 280.dp).focusGroup(),
                        contentPadding = PaddingValues(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(targets, key = { _, folder -> folder.id }) { index, folder ->
                            TvToolbarAction(
                                text = folder.title,
                                icon = if (folder.id == targetId) Icons.Rounded.Done else Icons.Rounded.Folder,
                                selected = folder.id == targetId,
                                onClick = { targetId = folder.id },
                                modifier = Modifier.fillMaxWidth().focusRequester(folderFocus[index])
                                    .focusProperties {
                                        if (index == 0) up = FocusRequester.Cancel
                                        if (index == targets.lastIndex) down = confirmFocus
                                    }
                            )
                        }
                    }
                }
            },
            onDismissRequest = { pendingMode = null },
            confirmButton = {
                TvToolbarAction("确认${if (mode == FavoriteTransferMode.Copy) "复制" else "移动"}",
                    modifier = Modifier.focusRequester(confirmFocus).focusProperties {
                        up = folderFocus.lastOrNull() ?: FocusRequester.Cancel
                        left = cancelFocus
                    },
                    onClick = {
                        targetId?.let { viewModel.transferSelected(it, mode) }
                        pendingMode = null
                    }, available = targetId != null && targets.any { it.id == targetId })
            },
            dismissButton = {
                TvToolbarAction("取消", modifier = Modifier.focusRequester(cancelFocus).focusProperties {
                    up = folderFocus.lastOrNull() ?: FocusRequester.Cancel
                    right = confirmFocus
                }, onClick = { pendingMode = null })
            }
        )
    }
}
