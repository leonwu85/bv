package dev.aaa1115910.bv.mobile.screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.repositories.ToViewCleanType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.viewmodel.user.ToViewSort
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToViewScreen(
    modifier: Modifier = Modifier,
    windowSize: WindowSizeClass,
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCleanMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Pair<VideoCardData, Int>?>(null) }
    var pendingClean by remember { mutableStateOf<ToViewCleanType?>(null) }
    val videos = toViewViewModel.visibleHistories

    fun closeSearch() {
        showSearch = false
        toViewViewModel.updateSearchQuery("")
    }

    BackHandler(enabled = showSearch, onBack = ::closeSearch)

    LaunchedEffect(Unit) {
        if (toViewViewModel.histories.isEmpty() && !toViewViewModel.noMore) {
            toViewViewModel.update()
        }
    }

    listState.OnBottomReached(
        loading = toViewViewModel.updating || toViewViewModel.noMore,
        enabled = videos.isNotEmpty()
    ) {
        toViewViewModel.update()
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_mobile_activity_toview)) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (showSearch) closeSearch() else (context as Activity).finish()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { if (showSearch) closeSearch() else showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索稍后再看")
                        }
                        IconButton(
                            enabled = videos.isNotEmpty(),
                            onClick = {
                                videos.firstOrNull()?.let {
                                    VideoPlayerActivity.actionStart(
                                        context = context,
                                        video = it,
                                        fromToView = true
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "播放当前列表")
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                ToViewSort.entries.forEach { sort ->
                                    DropdownMenuItem(
                                        text = { Text(sort.displayName()) },
                                        trailingIcon = {
                                            if (sort == toViewViewModel.selectedSort) Text("✓")
                                        },
                                        onClick = {
                                            showSortMenu = false
                                            toViewViewModel.selectSort(sort)
                                        }
                                    )
                                }
                            }
                        }
                        Box {
                            IconButton(
                                enabled = !toViewViewModel.cleaning,
                                onClick = { showCleanMenu = true }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "稍后再看管理")
                            }
                            DropdownMenu(
                                expanded = showCleanMenu,
                                onDismissRequest = { showCleanMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("清理失效内容") },
                                    onClick = {
                                        showCleanMenu = false
                                        pendingClean = ToViewCleanType.Invalid
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("清理已观看内容") },
                                    onClick = {
                                        showCleanMenu = false
                                        pendingClean = ToViewCleanType.Viewed
                                    }
                                )
                                DropdownMenuItem(
                                    enabled = toViewViewModel.histories.isNotEmpty(),
                                    text = { Text("清空稍后再看") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                                    onClick = {
                                        showCleanMenu = false
                                        pendingClean = ToViewCleanType.All
                                    }
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
                if (showSearch) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        value = toViewViewModel.searchQuery,
                        onValueChange = toViewViewModel::updateSearchQuery,
                        singleLine = true,
                        label = { Text("搜索标题、UP 主或 BV 号") },
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (toViewViewModel.updating && !toViewViewModel.noMore) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        if (toViewViewModel.searchQuery.isBlank()) stringResource(R.string.no_data)
                        else "没有匹配的稍后再看内容",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                columns = GridCells.Adaptive(
                    if (windowSize.widthSizeClass == WindowWidthSizeClass.Compact) 180.dp else 220.dp
                ),
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(videos, key = { _, video -> video.avid }) { index, video ->
                    SmallVideoCard(
                        data = video,
                        onClick = {
                            VideoPlayerActivity.actionStart(
                                context = context,
                                video = video,
                                fromToView = true
                            )
                        },
                        managementActionLabel = "从稍后再看移除",
                        onManagementAction = { pendingDelete = video to index }
                    )
                }
            }
        }
    }

    pendingDelete?.let { (video, index) ->
        ConfirmActionDialog(
            title = "移出稍后再看",
            text = "确定移除“${video.title}”吗？",
            onConfirm = {
                pendingDelete = null
                toViewViewModel.deleteToView(video.avid, index)
            },
            onDismiss = { pendingDelete = null }
        )
    }
    pendingClean?.let { type ->
        val (title, text) = when (type) {
            ToViewCleanType.All -> "清空稍后再看" to "全部内容将被移除且无法恢复，是否继续？"
            ToViewCleanType.Invalid -> "清理失效内容" to "所有已失效内容将被移除，是否继续？"
            ToViewCleanType.Viewed -> "清理已观看内容" to "所有已观看内容将被移除，是否继续？"
        }
        ConfirmActionDialog(
            title = title,
            text = text,
            onConfirm = {
                pendingClean = null
                toViewViewModel.clearToView(type)
            },
            onDismiss = { pendingClean = null }
        )
    }
}

private fun ToViewSort.displayName(): String = when (this) {
    ToViewSort.Default -> "默认顺序"
    ToViewSort.LatestPublish -> "最新投稿"
    ToViewSort.MostPlayed -> "最多播放"
    ToViewSort.Title -> "标题顺序"
}
