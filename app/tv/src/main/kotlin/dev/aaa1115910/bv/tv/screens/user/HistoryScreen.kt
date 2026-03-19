package dev.aaa1115910.bv.tv.screens.user

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = koinViewModel(),
    showPageTitle: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(0) }
    val showLargeTitle by remember { derivedStateOf { currentIndex < 4 } }
    val titleFontSize by animateFloatAsState(
        targetValue = if (showLargeTitle) 48f else 24f,
        label = "title font size"
    )
    var showMenuDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoCardData?>(null) }
    val menuFocusRequester = remember { FocusRequester() }
    val deleteFocusRequester = remember { FocusRequester() }
    val contentFocusAnchorRequester = remember { FocusRequester() }
    val lazyGridState = rememberLazyGridState()
    val focusRequesters = remember(historyViewModel.histories.size) {
        List(historyViewModel.histories.size) { FocusRequester() }
    }
    var pendingRestoreFocusIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        if (historyViewModel.histories.isEmpty()) {
            historyViewModel.clearData()
            historyViewModel.update()
        }
    }

    LaunchedEffect(showDeleteConfirmDialog) {
        if (showDeleteConfirmDialog) deleteFocusRequester.requestFocus(scope)
    }

    LaunchedEffect(historyViewModel.deletePhase) {
        when (historyViewModel.deletePhase) {
            1 -> Unit
            2 -> {
                if (historyViewModel.histories.isEmpty()) {
                    pendingRestoreFocusIndex = -1
                } else {
                    pendingRestoreFocusIndex = historyViewModel.pendingFocusIndex.coerceIn(
                        minimumValue = 0,
                        maximumValue = historyViewModel.histories.size - 1
                    )
                }
                historyViewModel.resetDeletePhase()
            }
        }
    }

    LaunchedEffect(
        pendingRestoreFocusIndex,
        showDeleteConfirmDialog,
        historyViewModel.histories.size,
        focusRequesters.size
    ) {
        if (pendingRestoreFocusIndex == -1 || showDeleteConfirmDialog) return@LaunchedEffect
        if (historyViewModel.histories.isEmpty()) {
            pendingRestoreFocusIndex = -1
            return@LaunchedEffect
        }

        val targetIndex = pendingRestoreFocusIndex.coerceIn(
            minimumValue = 0,
            maximumValue = historyViewModel.histories.size - 1
        )
        currentIndex = targetIndex
        lazyGridState.scrollToItem(targetIndex)
        withFrameNanos { }
        withFrameNanos { }
        focusRequesters.getOrNull(targetIndex)?.requestFocus(scope)
        pendingRestoreFocusIndex = -1
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showPageTitle) {
                Box(
                    modifier = Modifier.padding(
                        start = 48.dp,
                        top = 24.dp,
                        bottom = 8.dp,
                        end = 48.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.user_homepage_recent),
                            fontSize = titleFontSize.sp
                        )
                        if (historyViewModel.noMore) {
                            Text(
                                text = stringResource(
                                    R.string.load_data_count_no_more,
                                    historyViewModel.histories.size
                                ),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.load_data_count,
                                    historyViewModel.histories.size
                                ),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val gridColumns = Prefs.gridColumns
        ProvideListBringIntoViewSpec(padding = 26.dp) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .width(1.dp)
                        .height(1.dp)
                        .focusRequester(contentFocusAnchorRequester)
                        .focusable()
                )
                LazyVerticalGrid(
                    modifier = Modifier
                        .padding(innerPadding)
                        .focusRestorer(),
                    state = lazyGridState,
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = historyViewModel.histories,
                        key = { index, item ->
                            "${item.historyBusiness}_${item.historyKid ?: item.avid}_${item.historyViewAt ?: index.toLong()}"
                        }
                    ) { index, history ->
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            SmallVideoCard(
                                modifier = Modifier.focusRequester(focusRequesters[index]),
                                data = history,
                                onClick = {
                                    if (history.jumpToSeason) {
                                        SeasonInfoActivity.actionStart(
                                            context = context,
                                            epId = history.epId,
                                            seasonId = history.seasonId,
                                            proxyArea = ProxyArea.checkProxyArea(history.title)
                                        )
                                    } else {
                                        VideoInfoActivity.actionStart(
                                            context = context,
                                            aid = history.avid,
                                            proxyArea = ProxyArea.checkProxyArea(history.title)
                                        )
                                    }
                                },
                                onLongClick = {
                                    selectedVideo = history
                                    showMenuDialog = true
                                },
                                onFocus = {
                                    currentIndex = index
                                    if (index + 12 > historyViewModel.histories.size) {
                                        historyViewModel.update()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMenuDialog && selectedVideo != null) {
        HistoryMenuDialog(
            show = showMenuDialog,
            focusRequester = menuFocusRequester,
            showGotoUpSpace = selectedVideo!!.upId > 0,
            onDismiss = {
                showMenuDialog = false
                selectedVideo = null
            },
            onDelete = {
                showMenuDialog = false
                showDeleteConfirmDialog = true
            },
            onGotoUpSpace = {
                showMenuDialog = false
                UpInfoActivity.actionStart(
                    context,
                    mid = selectedVideo!!.upId,
                    name = selectedVideo!!.upName,
                    face = selectedVideo!!.upFace
                )
                selectedVideo = null
            }
        )
    }

    if (showDeleteConfirmDialog && selectedVideo != null) {
        DeleteHistoryConfirmDialog(
            show = showDeleteConfirmDialog,
            focusRequester = deleteFocusRequester,
            videoTitle = selectedVideo!!.title,
            onDismiss = {
                showDeleteConfirmDialog = false
                selectedVideo = null
            },
            onConfirm = {
                val targetIndex = maxOf(0, currentIndex - 1)
                historyViewModel.deleteHistory(selectedVideo!!, targetIndex)
                showDeleteConfirmDialog = false
                scope.launch {
                    withFrameNanos { }
                    contentFocusAnchorRequester.requestFocus(this)
                }
                selectedVideo = null
            }
        )
    }
}

@Composable
private fun HistoryMenuDialog(
    show: Boolean,
    focusRequester: FocusRequester,
    showGotoUpSpace: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onGotoUpSpace: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var hasFocused by remember { mutableStateOf(false) }
    val dummyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            hasFocused = false
            dummyFocusRequester.requestFocus(scope)
        }
    }

    if (show) {
        TvAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(R.string.toview_menu_title)) },
            text = {
                Box {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(1.dp)
                            .focusRequester(dummyFocusRequester)
                            .focusable()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    (keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown)
                                ) {
                                    if (!hasFocused) {
                                        focusRequester.requestFocus(scope)
                                        hasFocused = true
                                        return@onKeyEvent true
                                    }
                                }
                                false
                            }
                            .background(Color.Transparent)
                    )

                    LazyColumn(
                        modifier = Modifier.width(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        item {
                            HistoryMenuButton(
                                modifier = Modifier.focusRequester(focusRequester),
                                text = stringResource(R.string.history_menu_delete),
                                onClick = onDelete
                            )
                        }
                        if (showGotoUpSpace) {
                            item {
                                HistoryMenuButton(
                                    text = stringResource(R.string.toview_menu_goto_up_space),
                                    onClick = onGotoUpSpace
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {},
            confirmButton = {}
        )
    }
}

@Composable
private fun DeleteHistoryConfirmDialog(
    show: Boolean,
    focusRequester: FocusRequester,
    videoTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        TvAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(R.string.history_delete_confirm_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.history_delete_confirm_dialog_text,
                        videoTitle
                    )
                )
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(text = stringResource(R.string.history_delete_confirm_dialog_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(
                    modifier = Modifier.focusRequester(focusRequester),
                    onClick = onDismiss
                ) {
                    Text(text = stringResource(R.string.history_delete_confirm_dialog_dismiss))
                }
            }
        )
    }
}

@Composable
private fun HistoryMenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
