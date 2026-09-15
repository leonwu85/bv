package dev.aaa1115910.bv.mobile.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.viewmodel.user.FavoriteOrder
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    windowSize: WindowSizeClass,
    favoriteViewModel: FavoriteViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyGridState()
    var pendingRemove by remember { mutableStateOf<VideoCardData?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var showEditFolder by remember { mutableStateOf(false) }
    var showDeleteFolder by remember { mutableStateOf(false) }
    var showCleanFolder by remember { mutableStateOf(false) }

    val currentTabIndex by remember {
        derivedStateOf {
            favoriteViewModel.favoriteFolderMetadataList
                .indexOf(favoriteViewModel.currentFavoriteFolderMetadata)
                .coerceAtLeast(0)
        }
    }

    if (favoriteViewModel.favoriteFolderMetadataList.isNotEmpty()) {
        listState.OnBottomReached(loading = favoriteViewModel.updatingFolderItems) {
            favoriteViewModel.updateFolderItems()
        }
    }

    BackHandler(enabled = favoriteViewModel.selectionMode) { favoriteViewModel.toggleSelectionMode() }
    FavoriteContent(
        modifier = modifier,
        listState = listState,
        windowSize = windowSize,
        selectedTabIndex = currentTabIndex,
        currentFolder = favoriteViewModel.currentFavoriteFolderMetadata,
        favoriteFolders = favoriteViewModel.favoriteFolderMetadataList,
        favorites = favoriteViewModel.favorites,
        searchQuery = favoriteViewModel.searchQuery,
        selectedOrder = favoriteViewModel.selectedOrder,
        loading = favoriteViewModel.updatingFolders || favoriteViewModel.updatingFolderItems,
        operating = favoriteViewModel.operating,
        onSearchQueryChange = favoriteViewModel::updateSearchQuery,
        onOrderChange = favoriteViewModel::selectOrder,
        batchControls = { FavoriteTransferControls(favoriteViewModel) },
        selectionMode = favoriteViewModel.selectionMode,
        selectedIds = favoriteViewModel.selectedIds,
        onToggleSelection = favoriteViewModel::toggleSelectionMode,
        onSelectAllLoaded = favoriteViewModel::selectAllLoaded,
        onClickTab = favoriteViewModel::selectFolder,
        onClickVideo = { video ->
            if (favoriteViewModel.selectionMode) favoriteViewModel.toggleSelected(video.avid)
            else if (!favoriteViewModel.operating) VideoPlayerActivity.actionStart(context, video = video)
        },
        onRemoveVideo = { pendingRemove = it },
        onAddFolder = { showCreateFolder = true },
        onEditFolder = { showEditFolder = true },
        onCleanFolder = { showCleanFolder = true },
        onDeleteFolder = { showDeleteFolder = true },
        onBack = {
            if (favoriteViewModel.selectionMode) favoriteViewModel.toggleSelectionMode()
            else (context as Activity).finish()
        }
    )

    pendingRemove?.let { video ->
        ConfirmActionDialog(
            title = "移出收藏夹",
            text = "确定将“${video.title}”从当前收藏夹移除吗？",
            onConfirm = {
                pendingRemove = null
                favoriteViewModel.removeFavorite(video)
            },
            onDismiss = { pendingRemove = null }
        )
    }
    if (showCreateFolder) {
        FavoriteFolderEditorDialog(
            title = "新建收藏夹",
            initialTitle = "",
            initialPublic = true,
            onConfirm = { title, isPublic ->
                showCreateFolder = false
                favoriteViewModel.addFolder(title, isPublic)
            },
            onDismiss = { showCreateFolder = false }
        )
    }
    if (showEditFolder) {
        val folder = favoriteViewModel.currentFavoriteFolderMetadata
        FavoriteFolderEditorDialog(
            title = "编辑收藏夹",
            initialTitle = folder?.title.orEmpty(),
            initialPublic = folder?.isPublic ?: true,
            onConfirm = { title, isPublic ->
                showEditFolder = false
                favoriteViewModel.editCurrentFolder(title, isPublic)
            },
            onDismiss = { showEditFolder = false }
        )
    }
    if (showDeleteFolder) {
        ConfirmActionDialog(
            title = "删除收藏夹",
            text = "确定删除“${favoriteViewModel.currentFavoriteFolderMetadata?.title.orEmpty()}”吗？收藏夹内的视频不会被删除。",
            onConfirm = {
                showDeleteFolder = false
                favoriteViewModel.deleteCurrentFolder()
            },
            onDismiss = { showDeleteFolder = false }
        )
    }
    if (showCleanFolder) {
        ConfirmActionDialog(
            title = "清理失效内容",
            text = "将从当前收藏夹移除所有已失效内容，是否继续？",
            onConfirm = {
                showCleanFolder = false
                favoriteViewModel.cleanCurrentFolder()
            },
            onDismiss = { showCleanFolder = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteContent(
    modifier: Modifier = Modifier,
    listState: LazyGridState,
    windowSize: WindowSizeClass,
    selectedTabIndex: Int,
    currentFolder: FavoriteFolderMetadata?,
    favoriteFolders: List<FavoriteFolderMetadata>,
    favorites: List<VideoCardData>,
    searchQuery: String,
    selectedOrder: FavoriteOrder,
    loading: Boolean,
    operating: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onOrderChange: (FavoriteOrder) -> Unit,
    onClickTab: (FavoriteFolderMetadata) -> Unit,
    onClickVideo: (VideoCardData) -> Unit,
    onRemoveVideo: (VideoCardData) -> Unit,
    onAddFolder: () -> Unit,
    onEditFolder: () -> Unit,
    onCleanFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
    onBack: () -> Unit,
    batchControls: @Composable () -> Unit = {},
    selectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onToggleSelection: () -> Unit = {},
    onSelectAllLoaded: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showSearch by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }

    fun closeSearch() {
        showSearch = false
        onSearchQueryChange("")
    }

    BackHandler(enabled = showSearch && !selectionMode, onBack = ::closeSearch)
    val allLoadedSelected = favorites.isNotEmpty() && favorites.all { it.avid in selectedIds }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(if (selectionMode) "已选 ${selectedIds.size} 项" else stringResource(R.string.title_mobile_activity_favorite),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { if (showSearch && !selectionMode) closeSearch() else onBack() },
                            enabled = !selectionMode || !operating
                        ) {
                            Icon(if (selectionMode) Icons.Rounded.Close else Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = if (selectionMode) "完成选择" else "返回")
                        }
                    },
                    actions = {
                        if (selectionMode) {
                            TextButton(onClick = onSelectAllLoaded, enabled = !operating && favorites.isNotEmpty()) {
                                Text(if (allLoadedSelected) "取消全选" else "全选已加载")
                            }
                        } else {
                            IconButton(onClick = { if (showSearch) closeSearch() else showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索收藏")
                            }
                            TextButton(onClick = onToggleSelection, enabled = !operating && favorites.isNotEmpty()) {
                                Text("管理")
                            }
                            Box {
                                IconButton(
                                    onClick = { showFolderMenu = true },
                                    enabled = !operating
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "收藏夹管理")
                                }
                                DropdownMenu(
                                    expanded = showFolderMenu,
                                    onDismissRequest = { showFolderMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("新建收藏夹") },
                                        leadingIcon = { Icon(Icons.Default.Add, null) },
                                        onClick = { showFolderMenu = false; onAddFolder() }
                                    )
                                    HorizontalDivider()
                                    FavoriteOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = { Text(order.displayName()) },
                                            trailingIcon = { if (order == selectedOrder) Icon(Icons.Rounded.Check, null) },
                                            onClick = { showFolderMenu = false; onOrderChange(order) },
                                            enabled = currentFolder != null
                                        )
                                    }
                                    if (currentFolder != null) HorizontalDivider()
                                    if (currentFolder?.isDefault == false) {
                                        DropdownMenuItem(
                                            text = { Text("编辑收藏夹") },
                                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                                            onClick = {
                                                showFolderMenu = false
                                                onEditFolder()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("清理失效内容") },
                                        enabled = currentFolder != null,
                                        onClick = {
                                            showFolderMenu = false
                                            onCleanFolder()
                                        }
                                    )
                                    if (currentFolder?.isDefault == false) {
                                        DropdownMenuItem(
                                            text = { Text("删除收藏夹") },
                                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                                            onClick = {
                                                showFolderMenu = false
                                                onDeleteFolder()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior.takeUnless { selectionMode }
                )

                if (showSearch && !selectionMode) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        label = { Text("搜索当前收藏夹") },
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                }

                if (favoriteFolders.isNotEmpty()) {
                    PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex, divider = { }) {
                        favoriteFolders.forEachIndexed { index, folder ->
                            Tab(
                                selected = selectedTabIndex == index,
                                enabled = !operating && !selectionMode,
                                onClick = { onClickTab(folder) }
                            ) {
                                Box(
                                    modifier = Modifier.height(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        text = "${folder.title} ${folder.mediaCount}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        bottomBar = batchControls
    ) { innerPadding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (loading) CircularProgressIndicator()
                else Text(if (searchQuery.isBlank()) stringResource(R.string.no_data) else "没有匹配的收藏")
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier.padding(innerPadding),
                state = listState,
                columns = if (windowSize.widthSizeClass == WindowWidthSizeClass.Compact) {
                    GridCells.Fixed(2)
                } else {
                    GridCells.Adaptive(220.dp)
                },
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(favorites, key = { _, item -> item.avid }) { _, data ->
                    Box {
                        SmallVideoCard(
                            modifier = Modifier
                                .then(if (data.avid in selectedIds) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
                                .semantics {
                                    if (selectionMode) {
                                        role = Role.Checkbox
                                        toggleableState = if (data.avid in selectedIds) ToggleableState.On else ToggleableState.Off
                                        stateDescription = if (data.avid in selectedIds) "已选择" else "未选择"
                                    }
                                },
                            data = data,
                            showMoreMenu = !selectionMode,
                            enableUpNavigation = !selectionMode,
                            onClick = { onClickVideo(data) },
                            managementActionLabel = if (selectionMode) null else "移出当前收藏夹",
                            onManagementAction = if (selectionMode) null else ({ if (!operating) onRemoveVideo(data) })
                        )
                        if (selectionMode) {
                            Checkbox(
                                checked = data.avid in selectedIds,
                                onCheckedChange = null,
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                    .size(24.dp).clearAndSetSemantics { },
                                colors = CheckboxDefaults.colors(uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteFolderEditorDialog(
    title: String,
    initialTitle: String,
    initialPublic: Boolean,
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var folderTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var isPublic by remember(initialPublic) { mutableStateOf(initialPublic) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = folderTitle,
                    onValueChange = { folderTitle = it.take(20) },
                    label = { Text("名称") },
                    supportingText = { Text("${folderTitle.length}/20") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("公开收藏夹")
                        Text(
                            "关闭后仅自己可见",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = folderTitle.isNotBlank(),
                onClick = { onConfirm(folderTitle.trim(), isPublic) }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun ConfirmActionDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun FavoriteOrder.displayName(): String = when (this) {
    FavoriteOrder.FavoriteTime -> "最近收藏"
    FavoriteOrder.MostPlayed -> "最多播放"
    FavoriteOrder.PublishTime -> "最新投稿"
}
