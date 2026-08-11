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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
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
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    windowSize: WindowSizeClass,
    historyViewModel: HistoryViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showSearch by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Pair<VideoCardData, Int>?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteViewedConfirm by remember { mutableStateOf(false) }
    val histories = historyViewModel.visibleHistories

    fun closeSearch() {
        showSearch = false
        historyViewModel.updateSearchQuery("")
    }

    BackHandler(enabled = showSearch, onBack = ::closeSearch)

    LaunchedEffect(Unit) {
        if (historyViewModel.histories.isEmpty() && !historyViewModel.noMore) {
            historyViewModel.update()
        }
        historyViewModel.refreshHistoryPaused()
    }

    listState.OnBottomReached(
        loading = historyViewModel.isLoading,
        enabled = histories.isNotEmpty()
    ) {
        historyViewModel.update()
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_mobile_activity_history)) },
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
                            Icon(Icons.Default.Search, contentDescription = "搜索观看历史")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "历史记录管理")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    enabled = !historyViewModel.managingHistory,
                                    text = {
                                        Text(
                                            if (historyViewModel.historyPaused == true) "恢复记录观看历史"
                                            else "暂停记录观看历史"
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (historyViewModel.historyPaused == true) Icons.Default.PlayArrow
                                            else Icons.Default.Pause,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        historyViewModel.setHistoryPaused(historyViewModel.historyPaused != true)
                                    }
                                )
                                DropdownMenuItem(
                                    enabled = historyViewModel.histories.any { it.historyFinished } &&
                                        !historyViewModel.managingHistory,
                                    text = { Text("删除已观看记录") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteViewedConfirm = true
                                    }
                                )
                                DropdownMenuItem(
                                    enabled = historyViewModel.histories.isNotEmpty() &&
                                        !historyViewModel.managingHistory,
                                    text = { Text("清空全部观看历史") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                                    onClick = {
                                        showMenu = false
                                        showClearConfirm = true
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
                        value = historyViewModel.searchQuery,
                        onValueChange = historyViewModel::updateSearchQuery,
                        singleLine = true,
                        label = { Text("搜索标题、UP 主或 BV 号") },
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (histories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (historyViewModel.isLoading) CircularProgressIndicator()
                else Text(
                    if (historyViewModel.searchQuery.isBlank()) stringResource(R.string.no_data)
                    else "没有匹配的观看历史",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                columns = if (windowSize.widthSizeClass == WindowWidthSizeClass.Compact) {
                    GridCells.Fixed(2)
                } else {
                    GridCells.Adaptive(220.dp)
                },
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(
                    histories,
                    key = { index, item -> "${item.historyBusiness}:${item.historyKid}:$index" }
                ) { index, history ->
                    SmallVideoCard(
                        data = history,
                        onClick = { VideoPlayerActivity.actionStart(context, video = history) },
                        managementActionLabel = "删除历史记录",
                        onManagementAction = { pendingDelete = history to index }
                    )
                }
            }
        }
    }

    pendingDelete?.let { (history, index) ->
        ConfirmActionDialog(
            title = "删除历史记录",
            text = "确定删除“${history.title}”的观看记录吗？",
            onConfirm = {
                pendingDelete = null
                historyViewModel.deleteHistory(history, index)
            },
            onDismiss = { pendingDelete = null }
        )
    }
    if (showClearConfirm) {
        ConfirmActionDialog(
            title = "清空观看历史",
            text = "全部观看历史将被删除且无法恢复，是否继续？",
            onConfirm = {
                showClearConfirm = false
                historyViewModel.clearAllHistory()
            },
            onDismiss = { showClearConfirm = false }
        )
    }
    if (showDeleteViewedConfirm) {
        ConfirmActionDialog(
            title = "删除已观看记录",
            text = "将删除当前已加载且已观看完的历史记录，是否继续？",
            onConfirm = {
                showDeleteViewedConfirm = false
                historyViewModel.deleteViewedHistory()
            },
            onDismiss = { showDeleteViewedConfirm = false }
        )
    }
}
