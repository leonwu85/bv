package dev.aaa1115910.bv.mobile.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.FavoriteTransferMode
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel

@Composable
internal fun FavoriteTransferControls(viewModel: FavoriteViewModel) {
    if (!viewModel.selectionMode) return
    var pendingMode by rememberSaveable { mutableStateOf<FavoriteTransferMode?>(null) }
    val canTransfer = !viewModel.operating && !viewModel.updatingFolders && viewModel.selectedIds.isNotEmpty()

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            if (viewModel.operating) LinearProgressIndicator(Modifier.fillMaxWidth())
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when {
                        viewModel.operating -> "正在处理，请稍候…"
                        viewModel.selectedIds.isEmpty() -> "轻点视频选择，可继续向下加载"
                        else -> "已加载 ${viewModel.favorites.size} 项，可跨页选择"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                viewModel.transferError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        onClick = { pendingMode = FavoriteTransferMode.Copy }, enabled = canTransfer
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("复制到")
                    }
                    Button(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        onClick = { pendingMode = FavoriteTransferMode.Move }, enabled = canTransfer
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.DriveFileMove, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("移动到")
                    }
                }
            }
        }
    }
    pendingMode?.let { mode ->
        FavoriteTransferSheet(
            mode = mode,
            selectedCount = viewModel.selectedIds.size,
            targets = viewModel.transferTargets,
            onDismiss = { pendingMode = null },
            onConfirm = { targetId ->
                viewModel.transferSelected(targetId, mode)
                pendingMode = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoriteTransferSheet(
    mode: FavoriteTransferMode,
    selectedCount: Int,
    targets: List<FavoriteFolderMetadata>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var targetId by rememberSaveable(mode) { mutableStateOf<Long?>(null) }
    val action = if (mode == FavoriteTransferMode.Copy) "复制" else "移动"
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            Column(
                Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${action}到收藏夹", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "已选择 $selectedCount 项 · " + if (mode == FavoriteTransferMode.Copy) "保留原收藏" else "从当前收藏夹移出",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (targets.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f, fill = false).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Folder, null, Modifier.size(40.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("还没有其他收藏夹", style = MaterialTheme.typography.titleMedium)
                    Text("返回收藏页，在右上角菜单中新建收藏夹后重试。",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f, fill = false).selectableGroup(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(targets, key = { it.id }) { folder ->
                        val selected = targetId == folder.id
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { targetId = folder.id })
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Folder, null, Modifier.size(24.dp),
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(folder.title, style = MaterialTheme.typography.titleSmall, maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    Text("${folder.mediaCount} 个视频 · ${if (folder.isPublic) "公开" else "私密"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(selected = selected, onClick = null)
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("取消") }
                Button(
                    onClick = { targetId?.let(onConfirm) },
                    enabled = selectedCount > 0 && targets.any { it.id == targetId },
                    modifier = Modifier.weight(2f).heightIn(min = 48.dp)
                ) { Text("确认$action") }
            }
        }
    }
}
